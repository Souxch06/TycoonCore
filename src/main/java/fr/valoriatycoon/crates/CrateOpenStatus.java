package fr.valoriatycoon.crates;

/** Authoritative result of consuming a generic physical key. */
public enum CrateOpenStatus {
    SUCCESS,
    KEY_INVALID,
    KEY_TYPE_MISMATCH,
    KEY_ALREADY_USED
}
