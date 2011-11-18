package demo.disruptor.util;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import com.lmax.disruptor.*;
import demo.disruptor.DemoEvent;

public class DemoWorkHandler implements WorkHandler<DemoEvent> {
  private final int id;  // internal identifier for ease of identification in printouts
  private final CountDownLatch latch;

  public DemoWorkHandler(int id) {
    this(id, null);
  }

  public DemoWorkHandler(final int id, final CountDownLatch latch) {
    this.id = id;
    this.latch = latch;
  }

  public void onEvent(DemoEvent event) {
    System.out.printf("DemoWorkHandler #%d received event with ProcessID: %d\n",
                      id, event.getProcessId());
    System.out.flush();
    if (latch != null) latch.countDown();
  }
}
