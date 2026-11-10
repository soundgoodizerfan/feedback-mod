// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular;

import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FModular;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;

/**
 * What a placed modular block keeps of the item it was built from: the baked figures ({@link BlockFigures}) it runs on, and the
 * design, so the broken block drops the same item. A block entity owns one, forwards its four persistence hooks to it, and reads
 * its figures with {@link #get}. A block placed from nothing (a creative copy, an old world) has no figures and runs on the
 * fallback each call names, which is the fixed block's old constant.
 */
public final class FigureStore {

    private CompoundTag figs = new CompoundTag();
    private ModularDesign design;
    private ModularFigures figures;

    /** The figure called {@code key}, or {@code fallback} when the block was not built from a design. */
    public float get(String key, float fallback) {
        return figs.contains(key) ? figs.getFloat(key) : fallback;
    }

    /** Takes the figures an item carried (any may be null). */
    public void set(CompoundTag tag, ModularDesign design, ModularFigures figures) {
        this.figs = tag == null ? new CompoundTag() : tag.copy();
        this.design = design;
        this.figures = figures;
    }

    /** Puts them back on the dropped item. */
    public void collect(DataComponentMap.Builder components) {
        if (!figs.isEmpty())
            components.set(FDataComponents.BLOCK_FIGURES.get(), figs.copy());
        if (design != null)
            components.set(FModular.DESIGN.get(), design);
        if (figures != null)
            components.set(FModular.FIGURES.get(), figures);
    }

    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("BlockFigures", figs.copy());
        RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
        if (design != null)
            ModularDesign.CODEC.encodeStart(ops, design).result().ifPresent(t -> tag.put("Design", t));
        if (figures != null)
            ModularFigures.CODEC.encodeStart(ops, figures).result().ifPresent(t -> tag.put("Figures", t));
    }

    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        figs = tag.contains("BlockFigures") ? tag.getCompound("BlockFigures") : new CompoundTag();
        RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
        design = tag.contains("Design") ? ModularDesign.CODEC.parse(ops, tag.get("Design")).result().orElse(null) : null;
        figures = tag.contains("Figures") ? ModularFigures.CODEC.parse(ops, tag.get("Figures")).result().orElse(null) : null;
    }
}
