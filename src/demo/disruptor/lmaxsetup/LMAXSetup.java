package demo.disruptor.lmaxsetup;

import java.io.File;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;

import demo.disruptor.*;
import demo.disruptor.util.DemoEventTranslator;
import demo.disruptor.util.DemoEventHandler;

/**
 * LMAXSetup is a simple mock implementation of the LMAX Order Processing Application
 *  described in their presentations and Martin Fowler's blog writeup.
 * There are two Disruptors (RingBuffers).
 * There is one Publisher, the "LMAXReceiver" into Disruptor #1.
 * There are three parallel processors that gate on the LMAXReceiver (the "trio"):
 *  1. LMAXUnmarshaller
 *  2. LMAXJournaler
 *  3. LMAXReplicator
 * There is one processor that gates on the above three processors: 
 *  the Business Logic Processor (BLP)
 * The LMAX_BLP hands it off to some POJOs to business processing and then is the
 *  publisher onto the second Disruptor.
 * The second Disruptor has two processors that have to operate serially: first the 
 *  LMAXMarshaller and then the LMAX Publisher.  This latter one has a terrible name
 *  for the Disruptor pattern, so to avoid confusion, I call it the LMAXFinalHandler
 *
 * To simulate all this and ensure that things are done in the right order, I have constructed
 * an LMAXEvent that has one field for each of the "actors" in this scenario:
 * 
 * LMAXReceiver sets the pushedOnto1Id, incrementing by one
 * LMAXJournaler sets a "journalMessage" field stating that it was journaled and whether it was
 *  the first, second or third of the trio to handle it
 * LMAXReplicator sets a "replicatorMessage" field stating that it was replicated and whether it
 *  was the first, second or third of the trio to handle it
 * LMAXUnmarshaller sets a "unmarshalledMessage" field stating that it was unmarshalled and 
 *  whether it was the first, second or third of the trio to handle it
 * LMAX_BLP sets a "pulledOff1Id", incrementing by one and pushedOnto2Id, matching the pullOff1Id
 * LMAXMarshaller sets a "marshalled" boolean field to true
 * LMAXFinalHandler then checks that all fields above were set from their defaults, keeps
 *  track of how many went first from each of the trio, reports that to STDOUT and then 
 *  logs the result to a file "LMAX.output.txt".
 */
public class LMAXSetup {
  
  public static final int RING_SIZE = 4096;
  // number of events to push through the LMAX dual Disruptor system
  public static final int TOT_EVENTS = 400;
  public static final File outputFile = new File("lmax.out");

  private Disruptor<LMAXEvent> inputDisruptor;
  private Disruptor<LMAXEvent> outputDisruptor;
  private ExecutorService execService;
  private CountDownLatch latch = new CountDownLatch(TOT_EVENTS);

  public LMAXSetup init() {
    // TODO: try a Fixed Thread pool later
    // what is the pool for: publishers or consumers?
    execService = Executors.newCachedThreadPool();

    // create the Input Disruptor
    inputDisruptor = new Disruptor<LMAXEvent>(LMAXEvent.FACTORY,
                                              execService,
                                              new MultiThreadedClaimStrategy(RING_SIZE),
                                              new YieldingWaitStrategy());

    outputDisruptor = new Disruptor<LMAXEvent>(LMAXEvent.FACTORY,
                                               execService,
                                               new MultiThreadedClaimStrategy(RING_SIZE),
                                               new YieldingWaitStrategy());
    plugInConsumersForInputDisruptor();
    plugInConsumersForOutputDisruptor();

    return this;
  }

  /**
   * Main functionality method
   */
  public LMAXSetup engage() {
    inputDisruptor.start();
    outputDisruptor.start();
    startReceiverPublisher();

    try {
     latch.await(); // wait for FinalHandler to process all the events 
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    
    inputDisruptor.shutdown();
    outputDisruptor.shutdown();
    execService.shutdown();
    return this;
  }

  private void startReceiverPublisher() {
    execService.execute(new LMAXReceiver(inputDisruptor, TOT_EVENTS).shouldPause(true));
  }

  // TODO: still not sure how to create an implicit or explicit array
  //       of generic objects, which the handleEventsWith method requires
  @SuppressWarnings("unchecked")
  private void plugInConsumersForInputDisruptor() {
    // these three parallel processors, but all need to process all events
    // so we can't use a WorkerPool here, since that would farm out events
    // between the handlers
    inputDisruptor.handleEventsWith(new LMAXJournaler(),
                                    new LMAXUnmarshaller(),
                                    new LMAXReplicator()).
      // after all 3 have processed events, then the BusLogicProcessor goes
      then(new LMAXBusLogicProc(outputDisruptor));
  }


  @SuppressWarnings("unchecked")
  private void plugInConsumersForOutputDisruptor() {
    outputDisruptor.handleEventsWith(new LMAXMarshaller()).
      then(new LMAXFinalHandler(outputFile, latch));
  }

  /* ---[ Main ]--- */
  public static void main(String[] args) {
    new LMAXSetup().init().engage();
  }
}
