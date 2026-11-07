// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.creeper;

import io.github.soundgoodizerfan.feedback.combat.skill.SkillTree;
import io.github.soundgoodizerfan.feedback.combat.skill.Skills;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.hazard.Protection;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FItems;
import io.github.soundgoodizerfan.feedback.registry.FWeapons;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.BlockPositionSource;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Holder;

/**
 * A penned creeper's reserve (roadmap 6.6). The pen keeps no live entity: a charge, a stomach of two halves (an oxidiser
 * and a fuel, told apart by the feed material's {@code oxygen_balance}), and the game time it was last brought up to date.
 * It ticks once a second, and a pen that was unloaded catches up in one step on its first tick back: growth while the
 * stomach lasted, then time spent full. A reserve full for {@link FTuning#CREEPER_WARNING_TICKS} goes off, loaded or
 * caught up, and a spark within range sets off any pen holding a charge. Shearing takes the whole reserve as a sac and
 * leaves the creeper empty, which cannot go off until it grows again, and it grows only while fed.
 */
public class CreeperPenBlockEntity extends BlockEntity implements GameEventListener.Provider<CreeperPenBlockEntity.Ear> {

    private static final int STEP = 20;

    private final Ear ear = new Ear();
    private boolean occupied;
    private boolean charged;
    private float charge;
    private float oxidiser;
    private float fuel;
    /** Ticks the reserve has stood full. */
    private int fullFor;
    private long last;

