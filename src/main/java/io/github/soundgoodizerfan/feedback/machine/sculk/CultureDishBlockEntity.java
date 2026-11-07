// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.sculk;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.xp.Culture;
import io.github.soundgoodizerfan.feedback.core.xp.XpStore;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.core.fluid.FluidNetwork;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SculkShriekerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * The Culture Dish (roadmap 6.8): a living sculk culture that eats XP off the wire and breathes air from a neighbouring
 * gas holder. Its store is a sculk XP container whose size follows the culture's mass; the dish steps the
 * {@link Culture} once a second and does the world side of it: eating out of the store, bleeding orbs, a pulse the
 * sculk around can feel, and, if the culture is left ripe for long, a shrieker. A dish with no culture ticks nothing.
 */
public class CultureDishBlockEntity extends SculkNodeBlockEntity {

    private static final int STEP = 20;
    private static final int PULSE_EVERY = 600;
    private static final ResourceLocation SCULK = ResourceLocation.fromNamespaceAndPath(Feedback.MOD_ID, "sculk");

    private final Culture culture = new Culture();
    @Nullable
    private XpStore store;
    private float air;
    private Culture.Stage shown = Culture.Stage.EMPTY;
    private int sincePulse;

    public CultureDishBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.CULTURE_DISH.get(), pos, state);
    }

    public Culture culture() {
        return culture;
    }

    /** Seats a seed: a small culture with a small, empty store. */
    public void seed() {
        culture.start();
        store = shape(0);
        air = 0;
        changed();
    }

    /** What a harvest does to a ripe culture; returns whether a spare seed budded. */
    public boolean harvest() {
        boolean bud = culture.canBud() && level != null && level.random.nextFloat() < FTuning.CULTURE_SEED_CHANCE;
        culture.harvested();
        reshape();
        changed();
        return bud;
    }

    @Nullable
    @Override
    public XpStore xpStore() {
        return store;
    }

    @Override
    public void xpSet(XpStore s) {
        store = s;
        setChanged();
    }

    /** The air still held, in mB. For readouts. */
    public int airMb() {
        return Math.round(air);
    }

    /** The store a culture of the current mass has, holding {@code points}. */
    private XpStore shape(int points) {
        var spec = MaterialTable.get().materials().get(SCULK);
        var xp = spec == null ? null : spec.xpStore().orElse(null);
        float top = xp == null ? 20f : xp.topLevel();
        float cells = xp == null ? 250f : xp.capacitance();
        int max = Math.max(1, Math.round(cells * culture.mass()));
        XpStore s = XpStore.empty(max, top);
        return s.with(points);
    }

    /** Resizes the store to the culture's mass; points above the new size leave as orbs. */
    private void reshape() {
        if (!culture.alive()) {
            spill(store == null ? 0 : store.points());
            store = null;
            return;
        }
        int had = store == null ? 0 : store.points();
        store = shape(had);
        spill(had - store.points());
    }

    private void spill(int points) {
        if (points > 0 && level instanceof ServerLevel server)
            ExperienceOrb.award(server, Vec3.atCenterOf(worldPosition), points);
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide) {
            net.minecraft.world.level.block.state.BlockState s = getBlockState();
            int shownState = CultureDishBlock.look(culture.stage());
            if (s.hasProperty(CultureDishBlock.CULTURE) && s.getValue(CultureDishBlock.CULTURE) != shownState)
                level.setBlock(worldPosition, s.setValue(CultureDishBlock.CULTURE, shownState), 3);
            if (network() != null)
                network().wake();
        }
    }

    void tickServer() {
        if (level == null || !culture.alive() || (level.getGameTime() + worldPosition.hashCode()) % STEP != 0)
            return;
        pullAir();
        float need = FTuning.CULTURE_AIR_MB_PER_TICK * STEP;
        boolean breathing = air >= need;
        if (breathing)
            air -= need;

        int before = store == null ? 0 : store.points();
        int eaten = culture.step(STEP, before, breathing);
        float massBefore = culture.mass();
        int points = Math.max(0, before - eaten);
        int leaked = Math.min(points, culture.takeLeaked());
        points -= leaked;
        spill(leaked);
        if (store != null)
            store = store.with(points);
        reshape();

        if (culture.takeShriekerDue())
            bud();
        pulse();
        if (before != points || massBefore != culture.mass() || culture.stage() != shown)
            changed();
        shown = culture.stage();
        setChanged();
    }

    /** A faint pulse the first time a bad or ripe stage is reached, a loud one every half minute once it clots. */
    private void pulse() {
        Culture.Stage now = culture.stage();
        int event = 0;
        if (now == Culture.Stage.CLOTTED || now == Culture.Stage.SHRIEKING) {
            sincePulse += STEP;
            if (sincePulse >= PULSE_EVERY || shown != now) {
                event = 12;
                sincePulse = 0;
            }
        } else {
            sincePulse = 0;
            if (now != shown && now != Culture.Stage.GROWING)
                event = 2;
        }
        if (event > 0)
            level.gameEvent(event == 12 ? GameEvent.RESONATE_12 : GameEvent.RESONATE_2, worldPosition,
                    GameEvent.Context.of(getBlockState()));
    }

    /** A vanilla shrieker, set to summon, on a free face: the warden is vanilla's to bring. */
    private void bud() {
        for (Direction d : Direction.values()) {
            BlockPos at = worldPosition.relative(d);
            if (level.getBlockState(at).isAir()) {
                level.setBlock(at, Blocks.SCULK_SHRIEKER.defaultBlockState().setValue(SculkShriekerBlock.CAN_SUMMON, true), 3);
                culture.shriekerBudded();
                return;
            }
        }
    }

    /**
     * Breathes in from any neighbouring gas holder. What counts is the gas's material {@code oxygen_fraction}: the
     * buffer holds mB of pure oxygen, so a draw of air fills a fifth as much as the same draw of oxygen.
     */
    private void pullAir() {
        float space = FTuning.CULTURE_AIR_BUFFER_MB - air;
        if (space <= 0)
            return;
        for (Direction face : Direction.values()) {
            IFluidHandler neighbour = level.getCapability(Capabilities.FluidHandler.BLOCK, worldPosition.relative(face),
                    face.getOpposite());
            if (neighbour == null)
                continue;
            FluidStack offered = neighbour.drain(Math.max(1, (int) space), IFluidHandler.FluidAction.SIMULATE);
            if (offered.isEmpty() || !FluidNetwork.isGas(offered))
                continue;
            var m = MaterialTable.get().materials().get(BuiltInRegistries.FLUID.getKey(offered.getFluid()));
            float share = m == null ? 0f : m.combustion().map(c -> c.oxygenFraction()).orElse(0f);
            if (share <= 0f)
                continue;
            int want = Math.max(1, (int) (space / share));
            FluidStack got = neighbour.drain(offered.copyWithAmount(Math.min(want, offered.getAmount())),
                    IFluidHandler.FluidAction.EXECUTE);
            air += got.getAmount() * share;
            space -= got.getAmount() * share;
            if (space <= 0)
                return;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Culture", culture.save());
        tag.putFloat("Air", air);
        if (store != null)
            tag.putInt("Points", store.points());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        culture.load(tag.getCompound("Culture"));
        air = tag.getFloat("Air");
        store = culture.alive() ? shape(tag.getInt("Points")) : null;
        shown = culture.stage();
    }
}
