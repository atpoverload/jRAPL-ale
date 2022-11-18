package jrapl;

import static com.google.protobuf.util.Timestamps.fromMicros;

import java.util.HashMap;

/** Simple wrapper around rapl access. */
public final class Rapl {
  // TODO: the delimiter is currently hacked-in to be ;. this was done because of the formatting
  // issue associated with , vs . on certain locales
  private static final String ENERGY_STRING_DELIMITER = ";";
  private static final HashMap<String, Integer> COMPONENTS;

  private static final double WRAP_AROUND;
  private static final double DRAM_WRAP_AROUND;

  /** Returns an {@link RaplSample} populated by parsing the string returned by {@ readNative}. */
  public static RaplSample sample() {
    String[] entries = readNative().split(ENERGY_STRING_DELIMITER);

    RaplSample.Builder sample =
        RaplSample.newBuilder()
            .setTimestamp(fromMicros(Long.parseLong(entries[entries.length - 1])));

    // pull out energy values
    for (int socket = 0; socket < MicroArchitecture.SOCKET_COUNT; socket++) {
      RaplReading.Builder reading = RaplReading.newBuilder().setSocket(socket + 1);
      for (String component : COMPONENTS.keySet()) {
        double energy =
            Double.parseDouble(entries[COMPONENTS.size() * socket + COMPONENTS.get(component)]);
        switch (component) {
          case "pkg":
            reading.setPackage(energy);
            break;
          case "dram":
            reading.setDram(energy);
            break;
          case "core":
            reading.setCore(energy);
            break;
          case "gpu":
            reading.setGpu(energy);
            break;
        }
      }
      sample.addReading(reading);
    }

    return sample.build();
  }

  /**
   * Computes the forward differences of two {@link RaplSamples}, assuming that they are correctly
   * ordered and have matching sockets. Although this API guarantees that, samples from other
   * sources may misbehave when using this.
   */
  public static RaplDifference difference(RaplSample first, RaplSample second) {
    // TODO: this assumes the order is good. we should be checking the timestamps
    RaplDifference.Builder diff =
        RaplDifference.newBuilder().setStart(first.getTimestamp()).setEnd(second.getTimestamp());
    // TODO: this assumes the order is good. we should be checking the sockets match up
    for (int socket = 0; socket < first.getReadingCount(); socket++) {
      diff.addReading(
          RaplReading.newBuilder()
              .setSocket(socket + 1)
              .setPackage(
                  diffWithWraparound(
                      first.getReading(socket).getPackage(),
                      second.getReading(socket).getPackage()))
              .setDram(
                  diffWithDramWraparound(
                      first.getReading(socket).getDram(), second.getReading(socket).getDram()))
              .setCore(
                  diffWithWraparound(
                      first.getReading(socket).getCore(), second.getReading(socket).getCore()))
              .setGpu(
                  diffWithWraparound(
                      first.getReading(socket).getGpu(), second.getReading(socket).getGpu())));
    }

    return diff.build();
  }

  private static double diffWithWraparound(double first, double second) {
    double energy = second - first;
    if (energy < 0) {
      energy += WRAP_AROUND;
    }
    return energy;
  }

  private static double diffWithDramWraparound(double first, double second) {
    double energy = second - first;
    if (energy < 0) {
      energy += DRAM_WRAP_AROUND;
    }
    return energy;
  }

  private static HashMap<String, Integer> getComponents() {
    HashMap<String, Integer> components = new HashMap<>();
    // TODO -- there's a 5th possible power domain, right? like full motherboard energy or something
    int index = 0;
    for (String component : components().split(",")) {
      components.put(component, index++);
    }
    return components;
  }

  /**
   * Returns the energy of each component and the current timestamp as a delimited string. The
   * energy values are floating point numbers representing the number of joules since the last boot.
   * The order will be each socket's {@code components} followed by the microsecond timestamp:
   *
   * <p>socket0_component0,socket0_component1,socket1_component0,socket1_component1,...,timestamp
   */
  private static native String readNative();

  /** Returns the available components as a string ("dram,core,pkg"/"dram,core,gpu,pkg"/etc). */
  private static native String components();

  private static native int wrapAround();

  private static native int dramWrapAround();

  static {
    NativeLibrary.initialize();

    WRAP_AROUND = wrapAround();
    DRAM_WRAP_AROUND = dramWrapAround();
    COMPONENTS = getComponents();
  }

  private Rapl() {}

  public static void main(String[] args) throws Exception {
    System.out.println("RAPL initialized");
    System.out.println(String.format("Micro architecture: %s", MicroArchitecture.NAME));
    System.out.println(String.format("Socket count: %d", MicroArchitecture.SOCKET_COUNT));

    if (COMPONENTS.isEmpty()) {
      System.out.println("No components found!");
      return;
    }
    System.out.println(String.format("Available components: %s", COMPONENTS.keySet()));
    System.out.println(String.format("Energy counter wrap around: %f", WRAP_AROUND));
    if (WRAP_AROUND != DRAM_WRAP_AROUND) {
      System.out.println(String.format("DRAM counter wrap around: %f", DRAM_WRAP_AROUND));
    }

    JraplUtils.poll(args, Rapl::sample, Rapl::difference);
  }
}
