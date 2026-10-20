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
package io.github.soundgoodizerfan.feedback.machine.creative;

import java.util.List;

/**
 * A creative source: a block that emits one energy type at whatever figures an operator sets, for
 * building and testing everything downstream of that energy without first building its supply.
 * <p>
 * Free scalars are right here and nowhere else: philosophy 3's discrete-options rule is about
 * what a <i>player</i> tunes inside a factory, and a test bench has no factory to protect. A
 * creative source is never a survival route -- it lives in the operator tab and answers only to
 * {@code canUseGameMasterBlocks}.
 */
public interface CreativeSource {

    /** One setting. {@code key} names its lang entry under {@code screen.feedback.creative.}. */
    record Param(String key, float value, float step, float min, float max) {
    }

    boolean on();

    List<Param> params();

    /** Server side: take new settings, in {@link #params()} order, clamped to each range. */
    void configure(boolean on, float[] values);

    static float clamp(Param p, float v) {
        return Math.max(p.min(), Math.min(p.max(), v));
    }
}
