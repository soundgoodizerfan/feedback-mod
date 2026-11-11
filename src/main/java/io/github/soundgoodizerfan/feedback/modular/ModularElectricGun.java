// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import io.github.soundgoodizerfan.feedback.core.electric.ElectricRules;
import io.github.soundgoodizerfan.feedback.machine.battery.BatteryStack;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

/**
 * An electric gun ({@link ElectricGunSpec}): a coil, rail or plasma barrel fed from a battery carried in the item. The battery is loaded
 * and taken out in the inventory the way a bundle takes items (click the gun with a battery on the cursor to load it, click it with an
 * empty cursor to take it out) and keeps its own state, so it goes back to a Charger as the item it is. Use fires: it takes the shot's
 * energy from the cell (a weak cell gives a weaker shot, never a refusal), one metal nugget a shot (the kind the barrel can drive: a coil
 * needs a magnetic metal, a rail or plasma barrel any conductor), and the shot is a slug that flies as an arrow does or, for plasma, an
 * instant short ray.
 *
 * <p>Mistakes wear by steps and never from uptime: a cell over the receiver's voltage band shocks the wielder and wears the receiver,
 * a current over the barrel's rating wears it, far over fuses the conductor and wrecks the gun, and firing wet arcs to the wielder. A
 * shot warms the gun, and a warm gun recovers more slowly.
 */
public class ModularElectricGun extends Item implements Modular {

    private static final String HEAT = "EgunHeat", HEAT_AT = "EgunHeatAt", CHAMBERED = "EgunSlugs", METAL = "EgunSlugMetal";

