package jrapl;

import java.time.Instant;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/** Simple wrapper around rapl access. */
public final class Rapl {
  public static final int SOCKET_COUNT;

  private static final int COMPONENT_COUNT;
  private static final int[] COMPONENT_INDICES;
  // TODO: the delimiter is currently hacked-in to be ;. this was done because of the formatting
  // issue associated with , vs . on certain locales
  private static final String ENERGY_STRING_DELIMITER = ";";

  /** Returns an {@link EnergySample} populated by parsing the string returned by {@ readNative}. */
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

  /**
   * Returns the energy of each component and the current timestamp as a delimited string. The
   * energy values are floating point numbers representing the number of joules since the last boot.
   * The order will be each socket's {@code components} followed by the microsecond timestamp:
   *
   * <p>socket0_component0,socket0_component1,socket1_component0,socket1_component1,...,timestamp
   */
  private static native String readNative();

  /** Returns the number of sockets on the system. */
  private static native int sockets();

  /** Returns the available components as a string ("dram,core,pkg"/"dram,core,gpu,pkg"/etc). */
  private static native String components();

  static {
    NativeLibrary.initialize();

    SOCKET_COUNT = sockets();

    // TODO -- there's a 5th possible power domain, right? like full motherboard energy or something
    COMPONENT_INDICES = new int[] {-1, -1, -1, -1};
    int index = 0;
    for (String component : components().split(",")) {
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

  public static void main(String[] args) throws Exception {
    System.out.println("RAPL initialized");

    System.out.println(String.format("Socket count: %d", SOCKET_COUNT));

    ArrayList<EnergySample.Component> components = new ArrayList<>();
    for (EnergySample.Component component : EnergySample.Component.values()) {
      if (COMPONENT_INDICES[component.ordinal()] > -1) {
        components.add(component);
      }
    }
    if (components.isEmpty()) {
      System.out.println("No components found!");
      return;
    }
    System.out.println(String.format("Available components: %s", components));

    EnergySample lastSample;
    while (true) {
      EnergySample sample = sample();
      System.out.println(sample.toJson());
      lastSample = sample;
      Thread.sleep(1000);
    }
  }
}
