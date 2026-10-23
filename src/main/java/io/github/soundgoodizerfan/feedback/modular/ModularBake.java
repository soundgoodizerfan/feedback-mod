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

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.modular.ModularDesign.Part;
import io.github.soundgoodizerfan.feedback.modular.ModularFigures.Fig;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.Plating;
import io.github.soundgoodizerfan.feedback.weapon.GunFigures;
import io.github.soundgoodizerfan.feedback.weapon.GunSpec;

import net.minecraft.resources.ResourceLocation;

/**
 * A design and the material table in, figures out: a pure function, run once when an item is
 * assembled or changes and stored on the stack (the gun's {@code GunDesign.bake}; Silent Gear's
 * {@code GearData.recalculateGearData} and Tinkers' {@code ToolStack.rebuildStats} are the same
 * shape, taken). Nothing reads a stat table per material -- every figure is derived, and each
 * pulls on the others (spec 2.1):
 *
 * <ul>
 *   <li>mass is each part's volume times its material's density;</li>
 *   <li>how fast it swings falls with the inertia about the hand, which is mass and how far out
 *       it sits, and with the moment the arm has to hold out;</li>
 *   <li>a blow carries the striking mass at the tip's speed, so a heavier head hits harder and
 *       swings slower;</li>
 *   <li>bite is the edge's hardness against its strength, as it was ground;</li>
 *   <li>durability is the wearing edge's volume times that same competence;</li>
 *   <li>the joint carries the head's leverage on a handle and a binding of limited strength.</li>
 * </ul>
 *
 * <p>Two things from the spec are not here yet. Parts come out of the forge at their target size,
 * so there is no as-built geometry to read ({@code docs/feedback_modular_items_spec.md} 4); and heat
 * treatment is the material (hardened steel is its own file, mechanics 5.3), so there is no
 * treatment state either.
 */
public final class ModularBake {

    private ModularBake() {
    }

    /** The edge competence of a material: how hard it is and how well it holds that hardness. */
    public static float competence(Material m) {
        return (float) Math.sqrt(m.hardness().value() * strength(m));
    }

    /** Yield strength, else a tenth of toughness (a crystal states only that), else a small floor. */
    public static float strength(Material m) {
        return m.mechanical().yieldStrength().map(s -> s.value())
                .or(() -> m.mechanical().toughness().map(t -> t.value() * 0.1f))
                .orElse(ModularTuning.STRENGTH_FALLBACK);
    }

    /** Toughness, else the strength it would have stood on. */
    private static float toughness(Material m) {
        return m.mechanical().toughness().map(t -> t.value()).orElse(strength(m));
    }

    /**
     * How much of its competence a piece made whole keeps against cracking: a material tougher than
     * a fifth of its hardness keeps all of it, a brittle one (diamond, quartz) keeps the square of
     * what it has. A whole-diamond blade is therefore possible, bites well and shatters in use --
     * bad, never forbidden (philosophy 7); a diamond set in tougher metal ({@link #zone}) is not
     * whole, so does not pay it.
     */
    static float fracture(Material m) {
        float r = Math.min(1f, toughness(m) / (m.hardness().value() * ModularTuning.FRACTURE_RATIO));
        return r * r;
    }

    /**
     * The wearing zone: what bites, what chips and what lasts.
     *
     * @param hardness gates what it can dig.
     * @param toughness decides what it chips on.
     * @param bite     the competence its bite reads, hardness against the strength behind it.
     * @param wear     the competence its durability reads.
     */
    record Zone(float hardness, float toughness, float bite, float wear) {}

    /**
     * The wearing zone of {@code body}, with {@code set} -- a tip or studs -- set into it if it is
     * harder. A hard piece on a tough body has the piece's hardness (as far as {@code hardnessShare}
     * of the zone is it) and the body's strength behind it, so it bites like the hard material
     * without being brittle like it: the diamond saw blade and the carbide insert. The piece
     * lends only {@code mixShare} of the zone's toughness and wear, because most of it is body.
     */
    static Zone zone(Material body, Material set, float hardnessShare, float mixShare) {
        return mix(new Zone(body.hardness().value(), toughness(body), competence(body), competence(body) * fracture(body)),
                body, set, hardnessShare, mixShare);
    }

