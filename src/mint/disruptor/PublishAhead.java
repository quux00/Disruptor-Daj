package mint.disruptor;

import java.util.*;

public class PublishAhead extends AbstractDemoFramework {
  
  @Override
  public void engage() {
    long firstPub = publishAhead();
    consume(firstPub);   // this will get the one just pushed
    consume(firstPub-1); // this will get the one just pushed
                         // and the dummy/blank Event before it
  }

  public long publishAhead() {
    long pubat = getRingBuffer().next() + 3L;
    MintEvent oldev = getRingBuffer().get((long)3);
    MintEvent newev = new MintEvent(UUID.randomUUID(), "AdjustQty", 3);
    oldev.copy(newev);
    getRingBuffer().forcePublish(pubat);
    System.out.printf("Just published to event %s to sequence slot: %d\n",
                      newev.getUUID().toString(), pubat);
    return pubat;
  }

  @Override
  public int getRingCapacity() {
    return 8;
  }

  public static void main(String[] args) {
    new PublishAhead().init().engage();
  }
}
