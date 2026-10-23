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
package io.github.soundgoodizerfan.feedback.modular;

import java.util.HashMap;
import java.util.Map;

import io.github.soundgoodizerfan.feedback.core.unit.Tu;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * What each {@code kind} named in an {@link Attunements.Behaviour} does at runtime
 * ({@code docs/feedback_enchant_roster.md} 1). A kind is a coupling a part can have; the effect row picks one per role
 * and polarity, so a new effect is a row and a new kind is one registration here. A kind with no entry for a moment
 * does nothing at that moment; one that no map knows (a pure bake kind such as {@code fracture}, which only shifts
 * flaws through {@link Attunements#flawShift}) is not an error.
 *
 * <p>Three moments, the ones the existing kinds needed: the wielder's blow lands ({@link Strike}), the wearer is struck
 * ({@link Struck}), and the part is a flame in the hand ({@link Flame}). Add a moment when a kind needs one; keep each
 * handler to the one thing it does.
 */
public final class AttunementKinds {

    private AttunementKinds() {
    }

    /** The wielder's blow is landing: change the event or its target. */
    @FunctionalInterface
    public interface Strike {
        void apply(LivingIncomingDamageEvent event, MaterialPerks.Attunement attunement);
    }

    /** The wearer was struck by {@code attacker}. */
    @FunctionalInterface
    public interface Struck {
        void apply(LivingEntity attacker, MaterialPerks.Attunement attunement);
    }

    /** The heat the part burns the holder at, or null. */
    @FunctionalInterface
    public interface Flame {
        Tu of(MaterialPerks.Attunement attunement);
    }

    private static final Map<String, Strike> STRIKE = new HashMap<>();
    private static final Map<String, Struck> STRUCK = new HashMap<>();
    private static final Map<String, Flame> FLAME = new HashMap<>();

    public static void onStrike(String kind, Strike handler) {
        STRIKE.put(kind, handler);
    }

    public static void onStruck(String kind, Struck handler) {
        STRUCK.put(kind, handler);
    }

    public static void onFlame(String kind, Flame handler) {
        FLAME.put(kind, handler);
    }

    static void strike(LivingIncomingDamageEvent event, MaterialPerks.Attunement attunement) {
        Strike handler = STRIKE.get(attunement.kind());
        if (handler != null)
            handler.apply(event, attunement);
    }

    static void struck(LivingEntity attacker, MaterialPerks.Attunement attunement) {
        Struck handler = STRUCK.get(attunement.kind());
        if (handler != null)
            handler.apply(attacker, attunement);
    }

    /** The heat {@code attunement} burns its holder at, or null if its kind is not a flame. */
    static Tu flame(MaterialPerks.Attunement attunement) {
        Flame handler = FLAME.get(attunement.kind());
        return handler == null ? null : handler.of(attunement);
    }

    static {
        ImpulseKinds.register();
        EdgeKinds.register();
        BaneKinds.register();
        HeatKinds.register();
        WardKinds.register();
        MendKinds.register();
        TideKinds.register();
        HoldKinds.register();
        ConductKinds.register();
        RecallKinds.register();
        SpringKinds.register();
        YieldKinds.register();
    }
}
