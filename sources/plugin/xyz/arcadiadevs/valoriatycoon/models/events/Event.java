package xyz.arcadiadevs.valoriatycoon.models.events;

import lombok.Generated;

public abstract class Event {
    protected final long multiplier;
    protected String name;

    public Event(long l, String string) {
        this.multiplier = l;
        this.name = string;
    }

    @Generated
    public long getMultiplier() {
        return this.multiplier;
    }

    @Generated
    public String getName() {
        return this.name;
    }

    @Generated
    public void setName(String string) {
        this.name = string;
    }
}

