// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.item;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import io.github.soundgoodizerfan.feedback.weapon.GunCondition;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularTooltip;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularTuning;
import io.github.soundgoodizerfan.feedback.modular.bake.SpringGunSpec;
import io.github.soundgoodizerfan.feedback.modular.bake.SpringSet;
import io.github.soundgoodizerfan.feedback.modular.combat.RangedDispatch;
import io.github.soundgoodizerfan.feedback.modular.type.Modular;
import io.github.soundgoodizerfan.feedback.modular.type.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.type.ModularType;
import io.github.soundgoodizerfan.feedback.modular.type.ModularTypes;

/**
 * A spring-piston air gun ({@link SpringGunSpec}). Hold use to cock it by hand: the time is the spring's stored energy over the
 * wielder's strength, and a spring stronger than its receiver holds takes a set each cocking, or far past it snaps
 * ({@link SpringSet}). A cocked gun stays cocked until fired; use again to fire. The pellets are cut from a metal nugget of any
 * kind in the inventory, as many to a nugget as the bore leaves room for, and the pellet's mass is its metal's density. It flies
 * as an arrow does (drag, gravity) and hits with the kinetic energy it was given. The release is nearly silent.
 */
public class ModularSpringGun extends Item implements Modular {

    private static final String COCKED = "AirCocked", PELLETS = "AirPellets", METAL = "AirPelletMetal";

