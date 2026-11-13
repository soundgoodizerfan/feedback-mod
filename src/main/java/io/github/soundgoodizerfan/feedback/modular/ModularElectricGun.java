// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;

import io.github.soundgoodizerfan.feedback.core.electric.ElectricRules;
import io.github.soundgoodizerfan.feedback.machine.battery.BatteryStack;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.suit.ExoSuit;
import io.github.soundgoodizerfan.feedback.weapon.GunItem;
import io.github.soundgoodizerfan.feedback.weapon.GunMechanism;
import io.github.soundgoodizerfan.feedback.weapon.PulseShot;

/**
 * A pulse gun ({@link ElectricGunSpec}): a seat that shapes a pulse of electrical energy out of a battery carried in the item (or, for a
 * tethered seat, worn on the back or in a suit's bay). It is a {@link GunItem} like a firearm, so it is fired, aimed, moded and drawn
 * like one; {@link ElectricMechanism} is what differs. The battery is loaded and taken out in the inventory the way a bundle takes
 * items (click the gun with a battery on the cursor to load it, click it with an empty cursor to take it out) and keeps its own state, so
 * it goes back to a Charger as the item it is.
 *
 * <p>Everything a seat remembers between shots is on the stack as lazy clocks, so nothing ticks: heat, a cylinder's chambers, a bank's
 * charge, a flywheel's spin, how long the trigger has been held, the pumps, how long the sight has been up, a vent's lockout.
 *
 * <p>Mistakes wear by steps and never from uptime: a cell over the receiver's voltage band shocks the wielder and wears the receiver,
 * firing wet arcs to an uninsulated wielder, and a shot at the heat limit burns the wielder and wears the barrel.
 */
public class ModularElectricGun extends GunItem {

    static final String HEAT = "EgunHeat", HEAT_AT = "EgunHeatAt", CHAMBERS = "PulseChambers", CHAMBERS_AT = "PulseChambersAt",
            INDEX = "PulseIndex", PIPS = "PulsePips", PIPS_AT = "PulsePipsAt", SPIN = "PulseSpin", SPIN_AT = "PulseSpinAt",
            HELD = "PulseHeld", HELD_AT = "PulseHeldAt", HELD_TICKS = "PulseHeldTicks", PUMPS = "PulsePumps", HOT = "PulseHot", LOCK = "PulseLock",
            CUT = "PulseCut", AIM = "PulseAim";

    public ModularElectricGun(Properties properties) {
        super(properties);
    }

    @Override
    public GunMechanism mechanism() {
        return ElectricMechanism.INSTANCE;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ModularTooltip.add(stack, tooltip);
        ElectricGunSpec spec = specOf(stack);
        if (spec != null && spec.seat() == ElectricGunSpec.Seat.BACKPACK_TETHER) {
            tooltip.add(Component.translatable("tooltip.feedback.electric_gun.tether").withStyle(ChatFormatting.GRAY));
            return;
        }
        ItemStack cell = cell(stack);
        if (cell.isEmpty())
            tooltip.add(Component.translatable("tooltip.feedback.electric_gun.no_cell").withStyle(ChatFormatting.GRAY));
        else {
            CompoundTag state = BatteryStack.state(cell);
            tooltip.add(Component.translatable("tooltip.feedback.electric_gun.cell",
                    Math.round(BatteryStack.stateOfCharge(state) * 100f)).withStyle(ChatFormatting.GRAY));
        }
    }

    // --- the cell in the gun ---------------------------------------------------------------------

    /** The battery loaded, or empty. */
    public static ItemStack cell(ItemStack gun) {
        ItemContainerContents c = gun.get(DataComponents.CONTAINER);
        if (c == null)
            return ItemStack.EMPTY;
        for (ItemStack s : c.nonEmptyItems())
            return s.copy();
        return ItemStack.EMPTY;
    }