    public ModularElectricGun(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return ModularStacks.name(stack, super.getName(stack));
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

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        ModularStacks.tick(stack, level);
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return ModularStacks.enchantability(stack, 1);
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
        return false;
    }

    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @Nullable T entity, Consumer<Item> onBroken) {
        return ModularStacks.cap(stack, amount);
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

    private static void setCell(ItemStack gun, ItemStack cell) {
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

    private static int slugs(ItemStack gun) {
        CustomData data = gun.get(DataComponents.CUSTOM_DATA);
        return data == null ? 0 : data.copyTag().getInt(CHAMBERED);
    }

    private static void chamber(ItemStack gun, int count, ResourceLocation metal) {
        CustomData.update(DataComponents.CUSTOM_DATA, gun, tag -> {
            tag.putInt(CHAMBERED, count);
            tag.putString(METAL, metal.toString());
        });
    }

    /** The gun's heat, 0 to 1, now: what the last shots left, cooled by the time since (a lazy clock; nothing ticks). */
    private static float heat(ItemStack gun, long now, float shed) {
        CustomData data = gun.get(DataComponents.CUSTOM_DATA);
        if (data == null)
            return 0f;
        CompoundTag tag = data.copyTag();
        float h = tag.getFloat(HEAT);
        float seconds = Math.max(0L, now - tag.getLong(HEAT_AT)) / 20f;
        return Math.max(0f, h - seconds * ModularTuning.EGUN_HEAT_COOLING_PER_S * shed);
    }

    private static void setHeat(ItemStack gun, float heat, long now) {
        CustomData.update(DataComponents.CUSTOM_DATA, gun, tag -> {
            tag.putFloat(HEAT, Mth.clamp(heat, 0f, 1f));
            tag.putLong(HEAT_AT, now);
        });
    }

    private static Optional<ElectricGunSpec> spec(ItemStack gun) {
        ModularDesign design = ModularStacks.design(gun);
        ModularType type = design == null ? null : ModularTypes.known().get(design.type());
        return type == null ? Optional.empty() : ElectricGunSpec.of(design, type, MaterialTable.get().materials());
    }

    // --- firing ---------------------------------------------------------------------------------

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack gun = player.getItemInHand(hand);
        if (ModularStacks.broken(gun) || player.getCooldowns().isOnCooldown(this))
            return InteractionResultHolder.fail(gun);
        if (!level.isClientSide && player instanceof ServerPlayer server)
            fire(server, gun, hand);
        return InteractionResultHolder.success(gun);
    }

    /** A metal nugget the barrel can drive: magnetic for a coil, any conductor for a rail or plasma barrel. */
    private static Optional<Map.Entry<ResourceLocation, ItemStack>> nugget(Player player, ElectricGunSpec spec, Map<ResourceLocation, Material> materials) {
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

    private static void dry(Level level, ServerPlayer player, ItemStack gun) {
        level.playSound(null, player.blockPosition(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, ModularTuning.EGUN_VOLUME, 1.6f);
        player.getCooldowns().addCooldown(gun.getItem(), ModularTuning.EGUN_COOLDOWN);
    }

    private static void fire(ServerPlayer player, ItemStack gun, InteractionHand hand) {
        ServerLevel level = player.serverLevel();
        ElectricGunSpec spec = spec(gun).orElse(null);
        ItemStack cell = cell(gun);
        if (spec == null || cell.isEmpty()) {
            dry(level, player, gun);
            return;
        }
        CompoundTag state = BatteryStack.state(cell).copy();
        float volts = BatteryStack.terminalVolts(state);
        if (!BatteryStack.ready(state) || BatteryStack.charge(state) <= 0f || volts <= 0f) {
            dry(level, player, gun);
            return;
        }
        Map<ResourceLocation, Material> materials = MaterialTable.get().materials();
        // The slug: what is chambered from the last nugget, else a fresh nugget of a metal the barrel can drive.
        ResourceLocation metal = null;
        if (slugs(gun) > 0) {
            CustomData data = gun.get(DataComponents.CUSTOM_DATA);
            metal = data == null ? null : ResourceLocation.tryParse(data.copyTag().getString(METAL));
        }
        if (metal == null || !materials.containsKey(metal)) {
            Optional<Map.Entry<ResourceLocation, ItemStack>> nugget = nugget(player, spec, materials);
            if (nugget.isEmpty()) {
                dry(level, player, gun);
                return;
            }
            metal = nugget.get().getKey();
            if (!player.getAbilities().instabuild)
                nugget.get().getValue().shrink(1);
            chamber(gun, spec.perNugget(), metal);
        }
        Material m = materials.get(metal);
        float density = m.density().muPerMb();
        // The draw: the energy a shot takes over the volts; a weak cell gives the share it has.
        float needed = spec.energyIn() / (volts * ModularTuning.EGUN_J_PER_VAMPTICK);
        float took = BatteryStack.draw(state, needed);
        float fraction = needed <= 0f ? 0f : took / needed;
        cell.set(FDataComponents.BATTERY.get(), state);
        setCell(gun, cell);
        mistakes(player, gun, spec, volts, hand);
        ElectricGunSpec.Shot shot = spec.shot(density, fraction);
        Vec3 muzzle = player.getEyePosition().add(player.getLookAngle().scale(0.8));
        if (spec.kind() == ElectricGunSpec.Kind.PLASMA)
            plasma(level, player, shot, muzzle);
        else {
            // An eddy sleeve drains a conductor slug's energy after it leaves the barrel, so it leaves slower.
            float speed = RangedDispatch.launch(gun, shot.speed()) * (1f - Math.min(0.9f, ModularTuning.EGUN_EDDY_BRAKE * spec.eddy()));
            Arrow slug = new Arrow(level, player, m.item(metal, "nugget").map(ItemStack::new).orElse(new ItemStack(net.minecraft.world.item.Items.IRON_NUGGET)), gun.copy());
            slug.pickup = AbstractArrow.Pickup.DISALLOWED;
            slug.setBaseDamage(shot.damage() / Math.max(0.1f, shot.speed()));
            slug.setCritArrow(false);
            float spread = ModularTuning.AIR_SPREAD * ModularTuning.EGUN_LENGTH_HALF / Math.max(0.05f, spec.lengthM()) * spec.spreadMult();
            // A lit reticle helps where it is dark.
            if (spec.darkAt() > 0f && level.getMaxLocalRawBrightness(player.blockPosition()) <= spec.darkAt())
                spread *= 1f - spec.darkCut();
            slug.shootFromRotation(player, player.getXRot(), player.getYRot(), 0f, speed, spread);
            RangedDispatch.arrow(slug, gun);
            level.addFreshEntity(slug);
            if (slugs(gun) > 0)
                chamber(gun, slugs(gun) - 1, metal);
        }
        // The arc and the report: a crack from a rail, a thump from a coil, a hiss from plasma.
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, muzzle.x, muzzle.y, muzzle.z, 6, 0.08, 0.08, 0.08, 0.05);
        level.playSound(null, player.blockPosition(), switch (spec.kind()) {
            case RAIL -> SoundEvents.FIREWORK_ROCKET_BLAST;
            case COIL -> SoundEvents.PISTON_EXTEND;
            case PLASMA -> SoundEvents.BLAZE_SHOOT;
        }, SoundSource.PLAYERS, ModularTuning.EGUN_VOLUME * spec.report(), spec.kind() == ElectricGunSpec.Kind.RAIL ? 1.6f : 1.1f);
        // Heat and the recovery it slows.
        long now = level.getGameTime();
        float heat = heat(gun, now, spec.shed())
                + ModularTuning.EGUN_HEAT_PER_SHOT * (1f - spec.efficiency()) * spec.heatGain() * spec.heatMult();
        setHeat(gun, heat, now);
        player.getCooldowns().addCooldown(gun.getItem(), Math.round(ModularTuning.EGUN_COOLDOWN + heat * ModularTuning.EGUN_HEAT_COOLDOWN_TICKS));
    }

    /** What a mistake costs, by steps: nothing from correct use. */
    private static void mistakes(ServerPlayer player, ItemStack gun, ElectricGunSpec spec, float volts, InteractionHand hand) {
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

    /** The plasma ray: an instant, short bolt that falls off with distance and sets what it hits alight. */
    private static void plasma(ServerLevel level, ServerPlayer player, ElectricGunSpec.Shot shot, Vec3 muzzle) {
        Vec3 eye = player.getEyePosition(), dir = player.getLookAngle();
        double range = ModularTuning.EGUN_PLASMA_RANGE;
        Vec3 end = eye.add(dir.scale(range));
        BlockHitResult block = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        double reach = block.getType() == HitResult.Type.MISS ? range : block.getLocation().distanceTo(eye);
        Vec3 stop = eye.add(dir.scale(reach));
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(level, player, eye, stop, new AABB(eye, stop).inflate(1.0),
                e -> !e.isSpectator() && e.isPickable());
        if (hit != null) {
            reach = hit.getLocation().distanceTo(eye);
            float damage = (float) (shot.damage() * Math.max(0.0, 1.0 - reach / range));
            if (damage > 0f) {
                hit.getEntity().hurt(level.damageSources().playerAttack(player), damage);
                hit.getEntity().igniteForSeconds(ModularTuning.EGUN_PLASMA_BURN_TICKS / 20);
            }
            level.sendParticles(ParticleTypes.FLAME, hit.getLocation().x, hit.getLocation().y, hit.getLocation().z, 8, 0.1, 0.1, 0.1, 0.02);
        }
        // The arc's trail, a spark each half block.
        for (double d = 0.8; d < reach; d += 0.5) {
            Vec3 at = eye.add(dir.scale(d));
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }
}
