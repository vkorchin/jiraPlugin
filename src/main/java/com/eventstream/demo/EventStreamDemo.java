package com.eventstream.demo;

import com.eventstream.handler.EventHandlerRegistry;
import com.eventstream.handler.impl.CreateEntityHandler;
import com.eventstream.handler.impl.UpdateEntityHandler;
import com.eventstream.model.EntityState;
import com.eventstream.processor.EventProcessor;
import com.eventstream.rollback.RollbackService;
import com.eventstream.snapshot.SnapshotManager;
import com.eventstream.state.StateManager;
import com.eventstream.stream.InMemoryEventStream;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Demonstration application showing event stream processing with rollback capabilities
 */
public class EventStreamDemo {
    private static final Logger logger = Logger.getLogger(EventStreamDemo.class.getName());

    public static void main(String[] args) {
        EventStreamDemo demo = new EventStreamDemo();
        demo.runDemo();
    }

    public void runDemo() {
        logger.info("=== Event Stream Processing Demo ===");

        // Initialize all components
        InMemoryEventStream eventStream = new InMemoryEventStream(1000);
        StateManager stateManager = new StateManager();
        SnapshotManager snapshotManager = new SnapshotManager(50); // Max 50 snapshots per entity
        EventHandlerRegistry handlerRegistry = new EventHandlerRegistry();
        
        // Register event handlers
        handlerRegistry.registerHandler(new CreateEntityHandler());
        handlerRegistry.registerHandler(new UpdateEntityHandler());
        
        // Create event processor with snapshot every 5 events
        EventProcessor eventProcessor = new EventProcessor(
            eventStream, stateManager, snapshotManager, handlerRegistry, 5, 1000);
        
        // Create rollback service
        RollbackService rollbackService = new RollbackService(
            stateManager, snapshotManager, handlerRegistry, eventProcessor);

        try {
            // Start the system
            eventProcessor.start();
            
            // Demo 1: Basic event processing
            demonstrateBasicEventProcessing(eventStream);
            
            // Demo 2: State rollback
            demonstrateStateRollback(eventStream, rollbackService);
            
            // Demo 3: Performance test
            demonstratePerformance(eventStream, eventProcessor, rollbackService);
            
            // Demo 4: Statistics and monitoring
            demonstrateMonitoring(eventProcessor, rollbackService);
            
        } finally {
            // Cleanup
            eventProcessor.stop();
            rollbackService.shutdown();
            try {
                eventStream.close();
            } catch (Exception e) {
                logger.warning("Error closing event stream: " + e.getMessage());
            }
        }
        
        logger.info("=== Demo completed ===");
    }

    private void demonstrateBasicEventProcessing(InMemoryEventStream eventStream) {
        logger.info("\n--- Demo 1: Basic Event Processing ---");
        
        // Create some entities
        Map<String, Object> userPayload = new HashMap<>();
        userPayload.put("name", "John Doe");
        userPayload.put("email", "john@example.com");
        userPayload.put("age", 30);
        
        eventStream.addEvent("user-1", "ENTITY_CREATED", userPayload);
        
        // Update the entity
        Map<String, Object> updatePayload = new HashMap<>();
        updatePayload.put("age", 31);
        updatePayload.put("lastLogin", Instant.now().toString());
        
        eventStream.addEvent("user-1", "ENTITY_UPDATED", updatePayload);
        
        // Create another entity
        Map<String, Object> orderPayload = new HashMap<>();
        orderPayload.put("customerId", "user-1");
        orderPayload.put("amount", 99.99);
        orderPayload.put("status", "pending");
        
        eventStream.addEvent("order-1", "ENTITY_CREATED", orderPayload);
        
        // Wait for processing
        sleep(1000);
        
        logger.info("Basic event processing completed");
    }