    public static void setCell(ItemStack gun, ItemStack cell) {
        if (cell.isEmpty())
            gun.remove(DataComponents.CONTAINER);
        else
            gun.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(cell.copyWithCount(1))));
    }

    /** Where a shot draws from and where what is left goes back: the gun's own cell, or a tether's worn battery. */
    public record Source(ItemStack battery, Consumer<ItemStack> write) {
        public static final Source NONE = new Source(ItemStack.EMPTY, b -> {
        });
    }

    /** The battery {@code wielder}'s {@code gun} draws from: its own cell; a tether takes a suit's bay, else a pack's battery fitting. */
    public static Source source(LivingEntity wielder, ItemStack gun, ElectricGunSpec spec) {
        if (spec.seat() != ElectricGunSpec.Seat.BACKPACK_TETHER)
            return new Source(cell(gun), b -> setCell(gun, b));
        if (!(wielder instanceof Player player))
            return Source.NONE;
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!chest.isEmpty() && ExoSuit.hasBay(chest) && !ExoSuit.battery(chest).isEmpty())
            return new Source(ExoSuit.battery(chest).copy(), b -> ExoSuit.setBattery(chest, b));
        ItemStack pack = ModularBackpack.wornWithBattery(player);
        if (!pack.isEmpty())
            return new Source(ModularBackpack.battery(pack).copy(), b -> ModularBackpack.setBattery(pack, b));
        return Source.NONE;
    }

    /** A battery on the cursor is loaded into the gun in the slot (or swapped for the one in it); an empty cursor takes the cell out. */
    @Override
    public boolean overrideOtherStackedOnMe(ItemStack gun, ItemStack other, Slot slot, ClickAction action, Player player, SlotAccess access) {
        if (action != ClickAction.SECONDARY || !slot.allowModification(player) || tethered(gun))
            return false;
        if (other.isEmpty()) {
            ItemStack held = cell(gun);
            if (held.isEmpty())
                return false;
            setCell(gun, ItemStack.EMPTY);
            access.set(held);
            player.playSound(SoundEvents.BUNDLE_REMOVE_ONE, 0.8f, 1f);
            return true;
        }
        if (!BatteryStack.is(other))
            return false;
        if (!fits(gun, other, player))
            return true;
        ItemStack old = cell(gun);
        setCell(gun, other);
        access.set(old);
        player.playSound(SoundEvents.BUNDLE_INSERT, 0.8f, 1f);
        return true;
    }

    private static boolean tethered(ItemStack gun) {
        ElectricGunSpec spec = specOf(gun);
        return spec != null && spec.seat() == ElectricGunSpec.Seat.BACKPACK_TETHER;
    }

    /** Whether the receiver's type takes a battery this big; says so if it does not. */
    private static boolean fits(ItemStack gun, ItemStack battery, Player player) {
        ElectricGunSpec spec = specOf(gun);
        CompoundTag state = BatteryStack.state(battery);
        if (spec == null || state == null || Math.max(1, state.getInt("Cells")) <= spec.maxCells())
            return true;
        player.displayClientMessage(Component.translatable("message.feedback.electric_gun.too_big", spec.maxCells()), true);
        return false;
    }

    /** The gun on the cursor takes a battery from the slot it is clicked on, or puts its cell into an empty slot. */
    @Override
    public boolean overrideStackedOnOther(ItemStack gun, Slot slot, ClickAction action, Player player) {
        if (action != ClickAction.SECONDARY || !slot.allowModification(player) || tethered(gun))
            return false;
        ItemStack other = slot.getItem();
        if (other.isEmpty()) {
            ItemStack held = cell(gun);
            if (held.isEmpty())
                return false;
            setCell(gun, ItemStack.EMPTY);
            slot.safeInsert(held);
            player.playSound(SoundEvents.BUNDLE_REMOVE_ONE, 0.8f, 1f);
            return true;
        }
        if (!BatteryStack.is(other))
            return false;
        if (!fits(gun, other, player))
            return true;
        ItemStack old = cell(gun);
        setCell(gun, other);
        slot.set(old);
        player.playSound(SoundEvents.BUNDLE_INSERT, 0.8f, 1f);
        return true;
    }

    // --- state on the stack (lazy clocks; the gun ticks nothing) -----------------------------------

    private static CompoundTag tag(ItemStack gun) {
        CustomData data = gun.get(DataComponents.CUSTOM_DATA);
        return data == null ? new CompoundTag() : data.copyTag();
    }

    private static void edit(ItemStack gun, Consumer<CompoundTag> change) {
        CustomData.update(DataComponents.CUSTOM_DATA, gun, change);
    }

    /** The gun's heat, 0 to 1, now: what the last shots left, cooled by the time since. */
    static float heat(ItemStack gun, long now, float shed) {
        CompoundTag tag = tag(gun);
        float seconds = Math.max(0L, now - tag.getLong(HEAT_AT)) / 20f;
        return Math.max(0f, tag.getFloat(HEAT) - seconds * ModularTuning.EGUN_HEAT_COOLING_PER_S * shed);
    }

    static void setHeat(ItemStack gun, float heat, long now) {
        edit(gun, tag -> {
            tag.putFloat(HEAT, Mth.clamp(heat, 0f, 1f));
            tag.putLong(HEAT_AT, now);
        });
    }

    /** The gun's heat now, 0 to 1 (what a HUD reads). */
    public static float heatFraction(ItemStack gun, long now) {
        ElectricGunSpec spec = specOf(gun);
        return spec == null ? 0f : heat(gun, now, spec.shed());
    }

    /** The share of the loaded cell's charge, 0 to 1, or -1 with no cell. */
    public static float chargeFraction(ItemStack gun) {
        ItemStack cell = cell(gun);
        return cell.isEmpty() ? -1f : BatteryStack.stateOfCharge(BatteryStack.state(cell));
    }

    /** Chambers a cylinder has charged now (a fraction while one fills), at most {@link ModularTuning#EGUN_CHAMBERS}. */
    static float chambers(ItemStack gun, long now, ElectricGunSpec spec, float volts, int cells) {
        CompoundTag tag = tag(gun);
        float held = tag.contains(CHAMBERS) ? tag.getFloat(CHAMBERS) : ModularTuning.EGUN_CHAMBERS;
        float seconds = Math.max(0L, now - tag.getLong(CHAMBERS_AT)) / 20f;
        return Math.min(ModularTuning.EGUN_CHAMBERS, held + seconds * spec.refillPerSecond(volts, cells));
    }

    static void setChambers(ItemStack gun, float chambers, long now) {
        edit(gun, tag -> {
            tag.putFloat(CHAMBERS, chambers);
            tag.putLong(CHAMBERS_AT, now);
            tag.putInt(INDEX, tag.getInt(INDEX) + 1);
        });
    }

    /** Which chamber of the cylinder is at the muzzle: the shots fired, so the drum turns one place a shot. */
    public static int chamberIndex(ItemStack gun) {
        return tag(gun).getInt(INDEX);
    }

    /** Charged chambers of a cylinder as the HUD shows them (the battery is not read: its refill rate is taken as the reference). */
    public static float chambersShown(ItemStack gun, long now) {
        ElectricGunSpec spec = specOf(gun);
        ItemStack cell = cell(gun);
        if (spec == null || spec.seat() != ElectricGunSpec.Seat.CYLINDER)
            return 0f;
        CompoundTag state = cell.isEmpty() ? null : BatteryStack.state(cell);
        return chambers(gun, now, spec, state == null ? ModularTuning.EGUN_REF_VOLTS : BatteryStack.terminalVolts(state),
                state == null ? 2 : Math.max(1, state.getInt("Cells")));
    }

    /** Shots a bank holds now, of {@code full}: it refills all at once when its recharge is done. */
    static int pips(ItemStack gun, long now, int full) {
        CompoundTag tag = tag(gun);
        if (!tag.contains(PIPS))
            return full;
        int held = tag.getInt(PIPS);
        return held > 0 ? held : now >= tag.getLong(PIPS_AT) ? full : 0;
    }

    /** A bank spent one: when it is empty its recharge of {@code rechargeTicks} begins. */
    static void spendPip(ItemStack gun, long now, int full, int rechargeTicks) {
        int left = pips(gun, now, full) - 1;
        edit(gun, tag -> {
            tag.putInt(PIPS, left);
            tag.putLong(PIPS_AT, now + rechargeTicks);
        });
    }

    /** What the HUD shows of a bank: shots held over shots it holds full, or the recharge so far while empty. */
    public static float bankShown(ItemStack gun, long now) {
        ElectricGunSpec spec = specOf(gun);
        if (spec != null && spec.seat() == ElectricGunSpec.Seat.CORE_BREECH)
            return hot(gun) ? 0f : 1f;
        if (spec == null || spec.seat() != ElectricGunSpec.Seat.CAPACITOR_BANK && spec.seat() != ElectricGunSpec.Seat.BURST_BANK)
            return 0f;
        int full = spec.seat() == ElectricGunSpec.Seat.BURST_BANK ? io.github.soundgoodizerfan.feedback.core.FTuning.GUN_BURST_ROUNDS : 1;
        int have = pips(gun, now, full);
        if (have > 0)
            return have / (float) full;
        CompoundTag tag = tag(gun);
        int recharge = Math.max(1, spec.bankRechargeTicks(2));
        return Mth.clamp(1f - (tag.getLong(PIPS_AT) - now) / (float) recharge, 0f, 0.99f) / full;
    }

    // --- the trigger held: a flywheel's spin, a hold charge ----------------------------------------

    /** A flywheel's spin now, 0 to 1: it builds while the trigger is down and falls fast after. */
    public static float spin(ItemStack gun, long now, ElectricGunSpec spec) {
        CompoundTag tag = tag(gun);
        float s = tag.getFloat(SPIN);
        float t = Math.max(0L, now - tag.getLong(SPIN_AT));
        return tag.getBoolean(HELD) ? Math.min(1f, s + t / spec.spinUpTicks()) : Math.max(0f, s - t / ModularTuning.EGUN_FLY_DECAY_TICKS);
    }

    /** The spin the HUD and the model read, or 0 for a gun with no flywheel. */
    public static float spinShown(ItemStack gun, long now) {
        ElectricGunSpec spec = specOf(gun);
        return spec == null || spec.seat() != ElectricGunSpec.Seat.FLYWHEEL ? 0f : spin(gun, now, spec);
    }

    /** The trigger went down or came up: spin is carried to now first, and the time held is kept for a hold charge. */
    public static void setHeld(ItemStack gun, boolean down, long now) {
        ElectricGunSpec spec = specOf(gun);
        if (spec == null)
            return;
        float spun = spec.seat() == ElectricGunSpec.Seat.FLYWHEEL ? spin(gun, now, spec) : 0f;
        edit(gun, tag -> {
            boolean was = tag.getBoolean(HELD);
            if (down && !was)
                tag.putLong(HELD_AT, now);
            if (!down && was)
                tag.putLong(HELD_TICKS, Math.max(0L, now - tag.getLong(HELD_AT)));
            tag.putFloat(SPIN, spun);
            tag.putLong(SPIN_AT, now);
            tag.putBoolean(HELD, down);
        });
    }

    /** Whether the trigger is down, as the gun last heard. */
    public static boolean held(ItemStack gun) {
        return tag(gun).getBoolean(HELD);
    }

    /** The hold charge, 0 to 1: the time the trigger has been down (or was, before it came up), over the most it builds. */
    public static float holdShare(ItemStack gun, long now) {
        CompoundTag tag = tag(gun);
        float ticks = tag.getBoolean(HELD) ? Math.max(0L, now - tag.getLong(HELD_AT)) : tag.getLong(HELD_TICKS);
        return Mth.clamp(ticks / ModularTuning.EGUN_HOLD_MAX_TICKS, 0f, 1f);
    }

    /** A shot spent the hold: a fresh press starts over. */
    static void clearHold(ItemStack gun, long now) {
        edit(gun, tag -> {
            tag.putLong(HELD_TICKS, 0L);
            if (tag.getBoolean(HELD))
                tag.putLong(HELD_AT, now);
        });
    }

    /** Whether a core breech's cores are hot from the last shot: the gun is locked until it is broken open and vented. */
    public static boolean hot(ItemStack gun) {
        return tag(gun).getBoolean(HOT);
    }

    static void setHot(ItemStack gun, boolean hot) {
        edit(gun, tag -> tag.putBoolean(HOT, hot));
    }

    public static int pumps(ItemStack gun) {
        return tag(gun).getInt(PUMPS);
    }

    static void setPumps(ItemStack gun, int n) {
        edit(gun, tag -> tag.putInt(PUMPS, n));
    }

    /** The sight went up or came down: zoom charge counts from the moment it went up. */
    public static void setAim(ItemStack gun, boolean aiming, long now) {
        if (aiming == tag(gun).getLong(AIM) > 0L)
            return;
        edit(gun, tag -> tag.putLong(AIM, aiming ? now + 1L : 0L));
    }

    /** The scope zoom charge, 0 to 1: how long the sight has been up. */
    public static float zoomShare(ItemStack gun, long now) {
        long since = tag(gun).getLong(AIM);
        return since <= 0L ? 0f : Mth.clamp((now - (since - 1L)) / (float) ModularTuning.EGUN_ZOOM_TICKS, 0f, 1f);
    }

    /** A shot resets the zoom charge, if the sight is still up it builds again. */
    static void resetZoom(ItemStack gun, long now) {
        edit(gun, tag -> {
            if (tag.getLong(AIM) > 0L)
                tag.putLong(AIM, now + 1L);
        });
    }

    static long lockUntil(ItemStack gun) {
        return tag(gun).getLong(LOCK);
    }

    static void setLock(ItemStack gun, long until) {
        edit(gun, tag -> tag.putLong(LOCK, until));
    }

    static boolean tripped(ItemStack gun) {
        return tag(gun).getBoolean(CUT);
    }

    static void setTripped(ItemStack gun, boolean on) {
        if (tripped(gun) != on)
            edit(gun, tag -> tag.putBoolean(CUT, on));
    }

    /** The spec of {@code gun}'s design, or null. */
    public static ElectricGunSpec specOf(ItemStack gun) {
        return spec(gun).orElse(null);
    }

    static Optional<ElectricGunSpec> spec(ItemStack gun) {
        ModularDesign design = ModularStacks.design(gun);
        ModularType type = design == null ? null : ModularTypes.known(design.type());
        return type == null ? Optional.empty() : ElectricGunSpec.of(design, type, MaterialTable.get().materials());
    }

    /** The beam colour the drawing was given. */
    public static int colourOf(ItemStack gun) {
        ModularDesign design = ModularStacks.design(gun);
        return design == null ? ModularDesign.DEFAULT_COLOUR : design.colour();
    }

    // --- what a mistake costs ----------------------------------------------------------------------

    /** What a mistake costs, by steps: nothing from correct use. */
    static void mistakes(ServerPlayer player, ItemStack gun, ElectricGunSpec spec, float volts, InteractionHand hand) {
        EquipmentSlot slot = LivingEntity.getSlotForHand(hand);
        // A cell over the receiver's band shocks the wielder and wears the receiver.
        float excess = ElectricRules.overVoltage(volts, spec.band());
        if (excess > 0f) {
            player.hurt(player.damageSources().lightningBolt(), ModularTuning.EGUN_SHOCK_PER_EXCESS * excess);
            gun.hurtAndBreak(ModularStacks.cap(gun, 1 + (int) excess), player, slot);
        }
        // Wet, the arc finds the wielder; only insulation stops it.
        if (player.isInWaterOrRain() && !PulseShot.insulated(player, volts))
            player.hurt(player.damageSources().lightningBolt(), ModularTuning.EGUN_SHOCK);
    }

    /** A shot fired at the heat limit with nothing to govern it: it burns the wielder and wears the barrel. */
    static void overheated(LivingEntity wielder, ItemStack gun, InteractionHand hand) {
        wielder.hurt(wielder.damageSources().onFire(), ModularTuning.EGUN_OVERHEAT_BURN);
        gun.hurtAndBreak(ModularStacks.cap(gun, 2), wielder, LivingEntity.getSlotForHand(hand));
        wielder.level().playSound(null, wielder.blockPosition(), io.github.soundgoodizerfan.feedback.registry.FSounds.GUN_VENT.get(), SoundSource.PLAYERS, 0.7f, 1.2f);
    }
}
