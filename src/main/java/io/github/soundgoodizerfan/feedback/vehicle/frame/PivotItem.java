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
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
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
 * Sets a point to be a bearing, and switches what kind ({@code docs/feedback_vehicles_spec.md}
 * section 4a). Look at a point and use:
 *
 * <ul>
 *   <li><b>A point of a frame</b> cycles it plain, hinge, steered hinge, ball, locked, plain. A
 *       steered hinge is a kingpin: the frame's steering level turns it (spec section 5,
 *       <em>Steering</em>). On a frame not
 *       yet a body it is only recorded and wakes up when the chassis is assembled; on a vehicle
 *       it takes effect at once, the cut part going back to its rest pose as it is re-made.</li>
 *   <li><b>A point with a joint on it</b> -- two frames welded or already bearing -- cycles that
 *       joint hinge, ball, locked, so a weld between frames can be made a bearing and back.</li>
 *   <li><b>Sneak-use</b> toggles whether the bearing carries rotation across or spins free.</li>
 *   <li><b>A hinge, with a block in the off hand</b>, is made a hub: the block's material is the
 *       tire's ({@code docs/feedback_vehicles_spec.md} section 5, step 4). The same material again steps
 *       the size up, large wrapping to small, as tubes do. Cycling the hinge away takes the tire off.</li>
 *   <li><b>A hub, sneaking, with a block in the off hand</b>, is given a brake lined with that
 *       block's material; the same material again takes it off.</li>
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

    /**
     * Claims the click before the block under the crosshair gets it. Points hang in the air where
     * vanilla's block hit cannot see them, so a point in front of an engine used to send the click
     * to the engine -- switching it on and off -- and never to this tool (playtest 2026-10-16).
     * Near a frame, every click is the tool's.
     */
    @Override
    public net.minecraft.world.InteractionResult onItemUseFirst(ItemStack stack, net.minecraft.world.item.context.UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || WelderItem.aim(context.getLevel(), player) == null)
            return net.minecraft.world.InteractionResult.PASS;
        return use(context.getLevel(), player, context.getHand()).getResult();
    }

    /**
     * What point {@code hit} now is, given to the rest of a Welder selection it belongs to -- when
     * that selection was clicked point by point, not boxed (author, 2026-10-16) -- and to every
     * mirror image. A hinge takes its axis from its own tube where it has one.
     */
    private static void spread(Player player, FrameBlockEntity be, int hit) {
        Frame frame = be.frame();
        List<Integer> also = new java.util.ArrayList<>();
        for (ItemStack s : player.getInventory().items)
            if (s.getItem() instanceof WelderItem) {
                WelderSelected sel = WelderItem.selected(s, be);
                if (sel != null && sel.clicked() && sel.vertices().contains(hit)) {
                    also.addAll(sel.vertices());
                    break;
                }
            }
        also.remove((Integer) hit);
        Frame.Pivot p = frame.pivots().get(hit);
        Frame.Tire t = frame.tires().get(hit);
        for (int v : also) {
            if (p == null)
                frame.setPivot(v, null);
            else {
                Vec3 own = p.kind() == FrameJoint.Kind.HINGE ? WelderItem.tubeDirection(frame, v) : null;
                frame.setPivot(v, p.withAxis(own != null ? own : p.axis()));
            }
            if (t != null && p != null && p.kind() == FrameJoint.Kind.HINGE)
                frame.setTire(v, t);
        }
        Symmetry.copyPoint(frame, hit);
        also.forEach(v -> Symmetry.copyPoint(frame, v));
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
        Frame.Pivot was = frame.pivots().get(hit);
        ResourceLocation stock = FrameMaterial.plate(player.getOffhandItem());
        if (!player.isShiftKeyDown() && was != null && was.kind() == FrameJoint.Kind.HINGE && stock != null) {
            ResourceLocation material = stock;
            Block block = BuiltInRegistries.BLOCK.get(material);
            if (Material.find(MaterialTable.get().materials(), block.asItem()).map(m -> m.material().mechanical())
                    .flatMap(Material.Mechanical::friction).isEmpty()) {
                player.displayClientMessage(Component.translatable("message.feedback.tire.no_grip"), true);
                return InteractionResultHolder.consume(stack);
            }
            Frame.Tire on = frame.tires().get(hit);
            Frame.Tire tire = new Frame.Tire(material, on != null && on.material().equals(material)
                    ? (on.size() + 1) % Frame.TIRE_SIZES.length : 1, null, on != null ? on.suspension() : Frame.Tire.STOCK);
            frame.setTire(hit, tire);
            spread(player, be, hit);
            be.changed();
            player.displayClientMessage(Component.translatable("message.feedback.tire.set",
                    block.getName(), Component.translatable("message.feedback.tire.size." + tire.size())), true);
            return InteractionResultHolder.consume(stack);
        }
        // A second pivot tool in the off hand, sneaking: the hub's suspension band, soft / stock / stiff.
        // The same, on a slider: its spring's band.
        if (player.isShiftKeyDown() && was != null && was.kind() == FrameJoint.Kind.SLIDER && player.getOffhandItem().getItem() instanceof PivotItem) {
            int band = (was.band() + 1) % 3;
            frame.setPivot(hit, was.withBand(band));
            spread(player, be, hit);
            be.changed();
            player.displayClientMessage(Component.translatable("message.feedback.suspension." + band), true);
            return InteractionResultHolder.consume(stack);
        }
        if (player.isShiftKeyDown() && frame.tires().containsKey(hit) && player.getOffhandItem().getItem() instanceof PivotItem) {
            Frame.Tire on = frame.tires().get(hit);
            int band = (on.suspension() + 1) % 3;
            frame.setTire(hit, on.withSuspension(band));
            spread(player, be, hit);
            be.changed();
            player.displayClientMessage(Component.translatable("message.feedback.suspension." + band), true);
            return InteractionResultHolder.consume(stack);
        }
        // A second pivot tool in the off hand, not sneaking, on a driving point: steps none, open, limited slip, locked.
        if (!player.isShiftKeyDown() && was != null && was.drives() && !frame.tires().containsKey(hit)
                && player.getOffhandItem().getItem() instanceof PivotItem) {
            int next = (was.differential() + 1) % 4;
            frame.setPivot(hit, was.withDifferential(next));
            be.changed();
            player.displayClientMessage(Component.translatable("message.feedback.pivot.differential." + next), true);
            return InteractionResultHolder.consume(stack);
        }
        if (player.isShiftKeyDown() && frame.tires().containsKey(hit) && player.getOffhandItem().getItem() instanceof BlockItem block) {
            ResourceLocation pad = BuiltInRegistries.BLOCK.getKey(block.getBlock());
            if (Material.find(MaterialTable.get().materials(), block).map(m -> m.material().mechanical())
                    .flatMap(Material.Mechanical::friction).isEmpty()) {
                player.displayClientMessage(Component.translatable("message.feedback.tire.no_grip"), true);
                return InteractionResultHolder.consume(stack);
            }
            Frame.Tire on = frame.tires().get(hit);
            boolean off = pad.equals(on.brake());
            frame.setTire(hit, on.withBrake(off ? null : pad));
            spread(player, be, hit);
            be.changed();
            player.displayClientMessage(off ? Component.translatable("message.feedback.brake.off")
                    : Component.translatable("message.feedback.brake.set", block.getDescription()), true);
            return InteractionResultHolder.consume(stack);
        }
        if (player.isShiftKeyDown()) {
            if (was == null) {
                player.displayClientMessage(Component.translatable("message.feedback.pivot.not_a_bearing"), true);
                return InteractionResultHolder.consume(stack);
            }
            frame.setPivot(hit, was.withDrive(!was.drives()));
        } else {
            boolean hinge = was != null && was.kind() == FrameJoint.Kind.HINGE;
            // A plain hinge steps to a steered one, which keeps its axis: a kingpin is set by
            // the same look as the hinge it was.
            boolean steers = hinge && !was.steers();
            FrameJoint.Kind next = was == null ? FrameJoint.Kind.HINGE : steers ? FrameJoint.Kind.HINGE : switch (was.kind()) {
                case HINGE -> FrameJoint.Kind.BALL;
                case BALL -> FrameJoint.Kind.LOCKED;
                case LOCKED -> FrameJoint.Kind.SLIDER;
                default -> null;
            };
            Vec3 axis = was != null ? was.axis() : null;
            if (was == null) {
                Vec3 along = WelderItem.tubeDirection(frame, hit);
                axis = along != null ? along : WelderItem.nearestAxis(aim.look());
            }
            frame.setPivot(hit, next == null ? null : new Frame.Pivot(next, axis != null ? axis : new Vec3(0, 1, 0),
                    was != null && was.drives(), steers));
        }
        spread(player, be, hit);
        be.changed();

        Frame.Pivot now = frame.pivots().get(hit);
        player.displayClientMessage(now == null ? Component.translatable("message.feedback.pivot.cleared")
                : Component.translatable("message.feedback.pivot." + (now.steers() ? "steered" : now.kind().name().toLowerCase(Locale.ROOT)),
                        String.format("(%.2f, %.2f, %.2f)", now.axis().x, now.axis().y, now.axis().z))
                .append(Component.translatable(now.drives() ? "message.feedback.pivot.drives" : "message.feedback.pivot.free")), true);
        return InteractionResultHolder.consume(stack);
    }
}
