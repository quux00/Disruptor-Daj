package mint.disruptor;

import java.util.*;
import com.lmax.disruptor.*;

/**
 * Abstract test/demo framework providing key set up and publish
 * consume methods for testing out and learning the Disruptor framework
 */
public abstract class AbstractDemoFramework {
  
  private final int RING_CAPACITY;
  private RingBuffer<MintEvent> ringBuf;
  private SequenceBarrier consumerBarrier;
  private long npublished = 0L;

  public abstract void engage();

  // override this to set the RING_CAPACITY size
  // defaults to 32
  public void getRingCapacity() {
    return 32;
  }

  // override this to set the EventProcessor
  // defaults to NoOpEventProcessor
  public EventProcessor getEventProcessor() {
    return new NoOpEventProcessor(ringBuf);
  }

  public RingBuffer getRingBuffer() {
    return ringBuf;
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
    ringBuf = new RingBuffer<MintEvent>(MintEvent.FACTORY, getRingCapacity());
    consumerBarrier = ringBuf.newBarrier();
    // sets the sequence that will gate publishers to prevent the buffer wrapping
    //~TODO: do we need to create a NoOpEventProcessor here?  why not just
    // pass it the RingBuffer itself, - isn't that what this getSequence() returns?
    ringBuf.setGatingSequences(getEventProcessor().getSequence());
  }

}
