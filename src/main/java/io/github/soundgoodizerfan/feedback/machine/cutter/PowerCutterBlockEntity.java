// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.cutter;

import io.github.soundgoodizerfan.feedback.machine.MachineTuning;
import java.util.List;
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricRules;
import io.github.soundgoodizerfan.feedback.core.unit.VoltageBand;
import io.github.soundgoodizerfan.feedback.control.ChannelSource;
import io.github.soundgoodizerfan.feedback.control.RemoteChannels;
import io.github.soundgoodizerfan.feedback.machine.CircuitSlot;
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
 * Beside the seeded cut, input the stick does not cover can go wrong (philosophy 5, early machines sit toward the bad end): it jams
 * the output (the cutter stalls until a hand clears it), wears the head as misuse, or comes out as scrap of its material. The mix is
 * {@code CUTTER_SEEDED_ODDS} / {@code CUTTER_JAM_ODDS} / {@code CUTTER_BLADE_WEAR_ODDS} / {@code CUTTER_SCRAP_ODDS}, rolled per cut.
 *
 * <h2>The head wears only from misuse</h2>
 * Stock harder than the head's material, running with the output full, or a supply past its EHV rating each cost the head condition;
 * a correct cut never does. A worn head cuts slower (condition scales the rate) and never breaks or stops. It does nothing without
 * input, and does not tick work when idle.
 *
 * <h2>The circuit slot</h2>
 * A circuit ({@link CircuitSlot}) is built in by the cutter's engineering recipe and required: without a live one the cutter does not run. Its speed (read at its
 * warmth) makes the cutter play faster; its noise scales down the jam, blade-wear and scrap odds for input the stick does not cover and now and
 * then holds off guessing. A tubed circuit's heater draws from the cutter's line. Fitted, the cutter is also a data source: a Sensor card
 * reads its cut progress, head condition and output fill.
 */
public class PowerCutterBlockEntity extends ElectricNode implements Headed, Wearing, ChannelSource, RemoteChannels, io.github.soundgoodizerfan.feedback.control.data.Amplifying {

