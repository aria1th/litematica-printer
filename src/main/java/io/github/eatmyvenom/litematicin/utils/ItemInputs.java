package io.github.eatmyvenom.litematicin.utils;

import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.malilib.util.GuiUtils;
import io.github.eatmyvenom.litematicin.LitematicaMixinMod;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.*;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;

import java.util.*;

@SuppressWarnings({"ConstantConditions", "unused"})
public
class ItemInputs {

	private static long handling = new Date().getTime();
	private static final HashSet<Long> handledPos = new HashSet<>();
	private static Map.Entry<Long, Long> entry;
	public static BlockPos clickedPos = null;

	public static void clear() {
		handledPos.clear();
	}

	public static boolean canHandle() {
		return new Date().getTime() > handling + LitematicaMixinMod.INVENTORY_OPERATIONS_WAIT.getIntegerValue();
	}

	private static void handle() {
		handling = new Date().getTime();
	}

	/***
	 @param player : player Entity
	 @param required : List of stacks, might be empty to return false
	 @return boolean : should process or not
	 ***/
	public static boolean matchStacks(List<ItemStack> required, List<Slot> current, ClientPlayerEntity player, boolean allowNamed) {
		if (required.isEmpty()) {
			return false;
		}
		List<ItemStack> copy = new ArrayList<>();
		for (int i = 0; i < required.size(); i++) {
			ItemStack copiedStack = required.get(i).copy();
			copiedStack.decrement(current.get(i).getStack().getCount());
			copy.add(copiedStack);
		}
		int[] countArray = player.getInventory().main.stream().mapToInt(ItemStack::getCount).toArray();
		for (ItemStack itemStack : copy) {
			if (itemStack.isEmpty()) {
				continue;
			}
			//MessageHolder.sendUniqueDebugMessage("Checking for stack " + itemStack);
			int requiredAmount = itemStack.getCount();
			int i = 0;
			for (ItemStack playerStacks : player.getInventory().main) {
				if (countArray[i] <= 0) {
					i++;
					continue;
				}
				if (InventoryUtils.areItemsExact(itemStack, playerStacks, allowNamed)) {
					//MessageHolder.sendUniqueDebugMessage("Found stack " + itemStack + " with count " + countArray[i] + " at slot num" + i + " when remaining " + requiredAmount);
					if (countArray[i] >= requiredAmount) {
						countArray[i] -= requiredAmount;
						requiredAmount = 0;
					} else {
						requiredAmount -= countArray[i];
						countArray[i] = 0;
					}
				}
				//MessageHolder.sendUniqueDebugMessage("Item count array is " + Arrays.toString(countArray));
				if (requiredAmount <= 0) {
					break;
				}
				i++;
			}
			if (requiredAmount > 0) {
				return false;
			}
		}
		return true;
	}

	private static int getPreference(MinecraftClient client, ScreenHandler screen, ItemStack itemStack, boolean allowNamed) {
		if (screen == null || itemStack.isEmpty()) {
			return -1;
		}
		for (int i = 0; i < screen.slots.size(); i++) {
			if (!(screen.getSlot(i).inventory instanceof PlayerInventory)) {
				continue;
			}
			ItemStack playerStacks = screen.getSlot(i).getStack();
			if (InventoryUtils.areItemsExact(itemStack, playerStacks, allowNamed)) {
				return i;
			}
		}
		return -1;
	}

	private static List<Slot> getNonPlayerSlots(MinecraftClient client, ScreenHandler screen) {
		List<Slot> retVal = new ArrayList<>();
		for (int i = 0; i < screen.slots.size(); i++) {
			if ((screen.getSlot(i).inventory instanceof PlayerInventory)) {
				continue;
			}
			retVal.add(screen.getSlot(i));
		}
		return retVal;
	}

	private static void clearCursor(MinecraftClient client) {
		final ClientPlayerEntity player = client.player;
		if (player.currentScreenHandler != null && !player.currentScreenHandler.getCursorStack().isEmpty()) {
			ItemStack cursorStack = player.currentScreenHandler.getCursorStack();
			ScreenHandler handler = player.currentScreenHandler;
			for (int i = 0; i < handler.slots.size(); i++) {
				if (ItemStack.canCombine(cursorStack, handler.getSlot(i).getStack())) {
					client.interactionManager.clickSlot(handler.syncId, handler.getSlot(i).id, 0, SlotActionType.PICKUP, player);
					return;
				}
			}
			client.interactionManager.clickSlot(handler.syncId, -999, 1, SlotActionType.THROW, player);
		}
	}

