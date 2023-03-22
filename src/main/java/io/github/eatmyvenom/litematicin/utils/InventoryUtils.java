package io.github.eatmyvenom.litematicin.utils;

import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
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
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import static io.github.eatmyvenom.litematicin.utils.Printer.isSleeping;

public class InventoryUtils {
	private static int ptr = -1;
	public static int lastCount = 0;
	public static int itemChangeCount = 0;
	public static Item handlingItem = null;
	public static Item previousItem = null; //only used for checks
	public static int trackedSelectedSlot = -1;
	public static HashMap<Integer, Item> usedSlots = new LinkedHashMap<>();
	public static HashMap<Integer, Integer> slotCounts = new LinkedHashMap<>();

	public static void tick() {
		if (!isSleeping && Configs.Generic.EASY_PLACE_MODE.getBooleanValue() && Configs.Generic.EASY_PLACE_HOLD_ENABLED.getBooleanValue() && Hotkeys.EASY_PLACE_ACTIVATION.getKeybind().isKeybindHeld()) {
			for (int i = 0; i < 9; i++) {
				if (!usedSlots.containsKey(i)) {
					continue;
				}
				if (slotCounts.get(i) <= 0) {
					usedSlots.remove(i);
					slotCounts.remove(i);
				}
			}
		} else {
			trackedSelectedSlot = -1;
			previousItem = null;
			handlingItem = null;
			usedSlots.clear();
			slotCounts.clear();
		}
	}


	public static void decrementCount() {
		if (lastCount > 0) {
			lastCount--;
			slotCounts.computeIfPresent(trackedSelectedSlot, (key, value) -> value - 1);
		}
	}
	public static void decrementCount(boolean isCreative) {
		if (isCreative) lastCount = 65536;
		if (lastCount > 0) {
			lastCount--;
			slotCounts.computeIfPresent(trackedSelectedSlot, (key, value) -> value - 1);
		}
	}
	private static int getPtr() {
		ptr++;
		ptr = ptr % 9;
		return ptr;
	}

	public static int getAvailableSlot(Item item) {
		if (usedSlots.containsValue(item)) {
			for (Integer i : usedSlots.keySet()) {
				if (usedSlots.get(i) == item) {
					return i;
				}
			}
			return -1;
		}
		if (usedSlots.size() == 9) { //full
			return getPtr();
		}
		for (int i = 0; i < 9; i++) {
			if (usedSlots.containsKey(i)) {
				continue;
			}
			return i;
		}
		return -1;
	}

	private static int searchSlot(Item item) {
		for (Integer i : usedSlots.keySet()) {
			if (usedSlots.get(i) == item && slotCounts.getOrDefault(i, 0) > 0) {
				return i;
			}
		}
		return -1;
	}

	public static boolean hasEmptyHotbar() {
		return usedSlots.size() < 9;
	}

	public static ItemStack getMainHandStack(ClientPlayerEntity player) {
		return player.getMainHandStack();
	}

	public static boolean areItemsExact(ItemStack a, ItemStack b) {
		return ItemStack.areItemsEqual(a, b) && ItemStack.areTagsEqual(a, b);
	}

	public static boolean areItemsExact(ItemStack a, ItemStack b, boolean allowNamed) {
		if (allowNamed) {
			return areItemsExactAllowNamed(a, b);
		}
		return ItemStack.areItemsEqual(a, b) && ItemStack.areTagsEqual(a, b);
	}

	public static boolean areItemsExactCount(ItemStack a, ItemStack b, boolean allowNamed) {
		if (a.getCount() != b.getCount()) {
			return false;
		}
		if (allowNamed) {
			return areItemsExactAllowNamed(a, b);
		}
		return ItemStack.areItemsEqual(a, b) && ItemStack.areTagsEqual(a, b);
	}

