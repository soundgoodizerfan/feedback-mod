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

import java.util.Map;
import java.util.TreeMap;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/**
 * The attuned effects ({@code data/<namespace>/attunement/*.json}, {@code docs/feedback_enchanting_spec.md} 2): where
 * the polarization must land to take, and what the effect does on a part of each role. A role with no entry does
 * nothing. Keeps the client's copy ({@code AttunementSyncPayload}) beside the server's: the gauge and the
 * tooltip are drawn there, the bake runs on both.
 */
public final class Attunements extends SimpleJsonResourceReloadListener {

    public static final String DIRECTORY = "attunement";
    /** The key an attuned effect has in a part's {@code dims}: {@code attuned.<path of the effect id>}, the level as its value (negative when it took on the mirror side). */
    public static final String PREFIX = "attuned.";

    /**
     * What an effect does on one role.
     *
     * @param kind what happens (handlers in {@link MaterialPerks}); one the code does not know does nothing.
     * @param flaw flaws it adds to the part per level when the tool is baked: positive makes it brittle, negative
     *             tougher. This is how keen costs toughness and ward's plate resists fracture.
     */
    public record Behaviour(String kind, int flaw) {
        static final Codec<Behaviour> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.optionalFieldOf("kind", "").forGetter(Behaviour::kind),
                Codec.INT.optionalFieldOf("flaw", 0).forGetter(Behaviour::flaw)
        ).apply(i, Behaviour::new));
    }

    /**
     * @param target where P must land, 0..1 of saturation.
     * @param band   half-width of the good zone; narrower for a stronger effect.
     * @param roles  by the role's {@code name} in the type file (head, blade, handle, plate...).
     * @param mirror what the effect does on each role when P lands in the band at {@code -target} instead: the same
     *               coupling reversed. Empty means no mirror, and a landing below zero is a miss.
     */
    public record Effect(ResourceLocation id, float target, float band, Map<String, Behaviour> roles, Map<String, Behaviour> mirror) {
        public static final Codec<Effect> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.fieldOf("id").forGetter(Effect::id),
                Codec.floatRange(0.05f, 1f).fieldOf("target").forGetter(Effect::target),
                Codec.floatRange(0.02f, 0.5f).fieldOf("band").forGetter(Effect::band),
                Codec.unboundedMap(Codec.STRING, Behaviour.CODEC).optionalFieldOf("roles", Map.of()).forGetter(Effect::roles),
                Codec.unboundedMap(Codec.STRING, Behaviour.CODEC).optionalFieldOf("mirror", Map.of()).forGetter(Effect::mirror)
        ).apply(i, Effect::new));

        public String key() {
            return PREFIX + id.getPath();
        }

        public boolean hasMirror() {
            return !mirror.isEmpty();
        }

        /** What the effect does on {@code role}, on the mirror side when {@code negative}; null when it does nothing there. */
        public Behaviour on(String role, boolean negative) {
            return (negative ? mirror : roles).get(role);
        }
    }

    private static final Attunements INSTANCE = new Attunements();

    private static Map<ResourceLocation, Effect> client = Map.of();
    private Map<ResourceLocation, Effect> effects = Map.of();

    private Attunements() {
        super(new GsonBuilder().create(), DIRECTORY);
    }

    public static Attunements get() {
        return INSTANCE;
    }

    public static void setClient(Map<ResourceLocation, Effect> incoming) {
        client = Map.copyOf(incoming);
    }

    public Map<ResourceLocation, Effect> effects() {
        return new TreeMap<>(effects);
    }

    /** The effects this JVM knows: the client's copy when it has one, else the server's. */
    public static Map<ResourceLocation, Effect> known() {
        return client.isEmpty() ? INSTANCE.effects : client;
    }

    /** The effect a {@code attunement.<path>} key names, or null. */
    public static Effect byKey(String key) {
        if (!key.startsWith(PREFIX))
            return null;
        for (Effect e : known().values())
            if (e.key().equals(key))
                return e;
        return null;
    }

    /**
     * Flaws the attunements on {@code part} add to it when baked in a role called {@code roleName}.
     * Read at bake, so attuning a part before it goes into a tool is what counts.
     */
    public static int flawShift(ModularDesign.Part part, String roleName) {
        int shift = 0;
        for (Map.Entry<String, Float> e : part.dims().entrySet()) {
            Effect effect = byKey(e.getKey());
            Behaviour b = effect == null ? null : effect.on(roleName, e.getValue() < 0f);
            if (b != null)
                shift += b.flaw() * Math.abs(Math.round(e.getValue()));
        }
        return shift;
    }

    /**
     * Reheating past a part's attuning point erases the attunements in it (spec 6). Called wherever a heat stamp is
     * written ({@link io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat#set}), so a part in a furnace, crucible
     * or Hearth loses them the moment it is stamped hot. Parts already inside an assembled tool are not reached.
     */
    public static void eraseIfOverheated(net.minecraft.world.item.ItemStack stack,
            io.github.soundgoodizerfan.feedback.core.unit.Tu tu, net.minecraft.world.level.Level level) {
        io.github.soundgoodizerfan.feedback.process.MadeOf made = stack.get(io.github.soundgoodizerfan.feedback.registry.FDataComponents.MADE_OF.get());
        if (made == null || made.dims().keySet().stream().noneMatch(k -> k.startsWith(PREFIX)))
            return;
        io.github.soundgoodizerfan.feedback.process.Material m =
                io.github.soundgoodizerfan.feedback.process.ClientMaterials.on(level).get(made.material());
        if (m == null || tu.value() <= m.attuning().temperature().value())
            return;
        Map<String, Float> kept = new java.util.HashMap<>(made.dims());
        kept.keySet().removeIf(k -> k.startsWith(PREFIX));
        stack.set(io.github.soundgoodizerfan.feedback.registry.FDataComponents.MADE_OF.get(), new io.github.soundgoodizerfan.feedback.process.MadeOf(
                made.material(), made.stock(), made.cells(), made.variant(), made.flaw(), kept));
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> json, ResourceManager resources, ProfilerFiller profiler) {
        Map<ResourceLocation, Effect> loaded = new TreeMap<>();
        json.forEach((id, element) -> {
            var object = element.getAsJsonObject();
            object.addProperty("id", id.toString());
            Effect.CODEC.parse(JsonOps.INSTANCE, object)
                    .resultOrPartial(error -> Feedback.LOGGER.error("Bad attunement effect {}: {}", id, error))
                    .ifPresent(effect -> loaded.put(id, effect));
        });
        effects = Map.copyOf(loaded);
        Feedback.LOGGER.info("Loaded {} attunement effects", effects.size());
    }
}
