package jrapl;

import static com.google.protobuf.util.Timestamps.toMicros;
import static java.util.stream.Collectors.joining;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

public final class JRaplUtils {
  static final String SAMPLE_CSV_HEADER = "timestamp_us,socket,package,dram,core,gpu";
  static final String DIFFERENCE_CSV_HEADER = "start_us,end_us,socket,package,dram,core,gpu";

  /** Converts samples to a csv. */
  public static String samplesToCsv(JRaplSample... samples) {
    return samplesToCsv(Arrays.stream(samples));
  }

  /** Converts a {@link List} of samples to a csv. */
  public static String samplesToCsv(List<JRaplSample> samples) {
    return samplesToCsv(samples.stream());
  }

  /** Converts difference to a csv. */
  public static String diffsToCsv(JRaplDifference... differences) {
    return diffsToCsv(Arrays.stream(differences));
  }

  /** Converts a {@link List} of differences to a csv. */
  public static String diffsToCsv(List<JRaplDifference> differences) {
    return diffsToCsv(differences.stream());
  }

  private static String samplesToCsv(Stream<JRaplSample> samples) {
    return String.join(
        System.lineSeparator(),
        SAMPLE_CSV_HEADER,
        samples.map(s -> toCsv(s)).collect(joining(System.lineSeparator())));
  }

  private static String diffsToCsv(Stream<JRaplDifference> differences) {
    return String.join(
        System.lineSeparator(),
        DIFFERENCE_CSV_HEADER,
        differences.map(d -> toCsv(d)).collect(joining(System.lineSeparator())));
  }

  private static String toCsv(JRaplSample sample) {
    final long timestamp = toMicros(sample.getTimestamp());
    return toCsv(
        sample.getReadingList().stream(), r -> String.format("%d,%s", timestamp, toCsv(r)));
  }

  private static String toCsv(JRaplDifference difference) {
    final long start = toMicros(difference.getStart());
    final long end = toMicros(difference.getEnd());
    return toCsv(
        difference.getReadingList().stream(), r -> String.format("%d,%d,%s", start, end, toCsv(r)));
  }

  private static String toCsv(JRaplReading reading) {
    return String.join(
        ",",
        Integer.toString(reading.getSocket()),
        Double.toString(reading.getPackage()),
        Double.toString(reading.getDram()),
        Double.toString(reading.getCore()),
        Double.toString(reading.getGpu()));
  }

  private static <T> String toCsv(Stream<T> t, Function<T, String> converter) {
    return t.map(converter::apply).collect(joining(System.lineSeparator()));
  }

  private JRaplUtils() {}
}
