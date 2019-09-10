package org.timux.ports;

/**
 * Defines constants for the synchronization level of asynchronous components. The
 * synchronization level determines how an asynchronous component handles concurrent
 * IN port signals. The possible values are {@link SyncLevel#NONE},
 * {@link SyncLevel#COMPONENT} (default), and {@link SyncLevel#PORT}.
 *
 * @see Async
 *
 * @author Tim Rohlfs
 * @since 0.4.0
 */
public class SyncLevel {

    /**
     * Disables synchronization. DO NOT do this except you know exactly what you are doing.
     * In particular, the component must not manipulate any state in its port handlers.
     */
    public final static int NONE = 0;

    /**
     * Synchronizes on the component level, i.e. the entire component is locked while a
     * port handler is active. This is the default setting and the ONLY ONE that is safe in every
     * situation. You SHOULD NOT change this except you know exactly what you are doing.
     */
    public final static int COMPONENT = 1;

    /**
     * Synchronizes on the port level, i.e. while a port handler is active, only that particular
     * port is locked, but other ports remain unlocked. This setting can lead to
     * race conditions if you are not very careful with state manipulations. It is recommended to
     * use the {@link SyncLevel#COMPONENT} setting in all situations (which is the default).
     */
    public final static int PORT = 2;
}
