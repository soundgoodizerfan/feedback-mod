// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
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

    /** A light report (pistol class), three recordings picked at random. */
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_SHOT_LIGHT = SOUNDS.register("gun.shot_light",
            () -> SoundEvent.createVariableRangeEvent(Feedback.id("gun.shot_light")));

    /** A mid report, including the author's own 9 mm recording. */
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_SHOT_MEDIUM = SOUNDS.register("gun.shot_medium",
            () -> SoundEvent.createVariableRangeEvent(Feedback.id("gun.shot_medium")));

    /** A heavy rifle-class report. */
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_SHOT_HEAVY = SOUNDS.register("gun.shot_heavy",
            () -> SoundEvent.createVariableRangeEvent(Feedback.id("gun.shot_heavy")));

    /** A slide drawn: reload begun. */
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_RELOAD_START = SOUNDS.register("gun.reload_start",
            () -> SoundEvent.createVariableRangeEvent(Feedback.id("gun.reload_start")));

    /** One round pressed in. */
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_RELOAD_ROUND = SOUNDS.register("gun.reload_round",
            () -> SoundEvent.createVariableRangeEvent(Feedback.id("gun.reload_round")));

    /** A magazine seated and the action racked. */
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_RELOAD_MAGAZINE = SOUNDS.register("gun.reload_magazine",
            () -> SoundEvent.createVariableRangeEvent(Feedback.id("gun.reload_magazine")));

    /** Trigger on an empty or wrong-bore chamber. */
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_DRY_CLICK = SOUNDS.register("gun.dry_click",
            () -> SoundEvent.createVariableRangeEvent(Feedback.id("gun.dry_click")));

    /** A hold-charge trigger winding up. */
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_EGUN_CHARGE = SOUNDS.register("gun.egun_charge",
            () -> SoundEvent.createVariableRangeEvent(Feedback.id("gun.egun_charge")));

    /** A flywheel seat's hum, looped; pitch follows the spin. */
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_EGUN_SPIN = SOUNDS.register("gun.egun_spin",
            () -> SoundEvent.createVariableRangeEvent(Feedback.id("gun.egun_spin")));

    /** A cell seated in an electric gun. */
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_EGUN_CELL_IN = SOUNDS.register("gun.egun_cell_in",
            () -> SoundEvent.createVariableRangeEvent(Feedback.id("gun.egun_cell_in")));

    /** A cell drawn from an electric gun. */
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_EGUN_CELL_OUT = SOUNDS.register("gun.egun_cell_out",
            () -> SoundEvent.createVariableRangeEvent(Feedback.id("gun.egun_cell_out")));

    /** A pull with nothing to give: a weak fizz. */
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_EGUN_FLAT = SOUNDS.register("gun.egun_flat",
            () -> SoundEvent.createVariableRangeEvent(Feedback.id("gun.egun_flat")));

    /** A powered electromagnet holding a diamagnetic thing off: a faint low hum. */
    public static final DeferredHolder<SoundEvent, SoundEvent> MACHINE_ELECTROMAGNET_HUM = SOUNDS.register("machine.electromagnet_hum",
            () -> SoundEvent.createVariableRangeEvent(Feedback.id("machine.electromagnet_hum")));

    /** A seat gained a charge. */
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_EGUN_REFILL = SOUNDS.register("gun.egun_refill",
            () -> SoundEvent.createVariableRangeEvent(Feedback.id("gun.egun_refill")));

    /** Report layer of a burst bank. */
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_EGUN_BANK = SOUNDS.register("gun.egun_bank",
            () -> SoundEvent.createVariableRangeEvent(Feedback.id("gun.egun_bank")));

    /** Report layer of a recoil dynamo. */
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_EGUN_DYNAMO = SOUNDS.register("gun.egun_dynamo",
            () -> SoundEvent.createVariableRangeEvent(Feedback.id("gun.egun_dynamo")));

    /** Report layer of a backpack tether. */
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_EGUN_TETHER = SOUNDS.register("gun.egun_tether",
            () -> SoundEvent.createVariableRangeEvent(Feedback.id("gun.egun_tether")));

    /** Report layer of a beam splitter. */
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_EGUN_SPLIT = SOUNDS.register("gun.egun_split",
            () -> SoundEvent.createVariableRangeEvent(Feedback.id("gun.egun_split")));

    /** Report layer of an arc fork. */
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_EGUN_FORK = SOUNDS.register("gun.egun_fork",
            () -> SoundEvent.createVariableRangeEvent(Feedback.id("gun.egun_fork")));

    /** A beam lands on a body. */
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_EGUN_HIT_BODY = SOUNDS.register("gun.egun_hit_body",
            () -> SoundEvent.createVariableRangeEvent(Feedback.id("gun.egun_hit_body")));

    /** A beam lands on a block. */
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_EGUN_HIT_BLOCK = SOUNDS.register("gun.egun_hit_block",
            () -> SoundEvent.createVariableRangeEvent(Feedback.id("gun.egun_hit_block")));

    /** A core breech lets a pellet go. */
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_EGUN_PELLET = SOUNDS.register("gun.egun_pellet",
            () -> SoundEvent.createVariableRangeEvent(Feedback.id("gun.egun_pellet")));

    /** A pellet lands. */
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_EGUN_PELLET_HIT = SOUNDS.register("gun.egun_pellet_hit",
            () -> SoundEvent.createVariableRangeEvent(Feedback.id("gun.egun_pellet_hit")));

    /** A dead-man cutoff opens. */
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_EGUN_CUTOFF = SOUNDS.register("gun.egun_cutoff",
            () -> SoundEvent.createVariableRangeEvent(Feedback.id("gun.egun_cutoff")));

    /** A core breech locks hot. */
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_EGUN_HOT_LOCK = SOUNDS.register("gun.egun_hot_lock",
            () -> SoundEvent.createVariableRangeEvent(Feedback.id("gun.egun_hot_lock")));

    /** The barrel sizzles near its limit. */
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_EGUN_OVERHEAT = SOUNDS.register("gun.egun_overheat",
            () -> SoundEvent.createVariableRangeEvent(Feedback.id("gun.egun_overheat")));

    /** The wielder is shocked. */
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_EGUN_SHOCK = SOUNDS.register("gun.egun_shock",
            () -> SoundEvent.createVariableRangeEvent(Feedback.id("gun.egun_shock")));

    /** A pulse gun comes out and powers on. */
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_EGUN_POWER_ON = SOUNDS.register("gun.egun_power_on",
            () -> SoundEvent.createVariableRangeEvent(Feedback.id("gun.egun_power_on")));

    /** A pulse gun goes away and powers off. */
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_EGUN_POWER_OFF = SOUNDS.register("gun.egun_power_off",
            () -> SoundEvent.createVariableRangeEvent(Feedback.id("gun.egun_power_off")));

    /** A scope zoom coil comes up. */
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_EGUN_ZOOM = SOUNDS.register("gun.egun_zoom",
            () -> SoundEvent.createVariableRangeEvent(Feedback.id("gun.egun_zoom")));

    /** A heat gauge or lit reticle comes on. */
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_EGUN_BLIP = SOUNDS.register("gun.egun_blip",
            () -> SoundEvent.createVariableRangeEvent(Feedback.id("gun.egun_blip")));

    /** A pulse gun discharge. */
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_PULSE_SHOT = SOUNDS.register("gun.pulse_shot",
            () -> SoundEvent.createVariableRangeEvent(Feedback.id("gun.pulse_shot")));

    /** A pulse gun pump stroke. */
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_PULSE_PUMP = SOUNDS.register("gun.pulse_pump",
            () -> SoundEvent.createVariableRangeEvent(Feedback.id("gun.pulse_pump")));

    /** A pulse gun pumped to full: a blast armed. */
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_PULSE_ARMED = SOUNDS.register("gun.pulse_armed",
            () -> SoundEvent.createVariableRangeEvent(Feedback.id("gun.pulse_armed")));

    /** A suppressed report. */
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_SHOT_SUPPRESSED = SOUNDS.register("gun.shot_suppressed",
            () -> SoundEvent.createVariableRangeEvent(Feedback.id("gun.shot_suppressed")));

    /** Steam or pressure let out of a hot gun. */
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_VENT = SOUNDS.register("gun.vent",
            () -> SoundEvent.createVariableRangeEvent(Feedback.id("gun.vent")));

    /** A chamber over its rating letting go. */
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_BURST = SOUNDS.register("gun.burst",
            () -> SoundEvent.createVariableRangeEvent(Feedback.id("gun.burst")));

    /** A round going off by itself in a hot barrel. */
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_COOK_OFF = SOUNDS.register("gun.cook_off",
            () -> SoundEvent.createVariableRangeEvent(Feedback.id("gun.cook_off")));

    /** A bullet passing close. */
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_WHIZ = SOUNDS.register("gun.whiz",
            () -> SoundEvent.createVariableRangeEvent(Feedback.id("gun.whiz")));

    /** A bullet glancing off a block. */
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_RICOCHET = SOUNDS.register("gun.ricochet",
            () -> SoundEvent.createVariableRangeEvent(Feedback.id("gun.ricochet")));

    /** Fire thrown from a barrel. */
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_FLAME = SOUNDS.register("gun.flame",
            () -> SoundEvent.createVariableRangeEvent(Feedback.id("gun.flame")));

    /** Priming flashing in a flintlock pan. */
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_PAN_FLASH = SOUNDS.register("gun.pan_flash",
            () -> SoundEvent.createVariableRangeEvent(Feedback.id("gun.pan_flash")));

    /** A gun drawn or holstered. */
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_DRAW = SOUNDS.register("gun.draw",
            () -> SoundEvent.createVariableRangeEvent(Feedback.id("gun.draw")));

    /** A heat governor tripping. */
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_HEAT_TICK = SOUNDS.register("gun.heat_tick",
            () -> SoundEvent.createVariableRangeEvent(Feedback.id("gun.heat_tick")));

    /** The fire-mode selector. */
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_MODE_CLICK = SOUNDS.register("gun.mode_click",
            () -> SoundEvent.createVariableRangeEvent(Feedback.id("gun.mode_click")));

    /** A gun swung as a club. */
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_BUTT = SOUNDS.register("gun.butt",
            () -> SoundEvent.createVariableRangeEvent(Feedback.id("gun.butt")));

    /** Powder poured into a case at the Reloading Press. */
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_PRESS_POUR = SOUNDS.register("gun.press_pour",
            () -> SoundEvent.createVariableRangeEvent(Feedback.id("gun.press_pour")));

    /** A round seated at the Reloading Press. */
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_PRESS_SEAT = SOUNDS.register("gun.press_seat",
            () -> SoundEvent.createVariableRangeEvent(Feedback.id("gun.press_seat")));

    /** A spent case hitting the floor. */
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_CASING = SOUNDS.register("gun.casing",
            () -> SoundEvent.createVariableRangeEvent(Feedback.id("gun.casing")));

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

    /** The field first rises past the coercive field in the Enchanting Table: the part begins to take. */
    public static final DeferredHolder<SoundEvent, SoundEvent> ENCHANT_ENGAGE = ui("ui.enchant_engage");
    /** The held field, sounded every few ticks, its pitch following the field: a tone you steer. */
    public static final DeferredHolder<SoundEvent, SoundEvent> ENCHANT_TONE = ui("ui.enchant_tone");
    /** The polarization enters the target band. */
    public static final DeferredHolder<SoundEvent, SoundEvent> ENCHANT_BAND = ui("ui.enchant_band");
    /** The cooling part falls into its attuning window: the field can act now. */
    public static final DeferredHolder<SoundEvent, SoundEvent> ENCHANT_WINDOW = ui("ui.enchant_window");
    /** An enchantment took clean. */
    public static final DeferredHolder<SoundEvent, SoundEvent> ENCHANT_TAKE = ui("ui.enchant_take");
    /** An enchantment took, but the part is flawed: a note off. */
    public static final DeferredHolder<SoundEvent, SoundEvent> ENCHANT_FLAW = ui("ui.enchant_flaw");
    /** Nothing took: a fizz. */
    public static final DeferredHolder<SoundEvent, SoundEvent> ENCHANT_FIZZ = ui("ui.enchant_fizz");

    /** A punch card punched: the Programmer printing a program. */
    public static final DeferredHolder<SoundEvent, SoundEvent> UI_PUNCH = ui("ui.punch");
    /** The Prep Table's pour minigame, between the pour being judged and the result being shown: the music of what was poured (great, fine, failed), then its fanfare. */
    public static final DeferredHolder<SoundEvent, SoundEvent> COOKING_BGM_GREAT = ui("cooking.bgm_great");
    public static final DeferredHolder<SoundEvent, SoundEvent> COOKING_BGM_SUCCESS = ui("cooking.bgm_success");
    public static final DeferredHolder<SoundEvent, SoundEvent> COOKING_BGM_FAIL = ui("cooking.bgm_fail");
    public static final DeferredHolder<SoundEvent, SoundEvent> COOKING_FANFARE_SUCCESS = ui("cooking.fanfare_success");
    public static final DeferredHolder<SoundEvent, SoundEvent> COOKING_FANFARE_FAIL = ui("cooking.fanfare_fail");
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
    /** The Feedbacker punching a projectile back. */
    public static final DeferredHolder<SoundEvent, SoundEvent> FEEDBACKER_PARRY = ui("combat.feedbacker_parry");
    /** The parry window closed on nothing: dull and low, unlike the success. */
    public static final DeferredHolder<SoundEvent, SoundEvent> PARRY_WHIFF = ui("combat.parry_whiff");
    /** A press that did nothing (recovering, cooldown, no parrying weapon). */
    public static final DeferredHolder<SoundEvent, SoundEvent> PRESS_DENIED = ui("combat.press_denied");
    /** Free to parry or roll again. */
    public static final DeferredHolder<SoundEvent, SoundEvent> READY_TICK = ui("combat.ready_tick");
    /** A roll begun. */
    public static final DeferredHolder<SoundEvent, SoundEvent> ROLL_WHOOSH = ui("combat.roll_whoosh");

    /** One click of a Geiger counter; the counter draws how many a tick. */
    public static final DeferredHolder<SoundEvent, SoundEvent> GEIGER_CLICK = ui("geiger.click");

    private static DeferredHolder<SoundEvent, SoundEvent> ui(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Feedback.id(name)));
    }

    private FSounds() {
    }

    public static void register(IEventBus modBus) {
        SOUNDS.register(modBus);
    }

    /** A bullet landing in a living body. */
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_HIT_FLESH = SOUNDS.register("gun.hit_flesh",
            () -> SoundEvent.createVariableRangeEvent(Feedback.id("gun.hit_flesh")));

    /** A bullet meeting armour or a metal body. */
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_HIT_ARMOUR = SOUNDS.register("gun.hit_armour",
            () -> SoundEvent.createVariableRangeEvent(Feedback.id("gun.hit_armour")));

    /** The shooter's confirmed-hit tick. */
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_HIT_MARKER = SOUNDS.register("gun.hit_marker",
            () -> SoundEvent.createVariableRangeEvent(Feedback.id("gun.hit_marker")));

    /** The shooter's kill tick. */
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_HIT_KILL = SOUNDS.register("gun.hit_kill",
            () -> SoundEvent.createVariableRangeEvent(Feedback.id("gun.hit_kill")));
}
