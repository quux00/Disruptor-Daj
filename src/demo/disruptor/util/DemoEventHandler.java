package demo.disruptor.util;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import com.lmax.disruptor.*;
import demo.disruptor.DemoEvent;

public class DemoEventHandler implements EventHandler<DemoEvent> {
  private final int id;  // internal identifier for ease of identification in printouts
  private final CountDownLatch latch;

  public DemoEventHandler(int id) {
    this(id, null);
  }

  public DemoEventHandler(final int id, final CountDownLatch latch) {
    this.id = id;
    this.latch = latch;
  }

  public void onEvent(DemoEvent event, long sequence, boolean endOfBatch) {
    System.out.printf("DemoEventHandler #%d received event from slot %d: %s\n",
                      id, sequence, event.toString());
    if (endOfBatch) {
      System.out.println("Was End of Batch for DemoEventHandler: " + id);
    }
    System.out.flush();
    if (latch != null) latch.countDown();
  }
}
