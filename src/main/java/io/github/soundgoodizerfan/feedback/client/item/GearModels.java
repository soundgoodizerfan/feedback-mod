// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.client.gun.PlateDraw;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.modular.MaterialColors;
import io.github.soundgoodizerfan.feedback.modular.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.ModularType;
import io.github.soundgoodizerfan.feedback.modular.ModularTypes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/**
 * Worn modular armour on vanilla's own armour cubes (the same humanoid mesh, so it moves, sneaks and swings as vanilla's
 * does, in the dyed layer sprite) with a few extra cubes per variant of the plate: a crest, a visor, a neck guard, a
 * mail skirt, knee guards, a toe cap. Every extra rides a vanilla part, so nothing here animates by itself. The
 * variant's extras are shown and the rest hidden each time an armour piece asks for the model; studs are drawn
 * in their own material's colour after the rest.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class GearModels {

    private static final ModelLayerLocation OUTER = new ModelLayerLocation(Feedback.id("gear_outer"), "main");
    private static final ModelLayerLocation INNER = new ModelLayerLocation(Feedback.id("gear_inner"), "main");

    private GearModels() {
    }

    /** The extra cubes of a piece: where they ride (a vanilla part name) and the cube list, by extra name. */
    private record Extra(String name, String part, float[] box, CubeListBuilder cubes) {
    }

    private static CubeListBuilder cube(int u, int v, float x, float y, float z, float w, float h, float d) {
        return CubeListBuilder.create().texOffs(u, v).addBox(x, y, z, w, h, d);
    }

    /** All extras. Texture offsets pick plain regions of the layer sprite (the body's front for plate). */
    private static List<Extra> extras(boolean inner) {
        float g = inner ? -0.5f : 0f; // the legs' shell is thinner when worn as leggings
        return List.of(
                // Helmet (shell is 10 wide, 10 tall)
                new Extra("brow", "head", new float[]{-5.25f, -5f, -5.25f, 10.5f, 1, 10.5f}, cube(16, 16, -5.25f, -5f, -5.25f, 10.5f, 1, 10.5f)),
                new Extra("crest", "head", new float[]{-0.5f, -11.5f, -5f, 1, 2.5f, 10}, cube(16, 16, -0.5f, -11.5f, -5f, 1, 2.5f, 10)),
                new Extra("visor", "head", new float[]{-5.25f, -5f, -5.75f, 10.5f, 5, 1}, cube(16, 16, -5.25f, -5f, -5.75f, 10.5f, 5, 1)),
                new Extra("neck", "head", new float[]{-5.25f, 0.5f, 4f, 10.5f, 3, 1.5f}, cube(16, 16, -5.25f, 0.5f, 4f, 10.5f, 3, 1.5f)),
                new Extra("brim", "head", new float[]{-5.25f, -5.5f, -5.75f, 10.5f, 1, 1.5f}, cube(16, 16, -5.25f, -5.5f, -5.75f, 10.5f, 1, 1.5f)),
                // Chestplate (shell 10 wide, 6 deep)
                new Extra("collar", "body", new float[]{-5.25f, -0.5f, -3.25f, 10.5f, 1.5f, 6.5f}, cube(16, 16, -5.25f, -0.5f, -3.25f, 10.5f, 1.5f, 6.5f)),
                new Extra("ridge", "body", new float[]{-3f, 1f, -3.5f, 6, 7, 1}, cube(16, 16, -3f, 1f, -3.5f, 6, 7, 1)),
                new Extra("skirt", "body", new float[]{-5.25f, 12f, -3.25f, 10.5f, 2.5f, 6.5f}, cube(16, 16, -5.25f, 12f, -3.25f, 10.5f, 2.5f, 6.5f)),
                new Extra("pauldron_r", "right_arm", new float[]{-4.5f, -3.5f, -3.5f, 6.5f, 3, 7}, cube(40, 16, -4.5f, -3.5f, -3.5f, 6.5f, 3, 7)),
                new Extra("pauldron_l", "left_arm", new float[]{-2.5f, -3.5f, -3.5f, 6.5f, 3, 7}, cube(40, 16, -2.5f, -3.5f, -3.5f, 6.5f, 3, 7)),
                // Leggings
                new Extra("knee_r", "right_leg", new float[]{-2.75f, 4.5f, -3.5f - g, 5.5f, 2.5f, 1}, cube(0, 16, -2.75f, 4.5f, -3.5f - g, 5.5f, 2.5f, 1)),
                new Extra("knee_l", "left_leg", new float[]{-2.75f, 4.5f, -3.5f - g, 5.5f, 2.5f, 1}, cube(0, 16, -2.75f, 4.5f, -3.5f - g, 5.5f, 2.5f, 1)),
                new Extra("thigh_r", "right_leg", new float[]{-2.75f, 0f, -3.5f - g, 5.5f, 5.5f, 1}, cube(0, 16, -2.75f, 0f, -3.5f - g, 5.5f, 5.5f, 1)),
                new Extra("thigh_l", "left_leg", new float[]{-2.75f, 0f, -3.5f - g, 5.5f, 5.5f, 1}, cube(0, 16, -2.75f, 0f, -3.5f - g, 5.5f, 5.5f, 1)),
                // Boots (outer shell)
                new Extra("toe_r", "right_leg", new float[]{-3f, 9.5f, -3.75f, 6, 2.5f, 1.5f}, cube(0, 16, -3f, 9.5f, -3.75f, 6, 2.5f, 1.5f)),
                new Extra("toe_l", "left_leg", new float[]{-3f, 9.5f, -3.75f, 6, 2.5f, 1.5f}, cube(0, 16, -3f, 9.5f, -3.75f, 6, 2.5f, 1.5f)),
                new Extra("cuff_r", "right_leg", new float[]{-3.25f, 6.5f, -3.25f, 6.5f, 1.5f, 6.5f}, cube(0, 16, -3.25f, 6.5f, -3.25f, 6.5f, 1.5f, 6.5f)),
                new Extra("cuff_l", "left_leg", new float[]{-3.25f, 6.5f, -3.25f, 6.5f, 1.5f, 6.5f}, cube(0, 16, -3.25f, 6.5f, -3.25f, 6.5f, 1.5f, 6.5f)),
                new Extra("spur_r", "right_leg", new float[]{-0.5f, 10.5f, 3f, 1, 1, 1.5f}, cube(0, 16, -0.5f, 10.5f, 3f, 1, 1, 1.5f)),
                new Extra("spur_l", "left_leg", new float[]{-0.5f, 10.5f, 3f, 1, 1, 1.5f}, cube(0, 16, -0.5f, 10.5f, 3f, 1, 1, 1.5f)),
                // Studs, drawn in their own colour
                new Extra("studs_head", "head", new float[]{-3f, -6.5f, -5.35f, 6, 1, 0.6f}, cube(16, 16, -3f, -6.5f, -5.35f, 6, 1, 0.6f)),
                new Extra("studs_body", "body", new float[]{-3f, 2f, -3.4f, 6, 1, 0.6f}, cube(16, 16, -3f, 2f, -3.4f, 6, 1, 0.6f)),
                new Extra("studs_body2", "body", new float[]{-3f, 5f, -3.4f, 6, 1, 0.6f}, cube(16, 16, -3f, 5f, -3.4f, 6, 1, 0.6f)));
    }

    private static LayerDefinition layer(boolean inner) {
        MeshDefinition mesh = HumanoidModel.createMesh(new CubeDeformation(inner ? 0.5f : 1.0f), 0f);
        PartDefinition root = mesh.getRoot();
        for (Extra e : extras(inner))
            root.getChild(e.part())
                    .addOrReplaceChild(e.name(), e.cubes(), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }

    @SubscribeEvent
    public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(OUTER, () -> layer(false));
        event.registerLayerDefinition(INNER, () -> layer(true));
    }

    /** Which extras a variant of a slot's plate wears. */
    private static List<String> wears(EquipmentSlot slot, String variant) {
        return switch (slot) {
            case HEAD -> switch (variant) {
                case "conical", "ridged" -> List.of("crest");
                case "pointed_face" -> List.of("visor", "brim");
                case "tailed" -> List.of("neck", "brim");
                default -> List.of("brow");
            };
            case CHEST -> switch (variant) {
                case "brigandine" -> List.of("collar");
                case "breastplate" -> List.of("ridge", "pauldron_r", "pauldron_l");
                case "hauberk", "scale", "lamellar" -> List.of("collar", "skirt");
                case "segmented" -> List.of("ridge", "pauldron_r", "pauldron_l");
                default -> List.of("collar");
            };
            case LEGS -> switch (variant) {
                case "articulated" -> List.of("knee_r", "knee_l");
                case "mail_chausses" -> List.of("thigh_r", "thigh_l");
                default -> List.of();
            };
            case FEET -> switch (variant) {
                case "sabatons" -> List.of("toe_r", "toe_l");
                default -> List.of();
            };
            default -> List.of();
        };
    }

    /** The armour model: vanilla's mesh plus the extras, set up per piece by the extension. */
    private static final class Gear extends HumanoidModel<LivingEntity> {
        private final Map<String, ModelPart> extra = new HashMap<>();
        private final Map<String, ModelPart> host = new HashMap<>();
        private final Map<String, float[]> boxes = new HashMap<>();
        private List<String> on = List.of();
        private int studs = -1, plateColour = 0xFFFFFFFF, calls;
        private Material plateMaterial, studMaterial;

        Gear(ModelPart root, boolean inner) {
            super(root);
            for (Extra e : extras(inner)) {
                ModelPart parent = switch (e.part()) {
                    case "head" -> head;
                    case "body" -> body;
                    case "right_arm" -> rightArm;
                    case "left_arm" -> leftArm;
                    case "right_leg" -> rightLeg;
                    default -> leftLeg;
                };
                extra.put(e.name(), parent.getChild(e.name()));
                host.put(e.name(), parent);
                boxes.put(e.name(), e.box());
            }
        }

        void set(ItemStack stack, EquipmentSlot slot) {
            ModularDesign design = ModularStacks.design(stack);
            ModularDesign.Part plate = design == null ? null : design.part("P"), studPart = design == null ? null : design.part("T");
            on = wears(slot, plate == null ? "" : plate.variant());
            fit(design, plate, slot);
            var materials = ClientMaterials.get();
            plateMaterial = plate == null ? null : materials.get(plate.material());
            plateColour = plate == null ? 0xFFB4B4B8 : 0xFF000000 | MaterialColors.of(plate.material());
            studMaterial = studPart == null ? null : materials.get(studPart.material());
            studs = studPart != null && (slot == EquipmentSlot.HEAD || slot == EquipmentSlot.CHEST)
                    ? 0xFF000000 | MaterialColors.of(studPart.material()) : -1;
            calls = 0;
        }

        /**
         * The plate's drawn axes shape the worn piece: a taller crown, a wider chest, a longer leg stretch the host part
         * (its extras ride along), and a closed face opening or a high boot shaft adds the visor or the cuff.
         */
        private void fit(ModularDesign design, ModularDesign.Part plate, EquipmentSlot slot) {
            for (ModelPart p : new ModelPart[]{head, body, rightLeg, leftLeg})
                p.xScale = p.yScale = p.zScale = 1f;
            var type = design == null ? null : ModularTypes.known(design.type());
            var role = type == null ? null : type.roles().get("P");
            if (plate == null || role == null)
                return;
            switch (slot) {
                case HEAD -> {
                    head.yScale = scale(role, plate, "crown");
                    String band = band(role, plate, "face_opening");
                    List<String> shown = new ArrayList<>(on);
                    if (band.equals("closed"))
                        shown.add("visor");
                    else if (band.equals("open"))
                        shown.removeAll(List.of("visor", "brim"));
                    on = shown;
                }
                case CHEST -> {
                    body.xScale = scale(role, plate, "width");
                    body.yScale = scale(role, plate, "torso_length");
                }
                case LEGS -> {
                    float w = scale(role, plate, "width");
                    for (ModelPart leg : new ModelPart[]{rightLeg, leftLeg}) {
                        leg.xScale = leg.zScale = w;
                        leg.yScale = scale(role, plate, "length");
                    }
                }
                case FEET -> {
                    if (!band(role, plate, "shaft").equals("ankle")) {
                        List<String> shown = new ArrayList<>(on);
                        shown.addAll(List.of("cuff_r", "cuff_l"));
                        on = shown;
                    }
                }
                default -> {
                }
            }
        }

        private static float axisValue(ModularType.Role role, ModularDesign.Part plate, String name) {
            ModularType.Axis a = role.axes().get(name);
            return a == null ? 0f : plate.measures().axes().getOrDefault(name, a.def());
        }

        /** 1 at the axis's default, a little over or under at its ends. */
        private static float scale(ModularType.Role role, ModularDesign.Part plate, String name) {
            ModularType.Axis a = role.axes().get(name);
            return a == null || a.def() <= 0f ? 1f : Mth.clamp(1f + 0.5f * (axisValue(role, plate, name) - a.def()) / a.def(), 0.85f, 1.25f);
        }

        private static String band(ModularType.Role role, ModularDesign.Part plate, String name) {
            ModularType.Axis a = role.axes().get(name);
            ModularType.Band b = a == null ? null : a.band(axisValue(role, plate, name));
            return b == null ? "" : b.name();
        }

        private static final String[] STUDS = {"studs_head", "studs_body", "studs_body2"};

        /**
         * The first pass draws vanilla's cubes, then the extras with the gun sheet. The trim and glint passes that follow
         * draw the extras as vanilla cubes in the armour layer's layout, which the trim sprite and the glint cover too.
         */
        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, int color) {
            boolean first = calls++ == 0;
            Map<String, Boolean> saved = new HashMap<>();
            for (var e : extra.entrySet()) {
                saved.put(e.getKey(), e.getValue().visible);
                e.getValue().visible = !first && !e.getKey().startsWith("studs") && on.contains(e.getKey());
            }
            super.renderToBuffer(pose, buffer, light, overlay, color);
            for (var e : saved.entrySet())
                extra.get(e.getKey()).visible = e.getValue();
            if (!first)
                return;
            var source = Minecraft.getInstance().renderBuffers().bufferSource();
            for (var e : extra.entrySet()) {
                String name = e.getKey();
                boolean stud = name.startsWith("studs");
                if (stud ? studs == -1 || (name.equals("studs_head") != (currentSlot == EquipmentSlot.HEAD)) : !on.contains(name))
                    continue;
                ModelPart h = host.get(name);
                if (!h.visible)
                    continue;
                pose.pushPose();
                h.translateAndRotate(pose);
                float[] b = boxes.get(name);
                PlateDraw draw = new PlateDraw(pose, source, light, overlay).material(stud ? studMaterial : plateMaterial, stud ? studs : plateColour);
                draw.box(b[0], b[1], b[2], b[3], b[4], b[5]);
                draw.end();
                pose.popPose();
            }
            PlateDraw.flush(source);
        }

        EquipmentSlot currentSlot = EquipmentSlot.CHEST;
    }

    private static Gear outer, inner;

    /** Lets a modular armour piece draw as vanilla armour plus its variant's extras. */
    public static final class Extension implements IClientItemExtensions {
        /** The plate's colour: vanilla only reads the dye component off items tagged dyeable, which a modular piece is not. */
        @Override
        public int getDefaultDyeColor(ItemStack stack) {
            var dyed = stack.get(DataComponents.DYED_COLOR);
            return dyed == null ? 0xFFFFFFFF : 0xFF000000 | dyed.rgb();
        }

        @Override
        public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
            boolean legs = slot == EquipmentSlot.LEGS;
            var models = Minecraft.getInstance().getEntityModels();
            if (legs ? inner == null : outer == null) {
                Gear g = new Gear(models.bakeLayer(legs ? INNER : OUTER), legs);
                if (legs)
                    inner = g;
                else
                    outer = g;
            }
            Gear gear = legs ? inner : outer;
            gear.currentSlot = slot;
            gear.set(stack, slot);
            return gear;
        }
    }
}
