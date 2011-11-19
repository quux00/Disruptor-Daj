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

  private int id = -1;
  private boolean full_info_b = false;  // whether to print out a lot of info when translating an event
  
  public DemoEventTranslator() { }
  public DemoEventTranslator(int id, boolean full_info_b) { 
    this.id = id;
    this.full_info_b = full_info_b;
  }

  public DemoEvent translateTo(DemoEvent event, long sequence) {
    if (full_info_b) {
      System.out.println("----------------------------------------------------");
      System.out.println("DemoEventTranslator called with event: " + event.toString());
    }

    // one has to modify the passed in event, rather than creating a new event
    // and returning it
    if (id >= 0) {
      event.setName("Translated Event by Publisher/Translator #" + id);
    } else {
      event.setName("Translated Event");      
    }
    event.setProcessId(sequence);
    event.setUUID(UUID.randomUUID());
    event.setDate(new Date());

    if (full_info_b) {
      System.out.println("DemoEventTranslator modified it to: " + event.toString());
      System.out.println("----------------------------------------------------");
    } else {
      System.out.printf("Translated/published event: %s; procId: %d\n", event.getName(), event.getProcessId());
    }
    System.out.flush();

    // TODO: not sure what the return value is used for
    return null;
  }
}