    /** {@code zone} with a harder {@code set} mixed in, its bite still resting on {@code body}'s strength. */
    private static Zone mix(Zone zone, Material body, Material set, float hardnessShare, float mixShare) {
        if (set == null || set.hardness().value() <= zone.hardness())
            return zone;
        float h = zone.hardness() + (set.hardness().value() - zone.hardness()) * hardnessShare;
        return new Zone(h, zone.toughness() + (toughness(set) - zone.toughness()) * mixShare,
                (float) Math.sqrt(h * strength(body)), zone.wear() + (competence(set) - zone.wear()) * mixShare);
    }

    /**
     * How much of what a coat can do it does: it must have closed (thick enough), and a coat grown
     * burnt or mixed holds half as well. The same {@code Plating} the electrolytic cell grows, read
     * as properties of the metal that grew it -- no coating table of its own.
     */
    static float coatHold(Plating coat) {
        float closed = Math.min(1f, coat.thickness() / FTuning.PLATING_POROUS_BELOW_QU);
        return closed * (coat.quality() == Plating.Quality.BURNT || coat.quality() == Plating.Quality.MIXED
                ? ModularTuning.COAT_FAULTY_HOLD : 1f);
    }

    public static float bite(float competence, Edge edge) {
        float b = ModularTuning.BITE_BASE + ModularTuning.BITE_SLOPE * (float) Math.log(1 + competence);
        return Math.min(ModularTuning.BITE_MAX, Math.max(ModularTuning.BITE_MIN, b)) * edge.bite();
    }

    public static int enchantability(Material m) {
        return m.enchantability().orElse(ModularTuning.ENCHANTABILITY_FALLBACK);
    }

    /** Which step of the wear ladder {@code worn} (0 new to 1 spent) is on. */
    public static int wearStep(float worn) {
        int step = 0;
        for (float edge : ModularTuning.WEAR_STEPS)
            if (worn >= edge)
                step++;
        return step;
    }

    /**
     * The figures {@code design} makes at {@code step} of wear, or empty if a part names a
     * material the table does not have (a datapack removed it).
     */
    public static Optional<ModularFigures> bake(ModularDesign design, ModularType type,
                                                Map<ResourceLocation, Material> materials, int step) {
        return bake(design, type, materials, step, null);
    }

