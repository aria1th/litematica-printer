package io.github.eatmyvenom.litematicin.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class InventoryUtils {
	public static int lastCount = 0;
	public static boolean areItemsExact(ItemStack a, ItemStack b){
		return ItemStack.areItemsEqual(a,b) && ItemStack.areNbtEqual(a,b);
	}

	public static boolean requiresSwap(ClientPlayerEntity player, ItemStack stack){
		return !areItemsExact(player.getMainHandStack(), stack);
	}
	public static boolean canSwap(ClientPlayerEntity player, ItemStack stack){
		if(player.getAbilities().creativeMode){
			return true;
		}
		int slotNum = player.getInventory().getSlotWithStack(stack);
		return slotNum != -1 && areItemsExact(player.getInventory().getStack(slotNum), stack);
	}
	public static boolean swapToItem(MinecraftClient client, ItemStack stack){
		final ClientPlayerEntity player = client.player;
		if(player == null || client.interactionManager == null) {return false;}
		if (!requiresSwap(client.player, stack)){
			lastCount = client.player.getMainHandStack().getCount();
			return true;
		}
		if (survivalSwap(client, player, stack)){
			return true;
		}
		return creativeSwap(client, player, stack);
	}
	public static int getLastUsedItemCount(){
		return lastCount;
	}
	public static int getSlotWithStack(ClientPlayerEntity player, ItemStack stack){
		return player.getInventory().getSlotWithStack(stack);
	}

	@SuppressWarnings("ConstantConditions")
	private static boolean creativeSwap(MinecraftClient client, ClientPlayerEntity player, ItemStack stack){
		if (!player.getAbilities().creativeMode){
			return false;
		}
		player.getInventory().addPickBlock(stack);
		client.interactionManager.clickCreativeStack(player.getMainHandStack(), 36 + player.getInventory().selectedSlot);
		lastCount = 64;
		return true;
	}

	@SuppressWarnings("ConstantConditions")
	private static boolean survivalSwap(MinecraftClient client,ClientPlayerEntity player, ItemStack stack){
		if(!canSwap(player, stack)){
			return false;
		}
		if (areItemsExact(player.getOffHandStack(), stack)){
			lastCount = client.player.getOffHandStack().getCount();
			client.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN));
			return true;
		}
		int slot = getSlotWithStack(player, stack);
		if (slot == -1){
			return false;
		}
		lastCount = client.player.getInventory().getStack(slot).getCount();
		if (PlayerInventory.isValidHotbarIndex(slot)){
			player.getInventory().selectedSlot = slot;
			client.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
		}
		else {
			int selectedSlot = player.getInventory().selectedSlot;
			client.interactionManager.clickSlot(player.playerScreenHandler.syncId, slot, selectedSlot, SlotActionType.SWAP, player);
		}
		return true;
	}
}