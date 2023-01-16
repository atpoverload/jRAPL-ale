package jrapl;

import static java.util.stream.Collectors.toMap;

import com.google.protobuf.util.Durations;
import com.google.protobuf.util.Timestamps;
import java.util.Map;

/**
 * A smoke test to check which components are available and if they are
 * reporting similarly.
 */
final class SmokeTest {
  private static boolean raplAvailable() {
    if (!NativeLibrary.initialize()) {
      return false;
    }

    if (MicroArchitecture.NAME.equals(MicroArchitecture.UNKNOWN)) {
      JraplUtils.LOGGER.info("no microarchitecture could be found through rapl!");
      return false;
    }

    if (MicroArchitecture.SOCKET_COUNT < 1) {
      JraplUtils.LOGGER.info("microarchitecture has no energy domains through rapl!");
      return false;
    }

    if (Rapl.sample().equals(RaplSample.getDefaultInstance())) {
      JraplUtils.LOGGER.info("no data was produced from sampling rapl!");
      return false;
    }

    return true;
  }

  private static boolean powercapAvailable() {
    if (Powercap.SOCKET_COUNT < 1) {
      return false;
    }

    if (Powercap.sample().equals(RaplSample.getDefaultInstance())) {
      JraplUtils.LOGGER.info("no data was produced from sampling powercap!");
      return false;
    }

    return true;
  }

  private static boolean checkEquivalence() throws Exception {
    if (!NativeLibrary.initialize()) {
      return false;
    }

    if (Powercap.SOCKET_COUNT != MicroArchitecture.SOCKET_COUNT) {
      JraplUtils.LOGGER.info(
          String.format(
              "energy domains for powercap (%s) does not match rapl (%d)!",
              Powercap.SOCKET_COUNT, MicroArchitecture.SOCKET_COUNT));
      return false;
    }

    RaplSample rapl = Rapl.sample();
    RaplSample powercap = Powercap.sample();

    Thread.sleep(1000);

    return isSimilar(
        Rapl.difference(rapl, Rapl.sample()), Powercap.difference(powercap, Powercap.sample()));
  }

  private static boolean isSimilar(RaplDifference rapl, RaplDifference powercap) {
    if (Durations.toMicros(Timestamps.between(rapl.getStart(), powercap.getStart())) > 1000000) {
      JraplUtils.LOGGER.info(
          String.format(
              "powercap start time (%s) does not match rapl start time (%s)",
              powercap.getStart(), rapl.getStart()));
      return false;
    }

    if (Durations.toMicros(Timestamps.between(rapl.getEnd(), powercap.getEnd())) > 1000000) {
      JraplUtils.LOGGER.info(
          String.format(
              "powercap end time (%s) does not match rapl end time (%s)",
              powercap.getEnd(), rapl.getEnd()));
      return false;
    }

    if (powercap.getReadingCount() != rapl.getReadingCount()) {
      JraplUtils.LOGGER.info(
          String.format(
              "powercap reading count (%s) does not match rapl reading count (%s)",
              powercap.getReadingCount(), rapl.getReadingCount()));
      return false;
    }

    Map<Integer, RaplReading> raplReadings = rapl.getReadingList().stream().collect(toMap(r -> r.getSocket(), r -> r));
    Map<Integer, RaplReading> powercapReadings = powercap.getReadingList().stream()
        .collect(toMap(r -> r.getSocket(), r -> r));
    for (int socket : raplReadings.keySet()) {
      if (Math.abs(
          raplReadings.get(socket).getPackage() - powercapReadings.get(socket).getPackage()) < 0.000001) {
        JraplUtils.LOGGER.info(
            String.format(
                "powercap package energy (%f) does not match rapl package energy (%f)",
                powercapReadings.get(socket).getPackage(), raplReadings.get(socket).getPackage()));
        return false;
      }

      if (Math.abs(raplReadings.get(socket).getDram() - powercapReadings.get(socket).getDram()) < 0.000001) {
        JraplUtils.LOGGER.info(
            String.format(
                "powercap dram energy (%f) does not match rapl dram energy (%f)",
                powercapReadings.get(socket).getDram(), raplReadings.get(socket).getDram()));
        return false;
      }
    }

    return true;
  }

  public static void main(String[] args) throws Exception {
    if ((raplAvailable() & powercapAvailable()) && checkEquivalence()) {
      JraplUtils.LOGGER.info("all smoke tests passed!");
    } else {
      JraplUtils.LOGGER.info("smoke testing failed; please consult the log.");
    }
  }
}
