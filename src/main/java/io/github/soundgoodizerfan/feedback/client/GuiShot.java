// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * A screen to a PNG, with no one at the keyboard. When the environment variable {@code FEEDBACK_GUISHOT} is set the dev client runs this
 * script instead of waiting at the title screen: it makes a throwaway creative world, opens the screen, applies the options, saves a PNG of
 * the frame and a text report (every string drawn and every overlap, see {@link GuiCheck}) and quits. {@code scripts/gui_shot.sh} sets the
 * variable and runs the client; an agent runs that script.
 *
 * <p>The variable is {@code key=value} pairs joined by {@code ;}: {@code block} (a block id whose use opens the screen, placed two blocks in
 * front of the player), {@code commands} (server commands run first, joined by {@code |}: {@code give @s feedback:iron_plate 8}), {@code buttons}
 * (menu button ids clicked in order, comma separated: a tab), {@code clicks} (GUI-pixel clicks from the window's top-left, {@code x:y} comma
 * separated, left button), {@code scale} (GUI scales to capture at, comma separated, default 2, 0 is auto; one PNG each, {@code <out>_s<scale>.png}), {@code size} (window size, default 1920x1080: a GUI scale needs a window that holds 320x240 GUI pixels at it), {@code wait} (ticks to let the screen settle, default 70) and {@code out} (path without extension).
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
public final class GuiShot {

    private static final Map<String, String> CONFIG = parse(System.getenv("FEEDBACK_GUISHOT"));
    private static final int TIMEOUT_TICKS = 20 * 240;

    private enum Phase { TITLE, WORLD, OPENING, SETTLE, ACTIONS, RESCALE, CAPTURE, DONE }

    private static Phase phase = Phase.TITLE;
    private static int ticks, mark;
    private static int nextAction, scaleIndex;
    private static List<Runnable> actions = new ArrayList<>();

    private GuiShot() {
    }

    private static Map<String, String> parse(String raw) {
        Map<String, String> out = new LinkedHashMap<>();
        if (raw == null || raw.isBlank())
            return out;
        for (String pair : raw.split(";")) {
            int eq = pair.indexOf('=');
            if (eq > 0)
                out.put(pair.substring(0, eq).trim(), pair.substring(eq + 1).trim());
        }
        return out;
    }

    private static String out() {
        return CONFIG.getOrDefault("out", "build/guishot/shot");
    }

