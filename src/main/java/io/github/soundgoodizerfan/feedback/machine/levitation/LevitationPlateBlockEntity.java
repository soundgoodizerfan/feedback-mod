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
    private float diamagnetism = -1f;
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

    /** 0 to 1: the lift it felt on its last step as a share of the most it can give (0 when nothing is below). */
    public float lift() {
        return lift;
    }

    @Override
    public void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle handle, double dt) {
        float dia = diamagnetism();
        if (dia <= 0f || !(level instanceof ServerLevel server))
            return;
        Vec3 centre = subLevel.logicalPose().transformPosition(Vec3.atCenterOf(worldPosition));
        float field = MagnetField.below(server, centre.x, centre.y, centre.z);
        lift = field * dia;
        if (field <= 0f)
            return;
        double weight = LevitationTuning.PLATE_SABLE_MASS * FTuning.GRAVITY_BLOCKS_PER_S2;
        // Lift falls by (LIFT_AT_TOUCH / (reach - touch)) of a weight per block, which is also the hover's spring constant.
        double spring = weight * dia * LevitationTuning.LIFT_AT_TOUCH_WEIGHTS / (MagnetField.REACH - MagnetField.TOUCH);
        double lifting = weight * LevitationTuning.LIFT_AT_TOUCH_WEIGHTS * dia * field;
        at.set(centre.x, centre.y, centre.z);
        double vy = BodyPush.pointVelocity(subLevel, at).y;
        double mass = BodyPush.effectiveMass(subLevel, at, up);
        double damp = 2.0 * LevitationTuning.HOVER_DAMPING_RATIO * Math.sqrt(spring * mass);
        double push = Math.max(0.0, lifting - damp * vy);
        subLevel.logicalPose().transformNormalInverse(impulse.set(0, push * dt, 0), local);
        force.applyImpulseAtPoint(subLevel.getMassTracker(), this.at.set(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5), local);
        handle.applyForcesAndReset(force);
        if (++sinceSpark >= LevitationTuning.SPARK_INTERVAL_TICKS) {
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
    }
}
