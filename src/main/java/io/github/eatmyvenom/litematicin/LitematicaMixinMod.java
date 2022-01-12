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
	public static ImmutableList.Builder<IConfigBase> originalList = new ImmutableList.Builder<IConfigBase>().addAll(Configs.Generic.OPTIONS);
	public static final ConfigInteger EASY_PLACE_MODE_RANGE_X      	= new ConfigInteger("easyPlaceModeRangeX", 3, 0, 1024, "X Range for EasyPlace");
	public static final ConfigInteger EASY_PLACE_MODE_RANGE_Y      	= new ConfigInteger("easyPlaceModeRangeY", 3, 0, 1024, "Y Range for EasyPlace");
	public static final ConfigInteger EASY_PLACE_MODE_RANGE_Z      	= new ConfigInteger("easyPlaceModeRangeZ", 3, 0, 1024, "Z Range for EasyPlace");
	public static final ConfigInteger EASY_PLACE_MODE_MAX_BLOCKS   	= new ConfigInteger("easyPlaceModeMaxBlocks", 3, 1, 1000000, "Max block interactions per cycle");
	public static final ConfigBoolean EASY_PLACE_MODE_BREAK_BLOCKS 	= new ConfigBoolean("easyPlaceModeBreakBlocks", false, "Automatically breaks blocks.");
	public static final ConfigDouble  EASY_PLACE_MODE_DELAY		   	= new ConfigDouble( "easyPlaceModeDelay", 0.2, 0.0, 1.0, "Delay between printing blocks.\nDo not set to 0 if you are playing on a server.");
	public static final ConfigBoolean EASY_PLACE_MODE_HOTBAR_ONLY	= new ConfigBoolean("easyPlaceModeHotbarOnly", false, "Only place blocks from your hotbar.");
	public static final ConfigBoolean FLIPPIN_CACTUS	= new ConfigBoolean("easyPlaceModeFlippincactus", false, "If FlippinCactus is enabled and cactus is on mainhand, will not place block and do rotations only.");
	public static final ConfigBoolean CLEAR_AREA_MODE	= new ConfigBoolean("easyPlaceModeClearFluids", false, "It will try to place slime blocks at fluids anywhere to clear");
	public static final ConfigBoolean CLEAR_AREA_MODE_COBBLESTONE	= new ConfigBoolean("ClearFluidsUseCobblestone", false, "It will try to place Cobblestone at anywhere to clear");
	public static final ConfigBoolean CLEAR_AREA_MODE_SNOWPREVENT = new ConfigBoolean("ClearSnowLayer", false, "It will try to place string when snow layer is found");
	public static final ConfigBoolean ACCURATE_BLOCK_PLACEMENT = new ConfigBoolean("AccurateBlockPlacement",false,"if carpet extra/quickcarpet enabled it, turn on");
	public static final ConfigBoolean EASY_PLACE_MODE_USE_COMPOSTER = new  ConfigBoolean("easyPlaceModeUsePumpkinPie",false,"use punkin pie to adjust composter level");
	public static final ConfigBoolean EASY_PLACE_MODE_OBSERVER_EXPLICIT_ORDER = new  ConfigBoolean("easyPlaceModeObserverAvoidAll",false,"Observer will avoid all state update, can cause deadlock");
	public static final ConfigBoolean EASY_PLACE_MODE_REDSTONE_ORDERS = new  ConfigBoolean("easyPlaceModeSmartRedstoneAvoid",false,"Pistons / Observers will avoid and respect its order");
	public static final ConfigBoolean ADVANCED_ACCURATE_BLOCK_PLACEMENT = new  ConfigBoolean("CarpetExtraFixedVersion",false,"If carpet extra is updated, turn on to allow all facingblock rotation");
	public static final ConfigBoolean BEDROCK_BREAKING = new ConfigBoolean("BedrockBreaking", false, "Clear Bedrock mismatch with Bedrock Breaker");
	public static final ConfigBoolean BEDROCK_BREAKING_FORCE_TORCH = new ConfigBoolean("BedrockBreakingUseSlimeblock", false, "BecrockBreaker uses slime block to force torch location");
	public static final ImmutableList<IConfigBase> betterList = originalList.addAll(ImmutableList.of(
			EASY_PLACE_MODE_RANGE_X,
			EASY_PLACE_MODE_RANGE_Y,
			EASY_PLACE_MODE_RANGE_Z,
			EASY_PLACE_MODE_MAX_BLOCKS,
			EASY_PLACE_MODE_BREAK_BLOCKS,
			EASY_PLACE_MODE_DELAY,
			EASY_PLACE_MODE_HOTBAR_ONLY,
			FLIPPIN_CACTUS,
			CLEAR_AREA_MODE,
			CLEAR_AREA_MODE_COBBLESTONE,
			CLEAR_AREA_MODE_SNOWPREVENT,
			ACCURATE_BLOCK_PLACEMENT,
			EASY_PLACE_MODE_USE_COMPOSTER,
			ADVANCED_ACCURATE_BLOCK_PLACEMENT,
			EASY_PLACE_MODE_REDSTONE_ORDERS,
			EASY_PLACE_MODE_OBSERVER_EXPLICIT_ORDER,
			BEDROCK_BREAKING,
			BEDROCK_BREAKING_FORCE_TORCH)
	).build();
	@Override
	public void onInitialize() {
		System.out.println("YeeFuckinHaw");
	}
}

