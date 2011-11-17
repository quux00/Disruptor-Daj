package mint.disruptor;

import java.util.*;
import com.lmax.disruptor.*;

/**
 * Demonstrates how to set up a RingBuffer with a NoOpProcessor
 * and how to publish/consume in the same thread.
 * Key example is to demo how the SequenceBarrier#waitFor method
 * works - it will return the highest sequence slot that has been
 * published, even if you asked for a lower slot number.
 */
public class DisruptorDemoSeqBarrierWaitFor extends AbstractDemoFramework {

  @Override
  public void engage() {
    System.out.println("Sequencer.INITIAL_CURSOR_VALUE: " + Sequencer.INITIAL_CURSOR_VALUE);
    System.out.println("Cursor before pub1: " + getRingBuffer().getCursor());
    long firstPub = publish();
    System.out.println("Cursor after  pub1: " + getRingBuffer().getCursor());
    publish();
    System.out.println("Cursor after  pub2: " + getRingBuffer().getCursor());
    consume(firstPub);
  }

  public static void main(String[] args) {
    new DisruptorDemoSeqBarrierWaitFor().init().engage();
  }
}
