// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.Haste;
import io.github.soundgoodizerfan.feedback.machine.battery.BatteryStack;
import io.github.soundgoodizerfan.feedback.modular.attunement.MomentumKinds;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.weapon.FireMode;
import io.github.soundgoodizerfan.feedback.weapon.GunFigures;
import io.github.soundgoodizerfan.feedback.weapon.GunItem;
import io.github.soundgoodizerfan.feedback.weapon.GunMechanism;
import io.github.soundgoodizerfan.feedback.weapon.GunShots;
import io.github.soundgoodizerfan.feedback.weapon.GunSound;
import io.github.soundgoodizerfan.feedback.weapon.GunStance;

/**
 * An electric gun's mechanism: a cell in the gun gives the shot's energy and a metal nugget gives the slug. It prepares the next shot
 * once ({@link #prepare}, for the client's prediction and the server's pull alike, so the two agree), and a pull spends the nugget and
 * the draw, books the mistakes, and launches the slug through {@link GunShots#fire} like any gun's round.
 *
 * <p>A plasma barrel's shot is a fast round that loses its speed quickly ({@link ModularTuning#EGUN_PLASMA_DRAG}): a short bolt that
 * hits weaker with distance and sets what it hits alight.
 */
public final class ElectricMechanism implements GunMechanism {

    public static final ElectricMechanism INSTANCE = new ElectricMechanism();

    private ElectricMechanism() {
    }

    /** The next shot, worked out and not yet spent. */
    private record Prepared(ElectricGunSpec spec, ResourceLocation metal, Material material, boolean fresh, ItemStack nugget,
                            ItemStack cell, CompoundTag drawn, float volts, GunFigures figures) {
    }

    /**
     * The next shot of {@code gun} for {@code shooter}, or null if it cannot fire: no spec, no cell or a flat one, no nugget the
     * barrel can drive. Changes nothing.
     */
    private static Prepared prepare(Player shooter, ItemStack gun, Map<ResourceLocation, Material> materials, long now) {
        ElectricGunSpec spec = ModularElectricGun.spec(gun).orElse(null);
        ItemStack cell = ModularElectricGun.cell(gun);
        if (spec == null || cell.isEmpty())
            return null;
        CompoundTag drawn = BatteryStack.state(cell).copy();
        float volts = BatteryStack.terminalVolts(drawn);
        if (!BatteryStack.ready(drawn) || BatteryStack.charge(drawn) <= 0f || volts <= 0f)
            return null;
        // The slug: what is chambered from the last nugget, else a fresh nugget of a metal the barrel can drive.
        ResourceLocation metal = null;
        ItemStack nugget = ItemStack.EMPTY;
        boolean fresh = false;
        if (ModularElectricGun.slugs(gun) > 0) {
            CustomData data = gun.get(DataComponents.CUSTOM_DATA);
            metal = data == null ? null : ResourceLocation.tryParse(data.copyTag().getString(ModularElectricGun.METAL));
        }
        if (metal == null || !materials.containsKey(metal)) {
            Optional<Map.Entry<ResourceLocation, ItemStack>> found = ModularElectricGun.nugget(shooter, spec, materials);
            if (found.isEmpty())
                return null;
            metal = found.get().getKey();
            nugget = found.get().getValue();
            fresh = true;
        }
        Material m = materials.get(metal);
        float density = m.density().muPerMb();
        // The draw: the energy a shot takes over the volts; a weak cell gives the share it has.
        float needed = spec.energyIn() / (volts * ModularTuning.EGUN_J_PER_VAMPTICK);
        float took = BatteryStack.draw(drawn, needed);
        float fraction = needed <= 0f ? 0f : took / needed;
        float heat = ModularElectricGun.heat(gun, now, spec.shed());
        // An eddy sleeve drains a conductor slug's energy after it leaves the barrel, so it leaves slower.
        float eddy = spec.kind() == ElectricGunSpec.Kind.PLASMA ? 1f : 1f - Math.min(0.9f, ModularTuning.EGUN_EDDY_BRAKE * spec.eddy());
        float speedMult = RangedDispatch.launch(gun, 1f) * eddy;
        // A lit reticle helps where it is dark.
        float spreadMult = spec.darkAt() > 0f && shooter.level().getMaxLocalRawBrightness(shooter.blockPosition()) <= spec.darkAt()
                ? 1f - spec.darkCut() : 1f;
        ModularFigures baked = ModularStacks.figures(gun);
        float gunKg = baked == null || baked.get(ModularFigures.Fig.MASS) <= 0f ? spec.barrelMass() : baked.get(ModularFigures.Fig.MASS);
        GunFigures figures = spec.figures(density, fraction, gunKg, heat, speedMult, spreadMult);
        figures = io.github.soundgoodizerfan.feedback.modular.ModularTraits.gunAimed(shooter, gun, figures);
        if (GunItem.dualWielding(shooter))
            figures = figures.oneHanded();
        figures = figures.withCycle(1f / Haste.rate(shooter));
        return new Prepared(spec, metal, m, fresh, nugget, cell, drawn, volts, figures);
    }

