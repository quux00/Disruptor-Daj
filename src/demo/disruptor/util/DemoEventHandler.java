package demo.disruptor.util;

import java.util.*;
import com.lmax.disruptor.*;
import demo.disruptor.DemoEvent;

public class DemoEventHandler implements EventHandler<DemoEvent> {
  private int id;  // internal identifier for ease of identification in printouts

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
