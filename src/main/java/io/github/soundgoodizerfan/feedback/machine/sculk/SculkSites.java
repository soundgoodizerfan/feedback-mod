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
package io.github.soundgoodizerfan.feedback.machine.sculk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.xp.XpNetwork;
import io.github.soundgoodizerfan.feedback.core.xp.XpNetworks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

/**
 * Per-level lists of Catalyst Taps and Sculk Relays, and the one event that feeds the taps. Runtime only: each block
 * adds itself when it loads and leaves when it is removed or its chunk unloads. A death scans the taps of its own
 * level, so the cost is one pass per death and nothing at rest.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class SculkSites {

    private static final class Sites {
        final Set<BlockPos> taps = new HashSet<>();
        final Set<BlockPos> relays = new HashSet<>();
    }

    private static final Map<LevelAccessor, Sites> PER_LEVEL = new HashMap<>();

    private SculkSites() {
    }

    private static Sites sites(Level level) {
        return PER_LEVEL.computeIfAbsent(level, l -> new Sites());
    }

    public static void addTap(Level level, BlockPos pos) {
        sites(level).taps.add(pos.immutable());
    }

    public static void addRelay(Level level, BlockPos pos) {
        sites(level).relays.add(pos.immutable());
    }

    static void forget(Level level, BlockEntity be) {
        Sites s = PER_LEVEL.get(level);
        if (s != null) {
            s.taps.remove(be.getBlockPos());
            s.relays.remove(be.getBlockPos());
        }
    }

    /** Loaded relays other than {@code self} within the relay range of {@code self}. */
    public static List<SculkRelayBlockEntity> relaysNear(Level level, BlockPos self) {
        Sites s = PER_LEVEL.get(level);
        if (s == null || s.relays.size() < 2)
            return List.of();
        double r2 = FTuning.RELAY_RANGE * FTuning.RELAY_RANGE;
        List<SculkRelayBlockEntity> out = new ArrayList<>(2);
        for (BlockPos p : s.relays)
            if (!p.equals(self) && p.distSqr(self) <= r2 && level.isLoaded(p)
                    && level.getBlockEntity(p) instanceof SculkRelayBlockEntity relay && !relay.isRemoved())
                out.add(relay);
        return out;
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        PER_LEVEL.remove(event.getLevel());
    }

    /** A death near a tap puts the XP it would have dropped into the tap's run; what the run cannot hold drops as usual. */
    @SubscribeEvent
    public static void onExperience(LivingExperienceDropEvent event) {
        LivingEntity dead = event.getEntity();
        Level level = dead.level();
        if (level.isClientSide || event.getDroppedExperience() <= 0 || dead instanceof Player)
            return;
        Sites s = PER_LEVEL.get(level);
        if (s == null || s.taps.isEmpty())
            return;
        double r2 = FTuning.TAP_RADIUS * FTuning.TAP_RADIUS;
        int left = event.getDroppedExperience();
        for (BlockPos p : s.taps) {
            if (left <= 0)
                break;
            if (p.distToCenterSqr(dead.position()) > r2 || !level.isLoaded(p))
                continue;
            if (level.getBlockEntity(p) instanceof CatalystTapBlockEntity tap && tap.network() != null)
                left -= tap.network().inject(left);
        }
        event.setDroppedExperience(left);
    }
}
