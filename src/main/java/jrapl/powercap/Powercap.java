package jrapl.powercap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.stream.Stream;

public final class Powercap {
  private static final String POWERCAP_PATH =
      String.join("/", "sys", "devices", "virtual", "powercap", "intel-rapl");

  private static final int DOMAINS;

  private static double readComponent(int domainIndex, int componentIndex) {
    String domain = String.format("intel-rapl:%i", domainIndex);
    String component = String.format("%s:%i", componentIndex);
    try {
      BufferedReader reader =
          new BufferedReader(
              new FileReader(String.join("/", POWERCAP_PATH, domain, component, "energy_uj")));
      double energy = Double.parseDouble(reader.readLine());
      reader.close();
      return energy;
    } catch (Exception e) {
      return 0;
    }
  }

  public static double[] readEnergyStats() {
    double[] energy = new double[4 * DOMAINS];
    for (int i = 0; i < DOMAINS; i++) {
      energy[2 * i] = readComponent(i, 0);
      energy[2 * i + 1] = readComponent(i, 1);
    }
    return energy;
  }

  static {
    DOMAINS =
        (int)
            Stream.of(new File(POWERCAP_PATH).list()).filter(f -> f.contains("intel-rapl")).count();
  }
}
