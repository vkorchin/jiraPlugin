package com.eventstream.demo;

import com.eventstream.handler.EventHandler;
import com.eventstream.handler.EventHandlerRegistry;
import com.eventstream.handler.impl.CreateEntityHandler;
import com.eventstream.handler.impl.UpdateEntityHandler;
import com.eventstream.model.EntityState;
import com.eventstream.model.Event;
import com.eventstream.processor.EventProcessor;
import com.eventstream.rollback.RollbackService;
import com.eventstream.snapshot.SnapshotManager;
import com.eventstream.state.StateManager;
import com.eventstream.stream.InMemoryEventStream;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Demonstration of system extensibility through custom event handlers
 */
public class CustomEventHandlerDemo {
    private static final Logger logger = Logger.getLogger(CustomEventHandlerDemo.class.getName());

    public static void main(String[] args) {
        CustomEventHandlerDemo demo = new CustomEventHandlerDemo();
        demo.runDemo();
    }

    public void runDemo() {
        logger.info("=== Custom Event Handler Demo ===");

        // Initialize components
        InMemoryEventStream eventStream = new InMemoryEventStream(1000);
        StateManager stateManager = new StateManager();
        SnapshotManager snapshotManager = new SnapshotManager(50);
        EventHandlerRegistry handlerRegistry = new EventHandlerRegistry();
        
        // Register standard handlers
        handlerRegistry.registerHandler(new CreateEntityHandler());
        handlerRegistry.registerHandler(new UpdateEntityHandler());
        
        // Register custom handlers
        handlerRegistry.registerHandler(new BankAccountDepositHandler());
        handlerRegistry.registerHandler(new BankAccountWithdrawHandler());
        handlerRegistry.registerHandler(new BankAccountFreezeHandler());
        
        EventProcessor eventProcessor = new EventProcessor(
            eventStream, stateManager, snapshotManager, handlerRegistry, 3, 1000);
        
        RollbackService rollbackService = new RollbackService(
            stateManager, snapshotManager, handlerRegistry, eventProcessor);

        try {
            eventProcessor.start();
            
            // Demonstrate custom business logic
            demonstrateBankAccountOperations(eventStream, rollbackService);
            
        } finally {
            eventProcessor.stop();
            rollbackService.shutdown();
            try {
                eventStream.close();
            } catch (Exception e) {
                logger.warning("Error closing event stream: " + e.getMessage());
            }
        }
        
        logger.info("=== Custom Event Handler Demo completed ===");
    }

