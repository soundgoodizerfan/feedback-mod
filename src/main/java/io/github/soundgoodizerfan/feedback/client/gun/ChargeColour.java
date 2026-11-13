// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.gun;

/**
 * The colour a pulse gun's charge lamps take when a hold charge or pump charge is fitted: green, yellow, red, then blinking red at full
 * (a pump's next stroke would burst). The pump count 0 to 3 maps to the four; a hold's build runs in thirds. Shared by the gun's own lamps
 * ({@link GunDraw}) and the HUD card.
 */
public final class ChargeColour {

    private ChargeColour() {
    }

    /** {@code charge} 0 to 1, or below 0 with neither fitted, which keeps {@code plain}. */
    public static int of(float charge, int plain) {
        if (charge < 0f)
            return plain;
        return switch (Math.min(3, (int) (charge * 3f + 0.001f))) {
            case 0 -> 0xFF40FF60;
            case 1 -> 0xFFFFD030;
            case 2 -> 0xFFFF3020;
            default -> System.currentTimeMillis() / 150L % 2L == 0L ? 0xFFFF3020 : 0xFF501410;
        };
    }
}
