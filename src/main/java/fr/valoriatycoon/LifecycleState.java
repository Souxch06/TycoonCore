package fr.valoriatycoon;

/** Explicit fail-closed lifecycle used by commands and integrations. */
public enum LifecycleState {
    STARTING,
    READY,
    FAILED,
    STOPPING
}
