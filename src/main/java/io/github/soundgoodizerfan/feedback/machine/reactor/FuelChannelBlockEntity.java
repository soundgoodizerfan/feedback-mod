// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.reactor;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.hazard.Radiation;
import io.github.soundgoodizerfan.feedback.core.hazard.RadiationEmitter;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.registry.FItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * One lattice cell holding a fuel rod. The rod's neutron figures come from its material ({@code fissile}, {@code fertile}); a
 * thorium rod breeds, so its effective fissile rises over its life and it burns slower. The core calls {@link #advance} with the
 * running power; at full burnup the rod becomes a spent rod, radioactive and inert. Burnup and condition ride on the stack when it
 * is taken out and put back.
 */
public class FuelChannelBlockEntity extends BlockEntity implements RadiationEmitter {

    private ItemStack rod = ItemStack.EMPTY;
    private float burn;
    private float condition = 1f;
    private Material.Neutron neutron;
    private boolean neutronKnown;
    private float spectrum = 1f;

    public FuelChannelBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.FUEL_CHANNEL.get(), pos, state);
    }

    public boolean hasRod() {
        return !rod.isEmpty();
    }

    public ItemStack rod() {
        return rod;
    }

    public float burn() {
        return burn;
    }

    public float condition() {
        return condition;
    }

    /** The rod's neutron figures, looked up once and kept until the rod changes. */
    private Material.Neutron neutron() {
        if (!neutronKnown) {
            neutron = rod.isEmpty() ? null : Neutrons.of(rod).orElse(null);
            neutronKnown = true;
        }
        return neutron;
    }

    /** Whether the rod can still make power: it has a fissile figure (a spent rod has none). */
    public boolean live() {
        Material.Neutron n = neutron();
        return n != null && n.fissile() > 0f;
    }

    /** The multiplication this rod gives on its own, 0 for none: fissile plus what breeding has added, scaled by condition. */
    public float fissile() {
        Material.Neutron n = neutron();
        if (n == null)
            return 0f;
        // A fast breeder rod breeds well only in a hard spectrum: moderator or flowing coolant beside it softens that away.
        float breed = n.fast() ? FTuning.REACTOR_FAST_BREED_GAIN * spectrum : FTuning.REACTOR_BREED_GAIN;
        return (n.fissile() + n.fertile() * burn * breed) * condition;
    }

    /** The core's reading of what slows the neutrons at this cell: 1 for a bare cell, less when a moderator or flowing coolant touches it. */
    public void spectrum(float share) {
        spectrum = share;
    }

    /** Puts a rod in if the channel is empty, carrying its burnup and condition from the stack. */
    public boolean insert(ItemStack stack) {
        if (!rod.isEmpty() || stack.isEmpty() || Neutrons.of(stack).map(n -> n.fissile() <= 0f).orElse(true))
            return false;
        rod = stack.copyWithCount(1);
        neutronKnown = false;
        stack.shrink(1);
        CompoundTag tag = rod.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        burn = tag.contains("Burn") ? tag.getFloat("Burn") : 0f;
        condition = tag.contains("Condition") ? tag.getFloat("Condition") : 1f;
        setChanged();
        return true;
    }

    /** Takes the rod out, stamped with how burnt and damaged it is. */
    public ItemStack take() {
        ItemStack out = rod;
        rod = ItemStack.EMPTY;
        if (!out.isEmpty()) {
            CompoundTag tag = out.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            tag.putFloat("Burn", burn);
            tag.putFloat("Condition", condition);
            out.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
        burn = 0f;
        condition = 1f;
        neutronKnown = false;
        setChanged();
        return out;
    }

    /** One tick at relative power {@code power}: burn the rod, turn it spent at full burnup. */
    public void advance(float power) {
        if (!live() || power <= 0f)
            return;
        float fertile = neutron().fertile();
        burn += power * FTuning.REACTOR_BURN_PER_TICK / (1f + fertile);
        if (burn >= 1f) {
            ItemStack spent = new ItemStack(FItems.SPENT_FUEL_ROD.get());
            if (level != null) {
                CompoundTag stamp = new CompoundTag();
                stamp.putLong("Discharged", level.getGameTime());
                spent.set(DataComponents.CUSTOM_DATA, CustomData.of(stamp));
            }
            rod = spent;
            neutronKnown = false;
            burn = 1f;
        }
        setChanged();
    }

    /** Fuel damage: condition falls, to a floor the rod never leaves (it just stops being worth much). */
    public void damage(float amount) {
        if (amount <= 0f || rod.isEmpty())
            return;
        condition = Math.max(FTuning.REACTOR_FUEL_CONDITION_FLOOR, condition - amount);
        setChanged();
    }

    @Override
    public float radiationRu() {
        return rod.isEmpty() ? 0f : Radiation.activity(rod, Neutrons.materials());
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!rod.isEmpty())
            tag.put("Rod", rod.save(registries));
        tag.putFloat("Burn", burn);
        tag.putFloat("Condition", condition);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        rod = tag.contains("Rod") ? ItemStack.parseOptional(registries, tag.getCompound("Rod")) : ItemStack.EMPTY;
        neutronKnown = false;
        burn = tag.getFloat("Burn");
        condition = tag.contains("Condition") ? tag.getFloat("Condition") : 1f;
    }
}
