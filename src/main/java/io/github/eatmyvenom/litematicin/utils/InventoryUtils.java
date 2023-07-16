package io.github.eatmyvenom.litematicin.utils;

import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.litematica.materials.MaterialCache;
import io.github.eatmyvenom.litematicin.LitematicaMixinMod;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.*;
import java.util.function.Predicate;

import static io.github.eatmyvenom.litematicin.LitematicaMixinMod.*;
import static io.github.eatmyvenom.litematicin.utils.Printer.*;

public class InventoryUtils {
	private static int ptr = -1;

	public final static HashSet<Item> ITEMS = new HashSet<>();
	public static int lastCount = 0;
	public static int itemChangeCount = 0;
	public static Item handlingItem = null;
	public static Item previousItem = null; //only used for checks
	public static int trackedSelectedSlot = -1;

	private static long tickCount = 0;
	private static long lastWorkedTick = 0;

	private static String cachedPickBlockableSlots = "";

	private static final HashSet<Integer> pickBlockableSlots = new HashSet<>();
	public static HashMap<Integer, Item> usedSlots = new LinkedHashMap<>();
	public static HashMap<Integer, Integer> slotCounts = new LinkedHashMap<>();

	public static void tick() {
		tickCount++;
		if (RENDER_ONLY_HOLDING_ITEMS.getBooleanValue() && tickCount % 20 == 0) {
			calculateCache();
		}
		if (INVENTORY_CACHE_TICKS.getIntegerValue() != 0 && tickCount - lastWorkedTick > INVENTORY_CACHE_TICKS.getIntegerValue()){
			clearCache();
		}
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
			clearCache();
		}
	}

	public static void clearCache(){
		if (!usedSlots.isEmpty()) MessageHolder.sendOrderMessage("Clearing cache");
		trackedSelectedSlot = -1;
		previousItem = null;
		handlingItem = null;
		usedSlots.clear();
		slotCounts.clear();
		lastWorkedTick = tickCount;
	}


	private static void calculateCache() {
		ITEMS.clear();
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null || client.player == null || client.world == null) {
			return;
		}
		//#if MC>=11700
		for (ItemStack stack : client.player.getInventory().main) {
		//#else
		//$$ for (ItemStack stack : client.player.inventory.main) {
		//#endif
			Item item = stack.getItem();
			if (item != null) {
				ITEMS.add(item);
			}
			if (item instanceof BlockItem) {
				BlockItem blockItem = (BlockItem) item;
				if (blockItem.getBlock() instanceof ShulkerBoxBlock) {
					int invSize = 27;
					NbtCompound compound = stack.getSubNbt("BlockEntityTag");
					if (compound == null) {
						continue;
					}
					DefaultedList<ItemStack> returnStacks = DefaultedList.ofSize(invSize, ItemStack.EMPTY);
					if (compound.contains("Items")) {
						Inventories.readNbt(compound, returnStacks);
					}
					for (ItemStack returnStack : returnStacks) {
						Item returnItem = returnStack.getItem();
						if (returnItem != null) {
							ITEMS.add(returnItem);
						}
					}
				}
			}
		}
	}

	public static PlayerInventory getInventory(ClientPlayerEntity player) {
		//#if MC>=11700
		return player.getInventory();
		//#else
		//$$ return player.inventory;
		//#endif
	}

	public static boolean isCreative(ClientPlayerEntity player) {
		//#if MC>=11700
		return player.getAbilities().creativeMode;
		//#else
		//$$ return player.abilities.creativeMode;
		//#endif
	}


	public static void decrementCount() {
		if (lastCount > 0) {
			lastCount--;
			slotCounts.computeIfPresent(trackedSelectedSlot, (key, value) -> value - 1);
		}
	}
	public static void decrementCount(boolean isCreative) {
		if (isCreative) lastCount = 65536;
		if (lastCount > 0 && usedSlots.get(trackedSelectedSlot) != null && usedSlots.get(trackedSelectedSlot).getMaxCount() != 1) {
			lastCount--;
			slotCounts.computeIfPresent(trackedSelectedSlot, (key, value) -> value - 1);
		}
	}
	private static int getPtr() {
		parsePickblockableSlots();
		if (pickBlockableSlots.isEmpty()) {
			return -1;
		}
		ptr++;
		ptr = ptr % pickBlockableSlots.size();
		return ptr;
	}

	private static void parsePickblockableSlots() {
		String pickBlockableSlot = Configs.Generic.PICK_BLOCKABLE_SLOTS.getStringValue();
		if (!pickBlockableSlot.equals(cachedPickBlockableSlots)) {
			cachedPickBlockableSlots = pickBlockableSlot;
			pickBlockableSlots.clear();
			for (String s : pickBlockableSlot.split(",")) {
				try {
					int i = Integer.parseInt(s);
					if (i>0 && i<10) {
						pickBlockableSlots.add(i-1);
					}
				} catch (NumberFormatException e) {
					// ignore
				}
			}
		}
	}

	// getAvailableSlot() is used to get the slot that the item is in, or the next available slot if it's not in the hotbar
	public static int getAvailableSlot(Item item) {
		if (usedSlots.containsValue(item)) {
			for (Integer i : usedSlots.keySet()) {
				if (usedSlots.get(i) == item) {
					return i;
				}
			}
			return -1;
		}
		parsePickblockableSlots();
		if (usedSlots.size() == pickBlockableSlots.size()) { //full
			return getPtr();
		}
		for (int i = 0; i < 9; i++) {
			if (usedSlots.containsKey(i) || !pickBlockableSlots.contains(i)) {
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
		return usedSlots.size() < pickBlockableSlots.size();
	}

	public static ItemStack getMainHandStack(ClientPlayerEntity player) {
		return player.getMainHandStack();
	}

	public static boolean areItemsExact(ItemStack a, ItemStack b) {
		// ToolItem or FlintAndSteelItem
		return exceptToolItems(a, b);
	}

	public static boolean areItemsExact(ItemStack a, ItemStack b, boolean allowNamed) {
		if (allowNamed) {
			return areItemsExactAllowNamed(a, b);
		}
		return exceptToolItems(a, b);
	}

	private static boolean exceptToolItems(ItemStack a, ItemStack b) {
		if (a.getItem() instanceof ToolItem && b.getItem() instanceof ToolItem || a.getItem() instanceof FlintAndSteelItem && b.getItem() instanceof FlintAndSteelItem) {
			return a.getItem() == b.getItem();
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

	public static ItemStack getStackForState(MinecraftClient client, BlockState state, World world, BlockPos pos) {
		// if state is nether portal block, return FLINT_AND_STEEL
		if (state.isOf(Blocks.NETHER_PORTAL)) {
			if (!PRINTER_LIT_PORTAL_USE_FIRECHARGE.getBooleanValue()) return Items.FLINT_AND_STEEL.getDefaultStack();
			else {
				return Items.FIRE_CHARGE.getDefaultStack();
			}
		}
		ItemStack stack = isReplaceableWaterFluidSource(state) && PRINTER_PLACE_ICE.getBooleanValue() ? Items.ICE.getDefaultStack() : MaterialCache.getInstance().getRequiredBuildItemForState(state, world, pos);
		if (PRINTER_PRINT_DIRT_VARIANTS.getBooleanValue() && !canPickItem(client, stack)) {
			if (state.isOf(Blocks.FARMLAND)) stack = Items.DIRT.getDefaultStack();
			else if (state.isOf(Blocks.DIRT_PATH)) stack = Items.DIRT.getDefaultStack();
		}
		return stack;
	}

	public static boolean areItemsExactAllowNamed(ItemStack a, ItemStack b) {
		// ToolItem or FlintAndSteelItem
		if (a.getItem() instanceof ToolItem && b.getItem() instanceof ToolItem || a.getItem() instanceof FlintAndSteelItem && b.getItem() instanceof FlintAndSteelItem) {
			return a.getItem() == b.getItem();
		}
		else if (a.getItem() instanceof ToolItem || b.getItem() instanceof ToolItem) {
			return false; // safety
		}
		return ItemStack.areItemsEqual(a, b) || a.getMaxCount() == b.getMaxCount() && a.hasCustomName() && b.hasCustomName();
	}

	public static boolean requiresSwap(ClientPlayerEntity player, ItemStack stack) {
		int selectedSlot = getInventory(player).selectedSlot;
		if (usedSlots.get(selectedSlot) != null) {
			return stack.getItem() != usedSlots.get(selectedSlot) || slotCounts.getOrDefault(selectedSlot, 0) <= 0;
		}
		return previousItem == null || lastCount == 0 ? !areItemsExact(getMainHandStack(player), stack) : !areItemsExact(previousItem.getDefaultStack(), stack);
	}

	public static boolean canSwap(ClientPlayerEntity player, ItemStack stack) {
		if (isCreative(player)) {
			return true;
		}
		int slotNum = getSlotWithStack(player, stack);
		return slotNum != -1;
	}

	public static int getSlotWithItem(PlayerInventory inv, ItemStack stack) {
		for (int i = 0; i < inv.main.size(); i++) {
			if (ItemStack.areItemsEqual(inv.getStack(i), stack)) {
				return i;
			}
		}
		return -1;
	}

	public static boolean canSwap(ClientPlayerEntity player, Predicate<ItemStack> predicate) {
		if (isCreative(player)) {
			return true;
		}
		Inventory inv = getInventory(player);
		for (int i = 0; i < inv.size(); i++) {
			ItemStack stack = inv.getStack(i);
			if (stack.getItem() instanceof ToolItem && predicate.test(stack)) {
				return true;
			}
		}
		return false;
	}

	synchronized public static boolean swapToItem(MinecraftClient client, ItemStack stack) {
		MessageHolder.sendOrderMessage("Trying to swap item into " + stack.getItem());
		ClientPlayerEntity player = client.player;
		int maxChange = LitematicaMixinMod.PRINTER_MAX_ITEM_CHANGES.getIntegerValue();
		if (player == null || client.interactionManager == null) {
			MessageHolder.sendOrderMessage("Player or interaction manager was null");
			return false;
		}
		//getInventory(player).updateItems();
		if (stack.getItem() != handlingItem) {
			if (maxChange != 0 && itemChangeCount > maxChange) {
				MessageHolder.sendOrderMessage("Exceeded item change count");
				return false;
			}
		}
		if (!requiresSwap(player, stack)) {
			assert trackedSelectedSlot == -1 || trackedSelectedSlot == getInventory(player).selectedSlot :
				"Selected slot changed for external reason! : expected " + trackedSelectedSlot + ", current " + getInventory(player).selectedSlot;
			assert previousItem == null || previousItem == stack.getItem() : "Handling item :  " + handlingItem + " was not equal to " + stack.getItem();
			MessageHolder.sendOrderMessage("Didn't require swap for item " + stack.getItem() + " previous handling item : " + previousItem);
			lastCount = isCreative(player) ? 65536 : getMainHandStack(player).getCount();
			if (usedSlots.containsValue(stack.getItem())) {
				if (searchSlot(stack.getItem()) != trackedSelectedSlot) {
					MessageHolder.sendMessageUncheckedUnique("Hotbar has duplicate item references, which should not happen!");
				}
			}
			trackedSelectedSlot = getInventory(player).selectedSlot;
			usedSlots.put(getInventory(player).selectedSlot, getMainHandStack(player).getItem());
			slotCounts.put(getInventory(player).selectedSlot, lastCount);
			previousItem = stack.getItem();
			lastWorkedTick = tickCount;
			return true;
		}
		if (usedSlots.containsValue(stack.getItem())) {
			int slot = searchSlot(stack.getItem());
			if (slot != -1) {
				getInventory(player).selectedSlot = slot;
				trackedSelectedSlot = getInventory(player).selectedSlot;
				usedSlots.put(trackedSelectedSlot, stack.getItem());
				slotCounts.put(trackedSelectedSlot, stack.getCount());
				lastCount = stack.getCount();
				previousItem = stack.getItem();
				handlingItem = previousItem;
				lastWorkedTick = tickCount;
				MessageHolder.sendOrderMessage("Selected slot " + getInventory(player).selectedSlot + " based on cache for " + stack.getItem());
				client.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(getInventory(player).selectedSlot));
				return !getInventory(player).getMainHandStack().isEmpty();
			}
			else {
				MessageHolder.sendOrderMessage("Used slot contains stack but cannot find " + stack.getItem());
			}
		}
		MessageHolder.sendOrderMessage("Trying survival Swap");
		if (survivalSwap(client, player, stack)) {
			usedSlots.put(getInventory(player).selectedSlot, stack.getItem());
			slotCounts.put(trackedSelectedSlot, getMainHandStack(player).getCount());
			MessageHolder.sendOrderMessage("Swapped to item " + stack.getItem());
			handlingItem = stack.getItem();
			previousItem = handlingItem;
			itemChangeCount++;
			lastWorkedTick = tickCount;
			return true;
		}
		MessageHolder.sendOrderMessage("Survival swap failed, trying creative swap");
		return creativeSwap(client, player, stack);
	}

	synchronized public static ItemStack findItem(MinecraftClient client, Predicate<ItemStack> predicate) {
		ClientPlayerEntity player = client.player;
		if (player == null) {
			return ItemStack.EMPTY;
		}
		Inventory inv = getInventory(player);
		for (int i = 0; i < inv.size(); i++) {
			ItemStack stack = inv.getStack(i);
			if (stack.getItem() instanceof ToolItem && predicate.test(stack)) {
				return stack;
			}
		}
		return ItemStack.EMPTY;
	}

	synchronized public static boolean swapToItem(MinecraftClient client, Predicate<ItemStack> predicate) {
		ItemStack stack = findItem(client, predicate);
		return swapToItem(client, stack);
	}

	public static int getSlotWithStack(ClientPlayerEntity player, ItemStack stack) {
		PlayerInventory inv = getInventory(player);
		return stack.getItem() instanceof ToolItem || stack.getItem() instanceof FlintAndSteelItem ? getSlotWithItem(inv, stack) :inv.getSlotWithStack(stack);
	}

	public static int getSlotWithStack(PlayerInventory inv, ItemStack stack) {
		return stack.getItem() instanceof ToolItem || stack.getItem() instanceof FlintAndSteelItem ? getSlotWithItem(inv, stack) :inv.getSlotWithStack(stack);
	}

	@SuppressWarnings("ConstantConditions")
	private static boolean creativeSwap(MinecraftClient client, ClientPlayerEntity player, ItemStack stack) {
		if (!isCreative(player)) {
			MessageHolder.sendOrderMessage("Player is not in creative mode");
			return false;
		}
		int selectedSlot = getAvailableSlot(stack.getItem());
		if (selectedSlot == -1) {
			MessageHolder.sendOrderMessage("No available slot for " + stack.getItem());
			return false;
		}
		MessageHolder.sendOrderMessage("Clicked creative stack " + stack.getItem() + " for slot " + selectedSlot);
		//getInventory(player).addPickBlock(stack);
		getInventory(player).selectedSlot = selectedSlot;
		client.interactionManager.clickCreativeStack(stack, 36 + selectedSlot);
		client.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(getInventory(player).selectedSlot));
		trackedSelectedSlot = selectedSlot;
		getInventory(player).main.set(selectedSlot, stack);
		usedSlots.put(getInventory(player).selectedSlot, stack.getItem());
		slotCounts.put(trackedSelectedSlot, 65536);
		lastCount = 65536;
		handlingItem = stack.getItem();
		previousItem = handlingItem;
		itemChangeCount++;
		lastWorkedTick = tickCount;
		return true;
	}

	@SuppressWarnings("ConstantConditions")
	private static boolean survivalSwap(MinecraftClient client, ClientPlayerEntity player, ItemStack stack) {
		if (!canSwap(player, stack)) {
			return false;
		}
		if (areItemsExact(player.getOffHandStack(), stack) && !areItemsExact(getMainHandStack(player), stack)) {
			lastCount = isCreative(player) ? 65536 : client.player.getOffHandStack().getCount();
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
			getInventory(player).selectedSlot = slot;
			trackedSelectedSlot = slot;
			MessageHolder.sendOrderMessage("Selected hotbar Slot " + slot);
			lastCount = isCreative(player) ? 65536 : getInventory(player).getStack(slot).getCount();
			client.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
		} else {
			int selectedSlot = getAvailableSlot(stack.getItem());
			if (selectedSlot == -1) {
				MessageHolder.sendOrderMessage("All hotbar slots are used");
				return false;
			}
			lastCount = isCreative(player) ? 65536 : getInventory(player).getStack(slot).getCount();
			MessageHolder.sendOrderMessage("Slot at " + slot + "(" + getInventory(player).getStack(slot).getItem() + ")" + " is swapped with " + selectedSlot + "(" + getInventory(player).main.get(selectedSlot) + ")");
			usedSlots.put(selectedSlot, stack.getItem());
			client.interactionManager.clickSlot(player.playerScreenHandler.syncId, slot, selectedSlot, SlotActionType.SWAP, player);
			getInventory(player).selectedSlot = selectedSlot;
			trackedSelectedSlot = selectedSlot;

		}
		try {
			assert ItemStack.areEqual(getMainHandStack(player), stack);
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
			// might use JAVA 8
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
			// might use JAVA 8
			LootableContainerBlockEntity containerBlockEntity = (LootableContainerBlockEntity) blockEntity;
			if (containerBlockEntity.isEmpty()) {
				return false;
			}
			for (int i = 0; i < (containerBlockEntity).size(); i++) {
				if (!containerBlockEntity.getStack(i).isEmpty()) {
					return true;
				}
			}
		}
		return false;
	}
}