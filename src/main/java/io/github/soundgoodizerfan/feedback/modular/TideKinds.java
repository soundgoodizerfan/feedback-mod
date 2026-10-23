/*
 * Feedback -- a Minecraft technology mod.
 * Copyright (C) 2026 soundgoodizerfan
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Assets under src/main/resources/assets are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.modular;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * The runtime kinds of the Tide effect ({@code docs/feedback_enchant_roster.md}): the three that read fluid drag.
 * {@code seal} (a helmet part: breath lasts), {@code stride} (a boot part: the wearer wades with less drag) and
 * {@code affinity} (a tool head in the main hand: digging under water is not slowed) are each a transient attribute
 * modifier, put on or taken off while the attuned part is where it counts. The thrust-against-drag side is not built.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class TideKinds {

    /** Extra breath (the Respiration attribute) per level of seal. [OPEN] until played */
    private static final double SEAL_PER_LEVEL = 0.5;
    /** Water movement efficiency per level of stride. [OPEN] until played */
    private static final double STRIDE_PER_LEVEL = 0.2;
    /** Submerged mining speed per level of affinity. [OPEN] until played */
    private static final double AFFINITY_PER_LEVEL = 0.3;

    private static final ResourceLocation SEAL = id("tide_seal");
    private static final ResourceLocation STRIDE = id("tide_stride");
    private static final ResourceLocation AFFINITY = id("tide_affinity");

    /** Extra damage per level of douse against a target that water hurts. [OPEN] until played */
    private static final float DOUSE_DAMAGE_PER_LEVEL = 3f;

    private TideKinds() {
    }

    static void register() {
        // Tide+ on a head: a blow puts the target out, and hurts what water hurts (blazes, endermen, snow golems).
        AttunementKinds.onStrike("douse", (event, a) -> {
            LivingEntity target = event.getEntity();
            if (target.isSensitiveToWater())
                event.setAmount(event.getAmount() + a.level() * DOUSE_DAMAGE_PER_LEVEL);
            target.clearFire();
        });
    }

    /** A doused tool used on the block beside a fire puts it out. */
    @SubscribeEvent
    public static void onDouseBlock(PlayerInteractEvent.RightClickBlock event) {
        BlockPos at = event.getPos().relative(event.getFace());
        if (level(event.getItemStack(), "douse") <= 0 || !event.getLevel().getBlockState(at).is(BlockTags.FIRE))
            return;
        if (!event.getLevel().isClientSide) {
            event.getLevel().removeBlock(at, false);
            event.getLevel().playSound(null, at, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.7f, 1f);
        }
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Feedback.MOD_ID, path);
    }

    /** The summed level of {@code kind} across the parts of {@code stack}. */
    static int level(ItemStack stack, String kind) {
        int n = 0;
        for (MaterialPerks.Attunement a : MaterialPerks.attunements(stack))
            if (a.kind().equals(kind))
                n += a.level();
        return n;
    }

    /** Put a transient modifier of {@code amount} on {@code attribute}, or take it off when {@code amount} is zero. */
    static void sync(Player player, Holder<Attribute> attribute, ResourceLocation id, double amount) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null)
            return;
        if (amount > 0)
            instance.addOrUpdateTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
        else if (instance.hasModifier(id))
            instance.removeModifier(id);
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || player.tickCount % 10 != 0)
            return;
        sync(player, Attributes.OXYGEN_BONUS, SEAL, level(player.getItemBySlot(EquipmentSlot.HEAD), "seal") * SEAL_PER_LEVEL);
        sync(player, Attributes.WATER_MOVEMENT_EFFICIENCY, STRIDE, level(player.getItemBySlot(EquipmentSlot.FEET), "stride") * STRIDE_PER_LEVEL);
        sync(player, Attributes.SUBMERGED_MINING_SPEED, AFFINITY, level(player.getMainHandItem(), "affinity") * AFFINITY_PER_LEVEL);
        if (player.isOnFire() && level(player.getMainHandItem(), "douse") > 0)
            player.clearFire();
    }
}
