package io.github.eatmyvenom.litematicin.utils;

import io.github.eatmyvenom.litematicin.LitematicaMixinMod;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolItem;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class InventoryUtils {
	public static int lastCount = 0;
	public static int itemChangeCount = 0;
	public static Item handlingItem = null;
	public static Item previousItem = null; //only used for checks

	public static void decrementCount() {
		if (lastCount > 0) {
			lastCount--;
		}
	}

	public static ItemStack getMainHandStack(ClientPlayerEntity player) {
		return player.getInventory().main.get(player.getInventory().selectedSlot);
	}

	public static boolean areItemsExact(ItemStack a, ItemStack b) {
		return ItemStack.areItemsEqual(a, b) && ItemStack.areNbtEqual(a, b);
	}

	public static boolean areItemsExact(ItemStack a, ItemStack b, boolean allowNamed) {
		if (allowNamed) {
			return areItemsExactAllowNamed(a, b);
		}
		return ItemStack.areItemsEqual(a, b) && ItemStack.areNbtEqual(a, b);
	}

	public static boolean areItemsExactCount(ItemStack a, ItemStack b, boolean allowNamed) {
		if (a.getCount() != b.getCount()) {
			return false;
		}
		if (allowNamed) {
			return areItemsExactAllowNamed(a, b);
		}
		return ItemStack.areItemsEqual(a, b) && ItemStack.areNbtEqual(a, b);
	}

	public static boolean areItemsExactAllowNamed(ItemStack a, ItemStack b) {
		if (a.getItem() instanceof ToolItem || b.getItem() instanceof ToolItem) { //safety
			return false;
		}
		return ItemStack.areItemsEqual(a, b) || a.getMaxCount() == b.getMaxCount() && a.hasCustomName() && b.hasCustomName();
	}

	public static boolean requiresSwap(ClientPlayerEntity player, ItemStack stack) {
		return previousItem == null || lastCount == 0 ? !areItemsExact(getMainHandStack(player), stack) : !areItemsExact(previousItem.getDefaultStack(), stack);
	}

	public static boolean canSwap(ClientPlayerEntity player, ItemStack stack) {
		if (player.getAbilities().creativeMode) {
			return true;
		}
		int slotNum = player.getInventory().getSlotWithStack(stack);
		return slotNum != -1 && areItemsExact(player.getInventory().getStack(slotNum), stack);
	}

	synchronized public static boolean swapToItem(MinecraftClient client, ItemStack stack) {
		ClientPlayerEntity player = client.player;
		int maxChange = LitematicaMixinMod.PRINTER_MAX_ITEM_CHANGES.getIntegerValue();
		if (player == null || client.interactionManager == null) {
			return false;
		}
		player.getInventory().updateItems();
		if (stack.getItem() != handlingItem) {
			if (maxChange != 0 && itemChangeCount > maxChange) {
				return false;
			}
		}
		if (!requiresSwap(client.player, stack)) {
			assert previousItem == stack.getItem() : "Handling item :  " + handlingItem + " was not equal to " + stack.getItem();
			MessageHolder.sendOrderMessage("Didn't require swap for item " + stack.getItem() + " previous handling item : " + previousItem);
			lastCount = getMainHandStack(player).getCount();
			return true;
		}
		if (survivalSwap(client, player, stack)) {
			MessageHolder.sendOrderMessage("Swapped to item " + stack.getItem());
			handlingItem = stack.getItem();
			previousItem = handlingItem;
			itemChangeCount++;
			return true;
		}
		return creativeSwap(client, player, stack);
	}

	public static int getSlotWithStack(ClientPlayerEntity player, ItemStack stack) {
		return player.getInventory().getSlotWithStack(stack);
	}

	@SuppressWarnings("ConstantConditions")
	private static boolean creativeSwap(MinecraftClient client, ClientPlayerEntity player, ItemStack stack) {
		if (!player.getAbilities().creativeMode) {
			return false;
		}
		player.getInventory().addPickBlock(stack);
		client.interactionManager.clickCreativeStack(getMainHandStack(player), 36 + player.getInventory().selectedSlot);
		lastCount = 64;
		handlingItem = stack.getItem();
		previousItem = handlingItem;
		itemChangeCount++;
		return true;
	}

	@SuppressWarnings("ConstantConditions")
	private static boolean survivalSwap(MinecraftClient client, ClientPlayerEntity player, ItemStack stack) {
		if (!canSwap(player, stack)) {
			return false;
		}
		if (areItemsExact(player.getOffHandStack(), stack) && !areItemsExact(getMainHandStack(player), stack)) {
			lastCount = client.player.getOffHandStack().getCount();
			client.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN));
			return true;
		}
		int slot = getSlotWithStack(player, stack);
		if (slot == -1) {
			return false;
		}
		lastCount = client.player.getInventory().getStack(slot).getCount();
		if (PlayerInventory.isValidHotbarIndex(slot)) {
			player.getInventory().selectedSlot = slot;
			MessageHolder.sendOrderMessage("Selected Slot " + slot);
			//client.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
		} else {
			int selectedSlot = player.getInventory().selectedSlot;
			MessageHolder.sendOrderMessage("Slot at " + slot + " is swapped with " + selectedSlot);
			client.interactionManager.clickSlot(player.playerScreenHandler.syncId, slot, selectedSlot, SlotActionType.SWAP, player);
		}
		try {
			assert getMainHandStack(player).isItemEqual(stack);
		} catch (Exception e) {
			MessageHolder.sendMessageUncheckedUnique(player, stack.toString() + " does not match with " + player.getMainHandStack().toString() + "!");
		}
		return true;
	}

	public static List<ItemStack> getRequiredStackInSchematic(World schematicWorld, MinecraftClient minecraftClient, BlockPos pos) {
		final ClientPlayerEntity player = minecraftClient.player;
		List<ItemStack> result = new ArrayList<>();
		BlockEntity blockEntity = schematicWorld.getBlockEntity(pos);
		if (blockEntity == null) {
			return result;
		}
		if (blockEntity instanceof LootableContainerBlockEntity containerBlockEntity) {
			if (containerBlockEntity.isEmpty()) {
				return result;
			}
			if (containerBlockEntity.canPlayerUse(player)) {
				for (int i = 0; i < (containerBlockEntity).size(); i++) {
					result.add(containerBlockEntity.getStack(i));
				}
			} else {
				MessageHolder.sendMessageUncheckedUnique(player, "Container at " + pos.toShortString() + "can't be opened by player!");
			}
		}
		return result;
	}
}