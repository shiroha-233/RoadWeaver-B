package net.shiroha233.roadweaver.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.architectury.platform.Platform;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public final class PresetService {
    private static final Logger LOGGER = LoggerFactory.getLogger("roadweaver");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String BASE_DIR = "roadweaver";
    private static final String PRESET_DIR = "presets";

    private static final AtomicReference<Map<String, PresetDef>> PRESETS = new AtomicReference<>(new LinkedHashMap<>());

    private PresetService() {}

    public static synchronized void reload() {
        Path cfgRoot = Platform.getConfigFolder();
        Path baseDir = cfgRoot.resolve(BASE_DIR);
        Path presetDir = baseDir.resolve(PRESET_DIR);
        Map<String, PresetDef> map = new LinkedHashMap<>();
        try {
            try {
                Files.createDirectories(presetDir);
            } catch (Exception e) {
                LOGGER.warn("Failed to create preset directory: {}", presetDir, e);
            }
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(presetDir, "*.json")) {
                for (Path p : ds) {
                    PresetFile dto = readPresetFile(p);
                    if (dto == null) continue;
                    String id = dto.id;
                    if (id == null || id.isBlank()) id = stripExt(p.getFileName().toString());
                    List<String> mats = dto.materials != null ? dto.materials : List.of();
                    List<String> valid = new ArrayList<>();
                    for (String s : mats) {
                        try {
                            ResourceLocation rl = new ResourceLocation(s);
                            Block b = BuiltInRegistries.BLOCK.get(rl);
                            if (b != null && b != Blocks.AIR) valid.add(s);
                        } catch (Throwable ignored) {}
                    }
                    if (valid.isEmpty()) {
                        LOGGER.warn("Skip preset {} due to empty/invalid materials", p.getFileName());
                        continue;
                    }
                    int weight = dto.weight <= 0 ? 1 : dto.weight;
                    String name = dto.name == null || dto.name.isBlank() ? id : dto.name;
                    PresetDef def = new PresetDef(id, name, Collections.unmodifiableList(valid), weight);
                    if (map.containsKey(id)) {
                        LOGGER.warn("Duplicate preset id '{}', file {} is ignored", id, p.getFileName());
                        continue;
                    }
                    map.put(id, def);
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed scanning presets: {}", presetDir, e);
        }
        if (map.isEmpty()) {
            try {
                writeSamplePresets(presetDir);
            } catch (Exception e) {
                LOGGER.warn("Failed to write sample presets: {}", e.toString());
            }
            map = defaultPresets();
        }
        PRESETS.set(map);
        LOGGER.info("Presets loaded: {} entries", map.size());
    }

    private static String stripExt(String fn) {
        int i = fn.lastIndexOf('.');
        return i > 0 ? fn.substring(0, i) : fn;
    }

    private static PresetFile readPresetFile(Path p) {
        try (BufferedReader br = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
            return GSON.fromJson(br, PresetFile.class);
        } catch (Exception e) {
            LOGGER.warn("Failed to read preset file {}: {}", p.getFileName(), e.toString());
            return null;
        }
    }

    private static void writeSamplePresets(Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (Exception ignored) {}
        PresetFile a = new PresetFile();
        a.id = "stone_street";
        a.name = "Stone Street";
        a.materials = List.of("minecraft:stone_bricks", "minecraft:polished_andesite");
        a.weight = 8;
        PresetFile b = new PresetFile();
        b.id = "mud_road";
        b.name = "Mud Road";
        b.materials = List.of("minecraft:mud_bricks", "minecraft:packed_mud");
        b.weight = 1;
        writePreset(dir.resolve("stone_street.json"), a);
        writePreset(dir.resolve("mud_road.json"), b);
    }

    private static void writePreset(Path file, PresetFile dto) {
        try (BufferedWriter bw = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            GSON.toJson(dto, bw);
        } catch (Exception ignored) {}
    }

    private static Map<String, PresetDef> defaultPresets() {
        Map<String, PresetDef> m = new LinkedHashMap<>();
        PresetDef a = new PresetDef("mud_road", "Mud Road", List.of("minecraft:mud_bricks", "minecraft:packed_mud"), 1);
        PresetDef b = new PresetDef("stone_street", "Stone Street", List.of("minecraft:polished_andesite", "minecraft:stone_bricks"), 1);
        PresetDef c = new PresetDef("aged_stone", "Aged Stone", List.of("minecraft:stone_bricks", "minecraft:mossy_stone_bricks", "minecraft:cracked_stone_bricks"), 1);
        m.put(a.id(), a);
        m.put(b.id(), b);
        m.put(c.id(), c);
        return m;
    }

    public static synchronized List<PresetDef> getAllPresets() {
        if (PRESETS.get().isEmpty()) reload();
        return List.copyOf(PRESETS.get().values());
    }

    public static synchronized List<BlockState> chooseMaterialsForArtificial(RandomSource rnd, ModConfig cfg) {
        if (PRESETS.get().isEmpty()) reload();
        List<String> selected = cfg == null ? List.of() : cfg.selectedArtificialPresetIds();
        Map<String, PresetDef> all = PRESETS.get();
        List<PresetDef> pool = new ArrayList<>();
        if (selected != null && !selected.isEmpty()) {
            for (String id : selected) {
                PresetDef d = all.get(id);
                if (d != null) pool.add(d);
            }
        }
        if (pool.isEmpty()) pool = new ArrayList<>(all.values());
        if (pool.isEmpty()) pool = new ArrayList<>(defaultPresets().values());
        PresetDef chosen = pickPreset(rnd, pool, cfg != null && cfg.useWeightedPreset());
        return toBlockStates(chosen.materials());
    }

    private static PresetDef pickPreset(RandomSource rnd, List<PresetDef> pool, boolean weighted) {
        if (!weighted) return pool.get(rnd.nextInt(pool.size()));
        int sum = 0;
        for (PresetDef d : pool) sum += Math.max(1, d.weight());
        int r = rnd.nextInt(Math.max(1, sum));
        int acc = 0;
        for (PresetDef d : pool) {
            acc += Math.max(1, d.weight());
            if (r < acc) return d;
        }
        return pool.get(pool.size() - 1);
    }

    private static List<BlockState> toBlockStates(List<String> ids) {
        List<BlockState> out = new ArrayList<>();
        if (ids == null) return out;
        for (String s : ids) {
            try {
                ResourceLocation rl = new ResourceLocation(s);
                Block b = BuiltInRegistries.BLOCK.get(rl);
                if (b != null && b != Blocks.AIR) out.add(b.defaultBlockState());
            } catch (Throwable ignored) {}
        }
        if (out.isEmpty()) out.add(Blocks.STONE_BRICKS.defaultBlockState());
        return out;
    }

    public static synchronized List<List<String>> getMaterialCombos() {
        if (PRESETS.get().isEmpty()) reload();
        List<List<String>> combos = new ArrayList<>();
        for (PresetDef d : PRESETS.get().values()) combos.add(d.materials());
        return combos;
    }

    private static class PresetFile {
        String id;
        String name;
        List<String> materials;
        int weight;
    }

    public record PresetDef(String id, String name, List<String> materials, int weight) {}
}