    /** As {@link #bake(ModularDesign, ModularType, Map, int)}, for an item carrying {@code coat} (or none). */
    public static Optional<ModularFigures> bake(ModularDesign design, ModularType type,
                                                Map<ResourceLocation, Material> materials, int step, Plating coat) {
        Map<String, Material> by = new java.util.HashMap<>();
        for (Part p : design.parts()) {
            Material m = materials.get(p.material());
            if (m == null || !type.roles().containsKey(p.role()))
                return Optional.empty();
            by.put(p.role(), m);
        }
        Part wearPart = design.part(type.wearRole());
        if (wearPart == null)
            return Optional.empty();
        if (type.family() == ModularType.Family.GUN)
            return gun(design, type, by, materials, step);
        if (type.family() == ModularType.Family.CHARGE)
            return charge(design, type, by, materials, step);
        Map<Fig, Float> f = new EnumMap<>(Fig.class);
        float worn = ModularTuning.WEAR_FACTOR[Math.min(step, ModularTuning.WEAR_FACTOR.length - 1)];
        Material edge = by.get(type.wearRole());
        boolean armour = type.family() == ModularType.Family.ARMOUR;
        Part setPart = design.parts().stream().filter(p -> type.roles().get(p.role()).set()).findFirst().orElse(null);
        // Armour studs cover a share of the plate by how many there are; a tip is the whole edge.
        float studs = setPart == null ? 0 : Math.min(ModularTuning.STUD_MAX, setPart.count() * ModularTuning.STUD_SHARE);
        Zone zone = section(zone(edge, setPart == null ? null : by.get(setPart.role()), armour ? studs : 1f,
                armour ? studs : ModularTuning.TIP_SHARE), type.roles().get(type.wearRole()).section(wearPart)
                * (setPart == null ? 1f : type.roles().get(setPart.role()).section(setPart)),
                // An attunement can cost the part toughness (keen) or buy it back (ward on a plate): flaws, as forging leaves them.
                Math.max(0, Math.min(ForgeRhythm.MAX_FLAW, wearPart.flaw() + Attunements.flawShift(wearPart, type.roles().get(type.wearRole()).name()))));
        Material coatMetal = coat == null ? null : materials.get(coat.metal());
        float hold = coatMetal == null ? 0f : coatHold(coat);
        // A coat on armour is a skin of its metal over the whole plate; on a tool the edge is ground
        // bare, so a coat protects the flats from water and leaves the bite alone.
        if (armour)
            zone = mix(zone, edge, coatMetal, hold * ModularTuning.COAT_SURFACE_SHARE, hold * ModularTuning.COAT_SURFACE_SHARE);
        float comp = zone.bite();
        f.put(Fig.CORROSION, 1f - (1f - edge.corrosionResistance().orElse(0f))
                * (1f - hold * (coatMetal == null ? 0f : coatMetal.corrosionResistance().orElse(0f))));
        float durabilityComp = armour ? zone.wear() + ModularTuning.ARMOUR_DURABILITY_FLOOR : zone.wear();
        f.put(Fig.DURABILITY, (float) Math.max(1, Math.round(ModularTuning.DURABILITY_K * durabilityComp * type.wearLitres())));
        f.put(Fig.ENCHANTABILITY, (float) enchantability(by.get(type.primary()) != null ? by.get(type.primary()) : edge));
        f.put(Fig.HARDNESS, zone.hardness());
        f.put(Fig.EDGE_TOUGHNESS, zone.toughness());
        switch (type.family()) {
            case DIG, BLADE, MACE, SPEAR, SHEARS -> melee(design, type, by, zone, f, worn);
            case BOW, CROSSBOW -> {
                float drive = 0;
                float string = 0;
                float total = 0;
                for (Part p : design.parts()) {
                    ModularType.Role r = type.roles().get(p.role());
                    total += mass(r, p, by.get(p.role()));
                    if (r.name().equals("string"))
                        string = strength(by.get(p.role()));
                    else if (r.name().equals("limbs") || r.name().equals("prod"))
                        drive = strength(by.get(p.role()));
                }
                float held = Math.min(drive, ModularTuning.STRING_SHARE * string);
                float ratio = held / ModularTuning.DRAW_REFERENCE_STRENGTH;
                f.put(Fig.MASS, total);
                f.put(Fig.VELOCITY, clamp((float) Math.pow(ratio, ModularTuning.VELOCITY_EXPONENT) * worn,
                        ModularTuning.VELOCITY_MIN, ModularTuning.VELOCITY_MAX));
                f.put(Fig.DRAW, clamp(ModularTuning.DRAW_REFERENCE_TICKS * (float) Math.sqrt(ratio),
                        ModularTuning.DRAW_MIN_TICKS, ModularTuning.DRAW_MAX_TICKS));
                f.put(Fig.JOINT, string > 0 ? drive / (ModularTuning.STRING_SHARE * string) : 0f);
            }
            case ARMOUR -> {
                Part plate = wearPart;
                float share = comp / (comp + ModularTuning.ARMOUR_HALF_COMPETENCE);
                float cover = Math.min(1f, plate.count() / (float) type.canonicalCounts().getOrDefault(type.wearRole(), plate.count()));
                float points = type.armourMax() * share * type.roles().get(type.wearRole()).factor(plate.form()) * cover;
                float total = 0;
                for (Part p : design.parts()) {
                    ModularType.Role r = type.roles().get(p.role());
                    total += mass(r, p, by.get(p.role()));
                    if (r.name().equals("lining")) {
                        float c = competence(by.get(p.role()));
                        points += type.armourMax() * ModularTuning.LINING_SHARE * c / (c + ModularTuning.ARMOUR_HALF_COMPETENCE);
                    }
                }
                f.put(Fig.MASS, total);
                f.put(Fig.ARMOUR, points * worn);
                f.put(Fig.ARMOUR_TOUGHNESS, ModularTuning.TOUGHNESS_MAX * Math.max(0f,
                        (share - ModularTuning.TOUGHNESS_FROM_SHARE) / (1 - ModularTuning.TOUGHNESS_FROM_SHARE)));
                // Boots meet the ground: a coat grippier than the plate under it plants the foot, a slicker one loses it.
                float grip = type.slot() != ModularType.Slot.FEET || coatMetal == null ? 0f
                        : hold * ModularTuning.COAT_GRIP_PER_FRICTION * (coatMetal.mechanical().friction().orElse(0f)
                        - edge.mechanical().friction().orElse(0f));
                f.put(Fig.KNOCKBACK_RESISTANCE, clamp((edge.density().muPerMb() - ModularTuning.KNOCKBACK_DENSITY_FLOOR)
                        * ModularTuning.KNOCKBACK_PER_DENSITY + grip, 0f, ModularTuning.KNOCKBACK_MAX));
                f.put(Fig.SLOW, clamp(Math.max(0f, total - ModularTuning.SLOW_FREE_MASS) * ModularTuning.SLOW_PER_KG,
                        0f, ModularTuning.SLOW_MAX));
            }
            case SHIELD -> {
                float total = 0;
                float held = 1f;
                for (Part p : design.parts()) {
                    total += mass(type.roles().get(p.role()), p, by.get(p.role()));
                    held *= param(type.roles().get(p.role()), p, "capacity_mult");
                }
                f.put(Fig.MASS, total);
                f.put(Fig.CAPACITY, (ModularTuning.SHIELD_BASE_CAPACITY + comp * worn) * held);
            }
            default -> { }
        }
        return Optional.of(ModularFigures.of(f, step));
    }

