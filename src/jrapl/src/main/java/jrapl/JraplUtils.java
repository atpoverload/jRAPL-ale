package jrapl;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

final class JraplUtils {
  static Logger LOGGER = getLogger();

  /** Helper method that implements the left fold operation. */
  static <T, R> List<R> foldLeft(Iterable<T> iterable, BiFunction<T, T, R> func, T start) {
    ArrayList<R> diffs = new ArrayList<>();
    T previous = start;
    for (T element : iterable) {
      if (!previous.equals(start)) {
        diffs.add(func.apply(previous, element));
      }
      previous = element;
    }
    return diffs;
  }

  private static final SimpleDateFormat dateFormatter =
      new SimpleDateFormat("yyyy-MM-dd HH:mm:ss a z");

  private static String makePrefix(Date date) {
    return String.join(
        " ",
        "jrapl",
        "(" + dateFormatter.format(date) + ")",
        "[" + Thread.currentThread().getName() + "]:");
  }

  private static Logger getLogger() {
    ConsoleHandler handler = new ConsoleHandler();
    handler.setFormatter(
        new Formatter() {
          @Override
          public String format(LogRecord record) {
            return String.join(
                " ",
                makePrefix(new Date(record.getMillis())),
                record.getMessage(),
                System.lineSeparator());
          }
        });

    Logger logger = Logger.getLogger("jrapl");
    logger.setUseParentHandlers(false);

    for (Handler hdlr : logger.getHandlers()) {
      logger.removeHandler(hdlr);
    }
    logger.addHandler(handler);

    return logger;
  }
}
