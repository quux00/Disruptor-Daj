package demo.disruptor.lmaxsetup;

import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CyclicBarrier;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.LifecycleAware;

public class LMAXFinalHandler implements EventHandler<LMAXEvent>, LifecycleAware {
  private final List<String> eventMsgs = new ArrayList<String>();
  private final BufferedWriter fwriter;

  public LMAXFinalHandler(final File outFile) {
    try {
      fwriter = new BufferedWriter( new FileWriter(outFile) );      

    } catch (Exception e) {
      System.err.println("ERROR: Unable to open FileWriter in LMAXFinalHandler");
      throw new RuntimeException(e);
    }    
  }

  public void onEvent(LMAXEvent event, long sequence, boolean endOfBatch) {
    // modify the event in place
    event.setMarshalled(true);

    // batch the writes
    if (endOfBatch) {
      eventMsgs.add( event.toString() );
      writeToFile( joinMessages() );
      eventMsgs.clear();

    } else {
      eventMsgs.add( event.toString() );
    }    
  }

  private String joinMessages() {
    final StringBuilder sb = new StringBuilder();
    for (String s: eventMsgs) {
      sb.append(s).append("\n");
    }
    return sb.toString();
  }

  private void writeToFile(String s) {
    try {
      fwriter.write(s);
      fwriter.newLine();
      fwriter.flush();
      
    } catch (Exception e) {
      System.out.println("ERROR: unable to write to output file: " + e.toString());
      e.printStackTrace();
    }
  }

  /* ---[ LifecycleAware Methods ]--- */

  public void onStart() {
    // DEBUG
    System.out.println("LifeCycleAware in FinalHandler: onStart called");
    // END DEBUG
  }

  public void onShutdown() {
    // DEBUG
    System.out.println("LifeCycleAware in FinalHandler: onShutdown called");
    // END DEBUG
    if (fwriter != null) {
      try {
        fwriter.close();
      } catch (Exception e) {
        System.err.println("ERROR: Unable to close FileWriter in LMAXFinalHandler: " +
                           e.toString());
      }
    }
  }
}
