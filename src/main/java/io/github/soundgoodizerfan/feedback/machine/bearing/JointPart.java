// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.bearing;

/**
 * A block that is the stator side of a {@link Joint} when it is a micro voxel: a click on it with an
 * empty hand assembles the voxels joined to its front face into a body held by that kind of joint,
 * and again puts them back. Its {@code FACING} is the front face.
 */
public interface JointPart {

    Joint.Kind kind();
}
