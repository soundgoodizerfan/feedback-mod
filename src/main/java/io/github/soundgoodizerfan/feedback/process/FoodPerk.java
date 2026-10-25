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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.FTuning;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.stats.Stats;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;

/**
 * Loads every food perk from {@code data/<namespace>/food_perk/*.json}, keyed by file name. A perk is a
 * trait specific to one food (ginger settles the stomach), where a {@link Compound} is a stat everything
 * has. Its strength in a meal is the blended concentration, carried in the compound maps under
 * {@code perk:<id>}; every figure below scales with it continuously. {@code kind} names the family:
 * <ul>
 * <li>{@code cleanse}: cuts the remaining time of the listed effects up to a level cap.
 * <li>{@code ward}: for {@code ticks} x strength, the listed effects cannot land (to the cap), and any trait runs.
 * <li>{@code resist}: for {@code ticks} x strength, the {@code grant}ed effects and the resisting traits run.
 * <li>{@code tune}: reshapes the meal's other doses (traits {@code absorb}, {@code duration}) or its warm-meal
 *     regeneration ({@code warm_regen}), and may run timed traits like the others.
 * <li>{@code purge}: divides the meal's {@code target} hazard (pathogen or toxin) by {@code 1 + factor x strength}.
 * <li>{@code tolerance}: multiplies what salt does for the {@code target} hazard by {@code 1 + factor x strength}.
 * </ul>
 * A timed trait is armed in a list on the player and read where it acts ({@link #trait}, {@link #tick}); nothing
 * ticks while none is live. Server only.
 *
 * @param effects      cleanse: the effects it eases. ward: the effects it blocks.
 * @param maxAmplifier cleanse and ward: the highest level it can ease or block; a stronger case is beyond it.
 * @param target       purge and tolerance: {@code pathogen} or {@code toxin}.
 * @param factor       purge and tolerance: the figure at strength 1.
 * @param ticks        how long the timed traits run at strength 1; 0 for none.
 * @param grant        vanilla effects given when the dose arrives, their ticks at strength 1.
 * @param drawbacks    the cost, in the same form: effects given the same way.
 * @param traits       timed or dose-shaping behaviours by name and size at strength 1: freeze_relief, cool_burn,
 *                     natural_regen, air, stamina_regen, knockback_ward, strike, frail, rest, absorb, duration, warm_regen.
 */
public class FoodPerk extends SimpleJsonResourceReloadListener {

    /** Key prefix of a perk inside a food's or meal's active map. */
    public static final String PREFIX = "perk:";

    private static final String TIMERS = "feedback_perk_timers";
    private static final Set<String> KINDS = Set.of("cleanse", "ward", "resist", "tune", "purge", "tolerance");

