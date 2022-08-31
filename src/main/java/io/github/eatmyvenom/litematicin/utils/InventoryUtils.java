package io.github.eatmyvenom.litematicin.utils;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
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
import java.util.List;

public class InventoryUtils {
	private static int ptr = -1;
	public static int lastCount = 0;
	public static int itemChangeCount = 0;
	public static Item handlingItem = null;
	public static Item previousItem = null; //only used for checks
	public static int trackedSelectedSlot = -1;
	public static BiMap<Integer, Item> usedSlots = HashBiMap.create();

	public static void tick() {
		if (Configs.Generic.EASY_PLACE_MODE.getBooleanValue() && Configs.Generic.EASY_PLACE_HOLD_ENABLED.getBooleanValue() && Hotkeys.EASY_PLACE_ACTIVATION.getKeybind().isKeybindHeld()) {

		} else {
			trackedSelectedSlot = -1;
			previousItem = null;
			handlingItem = null;
			usedSlots.clear();
		}
	}


	public static void decrementCount() {
		if (lastCount > 0) {
			lastCount--;
		}
	}

	private static int getPtr() {
		ptr++;
		ptr = ptr % 9;
		return ptr;
	}

	public static int getAvailableSlot(Item item) {
		if (usedSlots.inverse().containsKey(item)) {
			return usedSlots.inverse().get(item);
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

	public static boolean hasEmptyHotbar() {
		return usedSlots.size() < 9;
	}

	public static ItemStack getMainHandStack(ClientPlayerEntity player) {
		return player.getMainHandStack();
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
		int selectedSlot = player.getInventory().selectedSlot;
		if (usedSlots.get(selectedSlot) != null) {
			return stack.getItem() != usedSlots.get(selectedSlot);
		}
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
		//player.getInventory().updateItems();
		if (stack.getItem() != handlingItem) {
			if (maxChange != 0 && itemChangeCount > maxChange) {
				return false;
			}
		}
		if (!requiresSwap(player, stack)) {
			assert trackedSelectedSlot == -1 || trackedSelectedSlot == player.getInventory().selectedSlot : "Selected slot changed for external reason! : expected %s, current %s".formatted(trackedSelectedSlot, player.getInventory().selectedSlot);
			assert previousItem == stack.getItem() : "Handling item :  " + handlingItem + " was not equal to " + stack.getItem();
			MessageHolder.sendOrderMessage("Didn't require swap for item " + stack.getItem() + " previous handling item : " + previousItem);
			lastCount = player.getAbilities().creativeMode ? 65536 : getMainHandStack(player).getCount();
			if (usedSlots.containsValue(stack.getItem())) {
				if (usedSlots.inverse().get(stack.getItem()) != trackedSelectedSlot) {
					MessageHolder.sendMessageUncheckedUnique("Hotbar has duplicate item references, which should not happen!");
				}
			}
			usedSlots.forcePut(player.getInventory().selectedSlot, getMainHandStack(player).getItem());
			return true;
		}
		if (usedSlots.containsValue(stack.getItem())) {
			player.getInventory().selectedSlot = usedSlots.inverse().get(stack.getItem());
			trackedSelectedSlot = player.getInventory().selectedSlot;
			previousItem = stack.getItem();
			handlingItem = previousItem;
			MessageHolder.sendOrderMessage("Selected slot " + player.getInventory().selectedSlot + " based on cache for " + stack.getItem());
			client.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(player.getInventory().selectedSlot));
			return !player.getInventory().getMainHandStack().isEmpty();
		}
		if (survivalSwap(client, player, stack)) {
			usedSlots.put(player.getInventory().selectedSlot, stack.getItem());
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
		int selectedSlot = getAvailableSlot(stack.getItem());
		if (selectedSlot == -1) {
			return false;
		}
		MessageHolder.sendOrderMessage("Clicked creative stack " + stack.getItem() + " for slot " + selectedSlot);
		//player.getInventory().addPickBlock(stack);
		player.getInventory().selectedSlot = selectedSlot;
		client.interactionManager.clickCreativeStack(stack, 36 + selectedSlot);
		client.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(player.getInventory().selectedSlot));
		trackedSelectedSlot = selectedSlot;
		player.getInventory().main.set(selectedSlot, stack);
		usedSlots.put(player.getInventory().selectedSlot, stack.getItem());
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
			lastCount = client.player.getAbilities().creativeMode ? 65536 : client.player.getOffHandStack().getCount();
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
			player.getInventory().selectedSlot = slot;
			trackedSelectedSlot = slot;
			MessageHolder.sendOrderMessage("Selected hotbar Slot " + slot);
			lastCount = player.getAbilities().creativeMode ? 65536 : player.getInventory().getStack(slot).getCount();
			client.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
		} else {
			int selectedSlot = getAvailableSlot(stack.getItem());
			if (selectedSlot == -1) {
				MessageHolder.sendOrderMessage("All hotbar slots are used");
				return false;
			}
			lastCount = player.getAbilities().creativeMode ? 65536 : player.getInventory().getStack(slot).getCount();
			MessageHolder.sendOrderMessage("Slot at " + slot + "(%s)".formatted(player.getInventory().getStack(slot).getItem()) + " is swapped with " + selectedSlot + "(%s)".formatted(player.getInventory().main.get(selectedSlot)));
			usedSlots.put(selectedSlot, stack.getItem());
			client.interactionManager.clickSlot(player.playerScreenHandler.syncId, slot, selectedSlot, SlotActionType.SWAP, player);
			player.getInventory().selectedSlot = selectedSlot;
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

	public static boolean hasItemInSchematic(World schematicWorld, BlockPos pos) {
		BlockEntity blockEntity = schematicWorld.getBlockEntity(pos);
		if (blockEntity == null) {
			return false;
		}
		if (blockEntity instanceof LootableContainerBlockEntity containerBlockEntity) {
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