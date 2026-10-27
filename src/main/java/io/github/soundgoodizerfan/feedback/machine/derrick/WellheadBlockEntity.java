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
package io.github.soundgoodizerfan.feedback.machine.derrick;

import javax.annotation.Nullable;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.hazard.PressureBurst;
import io.github.soundgoodizerfan.feedback.core.hazard.PressureHolder;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationNode;
import io.github.soundgoodizerfan.feedback.core.unit.Pu;
import io.github.soundgoodizerfan.feedback.core.unit.Su;
import io.github.soundgoodizerfan.feedback.geology.BasinState;
import io.github.soundgoodizerfan.feedback.geology.CrudeGrade;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.registry.FBlocks;
import io.github.soundgoodizerfan.feedback.registry.FFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * The top of a drilled well (spec 3.2, 3.3). It reads the basin under it and acts on three numbers:
 * the reservoir's pressure {@code P}, the hydrostatic head of the column above it, and the choke.
 *
 * <ul>
 * <li><b>P above the head:</b> the well flows by itself at (P - head) times the choke's share.
 * <li><b>P below the head:</b> it stops. A downhole lift -- this block's rotation load, whose Su rises
 *     with the height it must lift ({@code PUMP_LIFT_SU_PER_BLOCK} per block) -- raises {@code
 *     WELLHEAD_LIFT_MB_PER_RPM} per RPM. Under the floor pressure the lift's yield declines toward, never
 *     to, zero: the rest is stranded.
 * <li><b>Coning:</b> drawing more than the reservoir sustains adds to {@code cone}; the crude comes up
 *     gassy (light fraction stripped) and part of every draw is lost to gas. It falls slowly when the
 *     well is drawn under the sustainable rate.
 * <li><b>Blowout:</b> the surface pressure (P - head) against the choke's rating through {@link
 *     PressureBurst}: spray and hiss from 70% of the danger line, a climbing chance after it, certain at
 *     critical. A fresh well opened wide is the only way there.
 * </ul>
 * Output goes into any adjacent fluid handler (a pipe run, a tank), stamped with the basin's grade, and is
 * drawn from the basin only for what the handler accepts, so a shut-in well loses nothing.
 * shortcut: no water cut or gas product; coning only strips the grade and wastes volume.
 */
public class WellheadBlockEntity extends RotationNode implements PressureHolder {

    /** The discrete choke: how much it passes, and the surface pressure its gear is rated for. */
    public enum Choke {
        SMALL(FTuning.WELLHEAD_SMALL_FLOW, FTuning.WELLHEAD_SMALL_DANGER_PU, FTuning.WELLHEAD_SMALL_CRITICAL_PU),
        HALF(FTuning.WELLHEAD_HALF_FLOW, FTuning.WELLHEAD_HALF_DANGER_PU, FTuning.WELLHEAD_HALF_CRITICAL_PU),
        OPEN(FTuning.WELLHEAD_OPEN_FLOW, FTuning.WELLHEAD_OPEN_DANGER_PU, FTuning.WELLHEAD_OPEN_CRITICAL_PU);

        public final float flow;
        public final Pu danger, critical;

        Choke(float flow, Pu danger, Pu critical) {
            this.flow = flow;
            this.danger = danger;
            this.critical = critical;
        }

        public Choke next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    private int depth = FTuning.WELLHEAD_DEFAULT_DEPTH;
    private Choke choke = Choke.SMALL;
    private float cone;
    private float owed;
    private int lastMoved;
    private float liftSu;
    private boolean lifting;

    @Nullable
    private BasinState.Entry basin;
    private boolean basinKnown;

