package demo.disruptor;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import com.lmax.disruptor.*;

/**
 * Abstract test/demo framework providing key set up, publish
 * consume methods for testing out and learning the Disruptor framework
 */
public abstract class AbstractDemoFramework {
  
  private RingBuffer<DemoEvent> ringBuf;
  private SequenceBarrier consumerBarrier;
  private long npublished = 0L;

  /**
   * All subclasses must implement - main logic goes here
   */
  public abstract void engage();

  /**
   * 
   * Override this to set the RING_CAPACITY size
   * defaults to 32
   */
  public int getRingCapacity() {
    return 32;
  }

  public RingBuffer<DemoEvent> getRingBuffer() {
    return ringBuf;
  }

  /**
   * Consumes/Processes only one DemoEvent, even if more
   * are available
   * 
   * @param lastPub the slot number of the last published event
   */
  protected void consumeOne(long consumeSlot) {
    try {
      long mostRecent = consumerBarrier.waitFor(consumeSlot);
      System.out.printf("Consumer: Asked for %d; last published: %d\n",
                        consumeSlot, mostRecent);

      DemoEvent ev = ringBuf.get(consumeSlot);
      System.out.println("Consumed Event: " + ev.toString());

    } catch (Exception e)  {
      System.out.println("Consumer Barrier retrieve failed: " + e.toString());
      e.printStackTrace();
    }    
  }

  /**
   * Consumes/Processes as many DemoEvent's as it can - up to the last
   * one published and prints out their info to the screen
   * 
   * @param lastPub the slot number of the last published event
   */
  protected void consumeAll(long consumeSlot) {
    try {
      long mostRecent = consumerBarrier.waitFor(consumeSlot);
      System.out.printf("Consumer: Asked for %d; last published: %d\n",
                        consumeSlot, mostRecent);

      // cycle through all received
      for (long i = consumeSlot; i <= mostRecent; i++) {
        DemoEvent ev = ringBuf.get(i);
        System.out.println("Consumed Event: " + ev.toString());
      }

    } catch (Exception e)  {
      System.out.println("Consumer Barrier retrieve failed: " + e.toString());
      e.printStackTrace();
    }
  }

  /**
   * Publishes a DemoEvent into the next available slot on the RingBuffer
   * 
   * @return the slot number that was just published into
   */
  protected long publish() {
    return publish(-1);
  }

  /**
   * Publishes a DemoEvent into the next available slot on the RingBuffer
   * with a timeout.  If it times out, a note is printed to STDOUT
   * and the method returns -1.
   * 
   * @timeoutInSeconds number of seconds to wait before timing out
   * @return the slot number that was just published into
   */
  protected long publish(long timeoutInSeconds) {
    long claim;
    if (timeoutInSeconds < 0) {
      claim = ringBuf.next();
    } else {
      try {
        claim = ringBuf.next(timeoutInSeconds, TimeUnit.SECONDS);
      } catch (Exception e) {
        System.out.println("Publish request timed out: " + e.toString());
        return -1;
      }
    }
    DemoEvent oldev = ringBuf.get(claim);
    DemoEvent newev = new DemoEvent(UUID.randomUUID(), "MoveInventory", npublished++);
    oldev.copy(newev);
    ringBuf.publish(claim);
    System.out.printf("Just published to event %s to sequence slot: %d\n",
                      newev.getUUID().toString(), claim);
    return claim;    
  }

  /**
   * Initializes a RingBuffer and a NoOpEventProcessor
   * and sets the GatingSequences to be off the NoOpEventProcessor (which
   * doesn't gate at all, since it always advances in lock step with the
   * Ring's publisher barrier.
   */
  protected AbstractDemoFramework init() {
    ringBuf = new RingBuffer<DemoEvent>(DemoEvent.FACTORY, getRingCapacity());
    consumerBarrier = ringBuf.newBarrier();
    NoOpEventProcessor ep = new NoOpEventProcessor(ringBuf);
    // sets the sequence that will gate publishers to prevent the buffer wrapping
    ringBuf.setGatingSequences(ep.getSequence());
    return this;
  }


}