    /**
     * A gun: the physics of {@link GunSpec} at the reference load, written as figures. The round-dependent half
     * is {@link GunSpec#bake(Map, io.github.soundgoodizerfan.feedback.weapon.Cartridge)}, run at the shot and on
     * the spec sheet. Empty if the parts are not a gun the physics can fire (an action the feed does not suit).
     */
    private static Optional<ModularFigures> gun(ModularDesign design, ModularType type, Map<String, Material> by,
                                                Map<ResourceLocation, Material> materials, int step) {
        Optional<GunFigures> baked = GunSpec.of(design, type, materials).flatMap(spec -> spec.bake(materials));
        if (baked.isEmpty())
            return Optional.empty();
        Map<Fig, Float> f = new EnumMap<>(Fig.class);
        f.putAll(baked.get().figs());
        f.put(Fig.ENCHANTABILITY, (float) enchantability(by.get(type.primary())));
        return Optional.of(ModularFigures.of(f, step));
    }

    /**
     * A charge: its filler is a blend of propellants by count and its casing holds the gas, and
     * {@link io.github.soundgoodizerfan.feedback.weapon.Blast} reads both. Empty if the filler is not a propellant,
     * so the Tool Table refuses a charge of sugar.
     */
    private static Optional<ModularFigures> charge(ModularDesign design, ModularType type, Map<String, Material> by,
                                                   Map<ResourceLocation, Material> materials, int step) {
        List<io.github.soundgoodizerfan.feedback.weapon.Cartridge.Charge> filler = new java.util.ArrayList<>();
        for (Part p : design.parts())
            if (!p.role().equals(type.wearRole()))
                filler.add(new io.github.soundgoodizerfan.feedback.weapon.Cartridge.Charge(p.material(), p.count()));
        Optional<io.github.soundgoodizerfan.feedback.weapon.Cartridge.Blend> blend =
                io.github.soundgoodizerfan.feedback.weapon.Cartridge.Blend.of(materials, filler).filter(b -> !b.coil());
        if (blend.isEmpty() || blend.get().total() > FTuning.DEMO_FILLER_MAX)
            return Optional.empty();
        Map<Fig, Float> f = new EnumMap<>(Fig.class);
        f.put(Fig.BLAST, io.github.soundgoodizerfan.feedback.weapon.Blast.power(blend.get(), by.get(type.wearRole())));
        f.put(Fig.BURNED, blend.get().burned());
        f.put(Fig.PACKED, (float) blend.get().total());
        float mass = 0;
        for (Part p : design.parts())
            mass += mass(type.roles().get(p.role()), p, materials.get(p.material()));
        f.put(Fig.MASS, mass);
        return Optional.of(ModularFigures.of(f, step));
    }

