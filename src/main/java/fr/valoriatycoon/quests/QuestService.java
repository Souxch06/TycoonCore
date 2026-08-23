package fr.valoriatycoon.quests;

import fr.valoriatycoon.crates.QuestKeyRewardSink;
import fr.valoriatycoon.economy.InternalEconomyService;
import fr.valoriatycoon.tools.ToolType;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/** Batches repeatable quest actions and keeps online completion counters cached. */
public final class QuestService {
    private final JavaPlugin plugin;
    private final QuestSettings settings;
    private final QuestRepository repository;
    private final InternalEconomyService economy;
    private final QuestKeyRewardSink questKeyRewards;
    private final Logger logger;
    private final Map<UUID, QuestProfile> profiles = new ConcurrentHashMap<>();
    private final Map<Key, Long> pending = new ConcurrentHashMap<>();
    private BukkitTask task;

    public QuestService(
            JavaPlugin plugin,
            QuestSettings settings,
            QuestRepository repository,
            InternalEconomyService economy,
            QuestKeyRewardSink questKeyRewards,
            Logger logger
    ) {
        this.plugin=plugin; this.settings=settings; this.repository=repository; this.economy=economy;
        this.questKeyRewards=questKeyRewards; this.logger=logger;
    }

    public void start() {
        task=plugin.getServer().getScheduler().runTaskTimer(
                plugin,this::flushAll,settings.flushIntervalTicks(),settings.flushIntervalTicks()
        );
    }

    public CompletableFuture<QuestProfile> activate(UUID playerId) {
        return repository.loadOrCreate(playerId)
                .thenCompose(profile -> synchronizeQuestKeys(playerId, profile).thenApply(ignored -> profile))
                .thenApply(profile->{profiles.put(playerId,profile);return profile;});
    }

    public void deactivate(UUID playerId) {
        flushPlayer(playerId); profiles.remove(playerId);
    }

    public void recordToolAction(UUID playerId, ToolType type) {
        for (QuestDefinition quest:settings.quests().values()) {
            if(quest.toolType()==type) pending.merge(new Key(playerId,quest.id()),1L,Long::sum);
        }
    }

    public QuestProfile profile(UUID playerId) {
        return profiles.getOrDefault(playerId,new QuestProfile(Map.of(),Map.of()));
    }

    public CompletableFuture<Void> flushPlayer(UUID playerId) {
        Map<Key,Long> snapshot=new HashMap<>();
        pending.forEach((key,value)->{if(key.playerId.equals(playerId)&&pending.remove(key,value))snapshot.put(key,value);});
        return flush(snapshot);
    }

    public CompletableFuture<QuestProfile> reload(UUID playerId) {
        return repository.loadOrCreate(playerId)
                .thenApply(profile->{profiles.put(playerId,profile);return profile;});
    }

    public void stop(Duration timeout) {
        if(task!=null){task.cancel();task=null;}
        Map<Key,Long> snapshot=new HashMap<>(pending); pending.clear();
        try{flush(snapshot).get(timeout.toMillis(), TimeUnit.MILLISECONDS);}
        catch(Exception error){logger.log(Level.SEVERE,"Quest flush failed",error);}
    }

    public QuestSettings settings(){return settings;}

    private void flushAll(){Map<Key,Long>s=new HashMap<>(pending);s.forEach((k,v)->pending.remove(k,v));flush(s);}

    private CompletableFuture<Void> flush(Map<Key,Long> snapshot) {
        List<CompletableFuture<?>> futures=new ArrayList<>();
        java.util.Set<UUID> affected=new java.util.HashSet<>();
        snapshot.forEach((key,amount)->{
            QuestDefinition quest=settings.quests().get(key.questId);
            if(quest==null)return;
            affected.add(key.playerId);
            futures.add(repository.addProgress(key.playerId,quest,amount)
                    .thenCompose(update -> {
                        if(update.resultingMoneyCents()>=0) {
                            economy.synchronizeCommittedBalance(key.playerId,update.resultingMoneyCents());
                        }
                        return update.newlyCompleted()>0
                                ? questKeyRewards.synchronize(
                                        key.playerId,
                                        quest.id(),
                                        update.progress().completions()
                                )
                                : CompletableFuture.completedFuture(null);
                    })
                    .whenComplete((ignored,error)->{
                        if(error!=null) logger.log(Level.WARNING,"Quest update failed for "+key,error);
                    }));
        });
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).thenCompose(ignored->{
            CompletableFuture<?>[] reloads=affected.stream().filter(profiles::containsKey)
                    .map(this::reload).toArray(CompletableFuture[]::new);
            return CompletableFuture.allOf(reloads);
        });
    }

    private CompletableFuture<Void> synchronizeQuestKeys(UUID playerId, QuestProfile profile) {
        CompletableFuture<?>[] futures = profile.progress().values().stream()
                .filter(progress -> progress.completions() > 0L)
                .map(progress -> questKeyRewards.synchronize(
                        playerId,
                        progress.questId(),
                        progress.completions()
                ))
                .toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(futures);
    }

    private record Key(UUID playerId,String questId){}
}