    private static void finish(String why) {
        if (phase == Phase.DONE)
            return;
        phase = Phase.DONE;
        try {
            File status = new File(out() + ".status");
            status.getParentFile().mkdirs();
            Files.writeString(status.toPath(), why + "\n", StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
        Minecraft.getInstance().stop();
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (CONFIG.isEmpty() || phase == Phase.DONE)
            return;
        Minecraft mc = Minecraft.getInstance();
        ticks++;
        if (ticks > TIMEOUT_TICKS) {
            finish("timeout in " + phase);
            return;
        }
        switch (phase) {
            case TITLE -> {
                if (mc.screen instanceof TitleScreen && ticks > 40) {
                    LevelSettings settings = new LevelSettings("guishot", GameType.CREATIVE, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel("guishot_" + System.currentTimeMillis(), settings, new WorldOptions(4242L, false, false),
                            WorldPresets::createNormalWorldDimensions, mc.screen);
                    phase = Phase.WORLD;
                    mark = ticks;
                }
            }
            case WORLD -> {
                if (ticks == mark + 1) {
                    // GUI scale is capped by the window (the game wants 320x240 GUI pixels), so the window is the player's size, not the default.
                    String[] wh = CONFIG.getOrDefault("size", "1920x1080").split("x");
                    org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), Integer.parseInt(wh[0]), Integer.parseInt(wh[1]));
                }
                if (mc.player != null && mc.level != null && mc.screen == null && ticks - mark > 120) {
                    openScreen(mc);
                    phase = Phase.OPENING;
                    mark = ticks;
                }
            }
            case OPENING -> {
                if (mc.screen != null) {
                    phase = Phase.SETTLE;
                    mark = ticks;
                    buildActions(mc);
                } else if (ticks - mark > 200)
                    finish("the block did not open a screen");
            }
            case SETTLE -> {
                if (ticks - mark >= Integer.parseInt(CONFIG.getOrDefault("wait", "70"))) {
                    phase = Phase.ACTIONS;
                    mark = ticks;
                }
            }
            case ACTIONS -> {
                if (nextAction >= actions.size()) {
                    if (ticks - mark >= 30) {
                        phase = Phase.RESCALE;
                        mark = ticks;
                    }
                } else if (ticks - mark >= 8) {
                    actions.get(nextAction++).run();
                    mark = ticks;
                }
            }
            case RESCALE -> {
                // The GUI scale is the player's setting (0 is auto); the screen is laid out again for it, as on a real change.
                mc.options.guiScale().set(scales().get(scaleIndex));
                mc.resizeDisplay();
                phase = Phase.CAPTURE;
                mark = ticks;
            }
            case CAPTURE -> {
                if (ticks - mark >= 25)
                    capture(mc);
            }
            default -> {
            }
        }
    }

    /** Server side: run the commands, put the block in front of the player and use it, so the real menu opens and syncs. */
    private static void openScreen(Minecraft mc) {
        var server = mc.getSingleplayerServer();
        String[] commands = CONFIG.getOrDefault("commands", "").isEmpty() ? new String[0] : CONFIG.get("commands").split("\\|");
        ResourceLocation id = ResourceLocation.parse(CONFIG.getOrDefault("block", "minecraft:air"));
        server.execute(() -> {
            ServerPlayer sp = server.getPlayerList().getPlayers().get(0);
            for (String c : commands)
                server.getCommands().performPrefixedCommand(sp.createCommandSourceStack().withPermission(4), c.trim());
            var block = BuiltInRegistries.BLOCK.get(id);
            var pos = sp.blockPosition().relative(sp.getDirection(), 2);
            var level = sp.serverLevel();
            level.setBlock(pos, block.defaultBlockState(), 3);
            level.getBlockState(pos).useWithoutItem(level, sp, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
        });
    }

    private static void buildActions(Minecraft mc) {
        actions = new ArrayList<>();
        String buttons = CONFIG.getOrDefault("buttons", "");
        if (!buttons.isEmpty())
            for (String b : buttons.split(","))
                actions.add(() -> {
                    if (mc.screen instanceof AbstractContainerScreen<?> s)
                        mc.gameMode.handleInventoryButtonClick(s.getMenu().containerId, Integer.parseInt(b.trim()));
                });
        String clicks = CONFIG.getOrDefault("clicks", "");
        if (!clicks.isEmpty())
            for (String c : clicks.split(",")) {
                String[] xy = c.trim().split(":");
                double x = Double.parseDouble(xy[0]), y = Double.parseDouble(xy[1]);
                actions.add(() -> {
                    if (mc.screen != null) {
                        mc.screen.mouseClicked(x, y, 0);
                        mc.screen.mouseReleased(x, y, 0);
                    }
                });
            }
    }

    /** The GUI scales to capture at, from {@code scale=2,3}; 2 when none is given, 0 is auto. */
    private static List<Integer> scales() {
        List<Integer> out = new ArrayList<>();
        for (String v : CONFIG.getOrDefault("scale", "2").split(","))
            out.add(Integer.parseInt(v.trim()));
        return out;
    }

    private static void capture(Minecraft mc) {
        try {
            int scale = scales().get(scaleIndex);
            String stem = out() + "_s" + scale;
            File png = new File(stem + ".png"), txt = new File(stem + ".txt");
            png.getParentFile().mkdirs();
            var image = Screenshot.takeScreenshot(mc.getMainRenderTarget());
            image.writeToFile(png);
            image.close();
            List<String> lines = new ArrayList<>(mc.screen == null ? List.of("no screen open") : GuiCheck.report(mc, mc.screen, -1, -1));
            lines.add(0, "gui scale " + scale + " (" + mc.getWindow().getGuiScaledWidth() + "x" + mc.getWindow().getGuiScaledHeight() + " GUI pixels)");
            lines.forEach(l -> Feedback.LOGGER.info("gui shot: {}", l));
            Files.write(txt.toPath(), lines, StandardCharsets.UTF_8);
            if (++scaleIndex < scales().size()) {
                phase = Phase.RESCALE;
                mark = ticks;
            } else
                finish("ok");
        } catch (Exception e) {
            finish("capture failed: " + e);
        }
    }
}
