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
package io.github.soundgoodizerfan.feedback.weapon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.unit.Mu;
import io.github.soundgoodizerfan.feedback.core.unit.Pu;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.Optional;

/**
 * A gun as designed ({@code feedback_guns_spec.md} sections 2 and 3): four settings, and
 * {@link #bake} derives every figure the gun has from them and its material, once.
 *
 * <p>The derivation is the whole point and is deliberately a pure function: the same design in
 * the same materials is the same {@link GunFigures}, so a gun crafted from any metal needs no
 * registry row (spec 2.1 -- the thing TacZ's rows-by-id could not do). Nothing keys on an id.
 *
 * <p>What it models is interior ballistics cut down to three things a player can act on: the
 * chamber's rating (its metal and its wall), the pressure the charge makes, and how long the
 * barrel is that the pressure acts over (spec 3.2 and 3.3).
 *
 * @param boreMm   bore, in mm; a design parameter, not a quantity any system trades in.
 * @param barrel   barrel length, a band (spec 3.3).
 * @param wall     chamber wall, a band.
 * @param material the metal of the barrel and chamber, a material id (never an item id).
 */
public record GunDesign(int boreMm, Barrel barrel, Wall wall, ResourceLocation material) {

    public static final int MIN_BORE_MM = 5, MAX_BORE_MM = 20;

    /** Barrel length, named options not a free scalar (philosophy 3). */
    public enum Barrel implements StringRepresentable {
        SHORT("short", FTuning.GUN_BARREL_SHORT_M, FTuning.GUN_INGOTS_BARREL_SHORT),
        MEDIUM("medium", FTuning.GUN_BARREL_MEDIUM_M, FTuning.GUN_INGOTS_BARREL_MEDIUM),
        LONG("long", FTuning.GUN_BARREL_LONG_M, FTuning.GUN_INGOTS_BARREL_LONG);

        public static final Codec<Barrel> CODEC = StringRepresentable.fromEnum(Barrel::values);
        public static final StreamCodec<ByteBuf, Barrel> STREAM_CODEC =
                ByteBufCodecs.idMapper(i -> values()[i], Barrel::ordinal);

        private final String id;
        private final float metres;
        private final int ingots;

        Barrel(String id, float metres, int ingots) {
            this.id = id;
            this.metres = metres;
            this.ingots = ingots;
        }

        public int ingots() {
            return ingots;
        }

        public float metres() {
            return metres;
        }

        @Override
        public String getSerializedName() {
            return id;
        }
    }

    /** Chamber wall thickness, a band. */
    public enum Wall implements StringRepresentable {
        THIN("thin", FTuning.GUN_WALL_THIN_MM, FTuning.GUN_INGOTS_WALL_THIN),
        STANDARD("standard", FTuning.GUN_WALL_STANDARD_MM, FTuning.GUN_INGOTS_WALL_STANDARD),
        THICK("thick", FTuning.GUN_WALL_THICK_MM, FTuning.GUN_INGOTS_WALL_THICK);

        public static final Codec<Wall> CODEC = StringRepresentable.fromEnum(Wall::values);
        public static final StreamCodec<ByteBuf, Wall> STREAM_CODEC =
                ByteBufCodecs.idMapper(i -> values()[i], Wall::ordinal);

        private final String id;
        private final float mm;
        private final int ingots;

        Wall(String id, float mm, int ingots) {
            this.id = id;
            this.mm = mm;
            this.ingots = ingots;
        }

        public int ingots() {
            return ingots;
        }

        public float mm() {
            return mm;
        }

        @Override
        public String getSerializedName() {
            return id;
        }
    }

