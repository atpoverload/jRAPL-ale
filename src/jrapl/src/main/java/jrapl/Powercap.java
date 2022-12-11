package jrapl;

import static com.google.protobuf.util.Timestamps.fromMillis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.time.Instant;
import java.util.stream.Stream;

/** Simple wrapper to read powercap's energy. */
public final class Powercap {
  private static final String POWERCAP_PATH =
      String.join("/", "/sys", "devices", "virtual", "powercap", "intel-rapl");

  public static final int SOCKET_COUNT = getSocketCount();

  /** Returns an {@link RaplSample} populated by parsing the string returned by {@ readNative}. */
  public static RaplSample sample() {
    if (SOCKET_COUNT == 0) {
      JraplUtils.LOGGER.info("couldn't check the socket count; powercap likely not available");
      return RaplSample.getDefaultInstance();
    }
    RaplSample.Builder sample =
        RaplSample.newBuilder().setTimestamp(fromMillis(Instant.now().toEpochMilli()));

    // pull out energy values
    for (int socket = 0; socket < SOCKET_COUNT; socket++) {
      RaplReading.Builder reading = RaplReading.newBuilder().setSocket(socket + 1);
      reading.setPackage(readPackage(socket));
      reading.setDram(readDram(socket));
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
                  second.getReading(socket).getPackage() - first.getReading(socket).getPackage())
              .setDram(second.getReading(socket).getDram() - first.getReading(socket).getDram()));
    }

    return diff.build();
  }

  private static int getSocketCount() {
    try {
      return (int)
          Stream.of(new File(POWERCAP_PATH).list()).filter(f -> f.contains("intel-rapl")).count();
    } catch (Exception e) {
      JraplUtils.LOGGER.info("couldn't check the socket count; powercap likely not available");
      return 0;
    }
  }

  /**
   * Parses the contents of /sys/devices/virtual/powercap/intel-rapl/intel-rapl:<socket>/energy_uj,
   * which contains the number of microjoules consumed by the package since boot as an integer.
   */
  private static double readPackage(int socket) {
    String energyFile =
        String.join("/", POWERCAP_PATH, String.format("intel-rapl:%d", socket), "energy_uj");
    try (BufferedReader reader = new BufferedReader(new FileReader(energyFile))) {
      return Double.parseDouble(reader.readLine()) / 1000000;
    } catch (Exception e) {
      return 0;
    }
  }

  /**
   * Parses the contents of
   *
   * <p>/sys/devices/virtual/powercap/intel-rapl/intel-rapl:<socket>/intel-rapl:<socket>:0/energy_uj,
   * which contains the number of microjoules consumed by the dram since boot as an integer.
   */
  private static double readDram(int socket) {
    String socketPrefix = String.format("intel-rapl:%d", socket);
    String energyFile =
        String.join(
            "/",
            POWERCAP_PATH,
            socketPrefix,
            String.format("%s:%d", socketPrefix, socket),
            "energy_uj");
    try (BufferedReader reader = new BufferedReader(new FileReader(energyFile))) {
      return Double.parseDouble(reader.readLine()) / 1000000;
    } catch (Exception e) {
      return 0;
    }
  }

  private Powercap() {}

  public static void main(String[] args) throws Exception {
    System.out.println("powercap initialized");

    System.out.println(String.format("Socket count: %d", SOCKET_COUNT));

    JraplUtils.poll(args, Powercap::sample, Powercap::difference);
  }
}
