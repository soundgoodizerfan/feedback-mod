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
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.unit.Pu;
import io.github.soundgoodizerfan.feedback.process.Material;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Optional;

/**
 * A loaded round ({@code feedback_guns_spec.md} section 3.1): a case, a charge and a projectile,
 * each a <em>material</em> and none an item id. A round is never "a 9 mm"; it is this record, and
 * a chamber accepts it by fit (the bore), so a pack's round fits a gun it was never listed in.
 *
 * @param boreMm     the die set the press seated it in; a gun takes only its own bore.
 * @param caseMaterial the metal it was drawn from.
 * @param propellant the material of the charge, one with a {@link Material.Propellant}.
 * @param charge     charge items seated, up to {@link FTuning#GUN_CHARGE_MAX}. More than nominal
 *                   is more pressure, and a press does not know it is done.
 * @param projectile the metal of the bullet.
 */
public record Cartridge(int boreMm, ResourceLocation caseMaterial, ResourceLocation propellant, int charge,
                        ResourceLocation projectile) {

    public static final Codec<Cartridge> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(GunDesign.MIN_BORE_MM, GunDesign.MAX_BORE_MM).fieldOf("bore").forGetter(Cartridge::boreMm),
            ResourceLocation.CODEC.fieldOf("case").forGetter(Cartridge::caseMaterial),
            ResourceLocation.CODEC.fieldOf("propellant").forGetter(Cartridge::propellant),
            Codec.intRange(1, FTuning.GUN_CHARGE_MAX).fieldOf("charge").forGetter(Cartridge::charge),
            ResourceLocation.CODEC.fieldOf("projectile").forGetter(Cartridge::projectile)
    ).apply(i, Cartridge::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, Cartridge> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Cartridge::boreMm,
            ResourceLocation.STREAM_CODEC, Cartridge::caseMaterial,
            ResourceLocation.STREAM_CODEC, Cartridge::propellant,
            ByteBufCodecs.VAR_INT, Cartridge::charge,
            ResourceLocation.STREAM_CODEC, Cartridge::projectile,
            Cartridge::new);

    /**
     * A round for a creative tab and tests: brass case, nominal gunpowder, lead bullet, in
     * {@code boreMm}. Names three materials, which is what a round is; no item is named.
     */
    public static Cartridge reference(int boreMm) {
        return new Cartridge(boreMm, ResourceLocation.fromNamespaceAndPath("feedback", "brass"),
                ResourceLocation.fromNamespaceAndPath("feedback", "gunpowder"), FTuning.GUN_CHARGE_NOMINAL,
                ResourceLocation.fromNamespaceAndPath("feedback", "lead"));
    }

    /** What this round asks of a chamber, from its propellant's figures and how much of it was seated. */
    public Optional<GunDesign.Load> load(Map<ResourceLocation, Material> materials) {
        Material p = materials.get(propellant);
        if (p == null || p.propellant().isEmpty())
            return Optional.empty();
        var prop = p.propellant().get();
        return Optional.of(new GunDesign.Load(
                prop.peakPressure().value() * charge / FTuning.GUN_CHARGE_NOMINAL, prop.burnRate()));
    }

    /**
     * What the case itself holds: the chamber's hoop-stress rule with the case drawn to scale
     * with its bore, so the bore cancels ({@link FTuning#GUN_CASE_WALL_PER_RADIUS}).
     * A round whose pressure is over this disagrees with its own spec sheet (spec 5) -- a
     * diagnostic the tooltip shows; nothing reads it yet.
     */
    public Optional<Pu> caseRating(Map<ResourceLocation, Material> materials) {
        Material c = materials.get(caseMaterial);
        if (c == null || c.mechanical().yieldStrength().isEmpty())
            return Optional.empty();
        return Optional.of(new Pu(c.mechanical().yieldStrength().get().value() * FTuning.GUN_CASE_WALL_PER_RADIUS));
    }
}
