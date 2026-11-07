// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.vehicle.frame;

import net.minecraft.world.phys.Vec3;
import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

/**
 * A joint between a vertex of this frame and a vertex of another frame
 * ({@code docs/specs/feedback_vehicles_spec.md} section 4a). One frame is one body; joints are how bodies make
 * a vehicle. Stored on the frame that was welded <em>from</em>, made real as a Sable constraint by
 * {@link FrameBlockEntity} once both frames are bodies.
 *
 * @param other  the other frame's {@link FrameBlockEntity#id()}
 * @param mine   vertex on this frame
 * @param theirs vertex on the other frame
 * @param kind   what the joint lets move
 * @param axis   a hinge's axis, unit length, in this frame's own space; unused otherwise. Along
 *               the tube when the bearing sits on one, so a drive shaft turns about itself
 * @param drives whether a bearing carries rotation across it (a shaft through it) or spins free.
 *               Recorded now, acted on once the drivetrain exists (spec section 4a)
 */
public record FrameJoint(UUID other, int mine, int theirs, Kind kind, Vec3 axis, boolean drives) {

    public enum Kind {
        /** Nothing moves: the two frames are one piece until the weld lets go. */
        WELD,
        /** A bearing set rigid: holds like a weld, but can be switched back to a pivot. */
        LOCKED,
        /** A bearing that turns about {@link #axis} only -- locked one way, free the other. */
        HINGE,
        /** A bearing free to turn every way about its point: a ball joint. */
        BALL,
        /**
         * A bearing that slides along {@link #axis} only, held at rest by a spring and a shock
         * (spec section 5): the running gear's spring. Set on a point inside a frame; never made
         * between two frames.
         */
        SLIDER;

        public boolean bearing() {
            return this != WELD;
        }

        /** The next pivot setting a bearing cycles to. */
        public Kind nextPivot() {
            return switch (this) {
                case HINGE -> BALL;
                case BALL -> LOCKED;
                default -> HINGE;
            };
        }
    }

    public FrameJoint withKind(Kind kind, Vec3 axis) {
        return new FrameJoint(other, mine, theirs, kind, axis, drives);
    }

    public FrameJoint toggleDrive() {
        return new FrameJoint(other, mine, theirs, kind, axis, !drives);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Other", other);
        tag.putInt("Mine", mine);
        tag.putInt("Theirs", theirs);
        tag.putString("Kind", kind.name());
        tag.putDouble("AxisX", axis.x);
        tag.putDouble("AxisY", axis.y);
        tag.putDouble("AxisZ", axis.z);
        tag.putBoolean("Drives", drives);
        return tag;
    }

    public static FrameJoint load(CompoundTag tag) {
        Vec3 axis = new Vec3(tag.getDouble("AxisX"), tag.getDouble("AxisY"), tag.getDouble("AxisZ"));
        return new FrameJoint(tag.getUUID("Other"), tag.getInt("Mine"), tag.getInt("Theirs"), Kind.valueOf(tag.getString("Kind")),
                axis.lengthSqr() < 1e-9 ? new Vec3(0, 1, 0) : axis.normalize(), tag.getBoolean("Drives"));
    }
}
