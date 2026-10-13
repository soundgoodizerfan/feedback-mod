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
 * Assets under src/main/resources are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
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
