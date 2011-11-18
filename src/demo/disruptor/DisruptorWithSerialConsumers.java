package demo.disruptor;

import java.util.*;
import java.util.concurrent.Executors;
import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;

import demo.disruptor.util.DemoEventTranslator;
import demo.disruptor.util.DemoEventHandler;

/**
 * Using the Disruptor DSL API, this class demonstrates having one
 * consumer (processor) gated on another consumer (processor) which
 * is gated on the publisher.
 */
public class DisruptorWithSerialConsumers {
  
  public static final int RING_SIZE = 4;

  Disruptor<DemoEvent> disruptor;
  EventTranslator<DemoEvent> evTranslator;
  EventHandler<DemoEvent> evHandler1;
  EventHandler<DemoEvent> evHandler2;
  EventHandler<DemoEvent> evHandler3;

  @SuppressWarnings("unchecked")
  public DisruptorWithSerialConsumers() {
    disruptor = new Disruptor<DemoEvent>(DemoEvent.FACTORY,
                                         RING_SIZE,
                                         Executors.newCachedThreadPool());

    // create two EventHandlers, each of which will process
    // the same events, one after the other, but handler2
    // is gated on handler1
    evHandler1 = new DemoEventHandler(1);
    evHandler2 = new DemoEventHandler(2);

    // first register handler1 as gating on the publisher
    disruptor.handleEventsWith(evHandler1);
    // then register handler2 has gating on handler1
    disruptor.after(evHandler1).handleEventsWith(evHandler2);
    
    evTranslator = new DemoEventTranslator();
  }

  public void engage() {
    System.out.println("Starting the Disruptor");
    // starts the event processors (consumers)
    RingBuffer<DemoEvent> ringBuf = disruptor.start();   

    // publish three events
    disruptor.publishEvent(evTranslator);
    disruptor.publishEvent(evTranslator);
    disruptor.publishEvent(evTranslator);

    // wait for all events to be processed, then stop the processors
    disruptor.shutdown(); 
  }

  public static void main(String[] args) {
    new DisruptorWithSerialConsumers().engage();
  }
}
