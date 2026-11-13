// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.bench;

import io.github.soundgoodizerfan.feedback.modular.item.ModularEvents;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;

/**
 * How an edge was ground: a few named choices, never a slider (philosophy 3). Keen bites harder
 * and chips sooner; blunt is the reverse. Set at a grindstone, shift-using with the tool in
 * hand ({@link ModularEvents#onGrindstone}), because grinding an edge is something done to a
 * finished tool and takes metal off it, unlike a handle's length, which is the pattern it was put
 * together in.
 */
public enum Edge implements StringRepresentable {
    KEEN(1.15f, 1.5f),
    STANDARD(1f, 1f),
    BLUNT(0.85f, 0.5f);

    public static final Codec<Edge> CODEC = StringRepresentable.fromEnum(Edge::values);

    private final float bite;
    private final float chip;

    Edge(float bite, float chip) {
        this.bite = bite;
        this.chip = chip;
    }

    /** What the edge does to the material's own bite. */
    public float bite() {
        return bite;
    }

    /** How readily it chips on something harder than it can take. */
    public float chip() {
        return chip;
    }

    public Edge step(int direction) {
        Edge[] all = values();
        return all[Math.floorMod(ordinal() + direction, all.length)];
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
