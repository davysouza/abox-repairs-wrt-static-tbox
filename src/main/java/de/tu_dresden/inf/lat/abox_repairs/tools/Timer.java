package de.tu_dresden.inf.lat.abox_repairs.tools;

/**
 * @author koopmann
 */
public class Timer {

    public static Timer newTimer(){
        return new Timer();
    }

    public static Timer dummyTimer(){
        return new Timer(){
            @Override
            public void reset() {
            }

            @Override
            public void continueTimer() {
            }

            @Override
            public void pause() {
            }
        };
    }

    private Timer(){
    }

    private double previousTime = 0.0;

    private boolean running = false;

    private long start = 0;

    public void reset(){
        previousTime=0.0;
        running=false;
    }

    public void pause(){
        if(running) {
            previousTime += sinceLastStart();
        }
        running = false;
    }

    public void startTimer(){
        reset();
        continueTimer();
    }

    private double sinceLastStart(){
        if(running)
            return ((double)System.nanoTime()-start)/1_000_000_000;
        else
            return 0.0;
    }

    public void continueTimer(){
        running = true;
        start = System.nanoTime();
    }

    public double getTime(){
        return previousTime + sinceLastStart();
    }
}
