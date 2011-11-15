package mint.disruptor;

import java.util.Date;
import java.util.UUID;

import com.lmax.disruptor.EventFactory;

public class MintEvent {

  private UUID uuid;
  private String name;
  private Date timestamp;
  private long procId;    // dummy id to make it easier to watch in the RingBuffer

  public String toString() {
    return String.format("MintEvent:\n  UUID: %s\n  Name: %s\n  Process Id: %d\n  Timestamp: %s",
                         uuid.toString(), name, procId, timestamp.toString());
  }

  public MintEvent() {
  }

  public MintEvent(final UUID uuid, final String name, final long procId) {
    this(uuid, name, procId, new Date());
  }

  public MintEvent(final UUID uuid, final String name, final long procId, final Date tstamp) {
    this.uuid = uuid;
    this.name = name;
    this.procId = procId;
    this.timestamp = new Date(tstamp.getTime());
  }

  public MintEvent(MintEvent ev) {
    this.uuid = ev.uuid;
    this.name = ev.name;
    this.timestamp = new Date(ev.timestamp.getTime());    
  }

  public void copy(MintEvent ev) {
    this.uuid = ev.uuid;
    this.name = ev.name;
    this.procId = ev.procId;
    this.timestamp = new Date(ev.timestamp.getTime());
  }

  /* ---[ GETTERS and SETTERS ]--- */

  public UUID getUUID() { return uuid; }
  public String getName() { return name; }
  public Date getTimestamp() { return new Date(timestamp.getTime()); }
  public long getProcessId() { return procId; }
  
  public void setUUID(UUID uuid) {
    this.uuid = uuid;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setDate(Date tstamp) {
    this.timestamp = new Date(tstamp.getTime());
  }

  public void setProcessId(long pid) {
    this.procId = procId;
  }


  /* ---[ Static EventFactory ]--- */
  public static final EventFactory<MintEvent> FACTORY = new EventFactory<MintEvent>() {
    /**
     * Since only intended to be used to populate the RingBuffers 
     * with Dummy empty events, return dummy empty events from here
     */
    public MintEvent newInstance() {
      return new MintEvent();
    }
  };
  
}
