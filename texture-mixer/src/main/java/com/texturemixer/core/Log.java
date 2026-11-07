package com.texturemixer.core;

/** Minimaler Logger ohne Abhängigkeiten (damit der Kern auch ohne Minecraft testbar ist). */
public final class Log {
    private static final System.Logger L = System.getLogger("TextureMixer");
    private Log() {}
    public static void info(String m) { L.log(System.Logger.Level.INFO, m); }
    public static void warn(String m) { L.log(System.Logger.Level.WARNING, m); }
    public static void warn(String m, Throwable t) { L.log(System.Logger.Level.WARNING, m, t); }
}
