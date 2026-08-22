package fr.valoriatycoon.pets;

/** Result carrying an egg, an unlocked/activated pet and an authoritative balance when relevant. */
public record PetOperationResult(
        PetOperationStatus status,
        PetProfile pet,
        PetEgg egg,
        long balanceCents,
        int requiredRank
) {
    /** Returns whether the requested mutation committed. */
    public boolean successful() {
        return status == PetOperationStatus.SUCCESS;
    }
}
