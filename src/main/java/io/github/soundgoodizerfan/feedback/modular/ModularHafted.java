// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import io.github.soundgoodizerfan.feedback.core.unit.St;
import io.github.soundgoodizerfan.feedback.core.unit.Units;
import io.github.soundgoodizerfan.feedback.item.HandTool;
import io.github.soundgoodizerfan.feedback.modular.engineering.ToolAbilities;
import io.github.soundgoodizerfan.feedback.modular.trait.SmashAttack;
import io.github.soundgoodizerfan.feedback.modular.trait.SpearCharge;
import io.github.soundgoodizerfan.feedback.process.Operation;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

/**
 * Every tool you hold by a haft: a pick, an axe, a hoe, a sword, a spear, a maul, a hammer, a file. It is one item because it is one
 * thing (spec 2): a haft with one to three faces, and what it does is the union of what its live faces do ({@link ModularFaces}), never
 * a class chosen per type. It is vanilla's {@link MaceItem} so a heavy head's smash is the mace's, and it answers NeoForge's item
 * abilities from its faces so vanilla and other mods see a pick, an axe or a sword where there is one.
 *
 * <ul>
 *   <li><b>Digs</b> by the {@code TOOL} component the bake writes from every live face's mines.</li>
 *   <li><b>Strips, scrapes, tills, flattens</b> ({@link #useOn}) by what the faces' definitions list as actions, through vanilla's own
 *       axe, hoe and shovel code (the instances below are used only for that, never registered).</li>
 *   <li><b>Smashes</b> when its front trait is {@link SmashAttack}, <b>charges or is thrown</b> through the spear traits.</li>
 *   <li><b>Is a hand tool</b> ({@link HandTool}) when a face is a hammer's: the grid's blow, its remainder one point worse.</li>
 *   <li><b>Wears face by face</b> ({@link #damageItem}).</li>
 * </ul>
 */
public class ModularHafted extends MaceItem implements Modular, HandTool {

    /** Ticks of slow per joule of impact on a blow that does not smash (a maul head of another shape). */
    private static final float STAGGER_TICKS_PER_JOULE = 2f;

    /** Vanilla's own use code for what a face does to a block, in three registered but hidden items that nobody holds ({@code FModular.items} leaves them out). */
    public static Item axe() {
        return io.github.soundgoodizerfan.feedback.registry.FModular.AXE_DELEGATE.get();
    }

    public static Item hoe() {
        return io.github.soundgoodizerfan.feedback.registry.FModular.HOE_DELEGATE.get();
    }

    public static Item shovel() {
        return io.github.soundgoodizerfan.feedback.registry.FModular.SHOVEL_DELEGATE.get();
    }

    public ModularHafted(Properties properties) {
        super(properties);
    }

    // --- what it is -----------------------------------------------------------------------------

    @Override
    public Component getName(ItemStack stack) {
        return ModularStacks.name(stack, super.getName(stack));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ModularTooltip.add(stack, tooltip);
        if (ModularHandTool.blow(stack) > 0)
            tooltip.add(Component.translatable("tooltip.feedback.hand_swing",
                    Component.translatable("tooltip.feedback.hand_swing." + ModularHandTool.swing(stack).name().toLowerCase(Locale.ROOT)),
                    Units.figure(getStrength(stack))).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        ModularStacks.tick(stack, level);
        if (!level.isClientSide && entity instanceof Player player && ModularHandTool.blow(stack) > 0
                && ModularHandTool.swing(stack).within(player) != ModularHandTool.swing(stack))
            stack.set(FDataComponents.HAND_SWING, ModularHandTool.swing(stack).within(player));
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return ModularStacks.enchantability(stack, 1);
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
        return false;
    }

    /** Each face wears on its own: the point lands on the face being used, and the bar follows the most worn one. */
    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @Nullable T entity, Consumer<Item> onBroken) {
        return ModularFaces.wear(stack, amount);
    }

    // --- what it does to blocks -----------------------------------------------------------------

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {
        ModularFaces.use(stack, state);
        try {
            boolean result = super.mineBlock(stack, level, state, pos, miner);
            ModularStacks.mined(stack, level, state, pos, miner);
            sweep(stack, level, state, pos, miner);
            return result;
        } finally {
            ModularFaces.done();
        }
    }

