// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.gun;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.client.GunClient;
import io.github.soundgoodizerfan.feedback.weapon.GunItem;
import io.github.soundgoodizerfan.feedback.weapon.GunSpec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/**
 * A scope's lens: while a scope-fitted gun is aimed, the world is drawn a second time, zoomed, into an
 * offscreen square ({@link #TEXTURE}) which {@link GunMesh#lens} puts on the scope's eyepiece. The rest of
 * the screen stays at the ordinary field of view, so the zoom lives in the lens and nowhere else.
 *
 * <p>The extra pass runs at the start of the frame through the game's own {@code renderLevel}, with the
 * hand cancelled ({@link #rendering}) and the field of view set to the lens window's angle times the
 * scope's zoom; the middle of the result is copied into the offscreen square. It runs only while aimed
 * with a scope, so every other frame costs nothing. The level renderer's visibility is invalidated after
 * it, since the narrow pass would otherwise leave the main view culled to the narrow cone.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
public final class GunLens {

    /** The lens image, registered as a texture over the offscreen target's colour. */
    public static final ResourceLocation TEXTURE = Feedback.id("gun_lens");
    private static final int SIZE = 512;
    /** Aim progress from which the lens is drawn, and below which it is dropped. */
    private static final float ON = 0.85f, OFF = 0.3f;

    private static TextureTarget target;
    private static boolean pass, fresh;
    private static double passFov;

    private GunLens() {
    }

    /** Whether our own extra world pass is running: the hand and the gun are left out of it. */
    public static boolean rendering() {
        return pass;
    }

    /** Whether the lens holds a frame of this aim. */
    static boolean ready() {
        return fresh && target != null;
    }

    /** Whether the stack is a gun fitted with a scope. */
    public static boolean hasScope(net.minecraft.world.item.ItemStack stack) {
        if (!GunLike.is(stack.getItem()))
            return false;
        GunView v = GunView.of(stack);
        return v.has("S") && v.variant("S").equals("scope");
    }

    @SubscribeEvent
    public static void onFrame(RenderFrameEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        var player = mc.player;
        float aim = GunClient.aim();
        if (aim < OFF)
            fresh = false;
        if (player == null || mc.level == null || pass || aim < ON || !mc.options.getCameraType().isFirstPerson())
            return;
        var held = player.getMainHandItem();
        if (!hasScope(held))
            return;
        GunSpec spec = GunSpec.of(held).orElse(null);
        if (spec == null)
            return;
        GunView v = GunView.of(held);
        passFov = Math.max(1.0, v.lensAngle() * spec.sight().fov());
        draw(mc, event);
    }

    private static void draw(Minecraft mc, RenderFrameEvent.Pre event) {
        RenderTarget main = mc.getMainRenderTarget();
        if (target == null) {
            target = new TextureTarget(SIZE, SIZE, true, Minecraft.ON_OSX);
            target.setClearColor(0f, 0f, 0f, 1f);
            mc.getTextureManager().register(TEXTURE, new Image());
        }
        pass = true;
        try {
            main.bindWrite(true);
            mc.gameRenderer.renderLevel(event.getPartialTick());
        } finally {
            pass = false;
        }
        // The middle square of what was drawn, scaled into the lens image.
        int side = Math.min(main.viewWidth, main.viewHeight), x0 = (main.viewWidth - side) / 2, y0 = (main.viewHeight - side) / 2;
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.frameBufferId);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, target.frameBufferId);
        GlStateManager._glBlitFrameBuffer(x0, y0, x0 + side, y0 + side, 0, 0, SIZE, SIZE, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_LINEAR);
        // Fog clears the main target with alpha 0, so sky pixels came through see-through; force the image opaque.
        target.bindWrite(false);
        GL11.glColorMask(false, false, false, true);
        GL11.glClearColor(0f, 0f, 0f, 1f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        GL11.glColorMask(true, true, true, true);
        main.bindWrite(true);
        mc.levelRenderer.needsUpdate();
        fresh = true;
    }

    /** The extra pass looks through the lens: its own field of view. */
    @SubscribeEvent
    public static void onFov(ViewportEvent.ComputeFov event) {
        if (pass && event.usedConfiguredFov())
            event.setFOV(passFov);
    }

    /** The offscreen target's colour, seen as a texture. */
    private static final class Image extends AbstractTexture {
        @Override
        public int getId() {
            return target == null ? -1 : target.getColorTextureId();
        }

        @Override
        public void releaseId() {
        }

        @Override
        public void load(ResourceManager manager) {
        }
    }
}
