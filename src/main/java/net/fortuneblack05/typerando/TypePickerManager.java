package net.fortuneblack05.typerando;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class TypePickerManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Data structures for our state
    private final Map<String, Integer> roles = new HashMap<>();
    private final Map<String, Set<Integer>> blacklists = new HashMap<>();
    private final Map<String, Map<Integer, Double>> weights = new HashMap<>();
    private final Random random = new Random();

    // A helper class just for saving/loading all this data to JSON neatly
    private static class SaveData {
        Map<String, Integer> roles = new HashMap<>();
        Map<String, Set<Integer>> blacklists = new HashMap<>();
        Map<String, Map<Integer, Double>> weights = new HashMap<>();
    }

    private Path saveFile(MinecraftServer server) {
        return server.getSavePath(WorldSavePath.ROOT).resolve("typerando_data.json");
    }

    public void load(MinecraftServer server) {
        roles.clear(); blacklists.clear(); weights.clear();
        Path file = saveFile(server);
        if (!Files.exists(file)) return;

        try (Reader r = Files.newBufferedReader(file)) {
            SaveData data = GSON.fromJson(r, SaveData.class);
            if (data != null) {
                if (data.roles != null) roles.putAll(data.roles);
                if (data.blacklists != null) blacklists.putAll(data.blacklists);
                if (data.weights != null) weights.putAll(data.weights);
            }
        } catch (Exception e) {
            System.err.println("[TypeRando] Failed to load data: " + e.getMessage());
        }
    }

    public void save(MinecraftServer server) {
        Path file = saveFile(server);
        try (Writer w = Files.newBufferedWriter(file)) {
            SaveData data = new SaveData();
            data.roles = this.roles;
            data.blacklists = this.blacklists;
            data.weights = this.weights;
            GSON.toJson(data, w);
        } catch (Exception e) {
            System.err.println("[TypeRando] Failed to save data: " + e.getMessage());
        }
    }

    public Set<Integer> getBlacklist(String uuid) {
        return blacklists.getOrDefault(uuid, java.util.Collections.emptySet());
    }

    public double getWeight(String uuid, int typeId) {
        return weights.getOrDefault(uuid, java.util.Collections.emptyMap()).getOrDefault(typeId, 1.0);
    }

    public Set<String> getTrackedUuids() {
        Set<String> allUuids = new HashSet<>();
        allUuids.addAll(roles.keySet());
        allUuids.addAll(blacklists.keySet());
        allUuids.addAll(weights.keySet());
        return allUuids;
    }

    public void setBlacklist(String uuid, int typeId, boolean isBlacklisted) {
        blacklists.computeIfAbsent(uuid, k -> new HashSet<>());
        if (isBlacklisted) {
            blacklists.get(uuid).add(typeId);
        } else {
            blacklists.get(uuid).remove(typeId);
        }
    }

    public void setWeight(String uuid, int typeId, double weight) {
        weights.computeIfAbsent(uuid, k -> new HashMap<>());
        weights.get(uuid).put(typeId, weight);
    }

    public void clearRole(UUID uuid) {
        roles.remove(uuid.toString());
    }

    public boolean hasRole(UUID uuid) {
        return roles.containsKey(uuid.toString());
    }

    public Optional<Types> getRole(UUID uuid) {
        Integer id = roles.get(uuid.toString());
        return id == null ? Optional.empty() : Types.fromId(id);
    }

    // Assigns a type using Weights and Blacklists
    public Optional<Types> assignIfMissing(UUID uuid) {
        if (hasRole(uuid)) return getRole(uuid);

        String uuidStr = uuid.toString();
        Set<Integer> used = new HashSet<>(roles.values());
        Set<Integer> playerBlacklist = blacklists.getOrDefault(uuidStr, Collections.emptySet());
        Map<Integer, Double> playerWeights = weights.getOrDefault(uuidStr, Collections.emptyMap());

        List<Integer> available = new ArrayList<>();
        for (int i = 1; i <= 18; i++) {
            // Only add if nobody has it AND the player isn't blacklisted from it
            if (!used.contains(i) && !playerBlacklist.contains(i)) {
                available.add(i);
            }
        }

        if (available.isEmpty()) return Optional.empty();

        // Weighted Random Selection
        double totalWeight = 0.0;
        for (int id : available) {
            totalWeight += playerWeights.getOrDefault(id, 1.0); // Default weight is 1.0
        }

        double randomValue = random.nextDouble() * totalWeight;
        double currentSum = 0.0;
        int chosen = available.get(0); // fallback

        for (int id : available) {
            currentSum += playerWeights.getOrDefault(id, 1.0);
            if (randomValue <= currentSum) {
                chosen = id;
                break;
            }
        }

        roles.put(uuidStr, chosen);
        return Types.fromId(chosen);
    }
}