	public static boolean areItemsExactAllowNamed(ItemStack a, ItemStack b) {
		if (a.getItem() instanceof ToolItem || b.getItem() instanceof ToolItem) { //safety
			return false;
		}
		return ItemStack.areItemsEqual(a, b) || a.getMaxCount() == b.getMaxCount() && a.hasCustomName() && b.hasCustomName();
	}

	public static boolean requiresSwap(ClientPlayerEntity player, ItemStack stack) {
		int selectedSlot = player.inventory.selectedSlot;
		if (usedSlots.get(selectedSlot) != null) {
			return stack.getItem() != usedSlots.get(selectedSlot) || slotCounts.getOrDefault(selectedSlot, 0) <= 0;
		}
		return previousItem == null || lastCount == 0 ? !areItemsExact(getMainHandStack(player), stack) : !areItemsExact(previousItem.getDefaultStack(), stack);
	}

	public static boolean canSwap(ClientPlayerEntity player, ItemStack stack) {
		if (player.abilities.creativeMode) {
			return true;
		}
		int slotNum = player.inventory.getSlotWithStack(stack);
		return slotNum != -1 && areItemsExact(player.inventory.getStack(slotNum), stack);
	}

	synchronized public static boolean swapToItem(MinecraftClient client, ItemStack stack) {
		MessageHolder.sendOrderMessage("Trying to swap item into " + stack.getItem());
		ClientPlayerEntity player = client.player;
		int maxChange = LitematicaMixinMod.PRINTER_MAX_ITEM_CHANGES.getIntegerValue();
		if (player == null || client.interactionManager == null) {
			return false;
		}
		//player.inventory.updateItems();
		if (stack.getItem() != handlingItem) {
			if (maxChange != 0 && itemChangeCount > maxChange) {
				MessageHolder.sendOrderMessage("Exceeded item change count");
				return false;
			}
		}
		if (!requiresSwap(player, stack)) {
			assert trackedSelectedSlot == -1 || trackedSelectedSlot == player.inventory.selectedSlot : "Selected slot changed for external reason! : expected / current : " + trackedSelectedSlot + player.inventory.selectedSlot;
			assert previousItem == null || previousItem == stack.getItem() : "Handling item :  " + handlingItem + " was not equal to " + stack.getItem();
			MessageHolder.sendOrderMessage("Didn't require swap for item " + stack.getItem() + " previous handling item : " + previousItem);
			lastCount = player.abilities.creativeMode ? 65536 : getMainHandStack(player).getCount();
			if (usedSlots.containsValue(stack.getItem())) {
				if (searchSlot(stack.getItem()) != trackedSelectedSlot) {
					MessageHolder.sendMessageUncheckedUnique("Hotbar has duplicate item references, which should not happen!");
				}
			}
			trackedSelectedSlot = player.inventory.selectedSlot;
			usedSlots.put(player.inventory.selectedSlot, getMainHandStack(player).getItem());
			slotCounts.put(player.inventory.selectedSlot, lastCount);
			previousItem = stack.getItem();
			return true;
		}
		if (usedSlots.containsValue(stack.getItem())) {
			int slot = searchSlot(stack.getItem());
			if (slot != -1) {
				player.inventory.selectedSlot = slot;
				trackedSelectedSlot = player.inventory.selectedSlot;
				usedSlots.put(trackedSelectedSlot, stack.getItem());
				slotCounts.put(trackedSelectedSlot, stack.getCount());
				lastCount = stack.getCount();
				previousItem = stack.getItem();
				handlingItem = previousItem;
				MessageHolder.sendOrderMessage("Selected slot " + player.inventory.selectedSlot + " based on cache for " + stack.getItem());
				client.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(player.inventory.selectedSlot));
				return !player.inventory.getMainHandStack().isEmpty();
			}
		}
		if (survivalSwap(client, player, stack)) {
			usedSlots.put(player.inventory.selectedSlot, stack.getItem());
			slotCounts.put(trackedSelectedSlot, getMainHandStack(player).getCount());
			MessageHolder.sendOrderMessage("Swapped to item " + stack.getItem());
			handlingItem = stack.getItem();
			previousItem = handlingItem;
			itemChangeCount++;
			return true;
		}
		return creativeSwap(client, player, stack);
	}

	public static int getSlotWithStack(ClientPlayerEntity player, ItemStack stack) {
		return player.inventory.getSlotWithStack(stack);
	}

	@SuppressWarnings("ConstantConditions")
	private static boolean creativeSwap(MinecraftClient client, ClientPlayerEntity player, ItemStack stack) {
		if (!player.abilities.creativeMode) {
			return false;
		}
		int selectedSlot = getAvailableSlot(stack.getItem());
		if (selectedSlot == -1) {
			return false;
		}
		MessageHolder.sendOrderMessage("Clicked creative stack " + stack.getItem() + " for slot " + selectedSlot);
		//player.inventory.addPickBlock(stack);
		player.inventory.selectedSlot = selectedSlot;
		client.interactionManager.clickCreativeStack(stack, 36 + selectedSlot);
		client.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(player.inventory.selectedSlot));
		trackedSelectedSlot = selectedSlot;
		player.inventory.main.set(selectedSlot, stack);
		usedSlots.put(player.inventory.selectedSlot, stack.getItem());
		slotCounts.put(trackedSelectedSlot, 65536);
		lastCount = 65536;
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
			lastCount = client.player.abilities.creativeMode ? 65536 : client.player.getOffHandStack().getCount();
			client.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN));
			return true;
		}
		int slot = getSlotWithStack(player, stack);
		if (slot == -1) {
			return false;
		}
		if (PlayerInventory.isValidHotbarIndex(slot)) {
			if (usedSlots.get(slot) != null) {
				MessageHolder.sendOrderMessage("Hotbar slot should have been handled before, so it must be error!");
				MessageHolder.sendOrderMessage("Expected : " + usedSlots.get(slot) + " but current client handles : " + stack.getItem());
				return false;
			}
			player.inventory.selectedSlot = slot;
			trackedSelectedSlot = slot;
			MessageHolder.sendOrderMessage("Selected hotbar Slot " + slot);
			lastCount = player.abilities.creativeMode ? 65536 : player.inventory.getStack(slot).getCount();
			client.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
		} else {
			int selectedSlot = getAvailableSlot(stack.getItem());
			if (selectedSlot == -1) {
				MessageHolder.sendOrderMessage("All hotbar slots are used");
				return false;
			}
			lastCount = player.abilities.creativeMode ? 65536 : player.inventory.getStack(slot).getCount();
			MessageHolder.sendOrderMessage("Slot at " + slot + (player.inventory.getStack(slot).getItem()) + " is swapped with " + selectedSlot + (player.inventory.main.get(selectedSlot)));
			usedSlots.put(selectedSlot, stack.getItem());
			client.interactionManager.clickSlot(player.playerScreenHandler.syncId, slot, selectedSlot, SlotActionType.SWAP, player);
			player.inventory.selectedSlot = selectedSlot;
			trackedSelectedSlot = selectedSlot;

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
		if (blockEntity instanceof LootableContainerBlockEntity) {
			LootableContainerBlockEntity containerBlockEntity = (LootableContainerBlockEntity) blockEntity;
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

	public static boolean hasItemInSchematic(World schematicWorld, BlockPos pos) {
		BlockEntity blockEntity = schematicWorld.getBlockEntity(pos);
		if (blockEntity == null) {
			return false;
		}
		if (blockEntity instanceof LootableContainerBlockEntity) {
			LootableContainerBlockEntity containerBlockEntity = (LootableContainerBlockEntity) blockEntity;
			if (containerBlockEntity.isEmpty()) {
				return false;
			}
			for (int i = 0; i < (containerBlockEntity).size(); i++) {
				if (!containerBlockEntity.getStack(i).isEmpty()) {
					return false;
				}
			}
		}
		return false;
	}
}