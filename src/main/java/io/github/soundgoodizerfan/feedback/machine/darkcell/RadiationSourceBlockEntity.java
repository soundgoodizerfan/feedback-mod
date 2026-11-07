// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.darkcell;

import io.github.soundgoodizerfan.feedback.core.hazard.Radiation;
import io.github.soundgoodizerfan.feedback.core.hazard.RadiationEmitter;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FAutomation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The loaded source. It is a real {@link RadiationEmitter}: standing near it costs a dose, shielded as any source is. Against mobs it
 * works on every living thing; what it kills comes out contaminated.
 */
public class RadiationSourceBlockEntity extends BlockEntity implements KillMethod, RadiationEmitter {

    private ItemStack source = ItemStack.EMPTY;

    public RadiationSourceBlockEntity(BlockPos pos, BlockState state) {
        super(FAutomation.RADIATION_SOURCE_ENTITY.get(), pos, state);
    }

    public ItemStack source() {
        return source;
    }

    public void load(ItemStack one) {
        source = one;
        setChanged();
    }

    public ItemStack unload() {
        ItemStack out = source;
        source = ItemStack.EMPTY;
        setChanged();
        return out;
    }

    private float activity() {
        return source.isEmpty() ? 0f : Radiation.activity(source, MaterialTable.get().materials());
    }

    @Override
    public float radiationRu() {
        return activity();
    }

    @Override
    public KillKind kind() {
        return KillKind.RADIATION;
    }

    @Override
    public boolean ready() {
        return activity() > 0f;
    }

    @Override
    public float damage(MobFacts facts) {
        return Math.min(40f, (float) Math.sqrt(activity()) * 4f);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Source", source.saveOptional(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        source = ItemStack.parseOptional(registries, tag.getCompound("Source"));
    }
}
