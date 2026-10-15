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
package io.github.soundgoodizerfan.feedback.item;

import com.mojang.serialization.Codec;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.unit.St;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

/**
 * How hard a player swings a hand tool: a few named swings, never a slider (philosophy 3). Chosen by
 * sneak-scroll and kept on the stack, so the crafting grid's preview shows the blow that will land.
 *
 * <p>The swings make the hand hammer the crank's lesson in miniature (philosophy 17): a tap is
 * precision, a heavy blow is power, and on a soft metal the heavy blow overshoots. Lead wants a
 * tap; bronze wants a heavy blow and refuses anything less.
 *
 * <p><b>Minecraft lens.</b> A Strength effect is a real, measurable change to how hard a player
 * hits -- vanilla already adds it to every melee blow -- so it adds a swing above the heavy one,
 * and that swing is hard enough for iron. Nothing else lets a hand forge iron.
 */
public enum HandSwing {
    TAP(FTuning.HAND_SWING_TAP_ST, false),
    BLOW(FTuning.HAND_SWING_BLOW_ST, false),
    HEAVY(FTuning.HAND_SWING_HEAVY_ST, false),
    /** Only while the player has Strength; falls back to {@link #HEAVY} when it wears off. */
    MIGHTY(FTuning.HAND_SWING_MIGHTY_ST, true);

    public static final Codec<HandSwing> CODEC = Codec.INT.xmap(HandSwing::of, HandSwing::ordinal);
    public static final StreamCodec<ByteBuf, HandSwing> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(HandSwing::of, HandSwing::ordinal);

    private final St strength;
    private final boolean needsStrength;

    HandSwing(St strength, boolean needsStrength) {
        this.strength = strength;
        this.needsStrength = needsStrength;
    }

    public St strength() {
        return strength;
    }

    public static HandSwing of(int ordinal) {
        HandSwing[] all = values();
        return all[Math.min(Math.max(ordinal, 0), all.length - 1)];
    }

    /** One swing harder or softer, stopping at the ends and at what {@code player} can manage. */
    public HandSwing step(int direction, Player player) {
        return of(ordinal() + direction).within(player);
    }

    /** This swing, or the hardest one {@code player} can manage now. */
    public HandSwing within(Player player) {
        return needsStrength && !player.hasEffect(MobEffects.DAMAGE_BOOST) ? HEAVY : this;
    }
}
