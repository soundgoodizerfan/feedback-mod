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
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import io.github.soundgoodizerfan.feedback.vehicle.frame.ChassisCellBlockEntity;
import io.github.soundgoodizerfan.feedback.vehicle.frame.Frame;
import io.github.soundgoodizerfan.feedback.vehicle.frame.FrameAssembly;
import io.github.soundgoodizerfan.feedback.vehicle.frame.FrameBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Draws the plates, tubes and tires of a frame group that has a body of its own, from inside that
 * body. The cell block is posed by Sable with its body's own pose, as the physics hitbox is, so the
 * drawing cannot drift from it, which it did when the frame block drew every body through a matrix
 * of its own. Each shape is drawn by the one cell of its group nearest its middle; the frame block
 * draws the base, and any group whose body or cells are not on the client yet
 * ({@link FrameRenderer}).
 */
public class ChassisCellRenderer implements BlockEntityRenderer<ChassisCellBlockEntity> {

    /** What one cell draws, in frame-local coordinates, and where the group's plot origin is. */
    private record Owned(BlockPos origin, List<Frame.Plate> plates, List<Long> tubes, List<Integer> tires) {
    }

    /** The shapes per cell of one frame, valid while its groups ({@link FrameBlockEntity#cut()}) are the same list. */
    private record Cache(List<FrameAssembly.Part> cut, Map<BlockPos, Owned> cells) {
    }

    /** The frames drawn on this client, by id: a cell finds its frame here. */
    private static final Map<UUID, FrameBlockEntity> FRAMES = new HashMap<>();
    private static final Map<UUID, Cache> CACHE = new HashMap<>();

    public ChassisCellRenderer(BlockEntityRendererProvider.Context context) {
    }

    /** The frame block draws each frame, and tells its cells it is here. */
    static void seen(FrameBlockEntity be) {
        FRAMES.put(be.id(), be);
    }

    @Override
    public void render(ChassisCellBlockEntity cell, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
                       int light, int overlay) {
        UUID id = cell.frame();
        FrameBlockEntity be = id == null ? null : FRAMES.get(id);
        if (be == null || be.isRemoved() || be.getLevel() != cell.getLevel())
            return;
        Owned mine = cells(be).get(cell.getBlockPos());
        if (mine == null)
            return;
        Frame frame = be.frame();
        double time = be.getLevel().getGameTime() + partialTick;
        BlockPos at = cell.getBlockPos();
        poseStack.pushPose();
        // Frame-local to this cell's own space: the group's plot origin, less this cell's corner.
        poseStack.translate(mine.origin.getX() - at.getX(), mine.origin.getY() - at.getY(), mine.origin.getZ() - at.getZ());
        VertexConsumer tex = buffers.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
        PoseStack.Pose pose = poseStack.last();
        for (Frame.Plate p : mine.plates)
            FrameRenderer.drawPlate(tex, pose, frame, p, light);
        for (long key : mine.tubes) {
            Frame.Tube tube = frame.tubes().get(key);
            if (tube != null)
                FrameRenderer.drawTube(tex, pose, frame, key, tube, be.spin().tube(key, time), light);
        }
        for (int v : mine.tires) {
            Frame.Tire tire = frame.tires().get(v);
            if (tire != null)
                FrameRenderer.drawTire(tex, pose, frame, v, tire, be.spin().hub(v, time), light);
        }
        poseStack.popPose();
    }

    /** A shape is drawn from up to a few blocks away from its cell: never cull the cell on its own box. */
    @Override
    public AABB getRenderBoundingBox(ChassisCellBlockEntity cell) {
        return new AABB(cell.getBlockPos()).inflate(8);
    }

    @Override
    public boolean shouldRenderOffScreen(ChassisCellBlockEntity cell) {
        return true;
    }

    private static Map<BlockPos, Owned> cells(FrameBlockEntity be) {
        List<FrameAssembly.Part> cut = be.cut();
        Cache c = CACHE.get(be.id());
        if (c != null && c.cut == cut)
            return c.cells;
        Map<BlockPos, Owned> out = new HashMap<>();
        var level = be.getLevel();
        var container = level == null ? null : SubLevelContainer.getContainer(level);
        // Exactly the groups FrameRenderer leaves out: the frame in a body of its own, the group's body
        // on the client and holding cells.
        if (container != null && SableCompanion.INSTANCE.getContaining(be) instanceof ClientSubLevel) {
            Frame frame = be.frame();
            Set<Integer> vertices = new HashSet<>();
            Set<Long> edges = new HashSet<>();
            for (FrameAssembly.Part part : cut) {
                FrameBlockEntity.GroupBody body = be.groupBodies().get(part.id());
                boolean drawn = body != null && !body.cells().isEmpty() && container.getSubLevel(body.body()) instanceof ClientSubLevel;
                // First part to hold a vertex or edge owns it, as FrameRenderer does.
                List<Integer> mineV = new ArrayList<>();
                List<Long> mineE = new ArrayList<>();
                for (int v : part.vertices())
                    if (vertices.add(v))
                        mineV.add(v);
                for (long e : part.edges())
                    if (edges.add(e))
                        mineE.add(e);
                if (!drawn)
                    continue;
                for (Frame.Plate p : part.plates())
                    out.computeIfAbsent(nearest(body, frame.pos(p.a()).add(frame.pos(p.b())).add(frame.pos(p.c())).scale(1 / 3.0)),
                            k -> new Owned(body.origin(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>())).plates.add(p);
                for (long e : mineE)
                    if (frame.tubes().containsKey(e))
                        out.computeIfAbsent(nearest(body, frame.pos(Frame.edgeA(e)).add(frame.pos(Frame.edgeB(e))).scale(0.5)),
                                k -> new Owned(body.origin(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>())).tubes.add(e);
                for (int v : mineV)
                    if (frame.tires().containsKey(v))
                        out.computeIfAbsent(nearest(body, frame.pos(v)),
                                k -> new Owned(body.origin(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>())).tires.add(v);
            }
        }
        CACHE.put(be.id(), new Cache(cut, out));
        return out;
    }

    /** The cell of {@code body} nearest the frame-local point {@code p}, which is at origin + p in the body's plot. */
    private static BlockPos nearest(FrameBlockEntity.GroupBody body, Vec3 p) {
        Vec3 at = p.add(body.origin().getX(), body.origin().getY(), body.origin().getZ());
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (BlockPos c : body.cells()) {
            double d = Vec3.atCenterOf(c).distanceToSqr(at);
            if (d < bestD) {
                best = c;
                bestD = d;
            }
        }
        return best;
    }
}
