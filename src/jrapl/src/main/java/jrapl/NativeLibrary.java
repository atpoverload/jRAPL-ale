package jrapl;

/**
 * Simple wrapper around the native backend management. You should never need to access this; the
 * other classes that require the library should handle accessing the class for you.
 */
final class NativeLibrary {
  private static boolean IS_INITIALIZED = false;

  /** Synchronized helper for {@code initializeNative}. */
  static void initialize() {
    synchronized (NativeLibrary.class) {
      if (!IS_INITIALIZED) {
        IS_INITIALIZED = true;
        initializeNative();
      }
    }
  }

  /** Synchronized helper for {@code deallocateNative}. */
  static void deallocate() {
    synchronized (NativeLibrary.class) {
      if (IS_INITIALIZED) {
        IS_INITIALIZED = false;
        deallocateNative();
      }
    }
  }

  /** Initializes a reference to the rapl registers. Must call before doing anything else. */
  private static native void initializeNative();

  /** Deallocates the reference to the rapl registers. Should call when done. */
  public static native void deallocateNative();

  static {
    System.loadLibrary("jrapl");
  }

  private NativeLibrary() {}
}
