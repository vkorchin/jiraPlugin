package com.eventstream.stream;

import com.eventstream.model.Event;
import java.util.function.Consumer;

/**
 * Interface for event stream sources
 */
public interface EventStream extends AutoCloseable {
    
    /**
     * Subscribe to event stream with a consumer
     * @param eventConsumer consumer to process events
     */
    void subscribe(Consumer<Event> eventConsumer);
    
    /**
     * Start the event stream
     */
    void start();
    
    /**
     * Stop the event stream
     */
    void stop();
    
    /**
     * Check if the stream is running
     */
    boolean isRunning();
    
    /**
     * Get the total number of events processed
     */
    long getProcessedEventCount();
}