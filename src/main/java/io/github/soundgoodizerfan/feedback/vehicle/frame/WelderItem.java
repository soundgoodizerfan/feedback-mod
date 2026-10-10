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
package io.github.soundgoodizerfan.feedback.vehicle.frame;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Places vertices and welds edges between them ({@code feedback_vehicles_spec.md} section 4).
 *
 * <ul>
 *   <li>Use on nothing in particular: a vertex at the block face you are looking at, or -- in mid
 *       air -- at the same distance as the vertex you last touched, snapped to the 1/16 grid. If a
 *       vertex was selected, the new one is welded to it, so a chassis is drawn as a chain.</li>
 *   <li>Use on a vertex: select it; with another selected, weld or cut the edge between them.</li>
 *   <li>Sneak-use on a vertex: remove it and everything welded to it.</li>
 * </ul>
 *
 * <p>When a weld closes a triangle, a plate is laid on it in whatever block is in the off hand,
 * and one of that block is used up. No block in the off hand, no plate -- the edges stay a bare
 * frame. The material is recorded and its properties are read from the block; nothing here ever
 * asks which block it is.
 *
 * <p>The interaction shape is the Data Connector's -- select, then select again -- with a
 * raycast of its own, because vertices hang in the air where vanilla's block hit cannot see them.
 */
public class WelderItem extends Item {

    /** How close the look ray must pass to a vertex to touch it, in blocks. */
    private static final double PICK_RADIUS = 0.12;
    /** How far from the player a frame block is looked for. */
    private static final int SEARCH = 9;

    private record Selection(BlockPos frame, int vertex) {
    }

    private static final Map<UUID, Selection> SELECTED = new ConcurrentHashMap<>();

    public WelderItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide)
            return InteractionResultHolder.success(stack);

        FrameBlockEntity be = nearestFrame(level, player.blockPosition());
        if (be == null) {
            player.displayClientMessage(Component.translatable("message.feedback.welder.no_frame"), true);
            return InteractionResultHolder.consume(stack);
        }
        Frame frame = be.frame();
        Vec3 origin = Vec3.atLowerCornerOf(be.getBlockPos());
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1f);
        double reach = player.blockInteractionRange();

        Selection sel = SELECTED.get(player.getUUID());
        if (sel != null && (!sel.frame.equals(be.getBlockPos()) || !frame.vertices().containsKey(sel.vertex)))
            sel = null;

        int hit = pick(frame, origin, eye, look, reach);

        if (player.isShiftKeyDown()) {
            if (hit >= 0) {
                frame.remove(hit);
                if (sel != null && sel.vertex == hit)
                    SELECTED.remove(player.getUUID());
                be.changed();
            }
            return InteractionResultHolder.consume(stack);
        }

        if (hit < 0) {
            Vec3 at;
            BlockHitResult block = level.clip(new ClipContext(eye, eye.add(look.scale(reach)),
                    ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
            if (block.getType() == HitResult.Type.BLOCK) {
                at = block.getLocation();
            } else {
                double depth = sel != null ? frame.pos(sel.vertex).add(origin).distanceTo(eye) : 2.0;
                at = eye.add(look.scale(Math.min(depth, reach)));
            }
            Frame.Vertex v = Frame.Vertex.snap(at.subtract(origin));
            if (!v.inRange()) {
                player.displayClientMessage(Component.translatable("message.feedback.welder.out_of_range"), true);
                return InteractionResultHolder.consume(stack);
            }
            hit = frame.add(v);
            if (sel != null && sel.vertex != hit)
                weld(frame, sel.vertex, hit, player);
            SELECTED.put(player.getUUID(), new Selection(be.getBlockPos(), hit));
        } else if (sel == null) {
            SELECTED.put(player.getUUID(), new Selection(be.getBlockPos(), hit));
        } else if (sel.vertex == hit) {
            SELECTED.remove(player.getUUID());
        } else {
            weld(frame, sel.vertex, hit, player);
            SELECTED.put(player.getUUID(), new Selection(be.getBlockPos(), hit));
        }
        be.changed();
        return InteractionResultHolder.consume(stack);
    }

    /** Toggles edge {@code a-b}; if it now closes triangles, plates them in the off-hand block. */
    static void weld(Frame frame, int a, int b, Player player) {
        if (!frame.toggleEdge(a, b))
            return;
        for (int c : frame.closing(a, b)) {
            ItemStack off = player.getOffhandItem();
            if (!(off.getItem() instanceof BlockItem block))
                return;
            if (frame.addPlate(a, b, c, BuiltInRegistries.BLOCK.getKey(block.getBlock())) && !player.getAbilities().instabuild)
                off.shrink(1);
        }
    }

    /** The vertex nearest along the look ray, within {@link #PICK_RADIUS} of it; -1 if none. */
    static int pick(Frame frame, Vec3 origin, Vec3 eye, Vec3 look, double reach) {
        int best = -1;
        double bestT = Double.MAX_VALUE;
        for (Map.Entry<Integer, Frame.Vertex> e : frame.vertices().entrySet()) {
            Vec3 p = e.getValue().local().add(origin);
            double t = p.subtract(eye).dot(look);
            if (t < 0 || t > reach || t >= bestT)
                continue;
            if (p.distanceTo(eye.add(look.scale(t))) <= PICK_RADIUS) {
                best = e.getKey();
                bestT = t;
            }
        }
        return best;
    }

    @Nullable
    private static FrameBlockEntity nearestFrame(Level level, BlockPos around) {
        FrameBlockEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(around.offset(-SEARCH, -SEARCH, -SEARCH), around.offset(SEARCH, SEARCH, SEARCH))) {
            if (level.getBlockEntity(pos) instanceof FrameBlockEntity be) {
                double d = pos.distSqr(around);
                if (d < bestDist) {
                    best = be;
                    bestDist = d;
                }
            }
        }
        return best;
    }
}
