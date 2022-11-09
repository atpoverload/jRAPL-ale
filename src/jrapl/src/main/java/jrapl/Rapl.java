package jrapl;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

/** Simple wrapper around rapl access. */
public final class Rapl {
  public static final int SOCKET_COUNT;

  private static final int COMPONENT_COUNT;
  private static final int[] COMPONENT_INDICES;
  private static final String ENERGY_STRING_DELIMITER = ";";

  static EnergySample sample() {
    double[][] energy = new double[SOCKET_COUNT][EnergySample.Component.values().length];
    String[] entries = readNative().split(ENERGY_STRING_DELIMITER);

    // pull out energy values
    for (int socket = 0; socket < SOCKET_COUNT; socket++) {
      for (EnergySample.Component component : EnergySample.Component.values()) {
        int index = COMPONENT_INDICES[component.ordinal()];
        if (index >= 0) {
          int componentIndex = COMPONENT_COUNT * socket + index;
          energy[socket][component.ordinal()] =
              // TODO: replace() is a fix for localization issues (add the issue)
              Double.parseDouble(entries[componentIndex].replace(",", "."));
        }
      }
    }

    // parse the timestamp
    long ts = Long.parseLong(entries[entries.length - 1]);
    long seconds = TimeUnit.MICROSECONDS.toSeconds(ts);
    long nanos = TimeUnit.MICROSECONDS.toNanos(ts) - TimeUnit.SECONDS.toNanos(seconds);
    Instant timestamp = Instant.ofEpochSecond(seconds, nanos);

    return new EnergySample(timestamp, energy);
  }

  /** Returns the energy of each component as a delimited string. */
  static native String readNative();

  private static native int sockets();

  private static native String componentIndices();

  static {
    NativeLibrary.initialize();

    SOCKET_COUNT = sockets();

    // TODO -- there's a 5th possible power domain, right? like full motherboard energy or something
    COMPONENT_INDICES = new int[] {-1, -1, -1, -1};
    int index = 0;
    for (String component : componentIndices().split(",")) {
      switch (component) {
        case "dram":
          COMPONENT_INDICES[EnergySample.Component.DRAM.ordinal()] = index++;
          break;
        case "gpu":
          COMPONENT_INDICES[EnergySample.Component.GPU.ordinal()] = index++;
          break;
        case "core":
          COMPONENT_INDICES[EnergySample.Component.CORE.ordinal()] = index++;
          break;
        case "pkg":
          COMPONENT_INDICES[EnergySample.Component.PACKAGE.ordinal()] = index++;
          break;
        default:
          continue;
      }
    }
    COMPONENT_COUNT = index;
  }

  public static void main(String[] args) {
    System.out.println(sample().toJson());
  }
}
