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
 * One skill tree, from {@code data/<ns>/skill_tree/*.json}: {@code {"tree": id, "tab"?: pool, "nodes": [{id, cost,
 * requires, effect, branch?, exclusive?}]}}. A node's effect is either a figure modifier
 * ({@code {"figure": "parry_window", "amount": 2}}) or a move unlock ({@code {"unlock": "riposte"}}).
 * Unlocks gate the player's own combat moves only, never a machine or a recipe (plan 0.1's guard).
 * {@code branch} is display grouping (a weapon family name for the melee tree); weapons are matched by
 * baked family, never an item id.
 */
public record SkillTree(String tree, Optional<String> tab, List<Node> nodes) {

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
        REDIRECT_INACCURACY,
        /** Fraction added to the grapple's reel speed (0.2 is a fifth faster). */
        GRAPPLE_REEL,
        /** Ticks added to how long a {@code TIME} rearm takes (negative shortens it). */
        REARM_TICKS,
        /** Fraction added to the stagger a kick delivers into a wall (0.5 is half again). */
        KICK_WALL_BONUS,
        /** Ticks added to how long a grab holds. */
        GRAB_HOLD;

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

    /**
     * What spends a technique and what gives it back (spec 3.2). A technique that fires is spent until its rearm
     * happens: {@code TIME} a fixed number of ticks, {@code STRIKE} the player lands a melee hit, {@code PARRY}
     * the player completes a parry, {@code STAGGER} the player staggers a target.
     */
    public enum Rearm implements StringRepresentable {
        TIME, STRIKE, PARRY, STAGGER;

        static final Codec<Rearm> CODEC = StringRepresentable.fromEnum(Rearm::values);
        static final StreamCodec<ByteBuf, Rearm> STREAM = ByteBufCodecs.idMapper(i -> values()[i], Rearm::ordinal);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /**
     * {@code exclusive}: nodes of one tree sharing a tag cannot both be owned (stem variants); respec to swap.
     * {@code rearm}: the rule that rearms this technique; a node without one inherits its requirement's (the
     * stem's), so variants share the stem's clock unless they name their own.
     * {@code needsTrait}: a modular trait id the held weapon (or worn armour) must have for this skill to work. The skill
     * is still owned and slottable without it; it just does nothing in the wrong hands. Only a node may ask this, never a tree.
     */
    public record Node(String id, int cost, List<String> requires, Effect effect, Optional<String> branch,
            Optional<String> exclusive, Optional<Rearm> rearm, Optional<String> needsTrait) {
        static final Codec<Node> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("id").forGetter(Node::id),
                Codec.intRange(0, Integer.MAX_VALUE).fieldOf("cost").forGetter(Node::cost),
                Codec.STRING.listOf().optionalFieldOf("requires", List.of()).forGetter(Node::requires),
                Effect.CODEC.fieldOf("effect").forGetter(Node::effect),
                Codec.STRING.optionalFieldOf("branch").forGetter(Node::branch),
                Codec.STRING.optionalFieldOf("exclusive").forGetter(Node::exclusive),
                Rearm.CODEC.optionalFieldOf("rearm").forGetter(Node::rearm),
                Codec.STRING.optionalFieldOf("needs_trait").forGetter(Node::needsTrait)).apply(i, Node::new));
        // Eight fields: past StreamCodec.composite's six, so written out.
        static final StreamCodec<ByteBuf, Node> STREAM = StreamCodec.of(Node::write, Node::read);

        private static void write(ByteBuf buf, Node n) {
            ByteBufCodecs.STRING_UTF8.encode(buf, n.id);
            ByteBufCodecs.VAR_INT.encode(buf, n.cost);
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).encode(buf, n.requires);
            Effect.STREAM.encode(buf, n.effect);
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8).encode(buf, n.branch);
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8).encode(buf, n.exclusive);
            ByteBufCodecs.optional(Rearm.STREAM).encode(buf, n.rearm);
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8).encode(buf, n.needsTrait);
        }

        private static Node read(ByteBuf buf) {
            return new Node(ByteBufCodecs.STRING_UTF8.decode(buf), ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).decode(buf), Effect.STREAM.decode(buf),
                    ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8).decode(buf),
                    ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8).decode(buf),
                    ByteBufCodecs.optional(Rearm.STREAM).decode(buf),
                    ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8).decode(buf));
        }
    }

    public static final Codec<SkillTree> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("tree").forGetter(SkillTree::tree),
            Codec.STRING.optionalFieldOf("tab").forGetter(SkillTree::tab),
            Node.CODEC.listOf().fieldOf("nodes").forGetter(SkillTree::nodes)).apply(i, SkillTree::new));

    public static final StreamCodec<ByteBuf, SkillTree> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SkillTree::tree,
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8), SkillTree::tab,
            Node.STREAM.apply(ByteBufCodecs.list()), SkillTree::nodes,
            SkillTree::new);

    /** The rearm rule of {@code node}: its own, else the first requirement's that has one, else {@code TIME}. */
    public Rearm rearmOf(Node node) {
        if (node.rearm().isPresent())
            return node.rearm().get();
        for (String r : node.requires()) {
            Optional<Node> parent = node(r);
            if (parent.isPresent()) {
                Rearm up = rearmOf(parent.get());
                if (parent.get().rearm().isPresent() || up != Rearm.TIME)
                    return up;
            }
        }
        return Rearm.TIME;
    }

    /**
     * The tab this tree sits under, which is also the pool its points come from: a tab holds several trees, each its
     * own aspect, spending one shared pool. A tree with no {@code tab} is a tab of its own.
     */
    public String pool() {
        return tab.orElse(tree);
    }

    public Optional<Node> node(String id) {
        return nodes.stream().filter(n -> n.id().equals(id)).findFirst();
    }
}
