// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.arc;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
import io.github.soundgoodizerfan.feedback.core.hazard.HotContact;
import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.core.thermal.HeatSource;
import io.github.soundgoodizerfan.feedback.core.thermal.ThermalBody;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.core.unit.VoltageBand;
import io.github.soundgoodizerfan.feedback.machine.Wearing;
import io.github.soundgoodizerfan.feedback.machine.crucible.CrucibleBlockEntity;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * An arc: electrodes over a vessel, powered from an EHV line, burning at a plasma temperature the supply and the electrodes set.
 *
 * <h2>A fire that is an electrical sink</h2>
 * It is a {@link HeatSource} facing the vessel it was set against; the vessel reads its plasma through {@link HeatSource#at} like any
 * fire, so a crucible under it and an arc hearth under it are heated the same way. The plasma follows the power the line delivers
 * (a sag lowers it) and is capped by the weakest electrode: about 97% of its sublimation (or boiling) point, and never above
 * {@link FTuning#ARC_MAX_TU}. Fewer electrodes mean less power drawn and less delivered.
 *
 * <h2>The electrode is what wears</h2>
 * Each electrode loses condition every tick the arc burns, by {@link FTuning#ELECTRODE_WEAR_PER_TICK_AT_LIMIT} times the fourth power of the
 * bath's share of that electrode's limit: nearly nothing at steel's heat, steeply more near its ceiling, so running the bath
 * hot and long is what eats them. Below {@link FTuning#ELECTRODE_SNAP_CONDITION} an electrode snaps: the stump drops into the bath
 * and the arc flashes. A bath above {@link FTuning#ARC_FLASH_TU} that holds metal now and then splashes and flashes by itself.
 */
public class ArcHeadBlockEntity extends ElectricNode implements HeatSource, Wearing {

    private final NonNullList<ItemStack> electrodes = NonNullList.withSize(FTuning.ARC_ELECTRODE_SLOTS, ItemStack.EMPTY);
    private float arcTu = FTuning.AMBIENT_TU.value();
    private float power;
    private boolean powered;

    public ArcHeadBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.ARC_HEAD.get(), pos, state);
    }

    // --- electrodes -----------------------------------------------------------------------------

    public NonNullList<ItemStack> electrodes() {
        return electrodes;
    }

    private int fitted() {
        int n = 0;
        for (ItemStack held : electrodes)
            if (!held.isEmpty())
                n++;
        return n;
    }

    /** Fit one electrode into a free slot. Whether it went in. */
    public boolean fit(ItemStack electrode) {
        for (int i = 0; i < electrodes.size(); i++)
            if (electrodes.get(i).isEmpty()) {
                electrodes.set(i, electrode);
                setChanged();
                return true;
            }
        return false;
    }

    /** Take the last fitted electrode back. */
    public ItemStack pull() {
        for (int i = electrodes.size() - 1; i >= 0; i--)
            if (!electrodes.get(i).isEmpty()) {
                ItemStack taken = electrodes.get(i);
                electrodes.set(i, ItemStack.EMPTY);
                setChanged();
                return taken;
            }
        return ItemStack.EMPTY;
    }

    /** The lowest limit among the fitted electrodes: the arc cannot burn hotter than its weakest. */
    private float electrodeCeiling() {
        float ceiling = Float.MAX_VALUE;
        for (ItemStack held : electrodes)
            if (!held.isEmpty())
                ceiling = Math.min(ceiling, ElectrodeItem.limitTu(held) * FTuning.ARC_PLASMA_OF_SUBLIMATION);
        return Math.min(ceiling, FTuning.ARC_MAX_TU.value());
    }

    // --- sink -----------------------------------------------------------------------------------

    @Override
    public Role role() {
        return Role.SINK;
    }

    @Override
    public float wantedAmps() {
        return FTuning.ARC_AMPS * fitted() / FTuning.ARC_ELECTRODE_SLOTS;
    }

    @Override
    public VoltageBand ratedBand() {
        return VoltageBand.EHV;
    }

    @Override
    public void onResolved(float volts, float amps, float wanted, boolean blocked) {
        super.onResolved(volts, amps, wanted, blocked);
        int n = fitted();
        powered = n > 0 && volts > 0f && amps > 0f;
        float full = FTuning.ARC_FULL_WATTS * n / FTuning.ARC_ELECTRODE_SLOTS;
        power = powered && full > 0f ? Math.min(1f, volts * amps / full) : 0f;
    }

    // --- the tick -------------------------------------------------------------------------------

    public void tickServer() {
        if (!(level instanceof ServerLevel server))
            return;
        HeatSource.radiate(this);
        Tu ambient = AmbientTable.at(server);
        float target = ambient.value() + (electrodeCeiling() - ambient.value()) * power;
        if (fitted() == 0)
            powered = false;
        if (!powered)
            target = ambient.value();
        arcTu += (target - arcTu) / FTuning.ARC_PLASMA_MASS.workPerTu();
        if (arcTu < ambient.value())
            arcTu = ambient.value();

        BlockEntityRef bath = bathAt(server);
        if (powered && arcTu > ambient.value() + 50f) {
            float ref = bath.body != null ? bath.body.getTemperature().value() : arcTu;
            wearElectrodes(server, ref, bath);
            if (bath.holdsMetal() && ref >= FTuning.ARC_FLASH_TU.value() && server.random.nextFloat() < FTuning.ARC_FLASH_CHANCE)
                flash(server, ref);
        }
    }

    private void wearElectrodes(ServerLevel server, float ref, BlockEntityRef bath) {
        boolean snapped = false;
        for (int i = 0; i < electrodes.size(); i++) {
            ItemStack held = electrodes.get(i);
            if (held.isEmpty())
                continue;
            float limit = ElectrodeItem.limitTu(held);
            float share = limit > 0f ? Math.min(1.2f, ref / limit) : 1.2f;
            float wear = FTuning.ELECTRODE_WEAR_PER_TICK_AT_LIMIT * share * share * share * share;
            float condition = ElectrodeItem.condition(held) - wear;
            held.set(FDataComponents.TOOL_CONDITION.get(), Math.max(0f, condition));
            if (condition <= FTuning.ELECTRODE_SNAP_CONDITION) {
                electrodes.set(i, ItemStack.EMPTY);
                dropIntoBath(server, held, bath);
                snapped = true;
            }
        }
        if (snapped) {
            flash(server, ref);
            setChanged();
        }
    }

    /** A snapped stump falls into the bath, where its material does whatever the bath does with it; with no vessel it falls to the floor. */
    private void dropIntoBath(ServerLevel server, ItemStack stump, BlockEntityRef bath) {
        ItemStack left = stump.copy();
        if (bath.crucible != null && bath.crucible.insert(left))
            return;
        Containers.dropItemStack(server, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, left);
    }

    /** A splash and a flash: the bath's heat on anything close, and the light on eyes with no helmet in the way. */
    private void flash(ServerLevel server, float bathTu) {
        server.playSound(null, worldPosition, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.BLOCKS, 0.4f, 2f);
        server.sendParticles(ParticleTypes.FLASH, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, 1, 0, 0, 0, 0);
        float r = FTuning.ARC_FLASH_RADIUS;
        for (LivingEntity victim : server.getEntitiesOfClass(LivingEntity.class, new AABB(worldPosition).inflate(r))) {
            HotContact.burn(victim, new Tu(bathTu));
            if (victim.getItemBySlot(EquipmentSlot.HEAD).isEmpty())
                victim.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, FTuning.ARC_FLASH_BLIND_TICKS));
        }
    }

    /** The vessel the head faces, seen once a tick. */
    private BlockEntityRef bathAt(ServerLevel server) {
        var be = server.getBlockEntity(worldPosition.relative(heatFacing()));
        return new BlockEntityRef(be instanceof ThermalBody body ? body : null, be instanceof CrucibleBlockEntity c ? c : null);
    }

    private record BlockEntityRef(ThermalBody body, CrucibleBlockEntity crucible) {
        boolean holdsMetal() {
            return crucible != null && crucible.getTank().getFluidAmount() > 0;
        }
    }

    // --- HeatSource -----------------------------------------------------------------------------

    @Override
    public Direction heatFacing() {
        return HeatSource.facingOf(getBlockState());
    }

    /** A plasma is concentrated: it reaches every side of the face it is set to. */
    @Override
    public float sideShare() {
        return FTuning.ARC_HEAD_SIDE_SHARE;
    }

    @Override
    public Tu getFireTu() {
        return new Tu(arcTu);
    }

    // --- Wearing: the worst electrode's condition -----------------------------------------------

    @Override
    public float getCondition() {
        float worst = 1f;
        for (ItemStack held : electrodes)
            if (!held.isEmpty())
                worst = Math.min(worst, ElectrodeItem.condition(held));
        return worst;
    }

    @Override
    public boolean isRunning() {
        return powered;
    }

    /** Plasma temperature, for the readout. */
    public Tu plasma() {
        return new Tu(arcTu);
    }

    // --- persistence ----------------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, electrodes, true, registries);
        tag.putFloat("Arc", arcTu);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        electrodes.clear();
        ContainerHelper.loadAllItems(tag, electrodes, registries);
        arcTu = tag.contains("Arc") ? tag.getFloat("Arc") : FTuning.AMBIENT_TU.value();
    }
}
