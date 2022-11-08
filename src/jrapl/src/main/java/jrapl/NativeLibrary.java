package jrapl;

/** Simple wrapper around the native backend management. */
final class NativeLibrary {
  private static boolean IS_INITIALIZED = false;

  static void initialize() {
    synchronized (NativeLibrary.class) {
      if (!IS_INITIALIZED) {
        IS_INITIALIZED = true;
        initializeNative();
      }
    }
  }

  static void deallocate() {
    synchronized (NativeLibrary.class) {
      if (IS_INITIALIZED) {
        IS_INITIALIZED = false;
        initializeNative();
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
}
