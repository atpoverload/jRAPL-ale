package jrapl;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;

import com.google.protobuf.util.Durations;
import com.google.protobuf.util.Timestamps;
import java.util.Map;

/** A smoke test to check which components are available and if they are reporting similarly. */
final class SmokeTest {
  private static int fib(int n) {
    if (n == 0 || n == 1) {
      return 1;
    } else {
      return fib(n - 1) + fib(n - 2);
    }
  }

  private static void exercise() {
    fib(42);
  }

  private static boolean raplAvailable() throws Exception {
    if (!NativeLibrary.initialize()) {
      return false;
    }

    if (MicroArchitecture.NAME.equals(MicroArchitecture.UNKNOWN)) {
      JRaplLogger.LOGGER.info("no microarchitecture could be found through rapl!");
      return false;
    }

    if (MicroArchitecture.SOCKET_COUNT < 1) {
      JRaplLogger.LOGGER.info("microarchitecture has no energy domains through rapl!");
      return false;
    }

    if (Rapl.sample().equals(JRaplSample.getDefaultInstance())) {
      JRaplLogger.LOGGER.info("no data was produced from sampling rapl!");
      return false;
    }

    JRaplSample start = Rapl.sample();

    exercise();

    JRaplDifference diff = Rapl.difference(start, Rapl.sample());

    if (Timestamps.compare(diff.getStart(), diff.getEnd()) > 0) {
      JRaplLogger.LOGGER.info(
          String.format(
              "end timestamp (%d) is greater than start timestamp (%d)",
              Timestamps.toMicros(diff.getEnd()), Timestamps.toMicros(diff.getStart())));
      return false;
    }

    if (diff.getReadingList()
            .stream()
            .mapToDouble(r -> r.getPackage() + r.getDram() + r.getCore() + r.getGpu())
            .sum()
        == 0) {
      JRaplLogger.LOGGER.info("no energy consumed with the difference of two rapl samples!");
      return false;
    }

    JRaplLogger.LOGGER.info(
        String.join(
            System.lineSeparator(),
            String.format(
                "rapl report - microarchitecture: %s, time: %.3fs",
                MicroArchitecture.NAME,
                (double) Durations.toMicros(Timestamps.between(diff.getStart(), diff.getEnd()))
                    / 1000000),
            diff.getReadingList()
                .stream()
                .map(
                    r ->
                        String.format(
                            " - socket: %d, package: %.3fJ, dram: %.3fJ, core: %.3fJ, gpu: %.3fJ",
                            r.getSocket(), r.getPackage(), r.getDram(), r.getCore(), r.getGpu()))
                .collect(joining(System.lineSeparator()))));
    return true;
  }

  private static boolean powercapAvailable() throws Exception {
    if (Powercap.SOCKET_COUNT < 1) {
      return false;
    }

    if (Powercap.sample().equals(JRaplSample.getDefaultInstance())) {
      JRaplLogger.LOGGER.info("no data was produced from sampling powercap!");
      return false;
    }

    JRaplSample start = Powercap.sample();

    exercise();

    JRaplDifference diff = Powercap.difference(start, Powercap.sample());

    if (Timestamps.compare(diff.getStart(), diff.getEnd()) > 0) {
      JRaplLogger.LOGGER.info(
          String.format(
              "end timestamp (%d) is greater than start timestamp (%d)",
              Timestamps.toMicros(diff.getEnd()), Timestamps.toMicros(diff.getStart())));
      return false;
    }

    if (diff.getReadingList().stream().mapToDouble(r -> r.getPackage() + r.getDram()).sum() == 0) {
      JRaplLogger.LOGGER.info("no energy consumed with the difference of two powercap samples!");
      return false;
    }

    JRaplLogger.LOGGER.info(
        String.join(
            System.lineSeparator(),
            String.format(
                "powercap report - time: %.3fs",
                (double) Durations.toMicros(Timestamps.between(diff.getStart(), diff.getEnd()))
                    / 1000000),
            diff.getReadingList()
                .stream()
                .map(
                    r ->
                        String.format(
                            " - socket: %d, package: %.3fJ, dram: %.3fJ",
                            r.getSocket(), r.getPackage(), r.getDram()))
                .collect(joining(System.lineSeparator()))));
    return true;
  }

