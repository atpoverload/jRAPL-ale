package jrapl;

import static com.google.protobuf.util.Timestamps.fromMillis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.time.Instant;
import java.util.stream.Stream;
import jrapl.EnergyProtos.EnergyReading;
import jrapl.EnergyProtos.EnergySample;
import jrapl.EnergyProtos.EnergySampleDifference;

/** Simple wrapper to read powercap's energy. */
public final class Powercap {
  public static final int SOCKET_COUNT;

  private static final String POWERCAP_PATH =
      String.join("/", "/sys", "devices", "virtual", "powercap", "intel-rapl");

  /** Returns an {@link EnergySample} populated by parsing the string returned by {@ readNative}. */
  public static EnergySample sample() {
    EnergySample.Builder sample =
        EnergySample.newBuilder().setTimestamp(fromMillis(Instant.now().toEpochMilli()));

    // pull out energy values
    for (int socket = 0; socket < SOCKET_COUNT; socket++) {
      EnergyReading.Builder reading = EnergyReading.newBuilder().setSocket(socket);
      reading.setPackage(readPackage(socket));
      reading.setDram(readDram(socket));
      sample.addReading(reading);
    }

    return sample.build();
  }

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
          second.getReading(socket).getPackage() - first.getReading(socket).getPackage());
      reading.setDram(second.getReading(socket).getDram() - first.getReading(socket).getDram());
    }

    return diff.build();
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

  static {
    SOCKET_COUNT =
        (int)
            Stream.of(new File(POWERCAP_PATH).list()).filter(f -> f.contains("intel-rapl")).count();
  }

  public static void main(String[] args) throws Exception {
    System.out.println("powercap initialized");

    System.out.println(String.format("Socket count: %d", SOCKET_COUNT));

    EnergySample lastSample = sample();
    while (true) {
      Thread.sleep(1000);
      EnergySample sample = sample();
      System.out.println(difference(lastSample, sample));
      lastSample = sample;
    }
  }
}
