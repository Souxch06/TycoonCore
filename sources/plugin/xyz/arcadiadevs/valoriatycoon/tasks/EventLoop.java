/*
 * Décompilé avec CFR 0.152.
 * 
 * Impossible de charger les classes suivantes :
 *  lombok.Generated
 *  org.bukkit.scheduler.BukkitRunnable
 */
package xyz.arcadiadevs.valoriatycoon.tasks;

import java.util.List;
import java.util.Random;
import lombok.Generated;
import org.bukkit.scheduler.BukkitRunnable;
import xyz.arcadiadevs.valoriatycoon.ValoriaTycoon;
import xyz.arcadiadevs.valoriatycoon.models.events.ActiveEvent;
import xyz.arcadiadevs.valoriatycoon.models.events.Event;
import xyz.arcadiadevs.valoriatycoon.utils.TimeUtil;
import xyz.arcadiadevs.valoriatycoon.utils.config.Config;
import xyz.arcadiadevs.valoriatycoon.utils.config.message.Messages;

public class EventLoop
extends BukkitRunnable {
    private static ActiveEvent activeEvent = null;
    private static ActiveEvent nextEvent = null;
    private static List<Event> events;
    private static long timeBetweenEvents;
    private static long eventDuration;

    public EventLoop(List<Event> list) {
        events = list;
        timeBetweenEvents = TimeUtil.parseTimeMillis(Config.EVENTS_TIME_BETWEEN_EVENTS.getString());
        eventDuration = TimeUtil.parseTimeMillis(Config.EVENTS_EVENT_DURATION.getString());
        activeEvent = new ActiveEvent(null, System.currentTimeMillis(), System.currentTimeMillis() + timeBetweenEvents);
        EventLoop.setRandomNextEvent();
    }

    private static void setRandomNextEvent() {
        Random random = new Random();
        if (events.isEmpty()) {
            nextEvent = null;
            return;
        }
        int n = random.nextInt(events.size());
        nextEvent = new ActiveEvent(events.get(n), System.currentTimeMillis() + timeBetweenEvents, System.currentTimeMillis() + timeBetweenEvents + eventDuration);
    }

    public void run() {
        if (nextEvent != null && nextEvent.startTime() < System.currentTimeMillis()) {
            activeEvent = nextEvent;
            Messages.EVENT_STARTED.format("event", activeEvent.event().getName(), "time", TimeUtil.millisToTime(eventDuration)).send(ValoriaTycoon.getInstance().getConfig().getBoolean(Config.EVENTS_BROADCAST_ENABLED.getPath()));
            nextEvent = null;
            return;
        }
        if (activeEvent.endTime() < System.currentTimeMillis()) {
            Messages.EVENT_ENDED.format("event", activeEvent.event().getName(), "time", TimeUtil.millisToTime(timeBetweenEvents)).send(ValoriaTycoon.getInstance().getConfig().getBoolean(Config.EVENTS_BROADCAST_ENABLED.getPath()));
            activeEvent = new ActiveEvent(null, System.currentTimeMillis(), System.currentTimeMillis() + timeBetweenEvents);
            EventLoop.setRandomNextEvent();
        }
    }

    public static void setNextEvent(Event event) {
        if (event == null) {
            return;
        }
        nextEvent = activeEvent = new ActiveEvent(event, System.currentTimeMillis(), System.currentTimeMillis() + eventDuration);
    }

    public static void stopEvent() {
        activeEvent = new ActiveEvent(null, System.currentTimeMillis(), System.currentTimeMillis() + timeBetweenEvents);
        Messages.EVENT_FORCE_ENDED.format("time", TimeUtil.millisToTime(timeBetweenEvents)).send(Config.EVENTS_BROADCAST_ENABLED.getBoolean());
        nextEvent = null;
        EventLoop.setRandomNextEvent();
    }

    @Generated
    public static ActiveEvent getActiveEvent() {
        return activeEvent;
    }
}

