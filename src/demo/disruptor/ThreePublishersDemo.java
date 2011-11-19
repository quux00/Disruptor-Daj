package demo.disruptor;

import java.util.*;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.lmax.disruptor.*;
import demo.disruptor.util.DemoEventHandler;

public class ThreePublishersDemo {
  
  public final static int RING_BUFFER_SIZE = 8;
  public final static int NUM_MSGS_TO_PUBLISH = 18;  // needs to be evenly divisible by 3
  public final static int NUM_PUBLISHERS = 3;

  final RingBuffer<DemoEvent> ringBuf;
  final BatchEventProcessor<DemoEvent> batchProc;
  final List<DemoPublisher> pubs;   // list of 3 publishers 
  // latch to wait until the consumer is finished
  final CountDownLatch latch = new CountDownLatch(NUM_MSGS_TO_PUBLISH); 
  // barrier to synchronize all the publishing threads to start at the same time
  final CyclicBarrier cyclicBarrier = new CyclicBarrier(NUM_PUBLISHERS);

  public ThreePublishersDemo() {
    ringBuf = new RingBuffer<DemoEvent>(DemoEvent.FACTORY,
                                        new MultiThreadedClaimStrategy(RING_BUFFER_SIZE),
                                        new YieldingWaitStrategy());
    // define EventHandler
    DemoEventHandler evHandler = new DemoEventHandler(1, latch);
    
    // define BatchEventProcessor wrapping the EventHandler
    batchProc = new BatchEventProcessor<DemoEvent>(ringBuf,
                                                   ringBuf.newBarrier(),
                                                   evHandler);
    pubs = new ArrayList<DemoPublisher>();
    final int nmsgs = NUM_MSGS_TO_PUBLISH / 3;
    // NOTE: try it with and without pausing - w/o pauses, you'll longer stretches
    // from the same publisher in a row
    pubs.add( new DemoPublisher(1, nmsgs, ringBuf, cyclicBarrier).shouldPause(true) );
    pubs.add( new DemoPublisher(2, nmsgs, ringBuf, cyclicBarrier).shouldPause(true) );
    pubs.add( new DemoPublisher(3, nmsgs, ringBuf, cyclicBarrier).shouldPause(true) );

    // set the processor sequence as the gating sequence on the RingBuffer
    ringBuf.setGatingSequences( batchProc.getSequence() );
  }

  public void engage() {
    final ExecutorService execService = Executors.newFixedThreadPool(NUM_PUBLISHERS + 1);

    // start a thread with the BatchEventProcessor
    // don't need a future on this one, so call execute() instead of submit()
    execService.execute(batchProc);

    // start the publishers in separate threads
    final List<Future<?>> futures = new ArrayList<Future<?>>();
    for (DemoPublisher pub: pubs) {
      futures.add( execService.submit(pub) );
    }

    try {
      // wait for each publisher to finish (TODO: this step probably not necessary)
      for (Future f : futures) {
        f.get();
      }

      // wait for processor to finish 'consuming' all messages
      latch.await();
    } catch (Exception e) {
      System.out.println(e.toString());
      e.printStackTrace();
    } finally {
      batchProc.halt();
      execService.shutdown();
    }
  }

  /* ---[ Inner class: DemoPublisher ]--- */
  public static class DemoPublisher implements Runnable {
    private final int id;
    private final int howmany;   // how many events to publish once its run method is called
    private boolean should_pause_b = false;
    private final RingBuffer<DemoEvent> ringBuf;
    private final CyclicBarrier cyclicBarrier;

    public DemoPublisher(int id, int howmany, RingBuffer<DemoEvent> ringBuf) {
      this(id, howmany, ringBuf, null);
    }

    public DemoPublisher(int id, int howmany, RingBuffer<DemoEvent> ringBuf, CyclicBarrier cyclicBarrier) {
      this.id = id;
      this.howmany = howmany;
      this.ringBuf = ringBuf;
      this.cyclicBarrier = cyclicBarrier;
    }

    /**
     * Whether the publisher should pause very slightly between each 
     * event it publishes.  If yes, this can allow a more scrambled order
     * to the published events in the RingBuffer.
     * @return Reference to itself to allow for fluent chaining chains
     */
    public DemoPublisher shouldPause(boolean s) {
      should_pause_b = s;
      return this;
    }

    public void run() {
      try {
        if (cyclicBarrier != null) cyclicBarrier.await();

        for (int i = 0; i < howmany; i++) {
          long seq = ringBuf.next();
          DemoEvent ev = ringBuf.get(seq);
          // each event will have the pubcount in processId
          // and the name of the publisher it came from in the Name field
          ev.setProcessId(i);
          ev.setName("Published by DemoPublisher #" + id);
          ringBuf.publish(seq);
          if (should_pause_b) try { Thread.sleep(1); } catch (Exception e) {}
        }
        
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }


  public static void main(String[] args) {
    new ThreePublishersDemo().engage();
  }
}
