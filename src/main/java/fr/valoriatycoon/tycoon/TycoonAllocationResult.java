package fr.valoriatycoon.tycoon;

/** Atomic plot reservation result. */
public record TycoonAllocationResult(TycoonAllocationStatus status, Tycoon tycoon) {
    public boolean successful() {
        return status == TycoonAllocationStatus.SUCCESS;
    }
}
