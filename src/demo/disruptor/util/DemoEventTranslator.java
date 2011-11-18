package demo.disruptor.util;

import java.util.*;
import com.lmax.disruptor.*;
import demo.disruptor.DemoEvent;

/**
 * This class is used to publish new events to the RingBuffer.
 * In order to do so, it "translates" the existing event in the slot
 * passed to it (in the translateTo() methods) to an event with new data.
 */
public class DemoEventTranslator
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
