// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.fitting;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.StringRepresentable;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * How fittings look, as two datapack tables read by the client's {@code FittingRenderer} and shipped to it ({@code FittingLookSyncPayload}).
 * Nothing here is simulation: a missing row only means a fitting is not drawn.
 *
 * <h2>{@code data/<ns>/fitting_model/<kind>.json}: what a kind of fitting looks like</h2>
 * The id is the fitting's kind ({@link FittingKinds}: {@code feedback:temperature_sensor} for {@code TemperatureSensorFitting}).
 * <pre>
 * { "model": "feedback:fitting/temperature_sensor", "placement": "anchor", "anchor": "top",
 *   "overlay": "feedback:fitting/temperature_sensor_active", "tint": false }
 * </pre>
 * {@code model} is a plain element model under {@code assets/<ns>/models/fitting/}; every file there is loaded as a standalone model.
 * {@code placement} is {@link Placement}: {@code wrap} draws it at the holder's origin (a lining or sleeve around the holder), {@code anchor}
 * at the holder's named anchor ({@code anchor}), {@code counted} at that anchor stepped along its {@code step} for each further fitting of
 * the same kind. A <b>sided</b> fitting ignores both and sits on the face it is mounted on. {@code overlay} (optional) is a second model
 * drawn over the first while the fitting is {@link Fitting#active()}; {@code tint} (default false) tints the model by the fitting's
 * {@link Fitting#tintMaterial() material}'s colour.
 *
 * <h2>{@code data/<ns>/fitting_anchors/<holder>.json}: where a holder carries them</h2>
 * The id is the holder's <b>block entity type</b> id ({@code feedback:thermal_vessel}); a holder with no row draws no fittings.
 * <pre>
 * { "faces": { "up": { "pos": [8, 16, 8], "rot": [0, 0, 0] } },
 *   "anchors": { "top": { "pos": [8, 16, 8], "rot": [0, 0, 0], "step": [0, 0, 2] } } }
 * </pre>
 * Positions are in block units (0 to 16), rotations are degrees about the anchor, {@code step} is the offset per further copy. A sided
 * fitting's model is authored sitting on the <b>up</b> face (standing on y = 16, centred); the renderer turns it onto the face it is on
 * about the block's centre. {@code faces} (optional, keyed by direction name) overrides that default for one face.
 */
public final class FittingLooks {

    private FittingLooks() {
    }

    /** Where an unsided fitting's model is put. */
    public enum Placement implements StringRepresentable {
        /** At the holder's origin: it wraps the holder (a lining, a sleeve). */
        WRAP("wrap"),
        /** At one named anchor of the holder. */
        ANCHOR("anchor"),
        /** At a named anchor, stepped along for each further fitting of the same kind. */
        COUNTED("counted");

        public static final Codec<Placement> CODEC = StringRepresentable.fromEnum(Placement::values);
        private final String key;

        Placement(String key) {
            this.key = key;
        }

        @Override
        public String getSerializedName() {
            return key;
        }
    }

    /** One fitting kind's look; see the class doc. {@code anchor} is "" where the placement needs none. */
    public record Look(ResourceLocation model, Placement placement, String anchor, Optional<ResourceLocation> overlay, boolean tint) {
        public static final Codec<Look> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.fieldOf("model").forGetter(Look::model),
                Placement.CODEC.optionalFieldOf("placement", Placement.WRAP).forGetter(Look::placement),
                Codec.STRING.optionalFieldOf("anchor", "").forGetter(Look::anchor),
                ResourceLocation.CODEC.optionalFieldOf("overlay").forGetter(Look::overlay),
                Codec.BOOL.optionalFieldOf("tint", false).forGetter(Look::tint)).apply(i, Look::new));
    }

    /** A mount point on a holder: position in block units, rotation in degrees about it, and the step per further copy. */
    public record Anchor(Vector3f pos, Vector3f rot, Vector3f step) {
        public static final Codec<Anchor> CODEC = RecordCodecBuilder.create(i -> i.group(
                ExtraCodecs.VECTOR3F.fieldOf("pos").forGetter(Anchor::pos),
                ExtraCodecs.VECTOR3F.optionalFieldOf("rot", new Vector3f()).forGetter(Anchor::rot),
                ExtraCodecs.VECTOR3F.optionalFieldOf("step", new Vector3f()).forGetter(Anchor::step)).apply(i, Anchor::new));
    }

    /** The mount points of one holder: per-face overrides and the named anchors. */
    public record HolderAnchors(Map<String, Anchor> faces, Map<String, Anchor> anchors) {
        public static final Codec<HolderAnchors> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.unboundedMap(Codec.STRING, Anchor.CODEC).optionalFieldOf("faces", Map.of()).forGetter(HolderAnchors::faces),
                Codec.unboundedMap(Codec.STRING, Anchor.CODEC).optionalFieldOf("anchors", Map.of()).forGetter(HolderAnchors::anchors))
                .apply(i, HolderAnchors::new));
    }

    public static final Codec<Map<ResourceLocation, Look>> MODELS_CODEC = Codec.unboundedMap(ResourceLocation.CODEC, Look.CODEC);
    public static final Codec<Map<ResourceLocation, HolderAnchors>> ANCHORS_CODEC = Codec.unboundedMap(ResourceLocation.CODEC, HolderAnchors.CODEC);

    private static volatile Map<ResourceLocation, Look> models = Map.of();
    private static volatile Map<ResourceLocation, HolderAnchors> anchors = Map.of();

    /** The look of a fitting kind, or null when it has none (it is not drawn). */
    @Nullable
    public static Look model(ResourceLocation kind) {
        return models.get(kind);
    }

    /** The mount points of a holder's block entity type, or null when it has none (it draws no fittings). */
    @Nullable
    public static HolderAnchors anchors(ResourceLocation holderType) {
        return anchors.get(holderType);
    }

    public static Map<ResourceLocation, Look> models() {
        return models;
    }

    public static Map<ResourceLocation, HolderAnchors> anchors() {
        return anchors;
    }

    /** Client side of the sync. */
    public static void set(Map<ResourceLocation, Look> incomingModels, Map<ResourceLocation, HolderAnchors> incomingAnchors) {
        models = Map.copyOf(incomingModels);
        anchors = Map.copyOf(incomingAnchors);
    }

    /** Reads {@code data/<ns>/fitting_model/}. */
    public static final class Models extends SimpleJsonResourceReloadListener {
        public static final String DIRECTORY = "fitting_model";
        private static final Models INSTANCE = new Models();

        private Models() {
            super(new GsonBuilder().create(), DIRECTORY);
        }

        public static Models get() {
            return INSTANCE;
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> json, ResourceManager resources, ProfilerFiller profiler) {
            Map<ResourceLocation, Look> loaded = new HashMap<>();
            json.forEach((id, element) -> Look.CODEC.parse(JsonOps.INSTANCE, element)
                    .resultOrPartial(error -> Feedback.LOGGER.error("Bad fitting model {}: {}", id, error))
                    .ifPresent(look -> loaded.put(id, look)));
            models = Map.copyOf(loaded);
            Feedback.LOGGER.info("Loaded {} fitting models", models.size());
        }
    }

    /** Reads {@code data/<ns>/fitting_anchors/}. */
    public static final class Anchors extends SimpleJsonResourceReloadListener {
        public static final String DIRECTORY = "fitting_anchors";
        private static final Anchors INSTANCE = new Anchors();

        private Anchors() {
            super(new GsonBuilder().create(), DIRECTORY);
        }

        public static Anchors get() {
            return INSTANCE;
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> json, ResourceManager resources, ProfilerFiller profiler) {
            Map<ResourceLocation, HolderAnchors> loaded = new HashMap<>();
            json.forEach((id, element) -> HolderAnchors.CODEC.parse(JsonOps.INSTANCE, element)
                    .resultOrPartial(error -> Feedback.LOGGER.error("Bad fitting anchors {}: {}", id, error))
                    .ifPresent(holder -> loaded.put(id, holder)));
            anchors = Map.copyOf(loaded);
            Feedback.LOGGER.info("Loaded fitting anchors for {} holders", anchors.size());
        }
    }
}
