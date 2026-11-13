// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.bake;

import io.github.soundgoodizerfan.feedback.combat.gear.GearTuning;
import io.github.soundgoodizerfan.feedback.weapon.GunTuning;
import java.util.Map;
import java.util.Optional;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import io.github.soundgoodizerfan.feedback.core.unit.Mu;
import io.github.soundgoodizerfan.feedback.core.unit.Pu;
import io.github.soundgoodizerfan.feedback.core.unit.VoltageBand;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.weapon.FireMode;
import io.github.soundgoodizerfan.feedback.weapon.GunFigures;
import io.github.soundgoodizerfan.feedback.modular.type.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.type.ModularType;

/**
 * The physics of a pulse gun, read from its design. A shot is a pulse of electrical energy, not a solid: the <b>seat</b> (its variant is
 * the heat cycle, its conductor the ceiling on a pulse, its power tier a fraction of that ceiling) shapes the pulse and draws it from the
 * battery, and the <b>barrel</b> hands it on: a conductive barrel takes part of the pulse out and turns it to heat, so a barrel is chosen for
 * low conductivity. Nothing here is a dial: every choice is a part, a variant or one of three tiers.
 *
 * @param seat       the heat cycle.
 * @param tier       the power tier, 1 (Light) to 3 (Heavy).
 * @param boreMm     the bore, millimetres.
 * @param lengthM    the barrel's length, metres.
 * @param barrelLoss share of a pulse the barrel's conductor takes out of it.
 * @param seatAmp    the seat's conductor (ampacity), the ceiling on what a pulse holds.
 * @param seatDensity the seat's density: a flywheel's wheel.
 * @param dielectric the seat's insulation band index: what a bank's plates hold.
 * @param band       the voltage band the receiver's insulation holds off.
 * @param barrelMass kilograms of the barrel.
 * @param maxCells   the most cells of battery the receiver's type takes.
 * @param heatMult   multiple of the heat a shot leaves, from the receiver's type (a heavy frame keeps cooler).
 * @param report     multiple of how loud a shot is (a suppressor cuts it, a brake raises it).
 * @param heatGain   multiple of the heat a shot leaves, from fittings.
 * @param shed       multiple of how fast the gun sheds heat (a shroud, a thermal sleeve).
 * @param spreadMult multiple of the shot's spread (a stock, a bipod).
 * @param darkAt     light level at or below which a lit reticle helps (0 for none), and by how much it cuts spread.
 * @param governor   the heat governor fitted.
 * @param holdCharge whether a hold-charge trigger is fitted.
 * @param pump       whether a pump charge is fitted.
 * @param beams      beams of a beam splitter (0 for none).
 * @param fork       whether an arc fork is fitted.
 * @param zoom       whether a scope zoom charge is fitted.
 * @param gauge      whether a heat gauge is fitted.
 * @param sight      whether a sight is fitted.
 * @param sightFov   the field a scope closes the view to when aimed (its zoom axis through its lens); 1 for any other sight.
 * @param bank       levels of charge attuned on the seat: a seat that holds more charge sends a bigger pulse.
 *  * @param limitTu    the barrel temperature (Tu) the governor holds the gun under (its heat reads 1): the lower of where a bare hand is burned and the barrel material's service temperature.
 * @param serviceTu  the barrel material's service temperature (a constant for one that states none): past it a shot wears the gun.
 * @param sense      share of the barrel's true heat the governor's element passes on (its thermal conductivity); 1 for none fitted.
 * @param zoomGain   how strongly the zoom coil's field builds, from its winding's conductor; 1 for none fitted.
 */
