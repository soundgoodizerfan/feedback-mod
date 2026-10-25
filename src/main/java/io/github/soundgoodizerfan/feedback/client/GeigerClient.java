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
 * Assets under src/main/resources/assets are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.client;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.hazard.Radiation;
import io.github.soundgoodizerfan.feedback.instrument.Instruments;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;
import io.github.soundgoodizerfan.feedback.item.GeigerCounterItem;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.registry.FSounds;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * The Geiger counter's voice (mechanics 9.8). While one is held, each tick draws a Poisson number
 * of clicks at {@code background + Ru x clicks-per-Ru} a second; the true rate is worked out five
 * times a second by the same {@link Radiation#rate} the server doses with. The figure it prints is
 * not the true rate but what the clicks of the last few seconds say it is -- so it jitters the way a
 * real counter does, and a weak source reads as nothing some of the time. That is the noise floor
 * (digest 8): a better counter would count more of them, never all of them.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
public final class GeigerClient {

    private static final int WINDOW_TICKS = 100;
    private static final int REFRESH_TICKS = 4;

    private static float trueRate;
    /** Clicks heard in each of the last {@link #WINDOW_TICKS} ticks, as a ring. */
    private static final byte[] HEARD = new byte[WINDOW_TICKS];
    private static int head;
    private static long lastTick = Long.MIN_VALUE;

    private GeigerClient() {
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.isPaused())
            return;
        boolean held = player.getItemInHand(InteractionHand.MAIN_HAND).getItem() instanceof GeigerCounterItem
                || player.getItemInHand(InteractionHand.OFF_HAND).getItem() instanceof GeigerCounterItem;
        long now = mc.level.getGameTime();
        if (!held || Instruments.best(player, Quantity.RADIATION) == null) {
            java.util.Arrays.fill(HEARD, (byte) 0);
            lastTick = Long.MIN_VALUE;
            return;
        }
        if (now == lastTick)
            return;
        lastTick = now;
        if (now % REFRESH_TICKS == 0)
            trueRate = Radiation.rate(player, ClientMaterials.get());
        float expected = (FTuning.GEIGER_BACKGROUND_CPS + FTuning.GEIGER_CPS_PER_RU * trueRate) / 20f;
        int clicks = poisson(Math.min(expected, 20f), mc.level.random);
        head = (head + 1) % WINDOW_TICKS;
        HEARD[head] = (byte) Math.min(clicks, 100);
        for (int i = 0; i < Math.min(clicks, 4); i++)
            mc.level.playLocalSound(player.getX(), player.getY(), player.getZ(), FSounds.GEIGER_CLICK.get(),
                    SoundSource.PLAYERS, 0.35f, 0.9f + mc.level.random.nextFloat() * 0.3f, false);
    }

    /** Knuth's method; the mean is capped small enough that it is cheap. */
    private static int poisson(float mean, net.minecraft.util.RandomSource random) {
        double limit = Math.exp(-mean), product = 1.0;
        int k = 0;
        do {
            k++;
            product *= random.nextDouble();
        } while (product > limit);
        return k - 1;
    }

    /** What the clicks of the last few seconds say the rate is, in Ru; never below nothing. */
    public static float reading() {
        int total = 0;
        for (byte b : HEARD)
            total += b;
        float perSecond = total / (WINDOW_TICKS / 20f);
        return Math.max(0f, (perSecond - FTuning.GEIGER_BACKGROUND_CPS) / FTuning.GEIGER_CPS_PER_RU);
    }
}