	public static void execute(MinecraftClient client) {
		if (canHandle()) {
			handle();
		} else {
			MessageHolder.sendUniqueMessageActionBar(client.player, "Cooldown....");
			return;
		}
		boolean allowNamed = LitematicaMixinMod.INVENTORY_OPERATIONS_FILTER_ALLOW_NAMED.getBooleanValue();
		BlockPos where = rayCast(client);
		if (where == null) {
			MessageHolder.sendUniqueMessageActionBar(client.player, "Failed to raycast");
			return;
		}
		if (handledPos.contains(where.asLong())) {
			MessageHolder.sendUniqueMessageActionBar(client.player, "Position is already handled");
			clickedPos = null;
			client.player.closeHandledScreen();
			return;
		}
		List<ItemStack> requiredStacks = getRaycastRequiredItemStacks(client);
		if (requiredStacks.isEmpty()) {
			MessageHolder.sendUniqueDebugMessage("required stacks were empty for " + where.toShortString());
			client.player.closeHandledScreen();
			return;
		}
		MessageHolder.sendUniqueDebugMessage("Handled pos " + where.toShortString());
		List<Slot> nonPlayerSlot = getNonPlayerSlots(client, client.player.currentScreenHandler);
		if (matchStacks(requiredStacks, nonPlayerSlot, client.player, allowNamed)) {
			//MessageHolder.sendUniqueDebugMessage("Required pos " + where.toShortString());

			//MessageHolder.sendUniqueDebugMessage(nonPlayerSlot.toString());
			if (requiredStacks.size() != nonPlayerSlot.size()) {
				MessageHolder.sendMessageUncheckedUnique(client.player, "Sizes differ as " + requiredStacks.size() + " but non-player slot size : " + nonPlayerSlot.size());
				return;
			}
			if (entry == null || entry.getKey() != where.asLong()) {
				entry = Map.entry(where.asLong(), new Date().getTime() + LitematicaMixinMod.INVENTORY_OPERATIONS_WAIT.getIntegerValue());
				return;
			} else if (entry.getValue() > new Date().getTime()) {
				return;
			} else {
				entry = null;
			}
			boolean allCorrect = true;
			for (int j = 0; j < LitematicaMixinMod.INVENTORY_OPERATIONS_RETRY.getIntegerValue(); j++) {
				for (int i = 0; i < requiredStacks.size(); i++) {
					if (InventoryUtils.areItemsExactCount(nonPlayerSlot.get(i).getStack(), requiredStacks.get(i), allowNamed)) {
						continue;
					}
					sendItem(client, nonPlayerSlot.get(i), requiredStacks.get(i), allowNamed);
					if (!InventoryUtils.areItemsExactCount(nonPlayerSlot.get(i).getStack(), requiredStacks.get(i), allowNamed)) {
						allCorrect = false;
					}
				}
			}
			if (allCorrect) {
				handledPos.add(where.asLong());
				MessageHolder.sendUniqueDebugMessage("Successfully done operation at " + where.toShortString());
				if (LitematicaMixinMod.INVENTORY_OPERATIONS_CLOSE_SCREEN.getBooleanValue()) {
					client.player.closeHandledScreen();
				}
			} else {
				MessageHolder.sendUniqueDebugMessage("Partially failed to send all items at " + where.toShortString() + ", will retry");
			}
		} else {
			MessageHolder.sendUniqueDebugMessage("Does not have enough item for " + where.toShortString() + "!");
			if (LitematicaMixinMod.INVENTORY_OPERATIONS_CLOSE_SCREEN.getBooleanValue()) {
				client.player.closeHandledScreen();
			}
		}
	}

	/***
	 * Part of the execution
	 * @param client : MinecraftClient
	 * @param targetSlot : Integer of slot defined as slot.id
	 * @param stack : Wanted slot to send
	 * @param allowNamed : allows Named item to go instead
	 */
	private static void sendItem(MinecraftClient client, int targetSlot, ItemStack stack, boolean allowNamed) {
		clearCursor(client);
		clearUnmatchTargetSlot(client, targetSlot, stack, allowNamed);
		int holding = getPreference(client, client.player.currentScreenHandler, stack, allowNamed);
		if (holding == -1) {
			return;
		} //actually we can do this
		HandledScreen<? extends ScreenHandler> gui = (HandledScreen<?>) GuiUtils.getCurrentScreen();
		leftClickSlot(gui, holding);
		for (int i = 0; i < stack.getCount() - gui.getScreenHandler().getSlot(targetSlot).getStack().getCount(); i++) {
			rightClickSlot(gui, targetSlot);
		}
		leftClickSlot(gui, holding);
		MessageHolder.sendUniqueDebugMessage("Sent item from " + holding + " to " + targetSlot);
		//client.interactionManager.clickSlot(client.player.currentScreenHandler.syncId, targetSlot, holding, SlotActionType.SWAP, client.player);
	}

