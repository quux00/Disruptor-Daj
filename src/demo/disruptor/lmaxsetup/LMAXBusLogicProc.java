package demo.disruptor.lmaxsetup;


import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CyclicBarrier;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;

public class LMAXBusLogicProc implements EventHandler<LMAXEvent> {

  final Disruptor<LMAXEvent> outputDisruptor;

  public LMAXBusLogicProc(final Disruptor<LMAXEvent> outputDisruptor) {
    this.outputDisruptor = outputDisruptor;
  }

  public void onEvent(LMAXEvent event, long sequence, boolean endOfBatch) {
    validateEvent(event);
    doBusinessProcessingLogic(event);
    publishToOuputDisruptor(event);
  }


  private void publishToOuputDisruptor(final LMAXEvent inputEvent) {
    final RingBuffer<LMAXEvent> outputRingBuf = outputDisruptor.getRingBuffer();

    long seq = outputRingBuf.next();
    LMAXEvent outputEvent = outputRingBuf.get(seq);
    outputEvent.copy(inputEvent);
    outputEvent.setPushedOnto2Id(seq);
    outputRingBuf.publish(seq);
  }
  
  private void doBusinessProcessingLogic(final LMAXEvent ev) {
    // mark the event as "pulled off"
    long id_p1 = ev.getPushedOnto1Id();
    ev.setPulledOff1Id(id_p1);

    // simulate doing some important business work
    for (long i = 0; i < 100; i++) {
      id_p1++;
    }
  }

  private void validateEvent(LMAXEvent event) {
    if (event.getUnmarshalledMessage() == null) {
      throw new RuntimeException("LMAXBusLogicProc: unmarshalled message is null!");
    }
    if (event.getReplicatedMessage() == null) {
      throw new RuntimeException("LMAXBusLogicProc: replicated message is null!");
    }
    if (event.getJournalMessage() == null) {
      throw new RuntimeException("LMAXBusLogicProc: journaled message is null!");
    }    
  }
}
