// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.process;

import java.util.Map;
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

/**
 * The ore ladder: how much of a bearer's metal each stage gets out, and the one rule every ore separator shares.
 *
 * <p>An ore item holds {@link #ORE_CONTENT} ingots of its bearing share ({@code bears}); a stage has a recovery, the share of
 * that content that ends up as metal. The form of the item says which stage it is at, so recovery is not a counter on the stack:
 * {@code ThermalProcessTable.bearerMelts} reads {@link #recovery} for the form it melts, and a leach row's output is authored at
 * {@link #LEACH}.
 *
 * <h2>Separation</h2>
 * A separator takes a {@code crushed} or {@code dust} stack of a bearer and, per unit, either reports it to the concentrate
 * (a {@code concentrate} and a {@code tailings}) or leaves it where it was, to go round again. The chance is the contrast between the
 * bearer and its {@code separation.gangue} in the property the machine reads: density in water for the mixer drum, magnetism
 * for the electromagnet, reflectivity for the optical sorter. Nothing is lost on a miss, so a weak lever costs passes, not metal.
 * Crushed grains are still part locked in rock ({@link #LIBERATION}); ground dust is fully liberated.
 *
 * <p>Nothing here names an ore: the figures all come from material files.
 */
public final class OreLadder {

    /** Ingots of bearing metal in one run-of-mine ore item, times its {@code bears} share. Invented. */
    public static final float ORE_CONTENT = 3f;
    /** Recovery of melting the raw lump (the old figure: one {@code bears} share of an ingot). */
    public static final float MELT = 1f / ORE_CONTENT;
    /** Recovery of melting crushed or ground ore. Invented. */
    public static final float CRUSHED = 0.45f;
    /** Recovery of melting a separated concentrate. Invented. */
    public static final float CONCENTRATE = 0.67f;
    /** Recovery of a leach and win: authored into the leach rows' liquor amounts. Invented. */
    public static final float LEACH = 0.95f;
    /** Recovery of melting tailings, the metal separation could not report to the concentrate. Invented. */
    public static final float TAILINGS_MELT = 0.10f;
    /** Recovery of leaching tailings. Invented. */
    public static final float TAILINGS_LEACH = 0.35f;

    /** Share of a crushed grain that is freed from the rock (dust is 1). Invented. */
    public static final float LIBERATION = 0.6f;
    /** Gravity contrast {@code |p1 - p2| / (p1 + p2)} at which the wash reports everything. Invented. */
    public static final float GRAVITY_FULL = 0.5f;
    /** Reflectivity difference at which the optical sorter reports everything. Invented. */
    public static final float OPTICAL_FULL = 0.4f;
    /** Share a magnet reports when the bearer is magnetic and its gangue is not. Invented. */
    public static final float MAGNETIC_SHARE = 0.9f;
    /** Reflectivity of a gangue that gives none, and density when it gives none. Invented. */
    private static final float DEFAULT_REFLECTIVITY = 0.35f;

    /** Water a mixer drum holds as wash medium, mB; ticks between one unit's wash and the next. Invented. */
    public static final int WASH_WATER_MB = 1000, WASH_INTERVAL_TICKS = 10;

    /** The forms a native-metal ore (one that bears itself) gets from the ladder; its raw rock keeps its own melts. */
    public static final java.util.Set<String> LADDER_FORMS = java.util.Set.of("crushed", "concentrate", "tailings");

    public static final ResourceLocation DEFAULT_GANGUE = Feedback.id("stone");

    private OreLadder() {
    }

    /** The property a separator reads. */
    public enum Mode {
        GRAVITY, MAGNETIC, OPTICAL;

        public String key() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /** What a unit becomes when it reports to the concentrate: one of each. */
    public record Split(ItemStack concentrate, ItemStack tailings) {
    }

    /** Recovery of melting an item of {@code form}: the stage it sits at on the ladder. */
    public static float recovery(String form) {
        return switch (form) {
            case "crushed", "dust" -> CRUSHED;
            case "concentrate", "calcine" -> CONCENTRATE;
            case "tailings" -> TAILINGS_MELT;
            default -> MELT;
        };
    }

    /** What dissolving a piece in an acid cell pays: a leach of a concentrate or calcine, a poorer one of tailings, else the melt's own rung. */
    public static float dissolve(Material.Located piece) {
        String form = piece.form();
        // A pure metal's own dust is not ground ore: it pays its one ingot's worth like the metal.
        if (form.equals("dust") && piece.material().bears().containsKey(piece.id()))
            return MELT;
        return switch (form) {
            case "concentrate", "calcine" -> LEACH;
            case "tailings" -> TAILINGS_LEACH;
            default -> recovery(form);
        };
    }

