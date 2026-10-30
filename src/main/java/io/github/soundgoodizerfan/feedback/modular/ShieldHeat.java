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
package io.github.soundgoodizerfan.feedback.modular;

import java.util.Optional;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.hazard.Grip;
import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
import io.github.soundgoodizerfan.feedback.core.unit.TuRate;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;

/**
 * A shield as a thermal sink: a blow it stops ends as heat in the face.
 *
 * <h2>Which model, and why</h2>
 * {@link ItemHeat}'s stamp and tick, not a {@code ThermalBody}: a shield is carried, so it cools in
 * a chest and off-world for nothing, as a gun barrel does ({@code GunCondition}). It is stamped
 * directly rather than through {@link ItemHeat#set}, which drops anything under the warm
 * threshold -- a sword blow's few Tu would otherwise never accumulate from cold. Its cooling rate
 * is its own ({@link #coolingRate}): a heavy face sheds slowly.
 *
 * <h2>Heat in</h2>
 * Blocked damage is the blow's Fu; {@code FTuning.SHIELD_WORK_PER_DAMAGE} turns a point of it into
 * Work and {@code SHIELD_HEAT_FRACTION} is the share that stays in the face. Over the thermal mass
 * (baked mass x {@code SHIELD_THERMAL_MASS_PER_MU}; no material file has a specific heat, and
 * adding one was declined in mechanics 5.6) that is the rise, added to the present figure, so
 * blows stack and the gap between them matters. A blow from a fire (a damage-type tag in
 * {@link ShieldSources}) also pulls the face towards that source's temperature by the difference
 * law: it approaches the flame and never passes it.
 *
 * <h2>Softening</h2>
 * The figure that means a material "loses strength" is its {@code service_temperature} (mechanics
 * 5.6: a body of it softening when run too hot), else the point it burns at
 * ({@code oxidation_temperature}), as {@link Grip#stands} reads it. Capacity holds in full up to
 * {@code SHIELD_SOFTEN_ONSET} of the way from the room to that figure, then falls linearly to
 * {@code SHIELD_SOFTEN_FLOOR} at it. Past it a blow wears the shield {@code SHIELD_HOT_WEAR_MULT}
 * times as hard: misuse, never an instant break. A face with neither figure never softens.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class ShieldHeat {

    private ShieldHeat() {
    }

    private static float thermalMass(ModularFigures f) {
        return Math.max(1f, f.get(ModularFigures.Fig.MASS) * FTuning.SHIELD_THERMAL_MASS_PER_MU);
    }

    /** Tu shed per tick: a fixed leak over the thermal mass, so a heavy face cools slowly. */
    public static TuRate coolingRate(ItemStack shield) {
        ModularFigures f = ModularStacks.figures(shield);
        return f == null ? FTuning.ITEM_COOLING_TU_PER_TICK : new TuRate(FTuning.SHIELD_COOLING_WORK_PER_TICK / thermalMass(f));
    }

    private static void stamp(ItemStack shield, float tu, Level level) {
        shield.set(FDataComponents.TEMPERATURE.get(), tu);
        shield.set(FDataComponents.HEATED_AT.get(), level.getGameTime());
    }

    /** The material of the part in {@code role} of a designed shield, or null. */
    private static ResourceLocation partMaterial(ItemStack shield, boolean grip) {
        ModularDesign design = ModularStacks.design(shield);
        ModularType type = design == null ? null : ModularTypes.known().get(design.type());
        if (type == null)
            return null;
        ModularDesign.Part part = grip ? design.part("G") : null;
        if (part == null)
            part = design.part(type.primary());
        return part == null ? null : part.material();
    }

    /** The temperature the face softens at, if its material says. */
    public static Optional<Float> stands(Level level, ItemStack shield) {
        ResourceLocation id = partMaterial(shield, false);
        Material face = id == null ? null : ClientMaterials.on(level).get(id);
        return face == null ? Optional.empty() : Grip.stands(face);
    }

    /** The share of its capacity the face holds at its present temperature, 1 when cool. */
    public static float hold(Level level, ItemStack shield) {
        Optional<Float> stands = stands(level, shield);
        float ambient = AmbientTable.at(level).value();
        if (stands.isEmpty() || stands.get() <= ambient)
            return 1f;
        float frac = (ItemHeat.get(shield, level).value() - ambient) / (stands.get() - ambient);
        if (frac <= FTuning.SHIELD_SOFTEN_ONSET)
            return 1f;
        float t = Math.min(1f, (frac - FTuning.SHIELD_SOFTEN_ONSET) / (1f - FTuning.SHIELD_SOFTEN_ONSET));
        return 1f + (FTuning.SHIELD_SOFTEN_FLOOR - 1f) * t;
    }

    /**
     * The hottest face, in Tu, the hand may hold: through the grip's material if it has a grip
     * part, else through the face itself (a wood or ceramic face insulates its own holder), at
     * {@code SHIELD_GRIP_EXPOSURE}. A grip that does not insulate protects nothing.
     */
    public static float handlesUpToTu(Level level, ItemStack shield) {
        ResourceLocation id = partMaterial(shield, true);
        return id == null ? FTuning.CONTACT_PAIN_TU.value() : Grip.safeUpTo(level, id, FTuning.SHIELD_GRIP_EXPOSURE);
    }

    /** {@code from} drawn towards {@code source} for {@code ticks}, never past it. */
    private static float approach(float from, float source, float mass, int ticks) {
        if (source <= from)
            return from;
        float k = FTuning.FIRE_CONDUCTANCE.workPerTickPerTu() / mass;
        float keep = k >= 1f ? 0f : (float) Math.pow(1f - k, ticks);
        return source + (from - source) * keep;
    }

    private static ItemStack raised(LivingEntity entity) {
        ItemStack shield = entity.getUseItem();
        return shield.getItem() instanceof ModularShield && !ModularStacks.broken(shield) && ModularStacks.figures(shield) != null
                ? shield : ItemStack.EMPTY;
    }

    /** After every archetype has settled what was stopped, book the heat and the wear of it. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBlock(LivingShieldBlockEvent event) {
        LivingEntity blocker = event.getEntity();
        ItemStack shield = raised(blocker);
        if (blocker.level().isClientSide || shield.isEmpty() || !event.getBlocked())
            return;
        Level level = blocker.level();
        float mass = thermalMass(ModularStacks.figures(shield));
        float now = ItemHeat.get(shield, level).value();
        float t = now + event.getBlockedDamage() * FTuning.SHIELD_WORK_PER_DAMAGE * FTuning.SHIELD_HEAT_FRACTION / mass;
        t = approach(t, ShieldSources.get().tu(event.getDamageSource()), mass, FTuning.SHIELD_FIRE_TICKS);
        if (stands(level, shield).map(s -> now > s).orElse(false))
            event.setShieldDamage(event.shieldDamage() * FTuning.SHIELD_HOT_WEAR_MULT);
        stamp(shield, t, level);
    }

    /**
     * Fire that has no position (standing in it, lava, being alight) is never offered to a shield
     * by vanilla, so it does not reach {@link #onBlock}; a raised shield still sits in it, and its
     * face warms towards the source. It is not stopped: a shield is no fire screen.
     */
    @SubscribeEvent
    public static void onFire(LivingIncomingDamageEvent event) {
        LivingEntity entity = event.getEntity();
        ItemStack shield = raised(entity);
        if (entity.level().isClientSide || shield.isEmpty() || event.getSource().getSourcePosition() != null)
            return;
        float source = ShieldSources.get().tu(event.getSource());
        if (source <= 0f)
            return;
        Level level = entity.level();
        stamp(shield, approach(ItemHeat.get(shield, level).value(), source, thermalMass(ModularStacks.figures(shield)),
                FTuning.SHIELD_FIRE_TICKS), level);
    }
}
