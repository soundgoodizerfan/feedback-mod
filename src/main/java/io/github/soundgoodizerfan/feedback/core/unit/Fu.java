// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core.unit;

/**
 * Cumulative mechanical work beaten into a workpiece.
 *
 * <h2>An integer, and that is a model decision rather than an optimisation</h2>
 * Work arrives in blows. A workpiece is at 9 Fu of the 14 it needs because it has been hit twice,
 * not because a rate integrated to 9.2, and the whole overrun mechanic depends on the player
 * being able to count: a plate at five blows on the gentle setting and two on the strong one is a
 * fact you can hold in your head. Fractional Fu would make "one more blow" a question rather than
 * an arithmetic.
 * <p>
 * So this is the one unit that does not wrap a float, which is why {@link Units} carries integer
 * flavours of its codec and wire helpers, and why {@link Unit#raw()} is documented as good enough
 * to draw with rather than as the value.
 *
 * <h2>Not independent of {@link St}</h2>
 * §17 is explicit and it is the easiest thing here to get wrong: work delivered per blow is
 * {@code St / hardness}. A machine has a blow strength; what that blow <em>accomplishes</em> is a
 * property of the material being hit, never a machine stat. The two types are separate because
 * they are different quantities, not because they are unrelated -- and no method here converts
 * one to the other, because the conversion needs a material and neither type has one.
 */
public record Fu(int value) implements Unit {

    @Override
    public float raw() {
        return value;
    }

    @Override
    public String unitKey() {
        return "feedback.unit.fu";
    }
}