    @Override
    public boolean built(ItemStack gun) {
        return ModularElectricGun.spec(gun).isPresent();
    }

    @Override
    public boolean trigger(ServerLevel level, Player shooter, ItemStack gun, float yaw, float pitch, long nowMs, boolean aimed, InteractionHand hand) {
        if (ModularStacks.broken(gun))
            return false;
        long now = level.getGameTime();
        Prepared p = prepare(shooter, gun, io.github.soundgoodizerfan.feedback.process.MaterialTable.get().materials(), now);
        if (p == null || !GunShots.accept(shooter, p.figures(), nowMs, hand))
            return false;
        ElectricGunSpec spec = p.spec();
        if (p.fresh()) {
            if (!shooter.getAbilities().instabuild)
                p.nugget().shrink(1);
            ModularElectricGun.chamber(gun, spec.perNugget(), p.metal());
        }
        p.cell().set(FDataComponents.BATTERY.get(), p.drawn());
        ModularElectricGun.setCell(gun, p.cell());
        if (shooter instanceof net.minecraft.server.level.ServerPlayer server)
            ModularElectricGun.mistakes(server, gun, spec, p.volts(), hand);

        boolean plasma = spec.kind() == ElectricGunSpec.Kind.PLASMA;
        GunFigures figures = p.figures().withSpread(GunStance.of(shooter, aimed) * MomentumKinds.braceSpread(shooter, gun));
        var flight = plasma
                ? new GunShots.Flight(ModularTuning.EGUN_PLASMA_DRAG / FTuning.GUN_DRAG_PER_TICK, 1f, 1f, 1f, Map.of(), null, java.util.Set.of(), null)
                : GunShots.Flight.PLAIN;
        GunShots.fire(level, shooter, figures, yaw, pitch, plasma, plasma, 1, p.material().hardness().value(), gun, 0f, false, flight);
        if (!plasma && ModularElectricGun.slugs(gun) > 0)
            ModularElectricGun.chamber(gun, ModularElectricGun.slugs(gun) - 1, p.metal());

        // Heat and the recovery it slows.
        float heat = ModularElectricGun.heat(gun, now, spec.shed())
                + ModularTuning.EGUN_HEAT_PER_SHOT * (1f - spec.efficiency()) * spec.heatGain() * spec.heatMult();
        ModularElectricGun.setHeat(gun, heat, now);
        GunShots.bookShot(level, shooter, gun, figures, Math.min(1f, heat), hand, aimed);
        return true;
    }

    /** A mob has no cell to carry and no nugget pocket. */
    @Override
    public boolean triggerMob(ServerLevel level, LivingEntity mob, ItemStack gun, long nowGameTick, float yaw, float pitch) {
        return false;
    }

    @Override
    public GunFigures predict(Player player, ItemStack gun, Level level, Map<ResourceLocation, Material> materials, int hand) {
        if (ModularStacks.broken(gun))
            return null;
        Prepared p = prepare(player, gun, materials, level.getGameTime());
        return p == null ? null : p.figures();
    }

    @Override
    public float sightZoom(ItemStack gun) {
        return 1f;
    }

    @Override
    public int raiseMs(ItemStack gun) {
        return 250;
    }

    @Override
    public FireMode allow(ItemStack gun, FireMode set) {
        return set;
    }

    @Override
    public FireMode next(ItemStack gun, FireMode current) {
        FireMode[] all = FireMode.values();
        return all[(current.ordinal() + 1) % all.length];
    }

    /** The arc and the report: a crack from a rail, a thump from a coil, a hiss from plasma. */
    @Override
    public List<GunSound.Layer> sound(ItemStack gun, GunFigures figures, RandomSource random) {
        ElectricGunSpec.Kind kind = ModularElectricGun.spec(gun).map(ElectricGunSpec::kind).orElse(ElectricGunSpec.Kind.COIL);
        SoundEvent event = switch (kind) {
            case RAIL -> SoundEvents.FIREWORK_ROCKET_BLAST;
            case COIL -> SoundEvents.PISTON_EXTEND;
            case PLASMA -> SoundEvents.BLAZE_SHOOT;
        };
        float jitter = 0.95f + random.nextFloat() * 0.1f;
        return List.of(new GunSound.Layer(event, ModularTuning.EGUN_VOLUME * figures.report() * 2f,
                (kind == ElectricGunSpec.Kind.RAIL ? 1.6f : 1.1f) * jitter));
    }

    @Override
    public SimpleParticleType muzzleParticle(ItemStack gun, GunFigures figures) {
        return ParticleTypes.ELECTRIC_SPARK;
    }
}
