// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular;

import java.util.List;
import java.util.Map;
import java.util.Optional;


import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
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
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.weapon.GunItem;
import io.github.soundgoodizerfan.feedback.weapon.GunMechanism;

/**
 * An electric gun ({@link ElectricGunSpec}): a coil, rail or plasma barrel fed from a battery carried in the item. It is a
 * {@link GunItem} like a firearm, so it is fired, aimed, moded and drawn like one; {@link ElectricMechanism} is what differs. The
 * battery is loaded and taken out in the inventory the way a bundle takes items (click the gun with a battery on the cursor to load
 * it, click it with an empty cursor to take it out) and keeps its own state, so it goes back to a Charger as the item it is. A shot
 * takes its energy from the cell (a weak cell gives a weaker shot, never a refusal) and one metal nugget a few shots (the kind the
 * barrel can drive: a coil needs a magnetic metal, a rail or plasma barrel any conductor), and the slug is a bullet of the gun
 * pipeline ({@link io.github.soundgoodizerfan.feedback.weapon.GunShots#fire}).
 *
 * <p>Mistakes wear by steps and never from uptime: a cell over the receiver's voltage band shocks the wielder and wears the receiver,
 * a current over the barrel's rating wears it, far over fuses the conductor and wrecks the gun, and firing wet arcs to the wielder. A
 * shot warms the gun, and a warm gun recovers more slowly.
 */
public class ModularElectricGun extends GunItem {

    static final String HEAT = "EgunHeat", HEAT_AT = "EgunHeatAt", CHAMBERED = "EgunSlugs", METAL = "EgunSlugMetal";

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
        ItemStack cell = cell(stack);
        if (cell.isEmpty())
            tooltip.add(Component.translatable("tooltip.feedback.electric_gun.no_cell").withStyle(ChatFormatting.GRAY));
        else {
            CompoundTag state = BatteryStack.state(cell);
            tooltip.add(Component.translatable("tooltip.feedback.electric_gun.cell",
                    Math.round(BatteryStack.stateOfCharge(state) * 100f)).withStyle(ChatFormatting.GRAY));
        }
        int left = slugs(stack);
        if (left > 0)
            tooltip.add(Component.translatable("tooltip.feedback.electric_gun.chambered", left).withStyle(ChatFormatting.GRAY));
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

