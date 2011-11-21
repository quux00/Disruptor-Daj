package demo.disruptor.lmaxsetup;

import java.util.concurrent.CyclicBarrier;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;

public class LMAXReplicator implements EventHandler<LMAXEvent> {
  public void onEvent(LMAXEvent event, long sequence, boolean endOfBatch) {
    int pos = 1;
    if (event.getUnmarshalledMessage() != null) {
      pos++;
    }
    if (event.getJournalMessage() != null) {
      pos++;
    }
    // modify the event in place
    event.setReplicatedMessage("Replicated. Pos: " + pos);
  }
}
