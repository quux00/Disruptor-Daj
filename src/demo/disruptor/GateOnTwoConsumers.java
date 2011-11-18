package demo.disruptor;

import com.lmax.disruptor.*;
import demo.disruptor.util.SingleSlotProcessor;


/**
 * Here we have two processors (consumers) that alternate which slots
 * to consume (evens vs. odds).  This test shows how to do that and
 * also shows that the Publisher/Producer is gated on both.
 */
public class GateOnTwoConsumers extends AbstractDemoFramework {

  @Override
  public void engage() {
    final RingBuffer<DemoEvent> ringBuf = getRingBuffer();
    final SequenceBarrier ringBarrier = ringBuf.newBarrier();  
    //TODO: should each share the same barrier: yes
    SingleSlotProcessor ssp1 = new SingleSlotProcessor(1, ringBarrier, ringBuf);
    SingleSlotProcessor ssp2 = new SingleSlotProcessor(2, ringBarrier, ringBuf);

    // override the default gating sequences to our new processors
    ringBuf.setGatingSequences(ssp1.getSequence(), ssp2.getSequence());

    // publish once in each slot
    for (int i = 0; i < getRingCapacity(); i++) {
      publish();
    }
    
    // now try to publish again - should timeout
    System.out.println("Try to publish before any consumers have consumed - should timeout");
    publish(1);

    // now consume and try to publish again - should still timeout
    // as both consumers have not consumed
    ssp1.run();
    ssp1.run();  // consume second slot with first processor

    System.out.println("Try to publish after only one consumer has consumed - should timeout");
    publish(1);
    
    ssp2.run();
    System.out.println("Try to publish after only both consumer has consumed - should work");
    publish(1);    
  }
  
  @Override
  public int getRingCapacity() {
    return 4;
  }

  public static void main(String[] args) {
    new GateOnTwoConsumers().init().engage();
  }
}
