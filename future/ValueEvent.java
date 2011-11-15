import com.lmax.disruptor.EventFactory;

public class ValueEvent {

  private long value;

  public void setValue(long v) {
    value = v;
  }

  public long getValue() {
    return value;
  }

  public final static EventFactory<ValueEvent> EVENT_FACTORY = 
    new EventFactory<ValueEvent>() {
    public ValueEvent newInstance() {
      return new ValueEvent();
    }
  };
    
  // public static void main(String[] args) {
  //   ImmValueEvent e = new ImmValueEvent.Builder(32111L).build();
  //   System.out.printf("value: %d\n", e.value);
  // }
}

