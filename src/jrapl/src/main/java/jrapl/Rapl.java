package jrapl;

import static com.google.protobuf.util.Timestamps.fromMicros;

import java.util.HashMap;
import jrapl.EnergyProtos.EnergyReading;
import jrapl.EnergyProtos.EnergySample;
import jrapl.EnergyProtos.EnergySampleDifference;

/** Simple wrapper around rapl access. */
public final class Rapl {
  private static final double WRAP_AROUND = wrapAround();
  private static final double DRAM_WRAP_AROUND = dramWrapAround();

  private static final HashMap<String, Integer> COMPONENTS = getComponents();

  // TODO: the delimiter is currently hacked-in to be ;. this was done because of the formatting
  // issue associated with , vs . on certain locales
  private static final String ENERGY_STRING_DELIMITER = ";";

  /** Returns an {@link EnergySample} populated by parsing the string returned by {@ readNative}. */
  public static EnergySample sample() {
    String[] entries = readNative().split(ENERGY_STRING_DELIMITER);

    EnergySample.Builder sample =
        EnergySample.newBuilder()
            .setTimestamp(fromMicros(Long.parseLong(entries[entries.length - 1])));

    // pull out energy values
    for (int socket = 0; socket < MicroArchitecture.SOCKET_COUNT; socket++) {
      EnergyReading.Builder reading = EnergyReading.newBuilder().setSocket(socket);
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
   * Computes the forward differences of two {@link EnergySamples}, assuming that they are correctly
   * ordered and have matching sockets. Although this API guarantees that, samples from other
   * sources may misbehave when using this.
   */
  public static EnergySampleDifference difference(EnergySample first, EnergySample second) {
    // TODO: this assumes the order is good. we should be checking the timestamps
    EnergySampleDifference.Builder diff =
        EnergySampleDifference.newBuilder()
            .setStart(first.getTimestamp())
            .setEnd(second.getTimestamp());
    // TODO: this assumes the order is good. we should be checking the sockets match up
    for (int socket = 0; socket < first.getReadingCount(); socket++) {
      EnergyReading.Builder reading = EnergyReading.newBuilder();
      reading.setPackage(
          differenceWithWraparound(
              first.getReading(socket).getPackage(), second.getReading(socket).getPackage()));
      reading.setDram(
          differenceWithDramWraparound(
              first.getReading(socket).getDram(), second.getReading(socket).getDram()));
      reading.setCore(
          differenceWithWraparound(
              first.getReading(socket).getCore(), second.getReading(socket).getCore()));
      reading.setGpu(
          differenceWithWraparound(
              first.getReading(socket).getGpu(), second.getReading(socket).getGpu()));
      diff.addReading(diff);
    }

    return diff.build();
  }

  private static double differenceWithWraparound(double first, double second) {
    double energy = second - first;
    if (energy < 0) {
      energy += WRAP_AROUND;
    }
    return energy;
  }

  private static double differenceWithDramWraparound(double first, double second) {
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
  }

  private Rapl() {}

  public static void main(String[] args) throws Exception {
    System.out.println("RAPL initialized");

    System.out.println(String.format("Socket count: %d", MicroArchitecture.SOCKET_COUNT));

    if (COMPONENTS.isEmpty()) {
      System.out.println("No components found!");
      return;
    }
    System.out.println(String.format("Available components: %s", COMPONENTS.keySet()));
    System.out.println(String.format("Energy counter wrap around : %d", WRAP_AROUND));
    if (WRAP_AROUND != DRAM_WRAP_AROUND) {
      System.out.println(String.format("DRAM counter wrap around : %d", DRAM_WRAP_AROUND));
    }

    JraplUtils.poll(args, Rapl::sample, Rapl::difference);
  }
}
