package jrapl;

import static com.google.protobuf.util.Timestamps.fromMicros;
import static java.util.stream.Collectors.toMap;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/** Simple wrapper around rapl access. */
public final class Rapl {
  // TODO: the delimiter is currently hacked-in to be ;. this was done because of
  // the formatting
  // issue associated with , vs . on certain locales
  private static final String ENERGY_STRING_DELIMITER = ";";
  private static final HashMap<String, Integer> COMPONENTS;

  private static final double WRAP_AROUND;
  private static final double DRAM_WRAP_AROUND;

  /**
   * Returns the values read from rapl as a double array. Each component for a single socket is
   * listed before the next socket. The component order associated with the other element can be
   * determined from {@code getComponents}. The final element is always a microsecond precision unix
   * timestamp.
   *
   * <p>For example, if the components are {"package": 0, "dram": 1} with socket 0 and 1, the output
   * will be [package_0, dram_0, package_1, dram_1, unixtime_seconds].
   */
  public static double[] read() {
    double[] entries =
        Arrays.stream(readNative().split(ENERGY_STRING_DELIMITER))
            .mapToDouble(e -> Double.parseDouble(e))
            .toArray();
    entries[entries.length - 1] /= 1000000; // convert to seconds to be consistent
    return entries;
  }

  /** Returns an {@link RaplSample} populated by parsing the string returned by {@ readNative}. */
  public static RaplSample sample() {
    if (COMPONENTS.isEmpty()) {
      JraplUtils.LOGGER.info("no components founds; rapl likely not available");
      return RaplSample.getDefaultInstance();
    }

    String[] entries = readNative().split(ENERGY_STRING_DELIMITER);

    RaplSample.Builder sample =
        RaplSample.newBuilder()
            .setSource(RaplSource.RAPL)
            .setTimestamp(fromMicros(Long.parseLong(entries[entries.length - 1])));

    // pull out energy values
    for (int socket = 0; socket < MicroArchitecture.SOCKET_COUNT; socket++) {
      RaplReading.Builder reading = RaplReading.newBuilder().setSocket(socket);
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
    // TODO: this assumes the order is good. we should be checking the sockets match
    // up
    Map<Integer, RaplReading> firstReadings =
        first.getReadingList().stream().collect(toMap(r -> r.getSocket(), r -> r));
    Map<Integer, RaplReading> secondReadings =
        second.getReadingList().stream().collect(toMap(r -> r.getSocket(), r -> r));
    for (int socket : firstReadings.keySet()) {
      diff.addReading(
          RaplReading.newBuilder()
              .setSocket(socket)
              .setPackage(
                  diffWithWraparound(
                      firstReadings.get(socket).getPackage(),
                      secondReadings.get(socket).getPackage()))
              .setDram(
                  diffWithDramWraparound(
                      firstReadings.get(socket).getDram(), secondReadings.get(socket).getDram()))
              .setCore(
                  diffWithWraparound(
                      firstReadings.get(socket).getCore(), secondReadings.get(socket).getCore()))
              .setGpu(
                  diffWithWraparound(
                      firstReadings.get(socket).getGpu(), secondReadings.get(socket).getGpu())));
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
    // TODO -- there's a 5th possible power domain, right? like full motherboard
    // energy or something
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
    if (NativeLibrary.initialize()) {
      WRAP_AROUND = wrapAround();
      DRAM_WRAP_AROUND = dramWrapAround();
      COMPONENTS = getComponents();
    } else {
      JraplUtils.LOGGER.info("native library couldn't be initialized; rapl likely not available");
      WRAP_AROUND = 0;
      DRAM_WRAP_AROUND = 0;
      COMPONENTS = new HashMap<>();
    }
  }

  private Rapl() {}
}
