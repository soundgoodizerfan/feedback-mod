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
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.soundgoodizerfan.feedback.core.rotation.Rotatable;
import io.github.soundgoodizerfan.feedback.vehicle.micro.MicroCellBlockEntity;
import io.github.soundgoodizerfan.feedback.vehicle.micro.MicroRotor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.neoforged.neoforge.client.model.data.ModelData;

import java.util.List;

/**
 * Draws a micro cell ({@code docs/specs/feedback_micro_spec.md}): every filled voxel is its block's real
 * model, at a quarter of the scale, in the cell's own space -- so Sable's pose for the cell's body
 * carries the drawing exactly as it carries the collision, and the two cannot drift apart.
 *
 * <p>The model's quads are submitted one by one, as vanilla's {@code ModelBlockRenderer.renderModel}
 * does it for a block in the hand, rather than through {@code renderSingleBlock}: that way a face
 * hidden by a full block in the next voxel is not drawn, and each tinted quad gets its own tint
 * index. Lit as the cell is (one light for all its voxels) and shaded by face; no ambient occlusion.
 *
 * <p>A face on the edge of the cell is hidden by the voxel across it in the next cell, read from that
 * cell's entity when the face is drawn. Baking a cell's quads once instead of every frame is the upgrade
 * a chunk mesh would bring.
 *
 * <p>A rotation voxel whose full-size block is drawn turning (render shape
 * {@code ENTITYBLOCK_ANIMATED}: shaft, cog; not the gearbox, whose box stands still) is turned about
 * its own centre and axis by its rotor's angle, {@link RotatingRenderer}'s transform at quarter scale.
 */
public class MicroCellRenderer implements BlockEntityRenderer<MicroCellBlockEntity> {

    private static final int N = MicroCellBlockEntity.N;

    private final BlockRenderDispatcher blocks;

    public MicroCellRenderer(BlockEntityRendererProvider.Context context) {
        blocks = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(MicroCellBlockEntity cell, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
                       int light, int overlay) {
        Level level = cell.getLevel();
        if (level == null || cell.isEmpty())
            return;
        BlockColors colors = Minecraft.getInstance().getBlockColors();
        RandomSource random = RandomSource.create();
        float s = 1f / N;
        // A belt: two lines between the pulleys' centres, drawn once, from the end that sorts first.
        for (int i = 0; i < MicroCellBlockEntity.VOXELS; i++) {
            var far = cell.belt(i);
            if (far == null || far.pos().asLong() < cell.getBlockPos().asLong()
                    || far.pos().equals(cell.getBlockPos()) && far.index() < i)
                continue;
            float ax = (i & 3) * s + s / 2, ay = (i >> 2 & 3) * s + s / 2, az = (i >> 4) * s + s / 2;
            int j = far.index();
            float bx = (far.pos().getX() - cell.getBlockPos().getX()) + (j & 3) * s + s / 2;
            float by = (far.pos().getY() - cell.getBlockPos().getY()) + (j >> 2 & 3) * s + s / 2;
            float bz = (far.pos().getZ() - cell.getBlockPos().getZ()) + (j >> 4) * s + s / 2;
            VertexConsumer belt = buffers.getBuffer(RenderType.lines());
            float dx = bx - ax, dy = by - ay, dz = bz - az, len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len < 1e-4f)
                continue;
            for (int side = -1; side <= 1; side += 2) {
                float ox = side * s * 0.15f;
                belt.addVertex(poseStack.last(), ax + ox, ay, az).setColor(40, 30, 25, 255).setNormal(poseStack.last(), dx / len, dy / len, dz / len);
                belt.addVertex(poseStack.last(), bx + ox, by, bz).setColor(40, 30, 25, 255).setNormal(poseStack.last(), dx / len, dy / len, dz / len);
            }
        }
        // With a Data Connector in hand, the voxels a card can set are outlined in the cell's own space, so they ride the body.
        var player = Minecraft.getInstance().player;
        if (player != null && player.getMainHandItem().getItem() instanceof io.github.soundgoodizerfan.feedback.item.DataConnectorItem) {
            VertexConsumer lines = buffers.getBuffer(RenderType.lines());
            for (int i = 0; i < MicroCellBlockEntity.VOXELS; i++)
                if (io.github.soundgoodizerfan.feedback.vehicle.micro.VoxelNode.settable(cell.get(i))) {
                    float x = (i & 3) * s, y = (i >> 2 & 3) * s, z = (i >> 4) * s;
                    net.minecraft.client.renderer.LevelRenderer.renderLineBox(poseStack, lines, x, y, z, x + s, y + s, z + s, 0.4f, 0.8f, 1f, 1f);
                }
        }
        for (int z = 0; z < N; z++)
            for (int y = 0; y < N; y++)
                for (int x = 0; x < N; x++) {
                    BlockState state = cell.get(x, y, z);
                    if (state == null)
                        continue;
                    poseStack.pushPose();
                    poseStack.translate(x * s, y * s, z * s);
                    poseStack.scale(s, s, s);
                    MicroRotor rotor = cell.rotor(MicroCellBlockEntity.index(x, y, z));
                    if (rotor != null && state.getBlock() instanceof io.github.soundgoodizerfan.feedback.machine.gearbox.GearboxBlock) {
                        // A gearbox's box stands; its stubs turn, as full size.
                        SpinningParts.gearbox(poseStack, buffers, light, overlay, rotor.angle(level.getGameTime(), partialTick));
                        poseStack.popPose();
                        continue;
                    }
                    if (rotor != null && state.getRenderShape() == RenderShape.ENTITYBLOCK_ANIMATED
                            && state.getBlock() instanceof Rotatable rotatable) {
                        poseStack.translate(0.5f, 0.5f, 0.5f);
                        poseStack.mulPose((switch (rotatable.getRotationAxis(state)) {
                            case X -> Axis.XP;
                            case Y -> Axis.YP;
                            case Z -> Axis.ZP;
                        }).rotationDegrees(rotor.angle(level.getGameTime(), partialTick)));
                        poseStack.translate(-0.5f, -0.5f, -0.5f);
                    }
                    BakedModel model = blocks.getBlockModel(state);
                    BlockState skin = cell.skin(MicroCellBlockEntity.index(x, y, z));
                    TextureAtlasSprite skinSprite = skin == null ? null : spriteOf(skin, random);
                    for (RenderType type : model.getRenderTypes(state, random, ModelData.EMPTY)) {
                        VertexConsumer out = buffers.getBuffer(RenderTypeHelper.getEntityRenderType(type, false));
                        for (Direction side : Direction.values())
                            if (!hidden(cell, state, x, y, z, side)) {
                                random.setSeed(42L);
                                draw(out, poseStack.last(), colors, level, cell.getBlockPos(), state,
                                        reskinned(model.getQuads(state, side, random, ModelData.EMPTY, type), skinSprite), light, overlay);
                            }
                        random.setSeed(42L);
                        draw(out, poseStack.last(), colors, level, cell.getBlockPos(), state,
                                reskinned(model.getQuads(state, null, random, ModelData.EMPTY, type), skinSprite), light, overlay);
                    }
                    poseStack.popPose();
                }
    }

