package fr.valoriatycoon.farm;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.bukkit.Location;
import org.bukkit.World;

/** Runtime binding between a validated definition and its generated Bukkit world. */
public record FarmWorld(
        FarmDefinition definition,
        World world,
        Location destination,
        Map<Integer, Location> zoneDestinations
) {
    public FarmWorld {
        destination = destination.clone();
        Map<Integer, Location> copies = new LinkedHashMap<>();
        zoneDestinations.forEach((index, location) -> copies.put(index, location.clone()));
        zoneDestinations = Map.copyOf(copies);
    }

    @Override
    public Location destination() {
        return destination.clone();
    }

    @Override
    public Map<Integer, Location> zoneDestinations() {
        Map<Integer, Location> copies = new LinkedHashMap<>();
        zoneDestinations.forEach((index, location) -> copies.put(index, location.clone()));
        return Map.copyOf(copies);
    }

    public Optional<Location> zoneDestination(int index) {
        Location location = zoneDestinations.get(index);
        return location == null ? Optional.empty() : Optional.of(location.clone());
    }
}
