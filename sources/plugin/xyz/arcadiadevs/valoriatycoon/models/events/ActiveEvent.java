/*
 * Décompilé avec CFR 0.152.
 */
package xyz.arcadiadevs.valoriatycoon.models.events;

import xyz.arcadiadevs.valoriatycoon.models.events.Event;

public record ActiveEvent(Event event, long startTime, long endTime) {
}