    public record Give(ResourceLocation effect, int amplifier, int ticks) {
        static final Codec<Give> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("effect").forGetter(Give::effect),
                Codec.intRange(0, 4).optionalFieldOf("amplifier", 0).forGetter(Give::amplifier),
                Codec.intRange(1, 72000).fieldOf("ticks").forGetter(Give::ticks)
        ).apply(instance, Give::new));
    }

    public record Def(String kind, List<ResourceLocation> effects, int maxAmplifier, String target, float factor, int ticks,
                      List<Give> grant, List<Give> drawbacks, Map<String, Float> traits) {
        static final Codec<Def> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("kind").forGetter(Def::kind),
                ResourceLocation.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(Def::effects),
                Codec.intRange(0, 4).optionalFieldOf("max_amplifier", 0).forGetter(Def::maxAmplifier),
                Codec.STRING.optionalFieldOf("target", "").forGetter(Def::target),
                Codec.FLOAT.optionalFieldOf("factor", 0f).forGetter(Def::factor),
                Codec.intRange(0, 72000).optionalFieldOf("ticks", 0).forGetter(Def::ticks),
                Give.CODEC.listOf().optionalFieldOf("grant", List.of()).forGetter(Def::grant),
                Give.CODEC.listOf().optionalFieldOf("drawbacks", List.of()).forGetter(Def::drawbacks),
                Codec.unboundedMap(Codec.STRING, Codec.FLOAT).optionalFieldOf("traits", Map.of()).forGetter(Def::traits)
        ).apply(instance, Def::new));

        /** Whether a dose of it has anything to do when it comes due; the rest only shape other figures. */
        boolean dosed() {
            return kind.equals("cleanse") || ticks > 0 || !grant.isEmpty() || !drawbacks.isEmpty() || traits.containsKey("rest");
        }
    }

    private static final FoodPerk INSTANCE = new FoodPerk();

    private Map<String, Def> perks = Map.of();

    private FoodPerk() {
        super(new com.google.gson.GsonBuilder().create(), "food_perk");
    }

    public static FoodPerk get() {
        return INSTANCE;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> json, ResourceManager resources, ProfilerFiller profiler) {
        Map<String, Def> loaded = new TreeMap<>();
        json.forEach((id, element) -> Def.CODEC.parse(JsonOps.INSTANCE, element)
                .resultOrPartial(error -> Feedback.LOGGER.error("Bad food perk {}: {}", id, error))
                .ifPresent(d -> {
                    if (KINDS.contains(d.kind()))
                        loaded.put(id.getPath(), d);
                    else
                        Feedback.LOGGER.warn("Food perk {} has unknown kind {}", id, d.kind());
                }));
        perks = Map.copyOf(loaded);
        Feedback.LOGGER.info("Loaded {} food perks", perks.size());
    }

    public Optional<Def> find(String name) {
        return Optional.ofNullable(perks.get(name));
    }

    /** Whether a perk has anything to do when its dose comes due (see {@link Def#dosed}). */
    public boolean dosed(String name) {
        Def def = perks.get(name);
        return def != null && def.dosed();
    }

    /** Apply a perk whose dose has come due, at a strength of 0 to 1. */
    public void apply(ServerPlayer player, String name, float strength) {
        Def def = perks.get(name);
        if (def == null)
            return;
        if (def.kind().equals("cleanse"))
            cleanse(player, def, strength);
        if (def.ticks() > 0)
            arm(player, name, strength, Math.round(def.ticks() * strength));
        def.grant().forEach(g -> give(player, g, strength));
        def.drawbacks().forEach(g -> give(player, g, strength));
        float rest = def.traits().getOrDefault("rest", 0f);
        if (rest > 0f) {
            var stat = Stats.CUSTOM.get(Stats.TIME_SINCE_REST);
            player.getStats().setValue(player, stat, Math.round(player.getStats().getValue(stat) * (1f - Math.min(1f, rest * strength))));
        }
    }

    /** Cut the remaining duration of each listed effect at or under the cap by the strength; 1 removes it. */
    private static void cleanse(ServerPlayer player, Def def, float strength) {
        for (ResourceLocation id : def.effects()) {
            var holder = BuiltInRegistries.MOB_EFFECT.getHolder(id).orElse(null);
            if (holder == null)
                continue;
            MobEffectInstance have = player.getActiveEffectsMap().get(holder);
            if (have == null || have.getAmplifier() > def.maxAmplifier())
                continue;
            player.removeEffect(holder);
            int left = Math.round(have.getDuration() * (1f - strength));
            if (left > 0)
                player.addEffect(new MobEffectInstance(holder, left, have.getAmplifier(), have.isAmbient(), have.isVisible(), have.showIcon()));
        }
    }

    /** Give a vanilla effect for its ticks scaled by the strength; under a second is not worth a flicker. */
    private static void give(ServerPlayer player, Give g, float strength) {
        int ticks = Math.round(g.ticks() * strength);
        if (ticks >= 20)
            BuiltInRegistries.MOB_EFFECT.getHolder(g.effect()).ifPresent(h -> player.addEffect(new MobEffectInstance(h, ticks, g.amplifier())));
    }

    /** Start a timed trait; eating the same perk again lengthens it to the later end and keeps the stronger. */
    private static void arm(ServerPlayer player, String name, float strength, int ticks) {
        long until = player.level().getGameTime() + ticks;
        CompoundTag data = player.getPersistentData();
        ListTag list = data.getList(TIMERS, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            if (t.getString("perk").equals(name)) {
                t.putLong("until", Math.max(t.getLong("until"), until));
                t.putFloat("strength", Math.max(t.getFloat("strength"), strength));
                return;
            }
        }
        CompoundTag t = new CompoundTag();
        t.putString("perk", name);
        t.putLong("until", until);
        t.putFloat("strength", strength);
        list.add(t);
        data.put(TIMERS, list);
    }

    /** Drop every timed trait (death). */
    public static void clear(Player player) {
        player.getPersistentData().remove(TIMERS);
    }

    /** The named trait summed over the player's live timers, each at its strength; 0 when none are running. */
    public static float trait(Player player, String trait) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(TIMERS))
            return 0f;
        long now = player.level().getGameTime();
        float sum = 0f;
        ListTag list = data.getList(TIMERS, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            Def def = INSTANCE.perks.get(t.getString("perk"));
            if (def != null && t.getLong("until") > now)
                sum += def.traits().getOrDefault(trait, 0f) * t.getFloat("strength");
        }
        return sum;
    }

    /** Whether a live ward blocks this effect landing. */
    public static boolean warded(Player player, MobEffectInstance incoming) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(TIMERS))
            return false;
        long now = player.level().getGameTime();
        ResourceLocation id = BuiltInRegistries.MOB_EFFECT.getKey(incoming.getEffect().value());
        ListTag list = data.getList(TIMERS, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            Def def = INSTANCE.perks.get(t.getString("perk"));
            if (def != null && def.kind().equals("ward") && t.getLong("until") > now
                    && incoming.getAmplifier() <= def.maxAmplifier() && def.effects().contains(id))
                return true;
        }
        return false;
    }

    /** Twice a second: drop spent timers and run the traits that act on the player each pass. */
    public static void tick(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(TIMERS))
            return;
        long now = player.level().getGameTime();
        ListTag list = data.getList(TIMERS, Tag.TAG_COMPOUND);
        for (int i = list.size() - 1; i >= 0; i--)
            if (list.getCompound(i).getLong("until") <= now)
                list.remove(i);
        if (list.isEmpty()) {
            data.remove(TIMERS);
            return;
        }
        float cold = trait(player, "freeze_relief");
        if (cold > 0f && player.getTicksFrozen() > 0)
            player.setTicksFrozen(Math.max(0, player.getTicksFrozen() - Math.round(cold * FTuning.PERK_FREEZE_RELIEF_TICKS)));
        float cool = trait(player, "cool_burn");
        if (cool > 0f && player.getRemainingFireTicks() > 0)
            player.setRemainingFireTicks(Math.max(0, player.getRemainingFireTicks() - Math.round(cool * FTuning.PERK_COOL_BURN_TICKS)));
        float heal = trait(player, "natural_regen");
        if (heal > 0f && player.getFoodData().getFoodLevel() >= FTuning.PERK_FED_FOOD && player.getHealth() < player.getMaxHealth())
            player.heal(heal);
        float air = trait(player, "air");
        if (air > 0f && player.isUnderWater() && player.getAirSupply() < player.getMaxAirSupply())
            player.setAirSupply(Math.min(player.getMaxAirSupply(), player.getAirSupply() + Math.round(air * FTuning.PERK_AIR_TICKS)));
    }

    /** The named {@code tune} trait summed over the perks in a meal's active map, each at its blended strength. */
    public float tune(Map<String, Float> actives, String trait) {
        float sum = 0f;
        for (Map.Entry<String, Float> e : actives.entrySet()) {
            if (!e.getKey().startsWith(PREFIX))
                continue;
            Def def = perks.get(e.getKey().substring(PREFIX.length()));
            if (def != null && def.kind().equals("tune"))
                sum += def.traits().getOrDefault(trait, 0f) * e.getValue();
        }
        return sum;
    }

    /**
     * What a meal's {@code hazard} (pathogen or toxin) concentration is divided by: salt tolerates it
     * ({@link Cooking#tolerance}), a {@code tolerance} perk makes the salt count for more, and a {@code purge} perk
     * divides it further. {@code actives} is the meal's compound-and-perk map.
     */
    public float tolerance(float salt, Map<String, Float> actives, String hazard) {
        float brine = 0f;
        float purge = 0f;
        for (Map.Entry<String, Float> e : actives.entrySet()) {
            if (!e.getKey().startsWith(PREFIX))
                continue;
            Def def = perks.get(e.getKey().substring(PREFIX.length()));
            if (def == null || !def.target().equals(hazard))
                continue;
            if (def.kind().equals("tolerance"))
                brine += def.factor() * e.getValue();
            else if (def.kind().equals("purge"))
                purge += def.factor() * e.getValue();
        }
        return (1f + FTuning.SALT_TOLERANCE * salt * (1f + brine)) * (1f + purge);
    }
}
