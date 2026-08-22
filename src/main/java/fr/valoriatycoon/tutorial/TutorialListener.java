package fr.valoriatycoon.tutorial;

import fr.valoriatycoon.farm.FarmType;
import fr.valoriatycoon.farm.FarmWorld;
import fr.valoriatycoon.farm.FarmWorldService;
import java.util.Objects;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/** Connects validated public-farm actions and player sessions to tutorial progression. */
public final class TutorialListener implements Listener {
    private final TutorialService tutorial;
    private final FarmWorldService farms;

    public TutorialListener(TutorialService tutorial, FarmWorldService farms) {
        this.tutorial = Objects.requireNonNull(tutorial, "tutorial");
        this.farms = Objects.requireNonNull(farms, "farms");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        tutorial.activate(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        tutorial.deactivate(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        FarmWorld farm = farms.farm(event.getBlock().getWorld()).orElse(null);
        if (farm == null) {
            return;
        }
        Material material = event.getBlock().getType();
        TutorialStep step = null;
        if (farm.definition().type() == FarmType.MINE
                && (material == Material.COAL_ORE || material == Material.DEEPSLATE_COAL_ORE)) {
            step = TutorialStep.MINE_COAL;
        } else if (farm.definition().type() == FarmType.FIELDS && material == Material.WHEAT) {
            step = TutorialStep.HARVEST_WHEAT;
        } else if (farm.definition().type() == FarmType.FOREST && material == Material.OAK_LOG) {
            step = TutorialStep.CHOP_OAK;
        }
        if (step != null) {
            tutorial.record(event.getPlayer().getUniqueId(), step, 1L);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH
                || !(event.getCaught() instanceof Item caught)
                || caught.getItemStack().getType() != Material.COD) {
            return;
        }
        FarmWorld farm = farms.farm(event.getPlayer().getWorld()).orElse(null);
        if (farm == null || farm.definition().type() != FarmType.FISHING) {
            return;
        }
        tutorial.record(
                event.getPlayer().getUniqueId(),
                TutorialStep.CATCH_COD,
                caught.getItemStack().getAmount()
        );
    }
}
