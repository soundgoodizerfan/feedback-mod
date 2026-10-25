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
package io.github.soundgoodizerfan.feedback.combat.gear;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.soundgoodizerfan.feedback.process.Material;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/**
 * What a piece of gear is made of and what that made of it, baked once at the crafting table and
 * kept on the stack (the {@code ModularFigures} pattern, leaner: a gear piece has two parts and no
 * wear). The figures are zero where the kind does not use them. The materials ride along so the
 * tooltip and the tint can name them; nothing re-derives from them at runtime.
 *
 * @param window        ticks added to the parry window (bracer)
 * @param joint         fraction of the parrying weapon's joint rating added (bracer)
 * @param rollDistance  fraction added to the roll's speed (sash)
 * @param recoveryShave ticks off the roll's recovery (sash)
 * @param iFrames       ticks added to the roll's invulnerability (sash)
 * @param mass          Mu added to the wearer's mass for stagger (back piece)
 * @param fist          damage added to a barehanded swing, never to a weapon's (gauntlet)
 */
public record GearStats(ResourceLocation shell, ResourceLocation strap, float window, float joint,
                        float rollDistance, float recoveryShave, float iFrames, float mass, float fist) {

    public static final Codec<GearStats> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("shell").forGetter(GearStats::shell),
            ResourceLocation.CODEC.fieldOf("strap").forGetter(GearStats::strap),
            Codec.FLOAT.fieldOf("window").forGetter(GearStats::window),
            Codec.FLOAT.fieldOf("joint").forGetter(GearStats::joint),
            Codec.FLOAT.fieldOf("roll_distance").forGetter(GearStats::rollDistance),
            Codec.FLOAT.fieldOf("recovery_shave").forGetter(GearStats::recoveryShave),
            Codec.FLOAT.fieldOf("i_frames").forGetter(GearStats::iFrames),
            Codec.FLOAT.fieldOf("mass").forGetter(GearStats::mass),
            Codec.FLOAT.fieldOf("fist").forGetter(GearStats::fist)
    ).apply(i, GearStats::new));

    /** By hand: {@code StreamCodec.composite} stops at six fields. */
    public static final StreamCodec<RegistryFriendlyByteBuf, GearStats> STREAM_CODEC = StreamCodec.of(
            (buf, g) -> {
                ResourceLocation.STREAM_CODEC.encode(buf, g.shell);
                ResourceLocation.STREAM_CODEC.encode(buf, g.strap);
                buf.writeFloat(g.window);
                buf.writeFloat(g.joint);
                buf.writeFloat(g.rollDistance);
                buf.writeFloat(g.recoveryShave);
                buf.writeFloat(g.iFrames);
                buf.writeFloat(g.mass);
                buf.writeFloat(g.fist);
            },
            buf -> new GearStats(ResourceLocation.STREAM_CODEC.decode(buf), ResourceLocation.STREAM_CODEC.decode(buf),
                    buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat()));

    /**
     * The bake: a pure function of the kind and the two materials, as {@code ModularBake} is of a
     * design. {@code shell} is the plate stock (hardness, density), {@code strap} the leather-like
     * stock (toughness), {@code plates} how many plates went in.
     */
    public static GearStats bake(GearKind kind, ResourceLocation shellId, Material shell,
                                 ResourceLocation strapId, Material strap) {
        float hard = GearTuning.saturate(shell.hardness().value(), GearTuning.HARDNESS_HALF);
        float shellTough = GearTuning.saturate(shell.mechanical().toughness().map(t -> t.value()).orElse(0f), GearTuning.TOUGHNESS_HALF);
        float strapTough = GearTuning.saturate(strap.mechanical().toughness().map(t -> t.value()).orElse(0f), GearTuning.TOUGHNESS_HALF);
        float density = shell.density().muPerMb();
        return switch (kind) {
            case BRACER -> new GearStats(shellId, strapId, GearTuning.BRACER_WINDOW * hard,
                    GearTuning.BRACER_JOINT * Math.max(shellTough, hard * 0.5f), 0, 0, 0, 0, 0);
            case SASH -> new GearStats(shellId, strapId, 0, 0,
                    GearTuning.SASH_DISTANCE * (1f - GearTuning.saturate(density, GearTuning.DENSITY_HALF)),
                    GearTuning.SASH_RECOVERY * strapTough, GearTuning.SASH_I_FRAMES * hard, 0, 0);
            case BACK -> new GearStats(shellId, strapId, 0, 0, 0, 0, 0,
                    GearTuning.BACK_MU_PER_DENSITY * density * kind.plates(), 0);
            // Heavy and hard both hit harder; a light hard plate or a heavy soft one is half as good.
            case GAUNTLET -> new GearStats(shellId, strapId, 0, 0, 0, 0, 0, 0,
                    GearTuning.GAUNTLET_FIST * (hard + GearTuning.saturate(density, GearTuning.DENSITY_HALF)) / 2f);
        };
    }
}
