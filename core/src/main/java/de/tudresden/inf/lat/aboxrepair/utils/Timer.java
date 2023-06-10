package de.tudresden.inf.lat.aboxrepair.utils;

/**
 * @author Patrick Koopmann
 */
public class Timer {
    private double previousTime = 0.0;
    private boolean running = false;
    private long start = 0;

    /**
     * Starts timer
     */
    public void start() {
        reset();
        resume();
    }

    /**
     * Pauses timer
     */
    public void pause() {
        if(running) {
            previousTime += sinceLastStart();
        }
        running = false;
    }

    /**
     * Resumes timer
     */
    public void resume() {
        running = true;
        start = System.nanoTime();
    }

    /**
     * Resets the current timer
     */
    public void reset() {
        previousTime = 0.0;
        running = false;
    }

    /**
     * Get the total running time of the timer in seconds
     * @return A double representing the total running time
     */
    public double getTime() {
        return previousTime + sinceLastStart();
    }

    /**
     * Total time since last start
     * @return A double representing the total time since last start
     */
    private double sinceLastStart() {
        return running ? ((double) System.nanoTime() - start) / 1_000_000_000 : 0.0;
    }
}
