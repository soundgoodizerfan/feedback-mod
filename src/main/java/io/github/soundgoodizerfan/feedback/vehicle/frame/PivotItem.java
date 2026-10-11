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

import io.github.soundgoodizerfan.feedback.vehicle.frame.WelderItem.Aim;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Locale;

/**
 * Sets a point to be a bearing, and switches what kind ({@code feedback_vehicles_spec.md}
 * section 4a). Look at a point and use:
 *
 * <ul>
 *   <li><b>A point of an unassembled frame</b> cycles it plain, hinge, ball, locked, plain. It
 *       only becomes a joint when the chassis is assembled: the frame is cut at the point into
 *       bodies ({@link FrameAssembly}), and the bearing wakes up there.</li>
 *   <li><b>A point with a joint on it</b> -- two frames welded or already bearing -- cycles that
 *       joint hinge, ball, locked, so a weld between frames can be made a bearing and back.</li>
 *   <li><b>Sneak-use</b> toggles whether the bearing carries rotation across or spins free.</li>
 * </ul>
 *
 * <p>The tool is a switch, not a dial: a bearing is one of a few named things, never a stiffness
 * (philosophy section 3). A hinge takes its axis from the tube on the point if there is exactly
 * one -- a shaft turns about itself -- otherwise the axis nearest the look.
 *
 * <p>It is deliberately not the shaft Bearing block. That is a support for a rotating shaft and
 * belongs to the mechanical network; this is a joint between two rigid bodies, and the two have
 * nothing in common but the word.
 */
public class PivotItem extends Item {

    public PivotItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide)
            return InteractionResultHolder.success(stack);

        Aim aim = WelderItem.aim(level, player);
        if (aim == null) {
            player.displayClientMessage(Component.translatable("message.feedback.welder.no_frame"), true);
            return InteractionResultHolder.consume(stack);
        }
        FrameBlockEntity be = aim.be();
        Frame frame = be.frame();
        int hit = WelderItem.pick(frame, Vec3.atLowerCornerOf(be.getBlockPos()), aim.eye(), aim.look(), player.blockInteractionRange());
        if (hit < 0) {
            player.displayClientMessage(Component.translatable("message.feedback.pivot.none"), true);
            return InteractionResultHolder.consume(stack);
        }

        List<FrameBlockEntity.Held> joined = be.jointsAt(hit);
        if (!joined.isEmpty()) {
            for (FrameBlockEntity.Held h : joined) {
                FrameJoint was = h.joint();
                Vec3 axis = WelderItem.hingeAxis(be, hit, h.other(), h.theirs(be), aim.look());
                FrameJoint now = player.isShiftKeyDown() ? was.toggleDrive()
                        : was.withKind(was.kind() == FrameJoint.Kind.WELD ? FrameJoint.Kind.HINGE : was.kind().nextPivot(), axis);
                be.replace(h.other(), was, now);
                WelderItem.say(player, now);
            }
            return InteractionResultHolder.consume(stack);
        }
        if (be.assembled()) {
            player.displayClientMessage(Component.translatable("message.feedback.pivot.assembled"), true);
            return InteractionResultHolder.consume(stack);
        }

        Frame.Pivot was = frame.pivots().get(hit);
        if (player.isShiftKeyDown()) {
            if (was == null) {
                player.displayClientMessage(Component.translatable("message.feedback.pivot.not_a_bearing"), true);
                return InteractionResultHolder.consume(stack);
            }
            frame.setPivot(hit, was.withDrive(!was.drives()));
        } else {
            FrameJoint.Kind next = was == null ? FrameJoint.Kind.HINGE : switch (was.kind()) {
                case HINGE -> FrameJoint.Kind.BALL;
                case BALL -> FrameJoint.Kind.LOCKED;
                default -> null;
            };
            Vec3 axis = was != null && was.kind() == FrameJoint.Kind.HINGE ? was.axis() : null;
            if (next == FrameJoint.Kind.HINGE) {
                Vec3 along = WelderItem.tubeDirection(frame, hit);
                axis = along != null ? along : WelderItem.nearestAxis(aim.look());
            }
            frame.setPivot(hit, next == null ? null : new Frame.Pivot(next, axis != null ? axis : new Vec3(0, 1, 0),
                    was != null && was.drives()));
        }
        be.changed();

        Frame.Pivot now = frame.pivots().get(hit);
        player.displayClientMessage(now == null ? Component.translatable("message.feedback.pivot.cleared")
                : Component.translatable("message.feedback.pivot." + now.kind().name().toLowerCase(Locale.ROOT),
                        String.format("(%.2f, %.2f, %.2f)", now.axis().x, now.axis().y, now.axis().z))
                .append(Component.translatable(now.drives() ? "message.feedback.pivot.drives" : "message.feedback.pivot.free")), true);
        return InteractionResultHolder.consume(stack);
    }
}
