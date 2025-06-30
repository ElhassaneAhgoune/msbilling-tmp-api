package com.moneysab.cardexis.domain.enums;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * Enumeration of processing statuses for file processing jobs in the Visa EPIN system.
 * 
 * This enum defines the lifecycle states of file processing operations, including
 * valid state transitions and business rules for each status.
 * 
 * @author EL.AHGOUNE
 * @version 1.0.0
 * @since 2024
 */
public enum ProcessingStatus {
    
    /**
     * File has been uploaded and is awaiting processing.
     * Initial state when a file is first received by the system.
     */
    UPLOADED("File uploaded and queued for processing", true),
    
    /**
     * File is currently being processed.
     * Active state during parsing and data extraction operations.
     */
    PROCESSING("File is being processed", false),
    
    /**
     * File processing completed successfully.
     * Terminal state indicating all data has been extracted and persisted.
     */
    COMPLETED("File processing completed successfully", false),
    
    /**
     * File processing failed due to an error.
     * Terminal state indicating processing could not be completed.
     */
    FAILED("File processing failed", false),
    
    /**
     * File processing was cancelled by user or system.
     * Terminal state indicating processing was intentionally stopped.
     */
    CANCELLED("File processing was cancelled", false);
    
    private final String description;
    private final boolean canRetry;
    
    /**
     * Constructs a ProcessingStatus with the specified parameters.
     * 
     * @param description human-readable description of the status
     * @param canRetry whether processing can be retried from this status
     */
    ProcessingStatus(String description, boolean canRetry) {
        this.description = description;
        this.canRetry = canRetry;
    }
    
    /**
     * Gets the human-readable description of this status.
     * 
     * @return the status description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Indicates whether processing can be retried from this status.
     * 
     * @return true if retry is allowed, false otherwise
     */
    public boolean canRetry() {
        return canRetry;
    }
    
    /**
     * Checks if this status represents a terminal state.
     * Terminal states are final and cannot transition to other states.
     * 
     * @return true if this is a terminal status
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == CANCELLED;
    }
    
    /**
     * Checks if this status represents an active processing state.
     * 
     * @return true if processing is currently active
     */
    public boolean isActive() {
        return this == PROCESSING;
    }
    
    /**
     * Checks if this status represents a successful completion.
     * 
     * @return true if processing completed successfully
     */
    public boolean isSuccessful() {
        return this == COMPLETED;
    }
    
    /**
     * Gets the set of valid next states from the current status.
     * 
     * @return set of statuses that can be transitioned to from this status
     */
    public Set<ProcessingStatus> getValidNextStates() {
        return switch (this) {
            case UPLOADED -> EnumSet.of(PROCESSING, CANCELLED);
            case PROCESSING -> EnumSet.of(COMPLETED, FAILED, CANCELLED);
            case COMPLETED, FAILED, CANCELLED -> EnumSet.noneOf(ProcessingStatus.class);
        };
    }
    
    /**
     * Validates if a transition from this status to the target status is allowed.
     * 
     * @param targetStatus the status to transition to
     * @return true if the transition is valid, false otherwise
     */
    public boolean canTransitionTo(ProcessingStatus targetStatus) {
        if (targetStatus == null) {
            return false;
        }
        return getValidNextStates().contains(targetStatus);
    }
    
    /**
     * Validates a state transition and throws an exception if invalid.
     * 
     * @param targetStatus the status to transition to
     * @throws IllegalStateException if the transition is not allowed
     */
    public void validateTransition(ProcessingStatus targetStatus) {
        if (!canTransitionTo(targetStatus)) {
            throw new IllegalStateException(
                String.format("Invalid state transition from %s to %s. Valid transitions: %s",
                    this, targetStatus, getValidNextStates()));
        }
    }
    
    /**
     * Finds a ProcessingStatus by its name (case-insensitive).
     * 
     * @param name the status name to search for
     * @return Optional containing the matching status, or empty if no match found
     */
    public static Optional<ProcessingStatus> fromName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return Optional.empty();
        }
        
        return Arrays.stream(values())
                .filter(status -> status.name().equalsIgnoreCase(name.trim()))
                .findFirst();
    }
    
    /**
     * Gets all statuses that represent error or failure conditions.
     * 
     * @return set of error statuses
     */
    public static Set<ProcessingStatus> getErrorStatuses() {
        return EnumSet.of(FAILED, CANCELLED);
    }
    
    /**
     * Gets all statuses that represent active or pending processing.
     * 
     * @return set of active statuses
     */
    public static Set<ProcessingStatus> getActiveStatuses() {
        return EnumSet.of(UPLOADED, PROCESSING);
    }
    
    /**
     * Gets all terminal statuses (final states).
     * 
     * @return set of terminal statuses
     */
    public static Set<ProcessingStatus> getTerminalStatuses() {
        return EnumSet.of(COMPLETED, FAILED, CANCELLED);
    }
    
    /**
     * Determines the appropriate next status based on processing outcome.
     * 
     * @param success whether processing was successful
     * @param cancelled whether processing was cancelled
     * @return the appropriate next status
     */
    public static ProcessingStatus determineOutcomeStatus(boolean success, boolean cancelled) {
        if (cancelled) {
            return CANCELLED;
        }
        return success ? COMPLETED : FAILED;
    }
}