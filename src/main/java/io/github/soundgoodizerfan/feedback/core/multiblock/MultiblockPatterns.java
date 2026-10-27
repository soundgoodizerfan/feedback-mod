/*
 * Feedback -- a Minecraft technology mod.
 * Copyright (C) 2026 soundgoodizerfan
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Assets under src/main/resources/assets are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.core.multiblock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;

/**
 * Patterns from {@code data/<ns>/multiblock/<path>.json}:
 * <pre>{"controller": "C", "legend": {"A": "#c:storage_blocks/iron", "B": "role:tray"},
 *  "layers": [["AAA", "ACA", "AAA"], ["BBB", " B ", "   "]]}</pre>
 * Layers run bottom to top, rows are z, columns x, a space matches anything, the controller
 * character marks the controller's own cell (exactly once, needs no legend entry). Legend values
 * are a block tag ({@code #ns:tag}) or a part role ({@code role:name}) -- never a block id.
 * A bad file is logged and skipped.
 */
public class MultiblockPatterns extends SimpleJsonResourceReloadListener {

    public static final String DIRECTORY = "multiblock";
    private static final MultiblockPatterns INSTANCE = new MultiblockPatterns();

    private Map<ResourceLocation, PatternAssembler.Pattern> patterns = Map.of();

    private MultiblockPatterns() {
        super(new GsonBuilder().create(), DIRECTORY);
    }

    public static MultiblockPatterns get() {
        return INSTANCE;
    }

    @Nullable
    public PatternAssembler.Pattern find(ResourceLocation id) {
        return patterns.get(id);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> json, ResourceManager resources, ProfilerFiller profiler) {
        Map<ResourceLocation, PatternAssembler.Pattern> loaded = new HashMap<>();
        json.forEach((id, element) -> {
            try {
                loaded.put(id, parse(element.getAsJsonObject()));
            } catch (RuntimeException e) {
                Feedback.LOGGER.error("Bad multiblock pattern {}: {}", id, e.getMessage());
            }
        });
        patterns = Map.copyOf(loaded);
        Feedback.LOGGER.info("Loaded {} multiblock patterns", patterns.size());
    }

    private static PatternAssembler.Pattern parse(JsonObject o) {
        String ctl = GsonHelper.getAsString(o, "controller");
        if (ctl.length() != 1)
            throw new IllegalArgumentException("controller must be one character");
        Map<Character, PatternAssembler.Cell> legend = new HashMap<>();
        for (var e : GsonHelper.getAsJsonObject(o, "legend").entrySet()) {
            String v = e.getValue().getAsString();
            if (e.getKey().length() != 1)
                throw new IllegalArgumentException("legend key must be one character: " + e.getKey());
            legend.put(e.getKey().charAt(0), v.startsWith("#")
                    ? new PatternAssembler.Cell(TagKey.create(Registries.BLOCK, ResourceLocation.parse(v.substring(1))), null)
                    : v.startsWith("role:") ? new PatternAssembler.Cell(null, v.substring(5))
                            : fail("legend value must be #tag or role:name, got " + v));
        }
        List<List<String>> layers = new ArrayList<>();
        for (JsonElement l : GsonHelper.getAsJsonArray(o, "layers")) {
            List<String> rows = new ArrayList<>();
            l.getAsJsonArray().forEach(r -> rows.add(r.getAsString()));
            layers.add(rows);
        }
        int cy = -1, cz = -1, cx = -1;
        for (int y = 0; y < layers.size(); y++)
            for (int z = 0; z < layers.get(y).size(); z++) {
                int x = layers.get(y).get(z).indexOf(ctl.charAt(0));
                if (x < 0)
                    continue;
                if (cy >= 0)
                    throw new IllegalArgumentException("controller appears more than once");
                cy = y;
                cz = z;
                cx = x;
            }
        if (cy < 0)
            throw new IllegalArgumentException("controller character not in layers");
        List<PatternAssembler.Entry> entries = new ArrayList<>();
        for (int y = 0; y < layers.size(); y++)
            for (int z = 0; z < layers.get(y).size(); z++) {
                String row = layers.get(y).get(z);
                for (int x = 0; x < row.length(); x++) {
                    char c = row.charAt(x);
                    if (c == ' ' || c == ctl.charAt(0))
                        continue;
                    PatternAssembler.Cell cell = legend.get(c);
                    if (cell == null)
                        throw new IllegalArgumentException("no legend entry for '" + c + "'");
                    entries.add(new PatternAssembler.Entry(x - cx, y - cy, z - cz, cell));
                }
            }
        return new PatternAssembler.Pattern(entries);
    }

    private static PatternAssembler.Cell fail(String msg) {
        throw new IllegalArgumentException(msg);
    }
}
