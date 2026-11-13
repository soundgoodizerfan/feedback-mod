// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.cable;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.machine.ElectricMachineTuning;
import io.github.soundgoodizerfan.feedback.core.hazard.Shock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A fence post of live wire: a cable you cannot walk through, bare by design.
 *
 * <h2>The cable family, not a new machine</h2>
 * Same block entity, same material on the stack, same electrical role (a wire), so a run of posts
 * conducts, drops and is read exactly as cable is. What differs is the shape, that it is never
 * insulated, and what touching it does: whatever's box overlaps the post is shocked as {@link
 * Shock} says for the line's voltage, with the victim's {@code Exposure} scaling the figure,
 * pushed back, and the run billed {@link ElectricMachineTuning#FENCE_HIT_AMPS} for the tick. Touch only; it is
 * not a field. A fence taking hits is correct use, so it wears nothing.
 *
 * <h2>Collision and touch are different boxes</h2>
 * The post is solid to walk into but the box that shocks is a little wider, so pressing against
 * it overlaps and that is the touch.
 */
public class FenceBlock extends CableBlock {

    private static final VoxelShape TOUCH = Block.box(4, 0, 4, 12, 16, 12);
    private static final VoxelShape SOLID = Block.box(6, 0, 6, 10, 24, 10);
    private static final String LAST_SHOCK = "feedback_fence_shock";

    public FenceBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return TOUCH;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SOLID;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide || !(level.getBlockEntity(pos) instanceof CableBlockEntity fence))
            return;
        if (!Shapes.joinIsNotEmpty(TOUCH.move(pos.getX(), pos.getY(), pos.getZ()),
                Shapes.create(entity.getBoundingBox()), BooleanOp.AND))
            return;
        long now = level.getGameTime();
        if (now - entity.getPersistentData().getLong(LAST_SHOCK) < ElectricMachineTuning.FENCE_SHOCK_COOLDOWN_TICKS)
            return;
        var band = fence.touchBand();
        if (band == null)
            return;
        entity.getPersistentData().putLong(LAST_SHOCK, now);
        if (!Shock.apply(entity, band))
            return;
        fence.draw(ElectricMachineTuning.FENCE_HIT_AMPS);
        if (entity instanceof LivingEntity living) {
            living.knockback(FTuning.FENCE_KNOCKBACK, pos.getX() + 0.5 - entity.getX(), pos.getZ() + 0.5 - entity.getZ());
            living.hurtMarked = true;   // a player's client is only told of a push if it is marked
        }
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK, entity.getX(), entity.getY() + entity.getBbHeight() / 2,
                    entity.getZ(), 6, 0.2, 0.3, 0.2, 0.1);
            server.playSound(null, pos, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.BLOCKS, 0.2f, 2f);
        }
    }
}
