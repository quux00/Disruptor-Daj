package mint.disruptor;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import com.lmax.disruptor.*;

/**
 * Abstract test/demo framework providing key set up, publish
 * consume methods for testing out and learning the Disruptor framework
 */
public abstract class AbstractDemoFramework {
  
  private RingBuffer<MintEvent> ringBuf;
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

  public Sequence[] getGatingSequences() {
    EventProcessor[] eps = getEventProcessors();
    Sequence[] seqs = new Sequence[eps.length];
    for (int i = 0; i < eps.length; i++) {
      seqs[i] = eps[i].getSequence();
    }
    return seqs;
  }

  // override this to set the EventProcessor
  // defaults to NoOpEventProcessor
  public EventProcessor[] getEventProcessors() {
    EventProcessor[] ary = {new NoOpEventProcessor(ringBuf)};
    return ary;
  }

  public RingBuffer<MintEvent> getRingBuffer() {
    return ringBuf;
  }

  /**
   * Consumes/Processes only one MintEvent, even if more
   * are available
   * 
   * @param lastPub the slot number of the last published event
   */
  protected void consumeOne(long consumeSlot) {
    try {
      long mostRecent = consumerBarrier.waitFor(consumeSlot);
      System.out.printf("Consumer: Asked for %d; last published: %d\n",
                        consumeSlot, mostRecent);

      MintEvent ev = ringBuf.get(consumeSlot);
      System.out.println("Consumed Event: " + ev.toString());

    } catch (Exception e)  {
      System.out.println("Consumer Barrier retrieve failed: " + e.toString());
      e.printStackTrace();
    }    
  }

  /**
   * Consumes/Processes as many MintEvent's as it can - up to the last
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
        MintEvent ev = ringBuf.get(i);
        System.out.println("Consumed Event: " + ev.toString());
      }

    } catch (Exception e)  {
      System.out.println("Consumer Barrier retrieve failed: " + e.toString());
      e.printStackTrace();
    }
  }

  /**
   * Publishes a MintEvent into the next available slot on the RingBuffer
   * 
   * @return the slot number that was just published into
   */
  protected long publish() {
    return publish(-1);
  }

  /**
   * Publishes a MintEvent into the next available slot on the RingBuffer
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
    MintEvent oldev = ringBuf.get(claim);
    MintEvent newev = new MintEvent(UUID.randomUUID(), "MoveInventory", npublished++);
    oldev.copy(newev);
    ringBuf.publish(claim);
    System.out.printf("Just published to event %s to sequence slot: %d\n",
                      newev.getUUID().toString(), claim);
    return claim;    
  }

  /**
   * Initializes a RingBuffer
   * and sets the GatingSequences to be
   * 
   */
  protected AbstractDemoFramework init() {
    ringBuf = new RingBuffer<MintEvent>(MintEvent.FACTORY, getRingCapacity());
    consumerBarrier = ringBuf.newBarrier();
    // sets the sequence that will gate publishers to prevent the buffer wrapping
    ringBuf.getGatingSequences();
    return this;
  }

}
