// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;

/**
 * A stock braced on the shoulder: crouched and still, kick and spread fall by what the stock's pad cushions and its core holds
 * ({@link io.github.soundgoodizerfan.feedback.weapon.HandlingPhysics#stock}), scaled by the variant's {@code length}. The cut is
 * baked as {@code Fig.BRACE_KICK} and {@code BRACE_SPREAD} ({@link io.github.soundgoodizerfan.feedback.weapon.GunSpec.Hand#braced})
 * and {@link ModularTraits#gunAimed} applies it while the shooter is braced; this trait only marks the part. A gun with a bipod braces
 * on the legs too only when they are fully down; folded, the stock's own {@code FOLDED_BRACE_KICK} and {@code FOLDED_BRACE_SPREAD} apply.
 */
public class GunBraced implements ModularTrait {
}
