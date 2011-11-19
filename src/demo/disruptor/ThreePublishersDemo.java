package demo.disruptor;

import java.util.concurrent.CyclicBarrier;

import com.lmax.disruptor.*;
import demo.disruptor.util.SingleSlotProcessor;

public class ThreePublishersDemo {
  
  public final static int RING_BUFFER_SIZE = 8;
  final RingBuffer<DemoEvent> ringBuf;
  final BatchEventProcessor<DemoEvent> batchProc;

  public ThreePublishersDemo() {
    ringBuf = new RingBuffer<DemoEvent>(DemoEvent.FACTORY,
                                        new MultiThreadedClaimStrategy(RING_BUFFER_SIZE),
                                        new YieldingWaitStrategy());
    // define EventHandler
    // TODO:

    // define BatchEventProcessor wrapping the EventHandler
    batchProc = new BatchEventProcessor<DemoEvent>(ringBuf,
                                                   ringBuf.newBarrier(),
                                                   evHandler);
                                        
  }

  public void engage() {
    
  }

  public static class DemoPublisher implements Runnable {
    private final int id;
    private final RingBuffer<DemoEvent> ringBuf;
    private final CyclicBarrier cyclicBarrier;
    private int pubcount = 0;  // number of events published

    public DemoPublisher(int id, RingBuffer<DemoEvent> ringBuf) {
      this(id, ringBuf, null);
    }

    public DemoPublisher(int id, RingBuffer<DemoEvent> ringBuf, CyclicBarrier cyclicBarrier) {
      this.id = id;
      this.ringBuf = ringBuf;
      this.cyclicBarrier = cyclicBarrier;
    }

    public void run() {
      try {
        if (cyclicBarrier != null) cyclicBarrier.await();

        long seq = ringBuf.next();
        DemoEvent ev = ringBuf.get(seq);
        ev.setProcessId(pubcount++);
        ev.setName("Published by DemoPublisher #" + id);
        ringBuf.publish(seq);
        
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }


  public static void main(String[] args) {
    new ThreePublishersDemo().engage();
  }
}
