// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.attunement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.machine.Headed;
import io.github.soundgoodizerfan.feedback.machine.Weldable;
import io.github.soundgoodizerfan.feedback.modular.bake.MaterialPerks;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FModular;
import io.github.soundgoodizerfan.feedback.vehicle.engine.EngineBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * The one throttled tick for placed machines with attuned parts: every {@link #PERIOD} ticks, the machines in the chunks near a player are read through
 * the design they keep ({@link MachineKinds#levels}, cached per block entity) and the kinds that act on the world around a machine run. A machine
 * far from every player is not read; nothing here costs the machine anything for running (wear is only from misuse).
 * <ul>
 * <li>{@code lure}/{@code scare} (bait, mount): animals nearby walk toward or away from the machine.</li>
 * <li>{@code grow}/{@code reap} (bloom, working face): crops beside the machine take a bone-meal step or are harvested, every {@link #WORK_EVERY} periods.</li>
 * <li>{@code ring}/{@code hush} (ring, working face): the machine rings a vibration sculk and wandering hostiles notice; a hushed one silences game events near it ({@link #hushed}).</li>
 * <li>{@code mend}: a motor, generator, battery, engine or seated head closes its wear, one experience point per mend from the nearest player.</li>
 * </ul>
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class MachineAura {

    /** Ticks between reads; periods between crop work and rings. Placeholder. */
    private static final int PERIOD = 40, WORK_EVERY = 5, RING_EVERY = 3;
    /** Chunks around a player that are read. */
    private static final int CHUNKS = 2;
    /** Blocks an animal is drawn or driven from per level, and the most; blocks a hush reaches; blocks a ring is heard per level. */
    private static final double LURE_PER_LEVEL = 3, LURE_MAX = 12, HUSH_REACH = 8, RING_PER_LEVEL = 6;
    /** Crop radius cap. */
    private static final int CROP_MAX = 2;

    private static final Map<BlockEntity, Map<String, Integer>> LEVELS = new WeakHashMap<>();
    /** Where a hushing machine stood at the last read, by level and position, with the tick it was read. */
    private static final List<Object[]> HUSH = new ArrayList<>();

    private MachineAura() {
    }

    private static Map<String, Integer> levelsOf(BlockEntity be) {
        return LEVELS.computeIfAbsent(be, b -> {
            if (!BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(b.getType()).getNamespace().equals(Feedback.MOD_ID))
                return Map.of();
            var design = b.collectComponents().get(FModular.DESIGN.get());
            return design == null ? Map.of() : MachineKinds.levels(design);
        });
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % PERIOD != 0)
            return;
        for (ServerLevel level : event.getServer().getAllLevels()) {
            long now = level.getGameTime();
            long period = now / PERIOD;
            HUSH.removeIf(h -> h[0] == level && (long) h[2] < now - PERIOD * 2);
            Map<BlockPos, BlockEntity> seen = new HashMap<>();
            for (ServerPlayer p : level.players()) {
                ChunkPos c = p.chunkPosition();
                for (int dx = -CHUNKS; dx <= CHUNKS; dx++)
                    for (int dz = -CHUNKS; dz <= CHUNKS; dz++) {
                        LevelChunk chunk = level.getChunkSource().getChunkNow(c.x + dx, c.z + dz);
                        if (chunk != null)
                            chunk.getBlockEntities().forEach(seen::putIfAbsent);
                    }
            }
            for (BlockEntity be : seen.values()) {
                Map<String, Integer> levels = levelsOf(be);
                if (!levels.isEmpty())
                    act(level, be, levels, period);
            }
        }
    }

    private static void act(ServerLevel level, BlockEntity be, Map<String, Integer> levels, long period) {
        BlockPos pos = be.getBlockPos();
        int lure = levels.getOrDefault("lure", 0), scare = levels.getOrDefault("scare", 0);
        if (lure + scare > 0)
            draw(level, pos, lure, scare);
        int grow = levels.getOrDefault("grow", 0), reap = levels.getOrDefault("reap", 0);
        if (grow + reap > 0 && period % WORK_EVERY == 0)
            crops(level, pos, grow, reap);
        int ring = levels.getOrDefault("ring", 0);
        if (ring > 0 && period % RING_EVERY == 0) {
            level.gameEvent(null, GameEvent.EXPLODE, Vec3.atCenterOf(pos));
            for (Mob mob : level.getEntitiesOfClass(Mob.class, new net.minecraft.world.phys.AABB(pos).inflate(RING_PER_LEVEL * ring),
                    m -> m instanceof Enemy && m.getTarget() == null))
                mob.getNavigation().moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 1.0);
        }
        if (levels.getOrDefault("hush", 0) > 0)
            HUSH.add(new Object[] { level, pos, level.getGameTime() });
        int mend = levels.getOrDefault("mend", 0);
        if (mend > 0)
            mend(level, be, mend);
        if (be instanceof Headed headed && !headed.getHead().isEmpty())
            mendHead(level, pos, headed.getHead().get());
    }

    private static void draw(ServerLevel level, BlockPos pos, int lure, int scare) {
        boolean scares = scare > lure;
        double r = Math.min(LURE_MAX, LURE_PER_LEVEL * Math.max(lure, scare));
        Vec3 at = Vec3.atCenterOf(pos);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, new net.minecraft.world.phys.AABB(pos).inflate(r),
                m -> m instanceof Animal || (scares && m instanceof Enemy && m.getMaxHealth() <= 20f))) {
            if (mob instanceof TamableAnimal pet && pet.isTame())
                continue;
            if (!scares) {
                if (mob.distanceToSqr(at) > 4)
                    mob.getNavigation().moveTo(at.x, at.y, at.z, 1.0);
            } else if (mob instanceof PathfinderMob pm) {
                Vec3 away = DefaultRandomPos.getPosAway(pm, 8, 4, at);
                if (away != null)
                    mob.getNavigation().moveTo(away.x, away.y, away.z, 1.3);
            }
        }
    }

    private static void crops(ServerLevel level, BlockPos pos, int grow, int reap) {
        int r = Math.min(CROP_MAX, grow + reap);
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-r, -1, -r), pos.offset(r, 1, r))) {
            BlockState state = level.getBlockState(p);
            if (grow > 0 && state.getBlock() instanceof BonemealableBlock b
                    && b.isValidBonemealTarget(level, p, state) && b.isBonemealSuccess(level, level.random, p, state))
                b.performBonemeal(level, level.random, p, state);
            else if (reap > 0 && state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)) {
                Block.dropResources(state, level, p);
                level.setBlock(p, crop.getStateForAge(0), Block.UPDATE_ALL);
            }
        }
    }

    private static void mend(ServerLevel level, BlockEntity be, int mend) {
        float by = mend * MachineKinds.mendPerLevel();
        if (be instanceof Weldable w) {
            if (w.getCondition() < w.maxCondition() && MachineKinds.payMend(level, be.getBlockPos()))
                w.weld(Math.min(w.maxCondition(), w.getCondition() + by), w.maxCondition());
        } else if (be instanceof io.github.soundgoodizerfan.feedback.machine.Figured f) {
            var store = f.figureStore();
            if (store.get("Condition", 1f) < 1f && MachineKinds.payMend(level, be.getBlockPos()))
                store.put("Condition", Math.min(1f, store.get("Condition", 1f) + by));
        } else if (be instanceof io.github.soundgoodizerfan.feedback.machine.shaft.ShaftBlockEntity s && s.getCondition() < 1f && MachineKinds.payMend(level, be.getBlockPos()))
            s.mend(by);
        else if (be instanceof EngineBlockEntity e && e.condition() < 1f && MachineKinds.payMend(level, be.getBlockPos()))
            e.mend(by);
    }

    /** A seated head with mend closes its own wear, as a held tool does. */
    private static void mendHead(ServerLevel level, BlockPos pos, ItemStack head) {
        int n = 0;
        for (MaterialPerks.Attunement a : MaterialPerks.attunements(head))
            if (a.kind().equals("mend"))
                n += a.level();
        float c = head.getOrDefault(FDataComponents.TOOL_CONDITION.get(), 1f);
        if (n > 0 && c < 1f && MachineKinds.payMend(level, pos))
            head.set(FDataComponents.TOOL_CONDITION.get(), Mth.clamp(c + n * MachineKinds.mendPerLevel(), 0f, 1f));
    }

    /** Whether a hushing machine stood within reach of {@code at} at its last read. */
    public static boolean hushed(net.minecraft.world.level.Level level, Vec3 at) {
        for (Object[] h : HUSH)
            if (h[0] == level && Vec3.atCenterOf((BlockPos) h[1]).distanceToSqr(at) <= HUSH_REACH * HUSH_REACH)
                return true;
        return false;
    }
}