    /** The number a part's shape names under {@code name} (a binding's joint, a rim's capacity), as a multiple: 1 for none. */
    static float param(ModularType.Role role, Part part, String name) {
        return role.variant(part).map(v -> v.params().getOrDefault(name, 1f)).orElse(1f);
    }

    static float mass(ModularType.Role role, Part part, Material m) {
        return role.litres() * part.count() * m.density().muPerMb() * role.factor(part.form()) * role.kept(part);
    }

    /**
     * {@code zone} as the part was made: a section {@code thickness} times the standard (thin bites,
     * thick resists chipping), and a forging flaw, each step of which costs toughness.
     */
    private static Zone section(Zone zone, float thickness, int flaw) {
        if (thickness == 1f && flaw == 0)
            return zone;
        return new Zone(zone.hardness(), zone.toughness() * thickness * (1f - ModularTuning.FLAW_TOUGHNESS_LOSS * flaw),
                zone.bite() * (float) Math.pow(thickness, -ModularTuning.SECTION_BITE_EXPONENT),
                zone.wear() * (1f - ModularTuning.FLAW_DURABILITY_LOSS * flaw));
    }

    /**
     * Mass, balance, swing, blow, bite, damage, dig speed, reach and joint for a hand-swung head on a
     * handle. A twin-headed tool has two heads ({@link ModularType#strikers}); each is struck, dug
     * and worn as its own kind says ({@link ModularHeads}), the swing is the whole tool's, so a
     * second head costs speed and impact with no figure written for it (spec 5.3), and the blow
     * that lands is the better of the two.
     */
    private static void melee(ModularDesign design, ModularType type, Map<String, Material> by, Zone zone,
                              Map<Fig, Float> f, float worn) {
        List<String> heads = type.strikers().isEmpty() ? List.of(type.wearRole()) : type.strikers();
        float rodLength = 0;
        for (Part p : design.parts()) {
            ModularType.Role r = type.roles().get(p.role());
            rodLength += r.length() * p.count();
        }
        float total = 0;
        float feltTotal = 0;
        float inertia = 0;
        float moment = 0;
        float struck = 0;
        float rodMass = 0;
        float reachLength = rodLength;
        Map<String, Float> headMass = new java.util.HashMap<>();
        for (Part p : design.parts()) {
            ModularType.Role r = type.roles().get(p.role());
            float m = mass(r, p, by.get(p.role()));
            total += m;
            // What the hand feels: a featherlight part counts for less (spec 2.1a); the blow and MASS stay real.
            float felt = m * MaterialPerks.heft(by.get(p.role()));
            feltTotal += felt;
            if (r.rod()) {
                inertia += felt * rodLength * rodLength / 3;
                moment += felt * rodLength / 2;
                rodMass += m;
                continue;
            }
            float d = r.at() + r.centre(p) + (r.rides() ? rodLength : 0);
            inertia += felt * d * d;
            moment += felt * d;
            reachLength = Math.max(reachLength, r.at() + r.reach(p) + (r.rides() ? rodLength : 0));
            if (heads.contains(p.role())) {
                struck += m;
                headMass.put(p.role(), m);
            }
        }
        if (struck == 0)
            struck = rodMass;
        float swing = clamp(ModularTuning.SWING_K / (float) Math.pow(inertia + ModularTuning.BALANCE_K * moment,
                ModularTuning.SWING_EXPONENT), ModularTuning.SWING_MIN, ModularTuning.SWING_MAX);
        float tip = reachLength * swing * (float) Math.PI;
        float bestDamage = -1;
        float wear = 0;
        for (int i = 0; i < heads.size(); i++) {
            String role = heads.get(i);
            Part head = design.part(role);
            Zone z = i == 0 ? zone : head == null || by.get(role) == null ? zone
                    : section(zone(by.get(role), null, 1f, 1f), type.roles().get(role).section(head), head.flaw());
            float mass = headMass.getOrDefault(role, rodMass);
            float impact = 0.5f * (mass + ModularTuning.ROD_STRIKE_SHARE * rodMass) * tip * tip;
            float bite = bite(z.bite(), design.edge());
            float strike = type.strike();
            if (heads.size() > 1 && head != null)
                strike = ModularHeads.of(ModularHeads.kindOf(head, type.roles().get(role))).map(ModularHeads::strike).orElse(1f);
            if (head != null)
                strike *= type.roles().get(role).strike(head);
            float damage = 1 + ModularTuning.DAMAGE_K * strike * bite * (float) Math.sqrt(impact) * worn;
            float dig = ModularTuning.DIG_K * bite * (float) Math.sqrt(impact) * worn;
            wear += z.wear();
            if (i > 0) {
                f.put(Fig.DIG_SPEED_OFF, dig);
                f.put(Fig.HARDNESS_OFF, z.hardness());
                f.put(Fig.EDGE_TOUGHNESS_OFF, z.toughness());
            } else {
                f.put(Fig.DIG_SPEED, dig);
            }
            if (damage > bestDamage) {
                bestDamage = damage;
                f.put(Fig.IMPACT, impact);
                f.put(Fig.BITE, bite);
                f.put(Fig.DAMAGE, damage);
            }
        }
        if (heads.size() > 1)
            f.put(Fig.DURABILITY, (float) Math.max(1, Math.round(ModularTuning.DURABILITY_K * wear * type.wearLitres())));
        f.put(Fig.MASS, total);
        f.put(Fig.BALANCE, feltTotal > 0 && reachLength > 0 ? Math.min(1f, moment / (feltTotal * reachLength)) : 0f);
        f.put(Fig.SWING, swing);
        f.put(Fig.ATTACK_SPEED, swing * worn);
        f.put(Fig.REACH, Math.max(0, reachLength - ModularTuning.REACH_REFERENCE) * ModularTuning.REACH_PER_METRE);
        f.put(Fig.JOINT, joint(design, type, by, struck, reachLength, rodLength, heads));
    }

    /** Leverage on the joint over what the handle, head and any binding carry together. */
    private static float joint(ModularDesign design, ModularType type, Map<String, Material> by, float struck,
                               float reachLength, float rodLength, List<String> heads) {
        if (rodLength == 0)
            return 0;
        float weakest = Float.MAX_VALUE;
        float binding = 0;
        for (Part p : design.parts()) {
            ModularType.Role r = type.roles().get(p.role());
            Material m = by.get(p.role());
            if (r.name().equals("binding"))
                binding = (m.kind().filter(k -> k == Material.Kind.METAL).isPresent()
                        ? ModularTuning.PINNED_SHARE : ModularTuning.WRAPPED_SHARE) * strength(m) * param(r, p, "joint_mult");
            else if (r.rod() || heads.contains(p.role()))
                weakest = Math.min(weakest, strength(m));
        }
        float capacity = ModularTuning.JOINT_K * (weakest + binding);
        return capacity > 0 ? struck * reachLength / capacity : 0;
    }

    private static float clamp(float v, float lo, float hi) {
        return Math.min(hi, Math.max(lo, v));
    }
}
