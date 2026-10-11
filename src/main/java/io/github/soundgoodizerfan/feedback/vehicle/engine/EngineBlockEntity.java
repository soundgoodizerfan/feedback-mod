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
package io.github.soundgoodizerfan.feedback.vehicle.engine;

import io.github.soundgoodizerfan.feedback.registry.FVehicles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The design a placed Engine was built to. It comes off the item when placed and goes back on it
 * when broken (the loot table copies the component), so an engine is never re-designed by moving.
 *
 * <h2>Its weight rides on a box</h2>
 * Sable weighs a block by its state alone, so every Engine block would weigh the same. The block
 * is tagged {@code sable:super_light} (0.25 kpg, 250 Mu), and once it is part of a vehicle it
 * carries the rest of its design's mass on a free box welded at its centre, contacts off -- what
 * plates already do ({@code FrameBodies}). Boxes are not saved, so the tick re-makes it.
 * <p>
 * Not weightless, which was tried first: a vehicle of nothing but an engine then weighed zero
 * at the moment of lifting, before the box existed, and Sable discarded the body. The cost of
 * the floor is that an engine designed lighter than 250 Mu weighs 250.
 */
public class EngineBlockEntity extends BlockEntity {

    /** Just inside the block, so the box never reaches past what is drawn. */
    private static final double BOX_HALF = 0.45;
    /** What Sable already counts for the block itself: {@code sable:super_light}. */
    private static final double BLOCK_KPG = 0.25;

    @Nullable
    private EngineDesign design;
    @Nullable
    private dev.ryanhcode.sable.api.physics.object.box.BoxPhysicsObject box;
    @Nullable
    private dev.ryanhcode.sable.api.physics.constraint.PhysicsConstraintHandle weld;

    public EngineBlockEntity(BlockPos pos, BlockState state) {
        super(FVehicles.ENGINE_ENTITY.get(), pos, state);
    }

    @Nullable
    public EngineDesign design() {
        return design;
    }

    /** Puts the design's mass on the vehicle this engine is part of, once its box is in Sable. */
    static void serverTick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, EngineBlockEntity be) {
        if (be.design == null || !(level instanceof net.minecraft.server.level.ServerLevel server)
                || !(dev.ryanhcode.sable.companion.SableCompanion.INSTANCE.getContaining(be) instanceof dev.ryanhcode.sable.sublevel.ServerSubLevel body))
            return;
        var material = io.github.soundgoodizerfan.feedback.process.MaterialTable.get().materials().get(be.design.material());
        if (material == null)
            return;
        var system = dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem.get(server);
        if (be.box == null || be.box.isRemoved()) {
            org.joml.Vector3d centre = new org.joml.Vector3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            var pose = new dev.ryanhcode.sable.companion.math.Pose3d(body.logicalPose().transformPosition(centre, new org.joml.Vector3d()),
                    new org.joml.Quaterniond(body.logicalPose().orientation()), new org.joml.Vector3d(), new org.joml.Vector3d(1, 1, 1));
            be.box = new dev.ryanhcode.sable.api.physics.object.box.BoxPhysicsObject(pose,
                    new org.joml.Vector3d(BOX_HALF, BOX_HALF, BOX_HALF),
                    Math.max(1e-3, io.github.soundgoodizerfan.feedback.core.unit.Units.kpg(be.design.mass(material)) - BLOCK_KPG));
            system.addObject(be.box);
            be.weld = null;
        }
        if (be.box.getRuntimeId() == dev.ryanhcode.sable.api.physics.PhysicsPipelineBody.NULL_RUNTIME_ID
                || (be.weld != null && be.weld.isValid()))
            return;
        org.joml.Vector3d centre = new org.joml.Vector3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        be.weld = system.getPipeline().addConstraint(body, be.box,
                new dev.ryanhcode.sable.api.physics.constraint.FixedConstraintConfiguration(centre, new org.joml.Vector3d(), new org.joml.Quaterniond()));
        be.weld.setContactsEnabled(false); // the box sits inside its own block
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (weld != null && weld.isValid())
            weld.remove();
        if (box != null && level instanceof net.minecraft.server.level.ServerLevel server && !box.isRemoved())
            dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem.get(server).removeObject(box);
        box = null;
    }

    /** The mass this engine puts on its vehicle, block and box, in kpg, for tests; the block alone until the box exists. */
    public double carriedKpg() {
        return BLOCK_KPG + (box == null || box.isRemoved() ? 0 : box.getMass());
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        design = input.get(FVehicles.ENGINE_DESIGN.get());
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (design != null)
            components.set(FVehicles.ENGINE_DESIGN.get(), design);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (design != null)
            EngineDesign.CODEC.encodeStart(NbtOps.INSTANCE, design).ifSuccess(t -> tag.put("Design", t));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        design = tag.contains("Design") ? EngineDesign.CODEC.parse(NbtOps.INSTANCE, tag.get("Design")).result().orElse(null) : null;
    }
}
