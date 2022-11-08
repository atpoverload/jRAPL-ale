package jrapl;

import java.time.Instant;

/** Simple wrapper around rapl access. */
public final class Rapl {
  /** Returns the energy of each component as a delimited string. */
  static native String read();

  public static final int SOCKET_COUNT;

  private static final String ENERGY_STRING_DELIMITER = ";";
  // indices for each component of the energy string
  // TODO -- there's a 5th possible power domain, right? like full motherboard energy or something
  private static final int COMPONENT_COUNT = 4;
  private static final int[] COMPONENT_INDICES = new int[] {-1, -1, -1, -1};

  static EnergySample parseEnergyString(String energyString) {
    double[][] energy = new double[SOCKET_COUNT][COMPONENT_COUNT];

    String[] entries = energyString.split(ENERGY_STRING_DELIMITER);
    for (int socket = 0; socket < SOCKET_COUNT; socket++) {

      for (EnergySample.Component component : EnergySample.Component.values()) {
        int index = COMPONENT_INDICES[component.ordinal()];
        if (index >= 0) {
          energy[socket][component.ordinal()] =
              Double.parseDouble(
                  entries[COMPONENT_COUNT * socket + COMPONENT_INDICES[component.ordinal()]]
                      .replace(",", ".")); // fix for localiztion issues (add the issue)
        }
      }
    }

    return new EnergySample(Instant.now(), energy);
  }

  private static native int sockets();

  private static native String componentIndices();

  static {
    NativeLibrary.initialize();

    SOCKET_COUNT = sockets();

    int index = 0;
    for (String component : componentIndices().split(",")) {
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
  }

  public static void main(String[] args) {
    System.out.println(Rapl.read());
    // for (double energy : parseEnergyString(read())) System.out.println(energy);
  }
}
