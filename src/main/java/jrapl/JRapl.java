package jrapl;

public final class JRapl {
  public static native void initialize();

  public static native void deallocate();

  public static native String readEnergyStats();

  static {
    System.loadLibrary("libJrapl.so");
  }
}
