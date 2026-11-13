// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.soundgoodizerfan.feedback.modular.ModularFigures.Fig;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.registry.FModular;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * How well a built item fits. A blueprint is a <b>fit specification</b>, not a lock: per part it names the properties the bake reads
 * (yield strength, hardness, conductivity, an insulation band, a density band) as figures of the drawn material, and the axes as a
 * target. Any material inside the floors and bands fits clean; one short of them still builds and the item is <i>off-drawing</i>.
 * An axis outside the tolerance still fits and costs a <b>fit flaw</b>. Without a sheet the parts are hand-fitted: each is judged
 * against the parts it mates with (an axis two roles share, such as a bore) at the wide hand tolerance.
 *
 * <p>Two figures ride on the finished item ({@link Fit}), kept apart: the <b>fit</b> (joint strength; a gun's spread) and the
 * <b>head start</b> (the durability the item starts with). Neither exceeds what the as-baked figures allow; they only fall short.
 */
public final class ModularFit {

    private ModularFit() {
    }

    /**
     * The floors and bands one drawn part holds a material to, read from the drawn material. A figure the drawn material does not
     * state is 0 (no floor); {@code insulation} is -1 for none.
     */
    public record Spec(float yield, float hardness, float electrical, float thermal, int insulation, float density) {

        public static Spec of(Material m) {
            return new Spec(ModularBake.strength(m), m.hardness().value(), m.electrical().conductivity().orElse(0f),
                    m.thermal().conductivity().map(c -> c.workPerTickPerTu()).orElse(0f),
                    m.electrical().insulation().map(Enum::ordinal).orElse(-1), m.density().muPerMb());
        }

        /** Whether {@code made} is inside this spec's floors and density band. */
        public boolean admits(Material made) {
            Spec s = of(made);
            return floor(yield, s.yield) && floor(hardness, s.hardness) && floor(electrical, s.electrical) && floor(thermal, s.thermal)
                    && s.insulation >= insulation
                    && (density <= 0f || Math.abs(s.density - density) <= density * ModularTuning.FIT_DENSITY_BAND);
        }

        private static boolean floor(float wanted, float got) {
            return wanted <= 0f || got >= wanted * (1f - ModularTuning.FIT_FLOOR_SLACK);
        }
    }

    /** The spec of each part of a drawing, by role. Parts holding a finished item (a held component) have none. */
    public static Map<String, Spec> specs(ModularDesign drawn, Map<ResourceLocation, Material> materials) {
        Map<String, Spec> out = new LinkedHashMap<>();
        for (ModularDesign.Part p : drawn.parts()) {
            Material m = p.sub().isEmpty() ? materials.get(p.material()) : null;
            if (m != null)
                out.put(p.role(), Spec.of(m));
        }
        return out;
    }

    /**
     * What one judgement found.
     *
     * @param structural  the made parts are the drawing's, role for role, by shaped part and count (a held component is not pinned).
     * @param off         some part's material is short of its spec (off-drawing).
     * @param flaw        0 to 1: the fit flaw of the worst axes, summed and capped.
     * @param axes        axes judged; {@code inTolerance} of them within tolerance.
     * @param blueprint   a sheet was the yardstick (the tolerance was the blueprint's).
     * @param drawn       the sheet's design judged against, if any.
     */
    public record Result(boolean structural, boolean off, float flaw, int axes, int inTolerance, boolean blueprint, Optional<ModularDesign> drawn) {

        /** The share of judged axes outside tolerance, 0 when none were judged. */
        public float outShare() {
            return axes == 0 ? 0f : (axes - inTolerance) / (float) axes;
        }

        /** Nothing is off and nothing is flawed. */
        public boolean clean() {
            return structural && !off && flaw <= 0f;
        }
    }

    private static final Result NOTHING = new Result(false, false, 0f, 0, 0, false, Optional.empty());

    /** Judges {@code made} against the sheet {@code drawn}: materials by spec, axes by the blueprint tolerance. */
    public static Result against(ModularDesign drawn, ModularDesign made, ModularType type, Map<ResourceLocation, Material> materials) {
        if (!drawn.type().equals(made.type()))
            return NOTHING;
        if (drawn.parts().stream().filter(p -> p.sub().isEmpty()).count() != made.parts().stream().filter(p -> p.sub().isEmpty()).count())
            return NOTHING;
        boolean off = false;
        float flaw = 0f;
        int axes = 0, inside = 0;
        for (ModularDesign.Part want : drawn.parts()) {
            if (!want.sub().isEmpty())
                continue;
            ModularDesign.Part got = made.part(want.role());
            if (got == null || !got.sub().isEmpty() || !got.kind().equals(want.kind()) || got.count() != want.count())
                return NOTHING;
            if (!got.material().equals(want.material())) {
                Material dm = materials.get(want.material()), mm = materials.get(got.material());
                if (dm == null || mm == null || !Spec.of(dm).admits(mm))
                    off = true;
            }
            // A drawing pins each slot's material as it pins the part's: the slot made of another that the drawing's does not admit is off-drawing.
            for (Map.Entry<String, io.github.soundgoodizerfan.feedback.process.Measures.Slot> sl : want.measures().slots().entrySet()) {
                net.minecraft.resources.ResourceLocation have = got.measures().slot(sl.getKey()).map(io.github.soundgoodizerfan.feedback.process.Measures.Slot::material).orElse(got.material());
                Material dm = materials.get(sl.getValue().material()), mm = materials.get(have);
                if (!have.equals(sl.getValue().material()) && (dm == null || mm == null || !Spec.of(dm).admits(mm)))
                    off = true;
            }
            ModularType.Role role = type.roles().get(want.role());
            if (role == null)
                continue;
            for (Map.Entry<String, ModularType.Axis> a : role.axesFor(want).entrySet()) {
                float range = a.getValue().max() - a.getValue().min();
                if (range <= 0f)
                    continue;
                float target = role.axis(want, a.getKey()), built = role.axis(got, a.getKey());
                float over = Math.abs(built - target) / range - ModularTuning.FIT_TOLERANCE_BLUEPRINT;
                axes++;
                if (over <= 0f)
                    inside++;
                else
                    flaw += Math.min(1f, over / ModularTuning.FIT_FLAW_FULL_OVERSHOOT);
            }
        }
        return new Result(true, off, Math.min(1f, flaw), axes, inside, true, Optional.of(drawn));
    }

    /** Whether {@code made} still shows what {@code drawn} specifies, parts in spec: the test for a sheet's credit. */
    public static boolean drawnTo(ModularDesign drawn, ModularDesign made, ModularType type, Map<ResourceLocation, Material> materials) {
        Result r = against(drawn, made, type, materials);
        return r.structural() && !r.off();
    }

    /**
     * Judges {@code made} without a sheet: each axis that two or more roles share is compared between them at the hand tolerance
     * (a barrel's bore against the cylinder it feeds from). Never off-drawing.
     */
    public static Result handFit(ModularDesign made, ModularType type) {
        Map<String, List<Float>> shared = new LinkedHashMap<>();
        Map<String, Float> ranges = new LinkedHashMap<>();
        for (ModularDesign.Part p : made.parts()) {
            ModularType.Role role = type.roles().get(p.role());
            if (role == null || !p.sub().isEmpty())
                continue;
            for (Map.Entry<String, ModularType.Axis> a : role.axesFor(p).entrySet()) {
                shared.computeIfAbsent(a.getKey(), k -> new ArrayList<>()).add(role.axis(p, a.getKey()));
                ranges.putIfAbsent(a.getKey(), a.getValue().max() - a.getValue().min());
            }
        }
        float flaw = 0f;
        int axes = 0, inside = 0;
        for (Map.Entry<String, List<Float>> e : shared.entrySet()) {
            float range = ranges.get(e.getKey());
            List<Float> v = e.getValue();
            if (v.size() < 2 || range <= 0f)
                continue;
            float spread = (java.util.Collections.max(v) - java.util.Collections.min(v)) / range;
            float over = spread - ModularTuning.FIT_TOLERANCE_HAND;
            axes++;
            if (over <= 0f)
                inside++;
            else
                flaw += Math.min(1f, over / ModularTuning.FIT_FLAW_FULL_OVERSHOOT);
        }
        return new Result(true, false, Math.min(1f, flaw), axes, inside, false, Optional.empty());
    }

    /**
     * The best judgement of {@code made} among {@code sheets}: a sheet of its type it is structurally drawn to, clean ones first;
     * else the hand fit.
     */
    public static Result judge(List<ModularDesign> sheets, ModularDesign made, ModularType type, Map<ResourceLocation, Material> materials) {
        Result best = null;
        for (ModularDesign s : sheets) {
            Result r = against(s, made, type, materials);
            if (r.structural() && (best == null || (!r.off() && best.off())))
                best = r;
        }
        return best != null ? best : handFit(made, type);
    }

    // --- the figures on the item ----------------------------------------------------------------

    /**
     * The start condition of a finished item.
     *
     * @param fit  1 is the as-baked figure; lower is a looser joint (and a wider gun spread). Never above 1.
     * @param head 1 is a full durability bar at the start; lower starts the item worn. Never above 1.
     * @param off  the item was built off-drawing: a material short of the blueprint's spec.
     */
    public record Fit(float fit, float head, boolean off) {

        public static final Fit CLEAN = new Fit(1f, 1f, false);

        public static final Codec<Fit> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.FLOAT.fieldOf("fit").forGetter(Fit::fit),
                Codec.FLOAT.fieldOf("head").forGetter(Fit::head),
                Codec.BOOL.optionalFieldOf("off", false).forGetter(Fit::off)).apply(i, Fit::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, Fit> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.FLOAT, Fit::fit, ByteBufCodecs.FLOAT, Fit::head, ByteBufCodecs.BOOL, Fit::off, Fit::new);
    }

    /**
     * What the maker brought to the assembly beyond the parts.
     *
     * @param levels   fastener quality above the type's floor (negative when short of it); +5% fit and head start per level.
     * @param minigame the 0..1 score a minigame reported (the server clamps it), or a negative number for none.
     */
    public record Start(int levels, float minigame) {

        /** Hand assembly with no fasteners and no minigame: the neutral start. */
        public static final Start NONE = new Start(0, -1f);
    }

    /**
     * The figures an item built as {@code result} starts with. The fastener levels, a blueprint's presence (one level) and the minigame
     * score move both figures by the same step; the fit flaw lowers only the fit, the axes out of tolerance only the head start. Both are
     * capped at 1: a good start recovers a flaw, it never beats what physics allows.
     */
    public static Fit fit(Result result, Start start) {
        float delta = ModularTuning.START_PER_LEVEL * (start.levels() + (result.blueprint() ? ModularTuning.START_BLUEPRINT_LEVELS : 0));
        if (start.minigame() >= 0f)
            delta += (Math.min(1f, start.minigame()) - 0.5f) * ModularTuning.START_MINIGAME_SPAN;
        float fit = 1f - ModularTuning.FIT_JOINT_LOSS_CAP * result.flaw() + delta;
        float head = 1f - ModularTuning.HEAD_OUT_LOSS * result.outShare() + delta;
        return new Fit(Math.min(1f, Math.max(ModularTuning.FIT_MIN, fit)), Math.min(1f, Math.max(ModularTuning.HEAD_MIN, head)), result.off());
    }

    /** The fit {@code stack} carries, else clean. */
    public static float spread(ItemStack stack) {
        return (BlueprintCredit.of(stack) != null ? ModularTuning.FIT_TOLERANCE_BLUEPRINT : ModularTuning.FIT_TOLERANCE_HAND) / 2f;
    }

    /** The fit {@code stack} carries, or null if it has none. */
    public static Fit of(ItemStack stack) {
        return stack.getOrDefault(FModular.FIT.get(), Fit.CLEAN);
    }

    /**
     * {@code figures} as a fit of {@code fit} allows: the joint figure (load over capacity) rises as the joint's strength falls, and a
     * gun's spread widens.
     */
    public static ModularFigures adjust(ModularFigures figures, Fit fit) {
        if (fit.fit() >= 1f)
            return figures;
        Map<Fig, Float> m = new EnumMap<>(figures.map());
        m.computeIfPresent(Fig.JOINT, (k, v) -> v / fit.fit());
        m.computeIfPresent(Fig.SPREAD, (k, v) -> v * (1f + ModularTuning.FIT_SPREAD_PER_LOSS * (1f - fit.fit())));
        return ModularFigures.of(m, figures.step());
    }

    /** Sets {@code fit} on a freshly assembled {@code tool}, and starts a damageable one worn by the head start. Call before the first bake for the fit to count. */
    public static void start(ItemStack tool, Fit fit) {
        if (tool.isDamageableItem())
            tool.setDamageValue(Math.max(0, Math.min(tool.getMaxDamage() - 2, Math.round((1f - fit.head()) * tool.getMaxDamage()))));
    }
}
