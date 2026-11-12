// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
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
 * Screens to PNGs with no one at the keyboard. When the environment variable {@code FEEDBACK_GUISHOT} is set the dev client is a screenshot
 * server: it loads (or, the first time, makes) a creative world called {@code guishot_reuse}, then waits for request files and answers each
 * one, so only the first request pays for the game starting. {@code scripts/gui_shot.sh} starts it, drops requests and prints the answers; an
 * agent runs that script.
 *
 * <p>The variable is {@code key=value} pairs joined by {@code ;}: {@code dir} (the folder requests arrive in, required), {@code size} (window
 * size, default 1920x1080: the game caps the GUI scale so 320x240 GUI pixels fit) and {@code idle} (seconds without a request before the client
 * quits, default 900).
 *
 * <p>A request is a file {@code <dir>/<id>.req} (written as {@code .tmp} and renamed): one shot per line, blank lines and {@code #} lines
 * ignored, a line is {@code <block id> [options]} with options {@code key=value} joined by {@code ;}: {@code out} (path without extension, required),
 * {@code commands} (server commands run first, joined by {@code |}), {@code buttons} (menu button ids clicked in order), {@code clicks} ({@code x:y}
 * GUI pixels, left button), {@code tabs} ({@code auto} reads the menu's {@code BTN_TAB} and {@code TABS}; or {@code N} or {@code N:base}: one
 * picture per tab, clicking each tab's button in turn, files {@code <out>_tab<i>}), {@code scale} (GUI scales to capture at, comma separated, default 2;
 * 0 is auto; files {@code ..._s<scale>.png}) and {@code wait} (ticks to let the screen settle, default 40). The answer is {@code <id>.done}: the pictures
 * written and each picture's overlap lines. A request named {@code quit.req} stops the client.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
public final class GuiShot {

    private static final Map<String, String> CONFIG = parse(System.getenv("FEEDBACK_GUISHOT"), ";");
    private static final String WORLD = "guishot_reuse";
    private static final int SHOT_TIMEOUT = 20 * 60;

    private enum Phase { TITLE, WORLD, IDLE, OPEN, SETTLE, ACTIONS, VARIANT, VARWAIT, RESCALE, CAPTURE, CLOSE, DEAD }

    /** One picture to take: which tab (a menu button to click first, or -1) and the file suffix it gets. */
    private record Variant(String suffix, int button, boolean scroll) {
        Variant(String suffix, int button) {
            this(suffix, button, false);
        }
    }

    private static Phase phase = Phase.TITLE;
    private static int ticks, mark, shotStart, activity;
    private static boolean opened, readyWritten;
    private static int nextAction, variantIndex, scaleIndex, originalScale = -1, answerMark, pagesWithOverlaps;
    private static boolean clientTried;
    private static final java.util.Set<String> SEEN = new java.util.HashSet<>();
    private static final Deque<Map<String, String>> QUEUE = new ArrayDeque<>();
    private static Map<String, String> shot;
    private static File request;
    private static List<String> answer = new ArrayList<>();
    private static List<Runnable> actions = new ArrayList<>();
    private static List<Variant> variants = new ArrayList<>();

    private GuiShot() {
    }

    static {
        // The game's own thread can hang in a loading loop where no tick runs, so a plain thread says where it is, for whoever reads the folder.
        if (CONFIG.containsKey("dir")) {
            Thread t = new Thread(() -> {
                while (true) {
                    try {
                        Minecraft mc = Minecraft.getInstance();
                        write(new File(dir(), "state"), List.of("phase=" + phase + " ticks=" + ticks + " screen=" + (mc.screen == null ? "none" : mc.screen.getClass().getName())
                                + " overlay=" + (mc.getOverlay() == null ? "none" : mc.getOverlay().getClass().getSimpleName())
                                + " player=" + (mc.player != null) + " level=" + (mc.level != null)));
                        Thread.sleep(3000);
                    } catch (Exception e) {
                        return;
                    }
                }
            }, "feedback-guishot-state");
            t.setDaemon(true);
            t.start();
        }
    }

    private static Map<String, String> parse(String raw, String separator) {
        Map<String, String> out = new LinkedHashMap<>();
        if (raw == null || raw.isBlank())
            return out;
        for (String pair : raw.split(separator)) {
            int eq = pair.indexOf('=');
            if (eq > 0)
                out.put(pair.substring(0, eq).trim(), pair.substring(eq + 1).trim());
        }
        return out;
    }

    private static File dir() {
        return new File(CONFIG.get("dir"));
    }

    private static void write(File file, List<String> lines) {
        try {
            file.getParentFile().mkdirs();
            Files.write(file.toPath(), lines, StandardCharsets.UTF_8);
        } catch (Exception e) {
            Feedback.LOGGER.error("gui shot: cannot write {}", file, e);
        }
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!CONFIG.containsKey("dir") || phase == Phase.DEAD)
            return;
        Minecraft mc = Minecraft.getInstance();
        ticks++;
        switch (phase) {
            case TITLE -> {
                // An unfocused window would open the pause menu (and pause the world) whenever it is behind another window.
                mc.options.pauseOnLostFocus = false;
                // A window behind others is throttled by the compositor; a swap that waits on vsync then waits on the compositor.
                if (ticks == 1) {
                    mc.options.enableVsync().set(false);
                    mc.options.framerateLimit().set(260);
                }
                if (mc.screen instanceof TitleScreen && mc.getOverlay() == null && ticks > 40) {
                    if (mc.getLevelSource().levelExists(WORLD))
                        mc.createWorldOpenFlows().openWorld(WORLD, () -> write(new File(dir(), "ready"), List.of("could not open the world")));
                    else {
                        LevelSettings settings = new LevelSettings("guishot", GameType.CREATIVE, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
                        mc.createWorldOpenFlows().createFreshLevel(WORLD, settings, new WorldOptions(4242L, false, false), WorldPresets::createNormalWorldDimensions, mc.screen);
                    }
                    phase = Phase.WORLD;
                    mark = ticks;
                }
            }
            case WORLD -> {
                if (ticks == mark + 1) {
                    String[] wh = CONFIG.getOrDefault("size", "1920x1080").split("x");
                    org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), Integer.parseInt(wh[0]), Integer.parseInt(wh[1]));
                }
                if (mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen)
                    mc.setScreen(null);
                if (mc.player != null && mc.level != null && mc.screen == null && ticks - mark > 60) {
                    var server = mc.getSingleplayerServer();
                    server.execute(() -> {
                        ServerPlayer sp = server.getPlayerList().getPlayers().get(0);
                        for (String c : List.of("time set noon", "gamerule doDaylightCycle false", "gamerule doWeatherCycle false", "weather clear"))
                            server.getCommands().performPrefixedCommand(sp.createCommandSourceStack().withPermission(4), c);
                    });
                    phase = Phase.IDLE;
                    activity = ticks;
                }
            }
            case IDLE -> {
                if (mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen)
                    mc.setScreen(null);
                idle(mc);
            }
            case OPEN -> open(mc);
            case SETTLE -> {
                if (ticks - mark >= Integer.parseInt(shot.getOrDefault("wait", "40"))) {
                    buildActions(mc);
                    phase = Phase.ACTIONS;
                    mark = ticks;
                }
            }
            case ACTIONS -> {
                if (nextAction >= actions.size()) {
                    if (ticks - mark >= 8) {
                        buildVariants(mc);
                        variantIndex = 0;
                        phase = Phase.VARIANT;
                    }
                } else if (ticks - mark >= 5) {
                    actions.get(nextAction++).run();
                    mark = ticks;
                }
            }
            case VARIANT -> {
                Variant v = variants.get(variantIndex);
                if (v.button() >= 0 && mc.screen instanceof AbstractContainerScreen<?> s)
                    mc.gameMode.handleInventoryButtonClick(s.getMenu().containerId, v.button());
                if (v.scroll() && mc.screen != null)
                    mc.screen.mouseScrolled(mc.getWindow().getGuiScaledWidth() / 2.0, mc.getWindow().getGuiScaledHeight() / 2.0, 0, -1);
                mark = ticks;
                phase = Phase.VARWAIT;
            }
            case VARWAIT -> {
                if (ticks - mark >= 12) {
                    scaleIndex = 0;
                    phase = Phase.RESCALE;
                }
            }
            case RESCALE -> {
                // The GUI scale is the player's setting (0 is auto); the screen is laid out again for it, as on a real change.
                if (originalScale < 0)
                    originalScale = mc.options.guiScale().get();
                mc.options.guiScale().set(scales().get(scaleIndex));
                mc.resizeDisplay();
                mark = ticks;
                phase = Phase.CAPTURE;
            }
            case CAPTURE -> {
                if (ticks - mark >= 14)
                    capture(mc);
            }
            case CLOSE -> {
                if (mc.screen != null) {
                    mc.player.closeContainer();
                    mc.setScreen(null);
                }
                if (ticks - mark >= 6)
                    next(mc);
            }
            default -> {
            }
        }
        if (phase.ordinal() >= Phase.OPEN.ordinal() && phase != Phase.CLOSE && phase != Phase.DEAD && ticks - shotStart > SHOT_TIMEOUT)
            fail(mc, "timed out in " + phase);
    }

    // --- requests -------------------------------------------------------------------------------

    private static void idle(Minecraft mc) {
        if (!readyWritten) {
            readyWritten = true;
            write(new File(dir(), "ready"), List.of("ready"));
        }
        if (ticks % 5 != 0)
            return;
        File[] found = dir().listFiles((d, n) -> n.endsWith(".req"));
        if (found != null && found.length > 0) {
            Arrays.sort(found);
            request = found[0];
            activity = ticks;
            if (request.getName().equals("quit.req")) {
                request.delete();
                phase = Phase.DEAD;
                mc.stop();
                return;
            }
            answer = new ArrayList<>();
            try {
                for (String line : Files.readAllLines(request.toPath())) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#"))
                        continue;
                    int sp = line.indexOf(' ');
                    Map<String, String> s = parse(sp < 0 ? "" : line.substring(sp + 1), ";");
                    s.put("block", sp < 0 ? line : line.substring(0, sp));
                    QUEUE.add(s);
                }
            } catch (Exception e) {
                answer.add("FAILED: cannot read the request: " + e);
            }
            next(mc);
        } else if (ticks - activity > 20 * Integer.parseInt(CONFIG.getOrDefault("idle", "900"))) {
            phase = Phase.DEAD;
            mc.stop();
        }
    }

    /** The next shot of the request, or the answer when there are no more. */
    private static void next(Minecraft mc) {
        shot = QUEUE.poll();
        if (shot != null && shot.containsKey("jei") && shot.get("jei").endsWith("*")) {
            // jei=feedback:* (or just *): one shot per recipe category of that namespace, in the order JEI lists them.
            String ns = shot.get("jei").replace("*", "").replace(":", "");
            Map<String, String> template = shot;
            List<Map<String, String>> expanded = new ArrayList<>();
            for (String uid : io.github.soundgoodizerfan.feedback.compat.jei.JeiShots.categories(ns)) {
                Map<String, String> one = new LinkedHashMap<>(template);
                one.put("jei", uid);
                one.put("out", template.get("out") + "_" + uid.replace(':', '_'));
                expanded.add(one);
            }
            if (expanded.isEmpty())
                answer.add("FAILED " + shot.get("jei") + ": JEI has no such categories (is it up?)");
            for (int i = expanded.size() - 1; i >= 0; i--)
                QUEUE.addFirst(expanded.get(i));
            shot = QUEUE.poll();
        }
        if (shot == null) {
            if (originalScale >= 0) {
                mc.options.guiScale().set(originalScale);
                mc.resizeDisplay();
                originalScale = -1;
            }
            if (request != null) {
                write(new File(request.getPath().replaceAll("\\.req$", ".done")), answer);
                request.delete();
                request = null;
            }
            phase = Phase.IDLE;
            activity = ticks;
            return;
        }
        opened = false;
        clientTried = false;
        SEEN.clear();
        pagesWithOverlaps = 0;
        shotStart = ticks;
        phase = Phase.OPEN;
        mark = ticks;
    }

    private static void fail(Minecraft mc, String why) {
        answer.add("FAILED " + shot.get("block") + " " + shot.getOrDefault("out", "") + ": " + why);
        phase = Phase.CLOSE;
        mark = ticks;
    }

    /** Server side: run the commands, put the block in front of the player and use it, so the real menu opens and syncs. */
    private static void open(Minecraft mc) {
        if (!opened && shot.containsKey("jei")) {
            opened = true;
            if (!io.github.soundgoodizerfan.feedback.compat.jei.JeiShots.open(shot.get("jei")))
                fail(mc, "JEI has no category " + shot.get("jei"));
            return;
        }
        if (!opened) {
            opened = true;
            var server = mc.getSingleplayerServer();
            String[] commands = shot.getOrDefault("commands", "").isEmpty() ? new String[0] : shot.get("commands").split("\\|");
            ResourceLocation id = ResourceLocation.tryParse(shot.get("block"));
            server.execute(() -> {
                ServerPlayer sp = server.getPlayerList().getPlayers().get(0);
                for (String c : commands)
                    server.getCommands().performPrefixedCommand(sp.createCommandSourceStack().withPermission(4), c.trim());
                var block = id == null ? null : BuiltInRegistries.BLOCK.getOptional(id).orElse(null);
                if (block == null)
                    return;
                var pos = sp.blockPosition().relative(sp.getDirection(), 2);
                var level = sp.serverLevel();
                level.setBlock(pos, block.defaultBlockState(), 3);
                level.getBlockState(pos).useWithoutItem(level, sp, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
            });
            return;
        }
        if (mc.screen != null && ticks - mark > 3) {
            phase = Phase.SETTLE;
            mark = ticks;
        } else if (!clientTried && mc.screen == null && !shot.containsKey("jei") && ticks - mark > 30) {
            // Some blocks open their screen from the client side of the use; a real click does what a player does, on both sides.
            clientTried = true;
            var pos = mc.player.blockPosition().relative(mc.player.getDirection(), 2);
            mc.gameMode.useItemOn(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
        } else if (ticks - mark > 20 * 8)
            fail(mc, shot.containsKey("jei") ? "the recipes screen did not open" : "the block did not open a screen");
    }

    private static void buildActions(Minecraft mc) {
        actions = new ArrayList<>();
        nextAction = 0;
        String buttons = shot.getOrDefault("buttons", "");
        if (!buttons.isEmpty())
            for (String b : buttons.split(","))
                actions.add(() -> {
                    if (mc.screen instanceof AbstractContainerScreen<?> s)
                        mc.gameMode.handleInventoryButtonClick(s.getMenu().containerId, Integer.parseInt(b.trim()));
                });
        String clicks = shot.getOrDefault("clicks", "");
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

    /** One picture, or one per tab: {@code tabs=auto} asks the open menu for its {@code BTN_TAB} and {@code TABS}; {@code N} or {@code N:base} says them. */
    private static void buildVariants(Minecraft mc) {
        variants = new ArrayList<>();
        if (shot.containsKey("jei")) {
            // One picture per page of the category's recipes, scrolling the recipe area; the end is when a page comes round again.
            String pages = shot.getOrDefault("pages", "6");
            int n = pages.equals("all") ? 400 : Integer.parseInt(pages);
            for (int i = 0; i < n; i++)
                variants.add(new Variant("_p" + i, -1, i > 0));
            return;
        }
        String tabs = shot.getOrDefault("tabs", "");
        int count = 0, base = 0;
        if (tabs.equals("auto") && mc.screen instanceof AbstractContainerScreen<?> s) {
            try {
                Class<?> menu = s.getMenu().getClass();
                count = menu.getField("TABS").getInt(null);
                base = menu.getField("BTN_TAB").getInt(null);
            } catch (ReflectiveOperationException e) {
                answer.add("no tabs: " + s.getMenu().getClass().getSimpleName() + " has no public TABS and BTN_TAB");
            }
        } else if (!tabs.isEmpty() && !tabs.equals("auto")) {
            String[] nb = tabs.split(":");
            count = Integer.parseInt(nb[0]);
            base = nb.length > 1 ? Integer.parseInt(nb[1]) : 0;
        }
        if (count <= 1)
            variants.add(new Variant("", -1));
        else
            for (int i = 0; i < count; i++)
                variants.add(new Variant("_tab" + i, base + i));
    }

    /** The GUI scales to capture at, from {@code scale=2,3}; 2 when none is given, 0 is auto. */
    private static List<Integer> scales() {
        List<Integer> out = new ArrayList<>();
        for (String v : shot.getOrDefault("scale", "2").split(","))
            out.add(Integer.parseInt(v.trim()));
        return out;
    }

    private static void capture(Minecraft mc) {
        try {
            int scale = scales().get(scaleIndex);
            String stem = shot.get("out") + variants.get(variantIndex).suffix() + "_s" + scale;
            File png = new File(stem + ".png"), txt = new File(stem + ".txt");
            png.getParentFile().mkdirs();
            List<String> lines = new ArrayList<>(mc.screen == null ? List.of("no screen open") : GuiCheck.report(mc, mc.screen, -1, -1));
            lines.add(0, "gui scale " + scale + " (" + mc.getWindow().getGuiScaledWidth() + "x" + mc.getWindow().getGuiScaledHeight() + " GUI pixels)");
            boolean jei = shot.containsKey("jei"), overlaps = lines.stream().anyMatch(l -> l.startsWith("overlap:"));
            // A recipes screen scrolled past its last page comes round to the first: the same strings again mean the category is done.
            String signature = lines.stream().filter(l -> l.startsWith("string:")).collect(java.util.stream.Collectors.joining("\n"));
            boolean repeat = jei && !SEEN.add(scale + signature);
            if (repeat) {
                variantIndex = variants.size();
                phase = Phase.CLOSE;
                mark = ticks;
                answer.add(shot.get("jei") + ": " + SEEN.size() / scales().size() + " pages, " + pagesWithOverlaps + " with overlaps");
                return;
            }
            if (overlaps)
                pagesWithOverlaps++;
            // png=overlaps keeps only the pictures that have an overlap in them, for a sweep over many pages.
            if (!"overlaps".equals(shot.get("png")) || overlaps) {
                var image = Screenshot.takeScreenshot(mc.getMainRenderTarget());
                image.writeToFile(png);
                image.close();
                Files.write(txt.toPath(), lines, StandardCharsets.UTF_8);
                answer.add("screenshot: " + png.getPath());
                lines.stream().filter(l -> !l.startsWith("string:")).forEach(answer::add);
            }
            if (++scaleIndex < scales().size()) {
                phase = Phase.RESCALE;
                return;
            }
            if (++variantIndex < variants.size()) {
                phase = Phase.VARIANT;
                return;
            }
            if (shot.containsKey("jei"))
                answer.add(shot.get("jei") + ": " + SEEN.size() / scales().size() + " pages, " + pagesWithOverlaps + " with overlaps");
            phase = Phase.CLOSE;
            mark = ticks;
        } catch (Exception e) {
            fail(mc, "capture failed: " + e);
        }
    }
}
