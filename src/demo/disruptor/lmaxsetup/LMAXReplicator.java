package demo.disruptor.lmaxsetup;

import java.util.concurrent.CyclicBarrier;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;

public class LMAXReplicator implements EventHandler<LMAXEvent> {
  public void onEvent(LMAXEvent event, long sequence, boolean endOfBatch) {
    // // DEBUG
    // System.out.printf("LMAXReplicator: seq: %d endOfBatch: %s\n", sequence, endOfBatch);
    // System.out.flush();
    // // END DEBUG

    // simulate a slight delay due to IO
    try { Thread.sleep(5); } catch (Exception e) {};

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
