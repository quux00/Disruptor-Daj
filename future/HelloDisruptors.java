import java.util.concurrent.*;
import com.lmax.disruptor.*;

public class HelloDisruptors implements EventTranslator<ValueEvent> {

  public static final int RING_SIZE = 64;
  private static final ExecutorService exec = Executors.newFixedThreadPool(2);

  public HelloDisruptors() {}

  /**
   * Main working method 
   */
  public void engage() {
    RingBuffer<ValueEvent> ringBuffer = 
      new RingBuffer<ValueEvent>(ValueEvent.EVENT_FACTORY,
                                 new SingleThreadedClaimStrategy(RING_SIZE),
                                 new SleepingWaitStrategy());

    EventHandler<ValueEvent> evHandler = createEventHandler();

    SequenceBarrier barrier = ringBuffer.newBarrier();
    BatchEventProcessor<ValueEvent> evProc = 
      new BatchEventProcessor<ValueEvent>(ringBuffer, barrier, evHandler);
    
    ringBuffer.setGatingSequences(evProc.getSequence());

    // publish some messages
    EventPublisher<ValueEvent> eventPublisher = new EventPublisher<ValueEvent>(ringBuffer);
    eventPublisher.publishEvent(this);
    eventPublisher.publishEvent(this);

    System.out.println("About to start EventProc in a thread");
    System.out.flush();
    exec.submit(evProc);

    for (int i = 0; i < 100; i++) {
      eventPublisher.publishEvent(this);
      eventPublisher.publishEvent(this);
      try {
        Thread.sleep(5);
      } catch (Exception e) {
        System.err.println("ERROR: " + e.toString());
      }
      eventPublisher.publishEvent(this);
    }
    try {
      Thread.sleep(500);
    } catch (Exception e) {
      System.err.println("ERROR: " + e.toString());
    }
  }

  @Override
  public ValueEvent translateTo(ValueEvent event, long sequence) {
    System.out.println("Publisher translating is putting in: " + (sequence + 66));
    System.out.flush();
    event.setValue(sequence + 66);
    return event;
  }

  private EventHandler<ValueEvent> createEventHandler() {
    return new EventHandler<ValueEvent>() {
      public void onEvent(final ValueEvent event,
                          final long sequence,
                          final boolean endOfBatch) throws Exception 
      {
        System.out.printf("Event Handler Received: %s\n", event.getValue());
        System.out.flush();
      }
    };
  }

  public static void main(String[] args) {
    new HelloDisruptors().engage();
  }
}