    public WellheadBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.WELLHEAD.get(), pos, state);
    }

    public void setDepth(int depth) {
        this.depth = depth;
        setChanged();
    }

    public Choke choke() {
        return choke;
    }

    public void cycleChoke() {
        choke = choke.next();
        setChanged();
    }

    // --- the numbers -----------------------------------------------------------------------

    private void refreshBasin(ServerLevel level) {
        if (!basinKnown || level.getGameTime() % 200 == 0) {
            basin = BasinState.at(level, worldPosition);
            basinKnown = true;
        }
    }

    private float head() {
        return depth * (basin == null ? CrudeGrade.DEFAULT.density() : basin.grade().density())
                * FTuning.WELLHEAD_HEAD_PU_PER_BLOCK_DENSITY;
    }

    /** What the gauge reads: the reservoir's push over the column's weight, zero when it cannot reach the top. */
    @Override
    public Pu getPressure() {
        if (!(level instanceof ServerLevel server))
            return new Pu(0f);
        refreshBasin(server);
        return basin == null ? new Pu(0f) : new Pu(Math.max(0f, basin.pressure().value() - head()));
    }

    @Override
    public Su getLoadSu() {
        return new Su(liftSu);
    }

    // --- tick ------------------------------------------------------------------------------

    public void tickServer() {
        if (!(level instanceof ServerLevel server))
            return;
        refreshBasin(server);
        if (basin == null)
            return;
        float p = basin.pressure().value(), head = head(), surface = Math.max(0f, p - head);

        if (hazard(server, surface))
            return;

        float rate;
        float density = basin.grade().density();
        if (p > head) {
            setLift(false, 0f);
            rate = surface * FTuning.WELLHEAD_MB_PER_PU * choke.flow;
        } else {
            float height = (head - p) / (density * FTuning.WELLHEAD_HEAD_PU_PER_BLOCK_DENSITY);
            setLift(true, FTuning.PUMP_LOAD_SU.value() + FTuning.PUMP_LIFT_SU_PER_BLOCK.value() * height);
            float yield = Math.min(1f, Math.max(FTuning.WELLHEAD_FLOOR_MIN_YIELD, p / FTuning.WELLHEAD_FLOOR_PU));
            rate = Math.abs(getRpm().value()) * FTuning.WELLHEAD_LIFT_MB_PER_RPM * yield;
        }

        owed += rate;
        int amount = (int) owed;
        owed -= amount;
        int moved = amount > 0 ? push(server, amount) : 0;
        lastMoved = moved;

        float sustainable = FTuning.WELLHEAD_SUSTAINABLE_MB_PER_PU * Math.max(p, FTuning.WELLHEAD_FLOOR_PU);
        if (moved > sustainable)
            cone = Math.min(1f, cone + (moved - sustainable) / sustainable * FTuning.WELLHEAD_CONE_RISE);
        else
            cone = Math.max(0f, cone - FTuning.WELLHEAD_CONE_RECOVER);
    }

    /** The load follows the lift; the network only hears about it when it moves. */
    private void setLift(boolean on, float su) {
        lifting = on;
        if (Math.abs(su - liftSu) >= 0.5f) {
            liftSu = su;
            if (network != null)
                network.recalculate();
        }
    }

    /** Offers up to {@code amount} mB to the neighbours; draws from the basin only what they take. */
    private int push(ServerLevel server, int amount) {
        CrudeGrade grade = coned(basin.grade());
        FluidStack offer = CrudeGrade.stamp(new FluidStack(FFluids.CRUDE_OIL.get(), amount), grade);
        int moved = 0;
        for (Direction d : Direction.values()) {
            if (moved >= amount)
                break;
            IFluidHandler target = server.getCapability(Capabilities.FluidHandler.BLOCK, worldPosition.relative(d),
                    d.getOpposite());
            if (target == null)
                continue;
            int fits = target.fill(offer.copyWithAmount(amount - moved), IFluidHandler.FluidAction.SIMULATE);
            if (fits <= 0)
                continue;
            int drawn = basin.draw(fits);
            if (drawn <= 0)
                break;
            int delivered = Math.max(1, Math.round(drawn * (1f - cone * FTuning.WELLHEAD_CONE_WASTE)));
            target.fill(offer.copyWithAmount(delivered), IFluidHandler.FluidAction.EXECUTE);
            moved += drawn;
        }
        return moved;
    }

    /** Gas breakthrough strips the light ends and leaves the rest heavier and sourer. */
    private CrudeGrade coned(CrudeGrade g) {
        if (cone <= 0f)
            return g;
        return new CrudeGrade(g.density() * (1f + 0.1f * cone), Math.min(1f, g.sulfur() * (1f + cone)),
                g.lightFraction() * (1f - cone * FTuning.WELLHEAD_CONE_STRIP));
    }

    // --- hazard ----------------------------------------------------------------------------

    /** Warning, then the PressureBurst ladder. True if the well just blew out. */
    private boolean hazard(ServerLevel server, float surface) {
        if (surface > choke.danger.value() * FTuning.WELLHEAD_WARN_FRACTION) {
            if (server.getGameTime() % 4 == 0)
                server.sendParticles(ParticleTypes.SQUID_INK, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0,
                        worldPosition.getZ() + 0.5, 8, 0.15, 0.3, 0.15, 0.25);
            if (server.getGameTime() % 20 == 0)
                server.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.6f, 0.6f);
        }
        if (server.getGameTime() % 10 == 0
                && PressureBurst.fails(new Pu(surface), choke.danger, choke.critical, server.random)) {
            blowOut(server);
            return true;
        }
        return false;
    }

    /** Wrecks the wellhead and vents crude over the ground; the hole stays and can be drilled again. */
    private void blowOut(ServerLevel server) {
        BlockPos pos = worldPosition;
        int spill = basin == null ? 0 : basin.draw(FTuning.WELLHEAD_BLOWOUT_SPILL_MB);
        server.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, FTuning.WELLHEAD_BLOWOUT_POWER,
                Level.ExplosionInteraction.BLOCK);
        server.sendParticles(ParticleTypes.SQUID_INK, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 80, 0.4, 1.2,
                0.4, 0.4);
        server.removeBlock(pos, false);
        int reach = FTuning.WELLHEAD_BLOWOUT_REACH;
        for (int placed = 0, tries = 0; placed < spill / 1000 && tries < 60; tries++) {
            BlockPos at = pos.offset(server.random.nextIntBetweenInclusive(-reach, reach),
                    server.random.nextIntBetweenInclusive(-1, 1), server.random.nextIntBetweenInclusive(-reach, reach));
            if (io.github.soundgoodizerfan.feedback.geology.CrudePoolBlock.place(server, at))
                placed++;
        }
    }

    public Component describe() {
        String state = lifting ? (Math.abs(getRpm().value()) > 0f ? "lifting" : "needs_lift")
                : (lastMoved > 0 ? "flowing" : "shut_in");
        return Component.translatable("message.feedback.wellhead." + state, Component.translatable(
                "message.feedback.wellhead.choke." + choke.name().toLowerCase()), Math.round(choke.danger.value()),
                Math.round(cone * 100f));
    }

    // --- persistence -----------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Depth", depth);
        tag.putInt("Choke", choke.ordinal());
        tag.putFloat("Cone", cone);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Depth")) {
            depth = tag.getInt("Depth");
            choke = Choke.values()[Math.floorMod(tag.getInt("Choke"), Choke.values().length)];
            cone = tag.getFloat("Cone");
        }
    }
}
