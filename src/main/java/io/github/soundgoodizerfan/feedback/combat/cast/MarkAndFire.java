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
package io.github.soundgoodizerfan.feedback.combat.cast;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.feel.CombatCues;
import io.github.soundgoodizerfan.feedback.combat.feel.FeelTuning;
import io.github.soundgoodizerfan.feedback.registry.FSounds;
import io.github.soundgoodizerfan.feedback.weapon.GunAmmo;
import io.github.soundgoodizerfan.feedback.weapon.GunItem;
import io.github.soundgoodizerfan.feedback.weapon.GunShots;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.ahead;
import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.later;

/**
 * Mark and Fire (node {@code needs_trait: fan}, so a hammer-and-cylinder gun): the first press starts laying a volley.
 * Hold the sights on things and each beat marks one more of the rounds chambered (six in a revolver); a thing can take
 * several. Filling the cylinder, or pressing again, lets the hammer go: one exact shot at every mark, two ticks apart.
 * The player walks slowly while laying it and has six seconds. Each round is a real round: ammo, heat and cook-off as ever.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
final class MarkAndFire {

    private MarkAndFire() {
    }

    /** One volley being laid: the entity ids marked so far, the most that can be, when it began and when a mark last landed. */
    private record Marking(List<Integer> ids, int max, long start, long lastAdd) {
    }

    private static final Map<UUID, Marking> MARKS = new HashMap<>();
    private static final net.minecraft.resources.ResourceLocation SLOW = Feedback.id("mark_slow");
    /** Ticks between marks while the sights stay on a thing, and the longest a volley may be laid. */
    private static final int BEAT = 4, LIMIT = 120;

    static boolean active(ServerPlayer p) {
        return MARKS.containsKey(p.getUUID());
    }

    static boolean act(ServerPlayer p) {
        if (active(p)) {
            execute(p);
            return true;
        }
        ItemStack gun = p.getMainHandItem();
        int rounds = gun.getItem() instanceof GunItem ? GunAmmo.count(gun) : 0;
        if (rounds < 1)
            return false;
        long now = p.level().getGameTime();
        MARKS.put(p.getUUID(), new Marking(new ArrayList<>(), rounds, now, now));
        var speed = p.getAttribute(Attributes.MOVEMENT_SPEED);
        speed.removeModifier(SLOW);
        speed.addTransientModifier(new AttributeModifier(SLOW, -0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        CombatCues.everyone(p, FSounds.FEEL_LIGHT.get(), FeelTuning.LIGHT_VOLUME, 0.5f);
        CastFx.sound(p.serverLevel(), p.getEyePosition(), SoundEvents.NOTE_BLOCK_HAT.value(), 0.8f, 1.4f);
        return true;
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !active(p))
            return;
        Marking m = MARKS.get(p.getUUID());
        long now = p.level().getGameTime();
        ItemStack gun = p.getMainHandItem();
        if (!p.isAlive() || !(gun.getItem() instanceof GunItem) || GunAmmo.count(gun) < 1) {
            end(p);
            return;
        }
        ServerLevel level = p.serverLevel();
        // The marks are shown to the shooter alone: a glint over each marked thing, one more per round laid on it.
        Map<Integer, Integer> stacked = new HashMap<>();
        for (int id : m.ids())
            stacked.merge(id, 1, Integer::sum);
        stacked.forEach((id, n) -> {
            if (level.getEntity(id) instanceof LivingEntity e)
                for (int i = 0; i < n; i++)
                    level.sendParticles(p, ParticleTypes.CRIT, true, e.getX(), e.getY() + e.getBbHeight() + 0.25 + 0.2 * i, e.getZ(), 1, 0.12, 0, 0.12, 0);
        });
        if (now - m.start() > LIMIT) {
            if (m.ids().isEmpty())
                end(p);
            else
                execute(p);
            return;
        }
        if (now - m.lastAdd() < BEAT || m.ids().size() >= m.max())
            return;
        LivingEntity t = ahead(p, 40, 0.985);
        if (t == null || level.clip(new ClipContext(p.getEyePosition(), t.getBoundingBox().getCenter(), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, p)).getType() != HitResult.Type.MISS)
            return;
        m.ids().add(t.getId());
        MARKS.put(p.getUUID(), new Marking(m.ids(), m.max(), m.start(), now));
        CastFx.sound(level, p.getEyePosition(), SoundEvents.NOTE_BLOCK_PLING.value(), 0.8f, 0.6f + 0.15f * m.ids().size());
        if (m.ids().size() >= m.max())
            execute(p);
    }

    private static void end(ServerPlayer p) {
        MARKS.remove(p.getUUID());
        p.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(SLOW);
    }

    /** The hammer lets go: turn to each mark in order and put an exact shot in it, two ticks apart. */
    private static void execute(ServerPlayer p) {
        Marking m = MARKS.get(p.getUUID());
        end(p);
        if (m == null)
            return;
        for (int i = 0; i < m.ids().size(); i++) {
            int id = m.ids().get(i);
            later(p, i * 2, () -> {
                ItemStack held = p.getMainHandItem();
                if (!p.isAlive() || !(held.getItem() instanceof GunItem) || GunAmmo.count(held) < 1
                        || !(p.serverLevel().getEntity(id) instanceof LivingEntity e) || !e.isAlive())
                    return;
                Vec3 d = e.getBoundingBox().getCenter().subtract(p.getEyePosition());
                float yaw = (float) (Mth.atan2(-d.x, d.z) * Mth.RAD_TO_DEG);
                float pitch = (float) (-Mth.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * Mth.RAD_TO_DEG);
                // Ask the client to ease the view round to it, so the volley sweeps the player round the room, one mark at a time.
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(p, new CastPayloads.Turn(yaw, pitch));
                DeadEye.arm(p, 2); // each of these is exact
                GunShots.triggerHeard(p.serverLevel(), p, held, yaw, pitch, System.currentTimeMillis(), false, InteractionHand.MAIN_HAND);
            });
        }
    }
}
