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
 */
package io.github.soundgoodizerfan.feedback.modular.attunement;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.MaterialPerks;
import static io.github.soundgoodizerfan.feedback.modular.AttunementKinds.onStrike;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * The runtime kinds of the Pack effect. {@code pack}: a weapon part hits
 * harder for each ally near; {@code pack_guard}: a worn part takes less from a hit for each ally near. {@code lone}
 * and {@code lone_guard} are the mirror: the same, but only with no ally near. Counted at the moment of the blow, so
 * nothing runs per tick.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class PackKinds {

    /** Blocks around the holder an ally counts from. [OPEN] until played. */
    private static final double RADIUS = 6.0;
    /** Allies counted at most. [OPEN] until played. */
    private static final int ALLY_CAP = 5;
    /** Pack: extra damage share per ally per level, and the most that can be. [OPEN] until played. */
    private static final float PACK_DAMAGE_PER_ALLY = 0.05f, PACK_DAMAGE_CAP = 0.75f;
    /** Lone: extra damage share per level with no ally near, and the most that can be. [OPEN] until played. */
    private static final float LONE_DAMAGE_PER_LEVEL = 0.2f, LONE_DAMAGE_CAP = 0.6f;
    /** Pack guard: share of a hit turned away per ally per level, and the most that can be. [OPEN] until played. */
    private static final float GUARD_PER_ALLY = 0.04f, GUARD_CAP = 0.5f;
    /** Lone guard: share of a hit turned away per level with no ally near, and the most that can be. [OPEN] until played. */
    private static final float LONE_GUARD_PER_LEVEL = 0.1f, LONE_GUARD_CAP = 0.5f;

    /** Tag of mob types that stand with anyone (an iron golem). A pack adds entries; the code never names an id. */
    private static final TagKey<EntityType<?>> ALLIES = TagKey.create(Registries.ENTITY_TYPE, Feedback.id("pack_allies"));

    private PackKinds() {
    }

    public static void register() {
        onStrike("pack", (event, a) -> {
            int n = allies(source(event), event.getEntity());
            if (n > 0)
                event.setAmount(event.getAmount() * (1f + Math.min(PACK_DAMAGE_CAP, n * PACK_DAMAGE_PER_ALLY * a.level())));
        });
        onStrike("lone", (event, a) -> {
            if (allies(source(event), event.getEntity()) == 0)
                event.setAmount(event.getAmount() * (1f + Math.min(LONE_DAMAGE_CAP, LONE_DAMAGE_PER_LEVEL * a.level())));
        });
    }

    private static LivingEntity source(LivingIncomingDamageEvent event) {
        return (LivingEntity) event.getSource().getEntity();
    }

    /**
     * Allies of {@code self} within reach, not counting {@code except} (the one being fought). An ally is on the same
     * scoreboard team, tamed by the same owner (vanilla's {@code isAlliedTo} covers both), or of a type in the
     * {@code pack_allies} tag. FTB Teams (soft, optional) should later feed this through a compat hook so its party
     * members count; never a hard dependency and never an id check.
     */
    static int allies(LivingEntity self, Entity except) {
        int n = self.level().getEntitiesOfClass(LivingEntity.class, self.getBoundingBox().inflate(RADIUS),
                o -> o != self && o != except && o.isAlive()
                        && (self.isAlliedTo(o) || o.isAlliedTo(self) || o.getType().is(ALLIES))).size();
        return Math.min(ALLY_CAP, n);
    }

    /** Worn pack and lone parts act on the hit the wearer takes. */
    @SubscribeEvent
    public static void onGuard(LivingIncomingDamageEvent event) {
        LivingEntity wearer = event.getEntity();
        if (wearer.level().isClientSide)
            return;
        int pack = 0, lone = 0;
        for (ItemStack worn : wearer.getArmorSlots())
            for (MaterialPerks.Attunement a : MaterialPerks.attunements(worn)) {
                if (a.kind().equals("pack_guard"))
                    pack += a.level();
                else if (a.kind().equals("lone_guard"))
                    lone += a.level();
            }
        if (pack == 0 && lone == 0)
            return;
        int n = allies(wearer, event.getSource().getEntity());
        float cut = n > 0 ? Math.min(GUARD_CAP, n * GUARD_PER_ALLY * pack)
                : Math.min(LONE_GUARD_CAP, LONE_GUARD_PER_LEVEL * lone);
        if (cut > 0f)
            event.setAmount(event.getAmount() * (1f - cut));
    }
}
