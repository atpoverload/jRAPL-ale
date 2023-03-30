package jrapl;

import static com.google.protobuf.util.Durations.fromMillis;
import static com.google.protobuf.util.Timestamps.EPOCH;
import static com.google.protobuf.util.Timestamps.add;
import static jrapl.JRaplUtils.SAMPLE_CSV_HEADER;
import static org.junit.Assert.assertEquals;

import java.util.List;
import org.junit.Test;

public final class JRaplUtilsTest {
  @Test
  public void samplesToCsv() {
    assertEquals(
        String.join(
            System.lineSeparator(),
            SAMPLE_CSV_HEADER,
            "0,0,0.0,0.0,0.0,0.0",
            "1000,0,1.0,2.0,3.0,4.0",
            "1000,1,0.0,0.0,0.0,0.0"),
        JRaplUtils.samplesToCsv(
            JRaplSample.newBuilder()
                .setTimestamp(EPOCH)
                .addReading(JRaplReading.getDefaultInstance())
                .build(),
            JRaplSample.newBuilder()
                .setTimestamp(add(EPOCH, fromMillis(1)))
                .addReading(
                    JRaplReading.newBuilder()
                        .setSocket(0)
                        .setPackage(1)
                        .setDram(2)
                        .setCore(3)
                        .setGpu(4))
                .addReading(JRaplReading.newBuilder().setSocket(1))
                .build()));

    assertEquals(
        String.join(System.lineSeparator(), SAMPLE_CSV_HEADER, "0,0,0.0,0.0,0.0,0.0"),
        JRaplUtils.samplesToCsv(
            List.of(
                JRaplSample.newBuilder()
                    .setTimestamp(EPOCH)
                    .addReading(JRaplReading.getDefaultInstance())
                    .build())));
  }
}
