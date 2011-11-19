package demo.disruptor;

import java.util.*;
import java.util.concurrent.Executors;

import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;

import demo.disruptor.util.DemoEventTranslator;
import demo.disruptor.util.DemoEventHandler;

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
    // the same events, one after the other, not being gated on each 
    // other, but only on the publisher
    // if you run this demo a couple of times, you'll that sometimes
    // handler1 goes first and sometimes handler2 goes first - not
    // gated on each other, only the publisher
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
    System.out.println("Publishing one event via a Translator");
    disruptor.publishEvent(evTranslator);
    // wait for all events to be processed, then stop the processors
    disruptor.shutdown();    
  }

  public static void main(String[] args) {
    new SimpleDisruptor().engage();
  }
}
