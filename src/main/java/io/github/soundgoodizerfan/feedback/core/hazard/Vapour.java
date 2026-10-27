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
package io.github.soundgoodizerfan.feedback.core.hazard;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.thermal.FluidHeat;
import io.github.soundgoodizerfan.feedback.geology.CrudeGrade;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.process.PhaseChangeTable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Flammable vapour (petrochemistry spec 4.2): an unsealed holder of liquid hotter than its flash point
 * (open vessel, seep, pool) adds to a per-block float in the air above it. Not fluid dynamics: every
 * step each cell hands {@link FTuning#VAPOUR_SPREAD} of itself evenly to the air blocks around it, then
 * decays and is capped. Any ignition source ({@link HotIgnition#isSource}) in or beside a cell, or a
 * burning source, or last step's flame next door, burns it: a flash fire that walks the cloud one cell
 * a step, hurts what stands in it by concentration, then flashes back to the sources it touched.
 * Sealed and cold is safe: nothing here is called for a closed tank.
 * <p>
 * shortcut: held in memory, not saved (a reload clears clouds and fires on pools, which then re-register
 * from their own ticks); a source is a block position, so two holders at one position share an entry;
 * upgrade if clouds must survive unloads or drift with wind.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class Vapour {

    /** A block that is a source and has something to lose when it burns out (a pool vanishes, a seep runs dry). */
    public interface BurnsOut {
        void burnedOut(ServerLevel level, BlockPos pos);
    }

    private static final class Source {
        float rate;
        long until;
        int burnTicks;
        long burningUntil;
        boolean block;
    }

    private static final class Field {
        final Map<Long, Float> cloud = new HashMap<>();
        final Map<Long, Source> sources = new HashMap<>();
        Set<Long> flame = new HashSet<>();
    }

    private static final Map<ServerLevel, Field> FIELDS = new WeakHashMap<>();

    private Vapour() {
    }

    // --- what makes a source ----------------------------------------------------------------

    private static Optional<Material.Combustion> combustion(FluidStack stack) {
        Material m = MaterialTable.get().materials().get(BuiltInRegistries.FLUID.getKey(stack.getFluid()));
        return m == null ? Optional.empty() : m.combustion();
    }

    /**
     * How hard {@code stack} gives off vapour at its own temperature: 0 when its material has no flash
     * point or it is below it; for a crude, scaled by its light fraction (it is the lightest cut that
     * flashes). Cheap enough to call every tick of a holder.
     */
    public static float potency(FluidStack stack) {
        if (stack.isEmpty())
            return 0f;
        Optional<Material.Combustion> c = combustion(stack);
        if (c.isEmpty() || c.get().flashPoint().isEmpty() || FluidHeat.of(stack).value() < c.get().flashPoint().get().value())
            return 0f;
        return PhaseChangeTable.get().boiling(stack).filter(p -> p.lightShare().isPresent()).isPresent()
                ? Math.min(1f, 2f * CrudeGrade.of(stack).lightFraction()) : 1f;
    }

    /** Ticks a pool of {@code stack} burns, from its material (0 if it does not burn). */
    public static int burnTicks(FluidStack stack) {
        return combustion(stack).flatMap(Material.Combustion::burnTicks).orElse(0);
    }

    /**
     * Registers or refreshes a source at {@code pos} (emitting into the cell above): {@code potency} from
     * {@link #potency}, kept alive {@code linger} ticks past this call (an open vessel passes a few and
     * calls every tick; a seep passes a long time and is refreshed by random ticks). {@code burnTicks}
     * above 0 means it burns down for that long once lit and then {@code block} is told. Server side.
     */
    public static void source(ServerLevel level, BlockPos pos, float potency, int linger, int burnTicks, boolean block) {
        if (potency <= 0f)
            return;
        Source s = field(level).sources.computeIfAbsent(pos.asLong(), k -> new Source());
        s.rate = potency;
        s.until = level.getGameTime() + linger;
        s.burnTicks = burnTicks;
        s.block = block;
    }

    /** Vapour concentration at {@code pos}, 0 to {@link FTuning#VAPOUR_CAP}. */
    public static float at(ServerLevel level, BlockPos pos) {
        Field f = FIELDS.get(level);
        return f == null ? 0f : f.cloud.getOrDefault(pos.asLong(), 0f);
    }

    /** Whether the source at {@code pos} is burning. */
    public static boolean burning(ServerLevel level, BlockPos pos) {
        Field f = FIELDS.get(level);
        Source s = f == null ? null : f.sources.get(pos.asLong());
        return s != null && s.burningUntil > level.getGameTime();
    }

    private static Field field(ServerLevel level) {
        return FIELDS.computeIfAbsent(level, l -> new Field());
    }

    // --- the step ---------------------------------------------------------------------------

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.getGameTime() % FTuning.VAPOUR_INTERVAL_TICKS != 0)
            return;
        Field f = FIELDS.get(level);
        if (f == null || (f.cloud.isEmpty() && f.sources.isEmpty()))
            return;
        long now = level.getGameTime();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();

        // sources emit, burn down, or expire
        for (Iterator<Map.Entry<Long, Source>> it = f.sources.entrySet().iterator(); it.hasNext();) {
            var e = it.next();
            Source s = e.getValue();
            BlockPos pos = BlockPos.of(e.getKey());
            if (!level.isLoaded(pos))
                continue;
            boolean burning = s.burningUntil > now;
            if (s.burningUntil != 0 && !burning) {
                it.remove();
                if (level.getBlockState(pos).getBlock() instanceof BurnsOut b)
                    b.burnedOut(level, pos);
                continue;
            }
            if ((s.block && !(level.getBlockState(pos).getBlock() instanceof BurnsOut)) || (!burning && s.until < now)) {
                it.remove();
                continue;
            }
            BlockPos cell = pos.above();
            if (burning) {
                if (level.getBlockState(cell).isAir() && BaseFireBlock.canBePlacedAt(level, cell, Direction.UP))
                    level.setBlockAndUpdate(cell, BaseFireBlock.getState(level, cell));
            } else if (level.getBlockState(cell).isAir()) {
                f.cloud.merge(cell.asLong(), s.rate * FTuning.VAPOUR_EMIT, (a, b) -> Math.min(FTuning.VAPOUR_CAP, a + b));
            }
        }

        // flash: seeds are cells beside an ignition source, a burning source, or last step's flame
        Set<Long> seeds = new HashSet<>();
        for (Map.Entry<Long, Float> e : f.cloud.entrySet()) {
            BlockPos c = BlockPos.of(e.getKey());
            if (touched(level, f, c, p, now))
                seeds.add(e.getKey());
        }
        for (long c : seeds) {
            BlockPos cell = BlockPos.of(c);
            float v = f.cloud.remove(c);
            level.sendParticles(ParticleTypes.FLAME, cell.getX() + 0.5, cell.getY() + 0.5, cell.getZ() + 0.5, 3, 0.25, 0.25, 0.25, 0.01);
            for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, new AABB(cell))) {
                victim.igniteForSeconds(FTuning.VAPOUR_FLASH_FIRE_SECONDS);
                victim.hurt(Hazards.source(level, Hazards.HOT_CONTACT), FTuning.VAPOUR_FLASH_DAMAGE * v / FTuning.VAPOUR_CAP);
            }
            if (level.getBlockState(cell).isAir() && BaseFireBlock.canBePlacedAt(level, cell, Direction.UP))
                level.setBlockAndUpdate(cell, BaseFireBlock.getState(level, cell));
        }
        // flashback: a source beside a flame cell catches (a pool then burns for its material's burn_ticks)
        for (Map.Entry<Long, Source> e : f.sources.entrySet()) {
            BlockPos cell = BlockPos.of(e.getKey()).above();
            Source s = e.getValue();
            if (s.burnTicks > 0 && s.burningUntil <= now && (seeds.contains(cell.asLong()) || f.flame.contains(cell.asLong())))
                s.burningUntil = now + s.burnTicks;
        }
        f.flame = seeds;

        // spread, decay, cap
        Map<Long, Float> next = new HashMap<>();
        float share = FTuning.VAPOUR_SPREAD / 6f;
        for (Map.Entry<Long, Float> e : f.cloud.entrySet()) {
            float v = e.getValue();
            BlockPos c = BlockPos.of(e.getKey());
            float kept = v;
            for (Direction d : Direction.values()) {
                p.setWithOffset(c, d);
                if (level.isLoaded(p) && level.getBlockState(p).isAir()) {
                    next.merge(p.asLong(), v * share, Float::sum);
                    kept -= v * share;
                }
            }
            next.merge(e.getKey(), kept, Float::sum);
        }
        next.replaceAll((k, v) -> Math.min(FTuning.VAPOUR_CAP, v * FTuning.VAPOUR_DECAY));
        next.values().removeIf(v -> v < FTuning.VAPOUR_FLOOR);
        f.cloud.clear();
        f.cloud.putAll(next);
    }

    private static boolean touched(ServerLevel level, Field f, BlockPos c, BlockPos.MutableBlockPos p, long now) {
        if (HotIgnition.isSource(level.getBlockState(c)))
            return true;
        for (Direction d : Direction.values()) {
            p.setWithOffset(c, d);
            if (!level.isLoaded(p))
                continue;
            if (f.flame.contains(p.asLong()) || HotIgnition.isSource(level.getBlockState(p)))
                return true;
            // a burning source sits one block below its own cell
            if (d == Direction.DOWN) {
                Source s = f.sources.get(p.asLong());
                if (s != null && s.burningUntil > now)
                    return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    public static void onUnload(net.neoforged.neoforge.event.level.LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level)
            FIELDS.remove(level);
    }
}
