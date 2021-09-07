package io.github.eatmyvenom.litematicin.utils;

import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.malilib.event.TickHandler;
import fi.dy.masa.malilib.hotkeys.KeybindMulti;
import fi.dy.masa.malilib.interfaces.IClientTickHandler;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * The breaking needs to be done every tick, since the WorldUtils.easyPlaceOnUseTick (which calls our Printer)
 * is called multiple times per tick we cannot break blocks through that method. Or the speed will be twice the
 * normal speed and detectable by anti-cheats.
 * 
 *
 */
public class Breaker implements IClientTickHandler {
	
	private boolean breakingBlock = false;
	private BlockPos pos;
	
	public Breaker() {
		TickHandler.getInstance().registerClientTickHandler(this);
	}
	
	public void startBreakingBlock(BlockPos pos, MinecraftClient mc) {
		this.breakingBlock = true;
		this.pos = pos;
		// Check for best tool in inventory
		int bestSlotId = getBestItemSlotIdToMineBlock(mc, pos);
		// If slot isn't selected, change
		if (mc.player.getInventory().selectedSlot != bestSlotId) {
			mc.player.getInventory().selectedSlot = bestSlotId;
		}
		// Start breaking
		TickHandler.getInstance().registerClientTickHandler(this);
	}
	
	public boolean isBreakingBlock() {
		return this.breakingBlock;
	}
	
	public static int getBestItemSlotIdToMineBlock(MinecraftClient mc, BlockPos blockToMine) {
		int bestSlot = 0;
		float bestSpeed = 0;
		BlockState state = mc.world.getBlockState(blockToMine);
		for (int i = 8; i >= 0; i--) {
			float speed = getBlockBreakingSpeed(state, mc, i);
			if ((speed > bestSpeed && speed > 1.0F)
					|| (speed >= bestSpeed && !mc.player.getInventory().getStack(i).isDamageable())) {
				bestSlot = i;
				bestSpeed = speed;
			}
		}
		return bestSlot;
	}
	
	private static float getBlockBreakingSpeed(BlockState block, MinecraftClient mc, int slotId) {
		float f = ((ItemStack)mc.player.getInventory().main.get(slotId)).getMiningSpeedMultiplier(block);
	    if (f > 1.0F) {
	       int i = EnchantmentHelper.getEfficiency(mc.player);
	       ItemStack itemStack = mc.player.getInventory().getMainHandStack();
	       if (i > 0 && !itemStack.isEmpty()) {
	          f += (float)(i * i + 1);
	       }
	    }
	    return f;
	}

	@Override
	public void onClientTick(MinecraftClient mc) {
		if (!isBreakingBlock()) return;
		if (mc.player == null) return;
		
		if (Hotkeys.EASY_PLACE_ACTIVATION.getKeybind().isKeybindHeld() &&
	            KeybindMulti.isKeyDown(KeybindMulti.getKeyCode(mc.options.keyUse))) { // Only continue mining while the correct keys are pressed
			Direction side = Direction.values()[0];
			
			if (mc.interactionManager.updateBlockBreakingProgress(pos, side)) {
				mc.particleManager.addBlockBreakingParticles(pos, side);
				mc.player.swingHand(Hand.MAIN_HAND);
			}
		}
		
		if (!mc.world.getBlockState(pos).isAir()) return; // If block isn't broken yet, dont stop
		
		// Stop breaking
		this.breakingBlock = false;
		mc.interactionManager.cancelBlockBreaking();
	}
	
}
