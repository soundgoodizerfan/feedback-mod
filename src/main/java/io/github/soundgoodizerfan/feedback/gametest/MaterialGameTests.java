// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.gametest;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Form types generate by kind, and every listed or generated item finds its way back to its material. */
@GameTestHolder(Feedback.MOD_ID)
@PrefixGameTestTemplate(false)
public class MaterialGameTests {

    @GameTest(template = "empty")
    public static void formsGenerateByKindAndItemsFindTheirMaterial(GameTestHelper helper) {
        expect(helper, Items.IRON_HORSE_ARMOR, "wrought_iron", "horse_armor");
        expect(helper, item("feedback:steel_block"), "steel", "block");
        expect(helper, item("feedback:wrought_iron_plate"), "wrought_iron", "plate");
        expect(helper, Items.WHITE_WOOL, "wool", "white");
        helper.assertTrue(!BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse("feedback:stone_plate")),
                "a mineral gets no metal forms");
        helper.assertTrue(!BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse("feedback:cast_iron_wire")),
                "cast iron excludes drawn forms");
        helper.succeed();
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
    }

    private static void expect(GameTestHelper helper, Item item, String material, String form) {
        var found = Material.find(MaterialTable.get().materials(), item);
        helper.assertTrue(found.isPresent() && found.get().id().equals(Feedback.id(material))
                        && found.get().form().equals(form),
                item + " should be " + material + "/" + form + ", found " + found.map(f -> f.id() + "/" + f.form()).orElse("nothing"));
    }
}
