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
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

import java.util.List;

/**
 * What kind of gun a design is, and how it cycles ({@code feedback_guns_spec.md} section 12):
 * the discrete architecture, apart from the dimensions and metals the figures are read from.
 *
 * <p>The type is the first choice and decides which actions are offered -- SilentGear's
 * {@code GearType} deciding a gear item's part slots (MIT, read for design only). What a type
 * does <em>not</em> do is carry figures: a marksman rifle is not more accurate because of its
 * name, it is accurate because it is a long barrel on a bolt.
 */
public record GunLayout(Type type, Action action, Feed feed) {

    /** The four fundamental guns, each with the actions that make sense of it. */
    public enum Type implements StringRepresentable {
        PISTOL("pistol", List.of(Action.SELF_LOADING, Action.AUTOMATIC, Action.BREAK),
                List.of(Feed.BOX, Feed.CYLINDER, Feed.BARRELS)),
        MARKSMAN_RIFLE("marksman_rifle", List.of(Action.BOLT, Action.LEVER, Action.SELF_LOADING, Action.BREAK),
                List.of(Feed.BOX, Feed.SINGLE, Feed.TUBE, Feed.CYLINDER, Feed.BARRELS)),
        // A shotgun takes no magazine (spec 12): its rounds live in the gun.
        SHOTGUN("shotgun", List.of(Action.PUMP, Action.BREAK, Action.LEVER, Action.SELF_LOADING),
                List.of(Feed.TUBE, Feed.BARRELS, Feed.CYLINDER)),
        LMG("lmg", List.of(Action.AUTOMATIC), List.of(Feed.BELT, Feed.BOX));

        public static final Codec<Type> CODEC = StringRepresentable.fromEnum(Type::values);
        public static final StreamCodec<ByteBuf, Type> STREAM_CODEC = ByteBufCodecs.idMapper(i -> values()[i], Type::ordinal);

        private final String id;
        private final List<Action> actions;
        private final List<Feed> feeds;

        Type(String id, List<Action> actions, List<Feed> feeds) {
            this.id = id;
            this.actions = actions;
            this.feeds = feeds;
        }

        /** The feeds this type offers with {@code action}, its usual one first. Never empty. */
        public List<Feed> feeds(Action action) {
            return feeds.stream().filter(f -> f.fits(action)).toList();
        }

        /** The actions this type offers, its usual one first. */
        public List<Action> actions() {
            return actions;
        }

        @Override
        public String getSerializedName() {
            return id;
        }
    }

    /**
     * How the next round gets into the chamber. A manual action is worked by hand between shots,
     * which is time on every cycle; a self-loading one cycles off the shot; only an automatic
     * keeps firing while the trigger is held. A hammer action can take a revolver cylinder.
     */
    public enum Action implements StringRepresentable {
        BREAK("break", true, FTuning.GUN_CYCLE_MANUAL_MS_BREAK, List.of(FireMode.SEMI)),
        LEVER("lever", true, FTuning.GUN_CYCLE_MANUAL_MS_LEVER, List.of(FireMode.SEMI)),
        BOLT("bolt", false, FTuning.GUN_CYCLE_MANUAL_MS_BOLT, List.of(FireMode.SEMI)),
        PUMP("pump", true, FTuning.GUN_CYCLE_MANUAL_MS_PUMP, List.of(FireMode.SEMI)),
        SELF_LOADING("self_loading", true, 0, List.of(FireMode.SEMI)),
        AUTOMATIC("automatic", false, 0, List.of(FireMode.AUTO, FireMode.BURST, FireMode.SEMI));

        public static final Codec<Action> CODEC = StringRepresentable.fromEnum(Action::values);
        public static final StreamCodec<ByteBuf, Action> STREAM_CODEC = ByteBufCodecs.idMapper(i -> values()[i], Action::ordinal);

        private final String id;
        private final boolean hammer;
        private final int manualMs;
        private final List<FireMode> modes;

        Action(String id, boolean hammer, int manualMs, List<FireMode> modes) {
            this.id = id;
            this.hammer = hammer;
            this.manualMs = manualMs;
            this.modes = modes;
        }

