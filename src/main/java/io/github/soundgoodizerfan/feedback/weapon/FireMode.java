// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.weapon;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/** Runtime state of a gun (spec 2): what one press of the trigger does. */
public enum FireMode implements StringRepresentable {
    SEMI("semi"), BURST("burst"), AUTO("auto");

    public static final Codec<FireMode> CODEC = StringRepresentable.fromEnum(FireMode::values);
    public static final StreamCodec<ByteBuf, FireMode> STREAM_CODEC =
            ByteBufCodecs.idMapper(i -> values()[i], FireMode::ordinal);

    private final String id;

    FireMode(String id) {
        this.id = id;
    }

    public FireMode next() {
        return values()[(ordinal() + 1) % values().length];
    }

    @Override
    public String getSerializedName() {
        return id;
    }
}
