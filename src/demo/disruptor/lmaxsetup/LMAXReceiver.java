package demo.disruptor.lmaxsetup;

import java.util.concurrent.CyclicBarrier;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;

public class LMAXReceiver implements Runnable {
  // number published onto first Disruptor
  private int nPublished = -1;
  // number of events to publish
  private final int nToPub;
  private final Disruptor<LMAXEvent> inputDisruptor;
  private final CyclicBarrier cycBarrier;
  private boolean b_pause = false;
  
  public LMAXReceiver(final Disruptor<LMAXEvent> inputDisruptor, final int nToPub) {
    this(inputDisruptor, nToPub, null);
  }

  public LMAXReceiver(final Disruptor<LMAXEvent> inputDisruptor,
                      final int nToPub,
                      final CyclicBarrier cycBarrier) {
    this.inputDisruptor = inputDisruptor;
    this.nToPub = nToPub;
    this.cycBarrier = cycBarrier;
  }

  /**
   * Whether the publisher should (very briefly) pause between 
   * event publications in order to see interleaving of pubs
   * and consumes.
   */
  public LMAXReceiver shouldPause(boolean b) {
    b_pause = b;
    return this;
  }

  public void run() {
    final RingBuffer<LMAXEvent> ringBuf = inputDisruptor.getRingBuffer();

    if (cycBarrier != null) try { cycBarrier.await(); } catch (Exception e) {}

    for (int i = 0; i < nToPub; i++) {
      long seq = ringBuf.next();
      LMAXEvent ev = ringBuf.get(seq);
      ev.setPushedOnto1Id(seq);
      ringBuf.publish(seq);
      if (b_pause) try { Thread.sleep(1); } catch (Exception e) {};
    }
  }
}
