// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.skill;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/**
 * Loads every {@link SkillTree} from {@code data/<ns>/skill_tree/*.json}. The loaded list is also what
 * the client holds after {@link SkillTreeSyncPayload} (the client needs it to predict a roll and to draw
 * the screen); on a single-player world both sides share this JVM and the same data.
 */
public class SkillTrees extends SimpleJsonResourceReloadListener {

    public static final String DIRECTORY = "skill_tree";

    private static final SkillTrees INSTANCE = new SkillTrees();

    private List<SkillTree> trees = List.of();

    private SkillTrees() {
        super(new GsonBuilder().create(), DIRECTORY);
    }

    public static SkillTrees get() {
        return INSTANCE;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> json, ResourceManager resources, ProfilerFiller profiler) {
        List<SkillTree> loaded = new ArrayList<>();
        json.forEach((id, element) -> SkillTree.CODEC.parse(JsonOps.INSTANCE, element)
                .resultOrPartial(error -> Feedback.LOGGER.error("Bad skill tree {}: {}", id, error))
                .ifPresent(loaded::add));
        loaded.sort(Comparator.comparing(SkillTree::tree));
        trees = List.copyOf(loaded);
        Feedback.LOGGER.info("Loaded {} skill trees", trees.size());
    }

    public List<SkillTree> trees() {
        return trees;
    }

    public void set(List<SkillTree> incoming) {
        trees = List.copyOf(incoming);
    }

    public Optional<SkillTree> find(String tree) {
        return trees.stream().filter(t -> t.tree().equals(tree)).findFirst();
    }
}