    public ModularSpringGun(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return ModularStacks.name(stack, super.getName(stack));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ModularTooltip.add(stack, tooltip);
        if (cocked(stack))
            tooltip.add(Component.translatable("tooltip.feedback.spring_gun.cocked").withStyle(ChatFormatting.GRAY));
        GunCondition.wearLine(stack).ifPresent(tooltip::add);
        int left = pellets(stack);
        if (left > 0)
            tooltip.add(Component.translatable("tooltip.feedback.spring_gun.chambered", left).withStyle(ChatFormatting.GRAY));
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

    // --- state on the stack ---------------------------------------------------------------------

    public static boolean cocked(ItemStack gun) {
        CustomData data = gun.get(DataComponents.CUSTOM_DATA);
        return data != null && data.copyTag().getBoolean(COCKED);
    }

    private static void cock(ItemStack gun, boolean on) {
        CustomData.update(DataComponents.CUSTOM_DATA, gun, tag -> tag.putBoolean(COCKED, on));
    }

    /** Pellets cut and waiting in the chamber from the last nugget. */
    private static int pellets(ItemStack gun) {
        CustomData data = gun.get(DataComponents.CUSTOM_DATA);
        return data == null ? 0 : data.copyTag().getInt(PELLETS);
    }

    private static void chamber(ItemStack gun, int count, ResourceLocation metal) {
        CustomData.update(DataComponents.CUSTOM_DATA, gun, tag -> {
            tag.putInt(PELLETS, count);
            tag.putString(METAL, metal.toString());
        });
    }

    private static Optional<SpringGunSpec> spec(ItemStack gun) {
        ModularDesign design = ModularStacks.design(gun);
        ModularType type = design == null ? null : ModularTypes.known(design.type());
        return type == null ? Optional.empty() : SpringGunSpec.of(design, type, MaterialTable.get().materials());
    }

    // --- cocking --------------------------------------------------------------------------------

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack gun = player.getItemInHand(hand);
        if (ModularStacks.broken(gun) || player.getCooldowns().isOnCooldown(this))
            return InteractionResultHolder.fail(gun);
        if (cocked(gun)) {
            if (!level.isClientSide && player instanceof ServerPlayer server)
                fire(server, gun);
            return InteractionResultHolder.success(gun);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(gun);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.CROSSBOW;
    }

    /** Ticks this wielder takes to cock the gun: the baked time over their strength. */
    private static float cockTicks(ItemStack gun, LivingEntity wielder) {
        float pull = Mth.clamp((float) wielder.getAttributeValue(Attributes.ATTACK_DAMAGE), ModularTuning.AIR_PULL_MIN, ModularTuning.AIR_PULL_MAX);
        return ModularStacks.drawTicks(gun) * (1f - SpringSet.set(gun)) / pull;
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        if (!level.isClientSide && getUseDuration(stack, user) - remaining == Math.round(cockTicks(stack, user)))
            level.playSound(null, user.blockPosition(), SoundEvents.CROSSBOW_LOADING_MIDDLE.value(), SoundSource.PLAYERS, AIR_VOLUME, 1.4f);
    }

    private static final float AIR_VOLUME = ModularTuning.AIR_VOLUME;

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int timeLeft) {
        if (level.isClientSide || cocked(stack) || ModularStacks.broken(stack))
            return;
        if (getUseDuration(stack, user) - timeLeft < Math.round(cockTicks(stack, user)))
            return;
        SpringGunSpec spec = spec(stack).orElse(null);
        if (spec == null)
            return;
        SpringSet.Outcome outcome = SpringSet.overload(stack, spec.load(SpringSet.set(stack)), user);
        float ratio = spec.load(SpringSet.set(stack));
        if (outcome == SpringSet.Outcome.SET)
            GunCondition.misuse(stack, ModularTuning.GUN_WEAR_SPRING_SET + ModularTuning.GUN_WEAR_SPRING_SET_SPAN * Math.min(1f, (ratio - 1f) / 0.6f));
        if (outcome == SpringSet.Outcome.SNAP) {
            GunCondition.misuse(stack, ModularTuning.GUN_WEAR_SPRING_SNAP);
            level.playSound(null, user.blockPosition(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.9f, 1.2f);
            return;
        }
        cock(stack, true);
        level.playSound(null, user.blockPosition(), SoundEvents.CROSSBOW_LOADING_END.value(), SoundSource.PLAYERS, AIR_VOLUME,
                outcome == SpringSet.Outcome.SET ? 0.7f : 1.3f);
    }

    // --- firing ---------------------------------------------------------------------------------

    /** Any metal nugget the player carries: its metal and the stack it is in. */
    private static Optional<Map.Entry<ResourceLocation, ItemStack>> nugget(Player player, Map<ResourceLocation, Material> materials) {
        for (ItemStack s : player.getInventory().items) {
            if (s.isEmpty())
                continue;
            Optional<Material.Located> found = Material.find(materials, s);
            if (found.isPresent() && found.get().form().equals("nugget") && found.get().material().kind().orElse(null) == Material.Kind.METAL
                    && found.get().material().density().muPerMb() > 0)
                return Optional.of(Map.entry(found.get().id(), s));
        }
        return Optional.empty();
    }

    private static void fire(ServerPlayer player, ItemStack gun) {
        Level level = player.level();
        SpringGunSpec spec = spec(gun).orElse(null);
        if (spec == null)
            return;
        Map<ResourceLocation, Material> materials = MaterialTable.get().materials();
        ResourceLocation metal = null;
        if (pellets(gun) > 0) {
            CustomData data = gun.get(DataComponents.CUSTOM_DATA);
            metal = data == null ? null : ResourceLocation.tryParse(data.copyTag().getString(METAL));
        }
        if (metal == null || !materials.containsKey(metal)) {
            Optional<Map.Entry<ResourceLocation, ItemStack>> nugget = nugget(player, materials);
            if (nugget.isEmpty()) {
                GunCondition.misuse(gun, ModularTuning.GUN_WEAR_DRY_FIRE); // released with nothing loaded
                level.playSound(null, player.blockPosition(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, AIR_VOLUME, 1.6f);
                player.getCooldowns().addCooldown(gun.getItem(), ModularTuning.AIR_SHOT_COOLDOWN);
                return;
            }
            metal = nugget.get().getKey();
            if (!player.getAbilities().instabuild)
                nugget.get().getValue().shrink(1);
            chamber(gun, spec.perNugget(), metal);
        }
        Material m = materials.get(metal);
        SpringGunSpec.Shot shot = spec.shot(m.density().muPerMb(), SpringSet.set(gun));
        // A worn bore loses energy and sprays wider, by the same curve as a powder gun's (GunFigures.aged).
        float condition = GunCondition.condition(gun);
        float energyShare = 1f - io.github.soundgoodizerfan.feedback.weapon.GunTuning.GUN_WEAR_ENERGY_LOSS * (1f - condition)
                / (1f - io.github.soundgoodizerfan.feedback.weapon.GunTuning.GUN_CONDITION_FLOOR);
        float speed = RangedDispatch.launch(gun, shot.speed() * (float) Math.sqrt(energyShare));
        Arrow pellet = new Arrow(level, player, m.item(metal, "nugget").map(ItemStack::new).orElse(new ItemStack(net.minecraft.world.item.Items.IRON_NUGGET)), gun.copy());
        pellet.pickup = AbstractArrow.Pickup.DISALLOWED;
        pellet.setBaseDamage(shot.damage() * energyShare / Math.max(0.1f, shot.speed() * (float) Math.sqrt(energyShare)));
        pellet.setCritArrow(false);
        float spread = ModularTuning.AIR_SPREAD * ModularTuning.AIR_LENGTH_HALF / Math.max(0.05f, spec.lengthM());
        pellet.shootFromRotation(player, player.getXRot(), player.getYRot(), 0f, speed, RangedDispatch.spread(gun, spread / condition));
        RangedDispatch.arrow(pellet, gun);
        level.addFreshEntity(pellet);
        level.playSound(null, player.blockPosition(), SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, AIR_VOLUME, 1.8f);
        int left = pellets(gun) - 1;
        chamber(gun, Math.max(0, left), metal);
        cock(gun, false);
        player.getCooldowns().addCooldown(gun.getItem(), ModularTuning.AIR_SHOT_COOLDOWN);
    }
}
