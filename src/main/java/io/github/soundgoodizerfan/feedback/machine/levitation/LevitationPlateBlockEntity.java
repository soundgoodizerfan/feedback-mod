// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.levitation;

import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.force.ForceTotal;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.machine.electromagnet.Diamagnetism;
import io.github.soundgoodizerfan.feedback.machine.electromagnet.MagnetField;
import io.github.soundgoodizerfan.feedback.process.MadeOf;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.vehicle.BodyPush;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

/**
 * One plate of a diamagnetic material. In a vehicle body it is pushed up by the powered electromagnets below it: the lift at
 * full field and full diamagnetism is {@link LevitationTuning#LIFT_AT_TOUCH_WEIGHTS} plate weights with the plate touching the
 * magnet, falling linearly to nothing at {@link MagnetField#REACH}, so a bare plate settles about a block above and a load
 * presses it nearer. A damper (ratio {@link LevitationTuning#HOVER_DAMPING_RATIO}) on the plate's own vertical velocity stops it
 * bouncing. Diamagnets only push, so the force is never downward. Nothing runs unless a magnet is in reach.
 */
public class LevitationPlateBlockEntity extends BlockEntity implements BlockEntitySubLevelActor {

    @Nullable
    private ResourceLocation material;
    private volatile float diamagnetism = -1f;
    private volatile double realMass = -1.0;
    private volatile float lift;
    private int sinceSpark;

    private final ForceTotal force = new ForceTotal();
    private final Vector3d at = new Vector3d(), up = new Vector3d(0, 1, 0), impulse = new Vector3d(), local = new Vector3d();

    public LevitationPlateBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.LEVITATION_PLATE.get(), pos, state);
    }

    @Nullable
    public ResourceLocation material() {
        return material;
    }

    /** 0 to 1: the material's diamagnetism. */
    public float diamagnetism() {
        if (diamagnetism < 0f)
            diamagnetism = material == null ? 0f : Diamagnetism.of(MaterialTable.get().materials().get(material));
        return diamagnetism;
    }

    /**
     * The plate's real mass in Sable units: {@link LevitationTuning#PLATE_SABLE_MASS} is a bismuth plate's, scaled by this material's density
     * (a lead plate weighs more than a graphite one). Sable carries one fixed figure for the block; the difference is applied as weight.
     */
    public double realMass() {
        if (realMass < 0.0) {
            var m = material == null ? null : MaterialTable.get().materials().get(material);
            realMass = m == null ? LevitationTuning.PLATE_SABLE_MASS
                    : LevitationTuning.PLATE_SABLE_MASS * m.density().muPerMb() / LevitationTuning.PLATE_REFERENCE_DENSITY;
        }
        return realMass;
    }

    /** 0 to 1: the lift it felt on its last step as a share of the most it can give (0 when nothing is below). */
    public float lift() {
        return lift;
    }

    @Override
    public void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle handle, double dt) {
        if (!(level instanceof ServerLevel server))
            return;
        float dia = diamagnetism();
        double gravity = io.github.soundgoodizerfan.feedback.vehicle.Gravity.of(server);
        double mass0 = realMass();
        // Sable weighs the block at one fixed figure; what this plate really weighs over or under it is applied as weight.
        double extra = (mass0 - LevitationTuning.PLATE_SABLE_MASS) * gravity;
        Vec3 centre = subLevel.logicalPose().transformPosition(Vec3.atCenterOf(worldPosition));
        float field = dia <= 0f ? 0f : MagnetField.below(server, centre.x, centre.y, centre.z);
        lift = field * dia;
        double push = 0.0;
        if (field > 0f) {
            double weight = mass0 * gravity;
            // Lift falls by (LIFT_AT_TOUCH / (reach - touch)) of a weight per block, which is also the hover's spring constant.
            double spring = weight * dia * LevitationTuning.LIFT_AT_TOUCH_WEIGHTS / (MagnetField.REACH - MagnetField.TOUCH);
            double lifting = weight * LevitationTuning.LIFT_AT_TOUCH_WEIGHTS * dia * field;
            at.set(centre.x, centre.y, centre.z);
            double vy = BodyPush.pointVelocity(subLevel, at).y;
            double mass = BodyPush.effectiveMass(subLevel, at, up);
            double damp = 2.0 * LevitationTuning.HOVER_DAMPING_RATIO * Math.sqrt(spring * mass);
            push = Math.max(0.0, lifting - damp * vy);
        }
        double net = push - extra;
        if (net == 0.0)
            return;
        subLevel.logicalPose().transformNormalInverse(impulse.set(0, net * dt, 0), local);
        force.applyImpulseAtPoint(subLevel.getMassTracker(), this.at.set(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5), local);
        if (field > 0f)
            io.github.soundgoodizerfan.feedback.vehicle.ForceLedger.record(subLevel, io.github.soundgoodizerfan.feedback.vehicle.ForceLedger.Cause.LEVITATION,
                    this.at.x, this.at.y, this.at.z, local.x, local.y, local.z);
        handle.applyForcesAndReset(force);
        if (field > 0f && ++sinceSpark >= LevitationTuning.SPARK_INTERVAL_TICKS) {
            sinceSpark = 0;
            // The physics step is not the server thread: the spark goes to it.
            server.getServer().execute(() -> server.sendParticles(ParticleTypes.ELECTRIC_SPARK, centre.x, centre.y - 0.3, centre.z, 1, 0.3, 0.05, 0.3, 0.02));
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        MadeOf made = input.get(FDataComponents.MADE_OF.get());
        if (made != null)
            material = made.material();
        diamagnetism = -1f;
        realMass = -1.0;
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (material != null)
            components.set(FDataComponents.MADE_OF.get(), new MadeOf(material, LevitationPlateItem.FORM, 1));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (material != null)
            tag.putString("Material", material.toString());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        material = tag.contains("Material") ? ResourceLocation.parse(tag.getString("Material")) : null;
        diamagnetism = -1f;
        realMass = -1.0;
    }
}