    private void demonstrateBankAccountOperations(InMemoryEventStream eventStream, RollbackService rollbackService) {
        logger.info("\n--- Bank Account Operations Demo ---");
        
        String accountId = "account-12345";
        Instant startTime = Instant.now();
        
        // Create bank account
        Map<String, Object> createPayload = new HashMap<>();
        createPayload.put("accountNumber", "12345");
        createPayload.put("customerName", "John Smith");
        createPayload.put("balance", 0.0);
        createPayload.put("status", "ACTIVE");
        
        eventStream.addEvent(accountId, startTime.plusMillis(100), "ENTITY_CREATED", createPayload);
        
        // Make several deposits
        eventStream.addEvent(accountId, startTime.plusMillis(200), "ACCOUNT_DEPOSIT", 
                           Map.of("amount", 1000.0, "description", "Initial deposit"));
        
        eventStream.addEvent(accountId, startTime.plusMillis(300), "ACCOUNT_DEPOSIT", 
                           Map.of("amount", 500.0, "description", "Salary"));
        
        // Make withdrawals
        eventStream.addEvent(accountId, startTime.plusMillis(400), "ACCOUNT_WITHDRAW", 
                           Map.of("amount", 200.0, "description", "ATM withdrawal"));
        
        eventStream.addEvent(accountId, startTime.plusMillis(500), "ACCOUNT_WITHDRAW", 
                           Map.of("amount", 300.0, "description", "Online purchase"));
        
        // Freeze account
        eventStream.addEvent(accountId, startTime.plusMillis(600), "ACCOUNT_FREEZE", 
                           Map.of("reason", "Suspicious activity detected"));
        
        // Try to withdraw from frozen account (should fail)
        eventStream.addEvent(accountId, startTime.plusMillis(700), "ACCOUNT_WITHDRAW", 
                           Map.of("amount", 100.0, "description", "Attempted withdrawal"));
        
        // Wait for processing
        sleep(2000);
        
        // Show current state
        var currentState = rollbackService.createRollbackView(accountId, Instant.now());
        if (currentState.getRolledBackState().isPresent()) {
            EntityState state = currentState.getRolledBackState().get();
            logger.info("Current account state: " + state.getProperties());
        }
        
        // Rollback to before freeze
        Instant beforeFreeze = startTime.plusMillis(550);
        var rollbackResult = rollbackService.createRollbackView(accountId, beforeFreeze);
        
        if (rollbackResult.getRolledBackState().isPresent()) {
            EntityState rolledBackState = rollbackResult.getRolledBackState().get();
            logger.info("Account state before freeze: " + rolledBackState.getProperties());
        }
        
        logger.info("Rollback duration: " + rollbackResult.getDurationMs() + "ms");
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // Custom event handlers for bank account operations

    public static class BankAccountDepositHandler implements EventHandler {
        private static final Logger logger = Logger.getLogger(BankAccountDepositHandler.class.getName());

        @Override
        public String getEventType() {
            return "ACCOUNT_DEPOSIT";
        }

        @Override
        public EntityState apply(EntityState currentState, Event event) {
            if (currentState == null) {
                throw new IllegalStateException("Cannot deposit to non-existent account");
            }

            String status = (String) currentState.getProperty("status");
            if ("FROZEN".equals(status)) {
                throw new IllegalStateException("Cannot deposit to frozen account");
            }

            Double currentBalance = (Double) currentState.getProperty("balance");
            Double depositAmount = (Double) event.getPayload().get("amount");
            
            if (depositAmount <= 0) {
                throw new IllegalArgumentException("Deposit amount must be positive");
            }

            Double newBalance = (currentBalance != null ? currentBalance : 0.0) + depositAmount;
            
            logger.info("Processing deposit: $" + depositAmount + " to account " + 
                       currentState.getEntityId() + ", new balance: $" + newBalance);

            return currentState
                .withProperty("balance", newBalance, event.getTimestamp())
                .withProperty("lastTransaction", event.getPayload().get("description"), event.getTimestamp());
        }
    }

    public static class BankAccountWithdrawHandler implements EventHandler {
        private static final Logger logger = Logger.getLogger(BankAccountWithdrawHandler.class.getName());

        @Override
        public String getEventType() {
            return "ACCOUNT_WITHDRAW";
        }

        @Override
        public EntityState apply(EntityState currentState, Event event) {
            if (currentState == null) {
                throw new IllegalStateException("Cannot withdraw from non-existent account");
            }

            String status = (String) currentState.getProperty("status");
            if ("FROZEN".equals(status)) {
                logger.warning("Attempted withdrawal from frozen account: " + currentState.getEntityId());
                throw new IllegalStateException("Cannot withdraw from frozen account");
            }

            Double currentBalance = (Double) currentState.getProperty("balance");
            Double withdrawAmount = (Double) event.getPayload().get("amount");
            
            if (withdrawAmount <= 0) {
                throw new IllegalArgumentException("Withdrawal amount must be positive");
            }

            if (currentBalance == null || currentBalance < withdrawAmount) {
                throw new IllegalStateException("Insufficient funds for withdrawal");
            }

            Double newBalance = currentBalance - withdrawAmount;
            
            logger.info("Processing withdrawal: $" + withdrawAmount + " from account " + 
                       currentState.getEntityId() + ", new balance: $" + newBalance);

            return currentState
                .withProperty("balance", newBalance, event.getTimestamp())
                .withProperty("lastTransaction", event.getPayload().get("description"), event.getTimestamp());
        }
    }

    public static class BankAccountFreezeHandler implements EventHandler {
        private static final Logger logger = Logger.getLogger(BankAccountFreezeHandler.class.getName());

        @Override
        public String getEventType() {
            return "ACCOUNT_FREEZE";
        }

        @Override
        public EntityState apply(EntityState currentState, Event event) {
            if (currentState == null) {
                throw new IllegalStateException("Cannot freeze non-existent account");
            }

            String reason = (String) event.getPayload().get("reason");
            
            logger.info("Freezing account " + currentState.getEntityId() + " - Reason: " + reason);

            return currentState
                .withProperty("status", "FROZEN", event.getTimestamp())
                .withProperty("freezeReason", reason, event.getTimestamp())
                .withProperty("freezeTimestamp", event.getTimestamp().toString(), event.getTimestamp());
        }
    }
}