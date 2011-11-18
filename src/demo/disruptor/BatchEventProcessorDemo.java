package demo.disruptor;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import com.lmax.disruptor.*;
import demo.disruptor.util.DemoEventHandler;

/**
 * Demonstrates how to use a BatchEventProcessor - it will grab all
 * available (published) events/entries on the RingBuffer and send
 * one at a time to its EventHandler, telling it when it has reached
 * the end of the "batch".
 */
public class BatchEventProcessorDemo extends AbstractDemoFramework {
  
  @Override
  public void engage() {
    final RingBuffer<DemoEvent> ringBuf = getRingBuffer();
    final SequenceBarrier ringBarrier = ringBuf.newBarrier();
    final CountDownLatch latch = new CountDownLatch(9);

    final BatchEventProcessor<DemoEvent> batchProc =
      new BatchEventProcessor<DemoEvent>(ringBuf,
                                         ringBarrier,
                                         new DemoEventHandler(1, latch));

    ringBuf.setGatingSequences(batchProc.getSequence());

    Thread t = new Thread(batchProc);
    t.setDaemon(true);
    t.start();

    publish();
    publish();
    publish();
    
    try {
      System.out.println(">> Publisheder going to temp sleep");
      System.out.flush();
      Thread.sleep(800);
      publish();
      publish();
      publish();
      publish();
      publish();
      publish();
      latch.await();

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public int getRingCapacity() {
    // you can set this to 2 and you'll see the batch size is now
    // limited to 1 or 2, since the publisher is gated on the processor
    return 8;
  }

  public static void main(String[] args) {
    new BatchEventProcessorDemo().init().engage();
  }
}
