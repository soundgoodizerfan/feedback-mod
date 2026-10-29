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
 * Assets under src/main/resources are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Client-only settings (feedback-client.toml). Registered from {@code Feedback} on the client dist only. */
public final class ClientSettings {

    private ClientSettings() {}

    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();

    private static final ModConfigSpec.BooleanValue REDUCE_MOTION = B
            .comment("Drops screen shake, shutters and idle life; keeps sounds and the stage lamp.")
            .define("reduceMotion", false);

    public static final ModConfigSpec SPEC = B.build();

    /** True when the player asked for less motion. False before the config has loaded. */
    public static boolean reduceMotion() {
        return SPEC.isLoaded() && REDUCE_MOTION.get();
    }
}