    public CreeperPenBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.CREEPER_PEN.get(), pos, state);
    }

    public boolean occupied() {
        return occupied;
    }

    public boolean charged() {
        return charged;
    }

    public float charge() {
        return charge;
    }

    public float cap() {
        return FTuning.CREEPER_CAP * (charged ? FTuning.CREEPER_CHARGED_CAP : 1f);
    }

    /** Ticks of warning left before the blast, or -1 while the reserve is not full. */
    public int warningLeft() {
        return fullFor <= 0 ? -1 : Math.max(0, FTuning.CREEPER_WARNING_TICKS - fullFor);
    }

    public float stomachOxidiser() {
        return oxidiser;
    }

    public float stomachFuel() {
        return fuel;
    }

    public int fullFor() {
        return fullFor;
    }

    public void admit(boolean power, float carried, int stoodFull) {
        occupied = true;
        charged = power;
        charge = Math.min(carried, cap());
        fullFor = stoodFull;
        last = level == null ? 0 : level.getGameTime();
        refreshLook();
    }

    /** A bolt hit the pen: a creeper in it is charged for good. */
    public void struck() {
        if (occupied && !charged) {
            charged = true;
            refreshLook();
        }
    }

    /**
     * Feed it from a stack: a material with a combustion balance (positive fills the oxidiser half, negative the fuel
     * half) or a propellant such as gunpowder, which is both halves at once.
     *
     * @return items taken.
     */
    public int feed(ItemStack stack) {
        if (level == null)
            return 0;
        advance(level.getGameTime());
        var found = Material.find(MaterialTable.get().materials(), stack);
        if (found.isEmpty())
            return 0;
        var combustion = found.get().material().combustion().orElse(null);
        if (combustion == null)
            return 0;
        // A propellant (gunpowder) is a ready mix of both halves; any other material is the half its balance says.
        boolean mix = combustion.chamber().isPresent();
        if (!mix && combustion.oxygenBalance() == 0f)
            return 0;
        float each = mix ? FTuning.CREEPER_MIX_FEED : Math.abs(combustion.oxygenBalance());
        boolean oxi = combustion.oxygenBalance() > 0f;
        int taken = 0;
        while (taken < stack.getCount()) {
            boolean full = mix ? oxidiser >= FTuning.CREEPER_STOMACH || fuel >= FTuning.CREEPER_STOMACH
                    : (oxi ? oxidiser : fuel) >= FTuning.CREEPER_STOMACH;
            if (full)
                break;
            if (mix) {
                oxidiser += each;
                fuel += each;
            } else if (oxi)
                oxidiser += each;
            else
                fuel += each;
            taken++;
        }
        if (taken > 0)
            changed();
        return taken;
    }

    /** What is left in the stomach when the pen is broken: nothing recoverable; the creeper keeps it. */
    public void dropFeed() {
        oxidiser = 0;
        fuel = 0;
    }

    public void shear(Player player, ItemStack tool, InteractionHand hand) {
        if (level == null)
            return;
        advance(level.getGameTime());
        if (!occupied)
            return;
        if (charge < FTuning.CREEPER_SHEAR_MIN) {
            player.displayClientMessage(Component.translatable("message.feedback.creeper.nothing_to_shear"), true);
            return;
        }
        float fill = Math.min(1f, charge / cap());
        float risk = FTuning.CREEPER_SHEAR_SHOCK * fill * fill;
        if (player.isShiftKeyDown())
            risk *= FTuning.CREEPER_SHEAR_SNEAK;
        if (Protection.handlesUpToTu(player) > FTuning.CONTACT_BURN_TU.value())
            risk *= FTuning.CREEPER_SHEAR_PADDED;
        if (Skills.modifier(player, SkillTree.Figure.CHARGE_TAMP) != 0f)
            risk *= FTuning.CREEPER_SHEAR_CAREFUL;
        if (level.random.nextFloat() < risk) {
            detonate();
            return;
        }
        ItemStack sac = new ItemStack(FItems.CREEPER_SAC.get());
        sac.set(FDataComponents.CREEPER_CHARGE.get(), charge);
        Block.popResource(level, worldPosition.above(), sac);
        charge = 0;
        fullFor = 0;
        if (tool.isDamageableItem())
            tool.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        level.playSound(null, worldPosition, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 1f, 1f);
        changed();
    }

    /** Set off now: the pen goes and the reserve blasts. A pen with an empty creeper has nothing to blast. */
    public void detonate() {
        if (!(level instanceof ServerLevel server) || !occupied)
            return;
        float units = charge;
        boolean power = charged;
        BlockPos at = worldPosition;
        occupied = false;
        charge = 0;
        server.removeBlock(at, false);
        CreeperBlast.explode(server, at, units, power);
    }

    /** Brings the reserve up to {@code now}: growth while fed, then time stood full. */
    private void advance(long now) {
        long dt = now - last;
        last = now;
        if (!occupied || dt <= 0 || level == null)
            return;
        float cap = cap();
        float before = charge + oxidiser + fuel + fullFor;
        float rate = FTuning.CREEPER_CAP / FTuning.CREEPER_FILL_TICKS;
        if (charge < cap && oxidiser > 0f && fuel > 0f) {
            float toCap = (cap - charge) / rate;
            float feedTicks = Math.min(oxidiser, fuel) / (FTuning.CREEPER_FEED_PER_CHARGE * rate);
            float grew = Math.min(dt, Math.min(toCap, feedTicks));
            float units = grew * rate;
            charge = Math.min(cap, charge + units);
            oxidiser = Math.max(0f, oxidiser - units * FTuning.CREEPER_FEED_PER_CHARGE);
            fuel = Math.max(0f, fuel - units * FTuning.CREEPER_FEED_PER_CHARGE);
            dt -= (long) Math.ceil(grew);
        }
        if (charge >= cap - 1e-3f)
            fullFor += (int) Math.min(Integer.MAX_VALUE / 2, Math.max(0, dt));
        else
            fullFor = 0;
        if (fullFor >= FTuning.CREEPER_WARNING_TICKS) {
            detonate();
            return;
        }
        if (charge + oxidiser + fuel + fullFor != before)
            changed();
    }

    public void tickServer() {
        if (level == null || !occupied || (level.getGameTime() + worldPosition.hashCode()) % STEP != 0)
            return;
        boolean wasFull = fullFor > 0;
        advance(level.getGameTime());
        if (!occupied || !(level instanceof ServerLevel server))
            return;
        if (fullFor > 0) {
            if (!wasFull)
                level.gameEvent(GameEvent.PRIME_FUSE, worldPosition, GameEvent.Context.of(getBlockState()));
            server.playSound(null, worldPosition, SoundEvents.CREEPER_PRIMED, SoundSource.BLOCKS, 1f, 0.8f);
            server.sendParticles(ParticleTypes.SMOKE, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0,
                    worldPosition.getZ() + 0.5, 4, 0.2, 0.2, 0.2, 0.01);
        }
    }

    private void refreshLook() {
        if (level == null || level.isClientSide)
            return;
        BlockState s = getBlockState();
        int stage = !occupied ? 0 : fullFor > 0 ? 2 : 1;
        if (s.getValue(CreeperPenBlock.STAGE) != stage || s.getValue(CreeperPenBlock.CHARGED) != charged)
            level.setBlock(worldPosition, s.setValue(CreeperPenBlock.STAGE, stage).setValue(CreeperPenBlock.CHARGED, charged), 3);
        setChanged();
    }

    private void changed() {
        refreshLook();
        setChanged();
    }

    @Override
    public Ear getListener() {
        return ear;
    }

    /** Hears sparks: a spark within range sets off a pen holding a charge. */
    public class Ear implements GameEventListener {
        private final BlockPositionSource source = new BlockPositionSource(getBlockPos());

        @Override
        public net.minecraft.world.level.gameevent.PositionSource getListenerSource() {
            return source;
        }

        @Override
        public int getListenerRadius() {
            return FTuning.SPARK_RANGE_BLOCKS;
        }

        @Override
        public boolean handleGameEvent(ServerLevel level, Holder<GameEvent> event, GameEvent.Context context, Vec3 origin) {
            if (!event.is(FWeapons.SPARK.getId()) || isRemoved() || !occupied || charge < FTuning.CREEPER_SHEAR_MIN)
                return false;
            detonate();
            return true;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Occupied", occupied);
        tag.putBoolean("Charged", charged);
        tag.putFloat("Charge", charge);
        tag.putFloat("Oxidiser", oxidiser);
        tag.putFloat("Fuel", fuel);
        tag.putInt("FullFor", fullFor);
        tag.putLong("Last", last);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        occupied = tag.getBoolean("Occupied");
        charged = tag.getBoolean("Charged");
        charge = tag.getFloat("Charge");
        oxidiser = tag.getFloat("Oxidiser");
        fuel = tag.getFloat("Fuel");
        fullFor = tag.getInt("FullFor");
        last = tag.getLong("Last");
    }
}
