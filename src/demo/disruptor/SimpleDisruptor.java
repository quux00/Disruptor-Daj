package demo.disruptor;

import java.util.*;
import java.util.concurrent.Executors;
import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;

import java.util.*;

/**
 * Demonstration of a simple usage of the Disruptor "DSL"
 * provided by the Disruptor API.  It will create two processors (using
 * EventHandler implementation) and publish one event via an EventTranslator.
 */
public class SimpleDisruptor {
  
  public static final int RING_SIZE = 32;

  Disruptor<DemoEvent> disruptor;
  EventTranslator<DemoEvent> evTranslator;
                                                            
  // I can't seem to satisfy the compiler when passing EventHandlers
  // to handleEventsWith, so I just told it shut up instead ...
  @SuppressWarnings("unchecked")
  public SimpleDisruptor() {
    disruptor = new Disruptor<DemoEvent>(DemoEvent.FACTORY,
                                         RING_SIZE,
                                         Executors.newCachedThreadPool());
    // create two EventHandlers, each of which will process
    // the same events, one after the other, not being gated on each other,
    // but only on the publisher
    EventHandler<DemoEvent> evHandler1 = new DemoEventHandler(1);
    EventHandler<DemoEvent> evHandler2 = new DemoEventHandler(2);
    disruptor.handleEventsWith(evHandler1, evHandler2);

    evTranslator = new DemoEventTranslator();
  }

  public void engage() {
    System.out.println("Starting the Disruptor");
    // starts the event processors (consumers)
    RingBuffer<DemoEvent> ringBuf = disruptor.start();

    // now we have to publish something via the translator
    disruptor.publishEvent(evTranslator);
    disruptor.shutdown(); // wait for all events to be processed, then stop the processors
  }


  /* ---[ DemoEventHandler static class ]--- */
  public static class DemoEventHandler implements EventHandler<DemoEvent> {
    private int id;  // internal identifier

    public DemoEventHandler(int id) {
      this.id = id;
    }

    public void onEvent(DemoEvent event, long sequence, boolean endOfBatch) {
      System.out.printf("DemoEventHandler #%d received event from slot %d: %s\n",
                        id, sequence, event.toString());
      if (endOfBatch) {
        System.out.println("Was End of Batch for DemoEventHandler: " + id);
      }
      System.out.flush();
    }
  }

  /* ---[ DemoEventTranslator static class ]--- */
  /**
   * This class is used to publish new events to the RingBuffer.
   * In order to do so, it "translates" the existing event in the slot
   * passed to it (in the translateTo() methods) to an event with new data.
   */
  public static class DemoEventTranslator
    implements com.lmax.disruptor.EventTranslator<DemoEvent> {

    public DemoEvent translateTo(DemoEvent event, long sequence) {
      System.out.println("----------------------------------------------------");
      System.out.println("DemoEventTranslator called with event: " + event.toString());

      // one has to modify the passed in event, rather than creating a new event
      // and returning it
      event.setName("Translated Event");
      event.setProcessId( event.getProcessId() + 100 );
      event.setUUID(UUID.randomUUID());
      event.setDate(new Date());

      System.out.println("DemoEventTranslator modified it to: " + event.toString());
      System.out.println("----------------------------------------------------");
      System.out.flush();

      // TODO: not sure what the return value is used for
      return null;
    }
  }


  public static void main(String[] args) {
    new SimpleDisruptor().engage();
  }
}
