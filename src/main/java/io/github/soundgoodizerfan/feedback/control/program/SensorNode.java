// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control.program;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.soundgoodizerfan.feedback.control.data.DataNodeRef;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import org.jetbrains.annotations.Nullable;

/**
 * A number source: names the {@link DataNodeRef} this card reads from, embedded directly rather
 * than looked up by index -- spec §5's "one card per linked sensor, not a dropdown, the wiring
 * <em>is</em> the reference." Resolved at evaluation time via {@code DataNode.resolve} and cast
 * to {@code SensorFitting}.
 * <p>
 * {@code target} is null on a freshly placed, not-yet-linked card -- a Programmer's "Link"
 * action (or placing the card while a selection is already pending) is what fills it in, along
 * with {@code quantity}: the {@link Quantity} the linked {@code SensorFitting} reads, resolved
 * once at link time and carried on the node so wiring can be checked without touching the world.
 * A number port is typed by this, one level more specific than spec §2.3's Number/Boolean --
 * {@link ProgramGraph#findError} still only checks {@code target}; the quantity match itself is
 * enforced by {@link ProgramGraph#wireError}, when a wire is drawn and again on every commit.
 */
public record SensorNode(int id, @Nullable DataNodeRef target, @Nullable Quantity quantity, int channel) implements ProgramNode {

    public static final String TYPE = "sensor";

    /**
     * Which of a target's readings this card takes, for a target with more than one -- a Driver's
     * Seat's keys, by GLFW key code. Zero for everything that reads one figure.
     */
    public SensorNode(int id, @Nullable DataNodeRef target, @Nullable Quantity quantity) {
        this(id, target, quantity, 0);
    }

    public SensorNode withChannel(int channel) {
        return new SensorNode(id, target, quantity, channel);
    }

    public static final MapCodec<SensorNode> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.fieldOf("id").forGetter(SensorNode::id),
            DataNodeRef.CODEC.optionalFieldOf("target").forGetter(n -> Optional.ofNullable(n.target())),
            Quantity.CODEC.optionalFieldOf("quantity").forGetter(n -> Optional.ofNullable(n.quantity())),
            Codec.INT.optionalFieldOf("channel", 0).forGetter(SensorNode::channel)
    ).apply(instance, (id, target, quantity, channel) -> new SensorNode(id, target.orElse(null), quantity.orElse(null), channel)));

    public static final StreamCodec<ByteBuf, SensorNode> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SensorNode::id,
            ByteBufCodecs.optional(DataNodeRef.STREAM_CODEC), n -> Optional.ofNullable(n.target()),
            ByteBufCodecs.optional(Quantity.STREAM_CODEC), n -> Optional.ofNullable(n.quantity()),
            ByteBufCodecs.VAR_INT, SensorNode::channel,
            (id, target, quantity, channel) -> new SensorNode(id, target.orElse(null), quantity.orElse(null), channel));

    @Override
    public String typeKey() {
        return TYPE;
    }
}
