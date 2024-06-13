package io.github.eatmyvenom.litematicin.utils;

import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.malilib.event.TickHandler;
import fi.dy.masa.malilib.interfaces.IClientTickHandler;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
//#if MC >= 12100
//$$ import java.util.Optional;
//$$ import net.minecraft.enchantment.Enchantment;
//$$ import net.minecraft.enchantment.Enchantments;
//$$ import net.minecraft.registry.RegistryKeys;
//$$ import net.minecraft.registry.entry.RegistryEntry;
//#endif

/**
 * The breaking needs to be done every tick, since the WorldUtils.easyPlaceOnUseTick (which calls our Printer)
 * is called multiple times per tick we cannot break blocks through that method. Or the speed will be twice the
 * normal speed and detectable by anti-cheats.
 */
public class Breaker implements IClientTickHandler {

	private boolean breakingBlock = false;
	private BlockPos pos;

	public Breaker() {
		TickHandler.getInstance().registerClientTickHandler(this);
	}

	public boolean startBreakingBlock(BlockPos pos, MinecraftClient mc) {
		this.breakingBlock = true;
		this.pos = pos;
		// Check for best tool in inventory
		if (mc.world.getBlockState(pos).getHardness(mc.world, pos) == 0) {
			mc.interactionManager.attackBlock(pos, Direction.UP);
			return false;
		}
		int bestSlotId = getBestItemSlotIdToMineBlock(mc, pos);
		// If slot isn't selected, change
		if (bestSlotId != -1) {
			ItemStack stack = InventoryUtils.getInventory(mc.player).getStack(bestSlotId);
			InventoryUtils.swapToItem(mc, stack);
		}
		// Start breaking
		BlockState blockState = mc.world.getBlockState(pos);
		//#if MC>=11800
		if (blockState.calcBlockBreakingDelta(mc.player, mc.player.getWorld(), pos) >= 1.0F) {
		//#else
		//$$ if (blockState.calcBlockBreakingDelta(mc.player, mc.player.world, pos) >= 1.0F) {
		//#endif
			mc.interactionManager.attackBlock(pos, Direction.UP);
			return false;
		}
		TickHandler.getInstance().registerClientTickHandler(this);
		return true;
	}

	public boolean isBreakingBlock() {
		if (this.pos == null || MinecraftClient.getInstance().world == null) {
			return false;
		}
		if (BedrockBreaker.isReplaceable(MinecraftClient.getInstance().world.getBlockState(pos))) {
			this.breakingBlock = false;
		}
		return this.breakingBlock;
	}

	public static int getBestItemSlotIdToMineBlock(MinecraftClient mc, BlockPos blockToMine) {
		int bestSlot = -1;
		float bestSpeed = 0;
		BlockState state = mc.world.getBlockState(blockToMine);
		return getFastestToolSlot(mc, bestSlot, bestSpeed, state);
	}

	private static int getFastestToolSlot(MinecraftClient mc, int bestSlot, float bestSpeed, BlockState state) {
		for (int i = InventoryUtils.getInventory(mc.player).size(); i >= 0; i--) {
			float speed = getBlockBreakingSpeed(state, mc, i);
			if ((speed > bestSpeed && speed > 1.0F)
				|| (speed >= bestSpeed && !mc.player.getInventory().getStack(i).isDamageable())) {
				bestSlot = i;
				bestSpeed = speed;
			}
		}
		return bestSlot;
	}

	public static int getBestItemSlotIdToMineState(MinecraftClient mc, BlockState state) {
		int bestSlot = -1;
		float bestSpeed = 0;
		return getFastestToolSlot(mc, bestSlot, bestSpeed, state);
	}

	public static float getBlockBreakingSpeed(BlockState block, MinecraftClient mc, int slotId) {
		if (slotId < -1 || slotId >= 36) {
			return 0;
		}
		float f = InventoryUtils.getInventory(mc.player).main.get(slotId).getMiningSpeedMultiplier(block);
		if (f > 1.0F) {
			//#if MC >= 12100
			//$$ ItemStack itemStack = mc.player.getInventory().getMainHandStack();
			//$$ Optional<RegistryEntry.Reference<Enchantment>> optional = mc.world.getRegistryManager().get(RegistryKeys.ENCHANTMENT).getEntry(Enchantments.EFFICIENCY);
			//$$ int i =  optional.map(enchantmentReference -> EnchantmentHelper.getLevel(enchantmentReference, itemStack)).orElse(0);
			//#else
			int i = EnchantmentHelper.getEfficiency(mc.player);
			ItemStack itemStack = mc.player.getInventory().getMainHandStack();
			//#endif
			if (i > 0 && !itemStack.isEmpty()) {
				f += (float) (i * i + 1);
			}
		}
		return f;
	}

	@Override
	public void onClientTick(MinecraftClient mc) {
		if (!isBreakingBlock() || mc.player == null) {
			this.breakingBlock = false;
			return;
		}

		if (Hotkeys.EASY_PLACE_ACTIVATION.getKeybind().isKeybindHeld()) { // Only continue mining while the correct keys are pressed
			Direction side = Direction.values()[0];
			if (mc.interactionManager.updateBlockBreakingProgress(pos, side)) {
				mc.particleManager.addBlockBreakingParticles(pos, side);
				mc.player.swingHand(Hand.MAIN_HAND);
			}
		}

		if (!BedrockBreaker.isReplaceable(mc.world.getBlockState(pos))) {
			this.breakingBlock = false;
			return;
		} // If block isn't broken yet, dont stop
		// Stop breaking
		this.breakingBlock = false;
		mc.interactionManager.cancelBlockBreaking();
	}

}
