// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.gun;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.modular.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.ModularType;
import io.github.soundgoodizerfan.feedback.modular.ModularTypes;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * A gun drawn in a screen, bare (no hands), turning slowly; drag with the left button to turn it by hand, scroll to zoom, drag with the right button to move the camera. The
 * Blueprint Table holds one beside its panel to show the design on the canvas.
 */
public final class GunPreview {

    private ModularDesign shown;
    private ItemStack stack = ItemStack.EMPTY;
    private float yaw = 90f, pitch = 10f;
    private boolean dragging;
    private long last = System.currentTimeMillis();

    private static final int TAB_W = 14, TAB_H = 44, DRAWER_W = 150, DRAWER_H = 150;

    private boolean open, paused;
    private static final int PAUSE = 12;
    /** Drawer and tab boxes, set each frame by {@link #draw}; zero width when not shown. */
    private int bx, by, bw, bh, tx, ty, tw;

    /** The camera: pan in screen pixels, zoom as a multiple of the fitted size. Rotation is separate ({@link #yaw}). */
    private float zoom = 1f, panX, panY;
    private boolean panning;

    private boolean inDrawer(double mx, double my) {
        return open && bw > 0 && mx >= bx && mx < bx + bw && my >= by && my < by + bh;
    }

    private boolean inButton(double mx, double my, int slot) {
        int x = bx + 6 + slot * (PAUSE + 4);
        return open && bw > 0 && mx >= x && mx < x + PAUSE && my >= by + bh - 6 - PAUSE && my < by + bh - 6;
    }

    /** Handles a click: the tab toggles the drawer, the buttons pause and reset the camera, left in the drawer turns the gun, right moves the camera. True if it took the click. */
    public boolean click(double mx, double my, int button) {
        if (button == 0 && tw > 0 && mx >= tx && mx < tx + tw && my >= ty && my < ty + TAB_H) {
            open = !open;
            return true;
        }
        if (button == 0 && inButton(mx, my, 0)) {
            paused = !paused;
            return true;
        }
        if (button == 0 && inButton(mx, my, 1)) {
            zoom = 1f;
            panX = panY = 0f;
            return true;
        }
        if (!inDrawer(mx, my))
            return false;
        dragging = button == 0;
        panning = button == 1;
        return dragging || panning;
    }

    public boolean drag(double dx, double dy) {
        if (panning) {
            panX += (float) dx;
            panY += (float) dy;
            return true;
        }
        if (!dragging)
            return false;
        yaw += (float) dx * 0.7f;
        pitch = Mth.clamp(pitch + (float) dy * 0.7f, -89f, 89f);
        return true;
    }

    /** Scroll over the drawer zooms; true if it did. */
    public boolean scroll(double mx, double my, double dy) {
        if (!inDrawer(mx, my))
            return false;
        zoom = Mth.clamp(zoom * (float) Math.pow(1.15, dy), 0.3f, 6f);
        return true;
    }

    /** The hint for the button under the cursor, drawn on top of everything else the screen drew. */
    public void tooltip(GuiGraphics g, int mx, int my) {
        if (tw == 0)
            return;
        String key = inButton(mx, my, 0) ? paused ? "play" : "pause" : inButton(mx, my, 1) ? "reset" : null;
        if (key != null)
            g.renderTooltip(net.minecraft.client.Minecraft.getInstance().font,
                    net.minecraft.network.chat.Component.translatable("tooltip.feedback.preview." + key), mx, my);
    }

    public void release() {
        dragging = false;
        panning = false;
    }

    /**
     * A tab on the panel's left edge ({@code panelLeft}) pops a blueprint-skinned drawer out to its left holding the
     * gun. {@code design} null, or no room beside the panel, draws nothing.
     */
    public void draw(GuiGraphics g, io.github.soundgoodizerfan.feedback.client.GuiSkin skin, ModularDesign design, int panelLeft, int panelTop) {
        int dw = Math.min(DRAWER_W, panelLeft - TAB_W);
        tw = bw = 0;
        if (design == null || dw < 60) {
            release();
            return;
        }
        tw = TAB_W;
        tx = panelLeft - TAB_W;
        ty = panelTop + 24;
        var font = net.minecraft.client.Minecraft.getInstance().font;
        if (open) {
            bx = tx - dw;
            by = panelTop + 16;
            bw = dw;
            bh = DRAWER_H;
            skin.panel(g, bx, by, bw, bh);
        }
        skin.panel(g, tx, ty, tw, TAB_H);
        g.drawCenteredString(font, open ? ">" : "<", tx + tw / 2, ty + (TAB_H - 8) / 2, skin.ink());
        if (!open)
            return;
        int x = bx + 6, y = by + 6, w = bw - 12, h = bh - 12 - PAUSE - 4;
        skin.inset(g, x, y, w, h);
        // Too small for a panel's border, so an inset with the glyph filled in by hand.
        int px = bx + 6, py = by + bh - 6 - PAUSE, ink = 0xFF000000 | skin.ink();
        skin.inset(g, px, py, PAUSE, PAUSE);
        if (paused) {
            for (int i = 0; i < 3; i++)
                g.fill(px + 4 + i * 2, py + 3 + i, px + 6 + i * 2, py + 9 - i, ink);
        } else {
            g.fill(px + 3, py + 3, px + 5, py + 9, ink);
            g.fill(px + 7, py + 3, px + 9, py + 9, ink);
        }
        int rx = px + PAUSE + 4;
        skin.inset(g, rx, py, PAUSE, PAUSE);
        g.fill(rx + 5, py + 2, rx + 7, py + 10, ink); // a crosshair: the camera's centre
        g.fill(rx + 2, py + 5, rx + 10, py + 7, ink);
        if (!design.equals(shown)) {
            shown = design;
            stack = stackOf(design);
        }
        long now = System.currentTimeMillis();
        if (!dragging && !paused)
            yaw += (now - last) * 0.03f;
        last = now;
        g.enableScissor(x, y, x + w, y + h);
        drawStack(g, stack, x + w / 2f + panX, y + h / 2f + panY, Math.min(w, h) * zoom, yaw, pitch, 400f);
        g.disableScissor();
    }

