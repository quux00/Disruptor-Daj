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
public class WrapBehavior {
  
  private static final int RING_CAPACITY = 8;
  private RingBuffer<MintEvent> ringBuf;
  private SequenceBarrier consumerBarrier;
  private long npublished = 0L;

  public void engage() {
    setUpRingBuffer();
    long firstPub = publish();
    for (int i = 1; i < RING_CAPACITY; i++) {
      publish();
    }
    publish(); // publish one beyond capacity, overwriting the first
    consume(firstPub);  // this prints out a display that shows that 
                        // the event in slot zero and slot 8 are the same - 
                        // - MintEvent-0 got overwritten
  }

  /**
   * Consumes/Processes as many MintEvent's as it can - up to the last
   * one published and prints out their info to the screen
   * 
   * @param lastPub the slot number of the last published event
   */
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

  /**
   * Publishes a MintEvent into the next available slot on the RingBuffer
   * 
   * @return the slot number that was just published into
   */
  private long publish() {
    long claim = ringBuf.next();
    MintEvent oldev = ringBuf.get(claim);
    MintEvent newev = new MintEvent(UUID.randomUUID(), "MoveInventory", npublished++);
    oldev.copy(newev);
    ringBuf.publish(claim);
    System.out.printf("Just published to event %s to sequence slot: %d\n",
                      newev.getUUID().toString(), claim);
    return claim;
  }

  private void setUpRingBuffer() {
    ringBuf = new RingBuffer<MintEvent>(MintEvent.FACTORY, RING_CAPACITY);
    consumerBarrier = ringBuf.newBarrier();
    // sets the sequence that will gate publishers to prevent the buffer wrapping
    //~TODO: do we need to create a NoOpEventProcessor here?  why not just
    // pass it the RingBuffer itself, - isn't that what this getSequence() returns?
    ringBuf.setGatingSequences(new NoOpEventProcessor(ringBuf).getSequence());
  }

  public static void main(String[] args) {
    new WrapBehavior().engage();
  }
}
