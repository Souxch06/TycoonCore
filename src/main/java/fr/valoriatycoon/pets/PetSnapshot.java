package fr.valoriatycoon.pets;

import java.util.List;

/** Immutable startup snapshot of owned pets and eggs awaiting physical delivery. */
public record PetSnapshot(List<PetProfile> pets, List<PetEgg> pendingEggs) {
    public PetSnapshot {
        pets = List.copyOf(pets);
        pendingEggs = List.copyOf(pendingEggs);
    }
}
