import java.util.*;

import com.lmax.disruptor.EventFactory;

public class ImmValueEvent {

  public final long value;

  private ImmValueEvent(ImmValueEvent.Builder builder) {
    this.value = builder.value;
  }

  public static class Builder {
    final long value;
    public Builder() {}

    public Builder value(long value) {
      this.value = value;
    }
    public ImmValueEvent build() {
      return new ImmValueEvent(this);
    }
  }

  public final static EventFactory<ImmValueEvent.Builder> EVENT_FACTORY = 
    new EventFactor<ImmValueEvent.Builder>() {
    public ImmValueEvent.Builder newInstance() {
      return new ImmValueEvent.Builder();
    }
  }

  // public static void main(String[] args) {
  //   ImmValueEvent e = new ImmValueEvent.Builder(32111L).build();
  //   System.out.printf("value: %d\n", e.value);
  // }
}

