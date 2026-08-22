package fr.valoriatycoon.tutorial;

/** Result of one authoritative batched tutorial progression write. */
public record TutorialProgressUpdate(
        TutorialProfile profile,
        TutorialStep completedStep,
        long rewardedCents,
        long resultingBalanceCents
) {
    public boolean rewarded() {
        return completedStep != null && rewardedCents > 0L;
    }
}
