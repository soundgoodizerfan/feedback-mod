// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
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
