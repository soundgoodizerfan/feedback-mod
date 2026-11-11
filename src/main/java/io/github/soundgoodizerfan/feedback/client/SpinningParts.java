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
package io.github.soundgoodizerfan.feedback.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationNode;
import io.github.soundgoodizerfan.feedback.machine.adapter.RotationAdapterBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Quaternionf;

/**
 * Blocks whose shafts turn while the block itself stands still ({@code docs/feedback_micro_spec.md},
 * <em>Playtest fixes</em>): the gearbox, whose stub on each face turns by the node's angle about its
 * own outward direction (so by {@code sign(face) x angle} about the axis, the gearbox's own sign
 * rule, and opposite stubs turn against each other), and the Rotation Adapter, whose big stub and
 * four small ones all turn with its angle about its axis, each about its own centre line. Each is a
 * housing model and stub models, registered here as standalone models and drawn by the block entity
 * renderer -- full size, and by {@link MicroCellRenderer} for a micro gearbox.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class SpinningParts {

    private static final ModelResourceLocation GEARBOX_HOUSING = part("gearbox_housing"),
            GEARBOX_STUB = part("gearbox_stub"),
            ADAPTER_HOUSING = part("rotation_adapter_housing"),
            ADAPTER_BIG = part("rotation_adapter_stub_big"),
            ADAPTER_SMALL = part("rotation_adapter_stub_small");

    private SpinningParts() {
    }

    private static ModelResourceLocation part(String name) {
        return ModelResourceLocation.standalone(Feedback.id("block/" + name));
    }

    @SubscribeEvent
    public static void register(ModelEvent.RegisterAdditional event) {
        for (ModelResourceLocation m : new ModelResourceLocation[]{GEARBOX_HOUSING, GEARBOX_STUB, ADAPTER_HOUSING, ADAPTER_BIG, ADAPTER_SMALL})
            event.register(m);
    }

    private static void draw(PoseStack pose, MultiBufferSource buffers, ModelResourceLocation id, int light, int overlay) {
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(id);
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(pose.last(),
                buffers.getBuffer(RenderTypeHelper.getEntityRenderType(RenderType.solid(), false)),
                null, model, 1f, 1f, 1f, light, overlay, ModelData.EMPTY, RenderType.solid());
    }

    /** Turns the pose about the block's centre so the model's +Y points along {@code to}. */
    private static void point(PoseStack pose, Direction to) {
        pose.translate(0.5f, 0.5f, 0.5f);
        pose.mulPose(new Quaternionf().rotationTo(0, 1, 0, to.getStepX(), to.getStepY(), to.getStepZ()));
        pose.translate(-0.5f, -0.5f, -0.5f);
    }

    /** A gearbox in a unit cube at the pose, its node at {@code angle} degrees. */
    public static void gearbox(PoseStack pose, MultiBufferSource buffers, int light, int overlay, float angle) {
        draw(pose, buffers, GEARBOX_HOUSING, light, overlay);
        for (Direction face : Direction.values()) {
            pose.pushPose();
            point(pose, face);
            pose.translate(0.5f, 0.5f, 0.5f);
            pose.mulPose(Axis.YP.rotationDegrees(angle));
            pose.translate(-0.5f, -0.5f, -0.5f);
            draw(pose, buffers, GEARBOX_STUB, light, overlay);
            pose.popPose();
        }
    }

    /** A Rotation Adapter facing {@code facing}, its node at {@code angle} degrees about its axis's positive direction. */
    public static void adapter(PoseStack pose, MultiBufferSource buffers, int light, int overlay, Direction facing, float angle) {
        pose.pushPose();
        point(pose, facing);
        draw(pose, buffers, ADAPTER_HOUSING, light, overlay);
        // The model's +Y is the facing, so a turn about the positive axis is negated for a negative facing.
        float spin = facing.getAxisDirection() == Direction.AxisDirection.POSITIVE ? angle : -angle;
        stub(pose, buffers, ADAPTER_BIG, 8, 8, spin, light, overlay);
        for (int cx = 6; cx <= 10; cx += 4)
            for (int cz = 6; cz <= 10; cz += 4)
                stub(pose, buffers, ADAPTER_SMALL, cx, cz, spin, light, overlay);
        pose.popPose();
    }

    /** A stub modelled on the block's centre line, drawn on the line at ({@code cx}, {@code cz}) sixteenths, turned about it. */
    private static void stub(PoseStack pose, MultiBufferSource buffers, ModelResourceLocation id, int cx, int cz, float spin,
                             int light, int overlay) {
        pose.pushPose();
        pose.translate(cx / 16f, 0, cz / 16f);
        pose.mulPose(Axis.YP.rotationDegrees(spin));
        pose.translate(-0.5f, 0, -0.5f);
        draw(pose, buffers, id, light, overlay);
        pose.popPose();
    }

    /** Draws a gearbox's or an adapter's turning shafts, full size. */
    public static class Renderer<T extends RotationNode> implements BlockEntityRenderer<T> {

        public Renderer(BlockEntityRendererProvider.Context context) {
        }

        @Override
        public void render(T node, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            BlockState state = node.getBlockState();
            float angle = node.getVisualAngle(partialTick);
            if (state.getBlock() instanceof RotationAdapterBlock)
                adapter(pose, buffers, light, overlay, state.getValue(RotationAdapterBlock.FACING), angle);
            else
                gearbox(pose, buffers, light, overlay, angle);
        }
    }
}
