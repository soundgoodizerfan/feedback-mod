// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.weapon;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import net.minecraft.util.StringRepresentable;

import java.util.List;

/**
 * What kind of gun a design is and how it cycles: the vocabulary of the physics, read off a built gun
 * ({@link GunSpec}), never stored. The variant is the receiver's, the action is the action part's kind
 * and the feed is the feed part's, or the lack of one.
 * Nothing here carries figures: a rifle is not more accurate because of its name, it is accurate
 * because it is a long barrel on a bolt.
 */
public record GunLayout(Type type, Action action, Feed feed) {

    /** The four fundamentally different guns (spec 8.4): what the receiver is. */
    public enum Type implements StringRepresentable {
        HANDGUN("handgun"), LONG_GUN("long_gun"), MACHINE_GUN("machine_gun");

        private final String id;

        Type(String id) {
            this.id = id;
        }

        /** The variant called {@code name}; an unnamed receiver is a handgun. */
        public static Type of(String name) {
            for (Type t : values())
                if (t.id.equals(name))
                    return t;
            return HANDGUN;
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
        /** A flintlock: a hammer with a flint, a pan to prime and a muzzle-loaded round; slow to cock and prime again. */
        FLINTLOCK("flintlock", true, FTuning.GUN_CYCLE_MANUAL_MS_FLINTLOCK, List.of(FireMode.SEMI)),
        LEVER("lever", true, FTuning.GUN_CYCLE_MANUAL_MS_LEVER, List.of(FireMode.SEMI)),
        BOLT("bolt", false, FTuning.GUN_CYCLE_MANUAL_MS_BOLT, List.of(FireMode.SEMI)),
        PUMP("pump", true, FTuning.GUN_CYCLE_MANUAL_MS_PUMP, List.of(FireMode.SEMI)),
        /** A hammer cocked by thumb between shots (a revolver's). */
        SINGLE_ACTION("single_action", true, FTuning.GUN_CYCLE_MANUAL_MS_SINGLE_ACTION, List.of(FireMode.SEMI)),
        /** A hammer cocked by the trigger pull (a revolver's): nothing to work between shots. */
        DOUBLE_ACTION("double_action", true, 0, List.of(FireMode.SEMI)),
        SELF_LOADING("self_loading", false, 0, List.of(FireMode.SEMI)),
        AUTOMATIC("automatic", false, 0, List.of(FireMode.AUTO, FireMode.BURST, FireMode.SEMI));

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

        /** The action named {@code name} (an action part's variant); a part with none is a bolt action. */
        public static Action of(String name) {
            for (Action a : values())
                if (a.id.equals(name))
                    return a;
            return BOLT;
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
     * Where the rounds wait. Barrels, a cylinder, a tube and a lone chamber are in the gun and loaded a round at a
     * time; a box magazine and a belt are items, swapped whole, and their size is the item's.
     */
    public enum Feed implements StringRepresentable {
        /** A break action's barrels, one round in each. */
        BARRELS("barrels", true),
        /** A revolver's: any hammer action can take one, a top-break included. */
        CYLINDER("cylinder", true),
        /** Under the barrel and as long as it. */
        TUBE("tube", true),
        /** A bolt or lever with nothing under it: one round, put straight into the chamber. */
        SINGLE("single", true),
        BOX("box", false),
        BELT("belt", false);

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
                case SINGLE -> action == Action.BOLT || action == Action.LEVER || action == Action.FLINTLOCK;
                case BOX -> action != Action.BREAK && action != Action.SINGLE_ACTION && action != Action.DOUBLE_ACTION;
                case BELT -> action == Action.AUTOMATIC;
            };
        }

        @Override
        public String getSerializedName() {
            return id;
        }
    }
}
