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
import io.github.soundgoodizerfan.feedback.modular.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.StrikeSource;
import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.hazard.Exposure;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityStruckByLightningEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * The runtime kinds of the Conduct effect ({@code docs/feedback_enchant_roster.md}): Channeling. {@code draw} on a
 * conductive tool head, held under open sky in a thunderstorm, calls a strike on the holder at a chance scaled by the
 * head's electrical conductivity; a grip or handle that insulates ({@link Exposure#conduction}, the hazard rule every
 * shock uses) makes the bolt harmless to the holder. {@code shed} on worn plate cuts lightning's damage, and enough of
 * it turns the strike aside altogether. The same head also lands a bolt on whatever its blow hits, and on the ground a
 * slam strikes, every time in a storm ({@link #call}).
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class ConductKinds {

    /** Chance per second, per level and per unit of conductivity (copper 1), of a strike on the holder. [OPEN] until played */
    private static final float DRAW_CHANCE = 0.02f;
    /** Share of lightning damage each level of shed removes. [OPEN] until played */
    private static final float SHED_PER_LEVEL = 0.25f;
    /** Summed shed level at which a strike is turned aside altogether, fire and all. [OPEN] until played */
    private static final int SHED_TURNS_ASIDE = 3;
    /** Conductivity taken for a conducting material whose file gives no figure. [OPEN] until played */
    private static final float DRAW_UNMEASURED = 0.3f;

    private ConductKinds() {
    }

    /** Tag on a bolt a wielder called down: the UUID of a wielder whose insulated grip spares them it. */
    private static final String SPARED = "feedback_spared";

    public static void register() {
        AttunementKinds.onStrike("draw", (event, a) -> {
            if (event.getSource().getEntity() instanceof Player wielder)
                call(wielder, StrikeSource.weapon(wielder, event.getSource()), event.getEntity().position());
        });
    }

    /**
     * A drawing head's blow in a thunderstorm under open sky always lands a bolt at {@code at}: the victim of a hit, the
     * point struck by a slam. An insulated grip spares the wielder the bolt they called, not anyone else near it.
     */
    public static void call(Player wielder, ItemStack weapon, Vec3 at) {
        if (!(wielder.level() instanceof ServerLevel level) || !level.isThundering() || !level.canSeeSky(BlockPos.containing(at)))
            return;
        boolean draws = false;
        for (MaterialPerks.Attunement a : MaterialPerks.attunements(weapon))
            draws |= a.kind().equals("draw");
        if (draws)
            strike(wielder, weapon, at);
    }

    /** Land a bolt at {@code at} whatever the weather; an insulated grip spares the wielder the bolt they called. */
    public static void strike(Player wielder, ItemStack weapon, Vec3 at) {
        if (!(wielder.level() instanceof ServerLevel level))
            return;
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt == null)
            return;
        bolt.moveTo(at);
        if (wielder instanceof ServerPlayer sp)
            bolt.setCause(sp);
        if (insulated(weapon))
            bolt.getPersistentData().putUUID(SPARED, wielder.getUUID());
        level.addFreshEntity(bolt);
    }

    /** Whatever the holder's handle is made of insulates, by the same rule as the shock hazard. */
    static boolean insulated(ItemStack stack) {
        ModularDesign design = ModularStacks.design(stack);
        if (design == null)
            return false;
        ModularDesign.Part grip = design.part("handle") != null ? design.part("handle") : design.part("grip");
        Material m = grip == null ? null : MaterialTable.get().materials().get(grip.material());
        return m != null && Exposure.conduction(m) == Exposure.Conduction.INSULATES;
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel level) || player.tickCount % 20 != 0 || !level.isThundering()
                || !level.canSeeSky(player.blockPosition()))
            return;
        ItemStack held = player.getMainHandItem();
        float chance = 0f;
        for (MaterialPerks.Attunement a : MaterialPerks.attunements(held))
            if (a.kind().equals("draw") && Exposure.conduction(a.material()) == Exposure.Conduction.CONDUCTS)
                chance += DRAW_CHANCE * a.level() * Math.min(1f, a.material().electrical().conductivity().orElse(DRAW_UNMEASURED));
        if (chance <= 0f || player.getRandom().nextFloat() >= chance)
            return;
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt == null)
            return;
        bolt.moveTo(player.position());
        bolt.setVisualOnly(insulated(held));
        level.addFreshEntity(bolt);
    }

    private static int shed(LivingEntity entity) {
        int n = 0;
        for (ItemStack worn : entity.getArmorSlots())
            n += TideKinds.level(worn, "shed");
        return n;
    }

    @SubscribeEvent
    public static void onDamage(LivingIncomingDamageEvent event) {
        if (!event.getSource().is(DamageTypes.LIGHTNING_BOLT))
            return;
        int shed = shed(event.getEntity());
        if (shed > 0)
            event.setAmount(event.getAmount() * Math.max(0f, 1f - shed * SHED_PER_LEVEL));
    }

    @SubscribeEvent
    public static void onStruck(EntityStruckByLightningEvent event) {
        CompoundTag tag = event.getLightning().getPersistentData();
        if (tag.hasUUID(SPARED) && tag.getUUID(SPARED).equals(event.getEntity().getUUID())) {
            event.setCanceled(true);
            return;
        }
        if (event.getEntity() instanceof LivingEntity living && shed(living) >= SHED_TURNS_ASIDE)
            event.setCanceled(true);
    }
}
