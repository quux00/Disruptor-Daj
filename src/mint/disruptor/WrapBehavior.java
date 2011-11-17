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
public class WrapBehavior extends AbstractDemoFramework {
  
  @Override
  public void engage() {
    long firstPub = publish();
    for (int i = 1; i < getRingCapacity(); i++) {
      publish();
    }
    publish(); // publish one beyond capacity, overwriting the first
    consumeAll(firstPub);  // this prints out a display that shows that 
                           // the event in slot zero and slot 8 are the same - 
                           // - MintEvent-0 got overwritten
  }

  @Override
  public int getRingCapacity() {
    return 8;
  }

  public static void main(String[] args) {
    new WrapBehavior().init().engage();
  }
}
