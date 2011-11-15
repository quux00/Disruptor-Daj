package mint.disruptor;

import java.util.*;
import com.lmax.disruptor.*;

/**
 * Demonstrates how to set up a RingBuffer with a NoOpProcessor
 * and how to publish/consume in the same thread.
 * Key example is to demo how the SequenceBarrier#waitFor method
 * works - it will return the highest sequence slot that has been
 * published, even if you asked for a lower slot number.
 */
public class DisruptorDemoSeqBarrierWaitFor {
  
  private RingBuffer<MintEvent> ringBuf;
  private SequenceBarrier consumerBarrier;
  private long npublished = 0;

  public void engage() {
    setUpRingBuffer();
    long firstPub = publish();
    publish();
    consume(firstPub);
  }

  private void consume(long lastPub) {
    try {
      long mostRecent = consumerBarrier.waitFor(lastPub);
      System.out.printf("Consumer: Asked for %d; last published: %d\n", lastPub, mostRecent);
      for (long i = lastPub; i <= mostRecent - lastPub; i++) {
        MintEvent ev = ringBuf.get(i);
        System.out.println("Consumed Event: " + ev.toString());
      }
    } catch (Exception e)  {
      System.out.println("Consumer Barrier wait failed: " + e.toString());
    }
  }

  private long publish() {
    long claim = ringBuf.next();
    MintEvent oldev = ringBuf.get(claim);
    MintEvent newev = new MintEvent(UUID.randomUUID(), "MoveInventory", npublished++);
    //    MintEvent newev = new MintEvent(UUID.randomUUID(), "MoveInventory", npublished++);
    oldev.copy(newev);
    ringBuf.publish(claim);
    System.out.printf("Just published to event %s to sequence slot: %d\n", newev.getUUID().toString(), claim);
    return claim;
  }

  private void setUpRingBuffer() {
    ringBuf = new RingBuffer<MintEvent>(MintEvent.FACTORY, 64);
    consumerBarrier = ringBuf.newBarrier();
    // sets the sequence that will gate publishers to prevent the buffer wrapping
    //~TODO: do we need to create a NoOpEventProcessor here?  why not just
    // pass it the RingBuffer itself, - isn't that what this getSequence() returns?
    ringBuf.setGatingSequences(new NoOpEventProcessor(ringBuf).getSequence());
  }

  public static void main(String[] args) {
    new DisruptorDemoSeqBarrierWaitFor().engage();
  }
}
