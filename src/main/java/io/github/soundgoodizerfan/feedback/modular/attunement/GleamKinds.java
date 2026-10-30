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

import io.github.soundgoodizerfan.feedback.Feedback;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * The runtime kinds of the Gleam effect (light). {@code glow} (a held
 * weapon part) and {@code worn_glow} (armour): a temporary vanilla {@code light} block sits at the holder's head and is
 * moved as they move. It is taken away when the part comes off or leaves the hand, on logout, death, dimension change
 * and server stop. {@code dim} / {@code worn_dim} (the mirror): hostile mobs past a short range do not take up the
 * holder as a target while the holder stands in low light. Nothing wears.
 *
 * <p>shortcut: a crash or a killed server leaves the one light block of each glowing player in the world; upgrade with a
 * saved list swept on load. Mobs already hunting the holder keep doing so (only new targets are refused).
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class GleamKinds {

    /** Light level of the block: base plus a step per level, to 15. [OPEN] until played */
    private static final int BASE_LIGHT = 5, LIGHT_PER_LEVEL = 2;
    /** Ticks between moving the light. [OPEN] until played */
    private static final int PERIOD = 5;
    /** Dim: blocks inside which a mob still notices the holder, and the raw light at or under which they are hidden. [OPEN] until played */
    private static final double DIM_NEAR = 6;
    private static final int DIM_LIGHT = 7;

    private record Spot(ResourceKey<Level> dim, BlockPos pos) {
    }

    private static final Map<UUID, Spot> SPOTS = new HashMap<>();

    private GleamKinds() {
    }

    public static void register() {
    }

    private static int glow(Player p) {
        return TideKinds.level(p.getMainHandItem(), "glow") + BaitKinds.worn(p, "worn_glow");
    }

    private static int dim(LivingEntity p) {
        return TideKinds.level(p.getMainHandItem(), "dim") + BaitKinds.worn(p, "worn_dim");
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % PERIOD != 0)
            return;
        Spot old = SPOTS.get(p.getUUID());
        int lvl = glow(p);
        BlockPos at = p.blockPosition().above();
        if (lvl > 0 && old != null && old.dim.equals(p.level().dimension()) && old.pos.equals(at)
                && p.level().getBlockState(at).is(Blocks.LIGHT))
            return;
        if (old != null)
            remove(p.server, p.getUUID());
        if (lvl == 0)
            return;
        ServerLevel level = p.serverLevel();
        BlockState there = level.getBlockState(at);
        boolean water = there.getFluidState().isSourceOfType(Fluids.WATER) && there.is(Blocks.WATER);
        if (!there.isAir() && !water)
            return;
        level.setBlock(at, Blocks.LIGHT.defaultBlockState()
                .setValue(LightBlock.LEVEL, Math.min(15, BASE_LIGHT + LIGHT_PER_LEVEL * lvl))
                .setValue(LightBlock.WATERLOGGED, water), 3);
        SPOTS.put(p.getUUID(), new Spot(level.dimension(), at));
    }

    /** Takes the player's light block out, if the block there is still a light block. */
    private static void remove(MinecraftServer server, UUID id) {
        Spot s = SPOTS.remove(id);
        ServerLevel level = s == null ? null : server.getLevel(s.dim);
        if (level == null || !level.isLoaded(s.pos) || !level.getBlockState(s.pos).is(Blocks.LIGHT))
            return;
        boolean water = level.getBlockState(s.pos).getValue(LightBlock.WATERLOGGED);
        level.setBlock(s.pos, water ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p)
            remove(p.server, p.getUUID());
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer p)
            remove(p.server, p.getUUID());
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p)
            remove(p.server, p.getUUID());
    }

    @SubscribeEvent
    public static void onStop(ServerStoppingEvent event) {
        for (UUID id : java.util.List.copyOf(SPOTS.keySet()))
            remove(event.getServer(), id);
    }

    /** Dim: a hostile mob farther than {@link #DIM_NEAR} does not pick up a holder standing in the dark. */
    @SubscribeEvent
    public static void onRetarget(LivingChangeTargetEvent event) {
        LivingEntity next = event.getNewAboutToBeSetTarget();
        LivingEntity mob = event.getEntity();
        if (next == null || mob.level().isClientSide || mob.distanceToSqr(next) < DIM_NEAR * DIM_NEAR
                || next.level().getMaxLocalRawBrightness(next.blockPosition()) > DIM_LIGHT)
            return;
        if (dim(next) > 0)
            event.setCanceled(true);
    }
}
