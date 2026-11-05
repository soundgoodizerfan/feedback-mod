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
 * Assets under src/main/resources are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.potion;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.core.thermal.Heat;
import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
import io.github.soundgoodizerfan.feedback.core.thermal.ThermalBody;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.core.unit.TuRate;
import io.github.soundgoodizerfan.feedback.registry.FEffects;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Quench: a cool damp breeze round the drinker. Thermal block entities within a few blocks leak to the room faster, and the hot workpieces in
 * the drinker's pack cool faster. Nothing sets a temperature: each pass adds extra of the same leak {@link Heat#tick} already applies (the Work
 * goes to the room, as the ordinary leak does), so a fire keeps heating and a body never drops under ambient. The trade is cold: frost builds
 * faster in powder snow while it lasts.
 * <p>
 * Cost: one pass per drinker every {@link PotionTuning#QUENCH_EVERY} ticks, over the (at most nine) loaded chunks the radius touches, looking
 * only at block entities that are {@link ThermalBody}s above ambient; plus the 41 inventory slots, skipping any without a heat stamp. Nothing
 * runs for a player without the effect.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class Quench {

    private Quench() {}

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        Player p = event.getEntity();
        if (!(p.level() instanceof ServerLevel level) || !p.hasEffect(FEffects.QUENCH))
            return;
        if (p.isInPowderSnow && p.canFreeze() && p.tickCount % PotionTuning.QUENCH_COLD_BUILD_EVERY == 0)
            p.setTicksFrozen(Math.min(p.getTicksRequiredToFreeze() + 20, p.getTicksFrozen() + 1));
        if (p.tickCount % PotionTuning.QUENCH_EVERY != 0)
            return;
        float ambient = AmbientTable.at(level).value();
        forBodies(level, p.position(), PotionTuning.QUENCH_RADIUS, body -> cool(body, ambient));
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty() || !ItemHeat.isHot(s, level))
                continue;
            TuRate rate = s.getItem() instanceof ItemHeat.Cooling c ? c.coolingRate(s) : FTuning.ITEM_COOLING_TU_PER_TICK;
            ItemHeat.set(s, Heat.cooled(ItemHeat.get(s, level), PotionTuning.QUENCH_EVERY * (PotionTuning.QUENCH_COOL_MULT - 1f), rate, AmbientTable.at(level)), level);
        }
    }

    /** Every loaded thermal block entity within {@code r} blocks of {@code at}. */
    private static void forBodies(ServerLevel level, net.minecraft.world.phys.Vec3 at, float r, java.util.function.Consumer<ThermalBody> each) {
        double r2 = r * r;
        for (int cx = (int) Math.floor((at.x - r) / 16); cx <= (int) Math.floor((at.x + r) / 16); cx++)
            for (int cz = (int) Math.floor((at.z - r) / 16); cz <= (int) Math.floor((at.z + r) / 16); cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null)
                    continue;
                for (BlockEntity be : chunk.getBlockEntities().values())
                    if (be instanceof ThermalBody body && be.getBlockPos().distToCenterSqr(at) <= r2)
                        each.accept(body);
            }
    }

    /**
     * A thrown flask: where it lands, thermal block entities within {@link PotionTuning#QUENCH_RADIUS} fall toward ambient (each level keeps
     * {@link PotionTuning#QUENCH_DOUSE_KEEP} of the gap again), hot dropped items cool for {@link PotionTuning#QUENCH_DOUSE_TICKS} ticks per
     * level, and fire blocks in reach go out. Bodies are put out by the caller.
     */
    public static void douse(ServerLevel level, net.minecraft.world.phys.Vec3 at, int amplifier) {
        float ambient = AmbientTable.at(level).value();
        float keep = (float) Math.pow(PotionTuning.QUENCH_DOUSE_KEEP, amplifier + 1);
        forBodies(level, at, PotionTuning.QUENCH_RADIUS, body -> {
            float gap = body.getTemperature().value() - ambient;
            if (gap > 1f)
                body.setTemperature(new Tu(ambient + gap * keep));
        });
        int r = (int) PotionTuning.QUENCH_RADIUS;
        for (var item : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(at, at).inflate(r))) {
            ItemStack s = item.getItem();
            if (s.isEmpty() || !ItemHeat.isHot(s, level))
                continue;
            TuRate rate = s.getItem() instanceof ItemHeat.Cooling c ? c.coolingRate(s) : FTuning.ITEM_COOLING_TU_PER_TICK;
            ItemHeat.set(s, Heat.cooled(ItemHeat.get(s, level), PotionTuning.QUENCH_DOUSE_TICKS * (amplifier + 1), rate, AmbientTable.at(level)), level);
        }
        net.minecraft.core.BlockPos c = net.minecraft.core.BlockPos.containing(at);
        for (net.minecraft.core.BlockPos p : net.minecraft.core.BlockPos.betweenClosed(c.offset(-r, -r, -r), c.offset(r, r, r)))
            if (level.getBlockState(p).getBlock() instanceof net.minecraft.world.level.block.BaseFireBlock)
                level.removeBlock(p, false);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD, at.x, at.y + 0.3, at.z, 24, 1.5, 0.5, 1.5, 0.02);
    }

    /** The extra leak over one pass: the gap to ambient shrinks by (mult-1) x leak / mass per tick, never more than half in a pass. */
    private static void cool(ThermalBody body, float ambient) {
        float gap = body.getTemperature().value() - ambient;
        if (gap <= 1f)
            return;
        float k = (PotionTuning.QUENCH_COOL_MULT - 1f) * body.getLeak().workPerTickPerTu()
                / Math.max(1f, body.getThermalMass().workPerTu()) * PotionTuning.QUENCH_EVERY;
        body.setTemperature(new Tu(ambient + gap * (1f - Math.min(0.5f, k))));
    }
}
