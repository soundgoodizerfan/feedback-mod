// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.weapon;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.Optional;

/**
 * A gun's balance: the mass ahead of the grip against the mass behind it. A barrel, a muzzle device, a shroud and a
 * tube magazine are ahead; a grip, a trigger group, a stock and a buffer are behind; a receiver, slide, box magazine and
 * cylinder straddle the grip and count half each way. A gun heavier in front than behind is nose-heavy: the hand has to
 * hold the muzzle up, so the view sinks slowly while it is held ({@link #drift}), and spread and kick grow
 * ({@link #apply}). The figure is felt, never printed. A bipod braced on the ground takes the front's weight off the hand.
 */
public final class GunBalance {

    private GunBalance() {
    }

    /** Front mass and back mass of {@code spec}, in Mu; empty if a part's material is unknown. */
    public static Optional<float[]> masses(GunSpec spec, Map<ResourceLocation, Material> materials) {
        GunSpec.Parts p = spec.parts();
        Material barrel = materials.get(spec.material()), slide = materials.get(p.slide()), trigger = materials.get(p.trigger()),
                grip = materials.get(p.grip()), receiver = materials.get(p.receiver());
        if (barrel == null || slide == null || trigger == null || grip == null || receiver == null)
            return Optional.empty();
        float front = GunSpec.tubeMu(spec.barrel(), barrel) * FTuning.GUN_BARREL_FRONT_SHARE;
        float back = GunSpec.tubeMu(spec.barrel(), barrel) * (1 - FTuning.GUN_BARREL_FRONT_SHARE);
        if (p.second().isPresent()) {
            Material other = materials.get(p.second().get().material());
            if (other != null) {
                float tube = GunSpec.tubeMu(p.second().get().built(), other);
                front += tube * FTuning.GUN_BARREL_FRONT_SHARE;
                back += tube * (1 - FTuning.GUN_BARREL_FRONT_SHARE);
            }
        }
        float half = (FTuning.GUN_RECEIVER_METAL_MB * receiver.density().muPerMb() + FTuning.GUN_SLIDE_METAL_MB * slide.density().muPerMb()) / 2;
        front += half;
        back += half + FTuning.GUN_TRIGGER_METAL_MB * trigger.density().muPerMb() + FTuning.GUN_GRIP_METAL_MB * grip.density().muPerMb();
        if (p.feed().isPresent()) {
            Material feed = materials.get(p.feed().get());
            if (feed != null) {
                float mb = switch (spec.layout().feed()) {
                    case CYLINDER -> FTuning.GUN_CYLINDER_METAL_MB;
                    case TUBE -> FTuning.GUN_TUBE_METAL_MB;
                    case BELT -> FTuning.GUN_BELT_METAL_MB;
                    case BOX -> FTuning.GUN_BOX_METAL_MB;
                    default -> 0f;
                };
                float mu = mb * feed.density().muPerMb();
                if (spec.layout().feed() == GunLayout.Feed.TUBE) {
                    front += mu * FTuning.GUN_BARREL_FRONT_SHARE;
                    back += mu * (1 - FTuning.GUN_BARREL_FRONT_SHARE);
                } else {
                    front += mu / 2;
                    back += mu / 2;
                }
            }
        }
        for (var fitted : spec.fittings().entrySet()) {
            Material fm = materials.get(fitted.getValue());
            Optional<GunOption> option = GunOptions.find(fitted.getKey());
            if (fm == null || option.isEmpty())
                continue;
            float mu = option.get().metalMb() * fm.density().muPerMb();
            float ahead = switch (option.get().slot()) {
                case "muzzle", "lug", "shroud", "lagging", "sleeve", "bipod" -> 1f;
                case "buffer" -> 0f;
                default -> 0.5f;
            };
            front += mu * ahead;
            back += mu * (1 - ahead);
        }
        if (spec.has("gun_braced"))
            back += FTuning.GUN_STOCK_BACK_MU;
        return Optional.of(new float[] {front, back});
    }

    /** How nose-heavy the gun is: 0 when the back weighs at least the front, 1 when the front weighs twice the back, and on. */
    public static float nose(GunSpec spec) {
        Optional<float[]> m = masses(spec, MaterialTable.get().materials());
        if (m.isEmpty())
            return 0f;
        float front = m.get()[0], back = Math.max(0.01f, m.get()[1]);
        return Math.min(FTuning.GUN_NOSE_MAX, Math.max(0f, front / back - 1f));
    }

    /** The nose-heaviness {@code shooter} feels from {@code gun}: none while a bipod holds the muzzle up. */
    public static float felt(Player shooter, ItemStack gun) {
        Optional<GunSpec> spec = GunSpec.of(gun);
        if (spec.isEmpty())
            return 0f;
        if (shooter.isCrouching() && shooter.getDeltaMovement().horizontalDistanceSqr() <= 1.0E-3
                && spec.get().fittings().keySet().stream().anyMatch(o -> GunOptions.find(o).map(x -> x.slot().equals("bipod")).orElse(false)))
            return 0f;
        return nose(spec.get());
    }

    /** {@code f} with the spread and kick a nose-heavy gun adds. */
    public static GunFigures apply(Player shooter, ItemStack gun, GunFigures f) {
        float n = felt(shooter, gun);
        return n <= 0f ? f : f.withSpread(1 + FTuning.GUN_NOSE_SPREAD * n).withKick(1 + FTuning.GUN_NOSE_KICK * n);
    }

    /** Degrees per second the view sinks while the gun is held: the muzzle the hand must keep up. */
    public static float drift(Player holder, ItemStack gun) {
        return felt(holder, gun) * FTuning.GUN_NOSE_DRIFT_DEG_PER_S;
    }
}
