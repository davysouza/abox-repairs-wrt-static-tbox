package de.tudresden.inf.lat.aboxrepair.utils;

/**
 * Prints a message into System.out with some customization
 * @author Davy Souza
 */
public class PrettyMessage {
    private static String StartInfo= "\033[0;34m";
    private static String StartWarning = "\033[0;33m";
    private static String StartSuccess = "\033[0;32m";
    private static String StartError = "\033[0;31m";
    private static String End = "\033[0m";

    /**
     * Prints an <b>info</b> message into System.out
     * @param message message to be printed
     */
    public static void printInfo(String message) {
        System.out.println(StartInfo + message + End);
    }

    /**
     * Prints a <b>warning</b> message into System.out
     * @param message message to be printed
     */
    public static void printWarning(String message) {
        System.out.println(StartWarning + message + End);
    }

    /**
     * Prints a <b>success</b> message into System.out
     * @param message message to be printed
     */
    public static void printSuccess(String message) {
        System.out.println(StartSuccess + message + End);
    }

    /**
     * Prints an <b>error</b> message into System.out
     * @param message message to be printed
     */
    public static void printError(String message) {
        System.out.println(StartError + message + End);
    }
}