    /**
     * The slag a melt of a rung leaves: the rock the metal was locked in, still holding the metal the melt did not free. The raw rock and
     * crushed ore carry their gangue into the melt, and it comes out as that ore's own tailings (lean in its metal, so it leaches or
     * re-melts at the tailings rung); an ore with no tailings form leaves silicate slag. A concentrate has had its gangue stripped and
     * tailings are mostly rock already, so those leave none.
     */
    public static ItemStack slag(float recovery, java.util.Map<String, net.minecraft.world.item.Item> forms) {
        if (recovery != MELT && recovery != CRUSHED)
            return ItemStack.EMPTY;
        var tailings = forms.get("tailings");
        return new ItemStack(tailings != null ? tailings : io.github.soundgoodizerfan.feedback.registry.FItems.SILICATE_SLAG.get());
    }

    /** Whether a form is one a separator takes. */
    public static boolean isFeed(String form) {
        return form.equals("crushed") || form.equals("dust");
    }

    /** The gangue material of a bearer: its own named host rock, else stone. */
    public static Optional<Material> gangueOf(Material bearer, Map<ResourceLocation, Material> materials) {
        return Optional.ofNullable(materials.get(bearer.separation().gangue().orElse(DEFAULT_GANGUE)));
    }

    /** Gravity, magnetic or optical contrast of the bearer against its gangue, 0 to 1 of the mode's full scale. */
    public static float contrast(Mode mode, Material bearer, Material gangue) {
        return switch (mode) {
            case GRAVITY -> {
                float a = bearer.density().muPerMb(), b = gangue.density().muPerMb();
                yield a + b <= 0f ? 0f : Math.min(1f, Math.abs(a - b) / (a + b) / GRAVITY_FULL);
            }
            case MAGNETIC -> bearer.separation().magnetic() && !gangue.separation().magnetic() ? MAGNETIC_SHARE : 0f;
            case OPTICAL -> Math.min(1f, Math.abs(bearer.separation().reflectivity().orElse(DEFAULT_REFLECTIVITY)
                    - gangue.separation().reflectivity().orElse(DEFAULT_REFLECTIVITY)) / OPTICAL_FULL);
        };
    }

    /** The chance one unit of {@code form} reports to the concentrate under {@code mode}. */
    public static float share(Mode mode, Material bearer, Material gangue, String form) {
        return contrast(mode, bearer, gangue) * (form.equals("crushed") ? LIBERATION : 1f);
    }

    private static final Map<net.minecraft.world.item.Item, Optional<Material.Located>> FEED_MEMO = new java.util.concurrent.ConcurrentHashMap<>();
    private static volatile int memoGeneration = -1;

    /**
     * What a stack is as separator feed: a bearer's crushed or dust, with the material and form; empty for anything else. Memoised per
     * item (a belt, a magnet and a drum all ask every pass) and dropped when the material table reloads; a client with no server table
     * is not memoised.
     */
    public static Optional<Material.Located> feed(ItemStack stack) {
        if (stack.isEmpty())
            return Optional.empty();
        int generation = MaterialTable.generation();
        if (generation == 0)
            return lookup(stack);
        if (generation != memoGeneration) {
            FEED_MEMO.clear();
            memoGeneration = generation;
        }
        return FEED_MEMO.computeIfAbsent(stack.getItem(), item -> lookup(stack));
    }

    private static Optional<Material.Located> lookup(ItemStack stack) {
        return Material.find(MaterialIngredient.materialsNow(), stack.getItem())
                .filter(l -> isFeed(l.form()) && l.material().item(l.id(), "concentrate").isPresent()
                        // A pure metal's own dust (atomised) is not ore: it bears itself and has nothing to separate.
                        && !(l.form().equals("dust") && l.material().bears().containsKey(l.id())));
    }

    /**
     * Tries one unit of {@code feed} on a separator reading {@code mode}. A split is the unit's concentrate and tailings (the caller
     * takes the unit); empty means it stays where it was.
     */
    public static Optional<Split> attempt(ItemStack feed, Mode mode, RandomSource random) {
        var located = feed(feed);
        if (located.isEmpty())
            return Optional.empty();
        var materials = MaterialIngredient.materialsNow();
        var m = located.get().material();
        var id = located.get().id();
        var gangue = gangueOf(m, materials);
        if (gangue.isEmpty() || random.nextFloat() >= share(mode, m, gangue.get(), located.get().form()))
            return Optional.empty();
        var concentrate = m.stack(id, "concentrate");
        var tailings = m.stack(id, "tailings");
        return concentrate.isEmpty() || tailings.isEmpty() ? Optional.empty() : Optional.of(new Split(concentrate.get(), tailings.get()));
    }
}
