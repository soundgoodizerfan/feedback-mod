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
package io.github.soundgoodizerfan.feedback.machine.hammer;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
import io.github.soundgoodizerfan.feedback.machine.HeadSlot;
import io.github.soundgoodizerfan.feedback.machine.Headed;
import io.github.soundgoodizerfan.feedback.machine.Wearing;
import io.github.soundgoodizerfan.feedback.machine.WorkpieceHolder;
import io.github.soundgoodizerfan.feedback.machine.linkage.CrankLinkageBlockEntity;
import io.github.soundgoodizerfan.feedback.machine.linkage.Reciprocating;
import io.github.soundgoodizerfan.feedback.machine.linkage.StrengthPair;
import io.github.soundgoodizerfan.feedback.machine.linkage.Throw;
import io.github.soundgoodizerfan.feedback.process.Deforming;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.core.unit.Su;
import io.github.soundgoodizerfan.feedback.core.unit.St;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Hits whatever is in front of it, forever.
 *
 * <h2>The whole mod, in one block</h2>
 * The hammer has no recipe list and no notion of completion. It delivers {@code Fu} at whatever
 * {@code St} its linkage provides, and it keeps doing that for as long as it is driven. When the
 * workpiece has taken enough work it becomes the next thing along the same physical axis -- and
 * then the hammer carries straight on working <em>that</em>.
 * <p>
 * Copper ingot to plate is the process. Plate to foil is the same process, not a malfunction:
 * foil is a real material the thermometer needs, so the first overrun a player meets is a
 * sidegrade rather than a punishment. Only past foil is there scrap.
 * <p>
 * Nothing here decides any of that. The chain lives in {@link io.github.soundgoodizerfan.feedback.process.DeformationTable} as a property of
 * the material, and the hammer never consults it for anything except "how much work does this
 * need and how hard must I hit it".
 *
 * <h2>The head is the hammer</h2>
 * The St of a blow is the seated head's (its material, mass and shape), never the machine's: the crank
 * setting picks a share of the head's ceiling and gearing multiplies that, up to the ceiling. The head is also
 * what wears. With none seated the hammer does nothing, and wears nothing.
 */
public class MechanicalHammerBlockEntity extends BlockEntity implements Reciprocating, StrengthPair<St>, Wearing, WorkpieceHolder, Headed {

    private ItemStack workpiece = ItemStack.EMPTY;

    /**
     * The seated head; its condition falls from 1 to {@link FTuning#HAMMER_CONDITION_FLOOR}.
     *
     * <h3>Why wear scales both figures and not just the ceiling</h3>
     * Scaling only the ceiling was the first version and it punished exactly one mistake:
     * over-gearing. A hammer battered by beating cold steel went on hitting at its full 12 St,
     * which made two thirds of the wear invisible. Scaling {@link #getStrength} as well means a
     * worn head delivers less of every blow, which is what a mushroomed face actually does.
     * <p>
     * Gearing can then partly compensate for a worn hammer -- up to the ceiling, which has fallen
     * too -- and that is a genuine trade rather than a loophole. Recovering force by gearing costs
     * cogs and speed, and drives the machine nearer a ceiling it can no longer take.
     */
    private final HeadSlot head = new HeadSlot();