    /** A sickle face reaps a crop or a leaf's neighbours on the same level too: one extra point of wear per four taken. */
    private static void sweep(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {
        if (level.isClientSide || !reapable(state) || ModularStacks.broken(stack) || !ModularFaces.has(stack, "sickle_head")
                || ModularTraits.of(stack).map(a -> a.params().get("sweep", 1f)).orElse(1f) == 0)
            return;
        int taken = 0;
        for (BlockPos near : BlockPos.betweenClosed(pos.offset(-1, 0, -1), pos.offset(1, 0, 1)))
            if (!near.equals(pos) && reapable(level.getBlockState(near)) && level.destroyBlock(near.immutable(), true, miner))
                taken++;
        if (taken >= 4)
            stack.hurtAndBreak(taken / 4, miner, EquipmentSlot.MAINHAND);
    }

    private static boolean reapable(BlockState state) {
        return state.is(net.minecraft.tags.BlockTags.CROPS) || state.is(net.minecraft.tags.BlockTags.LEAVES)
                || state.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_HOE);
    }

    /** Strips, scrapes, tills or flattens by what the live faces do, through vanilla's own code for each. */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        if (ModularStacks.broken(stack))
            return InteractionResult.PASS;
        Set<String> acts = ModularFaces.actions(stack);
        if (acts.isEmpty())
            return InteractionResult.PASS;
        if (acts.contains("strip") || acts.contains("scrape") || acts.contains("wax_off")) {
            InteractionResult r = axe().useOn(context);
            if (r.consumesAction())
                return r;
        }
        if (acts.contains("till")) {
            InteractionResult r = hoe().useOn(context);
            if (r.consumesAction()) {
                kama(context, r);
                return r;
            }
        }
        if (acts.contains("flatten")) {
            InteractionResult r = shovel().useOn(context);
            if (r.consumesAction())
                return r;
        }
        return InteractionResult.PASS;
    }

    /** A kama's tilling takes its neighbours on the same level too (trait {@code kama}). */
    private void kama(UseOnContext context, InteractionResult result) {
        var player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        if (!result.consumesAction() || context.getLevel().isClientSide || player == null || player.isShiftKeyDown())
            return;
        ModularTraits.of(stack).filter(a -> a.trait() instanceof io.github.soundgoodizerfan.feedback.modular.trait.Kama).ifPresent(a -> {
            for (BlockPos spot : io.github.soundgoodizerfan.feedback.modular.trait.Kama.extra(context, a.params())) {
                if (ModularStacks.broken(stack))
                    break;
                hoe().useOn(new UseOnContext(context.getLevel(), player, context.getHand(), stack,
                        new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(spot),
                                net.minecraft.core.Direction.UP, spot, false)));
            }
            player.getCooldowns().addCooldown(this, (int) io.github.soundgoodizerfan.feedback.core.Haste.time((long) a.params().get("cooldown_ticks", 20f), player));
        });
    }

    // --- what it does to creatures --------------------------------------------------------------

    private static boolean smashes(ItemStack stack) {
        return ModularTraits.of(stack).filter(a -> a.trait() instanceof SmashAttack).isPresent();
    }

    @Override
    public float getAttackDamageBonus(Entity target, float damage, DamageSource source) {
        ItemStack weapon = source.getWeaponItem() != null ? source.getWeaponItem() : ItemStack.EMPTY;
        if (!smashes(weapon))
            return 0f;
        ModularFigures f = ModularStacks.figures(weapon);
        float scale = f == null ? 1f : Math.min(1.5f, Math.max(0.15f, f.mass().value() / ModularTuning.MACE_REFERENCE_MASS));
        return super.getAttackDamageBonus(target, damage, source) * scale;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        return !smashes(stack) || super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        ModularFaces.use(stack, null);
        try {
            if (smashes(stack)) {
                super.postHurtEnemy(stack, target, attacker);
                return;
            }
            ModularType.Family combat = ModularFaces.combat(stack);
            if (combat == ModularType.Family.MACE) {
                // A maul head of another shape than a mace's swings as a maul: no smash, and the stagger.
                ModularFigures f = ModularStacks.figures(stack);
                if (f != null && !ModularStacks.broken(stack))
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                            Math.round(f.get(ModularFigures.Fig.IMPACT) * STAGGER_TICKS_PER_JOULE), 1), attacker);
            }
            stack.hurtAndBreak(combat == ModularType.Family.DIG || combat == ModularType.Family.MACE ? 2 : 1, attacker, EquipmentSlot.MAINHAND);
        } finally {
            ModularFaces.done();
        }
    }

    // --- use: charge, throw, and the traits' own ------------------------------------------------

    /** Whether the front face is a spear's and is thrown rather than charged: any head but the standard's, or one with a spring. */
    private static boolean thrown(ItemStack stack) {
        return ModularFaces.combat(stack) == ModularType.Family.SPEAR
                && (SpringPart.of(stack).isPresent() || ModularTraits.of(stack).map(a -> !(a.trait() instanceof SpearCharge)).orElse(true));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        InteractionResultHolder<ItemStack> traited = ModularTraits.use(level, player, hand, stack);
        if (traited.getResult() != InteractionResult.PASS) {
            if (ModularTraits.useDuration(stack) > 0)
                player.startUsingItem(hand);
            return traited;
        }
        if (thrown(stack) && !ModularStacks.broken(stack)) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        int duration = ModularTraits.useDuration(stack);
        return duration > 0 ? duration : thrown(stack) ? 72000 : 0;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        UseAnim anim = ModularTraits.useAnimation(stack);
        return anim != UseAnim.NONE ? anim : thrown(stack) ? UseAnim.SPEAR : UseAnim.NONE;
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        ModularTraits.onUseTick(level, user, stack, remaining);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int remaining) {
        if (ModularTraits.useDuration(stack) > 0) {
            ModularTraits.releaseUsing(stack, level, user, remaining);
            return;
        }
        if (!(user instanceof Player player) || !thrown(stack))
            return;
        Throwing.release(stack, level, player, getUseDuration(stack, user) - remaining,
                ModularTraits.of(stack).map(ModularTraits.Active::params).orElse(new ModularTrait.Params(java.util.Map.of())), 0f, 1f, 1);
    }

    // --- abilities ------------------------------------------------------------------------------

    /** What the live faces make of {@code ability}: NeoForge's tool actions, the blade's sweep, the bench abilities, a blocking guard. */
    @Override
    public boolean canPerformAction(ItemStack stack, ItemAbility ability) {
        if (ModularStacks.broken(stack))
            return false;
        if (ability == ItemAbilities.SHIELD_BLOCK)
            return ModularTraits.of(stack).filter(a -> a.trait() instanceof io.github.soundgoodizerfan.feedback.modular.trait.Blocking).isPresent();
        Set<String> acts = ModularFaces.actions(stack);
        if (ability == ItemAbilities.AXE_STRIP)
            return acts.contains("strip");
        if (ability == ItemAbilities.AXE_SCRAPE)
            return acts.contains("scrape");
        if (ability == ItemAbilities.AXE_WAX_OFF)
            return acts.contains("wax_off");
        if (ability == ItemAbilities.HOE_TILL)
            return acts.contains("till");
        if (ability == ItemAbilities.SHOVEL_FLATTEN || ability == ItemAbilities.SHOVEL_DOUSE)
            return acts.contains("flatten");
        if (ability == ItemAbilities.SWORD_SWEEP)
            return ModularFaces.combat(stack) == ModularType.Family.BLADE;
        if (ability == ItemAbilities.PICKAXE_DIG || ability == ItemAbilities.AXE_DIG || ability == ItemAbilities.SHOVEL_DIG
                || ability == ItemAbilities.HOE_DIG)
            return ModularFaces.digs(stack, ability.name().substring(0, ability.name().indexOf('_')));
        return ModularFaces.toolAbility(stack, ability);
    }

    // --- the hand tool a hammer face is ---------------------------------------------------------

    @Override
    public St getStrength(ItemStack stack) {
        return ModularHandTool.of(stack, ModularHandTool.swing(stack));
    }

    @Override
    public Operation getOperation() {
        return Operation.BLOW;
    }

    @Override
    public SoundEvent getWorkSound() {
        return SoundEvents.ANVIL_LAND;
    }

    /** A hammer's use in a grid leaves it one point worse (worn out is broken, not gone); any other tool is not a grid ingredient. */
    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        ItemStack remainder = stack.copy();
        remainder.setDamageValue(remainder.getDamageValue() + Math.max(0, Math.min(1, stack.getMaxDamage() - 1 - stack.getDamageValue())));
        return remainder;
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return ModularHandTool.isHand(stack);
    }
}
