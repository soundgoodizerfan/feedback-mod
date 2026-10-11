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
package io.github.soundgoodizerfan.feedback.process;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * One way a form becomes another: {@code [input form, fussiness, output form]}.
 *
 * <h2>The function is stored, not its results</h2>
 * What this step costs a material is {@code base + slope * hardness}, evaluated per material when
 * {@link FormTable} compiles. Adding a material is one number; adding a step is one file and every
 * material that has both forms picks it up. The shape is affine on purpose. Per blow, a material
 * absorbs {@code St / hardness}, so blows needed go as {@code work * hardness}; a step that was
 * simply {@code k * hardness} would make that go as hardness squared and steel would be
 * unworkable. {@code base} is what keeps hard materials from paying twice.
 *
 * @param from      form consumed. A material without it gets no entry from this step.
 * @param to        form produced. Likewise.
 * @param operation how force must be delivered; also decides whether this compiles to a
 *                  {@link Deformation} or a {@link Removal} ({@link Operation#removesMaterial}).
 * @param base      work at zero hardness.
 * @param slope     extra work per point of hardness.
 */
public record FormStep(String from, String to, Operation operation, float base, float slope) {

    public static final Codec<FormStep> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("from").forGetter(FormStep::from),
            Codec.STRING.fieldOf("to").forGetter(FormStep::to),
            Operation.CODEC.fieldOf("operation").forGetter(FormStep::operation),
            Codec.FLOAT.optionalFieldOf("base", 0f).forGetter(FormStep::base),
            Codec.FLOAT.fieldOf("slope").forGetter(FormStep::slope)
    ).apply(instance, FormStep::new));

    /** Cumulative Fu this step costs a material of the given hardness. Never below 1. */
    public int work(float hardness) {
        return Math.max(1, Math.round(base + slope * hardness));
    }
}
