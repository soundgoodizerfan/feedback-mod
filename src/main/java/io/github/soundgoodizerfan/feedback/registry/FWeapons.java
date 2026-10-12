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
 * Assets under src/main/resources are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.registry;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.weapon.GunDesign;
import io.github.soundgoodizerfan.feedback.weapon.GunFigures;
import io.github.soundgoodizerfan.feedback.weapon.GunItem;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Weapons: every gun registration in one place ({@code feedback_guns_spec.md}). */
public class FWeapons {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Feedback.MOD_ID);
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, Feedback.MOD_ID);

    /** What a gun was designed as -- the settings, and nothing derived. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GunDesign>> GUN_DESIGN =
            COMPONENTS.register("gun_design", () -> DataComponentType.<GunDesign>builder()
                    .persistent(GunDesign.CODEC)
                    .networkSynchronized(GunDesign.STREAM_CODEC)
                    .build());

    /** What that design makes, baked once (spec section 2). The client predicts shots from this. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GunFigures>> GUN_FIGURES =
            COMPONENTS.register("gun_figures", () -> DataComponentType.<GunFigures>builder()
                    .persistent(GunFigures.CODEC)
                    .networkSynchronized(GunFigures.STREAM_CODEC)
                    .build());

    /**
     * Spec step 1's one hand-authored gun: a 9 mm, medium barrel, standard wall, in iron.
     * Creative-tab only; there is no way to make one yet and no ammunition to spend (step 3).
     */
    public static final DeferredItem<Item> TEST_PISTOL = ITEMS.register("test_pistol",
            () -> new GunItem(new Item.Properties()
                    .component(GUN_DESIGN.get(), GunDesign.starter(Feedback.id("iron")))));

    private FWeapons() {
    }

    public static void register(IEventBus modBus) {
        COMPONENTS.register(modBus);
        ITEMS.register(modBus);
    }
}
