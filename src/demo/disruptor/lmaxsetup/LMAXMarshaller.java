package demo.disruptor.lmaxsetup;

import java.util.concurrent.CyclicBarrier;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;

public class LMAXMarshaller implements EventHandler<LMAXEvent> {
  public void onEvent(LMAXEvent event, long sequence, boolean endOfBatch) {
    // modify the event in place
    event.setMarshalled(true);
  }
}
