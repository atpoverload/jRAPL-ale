package jrapl;

/** Simple wrapper around rapl access. */
public final class MicroArchitecture {
  public static final String NAME;
  public static final int SOCKET_COUNT;

  /** Returns the name of the micro-architecture. */
  private static native String name();

  /** Returns the number of sockets on the system. */
  private static native int sockets();

  static {
    NativeLibrary.initialize();

    NAME = name();
    SOCKET_COUNT = sockets();
  }
}
