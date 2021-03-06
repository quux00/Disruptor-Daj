package demo.disruptor.fluent_dsl;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.CyclicBarrier;

import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;

import demo.disruptor.*;
import demo.disruptor.util.DemoEventHandler;

public class MultiplePublishersUsingDisruptorNoTranslator {
  
  private final static int RING_BUFFER_SZ = 16;
  private final static int NUM_PUBLISHERS = 4;
  private final static int NUM_MSGS_TO_PUBLISH_PER_PUBLISHER = 4;

  private final Disruptor<DemoEvent> disruptor;
  private final List<DisruptorDemoPublisher> pubs = new ArrayList<DisruptorDemoPublisher>();
  private final CyclicBarrier cycBarrier = new CyclicBarrier(NUM_PUBLISHERS);

  
  @SuppressWarnings("unchecked")
  public MultiplePublishersUsingDisruptorNoTranslator() {
    disruptor = new Disruptor<DemoEvent>(DemoEvent.FACTORY,
                                         Executors.newCachedThreadPool(),  // TODO: is this the right one to use here???
                                         new MultiThreadedClaimStrategy(RING_BUFFER_SZ),
                                         new YieldingWaitStrategy());
    disruptor.handleEventsWith( new DemoEventHandler(1) );  // just one consumer for this demo

    for (int i = 1; i <= NUM_PUBLISHERS; i++) {
      pubs.add( new DisruptorDemoPublisher(i, NUM_MSGS_TO_PUBLISH_PER_PUBLISHER,
                                           disruptor.getRingBuffer(), cycBarrier).shouldPause(true) );
    }
  }

  public void engage() {
    System.out.println("Starting the Disruptor");
    // starts the event processors
    final RingBuffer<DemoEvent> ringBuf = disruptor.start();
    
    // now we start the publishers
    List<Future<?>> futures = new ArrayList<Future<?>>();
    ExecutorService execService = Executors.newFixedThreadPool(NUM_PUBLISHERS);

    try {
      for (DisruptorDemoPublisher pub: pubs) {
        futures.add( execService.submit(pub) );
      }

      // wait for each publisher to finish
      for (Future<?> f: futures) {
        f.get();
      }

      // this should wait until all events are processed
      disruptor.shutdown();
      
    } catch (Exception e) {
      System.out.println(e.toString());
      e.printStackTrace();

    } finally {
      execService.shutdown();
    }
  }

  /* --------------------------------------------- */
  /* ---[ Inner class: DisruptorDemoPublisher ]--- */
  /* --------------------------------------------- */

  public static class DisruptorDemoPublisher implements Runnable {
    private boolean should_pause_b = false;

    private final int id;
    private final int howmany;   // how many events to publish once its run method is called
    private final RingBuffer<DemoEvent> ringBuf;
    private final CyclicBarrier cyclicBarrier;

    public DisruptorDemoPublisher(int id, int howmany, RingBuffer<DemoEvent> rb) {
      this(id, howmany, rb, null);
    }

    public DisruptorDemoPublisher(int id, int howmany, RingBuffer<DemoEvent> rb, CyclicBarrier cyclicBarrier) {
      this.id = id;
      this.howmany = howmany;
      this.ringBuf = rb;
      this.cyclicBarrier = cyclicBarrier;
    }

    /**
     * Whether the publisher should pause very slightly between each 
     * event it publishes.  If yes, this can allow a more scrambled order
     * to the published events in the RingBuffer.
     * @return Reference to itself to allow for fluent chaining chains
     */
    public DisruptorDemoPublisher shouldPause(boolean s) {
      should_pause_b = s;
      return this;
    }

    @SuppressWarnings("unchecked")
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
          System.out.printf("Published by DemoPublisher #%d; procId: %d; slot: %d\n",id,i,seq);
          ringBuf.publish(seq);
          if (should_pause_b) try { Thread.sleep(8); } catch (Exception e) {}
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }
  


  public static void main(String[] args) {
    new MultiplePublishersUsingDisruptorNoTranslator().engage();
  }
}
