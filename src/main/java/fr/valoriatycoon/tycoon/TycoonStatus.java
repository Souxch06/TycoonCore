package fr.valoriatycoon.tycoon;

/** Durable lifecycle used to resume interrupted plot preparation or deletion. */
public enum TycoonStatus {
    PREPARING,
    ACTIVE,
    DELETING
}
