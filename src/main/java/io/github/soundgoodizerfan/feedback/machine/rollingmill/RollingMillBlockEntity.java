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
package io.github.soundgoodizerfan.feedback.machine.rollingmill;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.unit.St;
import io.github.soundgoodizerfan.feedback.core.unit.Su;
import io.github.soundgoodizerfan.feedback.machine.ShopMachineBlockEntity;
import io.github.soundgoodizerfan.feedback.process.Deforming;
import io.github.soundgoodizerfan.feedback.process.Operation;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.process.Plating;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A driven roller pair. Same shape as {@code MechanicalHammerBlockEntity}'s plate-making, through
 * {@link Operation#ROLL}: one pass of {@link FTuning#ROLLING_MILL_ST} per {@link #strokesDue}, the
 * same {@link Deforming#apply} the Hammer and Wire Drawer already share. A pass that cannot move
 * the metal is absorbed by the rolls.
 * <p>
 * Exists alongside the Hammer for a real reason, not a cosmetic one: a mill geared correctly runs
 * unattended off the network, where the Hammer needs either a Clutch or a hand on the crank --
 * capital against attention (philosophy §5), not a second way to make the same plate.
 * <p>
 * <b>Roll cladding.</b> A plate laid in the second slot is a skin; the next stroke on a plate
 * workpiece bonds it on as a {@link Plating} coat of the skin's metal. Both must be past their anneal
 * point (a cold roll joins nothing: the mill holds off and hints); a skin rolled past
 * {@link FTuning#CLAD_OVERRUN_MELT_SHARE} of its melting point comes out burnt.
 */
public class RollingMillBlockEntity extends ShopMachineBlockEntity {

    private static final Operation OPERATION = Operation.ROLL;

    private ItemStack cladding = ItemStack.EMPTY;

    public RollingMillBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.ROLLING_MILL.get(), pos, state);
    }

    @Override
    public Su getLoadSu() {
        return FTuning.ROLLING_MILL_LOAD_SU;
    }

    public ItemStack getCladding() {
        return cladding;
    }

    public boolean insertCladding(ItemStack stack) {
        if (!cladding.isEmpty() || !isPlate(stack))
            return false;
        cladding = stack.split(1);
        sync();
        return true;
    }

    public ItemStack removeCladding() {
        ItemStack taken = cladding;
        cladding = ItemStack.EMPTY;
        if (level != null)
            ItemHeat.settle(taken, level);
        sync();
        return taken;
    }

    private static Material.Located plate(ItemStack stack) {
        return Material.find(MaterialTable.get().materials(), stack).filter(l -> l.form().equals("plate")
                && l.material().kind().filter(k -> k == Material.Kind.METAL).isPresent()).orElse(null);
    }

    private static boolean isPlate(ItemStack stack) {
        return !stack.isEmpty() && plate(stack) != null;
    }

    /** Solid-state bonding starts at recrystallisation: the anneal point, else half the melting point. */
    private static float bondFloor(Material m) {
        return m.thermal().annealTemperature().map(t -> t.value())
                .orElse(m.thermal().meltingPoint().map(t -> t.value() * 0.5f).orElse(0f));
    }

    /** One stroke on a skin and a base plate: bond them, or hold off and say why. Returns whether anything changed. */
    private boolean clad() {
        Material.Located base = plate(workpiece), skin = plate(cladding);
        if (base == null || skin == null)
            return false;
        float baseTu = ItemHeat.get(workpiece, level).value(), skinTu = ItemHeat.get(cladding, level).value();
        if (baseTu < bondFloor(base.material()) || skinTu < bondFloor(skin.material())) {
            if (level.getGameTime() % 20 == 0) {
                net.minecraft.world.entity.player.Player near = level.getNearestPlayer(
                        worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, 6, false);
                if (near != null)
                    near.displayClientMessage(Component.translatable("feedback.rolling_mill.too_cold"), true);
            }
            return false;
        }
        boolean overrun = skin.material().thermal().meltingPoint()
                .filter(t -> skinTu >= t.value() * FTuning.CLAD_OVERRUN_MELT_SHARE).isPresent();
        ItemStack out = workpiece.copy();
        out.set(FDataComponents.PLATING.get(), new Plating(skin.id(), FTuning.CLAD_SKIN_QU,
                overrun ? Plating.Quality.BURNT : Plating.Quality.SOUND));
        ItemHeat.set(out, new io.github.soundgoodizerfan.feedback.core.unit.Tu(Math.min(baseTu, skinTu)), level);
        workpiece = out;
        cladding = ItemStack.EMPTY;
        return true;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Cladding", cladding.saveOptional(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        cladding = ItemStack.parseOptional(registries, tag.getCompound("Cladding"));
    }

    public void tickServer() {
        if (level == null || level.isClientSide)
            return;
        boolean changed = false;
        for (int due = strokesDue(); due > 0 && !workpiece.isEmpty(); due--) {
            if (!cladding.isEmpty() && plate(workpiece) != null) {
                changed |= clad();
                continue;
            }
            St strength = new St(FTuning.ROLLING_MILL_ST.value() * condition);
            Deforming.Blow blow = Deforming.apply(workpiece, strength, level, OPERATION);
            if (!blow.landed()) {
                wear(strength.value());
                continue;
            }
            workpiece = blow.result();
            changed = true;
        }
        if (changed)
            sync();
    }
}
