package jrapl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.time.Instant;
import java.util.stream.Stream;

/** Simple wrapper to read powercap's energy. */
public final class Powercap {
  public static final int SOCKET_COUNT;

  private static final String POWERCAP_PATH =
      String.join("/", "/sys", "devices", "virtual", "powercap", "intel-rapl");

  /** Returns an {@link EnergySample} populated by parsing the string returned by {@ readNative}. */
  public static EnergySample sample() {
    double[][] energy = new double[SOCKET_COUNT][EnergySample.Component.values().length];

    for (int socket = 0; socket < SOCKET_COUNT; socket++) {
      energy[socket][EnergySample.Component.PACKAGE.ordinal()] = readPackage(socket);
      energy[socket][EnergySample.Component.DRAM.ordinal()] = readDram(socket);
    }

    return new EnergySample(Instant.now(), energy);
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
   * /sys/devices/virtual/powercap/intel-rapl/intel-rapl:<socket>/intel-rapl:<socket>:0/energy_uj,
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

    EnergySample lastSample;
    while (true) {
      EnergySample sample = sample();
      System.out.println(sample.toJson());
      lastSample = sample;
      Thread.sleep(1000);
    }
  }
}
