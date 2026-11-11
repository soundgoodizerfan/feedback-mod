// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
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
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Sets a point to be a bearing, and switches what kind ({@code docs/specs/feedback_vehicles_spec.md}
 * section 4a). Look at a point and use:
 *
 * <ul>
 *   <li><b>A point of a frame</b> cycles it plain, hinge, steered hinge, ball, locked, slider,
 *       plain. A steered hinge is a kingpin: the frame's steering level turns it (spec section 5,
 *       <em>Steering</em>); a slider is the running gear's spring. On a frame not
 *       yet a body it is only recorded and wakes up when the chassis is assembled; on a vehicle
 *       it takes effect at once, the cut part going back to its rest pose as it is re-made.</li>
 *   <li><b>A point with a joint on it</b> -- two frames welded or already bearing -- cycles that
 *       joint hinge, ball, locked, so a weld between frames can be made a bearing and back.</li>
 *   <li><b>Sneak-use</b> toggles whether the bearing carries rotation across or spins free.</li>
 *   <li><b>A hinge, with a block in the off hand</b>, is made a hub: the block's material is the
 *       tire's ({@code docs/specs/feedback_vehicles_spec.md} section 5, step 4). The same material again steps
 *       the size up, large wrapping to small, as tubes do. Cycling the hinge away takes the tire off.</li>
 *   <li><b>A hub, sneaking, with a block in the off hand</b>, is given a brake lined with that
 *       block's material; the same material again takes it off.</li>
 *   <li><b>A second Pivot Setter in the off hand</b> sets the running gear's finer choices: sneaking
 *       on a hub or a slider steps its spring band, soft / stock / stiff; not sneaking on a driving
 *       point with no tire steps its differential, none / open / limited slip / locked.</li>
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

    /** The control-surface roles a hinge steps through: roll, pitch, yaw, each then its opposite sense. */
    private static final java.util.List<Integer> SURFACES = java.util.List.of(1, -1, 2, -2, 3, -3);

    public PivotItem(Properties properties) {
        super(properties);
    }

    /** Claims the click before the block under the crosshair gets it, as the Welder does: near a frame, every click is the tool's. */
    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        return WelderItem.claim(this, context);
    }

    /**
     * What point {@code hit} now is, given to the rest of a Welder selection it belongs to -- when
     * that selection was clicked point by point, not boxed (author, 2026-10-16) -- and to every
     * mirror image. A hinge takes its axis from its own tube where it has one.
     */
    private static void spread(Player player, FrameBlockEntity be, int hit) {
        Frame frame = be.frame();
        List<Integer> also = new ArrayList<>();
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

    /** Whether {@code item}'s material has a friction figure: with none it is neither a tread nor a brake lining. */
    private static boolean grips(Item item) {
        return Material.find(MaterialTable.get().materials(), item).map(m -> m.material().mechanical())
                .flatMap(Material.Mechanical::friction).isPresent();
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
        ResourceLocation material = FrameMaterial.plate(player.getOffhandItem());
        // A kingpin or a stator takes no tire (Frame.setTire): there the click cycles it, as without a block.
        if (!player.isShiftKeyDown() && was != null && was.kind() == FrameJoint.Kind.HINGE && !was.steers()
                && !frame.rotors().containsKey(hit) && material != null) {
            Block block = BuiltInRegistries.BLOCK.get(material);
            if (!grips(block.asItem())) {
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
            if (!grips(block)) {
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
            if (frame.rotors().containsValue(hit)) {
                player.displayClientMessage(Component.translatable("message.feedback.pivot.is_rotor"), true);
                return InteractionResultHolder.consume(stack);
            }
            boolean stator = frame.rotors().containsKey(hit);
            boolean hinge = !stator && was != null && was.kind() == FrameJoint.Kind.HINGE;
            // A hinge steps through its roles -- plain, steered, then the control surfaces (roll, pitch and
            // yaw, each in both senses) -- and then on to a ball; every role keeps the hinge's axis, so
            // one look sets them all. A stator only swaps hinge and ball, keeping its rotor.
            int role = hinge ? (was.steers() ? 1 : was.surface() == 0 ? 0 : 2 + SURFACES.indexOf(was.surface())) : -1;
            boolean stay = hinge && role + 1 <= 1 + SURFACES.size();
            FrameJoint.Kind next = stator ? (was.kind() == FrameJoint.Kind.HINGE ? FrameJoint.Kind.BALL : FrameJoint.Kind.HINGE)
                    : was == null || stay ? FrameJoint.Kind.HINGE : switch (was.kind()) {
                case HINGE -> FrameJoint.Kind.BALL;
                case BALL -> FrameJoint.Kind.LOCKED;
                case LOCKED -> FrameJoint.Kind.SLIDER;
                default -> null;
            };
            Vec3 along = was == null ? WelderItem.tubeDirection(frame, hit) : null;
            Vec3 axis = was != null ? was.axis() : along != null ? along : WelderItem.nearestAxis(aim.look());
            Frame.Pivot made = next == null ? null : new Frame.Pivot(next, axis != null ? axis : new Vec3(0, 1, 0),
                    was != null && was.drives(), stay && role + 1 == 1);
            if (made != null && stay && role + 1 >= 2)
                made = made.withSurface(SURFACES.get(role + 1 - 2));
            frame.setPivot(hit, made);
            if (was == null) {
                // The first click from one side makes the stator: its rotor goes one step on, the way you look.
                Frame.Vertex s = frame.vertices().get(hit);
                WelderItem.placeRotor(player, frame, hit, new Frame.Vertex(s.x() + (int) Math.round(aim.look().x * 8),
                        s.y() + (int) Math.round(aim.look().y * 8), s.z() + (int) Math.round(aim.look().z * 8)));
            }
        }
        spread(player, be, hit);
        be.changed();

        Frame.Pivot now = frame.pivots().get(hit);
        player.displayClientMessage(now == null ? Component.translatable("message.feedback.pivot.cleared")
                : Component.translatable("message.feedback.pivot." + (now.steers() ? "steered" : now.surface() != 0 ? "surface" + now.surface() : now.kind().name().toLowerCase(Locale.ROOT)),
                        String.format("(%.2f, %.2f, %.2f)", now.axis().x, now.axis().y, now.axis().z))
                .append(Component.translatable(now.drives() ? "message.feedback.pivot.drives" : "message.feedback.pivot.free")), true);
        return InteractionResultHolder.consume(stack);
    }
}
