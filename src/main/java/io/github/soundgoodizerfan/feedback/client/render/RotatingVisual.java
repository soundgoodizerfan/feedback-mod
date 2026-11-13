// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.render;

import io.github.soundgoodizerfan.feedback.core.rotation.Rotatable;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationNode;

import java.util.function.Consumer;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.baked.BakedModelBuilder;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;

import net.minecraft.core.BlockPos;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;

/**
 * Draws a rotation node as its own block model, turning.
 *
 * <h2>Why this is not decoration</h2>
 * Philosophy 10 says the player's own senses are free and sufficient — that you should be able to
 * walk into a factory and read its state without buying an instrument. Until a shaft visibly
 * turns, that is a claim the mod cannot back: a running network and a dead one look identical,
 * and every question about speed has to be answered by a HUD. Motion is the first readout, and
 * the only one that costs the player nothing.
 *
 * <h2>Why instanced</h2>
 * A shaft run is many copies of one model differing only by position and angle, which is exactly
 * what instancing is for: one draw call for the whole run instead of one per block. The transform
 * is recomputed on the CPU each frame rather than derived from a clock in a vertex shader —
 * slower in principle, and irrelevant at our block counts. Create does the GPU version with its
 * own shaders, which are not ours to take.
 *
 * <p>One class covers every spinning block because {@link Rotatable#getRotationAxis} and
 * The baked model and the rotation axis between them answer everything a rotating visual needs to know: which way
 * it turns, and what it looks like. Nothing here is per-block, so nothing here should be.
 */
public class RotatingVisual extends AbstractBlockEntityVisual<RotationNode> implements SimpleDynamicVisual {

    private final TransformedInstance instance;
    private final Direction.Axis axis;

    public RotatingVisual(VisualizationContext context, RotationNode node, float partialTick) {
        super(context, node, partialTick);

        this.axis = ((Rotatable) blockState.getBlock()).getRotationAxis(blockState);
        // NOT Models.block(state). Flywheel's baker runs the state through
        // BakedModelBufferer, which checks getRenderShape() and only tessellates MODEL -- and
        // these blocks report ENTITYBLOCK_ANIMATED precisely so the chunk mesh leaves them
        // alone. The result was a visual that created fine and drew nothing: shafts were
        // invisible with the backend on and appeared only via the fallback renderer.
        //
        // Building from the BakedModel directly skips that gate. The model that
        // getBlockModel(state) returns already has the blockstate's variant rotation baked into
        // it, so it points along its axis and only the spin is left to do.
        BakedModel baked = Minecraft.getInstance().getBlockRenderer().getBlockModel(blockState);
        instance = instancerProvider()
                .instancer(InstanceTypes.TRANSFORMED, new BakedModelBuilder(baked).build())
                .createInstance();

        instance.light(computePackedLight());
        applyTransform(partialTick);
    }

    /** Once per frame rather than once per tick, so a slow shaft glides instead of stepping. */
    @Override
    public void beginFrame(DynamicVisual.Context context) {
        applyTransform(context.partialTick());
    }

    private void applyTransform(float partialTick) {
        BlockPos origin = getVisualPosition();
        instance.setIdentityTransform()
                .translate(origin.getX(), origin.getY(), origin.getZ())
                .rotateCenteredDegrees(blockEntity.getVisualAngle(partialTick), axis)
                .setChanged();
    }

    @Override
    public void updateLight(float partialTick) {
        relight(instance);
    }

    /** Feeds the block-breaking crack overlay, which would otherwise draw on nothing. */
    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(instance);
    }

    @Override
    protected void _delete() {
        instance.delete();
    }

    /**
     * The visualizer to hand Flywheel for a block entity type.
     * <p>
     * {@code skipVanillaRender} is left at its default of always skipping, which suppresses the
     * fallback {@link RotatingRenderer} while the backend is on. It does <em>not</em> suppress the
     * static chunk model — that is what the block's own {@code RenderShape} is for, and without
     * both the block is drawn twice, once still and once spinning.
     */
    public static <T extends RotationNode> SimpleBlockEntityVisualizer.Factory<T> factory() {
        return RotatingVisual::new;
    }
}
