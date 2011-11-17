package demo.disruptor;

import com.lmax.disruptor.*;

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

  /* ----------------------------- */
  /* ---[ SingleSlotProcessor ]--- */
  /* ----------------------------- */

  // TODO: after finish this, try out WorkProcessor<T> to see
  // if it is also a SingleSlotProcessor
  public static class SingleSlotProcessor implements EventProcessor{
    private final int id;
    private final Sequence myseq = new Sequence();  // init val: -1
    private final SequenceBarrier barrier;
    private final RingBuffer<DemoEvent> ringBuf;
    private int lastSlotConsumed = -1;

    public SingleSlotProcessor(final int id,
                               final SequenceBarrier barrier,
                               final RingBuffer<DemoEvent> ringBuf)
    {
      this.id = id;
      this.barrier = barrier;
      this.ringBuf = ringBuf;
    }

    public Sequence getSequence() {
      return myseq;
    }

    public void halt() {}

    public void run() {
      long lastPub;
      try {
        lastPub = barrier.waitFor(barrier.getCursor());
      } catch (Exception e) {
        System.out.println("ERROR: " + e.toString());
        e.printStackTrace();
        return;
      }
      // consume only one, not the whole batch
      DemoEvent ev = ringBuf.get(++lastSlotConsumed);
      System.out.printf("Consumer %d; lastPub = %d; DemoEvent: %s\n", 
                         id, lastPub, ev.toString());

      // need to update your Sequence to let the publisher (or trailing)
      // consumer know when you have processed it
      myseq.set(myseq.get() + 1L);
    }
  }

  public static void main(String[] args) {
    new GateOnTwoConsumers().init().engage();
  }
}
