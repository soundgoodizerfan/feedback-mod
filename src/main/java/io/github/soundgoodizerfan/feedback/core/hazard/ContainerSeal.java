// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core.hazard;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.part.ModularBackpack;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

import java.util.HashSet;
import java.util.Set;

/**
 * A toxic fluid in a vessel that is not sealed leaks into whatever holds the vessel. A stack that carries a fluid (a bucket, a
 * flask, a cylinder) leaks the {@code toxic} perk of that fluid's material ({@link Fumes#level}) unless it is in the item tag
 * {@code feedback:sealed_fluid_containers} (a corked or pressure-tight vessel). Held inside another container (a pack, a shulker
 * box, a bundle, a chest or barrel) it lets a {@link FumeCloud} go at that container: the pack's wearer is in it, a chest's
 * neighbours are. Carried loose in the inventory it is not inside a container and does nothing here; a sealed vessel is safe
 * anywhere. The rule is generic: no container is named, it reads the item's stored contents and the fluid's material.
 * <p>
 * Looked at every {@link HazardTuning#CONTAINER_LEAK_PERIOD_TICKS} for each player's packs and bags and for the containers in
 * the chunks around each player (those are the only places anyone could be harmed).
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class ContainerSeal {

    /** Vessels that hold their contents in: nothing leaks from them, whatever they carry. */
    public static final TagKey<Item> SEALED = TagKey.create(Registries.ITEM, Feedback.id("sealed_fluid_containers"));
    private static final int DEPTH = 4;

    private ContainerSeal() {
    }

    /** Leak strength of one stack on its own: the toxic perk of the worst fluid it carries, 0 when sealed or harmless. */
    public static int leak(ItemStack stack) {
        if (stack.isEmpty() || stack.is(SEALED))
            return 0;
        IFluidHandlerItem handler = stack.getCapability(Capabilities.FluidHandler.ITEM);
        if (handler == null)
            return 0;
        int worst = 0;
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            FluidStack fluid = handler.getFluidInTank(tank);
            if (fluid.isEmpty())
                continue;
            Material m = MaterialTable.get().materials().get(BuiltInRegistries.FLUID.getKey(fluid.getFluid()));
            if (m != null)
                worst = Math.max(worst, Fumes.level(m));
        }
        return worst;
    }

    /** Leak strength from anything inside {@code holder}, to any depth: its own stored stacks, and what their stored stacks hold. */
    public static int inside(ItemStack holder) {
        return inside(holder, DEPTH);
    }

    private static int inside(ItemStack holder, int depth) {
        if (depth <= 0)
            return 0;
        int worst = 0;
        for (ItemStack in : holder.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).nonEmptyItems())
            worst = Math.max(worst, Math.max(leak(in), inside(in, depth - 1)));
        for (ItemStack in : holder.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY).items())
            worst = Math.max(worst, Math.max(leak(in), inside(in, depth - 1)));
        return worst;
    }

    /** Leak strength from a block's inventory: every stack in it, and what those stacks hold. */
    public static int inside(Container container) {
        int worst = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack in = container.getItem(i);
            worst = Math.max(worst, Math.max(leak(in), inside(in, DEPTH)));
        }
        return worst;
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.getGameTime() % HazardTuning.CONTAINER_LEAK_PERIOD_TICKS != 0)
            return;
        Set<Long> seen = new HashSet<>();
        for (ServerPlayer player : level.players()) {
            if (player.isCreative() || player.isSpectator())
                continue;
            int worst = inside(ModularBackpack.wornStack(player));
            for (ItemStack carried : player.getInventory().items)
                worst = Math.max(worst, inside(carried));
            for (ItemStack carried : player.getInventory().offhand)
                worst = Math.max(worst, inside(carried));
            FumeCloud.release(level, player.position().add(0, player.getBbHeight() / 2, 0), worst);
            ChunkPos centre = player.chunkPosition();
            int r = HazardTuning.CONTAINER_LEAK_RADIUS_CHUNKS;
            for (int dx = -r; dx <= r; dx++)
                for (int dz = -r; dz <= r; dz++) {
                    ChunkPos pos = new ChunkPos(centre.x + dx, centre.z + dz);
                    if (seen.add(pos.toLong()))
                        sweep(level, pos);
                }
        }
    }

    private static void sweep(ServerLevel level, ChunkPos pos) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x, pos.z);
        if (chunk == null)
            return;
        for (BlockEntity be : chunk.getBlockEntities().values())
            if (be instanceof Container container)
                FumeCloud.release(level, Vec3.atCenterOf(be.getBlockPos()), inside(container));
    }
}