  private static boolean checkEquivalence() throws Exception {
    if (!NativeLibrary.initialize()) {
      return false;
    }

    if (Powercap.SOCKET_COUNT != MicroArchitecture.SOCKET_COUNT) {
      JRaplLogger.LOGGER.info(
          String.format(
              "energy domains for powercap (%s) does not match rapl (%d)!",
              Powercap.SOCKET_COUNT, MicroArchitecture.SOCKET_COUNT));
      return false;
    }

    JRaplSample rapl = Rapl.sample();
    JRaplSample powercap = Powercap.sample();

    exercise();

    return isSimilar(
        Rapl.difference(rapl, Rapl.sample()), Powercap.difference(powercap, Powercap.sample()));
  }

  private static boolean isSimilar(JRaplDifference rapl, JRaplDifference powercap) {
    if (Durations.toMicros(Timestamps.between(rapl.getStart(), powercap.getStart())) > 2000) {
      JRaplLogger.LOGGER.info(
          String.format(
              "powercap start time (%s) does not match rapl start time (%s)",
              powercap.getStart(), rapl.getStart()));
      return false;
    }

    if (Durations.toMicros(Timestamps.between(rapl.getEnd(), powercap.getEnd())) > 2000) {
      JRaplLogger.LOGGER.info(
          String.format(
              "powercap end time (%s) does not match rapl end time (%s)",
              powercap.getEnd(), rapl.getEnd()));
      return false;
    }

    if (powercap.getReadingCount() != rapl.getReadingCount()) {
      JRaplLogger.LOGGER.info(
          String.format(
              "powercap reading count (%s) does not match rapl reading count (%s)",
              powercap.getReadingCount(), rapl.getReadingCount()));
      return false;
    }

    Map<Integer, JRaplReading> raplReadings =
        rapl.getReadingList().stream().collect(toMap(r -> r.getSocket(), r -> r));
    Map<Integer, JRaplReading> powercapReadings =
        powercap.getReadingList().stream().collect(toMap(r -> r.getSocket(), r -> r));
    raplReadings.keySet().equals(powercapReadings.keySet());
    for (int socket : raplReadings.keySet()) {
      if (Math.abs(
              raplReadings.get(socket).getPackage() - powercapReadings.get(socket).getPackage())
          > 1) {
        JRaplLogger.LOGGER.info(
            String.format(
                "powercap package energy (%f) does not match rapl package energy (%f)",
                powercapReadings.get(socket).getPackage(), raplReadings.get(socket).getPackage()));
        return false;
      }

      if (Math.abs(raplReadings.get(socket).getDram() - powercapReadings.get(socket).getDram())
          > 1) {
        JRaplLogger.LOGGER.info(
            String.format(
                "powercap dram energy (%f) does not match rapl dram energy (%f)",
                powercapReadings.get(socket).getDram(), raplReadings.get(socket).getDram()));
        return false;
      }
    }

    JRaplLogger.LOGGER.info(
        String.join(
            System.lineSeparator(),
            String.format(
                "equivalence report - elapsed time difference: %.6fs",
                Math.abs(
                    (double)
                            (Durations.toMicros(Timestamps.between(rapl.getStart(), rapl.getEnd()))
                                - Durations.toMicros(
                                    Timestamps.between(powercap.getStart(), powercap.getEnd())))
                        / 1000000)),
            raplReadings
                .values()
                .stream()
                .map(
                    r ->
                        String.format(
                            " - socket: %dJ, package difference: %.3fJ, dram difference: %.3fJ",
                            r.getSocket(),
                            Math.abs(
                                r.getPackage() - powercapReadings.get(r.getSocket()).getPackage()),
                            Math.abs(r.getDram() - powercapReadings.get(r.getSocket()).getDram())))
                .collect(joining(System.lineSeparator()))));
    return true;
  }

  public static void main(String[] args) throws Exception {
    if ((raplAvailable() & powercapAvailable()) && checkEquivalence()) {
      JRaplLogger.LOGGER.info("all smoke tests passed!");
    } else {
      JRaplLogger.LOGGER.info("smoke testing failed; please consult the log.");
    }
  }
}
