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

import java.util.Locale;

import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

/**
 * How force is delivered to a workpiece -- the axis {@link Deformation} was missing as long as
 * every entry was a blow.
 *
 * <h2>Why this exists now and not from the start</h2>
 * {@code HandToolItem} used to say, correctly at the time, that there should be no registry of
 * which tools do which operations until an operation existed that was not deformation. Wire
 * drawing is still deformation -- same {@code work}/{@code hardness}/{@code result} shape, same
 * overshoot-cascades-forward rule -- but it is a different physical action on the same class of
 * material: a die pulls a rod thinner in tension, a hammer squashes a plate thinner in
 * compression. The same input item could plausibly answer to either with a different result, and
 * once that is possible {@link DeformationTable} needs a second key or two entries collide.
 *
 * <h2>Still not a registry</h2>
 * Each tool declares its own operation the same way it already declares its own {@code St} --
 * {@code HandToolItem#getOperation()}, one method per tool, no lookup table anywhere mapping
 * tool classes to behaviour.
 *
 * <h2>{@code REMOVE} was one value, and that was wrong</h2>
 * The removal family originally shipped as a single {@code REMOVE} value, on the reasoning that
 * turning, drilling and grinding all share one gate (tool hardness over workpiece hardness) and
 * so were "one physical family." That reasoning does not survive contact with its own sibling:
 * {@code BLOW}/{@code DRAW}/{@code ROLL} share that identical hardness-gated shape and still
 * needed splitting, for the same reason removal does -- a shared *gate* is not a shared *action*.
 * One {@code REMOVE} value meant a Drill Press could just as happily "turn" a rod as drill it
 * (nothing distinguished which machine a {@code Removal} entry was actually for), and a recipe
 * browser had no way to say which of the three a card even needed, which is the concrete bug that
 * forced this correction. Split into {@code TURN}/{@code DRILL}/{@code GRIND} below, mirroring how
 * {@code BLOW}/{@code DRAW}/{@code ROLL} already work: each names the physical action, not the
 * block, and a machine still never learns what it is making -- only which action it performs.
 */
public enum Operation implements StringRepresentable {
    /** Compressive impact -- a hammer, or a press's single large stroke. */
    BLOW,
    /** Tensile pull through a die -- a draw plate or a wire drawer. */
    DRAW,
    /**
     * Continuous compression between a driven roller pair -- a rolling mill.
     * <p>
     * Not {@code BLOW}: an ingot rolled into rod and an ingot hammered into a plate are the same
     * material meeting two different physical actions, and giving them one operation would mean
     * one of the two entries silently shadowing the other in {@link DeformationTable#find} the
     * moment both existed for the same input item -- the exact collision this enum was created to
     * avoid in the first place (see the class doc). Rolling produces an elongated, round-ish
     * cross-section a flat blow cannot; that is a real physical difference, not a rules technicality.
     */
    ROLL,
    /** Workpiece spun against a fixed cutting edge -- a lathe. */
    TURN,
    /** Cutting edge spun and fed axially into a stationary workpiece -- a drill press. */
    DRILL,
    /** Abrasive wheel spun against the workpiece -- a grinding wheel. */
    GRIND;

    /** By lowercase name. An unknown name is a parse error on that entry, not an exception. */
    public static final Codec<Operation> CODEC = StringRepresentable.fromEnum(Operation::values);

    /** By ordinal -- fine on the wire, where both ends run the same build; never persisted. */
    public static final StreamCodec<ByteBuf, Operation> STREAM_CODEC = ByteBufCodecs.idMapper(
            ByIdMap.continuous(Operation::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO), Operation::ordinal);

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** {@code feedback.operation.turn} and friends, for the JEI card and machine tooltips. */
    /** The removal family compiles to a {@link Removal}; everything else to a {@link Deformation}. */
    public boolean removesMaterial() {
        return this == TURN || this == DRILL || this == GRIND;
    }

    public String translationKey() {
        return "feedback.operation." + name().toLowerCase(Locale.ROOT);
    }
}
