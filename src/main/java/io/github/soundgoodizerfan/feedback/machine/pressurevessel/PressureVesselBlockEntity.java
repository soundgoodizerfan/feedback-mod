// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.pressurevessel;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.hazard.PressureBurst;
import io.github.soundgoodizerfan.feedback.core.unit.Mb;
import io.github.soundgoodizerfan.feedback.core.unit.Pu;
import io.github.soundgoodizerfan.feedback.machine.bellows.Blown;
import io.github.soundgoodizerfan.feedback.modular.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.ModularFigures;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.registry.FModular;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import io.github.soundgoodizerfan.feedback.core.thermal.HeatSource;
import io.github.soundgoodizerfan.feedback.item.Canned;
import io.github.soundgoodizerfan.feedback.process.Canning;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A sealed tank of compressed air, fed by a Bellows exactly the way a Firebox or a self-fired
 * vessel already is -- see {@link Blown}. Air here does not multiply a flame; it accumulates, and
 * enough of it explodes.
 *
 * <h2>Amount and volume, not a stored Pu</h2>
 * {@code amount} is air in {@link Mb}, the unit {@link Blown#addAir} takes, and
 * {@link FTuning#PRESSURE_VESSEL_VOLUME} is fixed per block. {@link #getPressure()} divides them
 * on read, never stored -- the same discipline {@code ItemHeat} and {@code Heat.equilibrium}
 * already use for a derived figure, and directly {@code PressureTier}/{@code IAirHandler}'s own
 * shape (PneumaticCraft: Repressurized, GPL-3.0 -- see {@code THIRD-PARTY-LICENSES.md}).
 *
 * <h2>No identity check on the compressor either</h2>
 * A Bellows aimed at this finds it the same way it finds a Firebox or a self-fired vessel: by
 * scanning its six neighbours for anything that implements {@link Blown} and handing over air.
 * Nothing here knows it is being fed by a Bellows specifically, and nothing about the Bellows knows
 * it is feeding a Pressure Vessel rather than a fire -- philosophy 11 applied to an actuator
 * instead of a sorter.
 *
 * <h2>Canning</h2>
 * Pressure raises the boiling point, so jars in here can get hotter than plain boiling: the contents
 * relax toward the lesser of the fire below and {@code POT_BOIL_TU + CANNING_BOIL_PER_PU * Pu}. Held at
 * {@code CANNING_STERILE_TU} the jars seal; kept hot after that they go to mush (food spec 8). The
 * jars change nothing about the pressure ladder below.
 */
public class PressureVesselBlockEntity extends BlockEntity implements Blown, io.github.soundgoodizerfan.feedback.core.hazard.PressureHolder {

    private static final String AMOUNT = "Amount";

    private static final String TEMPERATURE = "Temperature";

    private float amount;
    /** What the shell was built to: the room inside (mB), and the pressures (Pu) it risks and gives way at. Baked by {@code ModularBake#tank}. */
    private float volume = FTuning.PRESSURE_VESSEL_VOLUME.value();
    private float danger = FTuning.PRESSURE_VESSEL_DANGER_PU.value();
    private float critical = FTuning.PRESSURE_VESSEL_CRITICAL_PU.value();
    /** The design it was built from, kept so the broken block drops the same item. */
    private ModularDesign design;
    private ModularFigures figures;
    /** One temperature for everything inside, relaxing toward the fire or the boiling point, whichever is lower. */
    private float temperature = FTuning.AMBIENT_TU.value();
    private final NonNullList<ItemStack> jars = NonNullList.withSize(FTuning.CANNING_SLOTS, ItemStack.EMPTY);

    public PressureVesselBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.PRESSURE_VESSEL.get(), pos, state);
    }

    @Override
    public void addAir(Mb air) {
        amount = Math.max(0, amount + air.value());
        setChanged();
    }

    @Override
    public Pu getPressure() {
        return new Pu(amount / volume);
    }

    public void tickServer() {
        if (level == null || level.isClientSide)
            return;

        if (amount > 0) {
            amount = Math.max(0, amount - FTuning.PRESSURE_VESSEL_LEAK_PER_TICK.value());
            setChanged();
        }

        canningTick();

        // Read from PneumaticCraft: Repressurized's MachineAirHandler#addAir (GPL-3.0) -- risk
        // climbs linearly across the danger-to-critical band, and critical itself is certain
        // rather than merely likely. This is philosophy 6's Hazard tier, and philosophy 14's named
        // GregTech-boiler precedent, both landing on Feedback's own equipment rather than staying
        // an open item -- see FTuning#PRESSURE_VESSEL_DANGER_PU. The ladder itself is shared with
        // the boiler: see PressureBurst.
        if (PressureBurst.fails(getPressure(), new Pu(danger),
                new Pu(critical), level.random)) {
            amount = 0;
            setChanged();
            Level level = this.level;
            level.explode(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                    worldPosition.getZ() + 0.5, 2.0f, Level.ExplosionInteraction.BLOCK);
        }
    }

    private void canningTick() {
        // Nothing in it to cook: look once a second and take the missed ticks in one step, not a heat lookup every tick.
        boolean empty = jarCount() == 0;
        if (empty && level.getGameTime() % 20 != 0)
            return;
        float target = Math.min(HeatSource.at(level, worldPosition).value(),
                FTuning.POT_BOIL_TU.value() + FTuning.CANNING_BOIL_PER_PU * getPressure().value());
        temperature += (target - temperature) * (empty ? 1f - (float) Math.pow(1f - FTuning.POT_RELAX_PER_TICK, 20) : FTuning.POT_RELAX_PER_TICK);
        // Per second, not per tick: nothing on an item is touched every tick.
        if (level.getGameTime() % 20 != 0 || temperature < FTuning.CANNING_STERILISE_TU.value())
            return;
        boolean changed = false;
        for (int i = 0; i < jars.size(); i++) {
            Canned can = jars.get(i).get(FDataComponents.CANNED.get());
            if (can == null)
                continue;
            if (!can.sealed()) {
                float sterile = can.sterile() + 20f / FTuning.CANNING_STERILE_TICKS;
                can = sterile >= 1f ? Canning.seal(can, level) : new Canned(can.food(), sterile, false, can.mush());
            } else if (can.mush() < 1f) {
                can = new Canned(can.food(), 1f, true, Math.min(1f, can.mush() + 20f / FTuning.CANNING_MUSH_TICKS));
            } else
                continue;
            jars.get(i).set(FDataComponents.CANNED.get(), can);
            Canning.refresh(jars.get(i));
            changed = true;
        }
        if (changed)
            setChanged();
    }

    public float temperature() {
        return temperature;
    }

    /** The jars as {sealed, still sealing, gone to mush}; the sealing ones' least progress is {@link #leastSterile}. */
    public int[] jarStates() {
        int sealed = 0, sealing = 0, mush = 0;
        for (ItemStack s : jars) {
            Canned can = s.get(FDataComponents.CANNED.get());
            if (can == null)
                continue;
            if (!can.sealed())
                sealing++;
            else
                sealed++;
            if (can.mush() >= FTuning.CANNING_MUSH_SHOWN_AT)
                mush++;
        }
        return new int[] { sealed, sealing, mush };
    }

    /** The furthest-behind unsealed jar's sterilising progress, 0 to 1, or -1 when none is waiting to seal. */
    public float leastSterile() {
        float least = -1f;
        for (ItemStack s : jars) {
            Canned can = s.get(FDataComponents.CANNED.get());
            if (can != null && !can.sealed())
                least = least < 0f ? can.sterile() : Math.min(least, can.sterile());
        }
        return least;
    }

    public int jarCount() {
        int n = 0;
        for (ItemStack s : jars)
            if (!s.isEmpty())
                n++;
        return n;
    }

    /** Put one jar in; false if full. */
    public boolean addJar(ItemStack one) {
        for (int i = 0; i < jars.size(); i++)
            if (jars.get(i).isEmpty()) {
                jars.set(i, one);
                setChanged();
                return true;
            }
        return false;
    }

    /** Take the last jar put in, or empty. */
    public ItemStack takeJar() {
        for (int i = jars.size() - 1; i >= 0; i--)
            if (!jars.get(i).isEmpty()) {
                ItemStack out = jars.get(i);
                jars.set(i, ItemStack.EMPTY);
                setChanged();
                return out;
            }
        return ItemStack.EMPTY;
    }

    public void dropJars() {
        for (ItemStack s : jars)
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), s);
    }

    // --- persistence --------------------------------------------------------------------------

    /** The shell's figures, as the item carries them ({@code FDataComponents.PRESSURE_VESSEL}) and the block saves them. */
    private CompoundTag writeFigures(CompoundTag tag) {
        tag.putFloat("Volume", volume);
        tag.putFloat("Danger", danger);
        tag.putFloat("Critical", critical);
        return tag;
    }

    private void readFigures(CompoundTag tag) {
        volume = tag.contains("Volume") ? Math.max(1f, tag.getFloat("Volume")) : volume;
        danger = tag.contains("Danger") ? tag.getFloat("Danger") : danger;
        critical = tag.contains("Critical") ? tag.getFloat("Critical") : critical;
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        CompoundTag state = input.get(FDataComponents.PRESSURE_VESSEL.get());
        if (state != null)
            readFigures(state);
        design = input.get(FModular.DESIGN.get());
        figures = input.get(FModular.FIGURES.get());
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(FDataComponents.PRESSURE_VESSEL.get(), writeFigures(new CompoundTag()));
        if (design != null)
            components.set(FModular.DESIGN.get(), design);
        if (figures != null)
            components.set(FModular.FIGURES.get(), figures);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putFloat(AMOUNT, amount);
        writeFigures(tag);
        RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
        if (design != null)
            ModularDesign.CODEC.encodeStart(ops, design).result().ifPresent(t -> tag.put("Design", t));
        if (figures != null)
            ModularFigures.CODEC.encodeStart(ops, figures).result().ifPresent(t -> tag.put("Figures", t));
        tag.putFloat(TEMPERATURE, temperature);
        ContainerHelper.saveAllItems(tag, jars, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        amount = tag.getFloat(AMOUNT);
        readFigures(tag);
        RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
        design = tag.contains("Design") ? ModularDesign.CODEC.parse(ops, tag.get("Design")).result().orElse(null) : null;
        figures = tag.contains("Figures") ? ModularFigures.CODEC.parse(ops, tag.get("Figures")).result().orElse(null) : null;
        temperature = tag.contains(TEMPERATURE) ? tag.getFloat(TEMPERATURE) : FTuning.AMBIENT_TU.value();
        jars.clear();
        ContainerHelper.loadAllItems(tag, jars, registries);
    }
}
