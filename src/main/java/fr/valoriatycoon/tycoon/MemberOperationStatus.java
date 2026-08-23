package fr.valoriatycoon.tycoon;

/** Authoritative result for adding/removing a trusted member. */
public enum MemberOperationStatus {
    SUCCESS,
    NOT_FOUND,
    NOT_OWNER,
    ALREADY_MEMBER,
    NOT_MEMBER,
    MEMBER_LIMIT,
    CANNOT_ADD_OWNER
}
