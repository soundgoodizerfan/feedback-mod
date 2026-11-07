// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.logistics.belt;

import java.util.List;

import io.github.soundgoodizerfan.feedback.core.thermal.HeatSource;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.logistics.LogisticsTuning;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.hazard.HotContact;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * One cell of a belt run. The block holds only a facing and a slope; its band (what it is made
 * of), its module and its cargo are the block entity's, and the run it belongs to is
 * {@link BeltRuns}'. A belt is a modular build (the {@code conveyor_belt} type) that places as this
 * block; the head roller at the run's end drives it.
 *
 * <h2>Prior art</h2>
 * Create's belts (MIT) were looked at for what a player expects of a belt: a run of blocks,
 * an incline, items riding the top, entities carried. Taken: that idea of a player-facing
 * behaviour only. Rejected: its per-belt-block item list, its shaft through every block and its
 * item-stack-in-segment cargo model; here the run owns one ledger and one shared phase. No code
 * or asset was copied.
 */
public class BeltBlock extends Block implements EntityBlock, BeltPart {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<Slope> SLOPE = EnumProperty.create("slope", Slope.class);
    /** Set by the run while the band moves; picks the animated band texture over the still one. */
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty RUNNING = net.minecraft.world.level.block.state.properties.BooleanProperty.create("running");

    /** How the cell lies: level, rising ahead (cargo leaves one block higher) or falling. */
    public enum Slope implements StringRepresentable {
        FLAT, UP, DOWN;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }

        public Slope next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    private static final VoxelShape[][] SHAPES = new VoxelShape[4][3];

    static {
        // Boxes in the north frame, pixels: {x0,y0,z0,x1,y1,z1}. A rise climbs four steps, a fall drops four.
        double[][][] boxes = {
                {{0, 0, 0, 16, 4, 16}},
                {{0, 0, 12, 16, 8, 16}, {0, 0, 8, 16, 12, 12}, {0, 0, 4, 16, 16, 8}, {0, 0, 0, 16, 20, 4}},
                {{0, -4, 12, 16, 0, 16}, {0, -8, 8, 16, -4, 12}, {0, -12, 4, 16, -8, 8}, {0, -16, 0, 16, -12, 4}}};
        Direction[] order = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
        for (int f = 0; f < 4; f++)
            for (int s = 0; s < 3; s++) {
                VoxelShape shape = Shapes.empty();
                for (double[] b : boxes[s]) {
                    double[] x = {b[0], b[3]}, z = {b[2], b[5]};
                    double x0, x1, z0, z1;
                    switch (order[f]) {
                        case EAST -> { x0 = 16 - z[1]; x1 = 16 - z[0]; z0 = x[0]; z1 = x[1]; }
                        case SOUTH -> { x0 = 16 - x[1]; x1 = 16 - x[0]; z0 = 16 - z[1]; z1 = 16 - z[0]; }
                        case WEST -> { x0 = z[0]; x1 = z[1]; z0 = 16 - x[1]; z1 = 16 - x[0]; }
                        default -> { x0 = x[0]; x1 = x[1]; z0 = z[0]; z1 = z[1]; }
                    }
                    shape = Shapes.or(shape, Block.box(x0, b[1], z0, x1, b[4], z1));
                }
                SHAPES[f][s] = shape;
            }
    }

    public BeltBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SLOPE, Slope.FLAT).setValue(RUNNING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SLOPE, RUNNING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    public Direction beltFacing(BlockState state) {
        return state.getValue(FACING);
    }

    @Override
    public Slope beltSlope(BlockState state) {
        return state.getValue(SLOPE);
    }

    private static VoxelShape shapeOf(BlockState state) {
        int f = switch (state.getValue(FACING)) {
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        };
        return SHAPES[f][state.getValue(SLOPE).ordinal()];
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeOf(state);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeOf(state);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BeltBlockEntity(pos, state);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        super.onPlace(state, level, pos, old, moved);
        if (!level.isClientSide && !old.is(this))
            BeltRuns.of(level).queue(pos);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moved) {
        super.neighborChanged(state, level, pos, block, from, moved);
        if (!level.isClientSide && BeltRuns.adjacent(pos, from))
            BeltRuns.of(level).queue(pos);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        super.onRemove(state, level, pos, newState, moved);
        if (!level.isClientSide && !newState.is(this))
            BeltRuns.of(level).queue(pos);
    }

    // --- interaction ----------------------------------------------------------------------------

    /** A sneaking click with a belt in hand turns the cell level, up or down. */
    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        if (player.isShiftKeyDown() && stack.is(asItem())) {
            if (!level.isClientSide) {
                level.setBlockAndUpdate(pos, state.setValue(SLOPE, state.getValue(SLOPE).next()));
                BeltRuns.of(level).queue(pos);
            }
            return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide)
            return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof BeltBlockEntity be))
            return InteractionResult.PASS;
        return be.handUse(player, hit) ? InteractionResult.CONSUME : InteractionResult.PASS;
    }

    // --- riders, strays and heat ----------------------------------------------------------------

    /** Lets this cell carry what stands on it, and takes dropped items into the ledger. */
    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (entity instanceof ItemEntity item) {
            if (!level.isClientSide)
                BeltRuns.of(level).adopt(pos, item);
            return;
        }
        if (entity.isShiftKeyDown() && entity instanceof Player)
            return;
        // A player is moved by their own client; everything else by the server.
        if (level.isClientSide != (entity instanceof Player))
            return;
        float speed = level.isClientSide ? BeltRuns.clientSpeed.apply(pos) : BeltRuns.of(level).speedAt(pos);
        if (speed == 0f)
            return;
        Direction f = state.getValue(FACING);
        Vec3 push = new Vec3(f.getStepX(), 0, f.getStepZ()).scale(speed * LogisticsTuning.BELT_RIDE_GAIN);
        entity.setDeltaMovement(entity.getDeltaMovement().add(push));
        if (!level.isClientSide && entity instanceof LivingEntity living && level.getGameTime() % FTuning.CONTACT_INTERVAL_TICKS == 0) {
            float fire = HeatSource.at(level, pos).value();
            if (fire >= FTuning.CONTACT_PAIN_TU.value())
                HotContact.burn(living, new Tu(fire));
        }
    }

    // --- drops ----------------------------------------------------------------------------------

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        ItemStack stack = new ItemStack(asItem());
        BlockEntity be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        List<ItemStack> out = new java.util.ArrayList<>();
        if (be != null) {
            stack.applyComponents(be.collectComponents());
            if (be instanceof BeltBlockEntity belt)
                belt.moduleDrop().ifPresent(out::add);
        }
        out.add(0, stack);
        return out;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        ItemStack stack = new ItemStack(asItem());
        BlockEntity be = level.getBlockEntity(pos);
        if (be != null)
            stack.applyComponents(be.collectComponents());
        return stack;
    }
}
