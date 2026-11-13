// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.engineering;

import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.soundgoodizerfan.feedback.process.MaterialIngredient;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * Screws, bolts and rivets: forms of any metal ({@code form_type/screw|rivet|machine_bolt}), whose quality is one coarse figure read
 * from the material ({@code fastener}, 1 to 5; absent means the stuff makes no fastener). The Engineering Table spends them per
 * assembly; their quality above or below a recipe's floor moves the start condition ({@link io.github.soundgoodizerfan.feedback.modular.type.ModularFit.Start}).
 * The gun's {@code bolt} part is another form and not a fastener, so the fastener bolt is named {@code machine_bolt}.
 */
public final class Fasteners {

    /** The forms that are fasteners. */
    public static final Set<String> FORMS = Set.of("screw", "machine_bolt", "rivet");

    /**
     * What an assembly asks of its fasteners: how many are spent, the quality it is built for, and the forms that will do (empty
     * means any). Defaults: two, quality 1, any form.
     */
    public record Need(int count, int floor, java.util.List<String> forms) {

        public static final Need DEFAULT = new Need(2, 1);

        public static final Codec<Need> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.intRange(0, 64).optionalFieldOf("count", 2).forGetter(Need::count),
                Codec.intRange(1, 5).optionalFieldOf("floor", 1).forGetter(Need::floor),
                Codec.STRING.listOf().optionalFieldOf("forms", java.util.List.of()).forGetter(Need::forms)).apply(i, Need::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, Need> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Need::count, ByteBufCodecs.VAR_INT, Need::floor,
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), Need::forms, Need::new);

        public Need(int count, int floor) {
            this(count, floor, java.util.List.of());
        }

        /** Whether a fastener of {@code form} will do. */
        public boolean accepts(String form) {
            return forms.isEmpty() || forms.contains(form);
        }
    }

    private Fasteners() {
    }

    public static boolean isFastenerForm(String form) {
        return FORMS.contains(form);
    }

    /** The quality (1 to 5) of {@code stack} as a fastener, or 0 when it is not one (not a fastener form, or its material makes none). */
    public static int quality(ItemStack stack) {
        return quality(stack, DEFAULT_ANY);
    }

    private static final Need DEFAULT_ANY = new Need(0, 1);

    /** As {@link #quality(ItemStack)}, but 0 when the stack's form is not one {@code need} accepts. */
    public static int quality(ItemStack stack, Need need) {
        if (stack.isEmpty())
            return 0;
        return MaterialIngredient.locate(stack.getItem())
                .filter(l -> isFastenerForm(l.form()) && need.accepts(l.form()))
                .flatMap(l -> l.material().fastener())
                .orElse(0);
    }

    /** Whether {@code stack} can sit in a fastener slot. */
    public static boolean is(ItemStack stack) {
        return quality(stack) > 0;
    }

    /**
     * The fastener levels above the floor (negative when short) of the fasteners {@code stacks} supply for {@code need}: the count is
     * taken from the best stacks first, quality averaged (rounded down) over the fasteners spent. Too few fasteners is a
     * {@link Supply#spent} shortfall, never a gate: a missing one counts as quality 0.
     */
    public static Supply supply(java.util.List<ItemStack> stacks, Need need) {
        java.util.List<int[]> pool = new java.util.ArrayList<>(); // {quality, stack index}
        for (int i = 0; i < stacks.size(); i++) {
            int q = quality(stacks.get(i), need);
            if (q > 0)
                pool.add(new int[] {q, i});
        }
        pool.sort((a, b) -> b[0] - a[0]);
        int[] take = new int[stacks.size()];
        int left = need.count(), sum = 0, have = 0;
        for (int[] p : pool) {
            int n = Math.min(left, stacks.get(p[1]).getCount());
            take[p[1]] += n;
            sum += n * p[0];
            have += n;
            left -= n;
            if (left <= 0)
                break;
        }
        int q = need.count() == 0 ? need.floor() : sum / need.count();
        return new Supply(need.count() - left, need.count(), q - need.floor(), take);
    }

    /**
     * What a set of fastener slots gives one assembly.
     *
     * @param have   fasteners found (up to the count wanted).
     * @param wanted the count asked.
     * @param levels quality above the floor (negative when short); a missing fastener counts as quality 0.
     * @param take   how many to remove from each stack, by index into the list given.
     */
    public record Supply(int have, int wanted, int levels, int[] take) {

        /** Whether every wanted fastener was found. */
        public boolean complete() {
            return have >= wanted;
        }

        /** Removes {@link #take} from {@code stacks}. Server side, after the assembly is accepted. */
        public void spend(java.util.List<ItemStack> stacks) {
            for (int i = 0; i < take.length && i < stacks.size(); i++)
                if (take[i] > 0)
                    stacks.get(i).shrink(take[i]);
        }
    }
}
