// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.HaftedLayers;
import io.github.soundgoodizerfan.feedback.modular.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.ModularType;
import io.github.soundgoodizerfan.feedback.modular.ModularTypes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.geometry.UnbakedGeometryHelper;

import org.joml.Vector3f;

/**
 * The hafted tool drawn as a stack of layers (haft, parts, heads) the {@link HaftedLayers} layout places, each a 16x16 grey sprite from
 * {@code textures/item/hafted/} moved by whole texels and tinted by its part's material (the tint index is the role's place in the type's
 * order, as {@link ModularClient} tints). A build whose haft or head has no layer files falls through to the whole-tool models of the item
 * model's overrides, so the layers can land a batch at a time. The geometry is built once per distinct layout and kept.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class HaftedModel {

    private HaftedModel() {
    }

    private static final ResourceLocation ITEM = Feedback.id("hafted_tool");

    @SubscribeEvent
    public static void wrap(ModelEvent.ModifyBakingResult event) {
        ModelResourceLocation key = ModelResourceLocation.inventory(ITEM);
        BakedModel base = event.getModels().get(key);
        if (base != null)
            event.getModels().put(key, new Layered(base));
    }

    private static final class Layered extends BakedModelWrapper<BakedModel> {
        private final ItemOverrides overrides;

        Layered(BakedModel base) {
            super(base);
            this.overrides = new Resolver(base);
        }

        @Override
        public ItemOverrides getOverrides() {
            return overrides;
        }
    }

    /**
     * The tool at two sizes: the icon that fits its slot in a screen, and the full-length tool everywhere in the world (in hand, on the
     * ground, in a frame), where a longer haft or blade takes up more space at the same texel size.
     */
    private static final class BySize extends BakedModelWrapper<BakedModel> {
        private final BakedModel held;

        BySize(BakedModel slot, BakedModel held) {
            super(slot);
            this.held = held;
        }

        @Override
        public BakedModel applyTransform(net.minecraft.world.item.ItemDisplayContext context, com.mojang.blaze3d.vertex.PoseStack pose, boolean leftHand) {
            return (context == net.minecraft.world.item.ItemDisplayContext.GUI ? originalModel : held).applyTransform(context, pose, leftHand);
        }
    }

    private static final class Resolver extends ItemOverrides {
        private final BakedModel base;
        private final Map<String, TextureAtlasSprite> sprites = new HashMap<>();
        private final Map<String, int[]> bounds = new HashMap<>();
        private final Map<List<HaftedLayers.Placement>, BakedModel> built = new HashMap<>();
        private final Map<List<BakedModel>, BakedModel> bySize = new HashMap<>();

        Resolver(BakedModel base) {
            this.base = base;
        }

        private TextureAtlasSprite sprite(String file) {
            return sprites.computeIfAbsent(file, f -> Minecraft.getInstance().getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS)
                    .getSprite(Feedback.id("item/hafted/" + f)));
        }

        private final HaftedLayers.Sprites known = new HaftedLayers.Sprites() {
            @Override
            public boolean has(String file) {
                return !sprite(file).contents().name().equals(MissingTextureAtlasSprite.getLocation());
            }

            @Override
            public int[] bounds(String file) {
                return bounds.computeIfAbsent(file, f -> {
                    var c = sprite(f).contents();
                    int x0 = c.width(), y0 = c.height(), x1 = -1, y1 = -1;
                    for (int y = 0; y < c.height(); y++)
                        for (int x = 0; x < c.width(); x++)
                            if (!c.isTransparent(0, x, y)) {
                                x0 = Math.min(x0, x);
                                y0 = Math.min(y0, y);
                                x1 = Math.max(x1, x);
                                y1 = Math.max(y1, y);
                            }
                    return x1 < 0 ? null : new int[] {x0, y0, x1, y1};
                });
            }
        };

        @Override
        public BakedModel resolve(BakedModel model, ItemStack stack, ClientLevel level, LivingEntity entity, int seed) {
            ModularDesign design = ModularStacks.design(stack);
            ModularType type = design == null ? null : ModularTypes.known().get(design.type());
            if (type != null) {
                List<HaftedLayers.Placement> icon = HaftedLayers.of(design, type, known);
                if (!icon.isEmpty()) {
                    BakedModel slot = built.computeIfAbsent(icon, l -> bake(l, type));
                    BakedModel held = built.computeIfAbsent(HaftedLayers.full(design, type, known), l -> bake(l, type));
                    return slot == held ? slot : bySize.computeIfAbsent(List.of(slot, held), k -> new BySize(slot, held));
                }
            }
            return base.getOverrides().resolve(base, stack, level, entity, seed);
        }

        /** Canvas texels the flattened grid reaches past the 16x16 canvas on every side: room for a full-length tool. */
        private static final int PAD = 16, GRID = HaftedLayers.SIZE + 2 * PAD;

        private BakedModel bake(List<HaftedLayers.Placement> layers, ModularType type) {
            SimpleBakedModel.Builder out = new SimpleBakedModel.Builder(false, false, false, base.getTransforms(), ItemOverrides.EMPTY);
            List<String> order = type.order();
            // Flatten first: each canvas texel belongs to the topmost layer drawing it, so no two faces ever share a plane (stacked
            // 1-texel slabs z-fight wherever layers overlap) and two heads merge into one outline.
            int n = GRID;
            int[] owner = new int[n * n];
            java.util.Arrays.fill(owner, -1);
            HaftedLayers.Placement tip = null;
            for (int i = 0; i < layers.size(); i++) {
                HaftedLayers.Placement p = layers.get(i);
                if (p.role().equals("T")) {
                    tip = p;
                    continue;
                }
                var c = sprite(p.file()).contents();
                for (int sy = 0; sy < c.height(); sy++)
                    for (int sx = 0; sx < c.width(); sx++) {
                        int x = sx + p.dx() + PAD, y = sy + p.dy() + PAD;
                        if (x >= 0 && x < n && y >= 0 && y < n && p.keeps(x - PAD, y - PAD) && !c.isTransparent(0, sx, sy))
                            owner[x + y * n] = i;
                    }
            }
            boolean[] tipped = tip == null ? null : tipMask(layers, owner, tip);
            boolean overlay = tip != null && known.has(tip.file());
            List<BlockElement> elements = new ArrayList<>();
            for (int y = 0; y < n; y++)
                for (int x = 0; x < n; x++) {
                    int i = owner[x + y * n];
                    if (i < 0)
                        continue;
                    HaftedLayers.Placement p = layers.get(i);
                    int tint = Math.max(0, order.indexOf(p.role()));
                    String file = p.file();
                    int cx = x - PAD, cy = y - PAD, ux = cx - p.dx(), uy = cy - p.dy();
                    if (tipped != null && tipped[x + y * n]) {
                        // The tip is the head's own texel in the tip's material, or the tip overlay's texel at this canvas spot where it has one.
                        tint = Math.max(0, order.indexOf("T"));
                        if (overlay && cx >= 0 && cx < HaftedLayers.SIZE && cy >= 0 && cy < HaftedLayers.SIZE
                                && !sprite(tip.file()).contents().isTransparent(0, cx, cy)) {
                            file = tip.file();
                            ux = cx;
                            uy = cy;
                        } else if (rim(owner, x + y * n)) {
                            // On the silhouette: the outline shade (the head sprite's darkest texel) in the tip's material, so the tip is outlined.
                            int[] dark = darkest(file);
                            ux = dark[0];
                            uy = dark[1];
                        }
                    }
                    String tex = Feedback.id("item/hafted/" + file).toString();
                    // UV of this texel in its own sprite, pulled in a hair so nearest sampling never picks up the neighbour.
                    float u0 = ux + 0.01f, v0 = uy + 0.01f, u1 = u0 + 0.98f, v1 = v0 + 0.98f;
                    Map<net.minecraft.core.Direction, BlockElementFace> faces = new java.util.EnumMap<>(net.minecraft.core.Direction.class);
                    faces.put(net.minecraft.core.Direction.SOUTH, new BlockElementFace(null, tint, tex, new BlockFaceUV(new float[] {u0, v0, u1, v1}, 0)));
                    faces.put(net.minecraft.core.Direction.NORTH, new BlockElementFace(null, tint, tex, new BlockFaceUV(new float[] {u1, v0, u0, v1}, 0)));
                    BlockFaceUV edge = new BlockFaceUV(new float[] {u0, v0, u1, v1}, 0);
                    if (x == 0 || owner[x - 1 + y * n] < 0)
                        faces.put(net.minecraft.core.Direction.WEST, new BlockElementFace(null, tint, tex, edge));
                    if (x == n - 1 || owner[x + 1 + y * n] < 0)
                        faces.put(net.minecraft.core.Direction.EAST, new BlockElementFace(null, tint, tex, edge));
                    if (y == 0 || owner[x + (y - 1) * n] < 0)
                        faces.put(net.minecraft.core.Direction.UP, new BlockElementFace(null, tint, tex, edge));
                    if (y == n - 1 || owner[x + (y + 1) * n] < 0)
                        faces.put(net.minecraft.core.Direction.DOWN, new BlockElementFace(null, tint, tex, edge));
                    elements.add(new BlockElement(new Vector3f(cx, 15 - cy, 7.5f), new Vector3f(cx + 1, 16 - cy, 8.5f), faces, null, true));
                }
            for (BakedQuad q : UnbakedGeometryHelper.bakeElements(elements, m -> sprite(m.texture().getPath().substring("item/hafted/".length())),
                    BlockModelRotation.X0_Y0))
                out.addUnculledFace(q);
            return out.particle(sprite(layers.get(0).file())).build();
        }

        /**
         * The canvas texels the tip covers: the front head's own texels farthest from the socket, in a band as deep as the tip's variant
         * asks (a needle is the very point, a cap runs further back). Being picked from the head's texels, it cannot miss the head.
         */
        private static boolean[] tipMask(List<HaftedLayers.Placement> layers, int[] owner, HaftedLayers.Placement tip) {
            int n = GRID;
            String frontRole = null;
            int sx = HaftedLayers.SOCKET_X + PAD, sy = HaftedLayers.SOCKET_Y + PAD;
            for (HaftedLayers.Placement p : layers) {
                if (frontRole == null && p.file().startsWith("head_"))
                    frontRole = p.role();
                if (p.file().startsWith("handle_")) {
                    // The last haft layer drawn is the topmost step: the socket is its head end.
                    sx = HaftedLayers.SOCKET_X + p.dx() + PAD;
                    sy = HaftedLayers.SOCKET_Y + p.dy() + PAD;
                }
            }
            boolean[] out = new boolean[n * n];
            if (frontRole == null)
                return out;
            float[] dist = new float[n * n];
            float far = -1;
            for (int k = 0; k < n * n; k++) {
                if (owner[k] < 0 || !layers.get(owner[k]).role().equals(frontRole))
                    continue;
                dist[k] = (float) Math.hypot(k % n - sx, k / n - sy);
                far = Math.max(far, dist[k]);
            }
            float depth = tip.file().endsWith("_needle") ? TIP_NEEDLE : tip.file().endsWith("_capped") ? TIP_CAPPED : TIP_PLAIN;
            for (int k = 0; k < n * n; k++)
                out[k] = owner[k] >= 0 && layers.get(owner[k]).role().equals(frontRole) && dist[k] >= far - depth;
            return out;
        }

        /** Whether texel {@code k} is on the silhouette's outline (a side neighbour empty or off the canvas). */
        private static boolean rim(int[] owner, int k) {
            int n = GRID, x = k % n, y = k / n;
            return x == 0 || y == 0 || x == n - 1 || y == n - 1
                    || owner[k - 1] < 0 || owner[k + 1] < 0 || owner[k - n] < 0 || owner[k + n] < 0;
        }

        private final Map<String, int[]> darkest = new HashMap<>();

        /** The darkest opaque texel of a sprite, {@code {x, y}}: the outline shade in this grey ramp. */
        private int[] darkest(String file) {
            return darkest.computeIfAbsent(file, f -> {
                var img = sprite(f).contents().getOriginalImage();
                int[] best = {0, 0};
                int low = Integer.MAX_VALUE;
                for (int y = 0; y < img.getHeight(); y++)
                    for (int x = 0; x < img.getWidth(); x++) {
                        int abgr = img.getPixelRGBA(x, y);
                        int lum = (abgr & 0xFF) + (abgr >> 8 & 0xFF) + (abgr >> 16 & 0xFF);
                        if ((abgr >>> 24) > 0 && lum < low) {
                            low = lum;
                            best = new int[] {x, y};
                        }
                    }
                return best;
            });
        }

        /** How far back from the head's farthest texel each tip runs, in texels: tuned by eye. */
        private static final float TIP_NEEDLE = 1.5f, TIP_PLAIN = 2.5f, TIP_CAPPED = 3.5f;
    }
}