    private final HeadSlot head = new HeadSlot();
    private ItemStack stick = ItemStack.EMPTY;
    private ItemStack input = ItemStack.EMPTY;
    private ItemStack output = ItemStack.EMPTY;
    private float progress;
    private float rate;
    private float over;
    private boolean jammed;
    private final CircuitSlot circuit = new CircuitSlot();

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
        return (working() ? MachineTuning.CUTTER_AMPS : 0f) + circuit.heaterAmps(ElectricRules.packetEu(VoltageBand.EHV));
    }

    @Override
    public VoltageBand ratedBand() {
        return VoltageBand.EHV;
    }

    @Override
    public void onResolved(VoltageBand band, float eu, float amps, float wanted, boolean blocked) {
        super.onResolved(band, eu, amps, wanted, blocked);
        rate = working() ? ElectricRules.rate(eu, amps, wanted, VoltageBand.EHV) : 0f;
        over = ElectricRules.overVoltage(band, eu, VoltageBand.EHV);
        circuit.supply(ElectricRules.rate(eu, amps, wanted, VoltageBand.EHV));
    }

    private boolean working() {
        return !head.isEmpty() && !input.isEmpty() && !jammed && circuit.live();
    }

    public boolean isJammed() {
        return jammed;
    }

    /** A hand frees the jam; true if there was one. */
    public boolean clearJam() {
        if (!jammed)
            return false;
        jammed = false;
        sync();
        return true;
    }

    // --- the tick -----------------------------------------------------------------------------

    public void tickServer() {
        if (level instanceof ServerLevel && level.getGameTime() % io.github.soundgoodizerfan.feedback.control.ControlTuning.CIRCUIT_HEAT_EVERY_TICKS == 0)
            circuit.heat(level, io.github.soundgoodizerfan.feedback.control.ControlTuning.CIRCUIT_HEAT_EVERY_TICKS);
        if (!(level instanceof ServerLevel server) || !working() || rate <= 0f)
            return;
        progress += rate * head.condition() * io.github.soundgoodizerfan.feedback.modular.attunement.EdgeKinds.headBlow(head.get()) * circuit.pace(server);
        if (progress < MachineTuning.CUTTER_TICKS_PER_CUT)
            return;
        progress -= MachineTuning.CUTTER_TICKS_PER_CUT;
        cut(server);
    }

    /**
     * Loads grit into a seated waterjet head: a piece of hard, abrasive stock (a gem or its dust, by what its material says, never an item id) is
     * ground into the jet and gives {@link MachineTuning#WATERJET_GRIT_PER_PIECE} cuts' worth. The head's grit abrasiveness becomes the average by amount.
     * Returns the stack left over, the same one if nothing was taken.
     */
    public ItemStack loadGrit(ItemStack stack, ServerLevel server) {
        if (!(head.get().getItem() instanceof CutterHeadItem h) || h.type() != CutterHeadItem.Type.WATERJET || stack.isEmpty())
            return stack;
        var found = Material.find(ClientMaterials.on(server), stack);
        if (found.isEmpty() || !(found.get().form().equals("gem") || found.get().form().equals("dust")))
            return stack;
        float abrasive = found.get().material().mechanical().abrasiveness().orElse(0f);
        int held = head.get().getOrDefault(io.github.soundgoodizerfan.feedback.registry.FWeapons.NOZZLE_GRIT.get(), 0);
        if (abrasive < MachineTuning.WATERJET_GRIT_MIN_ABRASIVENESS || held + MachineTuning.WATERJET_GRIT_PER_PIECE > MachineTuning.WATERJET_GRIT_MAX)
            return stack;
        float was = head.get().getOrDefault(io.github.soundgoodizerfan.feedback.registry.FWeapons.NOZZLE_GRIT_ABRASIVENESS.get(), abrasive);
        int now = held + MachineTuning.WATERJET_GRIT_PER_PIECE;
        head.get().set(io.github.soundgoodizerfan.feedback.registry.FWeapons.NOZZLE_GRIT.get(), now);
        head.get().set(io.github.soundgoodizerfan.feedback.registry.FWeapons.NOZZLE_GRIT_ABRASIVENESS.get(), (was * held + abrasive * MachineTuning.WATERJET_GRIT_PER_PIECE) / now);
        sync();
        ItemStack left = stack.copy();
        left.shrink(1);
        return left;
    }

    private void cut(ServerLevel server) {
        if (!(head.get().getItem() instanceof CutterHeadItem headItem))
            return;
        // A waterjet with no grit in it cannot cut: the cycle waits, the input stays.
        if (headItem.type() == CutterHeadItem.Type.WATERJET && head.get().getOrDefault(io.github.soundgoodizerfan.feedback.registry.FWeapons.NOZZLE_GRIT.get(), 0) <= 0) {
            progress = MachineTuning.CUTTER_TICKS_PER_CUT;
            return;
        }
        if (over > 0f)
            wear(over * MachineTuning.CUTTER_OVERVOLT_WEAR_UNITS);
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
        Optional<CutterCuts.Cut> told = CutterCuts.recorded(server, legal, what, stick.get(FDataComponents.RECORDED_CUT.get()));
        boolean covered = told.isPresent();
        CutterCuts.Cut chosen = told.orElseGet(() -> CutterCuts.seeded(server, legal, what, headItem.type()));
        if (!covered) {
            // A quiet circuit holds off guessing now and then (this cycle is spent, the input stays), and cuts the bad odds.
            if (circuit.declines(server, server.getRandom()))
                return;
            float bad = circuit.oddsFactor(server);
            float roll = server.getRandom().nextFloat() * (MachineTuning.CUTTER_SEEDED_ODDS + bad * (MachineTuning.CUTTER_JAM_ODDS
                    + MachineTuning.CUTTER_BLADE_WEAR_ODDS + MachineTuning.CUTTER_SCRAP_ODDS));
            if ((roll -= MachineTuning.CUTTER_SEEDED_ODDS) >= 0f) {
                if ((roll -= MachineTuning.CUTTER_JAM_ODDS * bad) < 0f) {
                    jammed = true;
                    sync();
                    return;
                }
                if ((roll -= MachineTuning.CUTTER_BLADE_WEAR_ODDS * bad) < 0f) {
                    wear(MachineTuning.CUTTER_BLADE_WEAR_UNITS);
                    consume(1);
                    return;
                }
                Optional<net.minecraft.world.item.Item> scrap = what.material().item(what.id(), "scrap");
                if (scrap.isPresent() && fitsOutput(new ItemStack(scrap.get()))) {
                    consume(1);
                    addOutput(new ItemStack(scrap.get()));
                    sync();
                    return;
                }
            }
        }
        if (input.getCount() < chosen.inputs())
            return; // waits for more stock: a log's cut needs a whole log
        if (!fitsOutput(chosen.result())) {
            wear(1f); // running against a full output is misuse
            progress = MachineTuning.CUTTER_TICKS_PER_CUT; // retries next tick rather than spending a cycle
            return;
        }
        if (headItem.type() == CutterHeadItem.Type.WATERJET) {
            // The grit is spent with the cut and wears the nozzle by how hard it is.
            head.get().set(io.github.soundgoodizerfan.feedback.registry.FWeapons.NOZZLE_GRIT.get(), Math.max(0, head.get().getOrDefault(io.github.soundgoodizerfan.feedback.registry.FWeapons.NOZZLE_GRIT.get(), 0) - 1));
            wear(MachineTuning.WATERJET_NOZZLE_WEAR_PER_ABRASIVENESS * head.get().getOrDefault(io.github.soundgoodizerfan.feedback.registry.FWeapons.NOZZLE_GRIT_ABRASIVENESS.get(), 0.5f));
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
        if (head.wear(units, MachineTuning.CUTTER_WEAR_PER_MISUSE, MachineTuning.CUTTER_CONDITION_FLOOR))
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

    @Override
    public float amplifierGain() {
        return level == null ? 0f : circuit.gain(level);
    }

    public ItemStack getCircuit() {
        return circuit.get();
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        circuit.fromComponents(input.get(io.github.soundgoodizerfan.feedback.registry.FDataComponents.CIRCUIT_STACK.get()));
    }

    @Override
    protected void collectImplicitComponents(net.minecraft.core.component.DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        circuit.toComponents(components);
    }

    /** Whether the cutter has the circuit it works through: without one it does not run. */
    public boolean hasCircuit() {
        return circuit.live();
    }

    // --- data: a fitted circuit lets a Controller read the cutter ------------------------------

    /** Channel 0: how far through the cut; 1: head condition; 2: how full the output is. */
    @Override
    public float read(int channel) {
        return switch (channel) {
            case 0 -> Math.min(1f, progress / MachineTuning.CUTTER_TICKS_PER_CUT);
            case 1 -> head.condition();
            case 2 -> output.isEmpty() ? 0f : output.getCount() / (float) output.getMaxStackSize();
            default -> 0f;
        };
    }

    @Override
    public boolean offersChannels() {
        return level != null && !circuit.isEmpty() && circuit.read(level).live();
    }

    @Override
    public String[] senseKeys() {
        return new String[] {"feedback.machine.sense.progress", "feedback.machine.sense.condition", "feedback.machine.sense.output"};
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
        int room = Math.min(MachineTuning.CUTTER_INPUT_LIMIT, stack.getMaxStackSize()) - input.getCount();
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
        circuit.save(tag, registries);
        tag.put("Stick", stick.saveOptional(registries));
        tag.put("Input", input.saveOptional(registries));
        tag.put("Output", output.saveOptional(registries));
        tag.putFloat("Progress", progress);
        tag.putBoolean("Jammed", jammed);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        head.load(tag, registries);
        circuit.load(tag, registries);
        stick = ItemStack.parseOptional(registries, tag.getCompound("Stick"));
        input = ItemStack.parseOptional(registries, tag.getCompound("Input"));
        output = ItemStack.parseOptional(registries, tag.getCompound("Output"));
        progress = tag.getFloat("Progress");
        jammed = tag.getBoolean("Jammed");
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