    public MechanicalHammerBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.MECHANICAL_HAMMER.get(), pos, state);
    }

    public ItemStack getWorkpiece() {
        return workpiece;
    }

    public ItemStack removeWorkpiece() {
        ItemStack taken = workpiece;
        workpiece = ItemStack.EMPTY;
        if (level != null)
            ItemHeat.settle(taken, level);
        sync();
        return taken;
    }

    public boolean insert(ItemStack stack) {
        if (!workpiece.isEmpty() || stack.isEmpty())
            return false;
        workpiece = stack.split(1);
        sync();
        return true;
    }

    @Override
    public HeadSlot getHead() {
        return head;
    }

    /** Seats one of {@code stack}; the head it replaces comes back. */
    public ItemStack seatHead(ItemStack stack) {
        ItemStack old = head.swap(stack);
        sync();
        return old;
    }

    public ItemStack removeHead() {
        ItemStack taken = head.take();
        sync();
        return taken;
    }

    /** How sound the head is, 1 down to {@link FTuning#HAMMER_CONDITION_FLOOR}. */
    @Override
    public float getCondition() {
        return head.condition();
    }

    @Override
    public boolean isRunning() {
        return CrankLinkageBlockEntity.isDriving(level, worldPosition);
    }

    @Override
    public St getStrength(Throw installed) {
        float share = installed == Throw.SHORT ? FTuning.HAMMER_THROW_SHARE_SHORT : FTuning.HAMMER_THROW_SHARE_LONG;
        return new St(head.ceiling().value() * share);
    }

    @Override
    public St getMaxStrength() {
        return head.ceiling();
    }

    @Override
    public Su getLoadSu() {
        return FTuning.HAMMER_LOAD_SU;
    }

    @Override
    public void onStroke(Throw installed, float gearAdvantage) {
        if (level == null || level.isClientSide || head.isEmpty())
            return;

        playBlow();

        // The ceiling is enforced here rather than in the linkage, and the surplus is not
        // discarded -- it is absorbed. Gearing past what the head can take now makes the head
        // worse at taking it, so the limit is discoverable by watching the machine instead of by
        // reading the source.
        float raw = getStrength(installed).value() * gearAdvantage;
        float ceiling = getMaxStrength().value();
        float strength = Math.min(raw, ceiling);
        if (raw > ceiling)
            wear(raw - ceiling);

        // A hammer beating air takes nothing, deliberately. Nothing resists it, so there is no
        // shock to do the damage -- and making an idle hammer wear would turn wear into an uptime
        // tax, which is the GregTech maintenance model this deliberately is not. Running empty is
        // already answered by overrun: leave the plate there and it becomes foil, then scrap. The
        // punishment for walking away belongs to the workpiece, not the machine.
        if (workpiece.isEmpty())
            return;

        // The blow itself is in Deforming, because a player swinging a hand hammer in a crafting
        // grid has to land exactly the same blow. What is left here is the half that is genuinely
        // the machine's: a ceiling, a sound, and somewhere for the force to go when it misses.
        //
        // This is where the two beats meet. A hot ingot is on a clock from the moment it leaves
        // the crucible, so the distance between fire and anvil became a decision nobody installed,
        // and the dead time between blows became the most expensive thing in the build. The
        // brute-force answer is a faster hammer and the Su to sustain it; the other is two hammers
        // and a player willing to shuffle hot metal between them. Capital against attention, in a
        // third place nobody put it (§5).
        Deforming.Blow blow = Deforming.strike(workpiece, new St(strength), level);

        // Every way a blow can accomplish nothing lands here, and they all cost the same thing.
        // The slice always said the hammer wears on metal too stiff to move and it never did until
        // the outcomes were named: the blow is landing on something that will not give, so all of
        // it comes back up the handle.
        if (!blow.landed()) {
            wear(strength);
            return;
        }

        workpiece = blow.result();
        sync();
    }

    /**
     * Force that went into the machine instead of the work.
     *
     * <h3>One method, because it is one rule</h3>
     * Every way a blow can accomplish nothing ends up here -- too cold, too soft a blow, too hard
     * a blow, or a workpiece with nothing further to become. Writing a separate consequence for
     * each was the obvious shape and it would have been four balance decisions pretending to be
     * physics. There is one physical fact instead: the energy is conserved and the head is what
     * absorbs it, so a mistake that wastes more force wears the machine faster, with nothing
     * needing to know which mistake it was.
     */
    public void absorb(float stAbsorbed) {
        wear(stAbsorbed);
    }

    private void wear(float stAbsorbed) {
        if (stAbsorbed <= 0)
            return;

        setChanged();
        // Sync only when the adjective would change. Wear happens on every wasted blow and the
        // readout has four states in the life of the machine; syncing per blow would be a packet
        // several times a second to say nothing new.
        if (head.wear(stAbsorbed, FTuning.HAMMER_WEAR_PER_ST, FTuning.HAMMER_CONDITION_FLOOR))
            sync();
    }

    private void playBlow() {
        if (level == null)
            return;
        level.playSound(null, worldPosition, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.15f, 1.6f);
    }

    public void sync() {
        setChanged();
        if (level != null && !level.isClientSide)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    /**
     * The key is written even when the hammer is empty, and that is load-bearing.
     * <p>
     * NeoForge's {@code onDataPacket} discards an update tag that is completely empty:
     * <pre>if (!compoundtag.isEmpty()) self().loadWithComponents(...)</pre>
     * Omitting the key when there is nothing to save produced exactly that -- an empty tag -- so
     * the packet announcing "the workpiece is gone" was thrown away and the client went on
     * rendering an item that no longer existed.
     */
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Workpiece", workpiece.saveOptional(registries));
        head.save(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        workpiece = ItemStack.parseOptional(registries, tag.getCompound("Workpiece"));
        head.load(tag, registries);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
