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
package io.github.soundgoodizerfan.feedback.logistics.sort;

/**
 * A kind of sensing head and how blurry it is. {@code noise} scales the wrong-fate chance near a
 * cut (1 = a plain head); better heads narrow it, none reaches zero. Each head's physical weakness
 * lives in {@link Sorter}.
 */
public enum Detector {
    /** A permanent magnet core: free, fixed pull. */
    MAGNET(1.0f),
    /** An energised magnet: stronger, switchable. */
    ELECTROMAGNET(0.6f),
    /** Reflectivity head. */
    OPTICAL(0.8f),
    /** Mass and density from the belt's weight on the cell. */
    WEIGHER(0.9f),
    /** A nozzle lifting light cargo; poorest cut. */
    JET(1.3f),
    /** A fine, hand-cut check (thermal read, kind): the baseline. */
    PLAIN(1.0f);

    public final float noise;

    Detector(float noise) {
        this.noise = noise;
    }
}
