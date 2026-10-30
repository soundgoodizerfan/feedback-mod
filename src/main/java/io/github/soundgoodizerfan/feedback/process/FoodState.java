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

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * A food stack's state ({@code docs/specs/feedback_food_spec.md} 4.1). Absent means fresh and raw by its
 * material. Stacks with different state do not merge, which is correct: they are not the same food.
 *
 * @param cook     0 to 1: how far through cooking. The pathogen it carries is the material's, less this.
 * @param moisture fraction of the item that is water; cooking and drying drive it off.
 * @param pathogen live-organism load left. Heat kills it; rot raises it.
 * @param toxin    stable-toxin load. Heat does not touch it; deep rot makes it.
 * @param charred  0 to 1: how burnt. 1 is charred.
 * @param rot      0 to 1: how far it has spoiled. Advanced on read from {@code stamp}, never ticked.
 * @param stamp    the game tick the state was last true on; 0 means never stamped, so no time has passed.
 * @param envTu    the temperature of its surroundings at the stamp, in Tu.
 * @param smoke    0 to 1: a smoke coat from a smoking vessel. Slows rot, and wears off with time (settled on read).
 * @param salt     0 to 1: how salted. Slows rot, raises hazard tolerance, and does not wear off.
 */
public record FoodState(float cook, float moisture, float pathogen, float toxin, float charred,
                        float rot, long stamp, float envTu, float smoke, float salt) {

    public static final Codec<FoodState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("cook").forGetter(FoodState::cook),
            Codec.FLOAT.fieldOf("moisture").forGetter(FoodState::moisture),
            Codec.FLOAT.fieldOf("pathogen").forGetter(FoodState::pathogen),
            Codec.FLOAT.fieldOf("toxin").forGetter(FoodState::toxin),
            Codec.FLOAT.fieldOf("charred").forGetter(FoodState::charred),
            Codec.FLOAT.optionalFieldOf("rot", 0f).forGetter(FoodState::rot),
            Codec.LONG.optionalFieldOf("stamp", 0L).forGetter(FoodState::stamp),
            Codec.FLOAT.optionalFieldOf("env", 20f).forGetter(FoodState::envTu),
            Codec.FLOAT.optionalFieldOf("smoke", 0f).forGetter(FoodState::smoke),
            Codec.FLOAT.optionalFieldOf("salt", 0f).forGetter(FoodState::salt)
    ).apply(instance, FoodState::new));

    // Ten fields are past StreamCodec.composite's six.
    public static final StreamCodec<RegistryFriendlyByteBuf, FoodState> STREAM_CODEC = StreamCodec.of(
            (buf, s) -> {
                buf.writeFloat(s.cook);
                buf.writeFloat(s.moisture);
                buf.writeFloat(s.pathogen);
                buf.writeFloat(s.toxin);
                buf.writeFloat(s.charred);
                buf.writeFloat(s.rot);
                buf.writeLong(s.stamp);
                buf.writeFloat(s.envTu);
                buf.writeFloat(s.smoke);
                buf.writeFloat(s.salt);
            },
            buf -> new FoodState(buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(),
                    buf.readFloat(), buf.readLong(), buf.readFloat(), buf.readFloat(), buf.readFloat()));
}
