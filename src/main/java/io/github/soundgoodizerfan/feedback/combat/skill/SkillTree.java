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
package io.github.soundgoodizerfan.feedback.combat.skill;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/**
 * One skill tree, from {@code data/<ns>/skill_tree/*.json}: {@code {"tree": id, "nodes": [{id, cost,
 * requires, effect, branch?}]}}. A node's effect is either a figure modifier
 * ({@code {"figure": "parry_window", "amount": 2}}) or a move unlock ({@code {"unlock": "riposte"}}).
 * Unlocks gate the player's own combat moves only, never a machine or a recipe (plan 0.1's guard).
 * {@code branch} is display grouping (a weapon family name for the melee tree); weapons are matched by
 * baked family, never an item id.
 */
public record SkillTree(String tree, List<Node> nodes) {

    /** The figures a node may nudge, additively. Units in the comments. */
    public enum Figure implements StringRepresentable {
        /** Ticks added to the parry's active window. */
        PARRY_WINDOW,
        /** Ticks added to the parry's recovery lockout (negative shortens it). */
        PARRY_RECOVERY,
        /** Blocks added to the roll's slide. */
        ROLL_DISTANCE,
        /** Ticks added to the roll's invulnerable window. */
        I_FRAMES,
        /** Fraction added to the stagger a parry delivers (0.25 is a quarter more). */
        STAGGER_POWER,
        /** Degrees added to a redirected projectile's inaccuracy (negative tightens it). */
        REDIRECT_INACCURACY;

        static final Codec<Figure> CODEC = StringRepresentable.fromEnum(Figure::values);
        static final StreamCodec<ByteBuf, Figure> STREAM = ByteBufCodecs.idMapper(i -> values()[i], Figure::ordinal);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public record Effect(Optional<Figure> figure, float amount, Optional<String> unlock) {
        static final Codec<Effect> CODEC = RecordCodecBuilder.<Effect>create(i -> i.group(
                Figure.CODEC.optionalFieldOf("figure").forGetter(Effect::figure),
                Codec.FLOAT.optionalFieldOf("amount", 0f).forGetter(Effect::amount),
                Codec.STRING.optionalFieldOf("unlock").forGetter(Effect::unlock)).apply(i, Effect::new))
                .validate(e -> e.figure.isPresent() != e.unlock.isPresent() ? DataResult.success(e)
                        : DataResult.error(() -> "effect needs exactly one of figure or unlock"));
        static final StreamCodec<ByteBuf, Effect> STREAM = StreamCodec.composite(
                ByteBufCodecs.optional(Figure.STREAM), Effect::figure,
                ByteBufCodecs.FLOAT, Effect::amount,
                ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8), Effect::unlock,
                Effect::new);
    }

    public record Node(String id, int cost, List<String> requires, Effect effect, Optional<String> branch) {
        static final Codec<Node> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("id").forGetter(Node::id),
                Codec.intRange(0, Integer.MAX_VALUE).fieldOf("cost").forGetter(Node::cost),
                Codec.STRING.listOf().optionalFieldOf("requires", List.of()).forGetter(Node::requires),
                Effect.CODEC.fieldOf("effect").forGetter(Node::effect),
                Codec.STRING.optionalFieldOf("branch").forGetter(Node::branch)).apply(i, Node::new));
        static final StreamCodec<ByteBuf, Node> STREAM = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Node::id,
                ByteBufCodecs.VAR_INT, Node::cost,
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), Node::requires,
                Effect.STREAM, Node::effect,
                ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8), Node::branch,
                Node::new);
    }

    public static final Codec<SkillTree> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("tree").forGetter(SkillTree::tree),
            Node.CODEC.listOf().fieldOf("nodes").forGetter(SkillTree::nodes)).apply(i, SkillTree::new));

    public static final StreamCodec<ByteBuf, SkillTree> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SkillTree::tree,
            Node.STREAM.apply(ByteBufCodecs.list()), SkillTree::nodes,
            SkillTree::new);

    public Optional<Node> node(String id) {
        return nodes.stream().filter(n -> n.id().equals(id)).findFirst();
    }
}