	private static void sendItem(MinecraftClient client, Slot targetSlot, ItemStack stack, boolean allowNamed) {
		sendItem(client, targetSlot.id, stack, allowNamed);
	}

	private static void clearUnmatchTargetSlot(MinecraftClient client, int targetSlot, ItemStack wantedItem, boolean allowNamed) {
		ItemStack slotStack = client.player.currentScreenHandler.getSlot(targetSlot).getStack();
		if (InventoryUtils.areItemsExact(slotStack, wantedItem, allowNamed) && slotStack.getCount() <= wantedItem.getCount()) {
			return;
		}
		//else we need to clear
		HandledScreen<? extends ScreenHandler> gui = (HandledScreen<?>) GuiUtils.getCurrentScreen();
		leftClickSlot(gui, targetSlot);
		clearCursor(client);
	}

	private static void leftClickSlot(HandledScreen<? extends ScreenHandler> gui, int slotNum) {
		clickSlot(gui, slotNum, 0, SlotActionType.PICKUP);
	}

	private static void rightClickSlot(HandledScreen<? extends ScreenHandler> gui, int slotNum) {
		clickSlot(gui, slotNum, 1, SlotActionType.PICKUP);
	}

	private static void shiftClickSlot(HandledScreen<? extends ScreenHandler> gui, int slotNum) {
		clickSlot(gui, slotNum, 0, SlotActionType.QUICK_MOVE);
	}

	public static void clickSlot(HandledScreen<? extends ScreenHandler> gui, int slotNum, int button, SlotActionType action) {
		if (slotNum >= 0 && slotNum < gui.getScreenHandler().slots.size()) {
			Slot slot = gui.getScreenHandler().getSlot(slotNum);
			clickSlot(gui, slot, button, action);
		}
	}

	public static void clickSlot(HandledScreen<? extends ScreenHandler> gui, Slot slot, int button, SlotActionType action) {
		try {
			MinecraftClient.getInstance().interactionManager.clickSlot(gui.getScreenHandler().syncId, slot.id, button, action, MinecraftClient.getInstance().player);
		} catch (Exception e) {
			MessageHolder.sendMessageUncheckedUnique(MinecraftClient.getInstance().player, "Clicking slot failed ");
			MessageHolder.sendMessageUncheckedUnique(MinecraftClient.getInstance().player, e.getMessage());
		}
	}

	public static List<ItemStack> getRaycastRequiredItemStacks(MinecraftClient minecraftClient) {
		List<ItemStack> retVal = new ArrayList<>();
		Screen screen = minecraftClient.currentScreen;
		if (screen == null) {
			return retVal;
		}
		if (screen instanceof InventoryScreen) {
			MessageHolder.sendUniqueDebugMessage(minecraftClient.player, "Screen was InventoryScreen");
			return retVal;
		}
		BlockPos context = rayCast(minecraftClient);
		if (context == null) {
			return retVal;
		}
		if (handledPos.contains(context.asLong())) {
			MessageHolder.sendUniqueDebugMessage(minecraftClient.player, "Screen was already registered");
			return retVal;
		}
		if (context == null) {
			return retVal;
		}
		if (!(screen instanceof GenericContainerScreen || screen instanceof Generic3x3ContainerScreen || screen instanceof HopperScreen)) {
			return retVal;
		}
		return InventoryUtils.getRequiredStackInSchematic(SchematicWorldHandler.getSchematicWorld(), minecraftClient, context);
	}

	private static BlockPos rayCast(MinecraftClient minecraftClient) {

		if (clickedPos == null) {
			MessageHolder.sendUniqueMessageActionBar(minecraftClient.player, "Current raycast is set to null");
			return null;
		}
		MessageHolder.sendUniqueMessageActionBar(minecraftClient.player, "Current raycast is set to " + clickedPos.toShortString());
		BlockPos castedPos = clickedPos;
		Block block = minecraftClient.world.getBlockState(castedPos).getBlock();
		if (block instanceof BlockWithEntity) {
			if (block instanceof HopperBlock || block instanceof ChestBlock || block instanceof DispenserBlock) {
				return castedPos;
			}
		}
		return null;
	}


}