    static void setCell(ItemStack gun, ItemStack cell) {
        if (cell.isEmpty())
            gun.remove(DataComponents.CONTAINER);
        else
            gun.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(cell.copyWithCount(1))));
    }

    /** A battery on the cursor is loaded into the gun in the slot (or swapped for the one in it); an empty cursor takes the cell out. */
    @Override
    public boolean overrideOtherStackedOnMe(ItemStack gun, ItemStack other, Slot slot, ClickAction action, Player player, SlotAccess access) {
        if (action != ClickAction.SECONDARY || !slot.allowModification(player))
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

    /** Whether the receiver's type takes a battery this big; says so if it does not. */
    private static boolean fits(ItemStack gun, ItemStack battery, Player player) {
        ElectricGunSpec spec = spec(gun).orElse(null);
        CompoundTag state = BatteryStack.state(battery);
        if (spec == null || state == null || Math.max(1, state.getInt("Cells")) <= spec.maxCells())
            return true;
        player.displayClientMessage(Component.translatable("message.feedback.electric_gun.too_big", spec.maxCells()), true);
        return false;
    }

    /** The gun on the cursor takes a battery from the slot it is clicked on, or puts its cell into an empty slot. */
    @Override
    public boolean overrideStackedOnOther(ItemStack gun, Slot slot, ClickAction action, Player player) {
        if (action != ClickAction.SECONDARY || !slot.allowModification(player))
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

    // --- state on the stack ---------------------------------------------------------------------

    static int slugs(ItemStack gun) {
        CustomData data = gun.get(DataComponents.CUSTOM_DATA);
        return data == null ? 0 : data.copyTag().getInt(CHAMBERED);
    }

    static void chamber(ItemStack gun, int count, ResourceLocation metal) {
        CustomData.update(DataComponents.CUSTOM_DATA, gun, tag -> {
            tag.putInt(CHAMBERED, count);
            tag.putString(METAL, metal.toString());
        });
    }

    /** The gun's heat, 0 to 1, now: what the last shots left, cooled by the time since (a lazy clock; nothing ticks). */
    static float heat(ItemStack gun, long now, float shed) {
        CustomData data = gun.get(DataComponents.CUSTOM_DATA);
        if (data == null)
            return 0f;
        CompoundTag tag = data.copyTag();
        float h = tag.getFloat(HEAT);
        float seconds = Math.max(0L, now - tag.getLong(HEAT_AT)) / 20f;
        return Math.max(0f, h - seconds * ModularTuning.EGUN_HEAT_COOLING_PER_S * shed);
    }

    static void setHeat(ItemStack gun, float heat, long now) {
        CustomData.update(DataComponents.CUSTOM_DATA, gun, tag -> {
            tag.putFloat(HEAT, Mth.clamp(heat, 0f, 1f));
            tag.putLong(HEAT_AT, now);
        });
    }

    /** The gun's heat now, 0 to 1 (what a HUD reads). */
    public static float heatFraction(ItemStack gun, long now) {
        return spec(gun).map(sp -> heat(gun, now, sp.shed())).orElse(0f);
    }

    /** The share of the loaded cell's charge, 0 to 1, or -1 with no cell. */
    public static float chargeFraction(ItemStack gun) {
        ItemStack cell = cell(gun);
        return cell.isEmpty() ? -1f : BatteryStack.stateOfCharge(BatteryStack.state(cell));
    }

    /** Slugs chambered, and nuggets in the player's inventory the barrel could drive. */
    public static int slugsLeft(ItemStack gun) {
        return slugs(gun);
    }

    static Optional<ElectricGunSpec> spec(ItemStack gun) {
        ModularDesign design = ModularStacks.design(gun);
        ModularType type = design == null ? null : ModularTypes.known(design.type());
        return type == null ? Optional.empty() : ElectricGunSpec.of(design, type, MaterialTable.get().materials());
    }

    // --- what a shot needs ---------------------------------------------------------------------

    /** A metal nugget the barrel can drive: magnetic for a coil, any conductor for a rail or plasma barrel. */
    static Optional<Map.Entry<ResourceLocation, ItemStack>> nugget(Player player, ElectricGunSpec spec, Map<ResourceLocation, Material> materials) {
        for (ItemStack s : player.getInventory().items) {
            if (s.isEmpty())
                continue;
            Optional<Material.Located> found = Material.find(materials, s);
            if (found.isEmpty() || !found.get().form().equals("nugget") || found.get().material().kind().orElse(null) != Material.Kind.METAL
                    || found.get().material().density().muPerMb() <= 0)
                continue;
            Material m = found.get().material();
            boolean drives = spec.kind() == ElectricGunSpec.Kind.COIL ? m.separation().magnetic() : m.ampacity().isPresent();
            if (drives)
                return Optional.of(Map.entry(found.get().id(), s));
        }
        return Optional.empty();
    }

    /** What a mistake costs, by steps: nothing from correct use. */
    static void mistakes(ServerPlayer player, ItemStack gun, ElectricGunSpec spec, float volts, InteractionHand hand) {
        net.minecraft.world.entity.EquipmentSlot slot = LivingEntity.getSlotForHand(hand);
        // A cell over the receiver's band shocks the wielder and wears the receiver.
        float excess = ElectricRules.overVoltage(volts, spec.band());
        if (excess > 0f) {
            player.hurt(player.damageSources().lightningBolt(), ModularTuning.EGUN_SHOCK_PER_EXCESS * excess);
            gun.hurtAndBreak(ModularStacks.cap(gun, 1 + (int) excess), player, slot);
        }
        // Wet, the arc finds the wielder.
        if (player.isInWaterOrRain())
            player.hurt(player.damageSources().lightningBolt(), ModularTuning.EGUN_SHOCK);
        // A current over the barrel's rating wears it; far over, the conductor fuses.
        float load = spec.load(volts);
        if (load > ModularTuning.EGUN_FUSE_RATIO) {
            gun.hurtAndBreak(ModularStacks.cap(gun, Math.max(1, gun.getMaxDamage() / 4)), player, slot);
            player.hurt(player.damageSources().lightningBolt(), ModularTuning.EGUN_SHOCK);
            player.level().playSound(null, player.blockPosition(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.9f, 1.2f);
        } else if (load > 1f) {
            gun.hurtAndBreak(ModularStacks.cap(gun, 1 + (int) ((load - 1f) * 3f)), player, slot);
        }
    }
}
