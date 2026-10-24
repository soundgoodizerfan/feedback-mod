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
package io.github.soundgoodizerfan.feedback.instrument;

import java.util.function.Consumer;

import net.minecraft.network.chat.Component;

/**
 * A placed part that can say how it responds to a component tester (mechanics 6.20). The part
 * writes its lines with {@link Testable#response} and {@link Testable#condition}, which quantise to
 * the tester it is handed; it never learns who is asking and the tester never learns what part
 * this is, so a new part opts in with one method and no tester change.
 */
public interface Testable {

    /** Say what the part does, in lines, at the resolution of {@code tester}. */
    void test(Instrument tester, Consumer<Component> out);

    /** A 0-1 response, as the tester resolves it. */
    static Component response(Instrument tester, float response) {
        return Component.translatable("feedback.test.response",
                Instruments.format(response, tester.resolution(Quantity.RESPONSE)));
    }

    /** A 0-1 noise, as the tester resolves it (condition's step). */
    static Component noise(Instrument tester, float noise) {
        return Component.translatable("feedback.test.noise",
                Instruments.format(noise, tester.resolution(Quantity.CONDITION)));
    }

    /** A 0-1 condition, as the tester resolves it. */
    static Component condition(Instrument tester, float condition) {
        return Component.translatable("feedback.test.condition",
                Instruments.format(condition, tester.resolution(Quantity.CONDITION)));
    }
}