public record ElectricGunSpec(Seat seat, int tier, float boreMm, float lengthM, float barrelLoss, float seatAmp, float seatDensity,
                              int dielectric, VoltageBand band, float barrelMass, int maxCells, float heatMult, float report,
                              float heatGain, float shed, float spreadMult, float darkAt, float darkCut, Governor governor,
                              boolean holdCharge, boolean pump, int beams, boolean fork, boolean zoom, boolean gauge, boolean sight, float sightFov, int bank, float limitTu, float serviceTu,
                              float sense, float zoomGain, float pumpGain, float pumpLever, float holdGain, float gaugeRes) {

    /** The heat cycle: what a seat does between shots, and the fire modes it allows. */
    public enum Seat {
        CYLINDER("cylinder", true, false, FireMode.SEMI),
        FLUX_CYCLER("flux_cycler", false, false, FireMode.AUTO, FireMode.SEMI),
        CAPACITOR_BANK("capacitor_bank", true, true, FireMode.SEMI),
        BURST_BANK("burst_bank", true, true, FireMode.BURST, FireMode.SEMI),
        CORE_BREECH("core_breech", true, true, FireMode.SEMI),
        FLYWHEEL("flywheel", false, true, FireMode.AUTO, FireMode.SEMI),
        RECOIL_DYNAMO("recoil_dynamo", false, false, FireMode.AUTO, FireMode.SEMI, FireMode.BURST),
        BACKPACK_TETHER("backpack_tether", false, false, FireMode.AUTO, FireMode.SEMI, FireMode.BURST);

        private final String id;
        private final boolean single, big;
        private final FireMode[] modes;

        Seat(String id, boolean single, boolean big, FireMode... modes) {
            this.id = id;
            this.single = single;
            this.big = big;
            this.modes = modes;
        }

        /** Whether the seat fires one shot a pull (the cylinder, the banks): the only seats a hold-charge trigger or a pump charge suit. */
        public boolean single() {
            return single;
        }

        /** Whether the seat needs a long gun or machine gun frame. */
        public boolean big() {
            return big;
        }

        public FireMode[] modes() {
            return modes;
        }

        public static Seat of(String name) {
            for (Seat s : values())
                if (s.id.equals(name))
                    return s;
            return CYLINDER;
        }
    }

    public enum Governor {
        NONE, THROTTLE, VENT, CUTOFF
    }

    public static final String BARREL = "barrel", RECEIVER = "receiver", SEAT = "seat";

    /** The spec of {@code design}, or empty if it lacks a barrel, receiver, trigger or seat, a material is unknown, or the build is refused ({@link #fault}). */
    public static Optional<ElectricGunSpec> of(ModularDesign design, ModularType type, Map<ResourceLocation, Material> materials) {
        float bore = 0, length = 0, loss = 0, mass = 0, seatAmp = 0, seatDensity = 0;
        int tier = 2, dielectric = 0, maxCells = 2, beams = 0;
        Seat seat = null;
        VoltageBand band = VoltageBand.LV;
        boolean barrel = false, receiver = false, trigger = false;
        float heatMult = 1f, report = 1f, heatGain = 1f, shed = 1f, spreadMult = 1f, darkAt = 0f, darkCut = 0f;
        Governor governor = Governor.NONE;
        boolean hold = false, pump = false, fork = false, zoom = false, gauge = false, sight = false;
        float sightFov = 1f;
        int bank = 0;
        float sense = 1f, zoomGain = 1f, pumpGain = 1f, pumpLever = 1f, holdGain = 1f, gaugeRes = 1f;
        float limitTu = ModularTuning.EGUN_LIMIT_TU, serviceTu = ModularTuning.EGUN_LIMIT_TU;
        for (MaterialPerks.Attunement a : MaterialPerks.attunements(design))
            if (a.kind().equals("pulse_bank"))
                bank += a.level();
        for (ModularDesign.Part p : design.parts()) {
            ModularType.Role role = type.roles().get(p.role());
            Material m = materials.get(p.material());
            if (role == null || m == null)
                return Optional.empty();
            switch (role.name()) {
                case BARREL -> {
                    bore = role.axis(p, "bore");
                    length = role.axis(p, "barrel_length");
                    loss = Mth.clamp(m.ampacity().orElse(0f) * ModularTuning.EGUN_BARREL_LOSS_PER_AMPACITY, 0f, ModularTuning.EGUN_BARREL_LOSS_MAX);
                    mass = ModularBake.mass(role, p, m);
                    serviceTu = m.thermal().serviceTemperature().map(v -> v.value()).orElse(ModularTuning.EGUN_LIMIT_TU);
                    limitTu = Math.min(serviceTu, io.github.soundgoodizerfan.feedback.core.hazard.HazardTuning.CONTACT_BURN_TU.value());
                    barrel = true;
                }
                case RECEIVER -> {
                    band = m.electrical().insulation().orElse(VoltageBand.LV);
                    var v = role.variant(p);
                    maxCells = Math.round(v.map(x -> x.params().getOrDefault("max_cells", 2f)).orElse(2f));
                    heatMult = v.map(x -> x.params().getOrDefault("heat_mult", 1f)).orElse(1f);
                    receiver = true;
                }
                case "trigger" -> trigger = true;
                case SEAT -> {
                    seat = Seat.of(role.variant(p).map(ModularType.Variant::name).orElse("cylinder"));
                    tier = Mth.clamp(Math.round(role.axis(p, "tier")), 1, 3);
                    seatAmp = m.ampacity().orElse(0f);
                    seatDensity = m.density().muPerMb();
                    Material dm = ModularBake.slotMaterial(role, p, "dielectric", materials);
                    dielectric = (dm == null ? m : dm).electrical().insulation().orElse(VoltageBand.LV).ordinal();
                }
                case "heat_governor" -> {
                    governor = switch (role.variant(p).map(ModularType.Variant::name).orElse("fire_rate_throttle")) {
                        case "forced_vent" -> Governor.VENT;
                        case "dead_man_cutoff" -> Governor.CUTOFF;
                        default -> Governor.THROTTLE;
                    };
                    // The element (a bimetal strip, a thermistor bead) follows the barrel as fast as it conducts heat.
                    Material el = ModularBake.slotMaterial(role, p, "element", materials);
                    float k = el == null ? 0f : el.thermal().conductivity().map(c -> c.workPerTickPerTu()).orElse(0f);
                    sense = Mth.clamp(GearTuning.saturate(k, ModularTuning.EGUN_SENSE_HALF_K), ModularTuning.EGUN_SENSE_MIN, 1f);
                    // The housing carries the barrel's heat to the element: a poor conductor reads late too.
                    Material hs = ModularBake.slotMaterial(role, p, "housing", materials);
                    float hk = hs == null ? 0f : hs.thermal().conductivity().map(c -> c.workPerTickPerTu()).orElse(0f);
                    sense *= ModularTuning.EGUN_HOUSING_FLOOR + (1f - ModularTuning.EGUN_HOUSING_FLOOR) * GearTuning.saturate(hk, io.github.soundgoodizerfan.feedback.weapon.GunTuning.FIT_K_HALF);
                }
                case "hold_charge" -> {
                    hold = true;
                    // The spring stores the charge: a stiffer metal holds more of it; a flexing lever lets the release wander.
                    Material sp = ModularBake.slotMaterial(role, p, "spring", materials);
                    holdGain = Mth.clamp((ModularTuning.EGUN_HOLD_SPRING_FLOOR + (1f - ModularTuning.EGUN_HOLD_SPRING_FLOOR) * io.github.soundgoodizerfan.feedback.weapon.HandlingPhysics.rigidity(sp))
                            / (ModularTuning.EGUN_HOLD_SPRING_FLOOR + (1f - ModularTuning.EGUN_HOLD_SPRING_FLOOR) * ModularTuning.EGUN_HOLD_SPRING_REF), 0.5f, 1.4f);
                    spreadMult *= 1f + ModularTuning.EGUN_LEVER_FLEX_SPREAD * (1f - io.github.soundgoodizerfan.feedback.weapon.HandlingPhysics.rigidity(ModularBake.slotMaterial(role, p, "lever", materials)));
                }
                case "pump_charge" -> {
                    pump = true;
                    // The magneto winding sets the energy of a pump; the lever's metal sets how hard it can be driven before it bends.
                    Material wd = ModularBake.slotMaterial(role, p, "winding", materials);
                    float wamp = wd == null ? 0f : wd.ampacity().orElse(0f);
                    pumpGain = Mth.clamp(GearTuning.saturate(wamp, ModularTuning.EGUN_ZOOM_AMP_HALF) / GearTuning.saturate(1f, ModularTuning.EGUN_ZOOM_AMP_HALF),
                            ModularTuning.EGUN_ZOOM_GAIN_MIN, ModularTuning.EGUN_ZOOM_GAIN_MAX);
                    pumpLever = io.github.soundgoodizerfan.feedback.weapon.HandlingPhysics.rigidity(ModularBake.slotMaterial(role, p, "lever", materials));
                }
                case "beam_lens" -> {
                    String v = role.variant(p).map(ModularType.Variant::name).orElse("beam_splitter");
                    if (v.equals("arc_fork"))
                        fork = true;
                    else
                        beams = Mth.clamp(Math.round(role.axis(p, "beams")), 3, 7);
                }
                case "zoom_charge" -> {
                    zoom = true;
                    // The winding's conductor sets the field a turn builds.
                    Material w = ModularBake.slotMaterial(role, p, "winding", materials);
                    float amp = w == null ? 0f : w.ampacity().orElse(0f);
                    Material zc = ModularBake.slotMaterial(role, p, "core", materials);
                    float coreGain = zc != null && zc.separation().magnetic() ? 1f : ModularTuning.EGUN_ZOOM_NONMAGNETIC_CORE;
                    zoomGain = coreGain * Mth.clamp(GearTuning.saturate(amp, ModularTuning.EGUN_ZOOM_AMP_HALF) / GearTuning.saturate(1f, ModularTuning.EGUN_ZOOM_AMP_HALF),
                            ModularTuning.EGUN_ZOOM_GAIN_MIN, ModularTuning.EGUN_ZOOM_GAIN_MAX);
                }
                case "heat_gauge" -> {
                    gauge = true;
                    // The coil's conductor sets how finely the needle can be read.
                    Material gc = ModularBake.slotMaterial(role, p, "coil", materials);
                    gaugeRes = gc == null ? 0.5f : Mth.clamp(GearTuning.saturate(gc.ampacity().orElse(0f), ModularTuning.EGUN_ZOOM_AMP_HALF) / GearTuning.saturate(1f, ModularTuning.EGUN_ZOOM_AMP_HALF), 0.2f, 1f);
                }
                case "sight" -> {
                    sight = true;
                    if ("scope".equals(role.variant(p).map(ModularType.Variant::name).orElse(""))) {
                        Material lens = ModularBake.slotMaterial(role, p, "lens", materials);
                        float lensLoss = lens == null ? 0f : lens.separation().reflectivity().orElse(ModularTuning.LENS_DEFAULT_LOSS);
                        sightFov = io.github.soundgoodizerfan.feedback.weapon.GunSpec.Sight.scope(role.axis(p, "zoom"), 1f - lensLoss).fov();
                    }
                    spreadMult *= role.variant(p).map(x -> x.params().getOrDefault("spread_mult", 1f)).orElse(1f);
                }
                default -> {
                    // A stock's variant steadies the aim; a lit reticle cuts spread in the dark.
                    var v = role.variant(p);
                    spreadMult *= v.map(x -> x.params().getOrDefault("spread_mult", 1f)).orElse(1f);
                    if (v.isPresent() && v.get().params().containsKey("dark_at")) {
                        darkAt = v.get().params().get("dark_at");
                        darkCut = v.get().params().getOrDefault("spread_cut", 0f);
                    }
                }
            }
            // A fitted attachment (a gun option, by the part's form): the figures it owns that mean something here.
            var option = p.kind().isEmpty() ? java.util.Optional.<io.github.soundgoodizerfan.feedback.weapon.GunOption>empty()
                    : io.github.soundgoodizerfan.feedback.weapon.GunOptions.ofForm(p.kind());
            if (option.isPresent()) {
                var fp = io.github.soundgoodizerfan.feedback.weapon.FittingPhysics.of(option.get(), m);
                report *= fp.blast();
                heatGain *= fp.heat();
                shed *= fp.shed();
            }
        }
        if (!barrel || !receiver || !trigger || seat == null || bore <= 0 || length <= 0 || seatAmp <= 0)
            return Optional.empty();
        ElectricGunSpec spec = new ElectricGunSpec(seat, tier, bore, length, loss, seatAmp, seatDensity, dielectric, band, mass, maxCells,
                heatMult, report, heatGain, shed, spreadMult, darkAt, darkCut, governor, hold, pump, beams, fork, zoom, gauge, sight, sightFov, bank, limitTu, serviceTu, sense, zoomGain, pumpGain, pumpLever, holdGain, gaugeRes);
        return fault(design, type) == null ? Optional.of(spec) : Optional.empty();
    }

    /**
     * Why the design is refused, as the tail of {@code message.feedback.blueprint_table.fault.<reason>}, or null: a big seat needs a long gun
     * or machine gun frame; a hold-charge trigger and a pump charge need a seat that fires one shot a pull.
     */
    public static String fault(ModularDesign design, ModularType type) {
        String frame = "", seat = "";
        boolean charge = false;
        for (ModularDesign.Part p : design.parts()) {
            ModularType.Role role = type.roles().get(p.role());
            if (role == null)
                continue;
            switch (role.name()) {
                case RECEIVER -> frame = role.variant(p).map(ModularType.Variant::name).orElse("handgun");
                case SEAT -> seat = role.variant(p).map(ModularType.Variant::name).orElse("cylinder");
                case "hold_charge", "pump_charge" -> charge = true;
                default -> {
                }
            }
        }
        Seat s = Seat.of(seat);
        if (s.big() && frame.equals("handgun"))
            return "pulse_seat_frame";
        if (charge && !s.single())
            return "pulse_charge_seat";
        return null;
    }

    // --- the pulse ---------------------------------------------------------------------------

    /** The hard ceiling on the joules one pulse holds: the seat's conductor. */
    public float ceilJ() {
        return ModularTuning.EGUN_SEAT_CEIL_J * Mth.clamp(seatAmp, 0.1f, 3f) * (1f + ModularTuning.EGUN_BANK_PER_LEVEL * bank);
    }

    /** The share of the ceiling this tier takes. */
    public float tierFraction() {
        return ModularTuning.EGUN_TIER_FRACTION[tier - 1];
    }

    /** Joules one ordinary pulse holds, before the barrel. */
    public float pulseJ() {
        return ceilJ() * tierFraction();
    }

    /** How the seat shapes its pulse against an ordinary one: a bank holds a lot, a flywheel by its density. */
    public float seatMult() {
        return switch (seat) {
            case CAPACITOR_BANK -> ModularTuning.EGUN_BANK_MULT * (1f + ModularTuning.EGUN_BANK_DIELECTRIC_STEP * dielectric);
            case BURST_BANK -> ModularTuning.EGUN_BANK_MULT * (1f + ModularTuning.EGUN_BANK_DIELECTRIC_STEP * dielectric) / ModularTuning.GUN_BURST_ROUNDS;
            case CORE_BREECH -> ModularTuning.EGUN_CORE_MULT;
            case FLYWHEEL -> Mth.clamp(0.6f + 0.4f * seatDensity / ModularTuning.EGUN_FLY_REF_DENSITY, 0.6f, 1.6f);
            default -> 1f;
        };
    }

    /** The look of the shot: an arc fork is a bolt, a core breech a blob of heat, a bank or a wide bore a thick discharge, else a thin beam. */
    public byte style() {
        if (fork)
            return io.github.soundgoodizerfan.feedback.weapon.EnergyStyle.ARC;
        if (seat == Seat.CORE_BREECH)
            return io.github.soundgoodizerfan.feedback.weapon.EnergyStyle.PLASMA;
        return seat == Seat.CAPACITOR_BANK || seat == Seat.BURST_BANK || boreShare() > 0.5f
                ? io.github.soundgoodizerfan.feedback.weapon.EnergyStyle.PULSE : io.github.soundgoodizerfan.feedback.weapon.EnergyStyle.LASER;
    }

    /** The bore's width against its range, 0 narrow to 1 wide. */
    private float boreShare() {
        return Mth.clamp((boreMm - 6.25f) / 12.5f, 0f, 1f);
    }

    /** What the bore does to a pulse's damage (and its cone): 1 - span/2 narrow to 1 + span/2 wide. */
    public float boreFactor() {
        return 1f - ModularTuning.EGUN_BORE_SPAN / 2f + ModularTuning.EGUN_BORE_SPAN * boreShare();
    }

    /** Joules of {@code drawn} that leave the barrel: the barrel's conductor takes its share out as heat. */
    public float delivered(float drawn) {
        return drawn * (1f - barrelLoss) * boreFactor();
    }

    /** The hit of a pulse that left the barrel with {@code joules}. */
    public float damage(float joules) {
        return joules * ModularTuning.EGUN_DAMAGE_PER_J;
    }

    /** Blocks a pulse holds its full damage out to, at zoom charge {@code zoomShare} (0 to 1): then nothing. */
    public float cutoff(float zoomShare) {
        float r = (ModularTuning.EGUN_RANGE_BASE + ModularTuning.EGUN_RANGE_PER_M * lengthM) * (1f + ModularTuning.EGUN_RANGE_PER_TIER * (tier - 1));
        return r * (1f + (zoom ? ModularTuning.EGUN_ZOOM_RANGE * zoomGain * zoomShare : 0f));
    }

    /** Mobs a pulse goes through beyond the first, at hold charge {@code holdShare} (0 to 1); a capacitor bank goes through every mob. */
    public int pierce(float holdShare) {
        if (seat == Seat.CAPACITOR_BANK)
            return Integer.MAX_VALUE;
        return tier * ModularTuning.EGUN_PIERCE_PER_TIER + (holdCharge ? Math.round(ModularTuning.EGUN_HOLD_PIERCE * holdShare) : 0);
    }

    /** Tu a pulse of {@code power} (1 for an ordinary one) puts in the barrel: its tier, the barrel's conductor and the fittings; nothing of what the barrel can take. */
    public float heatPerShot(float power) { // Tu
        return ModularTuning.EGUN_HEAT_PER_SHOT * (tierFraction() / ModularTuning.EGUN_TIER_FRACTION[1]) * power
                * (1f + barrelLoss * ModularTuning.EGUN_BARREL_HEAT_PER_LOSS) * heatGain * heatMult;
    }

    /** The cone of a pulse before a stance and a scope: the barrel's length and bore, the fittings. */
    public float coneDeg(float zoomShare) {
        return Math.max(GunTuning.GUN_SPREAD_FLOOR_DEG, ModularTuning.EGUN_SPREAD_BASE_DEG * 0.4f / Math.max(0.1f, lengthM) * spreadMult * boreFactor()
                * (1f - (zoom ? ModularTuning.EGUN_ZOOM_TIGHT * zoomGain * zoomShare : 0f)));
    }

    /** Chambers a second a cylinder refills at, from the seated battery's cells and band, the chamber's conductor and the tier. */
    public float refillPerSecond(io.github.soundgoodizerfan.feedback.core.unit.VoltageBand band, int cells) {
        float battery = Math.max(0.25f, cells / 2f) * Math.min(1.5f, io.github.soundgoodizerfan.feedback.core.electric.ElectricRules.packetEu(band) / io.github.soundgoodizerfan.feedback.core.electric.ElectricRules.packetEu(ModularTuning.EGUN_REF_BAND));
        return ModularTuning.EGUN_CYL_REFILL_PER_S * Mth.clamp(seatAmp, 0.3f, 2f) * battery * (ModularTuning.EGUN_TIER_FRACTION[1] / tierFraction());
    }

    /** Ticks a capacitor bank or a burst bank takes to recharge: the tier's base, longer for a bigger battery. */
    public int bankRechargeTicks(int cells) {
        return Math.round(ModularTuning.EGUN_BANK_RECHARGE_S[tier - 1] * 20f * Math.max(1, cells) / 2f);
    }

    /** Ticks of a held trigger a flywheel takes to spin up: a denser wheel takes longer. */
    public float spinUpTicks() {
        return ModularTuning.EGUN_FLY_UP_TICKS * Mth.clamp(seatDensity / ModularTuning.EGUN_FLY_REF_DENSITY, 0.5f, 2f);
    }

    /** Ticks between shots with nothing slowing it: the seat's cycle ({@code burst}: inside a burst bank's burst). */
    public int cycleTicks(boolean burst) {
        return switch (seat) {
            case CYLINDER -> ModularTuning.EGUN_COOLDOWN;
            case CAPACITOR_BANK -> ModularTuning.EGUN_BANK_CYCLE;
            case CORE_BREECH -> ModularTuning.EGUN_CORE_CYCLE;
            case BURST_BANK -> burst ? ModularTuning.EGUN_BURST_GAP_TICKS : ModularTuning.EGUN_BANK_CYCLE / 2;
            case FLYWHEEL -> ModularTuning.EGUN_FLY_CYCLE;
            default -> ModularTuning.EGUN_FLUX_CYCLE;
        };
    }

    /** The recoil figure of a pulse of {@code joules}: what kick and hand strain read. */
    public static float impulse(float joules) {
        return impulse(joules, Math.max(0f, joules) * ModularTuning.EGUN_DAMAGE_PER_J);
    }

    /**
     * As above, for a pulse that drew {@code joules} and hits for {@code hit}: the larger of the figure the draw gives and half of what a
     * powder round of that hit kicks with (a round's impulse runs about {@code 0.29 hit^1.5}), so a hard-hitting pulse is never a feather.
     */
    public static float impulse(float joules, float hit) {
        float drawn = 0.15f * (float) Math.sqrt(Math.max(0f, joules));
        return Math.max(drawn, ModularTuning.EGUN_RECOIL_PER_HIT * (float) Math.pow(Math.max(0f, hit), 1.5));
    }

    /**
     * The shot as the gun pipeline reads it ({@link GunFigures}): the muzzle energy is the work that gives the hit of a pulse, so what a
     * shot hurts stays in {@code EGUN_DAMAGE_PER_J}. A pulse has no projectile, so its speed only says it is instant.
     *
     * @param joules    drawn from the battery for the shot.
     * @param gunKg     the gun's mass.
     * @param cycleTicks ticks to the next shot.
     * @param spreadMult what a stance, a lit reticle and the like do to the cone.
     */
    public GunFigures figures(float joules, float gunKg, int cycleTicks, float zoomShare, float spreadMult) {
        float hit = damage(delivered(joules));
        float energy = hit / GunTuning.GUN_DAMAGE_PER_WORK;
        float impulse = impulse(joules, hit);
        float spread = coneDeg(zoomShare) * spreadMult;
        float kick = GunTuning.GUN_KICK_DEG_PER_IMPULSE * impulse * (GunTuning.GUN_KICK_REFERENCE_MU / Math.max(0.05f, gunKg));
        return new GunFigures(new Pu(0f), new Pu(0f), 10f, energy, new Mu(0.001f), new Mu(gunKg), impulse, cycleTicks * 50, spread, kick,
                report, heatGain * heatMult, shed);
    }
}
