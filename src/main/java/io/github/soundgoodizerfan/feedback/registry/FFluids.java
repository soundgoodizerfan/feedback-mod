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
package io.github.soundgoodizerfan.feedback.registry;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Molten metal, and Steam, as real {@link Fluid}s rather than lightweight stand-ins.
 *
 * <h2>Never placed, on purpose, for now</h2>
 * {@link BaseFlowingFluid.Properties#bucket} and {@code #block} are left unset. Nothing here is
 * wrong for it -- {@code BaseFlowingFluid} treats both as optional and falls back to no bucket
 * and no world block -- it is simply not needed yet: nothing pours molten metal into the world,
 * nothing hands a player a bucket of it, and there is no pipe to carry it. It is registered as a
 * real, standard fluid anyway (a {@code SizedFluidIngredient}, a tank, a future pipe mod's own
 * fluid handling all already work against it) because pipes and tanks are committed, not
 * speculative -- see the "JEI take-over" / melting spec conversation. Placement and a bucket are
 * a real design question of their own (does it ignite things? flow? cool into a block?) and stay
 * `[OPEN]` rather than falling out of this as a side effect.
 *
 * <h2>Still and flowing, even though flowing is never reached</h2>
 * {@code BaseFlowingFluid} models a fluid as a source/flowing pair unconditionally, the same
 * shape {@code Fluids.WATER}/{@code Fluids.LAVA} use -- the flowing variant is simply dead code
 * until something places this fluid in the world, and costs one extra registry entry to keep
 * that door open rather than closed.
 */
public final class FFluids {

    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(
            net.minecraft.core.registries.Registries.FLUID, Feedback.MOD_ID);

    public static final DeferredHolder<Fluid, Fluid> MOLTEN_COPPER =
            FLUIDS.register("molten_copper", () -> new BaseFlowingFluid.Source(moltenCopper()));
    public static final DeferredHolder<Fluid, Fluid> MOLTEN_COPPER_FLOWING =
            FLUIDS.register("molten_copper_flowing", () -> new BaseFlowingFluid.Flowing(moltenCopper()));

    public static final DeferredHolder<Fluid, Fluid> MOLTEN_GOLD =
            FLUIDS.register("molten_gold", () -> new BaseFlowingFluid.Source(moltenGold()));
    public static final DeferredHolder<Fluid, Fluid> MOLTEN_GOLD_FLOWING =
            FLUIDS.register("molten_gold_flowing", () -> new BaseFlowingFluid.Flowing(moltenGold()));

    public static final DeferredHolder<Fluid, Fluid> MOLTEN_IRON =
            FLUIDS.register("molten_iron", () -> new BaseFlowingFluid.Source(moltenIron()));
    public static final DeferredHolder<Fluid, Fluid> MOLTEN_IRON_FLOWING =
            FLUIDS.register("molten_iron_flowing", () -> new BaseFlowingFluid.Flowing(moltenIron()));

    public static final DeferredHolder<Fluid, Fluid> MOLTEN_TIN =
            FLUIDS.register("molten_tin", () -> new BaseFlowingFluid.Source(moltenTin()));
    public static final DeferredHolder<Fluid, Fluid> MOLTEN_TIN_FLOWING =
            FLUIDS.register("molten_tin_flowing", () -> new BaseFlowingFluid.Flowing(moltenTin()));

    public static final DeferredHolder<Fluid, Fluid> MOLTEN_ZINC =
            FLUIDS.register("molten_zinc", () -> new BaseFlowingFluid.Source(moltenZinc()));
    public static final DeferredHolder<Fluid, Fluid> MOLTEN_ZINC_FLOWING =
            FLUIDS.register("molten_zinc_flowing", () -> new BaseFlowingFluid.Flowing(moltenZinc()));

    /** What a crucible's copper+tin mix becomes once {@code data/feedback/alloy/bronze.json}
     * matches -- see {@link io.github.soundgoodizerfan.feedback.process.AlloyMix}. */
    public static final DeferredHolder<Fluid, Fluid> MOLTEN_BRONZE =
            FLUIDS.register("molten_bronze", () -> new BaseFlowingFluid.Source(moltenBronze()));
    public static final DeferredHolder<Fluid, Fluid> MOLTEN_BRONZE_FLOWING =
            FLUIDS.register("molten_bronze_flowing", () -> new BaseFlowingFluid.Flowing(moltenBronze()));

    /** Copper+zinc's, the same way. */
    public static final DeferredHolder<Fluid, Fluid> MOLTEN_BRASS =
            FLUIDS.register("molten_brass", () -> new BaseFlowingFluid.Source(moltenBrass()));
    public static final DeferredHolder<Fluid, Fluid> MOLTEN_BRASS_FLOWING =
            FLUIDS.register("molten_brass_flowing", () -> new BaseFlowingFluid.Flowing(moltenBrass()));

    public static final DeferredHolder<Fluid, Fluid> MOLTEN_GLASS =
            FLUIDS.register("molten_glass", () -> new BaseFlowingFluid.Source(moltenGlass()));
    public static final DeferredHolder<Fluid, Fluid> MOLTEN_GLASS_FLOWING =
            FLUIDS.register("molten_glass_flowing", () -> new BaseFlowingFluid.Flowing(moltenGlass()));

    public static final DeferredHolder<Fluid, Fluid> MOLTEN_LEAD =
            FLUIDS.register("molten_lead", () -> new BaseFlowingFluid.Source(moltenLead()));
    public static final DeferredHolder<Fluid, Fluid> MOLTEN_LEAD_FLOWING =
            FLUIDS.register("molten_lead_flowing", () -> new BaseFlowingFluid.Flowing(moltenLead()));

    public static final DeferredHolder<Fluid, Fluid> MOLTEN_QUARTZ_GLASS =
            FLUIDS.register("molten_quartz_glass", () -> new BaseFlowingFluid.Source(moltenQuartzGlass()));
    public static final DeferredHolder<Fluid, Fluid> MOLTEN_QUARTZ_GLASS_FLOWING =
            FLUIDS.register("molten_quartz_glass_flowing", () -> new BaseFlowingFluid.Flowing(moltenQuartzGlass()));

    /**
     * The vehicle between a boiler and a steam engine, and nothing else -- see {@code FTuning}'s
     * {@code --- the boiler ---} section for why those are two blocks rather than one. Never
     * placed, same as the molten metals above and for the same reason: nothing pours it into the
     * world and there is no bucket yet, but it is a real {@code Fluid} with a real tank on each
     * side, which is the entire point -- a pipe mod, a hopper, or a future bucket all already work
     * against it without this class changing.
     */
    public static final DeferredHolder<Fluid, Fluid> STEAM =
            FLUIDS.register("steam", () -> new BaseFlowingFluid.Source(steam()));
    public static final DeferredHolder<Fluid, Fluid> STEAM_FLOWING =
            FLUIDS.register("steam_flowing", () -> new BaseFlowingFluid.Flowing(steam()));

    // Each pair's own properties instance, built lazily by the registry's own supplier -- by the
    // time either supplier actually runs (a registry event, well after class init), every
    // DeferredHolder referenced here is already assigned. Standard fluid-registration idiom.
    private static BaseFlowingFluid.Properties moltenCopper() {
        return new BaseFlowingFluid.Properties(FFluidTypes.MOLTEN_COPPER, MOLTEN_COPPER, MOLTEN_COPPER_FLOWING);
    }

    private static BaseFlowingFluid.Properties moltenGold() {
        return new BaseFlowingFluid.Properties(FFluidTypes.MOLTEN_GOLD, MOLTEN_GOLD, MOLTEN_GOLD_FLOWING);
    }

    private static BaseFlowingFluid.Properties moltenIron() {
        return new BaseFlowingFluid.Properties(FFluidTypes.MOLTEN_IRON, MOLTEN_IRON, MOLTEN_IRON_FLOWING);
    }

    private static BaseFlowingFluid.Properties moltenTin() {
        return new BaseFlowingFluid.Properties(FFluidTypes.MOLTEN_TIN, MOLTEN_TIN, MOLTEN_TIN_FLOWING);
    }

    private static BaseFlowingFluid.Properties moltenZinc() {
        return new BaseFlowingFluid.Properties(FFluidTypes.MOLTEN_ZINC, MOLTEN_ZINC, MOLTEN_ZINC_FLOWING);
    }

    private static BaseFlowingFluid.Properties moltenBronze() {
        return new BaseFlowingFluid.Properties(FFluidTypes.MOLTEN_BRONZE, MOLTEN_BRONZE, MOLTEN_BRONZE_FLOWING);
    }

    private static BaseFlowingFluid.Properties moltenBrass() {
        return new BaseFlowingFluid.Properties(FFluidTypes.MOLTEN_BRASS, MOLTEN_BRASS, MOLTEN_BRASS_FLOWING);
    }

    private static BaseFlowingFluid.Properties moltenGlass() {
        return new BaseFlowingFluid.Properties(FFluidTypes.MOLTEN_GLASS, MOLTEN_GLASS, MOLTEN_GLASS_FLOWING);
    }

    private static BaseFlowingFluid.Properties moltenLead() {
        return new BaseFlowingFluid.Properties(FFluidTypes.MOLTEN_LEAD, MOLTEN_LEAD, MOLTEN_LEAD_FLOWING);
    }

    private static BaseFlowingFluid.Properties moltenQuartzGlass() {
        return new BaseFlowingFluid.Properties(FFluidTypes.MOLTEN_QUARTZ_GLASS, MOLTEN_QUARTZ_GLASS, MOLTEN_QUARTZ_GLASS_FLOWING);
    }

    private static BaseFlowingFluid.Properties steam() {
        return new BaseFlowingFluid.Properties(FFluidTypes.STEAM, STEAM, STEAM_FLOWING);
    }

    private FFluids() {
    }

    public static void register(IEventBus modBus) {
        FLUIDS.register(modBus);
    }
}
