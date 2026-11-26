// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.starburner;

import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.core.thermal.HeatSource;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.machine.MachineTuning;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A Nether Star seated against a vessel: a {@link HeatSource} that burns at {@link MachineTuning#STAR_BURNER_TU} and cannot be dimmed, switched
 * or fed (roadmap 6.8: a star is a reactor-type object, a heat source you can shield, divert and cool, never stop). It decays very slowly: after
 * {@link MachineTuning#STAR_BURNER_LIFE_TICKS} loaded ticks it is spent and the seat goes cold. Like the arc, it reaches every side of the
 * face it points at. Nothing here is held but the burn so far and whether a star sits in it.
 */
public class StarBurnerBlockEntity extends BlockEntity implements HeatSource {

    private boolean seated;
    private long burned;

    public StarBurnerBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.STAR_BURNER.get(), pos, state);
    }

    public boolean seated() {
        return seated;
    }

    /** Seat a star; false if one already burns here. */
    public boolean seat() {
        if (seated)
            return false;
        seated = true;
        burned = 0L;
        setChanged();
        return true;
    }

    /** Take the star back: a star with the burn so far is a star, whole, however long it has run. */
    public ItemStack unseat() {
        seated = false;
        setChanged();
        return new ItemStack(Items.NETHER_STAR);
    }

    /** How much of the star's life is left, 0 to 1, for the readout. */
    public float remaining() {
        return seated ? 1f - (float) burned / MachineTuning.STAR_BURNER_LIFE_TICKS : 0f;
    }

    public void tickServer() {
        if (!seated || level == null)
            return;
        HeatSource.radiate(this);
        if (++burned >= MachineTuning.STAR_BURNER_LIFE_TICKS) {
            seated = false;
            setChanged();
        }
    }

    @Override
    public Tu getFireTu() {
        return seated ? MachineTuning.STAR_BURNER_TU : AmbientTable.at(level);
    }

    @Override
    public Direction heatFacing() {
        return HeatSource.facingOf(getBlockState());
    }

    /** A star radiates every way: it reaches every side of the face it is set to. */
    @Override
    public float sideShare() {
        return 1f;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Seated", seated);
        tag.putLong("Burned", burned);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        seated = tag.getBoolean("Seated");
        burned = tag.getLong("Burned");
    }
}
