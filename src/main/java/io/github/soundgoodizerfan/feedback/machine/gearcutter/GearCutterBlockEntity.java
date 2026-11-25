// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.gearcutter;

import java.util.Optional;

import io.github.soundgoodizerfan.feedback.core.unit.Su;
import io.github.soundgoodizerfan.feedback.machine.MachineTuning;
import io.github.soundgoodizerfan.feedback.machine.ShopMachineBlockEntity;
import io.github.soundgoodizerfan.feedback.machine.cog.CogSpec;
import io.github.soundgoodizerfan.feedback.machine.linkage.Reciprocating;
import io.github.soundgoodizerfan.feedback.machine.linkage.Throw;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Cuts teeth into a plate. The cutting wheel spins for as long as the network drives it, which alone cuts
 * nothing; a cut happens on a {@link #push()}, fed by hand or by one stroke of a Crank Linkage, exactly as at the
 * Drill Press. A plate of metal goes in and a cog of that metal comes out, with the tooth count the cutter is
 * set to ({@link CogSpec#OPTIONS}: one shared pitch, so every count meshes with every other). The metal that
 * is not left on the cog falls as swarf, more of it for fewer teeth (a small cog is the plate with more cut away).
 * <p>
 * A plate harder than the cutter can bear is a push that cannot cut: it costs the machine, as at the other shop
 * machines, and nothing comes out. The cutter works only on a push, so it has no idle tick.
 */
public class GearCutterBlockEntity extends ShopMachineBlockEntity implements Reciprocating {

    /** The tooth count the next cut gives. */
    private int teeth = 12;

    public GearCutterBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.GEAR_CUTTER.get(), pos, state);
    }

    @Override
    public float workpieceSurface() {
        return 0.26f;
    }

    @Override
    public Su getLoadSu() {
        return MachineTuning.GEAR_CUTTER_LOAD_SU;
    }

    @Override
    public void onStroke(Throw installed, float gearAdvantage) {
        push();
    }

    public int teeth() {
        return teeth;
    }

    /** Set the tooth count of the next cut; a count that is not one of the options is refused. */
    public boolean setTeeth(int count) {
        if (!CogSpec.valid(count))
            return false;
        teeth = count;
        setChanged();
        sync();
        return true;
    }

    /** One cut: needs a plate on the table and the wheel turning. */
    public void push() {
        if (level == null || level.isClientSide || workpiece.isEmpty() || !isTurning())
            return;
        Optional<Material.Located> found = Material.find(MaterialTable.get().materials(), workpiece);
        if (found.isEmpty() || !found.get().form().equals("plate"))
            return;
        Material.Located plate = found.get();
        float hardness = plate.material().hardness().value();
        if (hardness > MachineTuning.GEAR_CUTTER_ST.value()) {
            // The wheel cannot bite: the force goes into the machine.
            wear(hardness - MachineTuning.GEAR_CUTTER_ST.value());
            return;
        }
        ItemStack cog = new ItemStack(CogSpec.isLarge(teeth) ? FItems.LARGE_COG.get() : FItems.SMALL_COG.get());
        cog.set(FDataComponents.COG.get(), new CogSpec(plate.id(), teeth));
        // What is left of the plate beyond the cog is swarf: a cog of more teeth keeps more of it.
        float kept = (float) teeth / CogSpec.OPTIONS.get(CogSpec.OPTIONS.size() - 1) * MachineTuning.GEAR_CUTTER_KEPT_AT_MOST;
        int swarf = Math.round((1f - kept) * MachineTuning.GEAR_CUTTER_SWARF_PER_PLATE);
        Optional<ItemStack> chip = plate.material().stack(plate.id(), "swarf");
        workpiece = cog;
        carriedWu = 0;
        setChanged();
        sync();
        io.github.soundgoodizerfan.feedback.core.Awards.near(level, worldPosition, io.github.soundgoodizerfan.feedback.process.Ages.MACHINE);
        level.playSound(null, worldPosition, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.6f, 1.2f);
        if (swarf > 0 && chip.isPresent()) {
            ItemStack out = chip.get().copy();
            out.setCount(swarf);
            Block.popResource(level, worldPosition.above(), out);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Teeth", teeth);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        teeth = tag.contains("Teeth") && CogSpec.valid(tag.getInt("Teeth")) ? tag.getInt("Teeth") : 12;
    }
}
