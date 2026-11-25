// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.cog;

import io.github.soundgoodizerfan.feedback.machine.MachineTuning;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationNode;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.core.unit.Drag;
import io.github.soundgoodizerfan.feedback.core.unit.Inertia;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A cog on the network. Carries no gearing logic at all -- the ratio is a property of how two
 * cogs are placed and of their tooth counts (a block state), which is {@link CogBlock}'s business and the
 * propagator's.
 * <p>
 * One block entity type serves both footprints. It holds the one thing a block state cannot: the metal the
 * cog was cut from. A heavier metal makes a heavier wheel to spin up (inertia follows the material's
 * density), and a cog with more teeth drags more. A wooden cog (no metal) is the block's own base figures.
 */
public class CogBlockEntity extends RotationNode {

    /** The metal this cog was cut from; null for the plain wooden cog. */
    private ResourceLocation metal;

    public CogBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.COG.get(), pos, state);
    }

    public ResourceLocation metal() {
        return metal;
    }

    private Material material() {
        if (metal == null)
            return null;
        return ClientMaterials.on(level).get(metal);
    }

    @Override
    public Drag getDragSuPerRpm() {
        if (!(getBlockState().getBlock() instanceof CogBlock cog))
            return MachineTuning.SMALL_COG_DRAG_SU_PER_RPM;
        // More teeth on the same footprint is more mesh to drag through: in proportion to the block's own count.
        return new Drag(cog.getDragSuPerRpm().suPerRpm() * cog.teeth(getBlockState()) / cog.getTeeth());
    }

    @Override
    public Inertia getInertia() {
        if (!(getBlockState().getBlock() instanceof CogBlock cog))
            return MachineTuning.SMALL_COG_INERTIA;
        Material m = level == null ? null : material();
        if (m == null)
            return cog.getInertia();
        return new Inertia(cog.getInertia().suTicksPerRpm() * m.density().muPerMb() / MachineTuning.COG_REFERENCE_DENSITY);
    }

    // The metal rides on the item (a cut cog's component), so mining a cog and placing it again keeps it.

    @Override
    protected void applyImplicitComponents(BlockEntity.DataComponentInput input) {
        super.applyImplicitComponents(input);
        CogSpec spec = input.get(FDataComponents.COG.get());
        metal = spec == null ? null : spec.metal();
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        if (metal != null)
            builder.set(FDataComponents.COG.get(), new CogSpec(metal, ((CogBlock) getBlockState().getBlock()).teeth(getBlockState())));
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        super.removeComponentsFromTag(tag);
        tag.remove("Metal");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (metal != null)
            tag.putString("Metal", metal.toString());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        metal = tag.contains("Metal") ? ResourceLocation.tryParse(tag.getString("Metal")) : null;
    }
}
