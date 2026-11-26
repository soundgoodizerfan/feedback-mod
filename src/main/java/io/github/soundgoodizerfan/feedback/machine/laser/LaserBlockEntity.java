// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.laser;

import io.github.soundgoodizerfan.feedback.control.ChannelSource;
import io.github.soundgoodizerfan.feedback.control.RemoteChannels;
import io.github.soundgoodizerfan.feedback.control.Switchable;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricRules;
import io.github.soundgoodizerfan.feedback.core.unit.VoltageBand;
import io.github.soundgoodizerfan.feedback.machine.MachineTuning;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A pumped-crystal laser (roadmap O4). A crystal seated in it (any item whose material lases: has the {@code phosphor} figure) is the gain
 * medium; the pump is an MV sink. Powered with a crystal it only ranges: a weak beam that reads the distance to the first solid thing, a
 * channel a Controller can read. Engaged (a Controller's Switch card, or a redstone signal) it fires: the beam doses the block it hits, by the
 * crystal's clarity (one minus its reflectivity), and cuts the block when the dose reaches the block's hardness times a rate, so what it cuts
 * is the world's hardness against its own power; a living thing in the beam is burned. The crystal turns what it does not emit into heat, more
 * the less clear it is, and sheds it only while the beam is off: fired too long it cracks (a cheap consumable crystal, as the optical terminal's
 * burns out), so a hard cut is made in pulses. A band over the pump's rating cracks it at once. Nothing else wears, and the housing never does.
 */
public class LaserBlockEntity extends ElectricNode implements Switchable, ChannelSource, RemoteChannels {

    private ItemStack crystal = ItemStack.EMPTY;
    private boolean engaged;
    private float heat;
    @org.jetbrains.annotations.Nullable
    private BlockPos target;
    private float dose;
    private int reach;

    public LaserBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.LASER.get(), pos, state);
    }

    // --- sink -----------------------------------------------------------------------------------

    @Override
    public Role role() {
        return Role.SINK;
    }

    @Override
    public float wantedAmps() {
        return crystal.isEmpty() ? 0f : firing() ? MachineTuning.LASER_FIRING_AMPS : MachineTuning.LASER_RANGING_AMPS;
    }

    @Override
    public VoltageBand ratedBand() {
        return VoltageBand.MV;
    }

    // --- the crystal ----------------------------------------------------------------------------

    /** The lasing material of {@code stack}, empty when its material does not lase. */
    private static Optional<Material> lasing(ItemStack stack) {
        if (stack.isEmpty())
            return Optional.empty();
        return Material.find(MaterialTable.get().materials(), stack).map(Material.Located::material).filter(Material::phosphor);
    }

    /** Whether a hand's stack can be seated as the crystal. */
    public static boolean isCrystal(ItemStack stack) {
        return lasing(stack).isPresent();
    }

    /** One minus the crystal's reflectivity: how much of the pump comes out as beam. 0 with no crystal. */
    public float clarity() {
        return lasing(crystal).map(m -> 1f - Math.min(1f, Math.max(0f, m.separation().reflectivity().orElse(MachineTuning.LASER_DEFAULT_REFLECTIVITY)))).orElse(0f);
    }

    private int color() {
        return lasing(crystal).flatMap(Material::color).orElse(0xFF3030);
    }

    /** Seats one of {@code in}; the one it replaces comes back. */
    public ItemStack seatCrystal(ItemStack in) {
        ItemStack old = crystal;
        crystal = in.split(1);
        heat = 0f;
        resetTarget();
        setChanged();
        return old;
    }

    public ItemStack takeCrystal() {
        ItemStack old = crystal;
        crystal = ItemStack.EMPTY;
        heat = 0f;
        resetTarget();
        setChanged();
        return old;
    }

    public ItemStack crystal() {
        return crystal;
    }

    /** The heat as a share of what cracks the crystal, 0 to 1. */
    public float heatShare() {
        return Math.min(1f, heat / MachineTuning.LASER_CRACK_HEAT);
    }

    // --- switch ----------------------------------------------------------------------------------

    @Override
    public void setEngaged(boolean engaged) {
        this.engaged = engaged;
        setChanged();
    }

    @Override
    public boolean isEngaged() {
        return engaged || level != null && level.hasNeighborSignal(worldPosition);
    }

    private boolean powered() {
        return !crystal.isEmpty() && ElectricRules.rate(getPacketEu(), getAmps(), getWantedAmps(), ratedBand()) >= 0.5f;
    }

    /** Whether the beam is cutting now: powered, with a crystal, switched on. */
    public boolean firing() {
        return powered() && isEngaged();
    }

    // --- what a Controller reads ------------------------------------------------------------------

    @Override
    public float read(int channel) {
        return switch (channel) {
            case 0 -> powered() ? reach : 0f;
            case 1 -> heatShare();
            case 2 -> target == null || level == null ? 0f : Math.min(1f, dose / threshold(level, target));
            default -> 0f;
        };
    }

    @Override
    public String[] senseKeys() {
        return new String[] {"feedback.machine.sense.laser_range", "feedback.machine.sense.laser_heat", "feedback.machine.sense.laser_cut"};
    }

    // --- the beam ---------------------------------------------------------------------------------

    private void resetTarget() {
        target = null;
        dose = 0f;
    }

    private static float threshold(net.minecraft.world.level.Level level, BlockPos pos) {
        return Math.max(1f, level.getBlockState(pos).getDestroySpeed(level, pos) * MachineTuning.LASER_DOSE_PER_HARDNESS);
    }

    public void tickServer() {
        if (!(level instanceof ServerLevel server))
            return;
        if (!crystal.isEmpty() && getBand().compareTo(ratedBand()) > 0) {
            crack(server);
            return;
        }
        boolean fire = firing();
        if (fire)
            heat += MachineTuning.LASER_HEAT_PER_TICK + MachineTuning.LASER_HEAT_LOSS_SHARE * (1f - clarity());
        else if (heat > 0f) {
            heat = Math.max(0f, heat - MachineTuning.LASER_COOL_PER_TICK);
            if (heat == 0f)
                setChanged();
        }
        if (fire && heat >= MachineTuning.LASER_CRACK_HEAT) {
            crack(server);
            return;
        }
        if (!powered()) {
            reach = 0;
            resetTarget();
            return;
        }
        // ranging reads every few ticks, firing every tick
        if (!fire && (server.getGameTime() + worldPosition.hashCode()) % 4 != 0)
            return;
        Direction face = getBlockState().getValue(LaserBlock.FACING);
        Vec3 from = Vec3.atCenterOf(worldPosition).relative(face, 0.6);
        Vec3 to = Vec3.atCenterOf(worldPosition).relative(face, MachineTuning.LASER_RANGE + 0.5);
        BlockHitResult hit = server.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, net.minecraft.world.phys.shapes.CollisionContext.empty()));
        Vec3 end = hit.getType() == HitResult.Type.MISS ? to : hit.getLocation();
        int blocks = hit.getType() == HitResult.Type.MISS ? 0 : (int) Math.round(from.distanceTo(end) + 0.6);
        if (blocks != reach) {
            reach = blocks;
            setChanged();
        }
        // a pause keeps the dose on the block the beam rested on: a hard cut is made in pulses
        if (!fire)
            return;
        server.sendParticles(new DustParticleOptions(new org.joml.Vector3f(((color() >> 16) & 255) / 255f, ((color() >> 8) & 255) / 255f, (color() & 255) / 255f), 1f),
                (from.x + end.x) / 2, (from.y + end.y) / 2, (from.z + end.z) / 2, 3,
                Math.abs(end.x - from.x) / 3, Math.abs(end.y - from.y) / 3, Math.abs(end.z - from.z) / 3, 0);
        if (server.getGameTime() % MachineTuning.LASER_HURT_EVERY == 0) {
            burn(server, from, end);
            mark(server, from, end);
        }
        if (hit.getType() == HitResult.Type.MISS) {
            resetTarget();
            return;
        }
        cut(server, hit.getBlockPos());
    }

    /** A made part lying in the beam is marked: its axes are stamped exact, as calipers would, with no hand and no instrument (roadmap O5). */
    private void mark(ServerLevel server, Vec3 from, Vec3 end) {
        for (net.minecraft.world.entity.item.ItemEntity item : server.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new AABB(from, end).inflate(0.5)))
            if (item.getBoundingBox().inflate(0.1).clip(from, end).isPresent()
                    && io.github.soundgoodizerfan.feedback.modular.bench.Measured.stamp(io.github.soundgoodizerfan.feedback.modular.bench.Measured.Kind.CALIPERS, item.getItem()))
                server.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, item.getX(), item.getY() + 0.2, item.getZ(), 3, 0.1, 0.1, 0.1, 0.01);
    }

    /** Burns the living things the beam passes through. */
    private void burn(ServerLevel server, Vec3 from, Vec3 end) {
        for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, new AABB(from, end).inflate(0.5)))
            if (e.getBoundingBox().inflate(0.15).clip(from, end).isPresent()) {
                e.hurt(server.damageSources().inFire(), MachineTuning.LASER_HURT * clarity());
                e.igniteForSeconds(MachineTuning.LASER_BURN_SECONDS);
            }
    }

    /** Puts the beam's dose into the block hit and breaks it when the dose reaches its hardness's worth (never an unbreakable one, and only where the world lets blasts break blocks). */
    private void cut(ServerLevel server, BlockPos pos) {
        if (!pos.equals(target)) {
            target = pos.immutable();
            dose = 0f;
        }
        float hardness = server.getBlockState(pos).getDestroySpeed(server, pos);
        if (hardness < 0f || !server.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING))
            return;
        dose += MachineTuning.LASER_DOSE_PER_TICK * clarity();
        if (dose >= threshold(server, pos)) {
            server.destroyBlock(pos, true);
            resetTarget();
        }
    }

    /** The crystal cracks: it is lost, with a sound, and the beam stops until another is seated. */
    private void crack(ServerLevel server) {
        crystal = ItemStack.EMPTY;
        heat = 0f;
        reach = 0;
        resetTarget();
        server.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.BLOCKS, 0.8f, 0.6f);
        setChanged();
    }

    // --- persistence --------------------------------------------------------------------------------

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!crystal.isEmpty())
            tag.put("Crystal", crystal.save(registries));
        tag.putBoolean("Engaged", engaged);
        tag.putFloat("Heat", heat);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        crystal = tag.contains("Crystal") ? ItemStack.parseOptional(registries, tag.getCompound("Crystal")) : ItemStack.EMPTY;
        engaged = tag.getBoolean("Engaged");
        heat = tag.getFloat("Heat");
    }
}