    public static final Codec<GunDesign> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(MIN_BORE_MM, MAX_BORE_MM).fieldOf("bore").forGetter(GunDesign::boreMm),
            Barrel.CODEC.fieldOf("barrel").forGetter(GunDesign::barrel),
            Wall.CODEC.fieldOf("wall").forGetter(GunDesign::wall),
            ResourceLocation.CODEC.fieldOf("material").forGetter(GunDesign::material)
    ).apply(i, GunDesign::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, GunDesign> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, GunDesign::boreMm,
            Barrel.STREAM_CODEC, GunDesign::barrel,
            Wall.STREAM_CODEC, GunDesign::wall,
            ResourceLocation.STREAM_CODEC, GunDesign::material,
            GunDesign::new);

    /** The bore pulled back into range, so a design is always buildable. */
    public GunDesign {
        boreMm = Mth.clamp(boreMm, MIN_BORE_MM, MAX_BORE_MM);
    }

    /**
     * Whether a gun can be made of {@code m}: a metal, weighed, with a yield strength to read the
     * chamber's rating from. The engine's rule ({@code EngineDesign.buildable}), one figure
     * short of it: a gun needs no ingot until the Stand pays for one.
     */
    public static boolean buildable(Material m) {
        return m.kind().orElse(null) == Material.Kind.METAL && m.density().muPerMb() > 0
                && m.mechanical().yieldStrength().isPresent();
    }

    /**
     * Every figure of this gun, derived once.
     *
     * <p>The rating is thin-wall hoop stress turned round: a vessel of radius r and wall t holds
     * {@code yield x t / r}. Peak pressure is the propellant's, and muzzle energy is the mean
     * pressure acting on the bore's area over the barrel's length -- so a longer barrel is more
     * speed and more weight, and a bigger bore more energy and more kick, with neither
     * dominating (spec 3.3).
     *
     * @param m          the barrel and chamber metal.
     * @param projectile the projectile's material; its density is what the bullet weighs.
     * @return empty if {@code m} cannot make a gun or {@code projectile} has not been weighed.
     */
    public Optional<GunFigures> bake(Material m, Material projectile) {
        return bake(m, projectile, Load.REFERENCE);
    }

    /**
     * What a round asks of a chamber and a barrel: the gas pressure it peaks at, and how much
     * barrel it takes to burn ({@link Material.Propellant}).
     */
    public record Load(float peakPressure, float burnRatePerM) {
        /** Gunpowder at its nominal charge: the figures a gun's spec sheet is read against. */
        public static final Load REFERENCE = new Load(FTuning.GUN_PROPELLANT_PEAK_PU, FTuning.GUN_PROPELLANT_BURN_RATE_PER_M);
    }

    /**
     * The figures of this gun firing one particular round, derived now rather than stored: a gun
     * is a baked design, a round is what it is fed (spec 2), and nothing keys on either's id.
     *
     * @return empty if the round is for another bore, or names a material that cannot fill its
     *         part.
     */
    public Optional<GunFigures> bake(Map<ResourceLocation, Material> materials, Cartridge round) {
        if (round.boreMm() != boreMm)
            return Optional.empty();
        Material metal = materials.get(material), bullet = materials.get(round.projectile());
        if (metal == null || bullet == null)
            return Optional.empty();
        return round.load(materials).flatMap(load -> bake(metal, bullet, load));
    }

    public Optional<GunFigures> bake(Material m, Material projectile, Load load) {
        if (!buildable(m) || projectile.density().muPerMb() <= 0)
            return Optional.empty();
        double boreM = boreMm / 1000.0, barrelM = barrel.metres(), wallM = wall.mm() / 1000.0;
        double area = Math.PI / 4 * boreM * boreM;

        float rating = m.mechanical().yieldStrength().get().value() * wall.mm() / (boreMm / 2f);
        float peak = load.peakPressure();

        // Only the part of the charge burned by the muzzle does work: a slow powder wants a long barrel.
        double burned = Math.min(1.0, load.burnRatePerM() * barrelM);
        // A chamber past its rating vents rather than delivers: the push is the pressure it held.
        double work = Math.min(peak, rating) * FTuning.GUN_PRESSURE_FILL * area * barrelM * burned * FTuning.GUN_WORK_PER_PU_M3;

        // 1 m3 is 1000 mB. A bullet is its bore's area over a length of some bores.
        double bulletMb = area * (boreM * FTuning.GUN_PROJECTILE_LENGTH_PER_BORE) * 1000;
        float bulletMu = (float) (bulletMb * projectile.density().muPerMb());
        float speedMs = (float) Math.sqrt(2 * work / bulletMu);

        float impulse = bulletMu * speedMs * (1 + FTuning.GUN_GAS_IMPULSE_FRACTION);

        // Barrel tube and receiver, in metal. A tube of outer and inner diameter, 1 m3 = 1000 mB.
        double outer = boreM + 2 * wallM;
        double tubeMb = Math.PI / 4 * (outer * outer - boreM * boreM) * (barrelM + 0.1) * 1000;
        float gunMu = (float) ((tubeMb + FTuning.GUN_FRAME_METAL_MB) * m.density().muPerMb());

        int cycleMs = Math.round(FTuning.GUN_CYCLE_BASE_MS + FTuning.GUN_CYCLE_MS_PER_IMPULSE * impulse);
        float spread = Math.max(FTuning.GUN_SPREAD_FLOOR_DEG,
                FTuning.GUN_SPREAD_AT_MEDIUM_DEG * FTuning.GUN_BARREL_MEDIUM_M / barrel.metres());
        float kick = FTuning.GUN_KICK_DEG_PER_IMPULSE * impulse;

        return Optional.of(new GunFigures(new Pu(peak), new Pu(rating), speedMs / 20f, (float) work,
                new Mu(bulletMu), new Mu(gunMu), impulse, cycleMs, spread, kick));
    }

    /** Whether the Stand may print a gun of {@code m}: buildable, and with an ingot to pay in. */
    public static boolean printable(ResourceLocation id, Material m) {
        return buildable(m) && m.item(id, "ingot").isPresent();
    }

    /** The material a sample item names for a gun -- any form of a printable metal -- if any. */
    public static Optional<ResourceLocation> sampleMaterial(Map<ResourceLocation, Material> materials, ItemStack sample) {
        if (sample.isEmpty())
            return Optional.empty();
        return Material.find(materials, sample.getItem()).filter(l -> printable(l.id(), l.material()))
                .map(Material.Located::id);
    }

    /** Ingots of its material it takes to build: the barrel's band and the wall's. */
    public int ingots() {
        return barrel.ingots() + wall.ingots();
    }

    /**
     * Bake against the material table, with lead as the projectile. Lead is a placeholder until
     * ammunition exists (spec step 3), and is named once, here.
     */
    public Optional<GunFigures> bake(Map<ResourceLocation, Material> materials) {
        Material metal = materials.get(material), lead = materials.get(Feedback.id("lead"));
        return metal == null || lead == null ? Optional.empty() : bake(metal, lead);
    }

    public GunDesign withMaterial(ResourceLocation m) {
        return new GunDesign(boreMm, barrel, wall, m);
    }

    /** A first design for the test pistol: a 9 mm, medium barrel, standard wall. */
    public static GunDesign starter(ResourceLocation material) {
        return new GunDesign(9, Barrel.MEDIUM, Wall.STANDARD, material);
    }
}
