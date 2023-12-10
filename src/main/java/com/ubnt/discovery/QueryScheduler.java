package com.ubnt.discovery; //@date 06.12.2022

import com.ubnt.net.QueryServer;

/**
 * {@code QueryScheduler} objects are used to schedule the message-sending
 * of query-packets. Usually, this {@link Runnable} is executed on a new daemon
 * thread to ensure that it is running as a background process.
 *
 * @see UbntDiscoveryTool#scheduleScan()
 */
public class QueryScheduler implements Runnable {

    /**
     * The milliseconds to sleep.
     */
    private final long sleepTimeMillis;

    /**
     * The ratio of this scheduler that specifies the amount of
     * loops to run.
     */
    private final int ratio;

    /**
     * A variable storing whether this scheduler is currently
     * running.
     */
    private volatile boolean running;

    private ScheduleListener listener;

    /**
     * Creates a new default {@link QueryScheduler}.
     */
    public QueryScheduler() {
        this(5000L, 5, null);
    }

    /**
     * Creates a new {@link QueryScheduler}.
     *
     * @param sleepTimeMillis the milliseconds to sleep
     * @param ratio see {@link #ratio}
     */
    public QueryScheduler(long sleepTimeMillis, int ratio, ScheduleListener scheduleListener) {
        this.sleepTimeMillis = sleepTimeMillis;
        this.ratio           = ratio;
        this.running         = false;
        setListener(scheduleListener);
    }

    /**
     * {@link ScheduleListener} objects are used to listen on every second of
     * this timer.
     *
     * @implSpec counting backwards
     */
    public interface ScheduleListener {
        /**
         * Invoked when a second has expired.
         *
         * @param finished whether the {@link QueryScheduler} has finished
         * @param second the current second
         */
        void nextSecond(boolean finished, long second);
    }

    /**
     * Cancels this task.
     */
    public synchronized void cancel() {
        this.running = false;
    }

    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * <p>
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    @Override
    public synchronized void run() {
        long time = 100L;
        synchronized (this) {
            long sleepTime = sleepTimeMillis / ratio;
            if (sleepTime >= 100) {
                time = sleepTime;
            }

            running = true;
            long seconds = time * ratio;
            sendAll();
            for (int i = 0; i < ratio && running; i++) {
                long second = seconds - ((long) i * time);
                try {
                    // TEST: using the yield() method
                    Thread.yield();

                    //noinspection BusyWait
                    Thread.sleep(time);
                    Thread.yield();
                    if (listener != null) {
                        listener.nextSecond(!running, second);
                    }
                } catch (InterruptedException e) {
                    // ignored exception: logger would be unnecessary
                }
            }
            running = false;
            if (listener != null) {
                listener.nextSecond(true, 0L);
            }

            for (QueryServer server : UbntDiscoveryTool.getServers()) {
                server.doFinish();
            }
        }
    }

    /**
     * Adds the given {@link ScheduleListener}-
     *
     * @param listener the listener to use
     */
    public void setListener(ScheduleListener listener) {
        this.listener = listener;
    }

    /**
     * Sends all query packets and starts all servers.
     */
    private void sendAll() {
        UbntDiscoveryTool.queryAndStart();
    }

    public boolean isRunning() {
        return running;
    }
}
