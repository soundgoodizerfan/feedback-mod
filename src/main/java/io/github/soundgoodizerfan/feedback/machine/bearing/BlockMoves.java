// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.bearing;

/**
 * Set while blocks are being moved between the world and a body's plot (the Lift, a bearing assembling or putting back):
 * a bearing whose block entity is removed by the move has not been broken, and must not give its body back. Server thread only.
 */
public final class BlockMoves {

    private static int depth;

    private BlockMoves() {
    }

    public static void enter() {
        depth++;
    }

    public static void leave() {
        depth--;
    }

    public static boolean active() {
        return depth > 0;
    }
}
