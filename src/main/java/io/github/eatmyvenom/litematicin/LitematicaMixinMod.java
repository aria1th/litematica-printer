package io.github.eatmyvenom.litematicin;

import com.google.common.collect.ImmutableList;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigDouble;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.config.options.ConfigInteger;
import fi.dy.masa.malilib.hotkeys.KeyCallbackToggleBooleanConfigWithMessage;
import net.fabricmc.api.ModInitializer;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class LitematicaMixinMod implements ModInitializer {
	public static ImmutableList.Builder<IConfigBase> originalList = new ImmutableList.Builder<IConfigBase>().addAll(Configs.Generic.OPTIONS);
	public static final ConfigBoolean USE_INVENTORY_CACHE = new ConfigBoolean("printerUseInventoryCache", true, "Uses Inventory cache instead of litematica's inventory utils.");
	public static final ConfigInteger INVENTORY_CACHE_TICKS = new ConfigInteger("printerInventoryCacheTicks", 20, 0, 100, "Ticks to wait before updating inventory cache.");
	public static final ConfigBoolean VERIFY_INVENTORY = new ConfigBoolean("verifierFindInventoryContents", true, "Schematic verifier will show blocks with inventory as wrong state.");
	public static final ConfigBoolean PRINTER_OFF = new ConfigBoolean("printerOff", false, "Disables printer.");

	public static final ConfigBoolean PRINTER_LIT_PORTAL_USE_FIRECHARGE = new ConfigBoolean("printerLitPortalUseFireCharge", false, "Uses fire charge to light portal instead of flint and steel.");
	public static final ConfigBoolean PRINTER_ONLY_FAKE_ROTATION_MODE = new ConfigBoolean("easyPlaceMode++", false, "Disables printer and only make fake rotation work.");
	public static final ConfigBoolean PRINTER_SHOULD_SWING_HAND = new ConfigBoolean("printerSwingHand", false, "Swings hand when placing/attacking blocks.");
	public static final ConfigBoolean PRINTER_PRINT_DIRT_VARIANTS = new ConfigBoolean("printerPrintDirtVariant", false, "Uses shovel/hoe on dirt to make variants. Also tries to place dirt instead first");
	public static final ConfigBoolean RENDER_ONLY_HOLDING_ITEMS = new ConfigBoolean("printerRenderOnlyHoldingItems", false, "Only render blocks that are in player's inventory.");
	public static final ConfigBoolean PRINTER_FAKE_ROTATION_AGGRESSIVE = new ConfigBoolean("printerFakeRotationAggressive", false, "Packets will be sent aggressively to force player direction.");
	public static final ConfigBoolean PRINTER_SUPPRESS_PACKETS = new ConfigBoolean("printerFakeRotationSuppressOther", false, "Prevents other packets than fake rotations from being sent when EasyPlaceMode is on.");
	public static final ConfigBoolean PRINTER_ALLOW_INVENTORY_OPERATIONS = new ConfigBoolean("printerAllowInventoryOperations", false, "Printer will try to fill filters when screen is open and stop other actions");
	public static final ConfigBoolean PRINTER_INVENTORY_OPERATIONS_CLOSE_SCREEN = new ConfigBoolean("inventoryCloseScreenAfterDone", false, "Screen will be closed after inventory filling operations");
	public static final ConfigInteger PRINTER_INVENTORY_SCREEN_WAIT = new ConfigInteger("printerInventoryScreenWait", 200, 0, 8000, "Time(ms) to wait screen to be opened and synced");
	public static final ConfigInteger PRINTER_INVENTORY_OPERATIONS_RETRY = new ConfigInteger("printerInventoryOperationRetry", 3, 1, 32, "Times to retry inventory operations");
	public static final ConfigBoolean PRINTER_INVENTORY_OPERATION_ALLOW_ALL_NAMED = new ConfigBoolean("printerInventoryOperationAllowAllNamed", false, "Filter items can be replaced with named items with same stack size");
	public static final ConfigBoolean DEBUG_MESSAGE = new ConfigBoolean("ShowDebugMessages", false, "Show Debugs and Reasons for block place failure");
	public static final ConfigBoolean DEBUG_EXTRA_MESSAGE = new ConfigBoolean("ShowDebugExtraMessages", false, "Show Debugs for block placement and fake rotations");
	public static final ConfigBoolean DEBUG_ORDER_PLACEMENTS = new ConfigBoolean("ShowDebugAndOrders", false, "Show Debugs for block picking / orders");
	public static final ConfigBoolean DEBUG_PACKET_SYNC = new ConfigBoolean("BetaPacketDesyncPrevention", false, "Prevents desync from previous revisions");
	public static final ConfigBoolean DISABLE_SINGLEPLAYER_HANDLE = new ConfigBoolean("disableSingleplayerPlacementHandling", true, "Disables handling for fake rotations block state adjustment");
	public static final ConfigInteger PRINTER_SLEEP_STACK_EMPTIED = new ConfigInteger("printerSleepStackEmptied", 200, 0, 8000, "Sleeps after stack is emptied (ms)");
	public static final ConfigInteger EASY_PLACE_CACHE_TIME = new ConfigInteger("easyPlaceCacheMs", 1000, 200, 5000, "Cache time for Easyplace");
	public static final ConfigInteger EASY_PLACE_MODE_RANGE_X = new ConfigInteger("easyPlaceModePrinterRangeX", 4, 0, 1024, "X Range for EasyPlace");
	public static final ConfigInteger EASY_PLACE_MODE_RANGE_Y = new ConfigInteger("easyPlaceModePrinterRangeY", 4, 0, 1024, "Y Range for EasyPlace");
	public static final ConfigInteger EASY_PLACE_MODE_RANGE_Z = new ConfigInteger("easyPlaceModePrinterRangeZ", 4, 0, 1024, "Z Range for EasyPlace");
	public static final ConfigInteger PRINTER_MAX_BLOCKS = new ConfigInteger("easyPlaceModePrinterMaxBlocks", 6, 1, 1000000, "Max block interactions per cycle");
	public static final ConfigInteger PRINTER_MAX_ITEM_CHANGES = new ConfigInteger("easyPlaceModePrinterMaxItemChanges", 0, 0, 1000000, "Max item categories per cycle");
	public static final ConfigBoolean PRINTER_BREAK_BLOCKS = new ConfigBoolean("printerBreakBlocks", false, "Automatically breaks blocks.");
	public static final ConfigBoolean PRINTER_BREAK_IGNORE_EXTRA = new ConfigBoolean("printerBreakIgnoresExtra", true, "Does not break extra blocks.");
	public static final ConfigBoolean DISABLE_SYNC = new ConfigBoolean("disableInventorySync", false, "Disables sync with inventory.");
	public static final ConfigDouble EASY_PLACE_MODE_DELAY = new ConfigDouble("easyPlaceModeDelay", 0.2, 0.0, 1.0, "Delay between printing blocks.\n Recommended to set value over 0.05(50ms).");
	public static final ConfigBoolean EASY_PLACE_MODE_HOTBAR_ONLY = new ConfigBoolean("easyPlaceModeHotbarOnly", false, "Only place blocks from your hotbar.");
	public static final ConfigBoolean PRINTER_FLIPPINCACTUS = new ConfigBoolean("printerFlippincactus", false, "If FlippinCactus is enabled and cactus is on mainhand, will not place block and do rotations only.");
	public static final ConfigBoolean PRINTER_CLEAR_FLUIDS = new ConfigBoolean("printerClearFluids", false, "It will try to place slime blocks at fluids anywhere to clear");
	public static final ConfigBoolean PRINTER_CLEAR_FLUIDS_USE_COBBLESTONE = new ConfigBoolean("printerClearFluidsUseCobblestone", false, "It will try to place Cobblestone at anywhere to clear");
	public static final ConfigBoolean PRINTER_CLEAR_SNOW_LAYER = new ConfigBoolean("printerClearSnowLayer", false, "It will try to place string when snow layer is found");
	public static final ConfigBoolean PRINTER_ACCURATE_BLOCK_PLACEMENT = new ConfigBoolean("printerAccurateBlockPlacement", false, "if carpet extra/quickcarpet enabled it, turn on");
	public static final ConfigBoolean PRINTER_PUMPKIN_PIE_FOR_COMPOSTER = new ConfigBoolean("printerUsePumpkinpieForComposter", false, "use pumpkin pie to adjust composter level");
	public static final ConfigBoolean PRINTER_OBSERVER_AVOID_ALL = new ConfigBoolean("printerObserverAvoidAll", true, "Observer will avoid all state update, can cause deadlock");
	public static final ConfigBoolean PRINTER_AVOID_CHECK_ONLY_PISTONS = new ConfigBoolean("printerAvoidCheckOnlyPistons", true, "QC order checks will ignore Dispenser QC state");
	public static final ConfigBoolean PRINTER_WATERLOGGED_WATER_FIRST = new ConfigBoolean("printerCheckWaterFirstForWaterlogged", true, "Watterlogged blocks won't be placed before water in place(except leaves in 1.19)");
	public static final ConfigBoolean PRINTER_SMART_REDSTONE_AVOID = new ConfigBoolean("printerSmartRedstoneAvoid", true, "Pistons / Observers will avoid and respect its order");
	public static final ConfigBoolean PRINTER_SUPPRESS_PUSH_LIMIT = new ConfigBoolean("printerSuppressPushLimitPistons", true, "Pistons that is suppressed with push limit won't be placed.");
	public static final ConfigBoolean PRINTER_SKIP_UNKNOWN_BLOCKSTATE = new ConfigBoolean("printerSkipsUnknownBlockstates", true, "Printer will skip some directional blocks");
	public static final ConfigBoolean ADVANCED_ACCURATE_BLOCK_PLACEMENT = new ConfigBoolean("CarpetExtraFixedVersion", false, "If carpet extra is updated, turn on to allow all facingblock rotation");
	public static final ConfigBoolean PRINTER_BEDROCK_BREAKING = new ConfigBoolean("printerBedrockBreaking", false, "Clear Bedrock mismatch with Bedrock Breaker");
	public static final ConfigBoolean PRINTER_BEDROCK_BREAKING_USE_SLIMEBLOCK = new ConfigBoolean("printerBedrockBreakingUseSlimeblock", false, "BecrockBreaker uses slime block to force torch location");
	public static final ConfigInteger PRINTER_BEDROCK_BREAKING_RANGE_SAFE = new ConfigInteger("bedrockBreakingCheckRange", 3, 0, 1024, "Safety distance between bedrock breakings");
	public static final ConfigInteger PRINTER_BEDROCK_DELAY = new ConfigInteger("bedrockBreakingClearTicks", 6, 0, 1024, "Waiting ticks after processing bedrock");
	public static final ConfigBoolean PRINTER_PLACE_ICE = new ConfigBoolean("printerUseIceForWater", false, "Should printer place ice where water/waterlogged should be?");
	public static final ConfigBoolean PRINTER_PLACE_MINECART = new ConfigBoolean("printerPlaceMinecart", true, "Should printer place minecart?(its smarter than average)");
	public static final ConfigBoolean PRINTER_FAKE_ROTATION = new ConfigBoolean("printerFakeRotation", true, "Printer will use fake rotations to place block correctly, at least in vanilla.");
	public static final ConfigInteger PRINTER_FAKE_ROTATION_DELAY = new ConfigInteger("printerFakeRotationTicks", 1, 0, 1000000, "Ticks between fake block packets");
	public static final ConfigInteger PRINTER_FAKE_ROTATION_LIMIT_PER_TICKS = new ConfigInteger("printerFakeRotationLimitPerTicks", 1, 1, 1000000, "Maximum fake placement per tick, prone to cause error(require:FakeRotationTick = 0)");
	public static final ConfigHotkey PRINTER_OFF_HOTKEY = new ConfigHotkey("printerToggleKey", "", "Printer will be toggled ON/OFF");
	public static final ConfigHotkey PRINTER_ALLOW_INVENTORY_OPERATIONS_HOTKEY = new ConfigHotkey("printerInventoryOperationsToggleKey", "", "Printer inventory operations will be toggled ON/OFF");
	public static final ConfigHotkey PRINTER_BREAK_BLOCKS_HOTKEY = new ConfigHotkey("printerBreakBlocksToggleKey", "", "Printer break blocks will be toggled ON/OFF");
	public static final ConfigHotkey PRINTER_CLEAR_FLUIDS_HOTKEY = new ConfigHotkey("printerClearFluidsToggleKey", "", "Printer clear fluids will be toggled ON/OFF");
	public static final ConfigHotkey PRINTER_CLEAR_SNOW_LAYER_HOTKEY = new ConfigHotkey("printerClearSnowLayerToggleKey", "", "Printer clear snow layer will be toggled ON/OFF");
	public static final ConfigHotkey PRINTER_CLEAR_FLUIDS_USE_COBBLESTONE_HOTKEY = new ConfigHotkey("printerClearFluidsUseCobblestoneToggleKey", "", "Printer clear fluids use cobblestone will be toggled ON/OFF");
	public static final ConfigHotkey PRINTER_BEDROCK_BREAKING_HOTKEY = new ConfigHotkey("printerBedrockBreakingToggleKey", "", "Printer bedrock breaking will be toggled ON/OFF");
	public static final ConfigBoolean PRINTER_AVOID_BLOCKING_BEACONS = new ConfigBoolean("printerAvoidBlockingBeacons", false, "Printer will avoid blocking beacons");

	public static final ConfigBoolean PRINTER_IGNORE_NBT = new ConfigBoolean("printerIgnoreNBT", false, "Printer will ignore NBT data when placing blocks");

	public static ImmutableList<ConfigHotkey> getHotkeyList() {
		ImmutableList.Builder<ConfigHotkey> hotkeyList = new ImmutableList.Builder<ConfigHotkey>().addAll(Hotkeys.HOTKEY_LIST);
		hotkeyList.addAll(List.of(
				PRINTER_OFF_HOTKEY,
				PRINTER_ALLOW_INVENTORY_OPERATIONS_HOTKEY,
				PRINTER_BREAK_BLOCKS_HOTKEY,
				PRINTER_CLEAR_FLUIDS_HOTKEY,
				PRINTER_CLEAR_SNOW_LAYER_HOTKEY,
				PRINTER_CLEAR_FLUIDS_USE_COBBLESTONE_HOTKEY,
				PRINTER_BEDROCK_BREAKING_HOTKEY)
		);
		return hotkeyList.build();
	}
	public static final ImmutableList<IConfigBase> betterList = originalList.addAll(List.of(
		VERIFY_INVENTORY,
		PRINTER_SHOULD_SWING_HAND,
		PRINTER_PRINT_DIRT_VARIANTS,
		USE_INVENTORY_CACHE,
		INVENTORY_CACHE_TICKS,
		PRINTER_OFF,
		PRINTER_ONLY_FAKE_ROTATION_MODE,
		PRINTER_FAKE_ROTATION_AGGRESSIVE,
		RENDER_ONLY_HOLDING_ITEMS,
		DISABLE_SYNC,
		DEBUG_MESSAGE,
		DEBUG_EXTRA_MESSAGE,
		DEBUG_ORDER_PLACEMENTS,
		DEBUG_PACKET_SYNC,
		PRINTER_SUPPRESS_PACKETS,
		DISABLE_SINGLEPLAYER_HANDLE,
		PRINTER_SLEEP_STACK_EMPTIED,
		EASY_PLACE_MODE_RANGE_X,
		EASY_PLACE_MODE_RANGE_Y,
		EASY_PLACE_MODE_RANGE_Z,
		EASY_PLACE_CACHE_TIME,
		PRINTER_MAX_BLOCKS,
		PRINTER_MAX_ITEM_CHANGES,
		PRINTER_BREAK_BLOCKS,
		PRINTER_BREAK_IGNORE_EXTRA,
		PRINTER_SKIP_UNKNOWN_BLOCKSTATE,
		EASY_PLACE_MODE_DELAY,
		EASY_PLACE_MODE_HOTBAR_ONLY,
		PRINTER_FLIPPINCACTUS,
		PRINTER_LIT_PORTAL_USE_FIRECHARGE,
		PRINTER_ALLOW_INVENTORY_OPERATIONS,
		PRINTER_INVENTORY_SCREEN_WAIT,
		PRINTER_INVENTORY_OPERATIONS_RETRY,
		PRINTER_INVENTORY_OPERATIONS_CLOSE_SCREEN,
		PRINTER_INVENTORY_OPERATION_ALLOW_ALL_NAMED,
		PRINTER_CLEAR_FLUIDS,
		PRINTER_PLACE_ICE,
		PRINTER_PLACE_MINECART,
		PRINTER_CLEAR_FLUIDS_USE_COBBLESTONE,
		PRINTER_CLEAR_SNOW_LAYER,
		PRINTER_ACCURATE_BLOCK_PLACEMENT,
		PRINTER_WATERLOGGED_WATER_FIRST,
		PRINTER_PUMPKIN_PIE_FOR_COMPOSTER,
		ADVANCED_ACCURATE_BLOCK_PLACEMENT,
		PRINTER_SMART_REDSTONE_AVOID,
		PRINTER_OBSERVER_AVOID_ALL,
		PRINTER_SUPPRESS_PUSH_LIMIT,
		PRINTER_AVOID_CHECK_ONLY_PISTONS,
		PRINTER_BEDROCK_BREAKING,
		PRINTER_BEDROCK_BREAKING_USE_SLIMEBLOCK,
		PRINTER_BEDROCK_BREAKING_RANGE_SAFE,
		PRINTER_BEDROCK_DELAY,
		PRINTER_FAKE_ROTATION,
		PRINTER_FAKE_ROTATION_DELAY,
		PRINTER_FAKE_ROTATION_LIMIT_PER_TICKS,
		PRINTER_IGNORE_NBT,
		PRINTER_AVOID_BLOCKING_BEACONS)
	).build();

	@Override
	public void onInitialize() {
		PRINTER_OFF_HOTKEY.getKeybind().setCallback(new KeyCallbackToggleBooleanConfigWithMessage(PRINTER_OFF));
		PRINTER_ALLOW_INVENTORY_OPERATIONS_HOTKEY.getKeybind().setCallback(new KeyCallbackToggleBooleanConfigWithMessage(PRINTER_ALLOW_INVENTORY_OPERATIONS));
		PRINTER_BREAK_BLOCKS_HOTKEY.getKeybind().setCallback(new KeyCallbackToggleBooleanConfigWithMessage(PRINTER_BREAK_BLOCKS));
		PRINTER_CLEAR_FLUIDS_HOTKEY.getKeybind().setCallback(new KeyCallbackToggleBooleanConfigWithMessage(PRINTER_CLEAR_FLUIDS));
		PRINTER_CLEAR_SNOW_LAYER_HOTKEY.getKeybind().setCallback(new KeyCallbackToggleBooleanConfigWithMessage(PRINTER_CLEAR_SNOW_LAYER));
		PRINTER_CLEAR_FLUIDS_USE_COBBLESTONE_HOTKEY.getKeybind().setCallback(new KeyCallbackToggleBooleanConfigWithMessage(PRINTER_CLEAR_FLUIDS_USE_COBBLESTONE));
		PRINTER_BEDROCK_BREAKING_HOTKEY.getKeybind().setCallback(new KeyCallbackToggleBooleanConfigWithMessage(PRINTER_BEDROCK_BREAKING));
		LoggerFactory.getLogger("Printer").info("YeeFuckinHaw");
	}
}

