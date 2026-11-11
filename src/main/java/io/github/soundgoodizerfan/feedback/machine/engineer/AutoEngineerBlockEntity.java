// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.engineer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricRules;
import io.github.soundgoodizerfan.feedback.core.unit.VoltageBand;
import io.github.soundgoodizerfan.feedback.item.RecordedAssembly;
import io.github.soundgoodizerfan.feedback.machine.Wearing;
import io.github.soundgoodizerfan.feedback.modular.ModularFit;
import io.github.soundgoodizerfan.feedback.modular.ModularType;
import io.github.soundgoodizerfan.feedback.modular.ToolTableAssembly;
import io.github.soundgoodizerfan.feedback.modular.engineering.AssemblyRecording;
import io.github.soundgoodizerfan.feedback.modular.engineering.EngineeringAssembly;
import io.github.soundgoodizerfan.feedback.modular.engineering.EngineeringInput;
import io.github.soundgoodizerfan.feedback.modular.engineering.Fasteners;
import io.github.soundgoodizerfan.feedback.modular.engineering.ToolAbilities;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FormItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A powered assembler that plays back one hand assembly a recipe data stick recorded at the Engineering Table, and knows no recipe itself.
 *
 * <h2>The stick is the knowledge</h2>
 * A stick holding a {@link RecordedAssembly} says which stock classes (material kind and form) go in which grid slots and how many. When the
 * parts bay holds exactly that stock, the machine lays it on a grid and asks {@link EngineeringAssembly#plan} (the same code as the table)
 * for the work, with its own tools and fastener stock and with {@code floor} set: no minigame, no bonus, the floor quality. Tools wear a
 * point per assembly as at the table (hand tools may always wear); a correct assembly wears nothing of the machine. A missing, broken or
 * short tool or fastener makes the machine wait, spending nothing.
 *
 * <h2>Anything else is improvised or scrapped</h2>
 * Parts the stick does not cover (or no stick) take {@link #improvise}: every modular type the whole bay can legally form is a candidate and a
 * seeded index (world seed and the stock classes, never item ids; cached per key) picks one. When nothing fits, one stack is scrapped to
 * nuggets of its material, keeping {@code ENGINEER_SCRAP_YIELD} of its mass, at a little misuse wear; stock with no material is voided. The
 * machine never jams on junk and never reveals completion.
 *
 * <h2>Wear is misuse only</h2>
 * Stock harder than the machine takes, a full output (retried every 20 ticks, wearing once per blocked episode), scrapping and a supply past the EHV rating each
 * cost condition; a worn machine only works slower, down to a floor.
 */
public class AutoEngineerBlockEntity extends ElectricNode implements Wearing {

    /** Parts bay slots (the grid it lays parts on is the table's 3x3), tool slots and fastener stock slots. */
    public static final int BAY = 9, TOOLS = 4, FASTENERS = 3;

    private ItemStack stick = ItemStack.EMPTY;
    private final NonNullList<ItemStack> bay = NonNullList.withSize(BAY, ItemStack.EMPTY);
    private final NonNullList<ItemStack> tools = NonNullList.withSize(TOOLS, ItemStack.EMPTY);
    private final NonNullList<ItemStack> fasteners = NonNullList.withSize(FASTENERS, ItemStack.EMPTY);
    private ItemStack output = ItemStack.EMPTY;
    private float condition = 1f;
    private float progress;
    private float rate;
    private float over;
    /** Blocked on a tool, a fastener, a refused plan or a full output: retried every {@link #RETRY} ticks or when a slot changes. */
    private boolean blocked;
    private boolean dirty = true;
    private boolean wore;
    private int wait;

    private static final int RETRY = 20;

    /** The seeded choice per (world seed, stock classes): a raw index, reduced by the candidate count, computed once. */
    private record Key(long seed, String classes) {
    }

    private static final Map<Key, Integer> CHOICES = new HashMap<>();
    private static int generation = -1;

    public AutoEngineerBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.AUTO_ENGINEER.get(), pos, state);
    }

    // --- sink ---------------------------------------------------------------------------------

    @Override
    public Role role() {
        return Role.SINK;
    }

    @Override
    public float wantedAmps() {
        return working() ? FTuning.ENGINEER_AMPS : 0f;
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
        for (ItemStack s : bay)
            if (!s.isEmpty())
                return true;
        return false;
    }

    // --- the tick -----------------------------------------------------------------------------

    public void tickServer() {
        if (!(level instanceof ServerLevel server) || !working() || rate <= 0f)
            return;
        if (blocked && !dirty && ++wait < RETRY)
            return;
        wait = 0;
        progress += rate * condition;
        if (progress < FTuning.ENGINEER_TICKS_PER_ASSEMBLY)
            return;
        progress -= FTuning.ENGINEER_TICKS_PER_ASSEMBLY;
        dirty = false;
        blocked = false;
        work(server);
        if (!blocked)
            wore = false;
    }

    private void work(ServerLevel server) {
        if (over > 0f)
            wear(over * FTuning.ENGINEER_OVERVOLT_WEAR_UNITS);
        Map<ResourceLocation, Material> materials = ClientMaterials.on(server);
        // Stock harder than the machine takes is spoiled, and the force goes into the machine.
        for (int i = 0; i < BAY; i++) {
            ItemStack s = bay.get(i);
            Optional<Material.Located> found = s.isEmpty() ? Optional.empty() : Material.find(materials, s);
            if (found.isPresent() && found.get().material().hardness().value() > FTuning.ENGINEER_STOCK_HARDNESS) {
                wear(found.get().material().hardness().value() / FTuning.ENGINEER_STOCK_HARDNESS);
                scrap(server, i, found);
                return;
            }
        }
        RecordedAssembly rec = stick.get(FDataComponents.RECORDED_ASSEMBLY.get());
        if (rec != null) {
            Boolean played = play(server, rec, materials);
            if (played != null) {
                if (!played)
                    block(false); // waiting on a tool, a fastener or the output
                return;
            }
        }
        improvise(server, materials);
    }

    /**
     * Plays the stick if the bay holds exactly its stock. Null when the stick does not cover the bay (improvise instead); false when it does
     * but cannot run yet; true when an assembly was made.
     */
    private Boolean play(ServerLevel server, RecordedAssembly rec, Map<ResourceLocation, Material> materials) {
        int[] from = new int[9];
        boolean[] used = new boolean[BAY];
        List<ItemStack> grid = new ArrayList<>();
        for (int g = 0; g < 9; g++) {
            String want = g < rec.grid().size() ? rec.grid().get(g) : "";
            int n = g < rec.counts().size() ? rec.counts().get(g) : 0;
            from[g] = -1;
            if (want.isEmpty()) {
                grid.add(ItemStack.EMPTY);
                continue;
            }
            for (int b = 0; b < BAY && from[g] < 0; b++)
                if (!used[b] && !bay.get(b).isEmpty() && bay.get(b).getCount() >= n && want.equals(AssemblyRecording.stockClass(materials, bay.get(b)))) {
                    from[g] = b;
                    used[b] = true;
                }
            if (from[g] < 0)
                return null;
            grid.add(bay.get(from[g]).copyWithCount(n));
        }
        for (int b = 0; b < BAY; b++)
            if (!bay.get(b).isEmpty() && !used[b])
                return null;
        EngineeringInput in = new EngineeringInput(grid, tools, fasteners, ItemStack.EMPTY, rec.school());
        EngineeringAssembly.Plan plan = EngineeringAssembly.plan(server, in, ItemStack.EMPTY, -1f, true);
        if (plan.status() == EngineeringAssembly.NOTHING || plan.ok() && !BuiltInRegistries.ITEM.getKey(plan.result().getItem()).toString().equals(rec.result()))
            return null;
        if (!plan.ok())
            return false;
        if (!fitsOutput(plan.result())) {
            block(true); // running against a full output is misuse
            return false;
        }
        ItemStack made = plan.result().copy();
        List<Integer> before = new ArrayList<>();
        for (ItemStack s : grid)
            before.add(s.getCount());
        plan.take().accept(in, null);
        for (int g = 0; g < 9; g++)
            if (from[g] >= 0)
                bay.get(from[g]).shrink(before.get(g) - grid.get(g).getCount());
        addOutput(made);
        tidy();
        return true;
    }

    /** What the whole bay can legally form, chosen by a seeded index; else one stack scrapped. */
    private void improvise(ServerLevel server, Map<ResourceLocation, Material> materials) {
        List<ItemStack> grid = new ArrayList<>();
        List<String> classes = new ArrayList<>();
        for (ItemStack s : bay)
            if (!s.isEmpty()) {
                grid.add(s.copy());
                classes.add(AssemblyRecording.stockClass(materials, s));
            }
        List<ItemStack> candidates = new ArrayList<>();
        Map<ResourceLocation, ModularType> types = EngineeringAssembly.types(server);
        for (Map.Entry<ResourceLocation, ModularType> t : types.entrySet())
            ToolTableAssembly.infer(grid, ItemStack.EMPTY, materials, Map.of(t.getKey(), t.getValue()), AutoEngineerBlockEntity::plainBench, new ModularFit.Start(0, -1f))
                    .ifPresent(m -> candidates.add(m.tool()));
        if (candidates.isEmpty()) {
            for (int i = 0; i < BAY; i++)
                if (!bay.get(i).isEmpty()) {
                    scrap(server, i, Material.find(materials, bay.get(i)));
                    return;
                }
            return;
        }
        ItemStack made = candidates.get(seeded(server, classes, candidates.size()));
        if (!fitsOutput(made)) {
            block(true);
            return;
        }
        for (int i = 0; i < BAY; i++)
            bay.set(i, ItemStack.EMPTY);
        addOutput(made.copy());
        sync();
    }

    /** What a plain crafting table could assemble: not a complex type, no school, no fasteners. */
    private static boolean plainBench(ModularType type) {
        return !type.blueprint() && type.school().isEmpty() && type.fasteners().count() == 0;
    }

    private static int seeded(ServerLevel level, List<String> classes, int size) {
        synchronized (CHOICES) {
            if (generation != MaterialTable.generation()) {
                CHOICES.clear();
                generation = MaterialTable.generation();
            }
            List<String> sorted = new ArrayList<>(classes);
            java.util.Collections.sort(sorted);
            Key key = new Key(level.getSeed(), String.join(",", sorted));
            int pick = CHOICES.computeIfAbsent(key, k -> new Random(k.seed() * 31 + k.classes().hashCode() * 17L).nextInt(1 << 20));
            return pick % size;
        }
    }

    /** Melts one stack of bay {@code index} down to nuggets of its material at a loss, or voids it when it has no material or no nugget. */
    private void scrap(ServerLevel server, int index, Optional<Material.Located> found) {
        ItemStack in = bay.get(index);
        ItemStack nuggets = ItemStack.EMPTY;
        int items = in.getCount();
        if (found.isPresent()) {
            Optional<net.minecraft.world.item.Item> nugget = found.get().material().item(found.get().id(), "nugget");
            int per = Math.round(FormItems.volume(found.get().form()) / (float) FormItems.volume("nugget") * FTuning.ENGINEER_SCRAP_YIELD);
            if (nugget.isPresent() && per > 0) {
                items = Math.max(1, Math.min(in.getCount(), nugget.get().getDefaultMaxStackSize() / per));
                nuggets = new ItemStack(nugget.get(), per * items);
            }
        }
        if (!nuggets.isEmpty() && !fitsOutput(nuggets)) {
            block(true);
            return;
        }
        wear(1f); // scrapping what the machine was never told about is misuse, a little
        in.shrink(items);
        if (!nuggets.isEmpty())
            addOutput(nuggets);
        tidy();
    }

    private void tidy() {
        for (int i = 0; i < BAY; i++)
            if (bay.get(i).isEmpty())
                bay.set(i, ItemStack.EMPTY);
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

    /** Waits on a tool, a fastener or a full output; a full output wears the machine once per blocked episode, not per retry. */
    private void block(boolean fullOutput) {
        blocked = true;
        progress = FTuning.ENGINEER_TICKS_PER_ASSEMBLY;
        if (fullOutput && !wore) {
            wore = true;
            wear(1f);
        }
    }

    private void wear(float units) {
        condition = Math.max(FTuning.ENGINEER_CONDITION_FLOOR, condition - units * FTuning.ENGINEER_WEAR_PER_MISUSE);
        setChanged();
        if (level != null && !level.isClientSide)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    // --- contents -----------------------------------------------------------------------------

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

    public NonNullList<ItemStack> getBay() {
        return bay;
    }

    public NonNullList<ItemStack> getTools() {
        return tools;
    }

    public NonNullList<ItemStack> getFasteners() {
        return fasteners;
    }

    public ItemStack getOutput() {
        return output;
    }

    /** Seats one of {@code stack} in the first free tool slot; false when it is no tool or all four are taken. */
    public boolean seatTool(ItemStack stack) {
        if (!ToolAbilities.isTool(stack))
            return false;
        for (int i = 0; i < TOOLS; i++)
            if (tools.get(i).isEmpty()) {
                tools.set(i, stack.split(1));
                sync();
                return true;
            }
        return false;
    }

    /** The last tool, else the last fastener stack, else the last parts stack, taken back by hand. */
    public ItemStack takeLoose() {
        for (var list : List.of(tools, fasteners, bay))
            for (int i = list.size() - 1; i >= 0; i--)
                if (!list.get(i).isEmpty()) {
                    ItemStack out = list.get(i);
                    list.set(i, ItemStack.EMPTY);
                    sync();
                    return out;
                }
        return ItemStack.EMPTY;
    }

    /** Takes as much of {@code stack} as bay slot {@code slot} holds; the rest comes back. The bay takes anything: the machine does not look. */
    public ItemStack insertPart(int slot, ItemStack stack, boolean simulate) {
        ItemStack held = bay.get(slot);
        if (stack.isEmpty() || !held.isEmpty() && !ItemStack.isSameItemSameComponents(held, stack))
            return stack;
        int room = Math.min(FTuning.CUTTER_INPUT_LIMIT, stack.getMaxStackSize()) - held.getCount();
        int moved = Math.min(room, stack.getCount());
        if (moved <= 0)
            return stack;
        if (!simulate) {
            if (held.isEmpty())
                bay.set(slot, stack.copyWithCount(moved));
            else
                held.grow(moved);
            sync();
        }
        return stack.copyWithCount(stack.getCount() - moved);
    }

    /** As {@link #insertPart} but into the first bay slot that takes it, for a hand feeding the machine. */
    public ItemStack insertPart(ItemStack stack) {
        ItemStack rest = stack;
        for (int i = 0; i < BAY && !rest.isEmpty(); i++)
            rest = insertPart(i, rest, false);
        return rest;
    }

    /** Refills the fastener stock: only a fastener is taken. */
    public ItemStack insertFastener(int slot, ItemStack stack, boolean simulate) {
        if (!Fasteners.is(stack))
            return stack;
        ItemStack held = fasteners.get(slot);
        if (!held.isEmpty() && !ItemStack.isSameItemSameComponents(held, stack))
            return stack;
        int moved = Math.min(stack.getMaxStackSize() - held.getCount(), stack.getCount());
        if (moved <= 0)
            return stack;
        if (!simulate) {
            if (held.isEmpty())
                fasteners.set(slot, stack.copyWithCount(moved));
            else
                held.grow(moved);
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
        return condition;
    }

    @Override
    public boolean isRunning() {
        return working() && rate > 0f;
    }

    // --- persistence --------------------------------------------------------------------------

    public void sync() {
        dirty = true;
        setChanged();
        if (level != null && !level.isClientSide)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Stick", stick.saveOptional(registries));
        tag.put("Output", output.saveOptional(registries));
        CompoundTag b = new CompoundTag(), t = new CompoundTag(), f = new CompoundTag();
        ContainerHelper.saveAllItems(b, bay, registries);
        ContainerHelper.saveAllItems(t, tools, registries);
        ContainerHelper.saveAllItems(f, fasteners, registries);
        tag.put("Bay", b);
        tag.put("Tools", t);
        tag.put("Fasteners", f);
        tag.putFloat("Condition", condition);
        tag.putFloat("Progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        stick = ItemStack.parseOptional(registries, tag.getCompound("Stick"));
        output = ItemStack.parseOptional(registries, tag.getCompound("Output"));
        bay.clear();
        tools.clear();
        fasteners.clear();
        ContainerHelper.loadAllItems(tag.getCompound("Bay"), bay, registries);
        ContainerHelper.loadAllItems(tag.getCompound("Tools"), tools, registries);
        ContainerHelper.loadAllItems(tag.getCompound("Fasteners"), fasteners, registries);
        condition = tag.contains("Condition") ? tag.getFloat("Condition") : 1f;
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
