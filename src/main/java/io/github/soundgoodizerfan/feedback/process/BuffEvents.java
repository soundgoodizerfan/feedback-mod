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
package io.github.soundgoodizerfan.feedback.process;

import java.util.Map;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.item.Meal;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * What a meal does for you (food spec 7). A food's compounds ({@link Compound}) map to mob effects;
 * fat and fibre slow absorption, so they delay the onset and stretch the duration, while a bare
 * sugar comes on fast, ends fast and leaves a crash. A dose that has not arrived yet waits in a list
 * on the player, checked twice a second: no item and no effect is ticked while it waits. No new meter
 * is made; the effects are vanilla's.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class BuffEvents {

    private static final String KEY = "feedback_buffs";

    private BuffEvents() {
    }

    @SubscribeEvent
    public static void onEaten(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;
        // A jar is the container: its dose is the contained food's, and a sealed jar's spoilage clock is stopped.
        io.github.soundgoodizerfan.feedback.item.Canned can = event.getItem().get(FDataComponents.CANNED.get());
        ItemStack eaten = can != null ? can.food() : event.getItem();
        boolean frozen = can != null && can.sealed();
        Meal meal = eaten.get(FDataComponents.MEAL.get());
        io.github.soundgoodizerfan.feedback.potion.Lacing lace = eaten.get(FDataComponents.LACING.get());
        if (meal != null && lace != null) {
            // A potion laced in (spec 7): a hidden dose through the same path, slowed by fat and fibre; the whole dish counts as eaten.
            float absorb = Math.max(0f, (meal.fat() + meal.fibre()) * (1f + FoodPerk.get().tune(meal.compounds(), "absorb")));
            schedule(player, player.level().getGameTime() + Math.round(absorb * FTuning.FOOD_BUFF_DELAY_TICKS), lace.effect(), lace.amplifier(), lace.duration());
            run(player, player.level().getGameTime());
            if (lace.detectable(meal))
                io.github.soundgoodizerfan.feedback.net.HudReadoutPayload.send(player, net.minecraft.network.chat.Component.translatable("message.feedback.taste.off"), FTuning.BITE_HUD_TICKS);
        }
        if (meal != null) {
            // Rotten food has nothing left in it to dose from (a dish carries its spoilage; a ladled serving carries none).
            FoodState rotten = eaten.get(FDataComponents.FOOD_STATE.get());
            if (rotten != null && Cooking.spoilage(rotten) == 2)
                return;
            dose(player, meal.compounds(), meal.fat() + meal.fibre(), 1f);
            // A warm meal comforts: it shakes off the cold and settles you.
            if (io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat.get(eaten, player.level()).value() > FTuning.WARM_TU.value()) {
                player.setTicksFrozen(0);
                player.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.REGENERATION,
                        Math.round(FTuning.WARM_MEAL_REGEN_TICKS * (1f + FoodPerk.get().tune(meal.compounds(), "warm_regen")))));
            }
            return;
        }
        var level = player.level();
        Cooking.find(level, eaten).ifPresent(entry -> {
            FoodState state = frozen ? Cooking.state(eaten, entry) : Cooking.current(eaten, entry, level, AmbientTable.at(level).value());
            if (Cooking.doneness(entry, state) == Cooking.Doneness.CHARRED || Cooking.spoilage(state) == 2)
                return;
            // Dry food has no water to carry its compound, so it comes on later.
            float dry = 1f - Math.min(1f, state.moisture() / Math.max(0.05f, entry.food().moisture()));
            dose(player, entry.food().actives(), entry.food().fat() + entry.food().fibre() + dry * 0.5f, 1f);
        });
    }

    private static void dose(ServerPlayer player, Map<String, Float> compounds, float absorb, float scale) {
        long now = player.level().getGameTime();
        // Tune perks in the same meal reshape every dose: absorb moves the delay (and with it the crash and the stretch), duration lengthens up to double.
        absorb = Math.max(0f, absorb * (1f + FoodPerk.get().tune(compounds, "absorb")));
        float stretch = 1f + Math.max(0f, Math.min(1f, FoodPerk.get().tune(compounds, "duration")));
        for (Map.Entry<String, Float> e : compounds.entrySet()) {
            float strength = e.getValue() * scale;
            if (strength < FTuning.FOOD_BUFF_MIN_STRENGTH)
                continue;
            int delay = Math.round(absorb * FTuning.FOOD_BUFF_DELAY_TICKS);
            if (e.getKey().startsWith(FoodPerk.PREFIX)) {
                String perk = e.getKey().substring(FoodPerk.PREFIX.length());
                if (FoodPerk.get().dosed(perk))
                    schedulePerk(player, now + delay, perk, strength);
                continue;
            }
            Compound c = CompoundTable.get().find(e.getKey()).orElse(null);
            if (c == null || c.effect().isEmpty())
                continue;
            int duration = Math.round(strength * c.ticksPerStrength() * (1f + absorb) * stretch);
            schedule(player, now + delay, c.effect().get(), c.amplifier(), duration);
            if (c.crash().isPresent() && absorb < FTuning.FOOD_BUFF_CRASH_ABSORB)
                schedule(player, now + delay + duration, c.crash().get().effect(), 0, c.crash().get().ticks());
        }
        run(player, now);
    }

    private static void schedule(ServerPlayer player, long due, ResourceLocation effect, int amplifier, int duration) {
        CompoundTag entry = new CompoundTag();
        entry.putLong("due", due);
        entry.putString("effect", effect.toString());
        entry.putInt("amp", amplifier);
        entry.putInt("ticks", duration);
        CompoundTag data = player.getPersistentData();
        ListTag list = data.getList(KEY, Tag.TAG_COMPOUND);
        list.add(entry);
        data.put(KEY, list);
    }

    private static void schedulePerk(ServerPlayer player, long due, String perk, float strength) {
        CompoundTag entry = new CompoundTag();
        entry.putLong("due", due);
        entry.putString("perk", perk);
        entry.putFloat("strength", strength);
        CompoundTag data = player.getPersistentData();
        ListTag list = data.getList(KEY, Tag.TAG_COMPOUND);
        list.add(entry);
        data.put(KEY, list);
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % 10 == 0) {
            run(player, player.level().getGameTime());
            FoodPerk.tick(player);
        }
    }

    /** A ward keeps the listed effects off. */
    @SubscribeEvent
    public static void onEffect(MobEffectEvent.Applicable event) {
        if (event.getEntity() instanceof ServerPlayer player && FoodPerk.warded(player, event.getEffectInstance()))
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
    }

    /** Keen-edge perks hit harder (not over a strength potion) and leave the eater easier to hurt. */
    @SubscribeEvent
    public static void onDamage(LivingIncomingDamageEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer from && !from.hasEffect(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST))
            event.setAmount(event.getAmount() * (1f + FoodPerk.trait(from, "strike")));
        if (event.getEntity() instanceof ServerPlayer hit)
            event.setAmount(event.getAmount() * (1f + FoodPerk.trait(hit, "frail")));
    }

    /** A knockback ward takes its strength off the shove. Explosions push by velocity, not through this, and still do. */
    @SubscribeEvent
    public static void onKnockback(LivingKnockBackEvent event) {
        if (event.getEntity() instanceof ServerPlayer player)
            event.setStrength(event.getStrength() * Math.max(0f, 1f - FoodPerk.trait(player, "knockback_ward")));
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player)
            FoodPerk.clear(player);
    }

    /** Start every dose that has come due. */
    private static void run(ServerPlayer player, long now) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(KEY))
            return;
        ListTag list = data.getList(KEY, Tag.TAG_COMPOUND);
        for (int i = list.size() - 1; i >= 0; i--) {
            CompoundTag entry = list.getCompound(i);
            if (entry.getLong("due") > now)
                continue;
            list.remove(i);
            // A perk dose names its perk; the original effect dose does not.
            if (entry.contains("perk")) {
                FoodPerk.get().apply(player, entry.getString("perk"), entry.getFloat("strength"));
                continue;
            }
            ResourceLocation id = ResourceLocation.tryParse(entry.getString("effect"));
            if (id != null)
                BuiltInRegistries.MOB_EFFECT.getHolder(id).ifPresent(holder ->
                        player.addEffect(new MobEffectInstance(holder, entry.getInt("ticks"), entry.getInt("amp"))));
        }
        if (list.isEmpty())
            data.remove(KEY);
    }
}