    private static final java.util.Map<ModularDesign, ItemStack> BUILT = new java.util.HashMap<>();

    /** The item {@code design} bakes to, kept: a tooltip asks every frame. Empty if the table cannot bake it. */
    public static ItemStack stackOf(ModularDesign design) {
        if (BUILT.size() > 8)
            BUILT.clear();
        return BUILT.computeIfAbsent(design, d -> {
            ItemStack s = ModularStacks.blank(d.type()).orElse(ItemStack.EMPTY);
            return !s.isEmpty() && ModularStacks.assemble(s, d, ClientMaterials.get(), ModularTypes.known()) ? s : ItemStack.EMPTY;
        });
    }

    private static net.minecraft.client.player.RemotePlayer mannequin;

    /** Puts {@code stack} on the mannequin (the player's own skin, nothing else worn) so it draws as it looks worn; false if there is no world to hold one. */
    private static boolean dress(ItemStack stack, ModularType type) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.level == null || mc.player == null)
            return false;
        if (mannequin == null || mannequin.level() != mc.level)
            mannequin = new net.minecraft.client.player.RemotePlayer(mc.level, mc.player.getGameProfile());
        for (net.minecraft.world.entity.EquipmentSlot s : net.minecraft.world.entity.EquipmentSlot.values())
            mannequin.setItemSlot(s, s == type.slot().equipment() ? stack : ItemStack.EMPTY);
        return true;
    }

    /**
     * {@code stack} drawn centred on (cx, cy) filling about {@code size} pixels, turned by yaw and pitch, at depth z. A gun
     * is its own mesh, bare of hands; anything else goes through the item renderer (a slab of tinted layers).
     */
    public static void drawStack(GuiGraphics g, ItemStack stack, float cx, float cy, float size, float yaw, float pitch, float z) {
        if (stack.isEmpty())
            return;
        ModularDesign design = ModularStacks.design(stack);
        ModularType type = design == null ? null : ModularTypes.known().get(design.type());
        if (type != null && type.family() == ModularType.Family.ARMOUR && dress(stack, type)) {
            // The vanilla call scales its translate vector by the entity scale, so depth goes in the pose (it adds 50 itself).
            g.pose().pushPose();
            g.pose().translate(0f, 0f, z - 50f);
            net.minecraft.client.gui.screens.inventory.InventoryScreen.renderEntityInInventory(g, cx, cy + size * 0.45f, size * 0.5f,
                    new org.joml.Vector3f(0f, 0f, 0f), new org.joml.Quaternionf().rotationZ((float) Math.PI)
                            .rotateY((float) Math.toRadians(yaw - 90f)).rotateX((float) Math.toRadians(-pitch)),
                    null, mannequin);
            g.flush();
            g.pose().popPose();
            return;
        }
        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(cx, cy, z);
        if (type != null && (type.family() == ModularType.Family.GUN || type.family() == ModularType.Family.ELECTRIC_GUN)) {
            GunView view = GunView.of(stack);
            float front = -(view.muzzle() + view.muzzleReach()), back = view.rear();
            float top = view.top() + (view.has("S") ? 1.75f : 0.5f), bottom = view.bottom();
            // Fit the longest turn of the gun inside the box.
            float s = size * 0.9f / Math.max(back - front, top - bottom);
            pose.scale(s, -s, s);
            pose.mulPose(Axis.XP.rotationDegrees(pitch));
            pose.mulPose(Axis.YP.rotationDegrees(yaw));
            pose.translate(0f, -(top + bottom) / 2f, -(front + back) / 2f);
            Lighting.setupForFlatItems();
            var mesh = new GunMesh(pose, GunRenderer.outputs(g.bufferSource(), stack), 0xF000F0,
                    net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
            GunDraw.draw(mesh, view, GunMotion.Parts.REST);
        } else {
            var mc = net.minecraft.client.Minecraft.getInstance();
            var model = mc.getItemRenderer().getModel(stack, mc.level, null, 0);
            float s = size * 0.85f;
            pose.scale(s, -s, s);
            pose.mulPose(Axis.XP.rotationDegrees(pitch));
            pose.mulPose(Axis.YP.rotationDegrees(yaw));
            if (model.usesBlockLight())
                Lighting.setupFor3DItems();
            else
                Lighting.setupForFlatItems();
            mc.getItemRenderer().render(stack, net.minecraft.world.item.ItemDisplayContext.NONE, false, pose,
                    g.bufferSource(), 0xF000F0, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, model);
        }
        g.flush();
        Lighting.setupFor3DItems();
        pose.popPose();
    }
}
