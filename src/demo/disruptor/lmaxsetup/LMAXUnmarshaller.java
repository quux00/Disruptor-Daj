package demo.disruptor.lmaxsetup;

import java.util.concurrent.CyclicBarrier;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;

public class LMAXUnmarshaller implements EventHandler<LMAXEvent> {
  public void onEvent(LMAXEvent event, long sequence, boolean endOfBatch) {
    // // DEBUG
    // System.out.printf("LMAXUnmarshaller: seq: %d endOfBatch: %s\n", sequence, endOfBatch);
    // System.out.flush();
    // // END DEBUG
    int pos = 1;
    if (event.getReplicatedMessage() != null) {
      pos++;
    }
    if (event.getJournalMessage() != null) {
      pos++;
    }
    // modify the event in place
    event.setUnmarshalledMessage("Unmarshalled. Pos: " + pos);
  }
}
