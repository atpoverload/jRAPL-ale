package jrapl;

import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.function.Supplier;

final class JraplUtils {
  static void poll(
      String[] args,
      Supplier<RaplSample> sample,
      BiFunction<RaplSample, RaplSample, RaplDifference> difference)
      throws Exception {
    if (args.length == 0) {
      return;
    } else if (args.length > 2 && args[0] != "--poll") {
      System.out.println(
          String.format(
              "wrong arguments for polling: expected --poll <ms>; got %s", Arrays.toString(args)));
      return;
    }

    long pollingTime = 1000;
    if (args.length == 2) {
      pollingTime = Long.parseLong(args[1]);
    }

    RaplSample lastSample = sample.get();
    while (true) {
      Thread.sleep(pollingTime);
      RaplSample nextSample = sample.get();
      System.out.println(difference.apply(lastSample, nextSample));
      lastSample = nextSample;
    }
  }
}
