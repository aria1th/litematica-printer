package io.github.eatmyvenom.litematicin;

import com.google.common.collect.ImmutableList;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigDouble;
import fi.dy.masa.malilib.config.options.ConfigInteger;
import net.fabricmc.api.ModInitializer;

public class LitematicaMixinMod implements ModInitializer {

	public static final ConfigInteger EASY_PLACE_MODE_RANGE_X      	= new ConfigInteger("easyPlaceModeRangeX", 3, 0, 8, "X Range for EasyPlace");
	public static final ConfigInteger EASY_PLACE_MODE_RANGE_Y      	= new ConfigInteger("easyPlaceModeRangeY", 3, 0, 8, "Y Range for EasyPlace");
	public static final ConfigInteger EASY_PLACE_MODE_RANGE_Z      	= new ConfigInteger("easyPlaceModeRangeZ", 3, 0, 8, "Z Range for EasyPlace");
	public static final ConfigInteger EASY_PLACE_MODE_MAX_BLOCKS   	= new ConfigInteger("easyPlaceModeMaxBlocks", 3, 1, 1000000, "Max block interactions per cycle");
	public static final ConfigBoolean EASY_PLACE_MODE_BREAK_BLOCKS 	= new ConfigBoolean("easyPlaceModeBreakBlocks", false, "Automatically breaks blocks.");
	public static final ConfigDouble  EASY_PLACE_MODE_DELAY		   	= new ConfigDouble( "easyPlaceModeDelay", 0.02, 0.0, 1.0, "Delay between printing blocks.\nDo not set to 0 if you are playing on a server.");
    public static final ConfigBoolean EASY_PLACE_MODE_HOTBAR_ONLY	= new ConfigBoolean("easyPlaceModeHotbarOnly", false, "Only place blocks from your hotbar.");
    public static final ConfigBoolean FLIPPIN_CACTUS	= new ConfigBoolean("easyPlaceModeFlippincactus", false, "If FlippinCactus is enabled and cactus is on mainhand, will not place block and do rotations only.");
    public static final ConfigBoolean CLEAR_AREA_MODE	= new ConfigBoolean("easyPlaceModeClearFluids", false, "It will try to place slime blocks at fluids anywhere to clear");
    public static final ConfigBoolean CLEAR_AREA_MODE_COBBLESTONE	= new ConfigBoolean("ClearFluidsUseCobblestone", false, "It will try to place Cobblestone at anywhere to clear");

	public static final ImmutableList<IConfigBase> betterList = ImmutableList.of(
			Configs.Generic.AREAS_PER_WORLD,
			//BETTER_RENDER_ORDER,
			Configs.Generic.CHANGE_SELECTED_CORNER,
			Configs.Generic.EASY_PLACE_MODE,
			Configs.Generic.EASY_PLACE_HOLD_ENABLED,
			Configs.Generic.EXECUTE_REQUIRE_TOOL,
			Configs.Generic.FIX_RAIL_ROTATION,
			Configs.Generic.LOAD_ENTIRE_SCHEMATICS,
			Configs.Generic.PASTE_IGNORE_INVENTORY,
			Configs.Generic.PICK_BLOCK_ENABLED,
			Configs.Generic.PLACEMENT_RESTRICTION,
			Configs.Generic.RENDER_MATERIALS_IN_GUI,
			Configs.Generic.RENDER_THREAD_NO_TIMEOUT,
			Configs.Generic.TOOL_ITEM_ENABLED,

			Configs.Generic.PASTE_REPLACE_BEHAVIOR,
			Configs.Generic.SELECTION_CORNERS_MODE,

			Configs.Generic.PASTE_COMMAND_INTERVAL,
			Configs.Generic.PASTE_COMMAND_LIMIT,
			Configs.Generic.PASTE_COMMAND_SETBLOCK,
			Configs.Generic.PICK_BLOCKABLE_SLOTS,
			Configs.Generic.TOOL_ITEM,

			EASY_PLACE_MODE_RANGE_X,
			EASY_PLACE_MODE_RANGE_Y,
			EASY_PLACE_MODE_RANGE_Z,
			EASY_PLACE_MODE_MAX_BLOCKS,
			EASY_PLACE_MODE_BREAK_BLOCKS,
			EASY_PLACE_MODE_DELAY,
			EASY_PLACE_MODE_HOTBAR_ONLY,
                                           FLIPPIN_CACTUS,
			CLEAR_AREA_MODE,
			CLEAR_AREA_MODE_COBBLESTONE

	);
	@Override
	public void onInitialize() {
		System.out.println("YeeFuckinHaw");
	}
}

