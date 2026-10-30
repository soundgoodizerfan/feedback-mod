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
package io.github.soundgoodizerfan.feedback.modular.attunement;

import io.github.soundgoodizerfan.feedback.modular.AttunementKinds;
import io.github.soundgoodizerfan.feedback.modular.MaterialPerks;
import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
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
 * The runtime kinds of the Tide effect: the three that read fluid drag.
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

    /** Blocks of lava a level of walker cools under the feet, beyond the first two. [OPEN] until played */
    private static final int WALKER_RADIUS_BASE = 2;
    /** Ticks cooled lava holds as obsidian before it runs again. [OPEN] until played */
    private static final long WALKER_TICKS = 100;

    /** Obsidian a walker made, by level: when it runs again. shortcut: lost on restart, the rock stays; upgrade with a saved attachment. */
    private static final Map<ResourceKey<Level>, Map<BlockPos, Long>> CRUST = new HashMap<>();

    private TideKinds() {
    }

    public static void register() {
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

    /** Walker on boots: lava source under the wearer's feet cools to obsidian, which runs again soon after. */
    @SubscribeEvent
    public static void onWalk(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        if (!(player.level() instanceof ServerLevel level) || player.tickCount % 2 != 0 || boots.isEmpty() || player.isShiftKeyDown())
            return;
        int walker = level(boots, "walker");
        if (walker <= 0)
            return;
        int r = WALKER_RADIUS_BASE + walker;
        Map<BlockPos, Long> crust = CRUST.computeIfAbsent(level.dimension(), k -> new HashMap<>());
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int dx = -r; dx <= r; dx++)
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > r * r)
                    continue;
                at.set(player.getBlockX() + dx, player.getBlockY() - 1, player.getBlockZ() + dz);
                BlockState state = level.getBlockState(at);
                if (crust.containsKey(at)) {
                    if (state.is(Blocks.OBSIDIAN))
                        crust.put(at.immutable(), level.getGameTime() + WALKER_TICKS);
                } else if (state.getBlock() instanceof LiquidBlock && state.getFluidState().is(FluidTags.LAVA)
                        && state.getFluidState().isSource() && level.getBlockState(at.above()).isAir()) {
                    level.setBlockAndUpdate(at, Blocks.OBSIDIAN.defaultBlockState());
                    crust.put(at.immutable(), level.getGameTime() + WALKER_TICKS);
                }
            }
    }

    @SubscribeEvent
    public static void onCrust(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.getGameTime() % 10 != 0)
            return;
        Map<BlockPos, Long> crust = CRUST.get(level.dimension());
        if (crust == null)
            return;
        for (Iterator<Map.Entry<BlockPos, Long>> it = crust.entrySet().iterator(); it.hasNext();) {
            Map.Entry<BlockPos, Long> e = it.next();
            if (level.getGameTime() < e.getValue())
                continue;
            if (level.getBlockState(e.getKey()).is(Blocks.OBSIDIAN))
                level.setBlockAndUpdate(e.getKey(), Blocks.LAVA.defaultBlockState());
            it.remove();
        }
    }
}