        /** Whether a hammer strikes the primer: a hammer action may take a revolver cylinder. */
        public boolean hammer() {
            return hammer;
        }

        /** Time a hand spends working the action between shots, in ms; 0 for one that cycles itself. */
        public int manualMs() {
            return manualMs;
        }

        /** What a trigger press can do on this action, the first being what it does unless switched. */
        public List<FireMode> modes() {
            return modes;
        }

        /** {@code mode} if this action can fire it, else the action's own first mode. */
        public FireMode allow(FireMode mode) {
            return modes.contains(mode) ? mode : modes.get(0);
        }

        /** The mode after {@code mode} among the ones this action has. */
        public FireMode next(FireMode mode) {
            return modes.get((modes.indexOf(allow(mode)) + 1) % modes.size());
        }

        @Override
        public String getSerializedName() {
            return id;
        }
    }

    /**
     * Where the rounds wait. Barrels, a cylinder and a tube are in the gun and loaded a round at a
     * time; a box magazine and a belt are items, swapped whole, and their size is the item's.
     */
    public enum Feed implements StringRepresentable {
        /** A break action's barrels, one round in each. */
        BARRELS("barrels", true),
        /** A revolver's: any hammer action can take one, a top-break included. */
        CYLINDER("cylinder", true),
        /** Under the barrel and as long as it. */
        TUBE("tube", true),
        BOX("box", false),
        BELT("belt", false),
        /** No magazine at all: one round, put straight into the chamber, every shot. Appended, never inserted. */
        SINGLE("single", true);

        public static final Codec<Feed> CODEC = StringRepresentable.fromEnum(Feed::values);
        public static final StreamCodec<ByteBuf, Feed> STREAM_CODEC = ByteBufCodecs.idMapper(i -> values()[i], Feed::ordinal);

        private final String id;
        private final boolean internal;

        Feed(String id, boolean internal) {
            this.id = id;
            this.internal = internal;
        }

        /** Whether the rounds live in the gun rather than in a magazine item. */
        public boolean internal() {
            return internal;
        }

        /** Whether this feed works with {@code action}: the mechanism, not a list of guns. */
        public boolean fits(Action action) {
            return switch (this) {
                case BARRELS -> action == Action.BREAK;
                case CYLINDER -> action.hammer();
                case TUBE -> action == Action.LEVER || action == Action.PUMP || action == Action.SELF_LOADING;
                case BOX -> action != Action.BREAK;
                case BELT -> action == Action.AUTOMATIC;
                // Worked by hand and not a break action, which already loads its barrels one by one.
                case SINGLE -> action == Action.BOLT || action == Action.LEVER;
            };
        }

        @Override
        public String getSerializedName() {
            return id;
        }
    }

    /** A self-loading pistol off a box magazine: the gun every design was before types existed. */
    public static final GunLayout DEFAULT = new GunLayout(Type.PISTOL, Action.SELF_LOADING, Feed.BOX);

    public static final Codec<GunLayout> CODEC = RecordCodecBuilder.create(i -> i.group(
            Type.CODEC.fieldOf("type").forGetter(GunLayout::type),
            Action.CODEC.fieldOf("action").forGetter(GunLayout::action),
            Feed.CODEC.optionalFieldOf("feed", Feed.BOX).forGetter(GunLayout::feed)
    ).apply(i, GunLayout::new));

    public static final StreamCodec<ByteBuf, GunLayout> STREAM_CODEC = StreamCodec.composite(
            Type.STREAM_CODEC, GunLayout::type,
            Action.STREAM_CODEC, GunLayout::action,
            Feed.STREAM_CODEC, GunLayout::feed,
            GunLayout::new);

    /** An action the type does not offer becomes its usual one, and likewise a feed. */
    public GunLayout {
        if (!type.actions().contains(action))
            action = type.actions().get(0);
        if (!type.feeds(action).contains(feed))
            feed = type.feeds(action).get(0);
    }

    public GunLayout withType(Type t) {
        return new GunLayout(t, action, feed);
    }

    public GunLayout withAction(Action a) {
        return new GunLayout(type, a, feed);
    }

    public GunLayout withFeed(Feed f) {
        return new GunLayout(type, action, f);
    }
}
