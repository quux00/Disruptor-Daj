package demo.disruptor.util;

import java.util.concurrent.CountDownLatch;
import com.lmax.disruptor.*;
import demo.disruptor.DemoEvent;

/* ----------------------------- */
/* ---[ SingleSlotProcessor ]--- */
/* ----------------------------- */

/**
 * An Event Processor that will consume only the next entry off
 * the RingBuffer, even if more are available to be processed
 */
public class SingleSlotProcessor implements EventProcessor{
  private final int id;
  private final Sequence myseq = new Sequence();  // init val: -1
  private final SequenceBarrier barrier;
  private final RingBuffer<DemoEvent> ringBuf;
  private final CountDownLatch latch;
  private int lastSlotConsumed = -1;

  public SingleSlotProcessor(final int id,
                             final SequenceBarrier barrier,
                             final RingBuffer<DemoEvent> ringBuf)
  {
    this(id, barrier, ringBuf, null);
  }

  public SingleSlotProcessor(final int id,
                             final SequenceBarrier barrier,
                             final RingBuffer<DemoEvent> ringBuf,
                             final CountDownLatch latch)
  {
    this.id = id;
    this.barrier = barrier;
    this.ringBuf = ringBuf;
    this.latch = latch;
  }

  public Sequence getSequence() {
    return myseq;
  }

  public void halt() {}

  public void run() {
    long lastPub;
    try {
      lastPub = barrier.waitFor(barrier.getCursor());
    } catch (Exception e) {
      System.out.println("ERROR: " + e.toString());
      e.printStackTrace();
      return;
    }
    // consume only one, not the whole batch
    DemoEvent ev = ringBuf.get(++lastSlotConsumed);
    System.out.printf("Consumer %d; lastPub = %d; DemoEvent: %s\n", 
                      id, lastPub, ev.toString());

    // need to update your Sequence to let the publisher (or trailing)
    // consumer know when you have processed it
    myseq.set(myseq.get() + 1L);

    if (latch != null) latch.countDown();
  }
}

