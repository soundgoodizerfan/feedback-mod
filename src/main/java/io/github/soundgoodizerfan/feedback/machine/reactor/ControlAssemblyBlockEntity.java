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
package io.github.soundgoodizerfan.feedback.machine.reactor;

import io.github.soundgoodizerfan.feedback.control.Switchable;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A control rod assembly: a bundle of absorber (any item whose material states a neutron {@code absorption}) that sits in one of
 * five named positions, out (0) to in (4). It moves one step per {@code REACTOR_ROD_STEP_TICKS} toward its target, so it coasts;
 * a Controller's switch sets the target (engaged is all the way in), a hand click cycles it, and the core drops it at once on a SCRAM.
 */
public class ControlAssemblyBlockEntity extends BlockEntity implements Switchable {

    public static final int IN = 4;

    private ItemStack absorber = ItemStack.EMPTY;
    private int position;
    private int target;
    private float strength = -1f;

    public ControlAssemblyBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.CONTROL_ASSEMBLY.get(), pos, state);
    }

    public int position() {
        return position;
    }

    public int target() {
        return target;
    }

    /** 0 (out) to 1 (all the way in). */
    public float insertion() {
        return position / (float) IN;
    }

    /** What the bundle soaks up fully inserted: the material's absorption times a share of a full bundle; 0 when empty. */
    public float strength() {
        if (strength < 0f)
            strength = absorber.isEmpty() ? 0f : Neutrons.of(absorber).map(n -> n.absorption()
                    * Math.min(1f, absorber.getCount() / (float) FTuning.REACTOR_ABSORBER_FULL_BUNDLE)).orElse(0f);
        return strength;
    }

    public ItemStack absorber() {
        return absorber;
    }

    /** Puts absorber in (same material only, up to a full bundle); true if any went in. */
    public boolean load(ItemStack stack) {
        if (stack.isEmpty() || Neutrons.of(stack).map(n -> n.absorption() <= 0f).orElse(true))
            return false;
        if (!absorber.isEmpty() && !ItemStack.isSameItemSameComponents(absorber, stack))
            return false;
        int room = FTuning.REACTOR_ABSORBER_FULL_BUNDLE - absorber.getCount();
        if (room <= 0)
            return false;
        int moved = Math.min(room, stack.getCount());
        if (absorber.isEmpty())
            absorber = stack.copyWithCount(moved);
        else
            absorber.grow(moved);
        stack.shrink(moved);
        strength = -1f;
        setChanged();
        return true;
    }

    public ItemStack unload() {
        ItemStack out = absorber;
        absorber = ItemStack.EMPTY;
        strength = -1f;
        setChanged();
        return out;
    }

    /** The next named position as the hand's target: 0, 1, 2, 3, 4, back to 0. */
    public void cycleTarget() {
        target = (target + 1) % (IN + 1);
        setChanged();
    }

    /** Gravity: every rod all the way in, now. Called by the core on a SCRAM. */
    public void scram() {
        target = IN;
        position = IN;
        setChanged();
    }

    @Override
    public void setEngaged(boolean engaged) {
        int t = engaged ? IN : 0;
        if (t != target) {
            target = t;
            setChanged();
        }
    }

    @Override
    public boolean isEngaged() {
        return position == IN;
    }

    public void tickServer() {
        if (position != target && level != null && level.getGameTime() % FTuning.REACTOR_ROD_STEP_TICKS == 0) {
            position += Integer.signum(target - position);
            setChanged();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!absorber.isEmpty())
            tag.put("Absorber", absorber.save(registries));
        tag.putInt("Position", position);
        tag.putInt("Target", target);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        absorber = tag.contains("Absorber") ? ItemStack.parseOptional(registries, tag.getCompound("Absorber")) : ItemStack.EMPTY;
        position = Math.max(0, Math.min(IN, tag.getInt("Position")));
        target = Math.max(0, Math.min(IN, tag.getInt("Target")));
        strength = -1f;
    }
}
