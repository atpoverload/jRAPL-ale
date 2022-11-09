package jrapl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.stream.Stream;

/** Unsafe wrapper to read powercap's energy. */
public final class Powercap {
  private static final String POWERCAP_PATH =
      String.join("/", "sys", "devices", "virtual", "powercap", "intel-rapl");

  private static final int SOCKETS;

  private static double readComponent(int socketIndex, int componentIndex) {
    String socket = String.format("intel-rapl:%i", socketIndex);
    String component = String.format("%s:%i", componentIndex);
    try {
      BufferedReader reader =
          new BufferedReader(
              new FileReader(String.join("/", POWERCAP_PATH, socket, component, "energy_uj")));
      double energy = Double.parseDouble(reader.readLine());
      reader.close();
      return energy;
    } catch (Exception e) {
      return 0;
    }
  }

  public static double[] read() {
    double[] energy = new double[4 * SOCKETS];
    for (int i = 0; i < SOCKETS; i++) {
      energy[2 * i] = readComponent(i, 0);
      energy[2 * i + 1] = readComponent(i, 1);
    }
    return energy;
  }

  static {
    SOCKETS =
        (int)
            Stream.of(new File(POWERCAP_PATH).list()).filter(f -> f.contains("intel-rapl")).count();
  }

  public static void main(String[] args) {
    for (double energy : Powercap.read()) System.out.println(energy);
  }
}