    private void demonstrateStateRollback(InMemoryEventStream eventStream, RollbackService rollbackService) {
        logger.info("\n--- Demo 2: State Rollback ---");
        
        String entityId = "user-rollback-test";
        Instant beforeUpdates = Instant.now();
        
        // Create entity
        Map<String, Object> initialPayload = new HashMap<>();
        initialPayload.put("name", "Alice");
        initialPayload.put("balance", 1000.0);
        
        eventStream.addEvent(entityId, beforeUpdates.plusMillis(100), "ENTITY_CREATED", initialPayload);
        
        // Make several updates
        for (int i = 1; i <= 10; i++) {
            Map<String, Object> updatePayload = new HashMap<>();
            updatePayload.put("balance", 1000.0 - (i * 50.0));
            updatePayload.put("transaction", "tx-" + i);
            
            eventStream.addEvent(entityId, beforeUpdates.plusMillis(100 + i * 200), 
                               "ENTITY_UPDATED", updatePayload);
        }
        
        // Wait for processing
        sleep(2000);
        
        // Demonstrate rollback to different points in time
        Instant rollbackTime1 = beforeUpdates.plusMillis(500);  // After 2 updates
        Instant rollbackTime2 = beforeUpdates.plusMillis(1000); // After 5 updates
        
        logger.info("Testing rollback to different timestamps...");
        
        // Rollback to first timestamp
        var rollbackResult1 = rollbackService.createRollbackView(entityId, rollbackTime1);
        logger.info("Rollback 1 result: " + rollbackResult1);
        
        if (rollbackResult1.getRolledBackState().isPresent()) {
            EntityState state1 = rollbackResult1.getRolledBackState().get();
            logger.info("State at " + rollbackTime1 + ": balance = " + state1.getProperty("balance"));
        }
        
        // Rollback to second timestamp
        var rollbackResult2 = rollbackService.createRollbackView(entityId, rollbackTime2);
        logger.info("Rollback 2 result: " + rollbackResult2);
        
        if (rollbackResult2.getRolledBackState().isPresent()) {
            EntityState state2 = rollbackResult2.getRolledBackState().get();
            logger.info("State at " + rollbackTime2 + ": balance = " + state2.getProperty("balance"));
        }
        
        logger.info("State rollback demonstration completed");
    }

    private void demonstratePerformance(InMemoryEventStream eventStream, 
                                      EventProcessor eventProcessor, 
                                      RollbackService rollbackService) {
        logger.info("\n--- Demo 3: Performance Test ---");
        
        int numEntities = 100;
        int eventsPerEntity = 50;
        
        logger.info("Generating " + (numEntities * eventsPerEntity) + " events...");
        
        long startTime = System.currentTimeMillis();
        
        // Generate many events
        for (int entityIdx = 0; entityIdx < numEntities; entityIdx++) {
            String entityId = "perf-entity-" + entityIdx;
            
            // Create entity
            Map<String, Object> createPayload = new HashMap<>();
            createPayload.put("id", entityId);
            createPayload.put("value", 0);
            
            eventStream.addEvent(entityId, "ENTITY_CREATED", createPayload);
            
            // Update entity multiple times
            for (int updateIdx = 1; updateIdx < eventsPerEntity; updateIdx++) {
                Map<String, Object> updatePayload = new HashMap<>();
                updatePayload.put("value", updateIdx);
                updatePayload.put("timestamp", System.currentTimeMillis());
                
                eventStream.addEvent(entityId, "ENTITY_UPDATED", updatePayload);
            }
        }
        
        // Wait for all events to be processed
        while (eventStream.getQueueSize() > 0) {
            sleep(100);
        }
        
        long endTime = System.currentTimeMillis();
        long processingTime = endTime - startTime;
        
        logger.info("Event generation and processing completed in " + processingTime + "ms");
        
        // Test rollback performance
        logger.info("Testing rollback performance...");
        
        String testEntityId = "perf-entity-50";
        Instant rollbackTime = Instant.now().minusSeconds(5);
        
        long rollbackStart = System.nanoTime();
        Optional<EntityState> rolledBackState = rollbackService.rollbackEntityState(testEntityId, rollbackTime);
        long rollbackEnd = System.nanoTime();
        
        long rollbackDurationMs = TimeUnit.NANOSECONDS.toMillis(rollbackEnd - rollbackStart);
        
        logger.info("Rollback completed in " + rollbackDurationMs + "ms");
        logger.info("Rollback target achieved: " + (rollbackDurationMs < 100 ? "YES" : "NO") + 
                   " (target: <100ms)");
        
        if (rolledBackState.isPresent()) {
            logger.info("Rolled back state: " + rolledBackState.get().getProperties());
        }
    }

    private void demonstrateMonitoring(EventProcessor eventProcessor, RollbackService rollbackService) {
        logger.info("\n--- Demo 4: Statistics and Monitoring ---");
        
        // Display processing statistics
        var processingStats = eventProcessor.getStatistics();
        logger.info("Processing Statistics: " + processingStats);
        
        // Display rollback statistics
        var rollbackStats = rollbackService.getStatistics();
        logger.info("Rollback Statistics: " + rollbackStats);
        
        // Display current system state
        logger.info("System State Summary:");
        logger.info("- Total entities: " + processingStats.getEntityCount());
        logger.info("- Total events processed: " + processingStats.getProcessedEvents());
        logger.info("- Failed events: " + processingStats.getFailedEvents());
        logger.info("- Success rate: " + String.format("%.2f%%", processingStats.getSuccessRate() * 100));
        logger.info("- Total snapshots: " + rollbackStats.getTotalSnapshots());
        logger.info("- Snapshot coverage: " + String.format("%.2f%%", rollbackStats.getSnapshotCoverage() * 100));
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}