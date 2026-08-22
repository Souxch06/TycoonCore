package fr.valoriatycoon.tycoon;

/** Result of atomically reserving a plot slot. */
public enum TycoonAllocationStatus {
    SUCCESS,
    ALREADY_OWNS,
    GROUP_FULL
}
