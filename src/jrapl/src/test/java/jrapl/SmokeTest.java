package jrapl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SmokeTest {
  @Test
  public void raplAvailable() {
    assertTrue(NativeLibrary.initialize());

    assertNotEquals(MicroArchitecture.NAME, MicroArchitecture.UNKNOWN);
    assertTrue(MicroArchitecture.SOCKET_COUNT > 0);

    assertNotEquals(Rapl.sample(), RaplSample.getDefaultInstance());
  }

  @Test
  public void powercapAvailable() {
    assertTrue(Powercap.SOCKET_COUNT > 0);
    assertNotEquals(Powercap.sample(), RaplSample.getDefaultInstance());
  }

  @Test
  public void raplPowercapEquivalence() throws Exception {
    if (!NativeLibrary.initialize()) {
      assertTrue(false);
    }

    assertEquals(MicroArchitecture.SOCKET_COUNT, Powercap.SOCKET_COUNT);
    RaplSample rapl = Rapl.sample();
    RaplSample powercap = Powercap.sample();

    Thread.sleep(1000);

    assertSimilar(
        Rapl.difference(rapl, Rapl.sample()), Powercap.difference(powercap, Powercap.sample()));
  }

  private static void assertSimilar(RaplDifference rapl, RaplDifference powercap) {
    assertInThreshold(rapl.getStart().getSeconds(), powercap.getStart().getSeconds(), 0);
    assertInThreshold(rapl.getStart().getNanos(), powercap.getStart().getNanos(), 10000000);

    assertInThreshold(rapl.getEnd().getSeconds(), powercap.getEnd().getSeconds(), 0);
    assertInThreshold(rapl.getEnd().getNanos(), powercap.getEnd().getNanos(), 10000000);

    assertEquals(rapl.getReadingCount(), powercap.getReadingCount());

    for (int socket = 0; socket < rapl.getReadingCount(); socket++) {
      RaplReading raplReading = rapl.getReading(socket);
      RaplReading powercapReading = powercap.getReading(socket);

      assertEquals(raplReading.getSocket(), powercapReading.getSocket());
      assertInThreshold(raplReading.getPackage(), powercapReading.getPackage(), 0.000001);
      assertInThreshold(raplReading.getDram(), powercapReading.getDram(), 0.000001);
    }
  }

  private static void assertInThreshold(long first, long second, long threshold) {
    assertTrue(Math.abs(first - second) <= threshold);
  }

  private static void assertInThreshold(double first, double second, double threshold) {
    assertTrue(Math.abs(first - second) <= threshold);
  }
}
