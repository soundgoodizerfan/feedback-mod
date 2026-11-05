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
package io.github.soundgoodizerfan.feedback.machine.cutter;

import java.util.List;
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricRules;
import io.github.soundgoodizerfan.feedback.core.unit.VoltageBand;
import io.github.soundgoodizerfan.feedback.machine.HeadSlot;
import io.github.soundgoodizerfan.feedback.machine.Headed;
import io.github.soundgoodizerfan.feedback.machine.Wearing;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A powered cutter: it applies a cut to whatever is in its input buffer for as long as it has input and power, and knows no recipe.
 *
 * <h2>The stick is the knowledge</h2>
 * A recipe data stick in its slot says which cut to make on stock of the class it was recorded on. Anything else gets a
 * seeded pseudo-random legal cut ({@link CutterCuts#seeded}), and an input the head cannot cut at all is voided: consumed, nothing out.
 * Voiding only: the cutter never jams on junk (an [OPEN] ruling the spec left; this is its default).
 *
 * <h2>The head wears only from misuse</h2>
 * Stock harder than the head's material, running with the output full, or a supply past its EHV rating each cost the head condition;
 * a correct cut never does. A worn head cuts slower (condition scales the rate) and never breaks or stops. It does nothing without
 * input, and does not tick work when idle.
 */
public class PowerCutterBlockEntity extends ElectricNode implements Headed, Wearing {

    private final HeadSlot head = new HeadSlot();
    private ItemStack stick = ItemStack.EMPTY;
    private ItemStack input = ItemStack.EMPTY;
    private ItemStack output = ItemStack.EMPTY;
    private float progress;
    private float rate;
    private float over;

    public PowerCutterBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.POWER_CUTTER.get(), pos, state);
    }

    // --- sink ---------------------------------------------------------------------------------

    @Override
    public Role role() {
        return Role.SINK;
    }

    @Override
    public float wantedAmps() {
        return working() ? FTuning.CUTTER_AMPS : 0f;
    }

    @Override
    public VoltageBand ratedBand() {
        return VoltageBand.EHV;
    }

    @Override
    public void onResolved(float volts, float amps, float wanted, boolean blocked) {
        super.onResolved(volts, amps, wanted, blocked);
        rate = working() ? ElectricRules.rate(volts, amps, wanted, VoltageBand.EHV) : 0f;
        over = ElectricRules.overVoltage(volts, VoltageBand.EHV);
    }

    private boolean working() {
        return !head.isEmpty() && !input.isEmpty();
    }

    // --- the tick -----------------------------------------------------------------------------

    public void tickServer() {
        if (!(level instanceof ServerLevel server) || !working() || rate <= 0f)
            return;
        progress += rate * head.condition();
        if (progress < FTuning.CUTTER_TICKS_PER_CUT)
            return;
        progress -= FTuning.CUTTER_TICKS_PER_CUT;
        cut(server);
    }

    private void cut(ServerLevel server) {
        if (!(head.get().getItem() instanceof CutterHeadItem headItem))
            return;
        if (over > 0f)
            wear(over * FTuning.CUTTER_OVERVOLT_WEAR_UNITS);
        Optional<Material.Located> found = Material.find(ClientMaterials.on(server), input);
        if (found.isEmpty()) {
            consume(1);
            return;
        }
        Material.Located what = found.get();
        float stock = what.material().hardness().value();
        float edge = CutterHeadItem.hardness(head.get());
        // Stock the head cannot take is not cut: the force goes into the head, and the piece is spoiled.
        if (stock > edge) {
            wear(stock / Math.max(1f, edge));
            consume(1);
            return;
        }
        List<CutterCuts.Cut> legal = CutterCuts.legal(server, input, what, headItem.type());
        if (legal.isEmpty()) {
            consume(1);
            return;
        }
        CutterCuts.Cut chosen = CutterCuts.recorded(server, legal, what, stick.get(FDataComponents.RECORDED_CUT.get()))
                .orElseGet(() -> CutterCuts.seeded(server, legal, what, headItem.type()));
        if (input.getCount() < chosen.inputs())
            return; // waits for more stock: a log's cut needs a whole log
        if (!fitsOutput(chosen.result())) {
            wear(1f); // running against a full output is misuse
            progress = FTuning.CUTTER_TICKS_PER_CUT; // retries next tick rather than spending a cycle
            return;
        }
        consume(chosen.inputs());
        addOutput(chosen.result());
        if (!chosen.spare().isEmpty() && fitsOutput(chosen.spare()))
            addOutput(chosen.spare());
        sync();
    }

    private void consume(int n) {
        input.shrink(n);
        if (input.isEmpty())
            input = ItemStack.EMPTY;
        sync();
    }

    private boolean fitsOutput(ItemStack add) {
        return output.isEmpty() || ItemStack.isSameItemSameComponents(output, add)
                && output.getCount() + add.getCount() <= output.getMaxStackSize();
    }

    private void addOutput(ItemStack add) {
        if (output.isEmpty())
            output = add.copy();
        else
            output.grow(add.getCount());
    }

    private void wear(float units) {
        setChanged();
        if (head.wear(units, FTuning.CUTTER_WEAR_PER_MISUSE, FTuning.CUTTER_CONDITION_FLOOR))
            sync();
    }

    // --- contents -----------------------------------------------------------------------------

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

    public ItemStack getStick() {
        return stick;
    }

    /** Seats one of {@code stack}; the stick it replaces comes back. */
    public ItemStack seatStick(ItemStack stack) {
        ItemStack old = stick;
        stick = stack.split(1);
        sync();
        return old;
    }

    public ItemStack removeStick() {
        ItemStack old = stick;
        stick = ItemStack.EMPTY;
        sync();
        return old;
    }

    public ItemStack getInput() {
        return input;
    }

    public ItemStack getOutput() {
        return output;
    }

    /** Takes as much of {@code stack} as the input buffer holds; the rest comes back. Anything is taken: the cutter does not look. */
    public ItemStack insertInput(ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || !input.isEmpty() && !ItemStack.isSameItemSameComponents(input, stack))
            return stack;
        int room = Math.min(FTuning.CUTTER_INPUT_LIMIT, stack.getMaxStackSize()) - input.getCount();
        int moved = Math.min(room, stack.getCount());
        if (moved <= 0)
            return stack;
        if (!simulate) {
            if (input.isEmpty())
                input = stack.copyWithCount(moved);
            else
                input.grow(moved);
            sync();
        }
        return stack.copyWithCount(stack.getCount() - moved);
    }

    public ItemStack takeOutput(int amount, boolean simulate) {
        if (output.isEmpty() || amount <= 0)
            return ItemStack.EMPTY;
        ItemStack out = output.copyWithCount(Math.min(amount, output.getCount()));
        if (!simulate) {
            output.shrink(out.getCount());
            if (output.isEmpty())
                output = ItemStack.EMPTY;
            sync();
        }
        return out;
    }

    // --- Wearing ------------------------------------------------------------------------------

    @Override
    public float getCondition() {
        return head.condition();
    }

    @Override
    public boolean isRunning() {
        return working() && rate > 0f;
    }

    // --- persistence --------------------------------------------------------------------------

    public void sync() {
        setChanged();
        if (level != null && !level.isClientSide)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        head.save(tag, registries);
        tag.put("Stick", stick.saveOptional(registries));
        tag.put("Input", input.saveOptional(registries));
        tag.put("Output", output.saveOptional(registries));
        tag.putFloat("Progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        head.load(tag, registries);
        stick = ItemStack.parseOptional(registries, tag.getCompound("Stick"));
        input = ItemStack.parseOptional(registries, tag.getCompound("Input"));
        output = ItemStack.parseOptional(registries, tag.getCompound("Output"));
        progress = tag.getFloat("Progress");
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
