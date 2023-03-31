package jrapl;

import static com.google.protobuf.util.Timestamps.fromMillis;
import static java.util.stream.Collectors.toMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Stream;

/** Simple wrapper to read powercap's energy with pure Java. */
public final class Powercap {
  private static final String POWERCAP_PATH =
      String.join("/", "/sys", "devices", "virtual", "powercap", "intel-rapl");

  public static final int SOCKET_COUNT = getSocketCount();

  /** Returns an {@link JRaplSample} populated by parsing the string returned by {@ readNative}. */
  public static JRaplSample sample() {
    if (SOCKET_COUNT == 0) {
      return JRaplSample.getDefaultInstance();
    }
    JRaplSample.Builder sample =
        JRaplSample.newBuilder()
            .setSource(JRaplSource.POWERCAP)
            .setTimestamp(fromMillis(Instant.now().toEpochMilli()));

    // pull out energy values
    for (int socket = 0; socket < SOCKET_COUNT; socket++) {
      JRaplReading.Builder reading = JRaplReading.newBuilder().setSocket(socket);
      reading.setPackage(readPackage(socket));
      reading.setDram(readDram(socket));
      sample.addReading(reading);
    }

    return sample.build();
  }

  /**
   * Computes the forward differences of two {@link JRaplSamples}, assuming that they are correctly
   * ordered and have matching sockets. Although this API guarantees that, samples from other
   * sources may misbehave when using this.
   */
  public static JRaplDifference difference(JRaplSample first, JRaplSample second) {
    // TODO: this assumes the order is good. we should be checking the timestamps
    JRaplDifference.Builder diff =
        JRaplDifference.newBuilder().setStart(first.getTimestamp()).setEnd(second.getTimestamp());
    // TODO: this assumes the order is good. we should be checking the sockets match
    // up
    Map<Integer, JRaplReading> firstReadings =
        first.getReadingList().stream().collect(toMap(r -> r.getSocket(), r -> r));
    Map<Integer, JRaplReading> secondReadings =
        second.getReadingList().stream().collect(toMap(r -> r.getSocket(), r -> r));
    for (int socket : firstReadings.keySet()) {
      diff.addReading(
          JRaplReading.newBuilder()
              .setSocket(socket)
              .setPackage(
                  secondReadings.get(socket).getPackage() - firstReadings.get(socket).getPackage())
              .setDram(secondReadings.get(socket).getDram() - firstReadings.get(socket).getDram()));
    }

    return diff.build();
  }

  private static int getSocketCount() {
    try {
      return (int)
          Stream.of(new File(POWERCAP_PATH).list()).filter(f -> f.contains("intel-rapl")).count();
    } catch (Exception e) {
      JRaplLogger.LOGGER.info("couldn't check the socket count; powercap likely not available");
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
            "/", POWERCAP_PATH, socketPrefix, String.format("%s:0", socketPrefix), "energy_uj");
    try (BufferedReader reader = new BufferedReader(new FileReader(energyFile))) {
      return Double.parseDouble(reader.readLine()) / 1000000;
    } catch (Exception e) {
      return 0;
    }
  }
}
