package demo.disruptor.fluent_dsl;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.CyclicBarrier;

import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;

import demo.disruptor.*;
import demo.disruptor.util.DemoEventTranslator;
import demo.disruptor.util.DemoEventHandler;

public class MultiplePublishersUsingDisruptor {
  
  private final static int RING_BUFFER_SZ = 16;
  private final static int NUM_PUBLISHERS = 4;
  private final static int NUM_MSGS_TO_PUBLISH_PER_PUBLISHER = 4;

  private final Disruptor<DemoEvent> disruptor;
  private final List<DisruptorDemoPublisher> pubs = new ArrayList<DisruptorDemoPublisher>();
  private final CyclicBarrier cycBarrier = new CyclicBarrier(NUM_PUBLISHERS);

  @SuppressWarnings("unchecked")
  public MultiplePublishersUsingDisruptor() {
    disruptor = new Disruptor<DemoEvent>(DemoEvent.FACTORY,
                                         Executors.newCachedThreadPool(),  // TODO: is this the right one to use here???
                                         new MultiThreadedClaimStrategy(RING_BUFFER_SZ),
                                         new YieldingWaitStrategy());
    disruptor.handleEventsWith( new DemoEventHandler(1) );  // just one consumer for this demo

    for (int i = 1; i <= NUM_PUBLISHERS; i++) {
      // TODO: also try setting shouldPause() !!
      pubs.add( new DisruptorDemoPublisher(i, NUM_MSGS_TO_PUBLISH_PER_PUBLISHER,
                                           disruptor, cycBarrier).shouldPause(true) );
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
    private final Disruptor<DemoEvent> disruptor;
    private final CyclicBarrier cyclicBarrier;
    private final EventTranslator evTranslator;

    public DisruptorDemoPublisher(int id, int howmany, Disruptor<DemoEvent> dr) {
      this(id, howmany, dr, null);
    }

    public DisruptorDemoPublisher(int id, int howmany, Disruptor<DemoEvent> dr, CyclicBarrier cyclicBarrier) {
      this.id = id;
      this.howmany = howmany;
      this.disruptor = dr;
      this.cyclicBarrier = cyclicBarrier;
      evTranslator = new DemoEventTranslator(id, false);
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

    // TODO: need to make DemoEventTranslator parameterized to see if this warning
    //       message goes away ...
    @SuppressWarnings("unchecked")
    public void run() {
      try {
        if (cyclicBarrier != null) cyclicBarrier.await();

        for (int i = 0; i < howmany; i++) {
          disruptor.publishEvent(evTranslator);
          if (should_pause_b) try { Thread.sleep(100); } catch (Exception e) {}
        }
        
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }
  


  public static void main(String[] args) {
    new MultiplePublishersUsingDisruptor().engage();
  }
}
