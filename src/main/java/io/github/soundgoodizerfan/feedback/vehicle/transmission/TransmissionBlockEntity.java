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
package io.github.soundgoodizerfan.feedback.vehicle.transmission;

import io.github.soundgoodizerfan.feedback.control.Analog;
import io.github.soundgoodizerfan.feedback.control.data.DataNode;
import io.github.soundgoodizerfan.feedback.control.data.DataNodeRef;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationNetworks;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationNode;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationPropagator;
import io.github.soundgoodizerfan.feedback.machine.Wearing;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FVehicles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The gearbox of a design, and the gear it is in. It joins the rotation node behind it to the one in
 * front with a {@link RotationNetworks.Link} whose ratio is the gear's (none in neutral), which the
 * propagator already walks both ways, so no network code knows a transmission exists.
 *
 * <p><b>Shifting at speed is misuse.</b> A change of gear while the input is turning faster than
 * {@link FTuning#TRANSMISSION_FREE_SHIFT_RPM} grinds the teeth: wear in proportion to the speed, by the
 * material's hardness. Worn below {@link FTuning#TRANSMISSION_POP_BELOW}, it jumps out of gear under
 * load now and then, more often the worse it is. Correct use -- stopped, or in neutral -- costs nothing,
 * and nothing wears it for running.
 *
 * <p>An {@link Analog} part: channel 0 is the shifter, 0 to 1 over neutral, the forward gears and reverse;
 * channel 1 is the clutch pedal, 0 to 1. Pressed past {@link FTuning#TRANSMISSION_CLUTCH_PRESSED} it
 * parts the link, so the input coasts free of the output, and a shift made then does not grind: the
 * clutch is how a gear is changed at speed without misuse. Dropped, it couples again at once.
 */
public class TransmissionBlockEntity extends BlockEntity implements Analog, DataNode, Wearing {

    @Nullable
    private TransmissionDesign design;
    private int position;
    private float condition = 1f;
    /** The clutch pedal, 0 to 1; the controller's (channel 1), so not saved. */
    private float clutch;
    /** What was last published, so an unchanged state costs nothing. */
    private float published = Float.NaN;
    private final Set<DataNodeRef> links = new HashSet<>();

    public TransmissionBlockEntity(BlockPos pos, BlockState state) {
        super(FVehicles.TRANSMISSION_ENTITY.get(), pos, state);
    }

    @Nullable
    public TransmissionDesign design() {
        return design;
    }

    public int position() {
        return position;
    }

    /** The shifter's position for a message: N, a gear number, or R. */
    public Component label() {
        if (design == null || position <= 0)
            return Component.literal("N");
        return Component.literal(position > design.gears() ? "R" : String.valueOf(position));
    }

    @Override
    public float getCondition() {
        return condition;
    }

    @Override
    public boolean isRunning() {
        return position > 0;
    }

    /** The input shaft's speed, RPM: zero with nothing turning behind. */
    private float inputRpm() {
        if (level == null)
            return 0;
        Direction out = getBlockState().getValue(TransmissionBlock.FACING);
        return level.getBlockEntity(worldPosition.relative(out.getOpposite())) instanceof RotationNode node
                ? Math.abs(node.getRpm().value()) : 0;
    }

    /** Moves the shifter (wrapping), grinding the teeth if the input is turning. */
    public void shift(int to) {
        if (design == null)
            return;
        int next = Math.floorMod(to, design.positions());
        if (next == position)
            return;
        float rpm = inputRpm();
        if (rpm > FTuning.TRANSMISSION_FREE_SHIFT_RPM && position != 0 && next != 0 && !clutchPressed()) {
            var material = MaterialTable.get().materials().get(design.material());
            if (material != null)
                condition = Math.max(FTuning.TRANSMISSION_CONDITION_FLOOR, condition - rpm * design.wearPerRpm(material));
            if (level != null)
                level.playSound(null, worldPosition, SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 0.3f, 0.5f);
        }
        position = next;
        setChanged();
        publish();
    }

    // --- the link -------------------------------------------------------------------------------

    private boolean clutchPressed() {
        return clutch >= FTuning.TRANSMISSION_CLUTCH_PRESSED;
    }

    private float ratio() {
        return design == null || clutchPressed() ? 0f : design.speedRatio(position);
    }

    /** Publishes the link for the current gear, and rebuilds the runs it joins or parts. */
    void publish() {
        if (level == null || level.isClientSide)
            return;
        Direction out = getBlockState().getValue(TransmissionBlock.FACING);
        float ratio = ratio();
        List<RotationNetworks.Link> links = ratio == 0f ? List.of()
                : List.of(new RotationNetworks.Link(worldPosition.relative(out.getOpposite()), worldPosition.relative(out), ratio));
        for (BlockPos touched : RotationNetworks.of(level).setLinks(worldPosition, links))
            RotationPropagator.rebuildFrom(level, touched);
        published = ratio;
    }

    /** The block is going: its link goes with it. */
    void unpublish() {
        if (level == null || level.isClientSide)
            return;
        for (BlockPos touched : RotationNetworks.of(level).setLinks(worldPosition, List.of()))
            RotationPropagator.rebuildFrom(level, touched);
    }

    static void serverTick(Level level, BlockPos pos, BlockState state, TransmissionBlockEntity be) {
        if (be.ratio() != be.published)
            be.publish();
        // A worn box jumps out of gear under load.
        if (be.design != null && be.position != 0 && be.condition < FTuning.TRANSMISSION_POP_BELOW
                && be.inputRpm() > FTuning.TRANSMISSION_FREE_SHIFT_RPM
                && level.random.nextFloat() < (FTuning.TRANSMISSION_POP_BELOW - be.condition) * FTuning.TRANSMISSION_POP_RATE) {
            level.playSound(null, pos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.4f, 0.4f);
            be.position = 0;
            be.setChanged();
            be.publish();
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        published = Float.NaN; // republished on the first tick, once the nodes beside it are loaded
    }

    // --- Analog ---------------------------------------------------------------------------------

    @Override
    public void setLevel(int channel, float level) {
        if (channel == 1)
            clutch = Mth.clamp(level, 0f, 1f);
        else if (channel == 0 && design != null)
            shift(Math.round(Mth.clamp(level, 0f, 1f) * (design.positions() - 1)));
    }

    @Override
    public String[] channelKeys() {
        return new String[]{"feedback.analog.gear", "feedback.analog.clutch"};
    }

    // --- DataNode -------------------------------------------------------------------------------

    @Override
    public boolean acceptsLinks() {
        return false;
    }

    @Override
    public BlockPos getNodePos() {
        return worldPosition;
    }

    @Nullable
    @Override
    public Direction getNodeSide() {
        return null;
    }

    @Override
    public Level getNodeLevel() {
        return level;
    }

    @Override
    public BlockEntity getNodeOwner() {
        return this;
    }

    @Override
    public Set<DataNodeRef> getNodeLinks() {
        return links;
    }

    // --- persistence ----------------------------------------------------------------------------

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        design = input.get(FVehicles.TRANSMISSION_DESIGN.get());
        condition = input.getOrDefault(FDataComponents.TOOL_CONDITION.get(), 1f);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (design != null)
            components.set(FVehicles.TRANSMISSION_DESIGN.get(), design);
        if (condition < 1f)
            components.set(FDataComponents.TOOL_CONDITION.get(), condition);
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        super.removeComponentsFromTag(tag);
        tag.remove("Design");
        tag.remove("Condition");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (design != null)
            TransmissionDesign.CODEC.encodeStart(NbtOps.INSTANCE, design).ifSuccess(t -> tag.put("Design", t));
        tag.putInt("Position", position);
        tag.putFloat("Condition", condition);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        design = tag.contains("Design") ? TransmissionDesign.CODEC.parse(NbtOps.INSTANCE, tag.get("Design")).result().orElse(null) : null;
        position = tag.getInt("Position");
        condition = tag.contains("Condition") ? tag.getFloat("Condition") : 1f;
    }
}
