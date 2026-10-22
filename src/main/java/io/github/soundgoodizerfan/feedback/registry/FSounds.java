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
package io.github.soundgoodizerfan.feedback.registry;

import io.github.soundgoodizerfan.feedback.Feedback;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The mod's own sound events. Files live under {@code assets/feedback/sounds/} and in {@code sounds.json}. */
public class FSounds {

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, Feedback.MOD_ID);

    /** A 9 mm pistol shot, recorded by the author. The blast layer of every shot (spec 7), re-pitched by power. */
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_SHOT_9MM = SOUNDS.register("gun.shot_9mm",
            () -> SoundEvent.createVariableRangeEvent(Feedback.id("gun.shot_9mm")));

    /**
     * Engine loops, four per family, recorded at {@link #ENGINE_LAYER_RPMS} (SummerCraft's, which
     * synthesised them): {@code ENGINE[family][layer]}. Families by cylinder count, see {@link #engine}.
     */
    public static final String[] ENGINE_FAMILIES = {"inline_4", "v6", "v8"};
    /** Engine speed each loop was made at. */
    public static final float[] ENGINE_LAYER_RPMS = {1000, 2200, 3800, 5800};
    private static final java.util.List<java.util.List<DeferredHolder<SoundEvent, SoundEvent>>> ENGINE = new java.util.ArrayList<>();

    static {
        for (String family : ENGINE_FAMILIES) {
            java.util.List<DeferredHolder<SoundEvent, SoundEvent>> layers = new java.util.ArrayList<>();
            for (int i = 0; i < ENGINE_LAYER_RPMS.length; i++)
                layers.add(ui("engine." + family + "_" + i));
            ENGINE.add(layers);
        }
    }

    /** The loop of {@code layer} for an engine of {@code cylinders}: four or fewer inline, five or six V6, more V8. */
    public static SoundEvent engine(int cylinders, int layer) {
        return ENGINE.get(cylinders <= 4 ? 0 : cylinders <= 6 ? 1 : 2).get(layer).get();
    }

    // Screen sounds. Until recorded files arrive, sounds.json points each at a vanilla event, so a
    // recording replaces the alias there and no code changes.

    /** A blueprint sheet unrolled across the stand: a screen opening on the drafting table. */
    public static final DeferredHolder<SoundEvent, SoundEvent> UI_UNROLL = ui("ui.blueprint_unroll");
    /** The sheet rolled back up: the drafting screen closing. */
    public static final DeferredHolder<SoundEvent, SoundEvent> UI_ROLL = ui("ui.blueprint_roll");
    /** A rubber stamp on paper: committing a drawing (Print). */
    public static final DeferredHolder<SoundEvent, SoundEvent> UI_STAMP = ui("ui.stamp");
    /** One detent of a rotary selector: stepping a setting one notch. */
    public static final DeferredHolder<SoundEvent, SoundEvent> UI_DETENT = ui("ui.detent");

    /** An iron hatch unlatched and swung open: a forge-era machine's screen opening. */
    public static final DeferredHolder<SoundEvent, SoundEvent> UI_HATCH_OPEN = ui("ui.hatch_open");
    /** The hatch shut and latched. */
    public static final DeferredHolder<SoundEvent, SoundEvent> UI_HATCH_CLOSE = ui("ui.hatch_close");
    /** A heavy toggle switch thrown: an instrument panel's switches. */
    public static final DeferredHolder<SoundEvent, SoundEvent> UI_TOGGLE = ui("ui.toggle");

    /** A punch card punched: the Programmer printing a program. */
    public static final DeferredHolder<SoundEvent, SoundEvent> UI_PUNCH = ui("ui.punch");
    /** A perfect forging rhythm: every note clean (spec 4.1). */
    public static final DeferredHolder<SoundEvent, SoundEvent> FORGE_FANFARE = ui("forge.fanfare");
    /** A ruined forging: the worst flaw (spec 4.1). */
    public static final DeferredHolder<SoundEvent, SoundEvent> FORGE_FAILURE = ui("forge.failure");

    /** A light impact: a glancing parry or a small hit. Aliased to vanilla in sounds.json until recorded. */
    public static final DeferredHolder<SoundEvent, SoundEvent> FEEL_LIGHT = ui("combat.feel_light");
    /** A heavy impact: a hard parry or a big hit. */
    public static final DeferredHolder<SoundEvent, SoundEvent> FEEL_HEAVY = ui("combat.feel_heavy");

    /** A parry raised: a swish at the fighter. */
    public static final DeferredHolder<SoundEvent, SoundEvent> PARRY_RAISE = ui("combat.parry_raise");
    /** The parry window closed on nothing: dull and low, unlike the success. */
    public static final DeferredHolder<SoundEvent, SoundEvent> PARRY_WHIFF = ui("combat.parry_whiff");
    /** A press that did nothing (recovering, cooldown, no parrying weapon). */
    public static final DeferredHolder<SoundEvent, SoundEvent> PRESS_DENIED = ui("combat.press_denied");
    /** Free to parry or roll again. */
    public static final DeferredHolder<SoundEvent, SoundEvent> READY_TICK = ui("combat.ready_tick");
    /** A roll begun. */
    public static final DeferredHolder<SoundEvent, SoundEvent> ROLL_WHOOSH = ui("combat.roll_whoosh");

    private static DeferredHolder<SoundEvent, SoundEvent> ui(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Feedback.id(name)));
    }

    private FSounds() {
    }

    public static void register(IEventBus modBus) {
        SOUNDS.register(modBus);
    }
}
