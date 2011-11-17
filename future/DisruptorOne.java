package demo.disruptor;

import java.util.*;
import com.lmax.disruptor.*;

public class DisruptorDemoSeqBarrierWaitFor {
  
  private RingBuffer<DemoEvent> ringBuf;
  private SequenceBarrier consumerBarrier;

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
        DemoEvent ev = ringBuf.get(i);
        System.out.println("Consumed Event: " + ev.toString());
      }
    } catch (Exception e)  {
      System.out.println("Consumer Barrier wait failed: " + e.toString());
    }
  }

  private long publish() {
    long claim = ringBuf.next();
    DemoEvent oldev = ringBuf.get(claim);
    DemoEvent newev = new DemoEvent(UUID.randomUUID(), "MoveInventory");
    oldev.copy(newev);
    ringBuf.publish(claim);
    System.out.printf("Just published to event %s to sequence slot: %d\n", newev.getUUID().toString(), claim);
    return claim;
  }

  private void setUpRingBuffer() {
    ringBuf = new RingBuffer<DemoEvent>(DemoEvent.FACTORY, 64);
    consumerBarrier = ringBuf.newBarrier();
    // sets the sequence that will gate publishers to prevent the buffer wrapping
    //~TODO: do we need to create a NoOpEventProcessor here?  why not just
    // pass it the RingBuffer itself, - isn't that what this getSequence() returns?
    ringBuf.setGatingSequences(new NoOpEventProcessor(ringBuf).getSequence());
  }

  public static void main(String[] args) {
    DisruptorOne d = new DisruptorOne();
    d.engage();
  }
}
