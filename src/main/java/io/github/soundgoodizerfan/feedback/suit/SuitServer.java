// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.suit;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.machine.battery.BatteryStack;
import io.github.soundgoodizerfan.feedback.modular.Modular;
import io.github.soundgoodizerfan.feedback.net.SuitPayload;
import io.github.soundgoodizerfan.feedback.net.SuitSyncPayload;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.vehicle.AirDensityTable;

import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The powered suit's server half: one {@link Run} per wearing player, held in memory and written back to the chestplate every
 * {@link SuitTuning#WRITE_TICKS}, so the stack is not resent every tick. It does nothing for a player with no suit worn (one stack check a
 * tick). Wear comes only from misuse: asking a servo to work while the suit is cut out by heat, and a dash into a wall.
 *
 * <p>Each tick the worn abilities draw amps from the bay's battery as a share of their servo's drive; the waste share of that heats the
 * suit, which sheds toward the ambient air (faster in water, rain and cold). Past {@link SuitTuning#LIMIT_TU} the servos cut out until
 * it is under {@link SuitTuning#RESUME_TU}: the suit is then only its plate and its weight.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class SuitServer {

    private SuitServer() {
    }

    private static final ResourceLocation SPEED = Feedback.id("suit_strength_speed"), DAMAGE = Feedback.id("suit_strength_damage"),
            BREAK = Feedback.id("suit_strength_break"), JUMP = Feedback.id("suit_jump"), FALL = Feedback.id("suit_fall"),
            ARMOUR = Feedback.id("suit_plating"), TOUGH = Feedback.id("suit_plating_toughness");

    /** One wearer's live state. */
    private static final class Run {
        float heat, pending, draw, carry = 1f, coldMult = 1f, lastSpeed;
        boolean cut, dash, hopped, powered, plating, jumpOn, nv, asked;
        int toggles, sinceWrite, fxSig = -1, stun;
        SuitSyncPayload sent;
    }

    private static final Map<UUID, Run> RUNS = new HashMap<>();

    static float carryFactor(net.minecraft.world.entity.player.Player player) {
        Run r = RUNS.get(player.getUUID());
        return r == null ? 1f : r.carry;
    }

    private static Run run(ServerPlayer p, ExoSuit.Rig rig) {
        return RUNS.computeIfAbsent(p.getUUID(), id -> {
            Run r = new Run();
            SuitState st = ExoSuit.state(rig.chest());
            r.heat = st.heat();
            r.cut = st.cut();
            r.toggles = st.toggles();
            return r;
        });
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer p)
            tick(p);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Run r = RUNS.remove(event.getEntity().getUUID());
        if (r != null)
            flush(event.getEntity().getItemBySlot(EquipmentSlot.CHEST), r);
        ThermalScan.forget(event.getEntity().getUUID());
    }

    private static void tick(ServerPlayer p) {
        Run r = RUNS.get(p.getUUID());
        ItemStack chest = p.getItemBySlot(EquipmentSlot.CHEST);
        if (r == null && (!(chest.getItem() instanceof Modular) || !ExoSuit.hasBay(chest)))
            return;
        ExoSuit.Rig rig = ExoSuit.of(p);
        if (!rig.worn()) {
            if (r != null)
                release(p, r);
            return;
        }
        if (r == null)
            r = run(p, rig);

        CompoundTag bt = BatteryStack.state(ExoSuit.battery(chest));
        boolean ready = bt != null && BatteryStack.ready(bt);
        float left = ready ? BatteryStack.charge(bt) - r.pending : 0f;
        boolean powered = ready && left > 0f && !r.cut;
        int on = r.toggles & rig.available();

        if ((on & Ability.JUMP_DASH.bit()) == 0)
            r.dash = false;
        float drawn = 0f, waste = 0f;
        if (powered) {
            if ((on & Ability.STRENGTH.bit()) != 0) {
                float d = SuitTuning.STRENGTH_DRAW * rig.driveChest();
                drawn += d;
                waste += d * rig.lossChest();
            }
            if ((on & Ability.JUMP_DASH.bit()) != 0) {
                float drive = rig.driveLegs() + rig.driveFeet();
                float d = SuitTuning.JUMP_IDLE_DRAW * drive;
                if (r.dash)
                    d += dash(p, r, drive, rig.driveLegs() > 0f);
                drawn += d;
                waste += d * rig.lossLegs();
            }
            float sensor = ((on & Ability.NIGHT.bit()) != 0 ? SuitTuning.NIGHT_AMPS : 0f) + ((on & Ability.THERMAL.bit()) != 0 ? SuitTuning.THERMAL_AMPS : 0f);
            drawn += sensor;
            waste += sensor * rig.lossChest();
            if ((on & Ability.THERMAL.bit()) != 0 && p.tickCount % SuitTuning.THERMAL_SCAN_TICKS == 0)
                ThermalScan.scan(p, Math.min(SuitTuning.THERMAL_MAX_RANGE, SuitTuning.THERMAL_BASE_RANGE + SuitTuning.THERMAL_RANGE_PER_CELL * rig.sense()));
        } else {
            r.dash = false;
        }
        r.pending += drawn;
        r.draw = drawn;

        // heat: waste in, the world takes it out
        float amb = AmbientTable.at(p.level()).value();
        float heat = r.heat <= 0f ? amb : r.heat;
        heat += waste * SuitTuning.HEAT_PER_WASTE;
        if (p.tickCount % 20 == 0)
            r.coldMult = p.level().getBiome(p.blockPosition()).value().coldEnoughToSnow(p.blockPosition()) ? SuitTuning.COOL_COLD : 1f;
        float mult = p.isInWater() ? SuitTuning.COOL_WATER : p.isInWaterOrRain() ? SuitTuning.COOL_RAIN : r.coldMult;
        heat -= (heat - amb) * SuitTuning.COOL_RATE * mult;
        if (!r.cut && heat > SuitTuning.LIMIT_TU)
            r.cut = true;
        else if (r.cut && heat < SuitTuning.RESUME_TU)
            r.cut = false;
        r.heat = heat;
        // the wearer keeps asking for thrust while the servos are cut out: that, and nothing idle, wears the chestplate
        if (r.cut && r.asked && p.tickCount % SuitTuning.OVERHEAT_WEAR_TICKS == 0)
            chest.hurtAndBreak(1, p, EquipmentSlot.CHEST);

        int sig = (powered ? 1 : 0) | on << 1;
        if (sig != r.fxSig || p.tickCount % 40 == 0) {
            effects(p, r, rig, powered, on);
            r.fxSig = sig;
        }
        r.powered = powered;
        boolean nv = powered && (on & Ability.NIGHT.bit()) != 0;
        if (nv && p.tickCount % 20 == 0)
            p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION, 260, 0, true, false, false));
        else if (!nv && r.nv)
            dropNightSight(p);
        r.nv = nv;

        if (++r.sinceWrite >= SuitTuning.WRITE_TICKS) {
            flush(chest, r);
            r.sinceWrite = 0;
        }
        sync(p, r, rig, chest, bt, left);
    }

    /** Held thrust: forward along the look, thinned by the air, with a short hop off the ground. Returns the amps it draws. */
    private static float dash(ServerPlayer p, Run r, float drive, boolean legs) {
        float density = Math.min(1f, AirDensityTable.at(p.level(), p.getY()) / SuitTuning.REF_AIR_DENSITY);
        Vec3 v = p.getDeltaMovement();
        double speed = Math.hypot(v.x, v.z);
        if (r.stun > 0) {
            r.stun--;
        } else if (p.horizontalCollision && r.lastSpeed > SuitTuning.DASH_WALL_SPEED) {
            EquipmentSlot driver = legs ? EquipmentSlot.LEGS : EquipmentSlot.FEET;
            p.getItemBySlot(driver).hurtAndBreak(1, p, driver);
            r.stun = 20;
        } else if (density > 0.05f) {
            Vec3 look = p.getLookAngle();
            double hx = look.x, hz = look.z, hl = Math.hypot(hx, hz);
            if (hl > 1e-4) {
                double accel = SuitTuning.DASH_ACCEL * Math.min(1f, drive / SuitTuning.DASH_REF_AMPS) * density;
                double top = SuitTuning.DASH_TOP_SPEED * density;
                double nx = v.x + hx / hl * accel, nz = v.z + hz / hl * accel, ns = Math.hypot(nx, nz);
                if (ns > Math.max(top, speed)) {
                    double k = Math.max(top, speed) / ns;
                    nx *= k;
                    nz *= k;
                }
                double ny = v.y;
                if (p.onGround() && !r.hopped) {
                    ny = Math.max(ny, SuitTuning.DASH_HOP * density);
                    r.hopped = true;
                }
                p.setDeltaMovement(nx, ny, nz);
                p.hurtMarked = true;
            }
        }
        if (p.onGround() && !r.dash)
            r.hopped = false;
        r.lastSpeed = (float) speed;
        return SuitTuning.DASH_DRAW * drive;
    }

    /** Puts the suit's effects on the wearer as transient modifiers, or takes them off. */
    private static void effects(ServerPlayer p, Run r, ExoSuit.Rig rig, boolean powered, int on) {
        boolean str = powered && (on & Ability.STRENGTH.bit()) != 0, jump = powered && (on & Ability.JUMP_DASH.bit()) != 0,
                plate = powered && (on & Ability.PLATING.bit()) != 0;
        float dc = rig.driveChest(), legs = rig.driveLegs() + rig.driveFeet();
        mod(p, Attributes.MOVEMENT_SPEED, SPEED, str ? rig.slow() : 0f, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        mod(p, Attributes.ATTACK_DAMAGE, DAMAGE, str ? Math.min(SuitTuning.STRENGTH_DAMAGE_MAX, SuitTuning.STRENGTH_DAMAGE_PER_AMP * dc) : 0f,
                AttributeModifier.Operation.ADD_VALUE);
        mod(p, Attributes.BLOCK_BREAK_SPEED, BREAK, str ? Math.min(SuitTuning.STRENGTH_BREAK_MAX, SuitTuning.STRENGTH_BREAK_PER_AMP * dc) : 0f,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        mod(p, Attributes.JUMP_STRENGTH, JUMP, jump ? Math.min(SuitTuning.JUMP_MAX, SuitTuning.JUMP_PER_AMP * legs) : 0f,
                AttributeModifier.Operation.ADD_VALUE);
        mod(p, Attributes.SAFE_FALL_DISTANCE, FALL, jump ? Math.min(SuitTuning.FALL_MAX, SuitTuning.FALL_PER_AMP * legs) : 0f,
                AttributeModifier.Operation.ADD_VALUE);
        mod(p, Attributes.ARMOR, ARMOUR, plate ? Math.min(SuitTuning.PLATING_ARMOUR_MAX, SuitTuning.PLATING_ARMOUR_PER_AMP * dc) : 0f,
                AttributeModifier.Operation.ADD_VALUE);
        mod(p, Attributes.ARMOR_TOUGHNESS, TOUGH, plate ? SuitTuning.PLATING_TOUGHNESS : 0f, AttributeModifier.Operation.ADD_VALUE);
        r.carry = str ? 1f + Math.min(SuitTuning.STRENGTH_CARRY_MAX, SuitTuning.STRENGTH_CARRY_PER_AMP * dc) : 1f;
        r.plating = plate;
        r.jumpOn = jump;
    }

    private static void mod(ServerPlayer p, Holder<Attribute> attribute, ResourceLocation id, float value, AttributeModifier.Operation op) {
        AttributeInstance a = p.getAttribute(attribute);
        if (a == null)
            return;
        if (value == 0f)
            a.removeModifier(id);
        else
            a.addOrUpdateTransientModifier(new AttributeModifier(id, value, op));
    }

    private static void clear(ServerPlayer p) {
        mod(p, Attributes.MOVEMENT_SPEED, SPEED, 0f, AttributeModifier.Operation.ADD_VALUE);
        mod(p, Attributes.ATTACK_DAMAGE, DAMAGE, 0f, AttributeModifier.Operation.ADD_VALUE);
        mod(p, Attributes.BLOCK_BREAK_SPEED, BREAK, 0f, AttributeModifier.Operation.ADD_VALUE);
        mod(p, Attributes.JUMP_STRENGTH, JUMP, 0f, AttributeModifier.Operation.ADD_VALUE);
        mod(p, Attributes.SAFE_FALL_DISTANCE, FALL, 0f, AttributeModifier.Operation.ADD_VALUE);
        mod(p, Attributes.ARMOR, ARMOUR, 0f, AttributeModifier.Operation.ADD_VALUE);
        mod(p, Attributes.ARMOR_TOUGHNESS, TOUGH, 0f, AttributeModifier.Operation.ADD_VALUE);
    }

    /** Takes off the night-vision the suit lent, leaving a longer or visible one (a potion's) alone. */
    private static void dropNightSight(ServerPlayer p) {
        var inst = p.getEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION);
        if (inst != null && inst.isAmbient() && !inst.isVisible() && inst.getDuration() <= 260)
            p.removeEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION);
    }

    /** The suit came off (or lost its bay): the effects go and the client is told. */
    private static void release(ServerPlayer p, Run r) {
        clear(p);
        flush(p.getItemBySlot(EquipmentSlot.CHEST), r);
        if (r.nv)
            dropNightSight(p);
        RUNS.remove(p.getUUID());
        PacketDistributor.sendToPlayer(p, SuitSyncPayload.OFF);
    }

    /** Writes the drawn charge off the battery and the state onto the chestplate. */
    private static void flush(ItemStack chest, Run r) {
        ItemStack bat = ExoSuit.battery(chest);
        CompoundTag t = BatteryStack.state(bat);
        if (t != null && r.pending > 0f) {
            ItemStack copy = bat.copy();
            CompoundTag next = t.copy();
            BatteryStack.draw(next, r.pending);
            copy.set(FDataComponents.BATTERY.get(), next);
            ExoSuit.setBattery(chest, copy);
        }
        r.pending = 0f;
        SuitState now = new SuitState(r.heat, r.toggles, r.cut);
        if (!now.equals(chest.get(FDataComponents.SUIT.get())))
            chest.set(FDataComponents.SUIT.get(), now);
    }

    private static void sync(ServerPlayer p, Run r, ExoSuit.Rig rig, ItemStack chest, CompoundTag bt, float left) {
        float charge = bt == null || !BatteryStack.ready(bt) ? 0f : Math.max(0f, left) / Math.max(1e-3f, BatteryStack.effectiveCapacity(bt));
        SuitSyncPayload now = new SuitSyncPayload(true, rig.available(), r.toggles & rig.available(), Math.round(Math.min(1f, charge) * 64f) / 64f, bt != null,
                Math.round(r.heat), r.cut, p.getPersistentData().getBoolean("feedback_pack_over"), rig.gauge(), Math.round(rig.mass() * 10f) / 10f, Math.round(r.draw * 10f) / 10f, SuitTuning.LIMIT_TU);
        if (!now.equals(r.sent)) {
            r.sent = now;
            PacketDistributor.sendToPlayer(p, now);
        }
    }

    // --- events -------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onHurt(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p))
            return;
        Run r = RUNS.get(p.getUUID());
        if (r == null || !r.plating)
            return;
        float amount = event.getNewDamage() * SuitTuning.PLATING_PER_DAMAGE;
        r.pending += amount;
        r.heat += event.getNewDamage() * SuitTuning.PLATING_WASTE * SuitTuning.HEAT_PER_WASTE;
    }

    @SubscribeEvent
    public static void onJump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p))
            return;
        Run r = RUNS.get(p.getUUID());
        if (r != null && r.jumpOn)
            r.pending += SuitTuning.JUMP_DRAW;
    }

    // --- requests ---------------------------------------------------------------------------------------

    public static void handle(ServerPlayer p, byte action, int arg) {
        ExoSuit.Rig rig = ExoSuit.of(p);
        if (!rig.worn())
            return;
        Run r = run(p, rig);
        ItemStack chest = rig.chest();
        switch (action) {
            case SuitPayload.TOGGLE -> {
                Ability a = Ability.of(arg);
                if (a != null && rig.can(a)) {
                    r.toggles ^= a.bit();
                    flush(chest, r);
                    r.fxSig = -1;
                }
            }
            case SuitPayload.DASH -> {
                r.asked = arg == 1 && r.cut;
                r.dash = arg == 1 && r.powered && (r.toggles & Ability.JUMP_DASH.bit()) != 0 && rig.can(Ability.JUMP_DASH);
            }
            case SuitPayload.BAY_PUT -> {
                if (!ExoSuit.battery(chest).isEmpty() || arg < 0 || arg >= p.getInventory().getContainerSize())
                    return;
                ItemStack from = p.getInventory().getItem(arg);
                if (BatteryStack.is(from) && from.getCount() == 1) {
                    ExoSuit.setBattery(chest, from.copy());
                    p.getInventory().setItem(arg, ItemStack.EMPTY);
                    r.pending = 0f;
                }
            }
            case SuitPayload.BAY_TAKE -> {
                flush(chest, r);
                ItemStack bat = ExoSuit.battery(chest).copy();
                if (!bat.isEmpty()) {
                    ExoSuit.setBattery(chest, ItemStack.EMPTY);
                    if (!p.getInventory().add(bat))
                        p.drop(bat, false);
                }
            }
            default -> { }
        }
        r.sent = null;
    }
}