    /** The look of {@code skin}: its model's side texture, or its particle if it has none. */
    private TextureAtlasSprite spriteOf(BlockState skin, RandomSource random) {
        BakedModel model = blocks.getBlockModel(skin);
        random.setSeed(42L);
        List<BakedQuad> side = model.getQuads(skin, Direction.NORTH, random, ModelData.EMPTY, null);
        return side.isEmpty() ? model.getParticleIcon(ModelData.EMPTY) : side.get(0).getSprite();
    }

    /** {@code quads} with their texture swapped for {@code to}, each corner's coordinates carried across the new sprite. */
    private static List<BakedQuad> reskinned(List<BakedQuad> quads, TextureAtlasSprite to) {
        if (to == null || quads.isEmpty())
            return quads;
        List<BakedQuad> out = new java.util.ArrayList<>(quads.size());
        for (BakedQuad q : quads) {
            TextureAtlasSprite from = q.getSprite();
            int[] v = q.getVertices().clone();
            float du = from.getU1() - from.getU0(), dv = from.getV1() - from.getV0();
            for (int k = 0; k < 4; k++) {
                float fu = du == 0 ? 0 : (Float.intBitsToFloat(v[k * 8 + 4]) - from.getU0()) / du;
                float fv = dv == 0 ? 0 : (Float.intBitsToFloat(v[k * 8 + 5]) - from.getV0()) / dv;
                v[k * 8 + 4] = Float.floatToRawIntBits(to.getU0() + (to.getU1() - to.getU0()) * fu);
                v[k * 8 + 5] = Float.floatToRawIntBits(to.getV0() + (to.getV1() - to.getV0()) * fv);
            }
            out.add(new BakedQuad(v, -1, q.getDirection(), to, q.isShade(), q.hasAmbientOcclusion()));
        }
        return out;
    }

    private static void draw(VertexConsumer out, PoseStack.Pose pose, BlockColors colors, Level level, BlockPos pos,
                             BlockState state, List<BakedQuad> quads, int light, int overlay) {
        for (BakedQuad quad : quads) {
            float r = 1f, g = 1f, b = 1f;
            if (quad.isTinted()) {
                int tint = colors.getColor(state, level, pos, quad.getTintIndex());
                r = (tint >> 16 & 255) / 255f;
                g = (tint >> 8 & 255) / 255f;
                b = (tint & 255) / 255f;
            }
            out.putBulkData(pose, quad, r, g, b, 1f, light, overlay);
        }
    }

    /**
     * True if the voxel across {@code side} is in this cell and hides this one's face on that side:
     * a block whose face there is whole and opaque, or one that is its own like (glass in glass).
     */
    private static boolean hidden(MicroCellBlockEntity cell, BlockState state, int x, int y, int z, Direction side) {
        int nx = x + side.getStepX(), ny = y + side.getStepY(), nz = z + side.getStepZ();
        BlockState next;
        if (nx < 0 || ny < 0 || nz < 0 || nx >= N || ny >= N || nz >= N) {
            // On the cell's edge: the voxel across it is in the next cell, if that is one.
            if (cell.getLevel() == null || !(cell.getLevel().getBlockEntity(cell.getBlockPos().relative(side)) instanceof MicroCellBlockEntity other))
                return false;
            next = other.get(Math.floorMod(nx, N), Math.floorMod(ny, N), Math.floorMod(nz, N));
        } else
            next = cell.get(nx, ny, nz);
        return next != null && (state.skipRendering(next, side)
                || next.canOcclude() && Block.isFaceFull(next.getOcclusionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO), side.getOpposite()));
    }

    /**
     * The voxels are drawn inside the cell, but Sable poses the cell as part of a body, so the
     * frustum test gets a block of margin rather than the cell's own box.
     * shortcut: sized by eye, not checked against a rotated body in game; tighten if cells draw when off screen.
     */
    @Override
    public AABB getRenderBoundingBox(MicroCellBlockEntity cell) {
        return new AABB(cell.getBlockPos()).inflate(1);
    }
}
