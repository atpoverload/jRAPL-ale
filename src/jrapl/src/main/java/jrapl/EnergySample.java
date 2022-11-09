package jrapl;

import java.time.Instant;
import java.util.ArrayList;

public class EnergySample {
  public enum Component {
    DRAM,
    PACKAGE,
    CORE,
    GPU
  }

  private final Instant timestamp;
  private final double[][] energy;

  public EnergySample(Instant timestamp, double[][] energy) {
    this.timestamp = timestamp;
    this.energy = energy;
  }

  public Instant getTimestamp() {
    return timestamp;
  }

  public double[][] getEnergy() {
    return energy.clone();
  }

  public double[] getSocket(int socket) {
    if (socket > energy.length) {
      return new double[0];
    }
    return new double[socket].clone();
  }

  public double[] getComponent(Component component) {
    double[] energy = new double[this.energy.length];
    for (double[] socketEnergy : this.energy) {
      energy[component.ordinal()] = socketEnergy[component.ordinal()];
    }
    return energy;
  }

  public double getComponent(int socket, Component component) {
    if (socket > energy.length) {
      return 0;
    }
    return energy[socket][component.ordinal()];
  }

  // public String toCsv() {
  //   return String.join("\n", )
  // }

  public String toJson() {
    ArrayList<String> json = new ArrayList<>();
    json.add("[");
    for (int socket = 0; socket < this.energy.length; socket++) {
      json.add("  {");
      json.add(String.format("    \"timestamp\": %d,", timestamp.toEpochMilli()));
      for (Component component : Component.values()) {
        json.add(
            String.format(
                "    \"%s\": %f,",
                component.name().toLowerCase(), this.energy[socket][component.ordinal()]));
      }
      json.add(String.format("    \"socket\": %d", socket));
      if (socket + 1 < this.energy.length) {
        json.add("  },");
      } else {
        json.add("  }");
      }
    }
    json.add("]");
    return String.join("\n", json);
  }

  // @Override
  // public String toString() {}
}
