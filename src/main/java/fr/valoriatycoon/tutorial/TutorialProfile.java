package fr.valoriatycoon.tutorial;

/** Persisted sequential onboarding state for one player. */
public record TutorialProfile(
        TutorialStep step,
        long progress,
        boolean completed
) {
    public TutorialProfile {
        if (step == null || progress < 0) {
            throw new IllegalArgumentException("Invalid tutorial profile");
        }
        if (step == TutorialStep.READY_FOR_RANK && progress != 0L) {
            throw new IllegalArgumentException("Ready tutorial profile cannot retain progress");
        }
    }

    public static TutorialProfile initial() {
        return new TutorialProfile(TutorialStep.MINE_COAL, 0L, false);
    }
}
