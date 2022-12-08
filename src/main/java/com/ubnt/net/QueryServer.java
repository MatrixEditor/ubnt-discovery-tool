package com.ubnt.net; //@date 06.12.2022

import javax.swing.event.EventListenerList;
import java.io.IOException;

/**
 * The {@code QueryServer} is used to create an abstract layer for
 * {@link IDiscoveryServer} classes. It contains an {@code EventListenerList}
 * to store {@code EventListener} objects based on their base class.
 * <p>
 * Usually, objects of this type will be executed on an extra {@link Thread}
 * to prevent blocking the main application thread.
 *
 * @see IDiscoveryServer
 */
public abstract class QueryServer implements Runnable {

    /**
     * The configured name of this runnable task,
     */
    private final String name;

    /**
     * The {@link EventListenerList} holding all listeners that can listen
     * on this task.
     */
    protected EventListenerList listenerList = new EventListenerList();

    /**
     * Creates a new {@link QueryServer} with the given identifier.
     *
     * @param name the name of this service.
     */
    protected QueryServer(String name) {
        this.name = name;
    }

    /**
     * Tells the caller whether this {@link QueryServer} has finished
     * executing its statements.
     *
     * @return whether this object has done its job
     */
    public abstract boolean isFinished();

    /**
     * Finished this runnable task. (has the same effect as the {@code join()}
     * takes in {@link Thread}).
     */
    public abstract void doFinish();

    /**
     * Returns the name identifier for this task.
     *
     * @return the name
     */
    public /*@NotNull*/ String getName() {
        return this.name;
    }

    /**
     * Starts this task
     *
     * @see Thread#run()
     */
    @Override
    public abstract void run();

    /**
     * Marks this thread as either a daemon thread or a user thread.
     *
     * @return whether the executor should create a daemon {@link Thread}.
     * @see Thread#setDaemon(boolean)
     */
    public abstract boolean isDaemon();

    /**
     * Sends all packets that were cached.
     */
    public abstract void sendAll() throws IOException;

    /**
     * Clears all discovered services and cached packets.
     *
     * @throws IOException if an I/O error occurs
     */
    public abstract void clearAll() throws IOException;

    /**
     * Adds the given listener to the list.
     *
     * @param listener the listener to add
     */
    public void addListener(IDiscoveryListener listener) {
        if (listener != null) {
            listenerList.add(IDiscoveryListener.class, listener);
        }
    }

    /**
     * Removes the given {@link IDiscoveryListener}.
     *
     * @param listener the listener to be removed
     */
    public void removeListener(IDiscoveryListener listener) {
        listenerList.remove(IDiscoveryListener.class, listener);
    }

    /**
     * Notifies all stored {@link IDiscoveryListener}s that a new service
     * has been found.
     *
     * @param service the service that has been discovered
     */
    public synchronized void fireOnServiceDiscovered(IUbntService service) {
        for (IDiscoveryListener listener : listenerList.getListeners(IDiscoveryListener.class)) {
            listener.onServiceLocated(service);
        }
    }

}
