// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.ipn;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.JsonObject;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.compat.MenuRoles;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLPaths;

/**
 * Inventory Profiles Next (AGPL-3.0) soft compat, by a generated hint file. IPN reads every {@code *.json} in
 * {@code config/inventoryprofilesnext/integrationHints/} once at the first world join; an entry keyed by a menu's binary class name with
 * {@code "ignore": true} makes IPN treat that menu as non-storage (no sort, no move-all, no scroll-move). This writes {@code feedback.json}
 * there from {@link MenuRoles} (every role menu), rewriting only when the content changed, and only when IPN is installed.
 *
 * <p>Taken from IPN, as a reader of its source (design only, nothing copied): the folder, the flat class-name-to-record JSON shape and the
 * {@code ignore} field. Rejected: its annotations (a compile dependency on its jar) and its per-mod hint table.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class IpnCompat {

    private IpnCompat() {
    }

    @SubscribeEvent
    public static void onSetup(FMLCommonSetupEvent event) {
        if (ModList.get().isLoaded("inventoryprofilesnext"))
            event.enqueueWork(IpnCompat::write);
    }

    private static void write() {
        JsonObject hints = new JsonObject();
        for (String menu : MenuRoles.roleClasses()) {
            JsonObject hint = new JsonObject();
            hint.addProperty("ignore", true);
            hints.add(menu, hint);
        }
        String text = new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(hints) + "\n";
        Path file = FMLPaths.CONFIGDIR.get().resolve("inventoryprofilesnext").resolve("integrationHints").resolve("feedback.json");
        try {
            if (Files.exists(file) && Files.readString(file, StandardCharsets.UTF_8).equals(text))
                return;
            Files.createDirectories(file.getParent());
            Files.writeString(file, text, StandardCharsets.UTF_8);
        } catch (IOException e) {
            Feedback.LOGGER.warn("Could not write the Inventory Profiles Next hint file {}", file, e);
        }
    }
}
