package io.github.eatmyvenom.litematicin.utils;

import com.google.common.collect.ImmutableMap;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.materials.MaterialCache;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager.PlacementPart;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement.RequiredEnabled;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.util.InventoryUtils;
import fi.dy.masa.litematica.util.*;
import fi.dy.masa.litematica.util.RayTraceUtils.RayTraceWrapper;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.malilib.util.IntBoundingBox;
import fi.dy.masa.malilib.util.LayerRange;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.enums.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.fluid.LavaFluid;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.UpdateSignC2SPacket;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
//#if MC>=11900
import net.minecraft.text.TextContent;
//#else
//$$ import net.minecraft.text.LiteralText;
//#endif
//#if MC<11902
//$$ import fi.dy.masa.malilib.util.SubChunkPos;
//#endif
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Pair;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.dimension.NetherPortal;

import java.util.*;
import java.util.function.Predicate;

import static io.github.eatmyvenom.litematicin.LitematicaMixinMod.*;
import static io.github.eatmyvenom.litematicin.utils.BedrockBreaker.interactBlock;
import static io.github.eatmyvenom.litematicin.utils.FakeAccurateBlockPlacement.getYaw;
import static io.github.eatmyvenom.litematicin.utils.InventoryUtils.*;

@SuppressWarnings("ConstantConditions")
public class Printer {

	private static final HashSet<Long> signCache = new HashSet<>();
	private static final LinkedHashMap<Pair<Long, Boolean>, PositionCache> positionCache = new LinkedHashMap<>();
	// For printing delay
	public static boolean isSleeping = false;
	public static long lastPlaced = new Date().getTime();
	private static boolean shouldSleepLonger = false;
	public static Breaker breaker = new Breaker();
	public static int worldBottomY = 0; // this is handled in MinecraftClientMixin.joinWorld callback
	public static int worldTopY = 256;
	private static final LinkedHashMap<Long, String> causeMap = new LinkedHashMap<>();
	private static final Long2LongOpenHashMap referenceSet = new Long2LongOpenHashMap();


	// TODO: This must be moved to another class and not be static.
	// Simulates and returns if player can place block as wanted.
	private static boolean simulateFacingData(BlockState state, BlockPos blockPos, Vec3d hitVec) {
		if (!state.getProperties().contains(Properties.FACING) && !state.getProperties().contains(Properties.HORIZONTAL_FACING)) {
			return true;
		}
		//blocks that does not require facing
		if (state.isOf(Blocks.HOPPER) || state.isIn(BlockTags.SHULKER_BOXES)) {
			return true;
		}
		// int 0 : none, 1 : clockwise, 2 : counterclockwise, 3 : reverse
		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		BlockHitResult hitResult = new BlockHitResult(hitVec, Direction.NORTH, blockPos, false);
		Block block = state.getBlock();
		ItemPlacementContext ctx = new ItemPlacementContext(player, Hand.MAIN_HAND, state.getBlock().asItem().getDefaultStack(), hitResult);
		BlockState testState;
		try {
			testState = block.getPlacementState(ctx);
		} catch (Exception e) { //doors wtf
			MessageHolder.sendMessageUncheckedUnique("Cannot get tested orientation of given block "+ state.getBlock().getName());
			//fallback to player horizontal facing...
			return player.getHorizontalFacing() == fi.dy.masa.malilib.util.BlockUtils.getFirstPropertyFacingValue(state);
		}
		if (testState == null) {
			MessageHolder.sendMessageUncheckedUnique("Cannot get tested orientation of given block "+ state.getBlock().getName());
			return player.getHorizontalFacing() == fi.dy.masa.malilib.util.BlockUtils.getFirstPropertyFacingValue(state);
		}
		Direction testFacing = fi.dy.masa.malilib.util.BlockUtils.getFirstPropertyFacingValue(testState);
		return testFacing == fi.dy.masa.malilib.util.BlockUtils.getFirstPropertyFacingValue(state);
	}

	public static boolean canPickBlock(MinecraftClient mc, BlockState preference, BlockPos pos) {
		World world = SchematicWorldHandler.getSchematicWorld();
		ItemStack stack = getStackForState(mc, preference, world, pos);
		if (stack.isEmpty()) {
			return false;
		}
		if (!stack.isEmpty() && stack.getItem() != Items.AIR) {
			PlayerInventory inv = getInventory(mc.player);
			if (!isCreative(mc.player)) {
				int slot = inv.getSlotWithStack(stack);
				if (slot == -1) {
					return false;
				}
				if (EASY_PLACE_MODE_HOTBAR_ONLY.getBooleanValue()) {
					return slot < 9;
				}
			}
			return true;
		}
		return true;
	}

	public static boolean canPickItem(MinecraftClient mc, ItemStack stack) {
		if (!stack.isEmpty()) {
			PlayerInventory inv = getInventory(mc.player);
			if (!isCreative(mc.player)) {
				int slot = inv.getSlotWithStack(stack);
				if (slot == -1) {
					return false;
				}
				if (EASY_PLACE_MODE_HOTBAR_ONLY.getBooleanValue()) {
					return slot < 9;
				}
			}
			return true;
		}
		return false;
	}

	/**
	 * New doSchematicWorldPickBlock that allows you to choose which block you want
	 */
	@Environment(EnvType.CLIENT)
	synchronized public static boolean doSchematicWorldPickBlock(MinecraftClient mc, BlockState preference,
	                                                             BlockPos pos) {
		World world = SchematicWorldHandler.getSchematicWorld();
		ItemStack stack = getStackForState(mc, preference, world, pos);
		if (!FakeAccurateBlockPlacement.canHandleOther(stack.getItem())) {
			return false;
		}
		if (!stack.isEmpty()) {
			if (USE_INVENTORY_CACHE.getBooleanValue()) {
				return io.github.eatmyvenom.litematicin.utils.InventoryUtils.swapToItem(mc, stack);
			} else {
				InventoryUtils.schematicWorldPickBlock(stack, pos, world, mc);
				return mc.player.getMainHandStack().isItemEqual(stack);
			}
		}
		return false;
	}

	@Environment(EnvType.CLIENT)
	synchronized public static boolean doSchematicWorldPickBlock(MinecraftClient mc, ItemStack stack) {
		if (!FakeAccurateBlockPlacement.canHandleOther(stack.getItem())) {
			return false;
		}
		if (!stack.isEmpty()) {
			if (USE_INVENTORY_CACHE.getBooleanValue()) {
				return io.github.eatmyvenom.litematicin.utils.InventoryUtils.swapToItem(mc, stack);
			} else {
				fi.dy.masa.malilib.util.InventoryUtils.swapItemToMainHand(stack, mc);
				return mc.player.getMainHandStack().isItemEqual(stack);
			}
		}
		return false;
	}

	@Environment(EnvType.CLIENT)
	synchronized public static boolean doSchematicWorldPickBlock(MinecraftClient mc, Predicate<ItemStack> stack) {
		ItemStack stack1 = io.github.eatmyvenom.litematicin.utils.InventoryUtils.findItem(mc, stack);
		return doSchematicWorldPickBlock(mc, stack1);
	}

	public static ActionResult doEasyPlaceFakeRotation(MinecraftClient mc) { //force normal easyplace action, ignore condition checks
		if (FakeAccurateBlockPlacement.isHandling()){
			MessageHolder.sendDebugMessage(mc.player, "Passed because already handling something");
			return ActionResult.PASS;
		}
		RayTraceWrapper traceWrapper = RayTraceUtils.getGenericTrace(mc.world, mc.player, 6);
		FakeAccurateBlockPlacement.requestedTicks = Math.max(-2, FakeAccurateBlockPlacement.requestedTicks);
		if (traceWrapper == null) {
			return ActionResult.PASS;
		}
		BlockHitResult trace = traceWrapper.getBlockHitResult();
		if (trace == null) {
			return ActionResult.PASS;
		}
		World world = SchematicWorldHandler.getSchematicWorld();
		World clientWorld = mc.world;
		BlockPos blockPos = trace.getBlockPos();
		if (isPositionCached(blockPos, false)){
			MessageHolder.sendDebugMessage(mc.player, "Passed because position "+ blockPos.toShortString() + " is cached");
			return ActionResult.PASS;
		}
		BlockState schematicState = world.getBlockState(blockPos);
		BlockState clientState = clientWorld.getBlockState(blockPos);
		if (schematicState.getBlock().getName().equals(clientState.getBlock().getName()) || schematicState.isAir()) {
			MessageHolder.sendDebugMessage(mc.player, "Passed because position "+ blockPos.toShortString() + " is satisfied");
			return ActionResult.FAIL;
		}
		if (FakeAccurateBlockPlacement.canHandleOther(schematicState.getBlock().asItem()) && canPickBlock(mc, schematicState, blockPos)) {
			MessageHolder.sendOrderMessage("Requested " + schematicState + " at " +blockPos.toShortString());
			FakeAccurateBlockPlacement.request(schematicState, blockPos);
			return ActionResult.SUCCESS;
		}
		MessageHolder.sendDebugMessage(mc.player, "Passed because position "+ blockPos.toShortString() + " cannot pick block or cannot handle other, handling "+ FakeAccurateBlockPlacement.currentHandling);
		return ActionResult.FAIL;
	}
	public static ActionResult doEasyPlaceNormally(MinecraftClient mc) { //force normal easyplace action, ignore condition checks
		RayTraceWrapper traceWrapper = RayTraceUtils.getGenericTrace(mc.world, mc.player, 6);
		if (traceWrapper == null) {
			return ActionResult.PASS;
		}
		BlockHitResult trace = traceWrapper.getBlockHitResult();
		if (trace == null) {
			return ActionResult.PASS;
		}
		World world = SchematicWorldHandler.getSchematicWorld();
		World clientWorld = mc.world;
		BlockPos blockPos = trace.getBlockPos();
		BlockState schematicState = world.getBlockState(blockPos);
		BlockState clientState = clientWorld.getBlockState(blockPos);
		if (schematicState.getBlock().getName().equals(clientState.getBlock().getName())) {
			return ActionResult.FAIL;
		}
		ItemStack stack = MaterialCache.getInstance().getRequiredBuildItemForState(schematicState);
		if (!stack.isEmpty()) {
			InventoryUtils.schematicWorldPickBlock(stack, blockPos, world, mc);
			Hand hand = EntityUtils.getUsedHandForItem(mc.player, stack);
			if (hand == null) {
				return ActionResult.FAIL;
			}
			Vec3d hitPos;
			Direction sideOrig = trace.getSide();
			Direction side = applyPlacementFacing(schematicState, sideOrig, clientState);
			if (PRINTER_ACCURATE_BLOCK_PLACEMENT.getBooleanValue()) {
				hitPos = applyCarpetProtocolHitVec(blockPos, schematicState);
			} else {
				hitPos = applyHitVec(blockPos, schematicState, side);
			}
			BlockHitResult hitResult = new BlockHitResult(hitPos, side, blockPos, false);
			boolean canContinue;
			if (!PRINTER_FAKE_ROTATION.getBooleanValue() || PRINTER_ACCURATE_BLOCK_PLACEMENT.getBooleanValue()) { //Accurateblockplacement, or vanilla but no fake
				canContinue = interactBlock(mc, hitResult).isAccepted(); //PLACE block
				cacheEasyPlacePosition(blockPos, false);
			} else {
				canContinue = FakeAccurateBlockPlacement.request(schematicState, blockPos);
			}
			if (canContinue) {
				return ActionResult.SUCCESS;
			} else {
				return ActionResult.FAIL;
			}
		}
		return ActionResult.FAIL;
	}

	private static void recordCause(BlockPos pos, String reason, BlockPos reasonPos) {
		if (!DEBUG_MESSAGE.getBooleanValue()) {
			return;
		}
		if (reasonPos != null) {
			if (pos.asLong() == reasonPos.asLong()) {
				causeMap.put(pos.asLong(), "self registered+\n");
				//throw new AssertionError("Position should not equal to reason position!");
			}
			referenceSet.put(pos.asLong(), reasonPos.asLong());
		}
		causeMap.put(pos.asLong(), reason + '\n');
	}

	private static void recordCause(BlockPos pos, String reason) {
		recordCause(pos, reason, null);
	}

	private static String getReason(Long pos) {
		return "<" + internalGetReason(pos, null, 0) + ">";
	}

	private static String internalGetReason(Long pos, LongOpenHashSet set, int count) {
		if (count > 10) {
			return BlockPos.fromLong(pos).toShortString() + "RECURSIVE_COUNT_EXCEED";
		}
		if (set == null) {
			set = new LongOpenHashSet();
		}
		if (set.contains((long) pos)) {
			return BlockPos.fromLong(pos).toShortString() + "Recursive ";
		}
		if (referenceSet.containsKey((long) pos)) {
			set.add((long) pos);
			return causeMap.getOrDefault(pos, BlockPos.fromLong(pos).toShortString() + " : Not registered") + " " + internalGetReason(referenceSet.get((long) pos), set, count + 1);
		}
		return causeMap.getOrDefault(pos, BlockPos.fromLong(pos).toShortString() + " : Not registered");
	}

	private static boolean isPositionWithinBox(Box box, BlockPos pos) {
		if (box == null) {
			return true;
		}
		BlockPos start = box.getPos1();
		BlockPos end = box.getPos2();
		BlockPos ref1 = new BlockPos(Math.min(start.getX(), end.getX()), Math.min(start.getY(), end.getY()), Math.min(start.getZ(), end.getZ()));
		BlockPos ref2 = new BlockPos(Math.max(start.getX(), end.getX()), Math.max(start.getY(), end.getY()), Math.max(start.getZ(), end.getZ()));
		return (ref1.getX() <= pos.getX() && pos.getX() <= ref2.getX() && ref1.getY() <= pos.getY() && pos.getY() <= ref2.getY() && ref1.getZ() <= pos.getZ() && pos.getZ() <= ref2.getZ());
	}

	@Environment(EnvType.CLIENT)
	synchronized public static ActionResult doPrinterAction(MinecraftClient mc) {
		io.github.eatmyvenom.litematicin.utils.InventoryUtils.itemChangeCount = 0;
		if (!DEBUG_MESSAGE.getBooleanValue()) {
			causeMap.clear(); //reduce ram usage
		}
		if (!PRINTER_BEDROCK_BREAKING.getBooleanValue()) {
			BedrockBreaker.clear();
		}
		FakeAccurateBlockPlacement.requestedTicks = Math.max(-2, FakeAccurateBlockPlacement.requestedTicks);
		if (breaker.isBreakingBlock()) {
			mc.player.sendMessage(Text.of("Handling breakBlock!"), true);
			return ActionResult.SUCCESS;
		}
		if (PRINTER_ALLOW_INVENTORY_OPERATIONS.getBooleanValue()) {
			ItemInputs.execute(mc);
			mc.player.sendMessage(Text.of("Handling inventory operation!"), true);
			return ActionResult.PASS;
		} else {
			ItemInputs.clear();
		}
		if (new Date().getTime() < lastPlaced + 1000.0 * EASY_PLACE_MODE_DELAY.getDoubleValue()) {
			mc.player.sendMessage(Text.of("Handling delay"), true);
			return ActionResult.PASS;
		} else {
			isSleeping = false;
		}
		boolean isCreative = isCreative(mc.player);
		BlockPos tracePos = mc.player.getBlockPos();
		int posX = tracePos.getX();
		int posY = tracePos.getY();
		int posZ = tracePos.getZ();

		RayTraceWrapper traceWrapper = RayTraceUtils.getGenericTrace(mc.world, mc.player, 6);
		//RayTraceWrapper traceWrapper = RayTraceUtils.getGenericTrace(mc.world, mc.player, 6, true); previous litematica code
		if (traceWrapper != null) {
			BlockHitResult trace = traceWrapper.getBlockHitResult();
			tracePos = trace.getBlockPos();
			posX = tracePos.getX();
			posY = tracePos.getY();
			posZ = tracePos.getZ();
		}

		boolean ClearArea = PRINTER_CLEAR_FLUIDS.getBooleanValue(); // if it's true, will ignore everything and remove fluids.
		boolean UseCobble = PRINTER_CLEAR_FLUIDS_USE_COBBLESTONE.getBooleanValue() && ClearArea;
		boolean ClearSnow = PRINTER_CLEAR_SNOW_LAYER.getBooleanValue() && ClearArea;
		boolean CanUseProtocol = PRINTER_ACCURATE_BLOCK_PLACEMENT.getBooleanValue();
		boolean FillInventory = PRINTER_PUMPKIN_PIE_FOR_COMPOSTER.getBooleanValue();
		ItemStack composableItem = Items.PUMPKIN_PIE.getDefaultStack();
		//#if MC>=11902
		List<PlacementPart> allPlacementsTouchingSubChunk = DataManager.getSchematicPlacementManager().getAllPlacementsTouchingChunk(tracePos);
		//#else
		//$$ List<PlacementPart> allPlacementsTouchingSubChunk = DataManager.getSchematicPlacementManager().getAllPlacementsTouchingSubChunk(new SubChunkPos(tracePos));
		//#endif
		Box selectedBox = null;
		if (allPlacementsTouchingSubChunk.isEmpty() && !ClearArea) {
			if (PRINTER_BEDROCK_BREAKING.getBooleanValue()) {
				BedrockBreaker.scheduledTickHandler(mc, null);
			}
			return ActionResult.PASS;
		}
		int maxX = 0;
		int maxY = 0;
		int maxZ = 0;
		int minX = 0;
		int minY = 0;
		int minZ = 0;
		int rangeX = EASY_PLACE_MODE_RANGE_X.getIntegerValue();
		int rangeY = EASY_PLACE_MODE_RANGE_Y.getIntegerValue();
		int rangeZ = EASY_PLACE_MODE_RANGE_Z.getIntegerValue();
		if (rangeX == 0 && rangeY == 0 && rangeZ == 0 && traceWrapper != null) {
			return doEasyPlaceNormally(mc);
		}
		boolean foundBox = false;
		if (ClearArea) {
			foundBox = true;
			maxX = posX + rangeX;
			maxY = posY + rangeY;
			maxZ = posZ + rangeZ;
			minX = posX - rangeX;
			minY = posY - rangeY;
			minZ = posZ - rangeZ;
		} else {
			for (PlacementPart part : allPlacementsTouchingSubChunk) {
				IntBoundingBox pbox = part.getBox();
				if (pbox.containsPos(tracePos)) {

					ImmutableMap<String, Box> boxes = part.getPlacement()
						.getSubRegionBoxes(RequiredEnabled.PLACEMENT_ENABLED);

					for (Box box : boxes.values()) {

						final int boxXMin = Math.min(box.getPos1().getX(), box.getPos2().getX());
						final int boxYMin = Math.min(box.getPos1().getY(), box.getPos2().getY());
						final int boxZMin = Math.min(box.getPos1().getZ(), box.getPos2().getZ());
						final int boxXMax = Math.max(box.getPos1().getX(), box.getPos2().getX());
						final int boxYMax = Math.max(box.getPos1().getY(), box.getPos2().getY());
						final int boxZMax = Math.max(box.getPos1().getZ(), box.getPos2().getZ());
						if (posX < boxXMin || posX > boxXMax || posY < boxYMin || posY > boxYMax || posZ < boxZMin
							|| posZ > boxZMax) {
							continue;
						}
						minX = boxXMin;
						maxX = boxXMax;
						minY = boxYMin;
						maxY = boxYMax;
						minZ = boxZMin;
						maxZ = boxZMax;
						foundBox = true;
						selectedBox = box;
						break;
					}

					break;
				}
			}
		}

		if (!foundBox) {
			if (PRINTER_BEDROCK_BREAKING.getBooleanValue()) {
				BedrockBreaker.scheduledTickHandler(mc, null);
			}
			return ActionResult.PASS;
		}
		LayerRange range = DataManager.getRenderLayerRange(); //add range following
		int MaxReach = Math.max(Math.max(rangeX, rangeY), rangeZ);
		boolean breakBlocks = PRINTER_BREAK_BLOCKS.getBooleanValue();
		boolean ExplicitObserver = PRINTER_OBSERVER_AVOID_ALL.getBooleanValue();
		boolean flipBlocks = mc.player.getMainHandStack().getItem().equals(Items.CACTUS) && PRINTER_FLIPPINCACTUS.getBooleanValue();; //if true, will flip blocks with cactus
		boolean smartRedstone = PRINTER_SMART_REDSTONE_AVOID.getBooleanValue();
		Direction[] facingSides = Direction.getEntityFacingOrder(mc.player);
		Direction primaryFacing = facingSides[0];
		Direction horizontalFacing = primaryFacing; // For use in blocks with only horizontal rotation

		int index = 0;
		while (horizontalFacing.getAxis() == Direction.Axis.Y && index < facingSides.length) {
			horizontalFacing = facingSides[index++];
		}

		World world = SchematicWorldHandler.getSchematicWorld();

		/*
		 * TODO: THIS IS REALLY BAD IN TERMS OF EFFICIENCY. I suggest using some form of
		 * search with a built in datastructure first Maybe quadtree? (I dont know how
		 * MC works)
		 */

		int maxInteract = PRINTER_MAX_BLOCKS.getIntegerValue();
		int interact = 0;

		int fromX = Math.max(posX - rangeX, minX);
		int fromY = Math.max(posY - rangeY, minY);
		int fromZ = Math.max(posZ - rangeZ, minZ);

		int toX = Math.min(posX + rangeX, maxX);
		int toY = Math.min(posY + rangeY, maxY);
		int toZ = Math.min(posZ + rangeZ, maxZ);

		toY = Math.max(Math.min(toY, worldTopY), worldBottomY);
		fromY = Math.max(Math.min(fromY, worldTopY), worldBottomY);

		fromX = Math.max(fromX, (int) mc.player.getX() - rangeX);
		fromY = Math.max(fromY, (int) mc.player.getY() - rangeY);
		fromZ = Math.max(fromZ, (int) mc.player.getZ() - rangeZ);

		toX = Math.min(toX, (int) mc.player.getX() + rangeX);
		toY = Math.min(toY, (int) mc.player.getY() + rangeY);
		toZ = Math.min(toZ, (int) mc.player.getZ() + rangeZ);
		for (int y = fromY; y <= toY; y++) {
			for (int x = fromX; x <= toX; x++) {
				for (int z = fromZ; z <= toZ; z++) {
					if (interact >= maxInteract) {
						if (shouldSleepLonger) {
							shouldSleepLonger = false;
							lastPlaced = Math.max(lastPlaced, new Date().getTime() + PRINTER_SLEEP_STACK_EMPTIED.getIntegerValue());
						} else {
							lastPlaced = Math.max(lastPlaced, new Date().getTime());
						}
						return ActionResult.SUCCESS;
					}
					if (FakeAccurateBlockPlacement.emptyWaitingQueue()) {
						interact++;
					}
					if (FakeAccurateBlockPlacement.shouldReturnValue) {
						FakeAccurateBlockPlacement.shouldReturnValue = false;
						return ActionResult.SUCCESS;
					}
					double dx = mc.player.getX() - x - 0.5;
					double dy = mc.player.getY() - y - 0.5;
					double dz = mc.player.getZ() - z - 0.5;

					if (dx * dx + dy * dy + dz * dz > MaxReach * MaxReach) {
						continue;
					}

					BlockPos pos = new BlockPos(x, y, z);
					if (PRINTER_ALLOW_INVENTORY_OPERATIONS.getBooleanValue() && io.github.eatmyvenom.litematicin.utils.InventoryUtils.hasItemInSchematic(world, pos)) {
						MessageHolder.sendUniqueMessageAlways("Inventory in " + pos.toCenterPos().toString() + " has Item inside!");
					}
					BlockState stateSchematic;
					BlockState stateClient;
					updateSignText(mc, world, pos);
					if (!breakBlocks && !ClearArea && !flipBlocks && !PRINTER_BEDROCK_BREAKING.getBooleanValue()) {
						if (world.isAir(pos)) {
							continue;
						} else {
							if (world.getBlockState(pos) == mc.world.getBlockState(pos)) {
								continue;
							}
						}
					}
					if (breakBlocks) {
						if (PRINTER_BREAK_IGNORE_EXTRA.getBooleanValue() && world.isAir(pos)) {
							continue;
						}
					}
					stateSchematic = world.getBlockState(pos);
					stateClient = mc.world.getBlockState(pos);
					if (!ClearArea) {
						if (!range.isPositionWithinRange(pos)) {
							continue;
						}
						if (breakBlocks && stateSchematic != null && !(stateClient.getBlock() instanceof SnowBlock) &&
							!stateClient.isAir() &&
							!(stateClient.isOf(Blocks.WATER) || stateClient.isOf(Blocks.LAVA) || stateClient.isOf(Blocks.BUBBLE_COLUMN)) &&
							!stateClient.isOf(Blocks.PISTON_HEAD) && !stateClient.isOf(Blocks.MOVING_PISTON)) {
							if (!stateClient.getBlock().getName().equals(stateSchematic.getBlock().getName()) ||
								(stateClient.getBlock() instanceof SlabBlock && stateSchematic.getBlock() instanceof SlabBlock && stateClient.get(SlabBlock.TYPE) != stateSchematic.get(SlabBlock.TYPE))
									&& dx * dx + Math.pow(dy + 1.5, 2) + dz * dz <= MaxReach * MaxReach) {

								if (isCreative(mc.player)) {
									mc.interactionManager.attackBlock(pos, Direction.DOWN);
									interact++;

									if (interact >= maxInteract) {
										if (shouldSleepLonger) {
											shouldSleepLonger = false;
											lastPlaced = Math.max(lastPlaced, new Date().getTime() + PRINTER_SLEEP_STACK_EMPTIED.getIntegerValue());
										} else {
											lastPlaced = Math.max(lastPlaced, new Date().getTime());
										}
										return ActionResult.SUCCESS;
									}
								} else if (BedrockBreaker.isBlockNotInstantBreakable(stateClient.getBlock()) && PRINTER_BEDROCK_BREAKING.getBooleanValue()) {
									mc.player.sendMessage(Text.of("Handling printerBedrockBreaking!"), true);
									interact += BedrockBreaker.scheduledTickHandler(mc, pos);
									continue;
								} else if (PRINTER_BEDROCK_BREAKING.getBooleanValue()) {
									mc.player.sendMessage(Text.of("Handling printerBedrockBreaking!"), true);
									interact += BedrockBreaker.scheduledTickHandler(mc, null);
									continue;
								} else if (!positionStorage.hasPos(pos)) { // For survival
									boolean replaceable = mc.world.getBlockState(pos).getMaterial().isReplaceable();
									if (!replaceable && mc.world.getBlockState(pos).getHardness(world, pos) == -1) {
										continue;
									}
									if (replaceable || mc.world.getBlockState(pos).getHardness(world, pos) == 0) {
										mc.interactionManager.attackBlock(pos, Direction.DOWN);
										return ActionResult.SUCCESS;
									}
									if (!replaceable) {
										if (breaker.startBreakingBlock(pos, mc)) {
											return ActionResult.SUCCESS;
										}
									} // it needs to avoid unbreakable blocks and just added and lava, but it's not block so somehow made it work
									continue;
								}
							}
						}
						// Abort if there is already a block in the target position
						if (flipBlocks || requiresMoreAction(stateSchematic, stateClient)) {
							/*
							 * Sometimes, blocks have other states like the delay on a repeater. So, this
							 * code clicks the block until the state is the same I don't know if Schematica
							 * does this too, I just did it because I work with a lot of redstone
							 */
							if (!flipBlocks && !stateClient.isAir() && !mc.player.isSneaking() && !isPositionCached(pos, true)) {
								Block cBlock = stateClient.getBlock();
								Block sBlock = stateSchematic.getBlock();
								// Blocks are equal, but have different states
								if (cBlock.getName().equals(sBlock.getName())) {
									Direction facingSchematic = fi.dy.masa.malilib.util.BlockUtils
										.getFirstPropertyFacingValue(stateSchematic);
									Direction facingClient = fi.dy.masa.malilib.util.BlockUtils
										.getFirstPropertyFacingValue(stateClient);

									if (facingSchematic == facingClient) {
										int clickTimes = 0;
										Direction side = Direction.NORTH;
										if (sBlock instanceof RepeaterBlock && !PRINTER_ACCURATE_BLOCK_PLACEMENT.getBooleanValue()) {
											int clientDelay = stateClient.get(RepeaterBlock.DELAY);
											int schematicDelay = stateSchematic.get(RepeaterBlock.DELAY);
											if (clientDelay != schematicDelay) {

												if (clientDelay < schematicDelay) {
													clickTimes = schematicDelay - clientDelay;
												} else if (clientDelay > schematicDelay) {
													clickTimes = schematicDelay + (4 - clientDelay);
												}
											}
											side = Direction.UP;
										} else if (sBlock instanceof ComparatorBlock && !PRINTER_ACCURATE_BLOCK_PLACEMENT.getBooleanValue()) {
											if (stateSchematic.get(ComparatorBlock.MODE) != stateClient
												.get(ComparatorBlock.MODE)) {
												clickTimes = 1;
											}
											side = Direction.UP;
										} else if (sBlock instanceof LeverBlock) {
											if (stateSchematic.get(LeverBlock.POWERED) != stateClient
												.get(LeverBlock.POWERED)) {
												clickTimes = 1;
											}

											/*
											 * I don't know if this direction code is needed. I am just doing it anyway to
											 * make it "make sense" to the server (I am emulating what the client does so
											 * the server isn't confused)
											 */
											if (stateClient.get(LeverBlock.FACE) == WallMountLocation.CEILING) {
												side = Direction.DOWN;
											} else if (stateClient.get(LeverBlock.FACE) == WallMountLocation.FLOOR) {
												side = Direction.UP;
											} else {
												side = stateClient.get(LeverBlock.FACING);
											}

										} else if (sBlock instanceof TrapdoorBlock) {
											if (stateSchematic.getMaterial() != Material.METAL && stateSchematic
												.get(TrapdoorBlock.OPEN) != stateClient.get(TrapdoorBlock.OPEN)) {
												clickTimes = 1;
											}
										} else if (sBlock instanceof FenceGateBlock) {
											if (stateSchematic.get(FenceGateBlock.OPEN) != stateClient
												.get(FenceGateBlock.OPEN)) {
												clickTimes = 1;
											}
										} else if (sBlock instanceof DoorBlock) {
											if (stateClient.getMaterial() != Material.METAL && stateSchematic
												.get(DoorBlock.OPEN) != stateClient.get(DoorBlock.OPEN)) {
												clickTimes = 1;
											}
										} else if (sBlock instanceof NoteBlock) {
											int note = stateClient.get(NoteBlock.NOTE);
											int targetNote = stateSchematic.get(NoteBlock.NOTE);
											if (note != targetNote) {
												if (note < targetNote) {
													clickTimes = targetNote - note;
												} else if (note > targetNote) {
													clickTimes = targetNote + (25 - note);
												}
											}
										} else if (sBlock instanceof ComposterBlock && FillInventory) {
											if (!FakeAccurateBlockPlacement.canHandleOther(composableItem.getItem())) {
												continue;
											}
											int level = stateClient.get(ComposterBlock.LEVEL);
											int Schematiclevel = stateSchematic.get(ComposterBlock.LEVEL);
											if (level != Schematiclevel && !(level == 7 && Schematiclevel == 8)) {
												if (doSchematicWorldPickBlock(mc, composableItem)) {
													Vec3d hitPos = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
													BlockHitResult hitResult = new BlockHitResult(hitPos, side, pos, false);
													interactBlock(mc, hitResult); //COMPOSTER
													io.github.eatmyvenom.litematicin.utils.InventoryUtils.decrementCount(isCreative);
													cacheEasyPlacePosition(pos, false);
													if (shouldSleepLonger) {
														shouldSleepLonger = false;
														lastPlaced = Math.max(lastPlaced, new Date().getTime() + 200 + PRINTER_SLEEP_STACK_EMPTIED.getIntegerValue());
													} else {
														lastPlaced = Math.max(lastPlaced, new Date().getTime() + 200);
													}
													return ActionResult.SUCCESS;
												}
											} else {
												cacheEasyPlacePosition(pos, true);
											}
										} else if (!isPositionCached(pos, false) && PRINTER_PLACE_MINECART.getBooleanValue() && sBlock instanceof DetectorRailBlock && cBlock instanceof DetectorRailBlock) {
											if (!shouldAvoidPlaceCart(pos, world) && placeCart(stateSchematic, mc, pos)) {
												continue;
											}
										}
										for (int i = 0; i < clickTimes; i++) // Click on the block a few times
										{
											Vec3d hitPos = Vec3d.ofCenter(pos);

											BlockHitResult hitResult = new BlockHitResult(hitPos, side, pos, false);

											interactBlock(mc, hitResult); //NOTEBLOCK, REPEATER...
											interact++;
										}

										if (clickTimes > 0) {
											cacheEasyPlacePosition(pos, true, 3600);
										}

									} //can place vanilla
								}
								// Blocks are not equal, but can be converted. example: dirt -> dirt path
								if (stateClient.isOf(Blocks.DIRT)) {
									if (stateSchematic.isOf(Blocks.DIRT_PATH) && PRINTER_PRINT_DIRT_VARIANTS.getBooleanValue() && io.github.eatmyvenom.litematicin.utils.InventoryUtils.canSwap(mc.player, item -> item.getItem() instanceof ShovelItem)) {
										if (doSchematicWorldPickBlock(mc, stack -> stack.getItem() instanceof ShovelItem)){
											Vec3d hitPos = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
											BlockHitResult hitResult = new BlockHitResult(hitPos, Direction.UP, pos, false);
											interactBlock(mc, hitResult);
										}
									}
									// farmland
									else if (stateSchematic.isOf(Blocks.FARMLAND) && PRINTER_PRINT_DIRT_VARIANTS.getBooleanValue() && io.github.eatmyvenom.litematicin.utils.InventoryUtils.canSwap(mc.player, item -> item.getItem() instanceof HoeItem)) {
										if (doSchematicWorldPickBlock(mc, stack -> stack.getItem() instanceof HoeItem)){
											Vec3d hitPos = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
											BlockHitResult hitResult = new BlockHitResult(hitPos, Direction.UP, pos, false);
											interactBlock(mc, hitResult);
										}
									}
								}
							} else if (!ClearArea && flipBlocks) {
								// Flip the block
								Block cBlock = stateClient.getBlock();
								Block sBlock = stateSchematic.getBlock();
								if (cBlock.getName().equals(sBlock.getName())) {
									boolean ShapeBoolean = false;
									boolean ShouldFix = false;
									if (sBlock instanceof AbstractRailBlock) {
										if (sBlock instanceof RailBlock) {
											String SchematicRailShape = stateSchematic.get(RailBlock.SHAPE).toString();
											String ClientRailShape = stateClient.get(RailBlock.SHAPE).toString();
											ShouldFix = !Objects.equals(SchematicRailShape, ClientRailShape);
											ShapeBoolean = !Objects.equals(SchematicRailShape, ClientRailShape) && ((Objects.equals(SchematicRailShape, "south_west") || Objects.equals(SchematicRailShape, "north_west") ||
												Objects.equals(SchematicRailShape, "south_east") || Objects.equals(SchematicRailShape, "north_east")) && (Objects.equals(ClientRailShape, "south_west") ||
												Objects.equals(ClientRailShape, "north_west") || Objects.equals(ClientRailShape, "south_east") || Objects.equals(ClientRailShape, "north_east")) ||
												(Objects.equals(SchematicRailShape, "east_west") || Objects.equals(SchematicRailShape, "north_south")) && (Objects.equals(ClientRailShape, "east_west") || Objects.equals(ClientRailShape, "north_south")));
										} else {
											String SchematicRailShape = stateSchematic.get(PoweredRailBlock.SHAPE).toString();
											String ClientRailShape = stateClient.get(PoweredRailBlock.SHAPE).toString();
											ShouldFix = !Objects.equals(SchematicRailShape, ClientRailShape);
											ShapeBoolean = !Objects.equals(SchematicRailShape, ClientRailShape) && (Objects.equals(SchematicRailShape, "east_west") || Objects.equals(SchematicRailShape, "north_south")) &&
												(Objects.equals(ClientRailShape, "east_west") || Objects.equals(ClientRailShape, "north_south"));
										}
									} else if (sBlock instanceof ObserverBlock || sBlock instanceof PistonBlock || sBlock instanceof RepeaterBlock || sBlock instanceof ComparatorBlock || sBlock instanceof FenceGateBlock || sBlock instanceof TrapdoorBlock) {
										Direction facingSchematic = fi.dy.masa.malilib.util.BlockUtils.getFirstPropertyFacingValue(stateSchematic);
										Direction facingClient = fi.dy.masa.malilib.util.BlockUtils.getFirstPropertyFacingValue(stateClient);
										ShouldFix = facingSchematic != facingClient;
										ShapeBoolean = facingClient.getOpposite().equals(facingSchematic);
									}
									Direction side = Direction.UP;
									if (ShapeBoolean) {
										Vec3d hitPos = Vec3d.ofCenter(pos);
										BlockHitResult hitResult = new BlockHitResult(hitPos, side, pos, false);
										interactBlock(mc, hitResult); //CACTUS
										cacheEasyPlacePosition(pos, true);
										interact++;
									} else if (breakBlocks && ShouldFix) { //cannot fix via flippincactus
										mc.interactionManager.attackBlock(pos, Direction.DOWN);//by one hit possible?
										breaker.startBreakingBlock(pos, mc); //register
										return ActionResult.SUCCESS;
									}
									continue;
								}
							} //flip
							continue;
						} //cancel normal placing
					}
					if (!ClearArea && flipBlocks) {
						mc.player.sendMessage(Text.of("Handling printerFlippinCactus!"), true);
						continue;
					}
					if (isPositionCached(pos, false) || PRINTER_BEDROCK_BREAKING.getBooleanValue() || (!(stateSchematic.getBlock() instanceof NetherPortalBlock) && stateSchematic.isAir() && !ClearArea)) {
						continue;
					}
					ItemStack stack = MaterialCache.getInstance().getRequiredBuildItemForState(stateSchematic);
					Block cBlock = stateClient.getBlock();
					Block sBlock = stateSchematic.getBlock();
					if (ClearArea) {
						MessageHolder.sendUniqueMessageActionBar(mc.player, "Handling printerClearArea!");
						if (isReplaceableWaterFluidSource(stateClient)) {
							if (!UseCobble) {
								stack = Items.SPONGE.getDefaultStack();
							} else {
								stack = Items.COBBLESTONE.getDefaultStack();
							}
						} else if (stateClient.getFluidState().getFluid() instanceof LavaFluid && stateClient.contains(FluidBlock.LEVEL) && stateClient.get(FluidBlock.LEVEL) == 0) {
							if (!UseCobble) {
								stack = Items.SLIME_BLOCK.getDefaultStack();
							} else {
								stack = Items.COBBLESTONE.getDefaultStack();
							}
						} else if (ClearSnow && cBlock instanceof SnowBlock) {
							stack = Items.STRING.getDefaultStack();
						} else {
							continue;
						}
					}
					if (ClearArea) {
						if (ClearArea && doSchematicWorldPickBlock(mc, stack)) {
							Vec3d hitPos = Vec3d.ofCenter(pos).add(0, 0.5, 0);
							BlockHitResult hitResult = new BlockHitResult(hitPos, Direction.UP, pos, false);
							interactBlock(mc, hitResult); //FLUID REMOVAL
							io.github.eatmyvenom.litematicin.utils.InventoryUtils.decrementCount(isCreative);
							interact++;
							cacheEasyPlacePosition(pos, false);
							sleepWhenRequired(mc);
							if (isReplaceableFluidSource(stateClient) || cBlock instanceof SnowBlock) {
								lastPlaced = new Date().getTime() + 200;
							}
						}
						continue;
					}
					if (!FakeAccurateBlockPlacement.canPlace(stateSchematic, pos)) {
						continue;
					}
					if (sBlock instanceof PistonHeadBlock || stateSchematic.isOf(Blocks.MOVING_PISTON)) {
						continue;
					}
					if (stateSchematic == stateClient) {
						causeMap.remove(pos.asLong());
						continue;
					}
					if (cBlock != sBlock && !stateClient.getMaterial().isReplaceable()) {
						MessageHolder.sendUniqueMessage(mc.player, sBlock.getTranslationKey() + " at " + pos.toShortString() + " is blocking placement of " + cBlock.getTranslationKey() + "!!");
						continue;
					}
					if (canPickBlock(mc, stateSchematic, pos)) {
						if (willFall(stateSchematic, mc.world, pos)) {
							recordCause(pos, stateSchematic.getBlock().getTranslationKey() + " at " + pos.toShortString() + " is Falling block", pos.down());
							MessageHolder.sendUniqueMessage(mc.player, getReason(pos.asLong()));
							continue;
						} else if (!PRINTER_PLACE_ICE.getBooleanValue() && stateSchematic.isOf(Blocks.WATER)) {
							recordCause(pos, stateSchematic.getBlock().getTranslationKey() + " at " + pos.toShortString() + " is water");
							MessageHolder.sendUniqueMessage(mc.player, getReason(pos.asLong()));
							continue;
						} else if (!PRINTER_PLACE_ICE.getBooleanValue() && stateSchematic.isOf(Blocks.LAVA)) {
							recordCause(pos, stateSchematic.getBlock().getTranslationKey() + " at " + pos.toShortString() + " is lava");
							MessageHolder.sendUniqueMessage(mc.player, getReason(pos.asLong()));
							continue;
						} else if (sBlock instanceof SandBlock || sBlock instanceof DragonEggBlock || sBlock instanceof ConcretePowderBlock || sBlock instanceof GravelBlock || sBlock instanceof AnvilBlock) {
							BlockPos Offsetpos = new BlockPos(x, y - 1, z);
							BlockState OffsetstateSchematic = world.getBlockState(Offsetpos);
							BlockState OffsetstateClient = mc.world.getBlockState(Offsetpos);
							if (OffsetstateClient.isAir() || (breakBlocks && !OffsetstateClient.getBlock().getName().equals(OffsetstateSchematic.getBlock().getName()))) {
								recordCause(pos, stateSchematic.getBlock().getTranslationKey() + " at " + pos.toShortString() + " is Falling block", pos.down());
								MessageHolder.sendUniqueMessage(mc.player, getReason(pos.asLong()));
								continue;
							}
						}
						// BUD, for positions near piston with BUD, place block first.
						if (smartRedstone) {
							if (sBlock instanceof RedstoneBlock) {
								if (isQCable(mc, world, pos)) {
									recordCause(pos, sBlock.getTranslationKey() + " at " + pos.toShortString() + "will QC, waiting other block");
									MessageHolder.sendUniqueMessage(mc.player, getReason(pos.asLong()));
									continue;
								}
							} else if (sBlock instanceof TntBlock) {
								if (mc.world.isReceivingRedstonePower(pos)) {
									recordCause(pos, sBlock.getTranslationKey() + " at " + pos.toShortString() + " is now receiving power!");
									MessageHolder.sendUniqueMessage(mc.player, getReason(pos.asLong()));
									continue;
								}
							} else if (sBlock instanceof PistonBlock) {
								if (!shouldExtendQC(mc, world, pos)) {
									recordCause(pos, sBlock.getTranslationKey() + " at " + pos.toShortString() + " is QC");
									MessageHolder.sendUniqueMessage(mc.player, getReason(pos.asLong()));
									continue;
								} else if (hasNearbyRedirectDust(mc, world, pos)) {
									recordCause(pos, sBlock.getTranslationKey() + " at " + pos.toShortString() + " has redirectable dust nearby at " + hasNearbyRedirectDustPos(mc, world, pos).toShortString());
									MessageHolder.sendUniqueMessage(mc.player, getReason(pos.asLong()));
									continue;
								}
								if (cantAvoidExtend(mc.world, pos, world)) {
									recordCause(pos, sBlock.getTranslationKey() + " at " + pos.toShortString() + " will unexpectedly extend");
									MessageHolder.sendUniqueMessage(mc.player, getReason(pos.asLong()));
									continue;
								}
								if (shouldSuppressExtend(world, pos) && hasWrongStateNearby(mc, world, pos)) {
									recordCause(pos, sBlock.getTranslationKey() + " at " + " is BUD but has wrong state nearby \n" + hasWrongStateNearbyReason(mc, world, pos), hasWrongStateNearbyPos(mc, world, pos));
									MessageHolder.sendUniqueMessage(mc.player, getReason(pos.asLong()));
									continue;
								}
								if (willExtendInWorld(world, pos, stateSchematic.get(PistonBlock.FACING)) != stateSchematic.get(PistonBlock.EXTENDED) && directlyPowered(world, pos, stateSchematic.get(PistonBlock.FACING))) {
									if (PRINTER_SUPPRESS_PUSH_LIMIT.getBooleanValue()) {
										recordCause(pos, sBlock.getTranslationKey() + " at " + pos.toShortString() + " should respect push limit because its directly powered");
										MessageHolder.sendUniqueMessage(mc.player, getReason(pos.asLong()));
										continue;
									}
									MessageHolder.sendUniqueMessage(mc.player, sBlock.getTranslationKey() + " at " + " is placed ignoring push limit checks, check printerSuppressPushLimitPistons option.");
								}
							} else if (sBlock instanceof ObserverBlock) {
								if (ObserverUpdateOrder(mc, world, pos, selectedBox)) {
									if (PRINTER_FLIPPINCACTUS.getBooleanValue() && canBypass(mc, world, pos)) {
										stateSchematic = stateSchematic.with(ObserverBlock.FACING, stateSchematic.get(ObserverBlock.FACING).getOpposite());
									} else {
										BlockPos causedPos = ObserverUpdateOrderPos(mc, world, pos);
										if (causedPos.asLong() == pos.asLong()) {
											MessageHolder.sendUniqueMessage(mc.player, "Observer at " + pos.toShortString() + " is causing self-blocking, check manually");
										}
										recordCause(pos, sBlock.getTranslationKey() + " at " + pos.toShortString() + " is waiting for ", causedPos);
										MessageHolder.sendUniqueMessage(mc.player, getReason(pos.asLong()));
										continue;
									}
								}
							}
						}
						if (smartRedstone && ExplicitObserver) {
							BlockPos observerPos = isObserverCantAvoidOutput(mc, world, pos);
							if (observerPos != null) {
								recordCause(pos, sBlock.getTranslationKey() + " at " + pos.toShortString() + " is waiting for preceded observer at " + observerPos.toShortString(), observerPos);
								MessageHolder.sendUniqueMessage(mc.player, getReason(pos.asLong()));
								continue;
							}
							if (sBlock instanceof ObserverBlock) {
								Pair<Boolean, BlockPos> value = isWatchingCorrectState(mc, world, pos, null, true);
								if (!value.getLeft()) {
									recordCause(pos, sBlock.getTranslationKey() + " at " + pos.toShortString() + " can't be placed due to " + value.getRight().toShortString(), value.getRight());
									MessageHolder.sendUniqueMessage(mc.player, getReason(pos.asLong()));
									continue;
								}
							}
						}
						if (sBlock instanceof NetherPortalBlock && !sBlock.getName().equals(cBlock.getName()) &&
							//#if MC>=11700
							NetherPortal.getNewPortal(mc.world, pos, Direction.Axis.X).isPresent()
							//#else
							//$$ AreaHelper.method_30485(mc.world, pos, Direction.Axis.X).isPresent()
							//#endif
						) {
							ItemStack lightStack = Items.FIRE_CHARGE.getDefaultStack();
							if (getInventory(mc.player).getSlotWithStack(lightStack) == -1) {
								lightStack = Items.FLINT_AND_STEEL.getDefaultStack();
							}
							BlockPos offsetPos = new BlockPos(x, y - 1, z);
							BlockState offsetStateSchematic = world.getBlockState(offsetPos);
							BlockState offsetStateClient = mc.world.getBlockState(offsetPos);
							if (getInventory(mc.player).getSlotWithStack(lightStack) == -1 || offsetStateClient.isAir() || (!offsetStateClient.getBlock().getName().equals(offsetStateSchematic.getBlock().getName()))) {
								continue;
							}
							if (doSchematicWorldPickBlock(mc, lightStack)) {
								Vec3d hitPos = Vec3d.ofCenter(new BlockPos(x, y - 1, z)).add(0, 0.5, 0);
								BlockHitResult hitResult = new BlockHitResult(hitPos, Direction.UP, new BlockPos(x, y - 1, z), false);
								interactBlock(mc, hitResult); //LIGHT
								cacheEasyPlacePosition(pos, false);
								sleepWhenRequired(mc);
								if (shouldSleepLonger) {
									shouldSleepLonger = false;
									lastPlaced = Math.max(lastPlaced, new Date().getTime() + 200 + PRINTER_SLEEP_STACK_EMPTIED.getIntegerValue());
								} else {
									lastPlaced = Math.max(lastPlaced, new Date().getTime() + 200);
								}
								interact++;
							}
						}
						Direction facing = fi.dy.masa.malilib.util.BlockUtils.getFirstPropertyFacingValue(stateSchematic);
						if (facing != null) {
							facing = facing.getOpposite();
						}
						if (stateSchematic.getBlock() instanceof AbstractRailBlock) {
							facing = convertRailShapetoFace(stateSchematic);
						}
						if (facing != null) {
							FacingData facedata = FacingData.getFacingData(stateSchematic);
							if (facedata == null && !(stateSchematic.getBlock() instanceof AbstractRailBlock) && !simulateFacingData(stateSchematic, pos, Vec3d.ofCenter(pos)) ) {
								MessageHolder.sendMessageUncheckedUnique(mc.player, stateSchematic.getBlock() + " does not have facing data, please add this!");
								if (PRINTER_SKIP_UNKNOWN_BLOCKSTATE.getBooleanValue()) continue;

							}
							if (!(CanUseProtocol && IsBlockSupportedCarpet(stateSchematic.getBlock())) && !PRINTER_FAKE_ROTATION.getBooleanValue() && !canPlaceFace(facedata, stateSchematic, primaryFacing, horizontalFacing)) {
								continue;
							}

							if ((stateSchematic.getBlock() instanceof DoorBlock
								&& stateSchematic.get(DoorBlock.HALF) == DoubleBlockHalf.UPPER)
								|| (stateSchematic.getBlock() instanceof BedBlock
								&& stateSchematic.get(BedBlock.PART) == BedPart.HEAD)) {
								continue;
							}
						}

						// Exception for signs (edge case)
						if (stateSchematic.getBlock() instanceof SignBlock
							&& !(stateSchematic.getBlock() instanceof WallSignBlock)) {
							if ((MathHelper.floor((double) ((180.0F + getYaw(mc.player)) * 16.0F / 360.0F) + 0.5D)
								& 15) != stateSchematic.get(SignBlock.ROTATION)) {
								continue;
							}
						}
						Direction sideOrig = Direction.NORTH;
						BlockPos npos = pos;
						Direction side = applyPlacementFacing(stateSchematic, sideOrig, stateClient);
						Block blockSchematic = stateSchematic.getBlock();
						//Don't place waterlogged block's original block before fluid since its painful
						// 1. if
						if (PRINTER_PLACE_ICE.getBooleanValue() &&
							(isReplaceableWaterFluidSource(stateSchematic) && stateClient.getMaterial().isReplaceable() && !isReplaceableWaterFluidSource(stateClient) && !stateClient.isOf(Blocks.LAVA) ||
								PRINTER_WATERLOGGED_WATER_FIRST.getBooleanValue() && stateClient.getMaterial().isReplaceable() && containsWaterloggable(stateSchematic))
						) {
							ItemStack iceStack = Items.ICE.getDefaultStack();
							if (!FakeAccurateBlockPlacement.canHandleOther(iceStack.getItem())) {
								continue;
							}
							if (doSchematicWorldPickBlock(mc, iceStack)) {
								interactBlock(mc, new BlockHitResult(new Vec3d(pos.getX(), pos.getY(), pos.getZ()), Direction.DOWN, pos, false));
								io.github.eatmyvenom.litematicin.utils.InventoryUtils.decrementCount(isCreative);
								cacheEasyPlacePosition(pos, false);
								sleepWhenRequired(mc);
								interact++;
							} //ICE
							else {
								recordCause(pos, "Can't pick item " + Items.ICE.getTranslationKey() + " at " + pos.toShortString());
							}
							continue;
						}
						if (!canPickBlock(mc, stateSchematic, pos)) {
							//mc.player.sendMessage(Text.of("Can't pick block"),true);
							recordCause(pos, "Can't pick item " + stateSchematic.getBlock().asItem().getTranslationKey() + " at " + pos.toShortString());
							MessageHolder.sendUniqueMessage(mc.player, "Can't pick item " + stateSchematic.getBlock().asItem().getTranslationKey() + " at " + pos.toShortString());
							continue;
						}
						if (!blockSchematic.canPlaceAt(stateSchematic, mc.world, pos)) {
							recordCause(pos, stateSchematic.getBlock().toString() + "(" + pos.toShortString() + ", can't be placed)");
							MessageHolder.sendUniqueMessage(mc.player, stateSchematic.getBlock().getTranslationKey() + " can't be placed at " + pos.toShortString());
							continue;
						}
						if (blockSchematic instanceof GrindstoneBlock) {
							placeGrindStone(stateSchematic, mc, pos);
							interact++;
							continue;
						}
						if (blockSchematic instanceof TrapdoorBlock && !CanUseProtocol && !PRINTER_FAKE_ROTATION.getBooleanValue()) {
							placeTrapDoor(stateSchematic, mc, pos);
							interact++;
							continue;
						}
						int miliseconds = EASY_PLACE_CACHE_TIME.getIntegerValue();
						if (blockSchematic instanceof WallMountedBlock || blockSchematic instanceof TorchBlock || blockSchematic instanceof WallSkullBlock
							|| blockSchematic instanceof LadderBlock
							|| blockSchematic instanceof TripwireHookBlock || blockSchematic instanceof WallSignBlock ||
							blockSchematic instanceof EndRodBlock || blockSchematic instanceof DeadCoralFanBlock) {

							/*
							 * Some blocks, especially wall mounted blocks must be placed on another for
							 * directionality to work Basically, the block pos sent must be a "clicked"
							 * block.
							 */
							if (blockSchematic instanceof ButtonBlock || blockSchematic instanceof LeverBlock) {
								WallMountLocation wallMountLocation = stateSchematic.get(WallMountedBlock.FACE);
								if (wallMountLocation == WallMountLocation.FLOOR) {
									npos = pos.down();
								} else if (wallMountLocation == WallMountLocation.CEILING) {
									npos = pos.up();
								} else {
									npos = pos.offset(stateSchematic.get(WallMountedBlock.FACING).getOpposite());
								}
							} else if (blockSchematic instanceof TorchBlock) {
								if (blockSchematic instanceof WallTorchBlock || blockSchematic instanceof WallRedstoneTorchBlock) {
									npos = pos.offset(stateSchematic.get(WallTorchBlock.FACING).getOpposite());
								} else {
									npos = pos.down();
								}
								if (hasGui(world.getBlockState(npos).getBlock())) {
									if (PRINTER_FAKE_ROTATION.getBooleanValue() && interact < maxInteract) {
										if (FakeAccurateBlockPlacement.request(stateSchematic, pos)) {
											interact++;
										}
										continue;
									}
									recordCause(pos, "Torch at " + pos.toShortString() + " can't be placed due to " + world.getBlockState(npos).getBlock().getTranslationKey() + "at " + npos.toShortString() + " has GUI");
									MessageHolder.sendUniqueMessage(mc.player, "Torch at " + pos.toShortString() + " can't be placed due to " + world.getBlockState(npos).getBlock().getTranslationKey() + "at " + npos.toShortString() + " has GUI");
									continue;
								}
							} else if (blockSchematic instanceof DeadCoralFanBlock) {
								if (blockSchematic instanceof DeadCoralWallFanBlock) {
									npos = pos.offset(stateSchematic.get(DeadCoralWallFanBlock.FACING).getOpposite());
								} else {
									npos = pos.down();
								}
							} else {
								npos = pos.offset(side.getOpposite()); //offset block for 'side'
							}
							//Any : if we have block in testPos, then we can place with wanted direction.
							//Trapdoors : it can be placed in air with player direction's opposite.
							//Else : can't be placed except End Rod.
							if (!mc.world.getBlockState(npos).getMaterial().isReplaceable()) {
								//npos is blockPos to be hit.
								//instead, hitVec should have 1 corresponding to direction property.
								//but First check if its block with GUI*
								Block checkGui = mc.world.getBlockState(npos).getBlock();
								if (!mc.player.shouldCancelInteraction() && hasGui(checkGui)) {
									if (blockSchematic instanceof TrapdoorBlock && PRINTER_FAKE_ROTATION.getBooleanValue() && interact < maxInteract) {
										if (FakeAccurateBlockPlacement.request(stateSchematic, pos)) {
											interact++;
										}
										continue;
									}
									//Has GUI so clickPos can't be clicked.
									recordCause(pos, stateSchematic.getBlock().getTranslationKey() + " can't be placed at " + pos.toShortString() + "because " + npos.toShortString() + " has GUI");
									MessageHolder.sendUniqueMessage(mc.player, getReason(pos.asLong()));
									continue;
								} else if (blockSchematic instanceof TorchBlock) {
									//no gui, just place
									if (blockSchematic instanceof WallTorchBlock || blockSchematic instanceof WallRedstoneTorchBlock) {
										MessageHolder.sendDebugMessage(mc.player, "placing wall torch clicking " + npos.toShortString() + " torch facing : " + stateSchematic.get(WallTorchBlock.FACING).toString());
										Vec3d hitVec = Vec3d.ofCenter(npos).add(Vec3d.of(stateSchematic.get(WallTorchBlock.FACING).getVector()).multiply(0.5));
										if (stateSchematic.contains(RedstoneTorchBlock.LIT) && !stateSchematic.get(RedstoneTorchBlock.LIT)) {
											cacheEasyPlacePosition(pos.up(), false, miliseconds);
										}
										Direction required = stateSchematic.get(WallTorchBlock.FACING);
										if (doSchematicWorldPickBlock(mc, stateSchematic, pos)) {
											cacheEasyPlacePosition(pos, false);
											interact++;
											if (stateSchematic.contains(RedstoneTorchBlock.LIT) && !stateSchematic.get(RedstoneTorchBlock.LIT)) {
												cacheEasyPlacePosition(pos.up(), false, miliseconds);
											}
											interactBlock(mc, new BlockHitResult(hitVec, required, npos, false)); //place block
											io.github.eatmyvenom.litematicin.utils.InventoryUtils.decrementCount(isCreative);
											sleepWhenRequired(mc);
										}
										continue;
									}
									Vec3d hitVec = Vec3d.ofCenter(npos).add(Vec3d.of(Direction.UP.getVector()).multiply(0.5));
									if (doSchematicWorldPickBlock(mc, stateSchematic, pos)) {
										MessageHolder.sendDebugMessage(mc.player, "Placing torch clicking " + npos.toShortString());
										MessageHolder.sendDebugMessage(mc.player, "\t Wanted torch pos : " + pos.toShortString());
										MessageHolder.sendDebugMessage(mc.player, "\t HitVec applied : " + hitVec);
										MessageHolder.sendDebugMessage(mc.player, "\t Side applied : " + Direction.UP);
										cacheEasyPlacePosition(pos, false);
										interact++;
										if (stateSchematic.contains(RedstoneTorchBlock.LIT) && !stateSchematic.get(RedstoneTorchBlock.LIT)) {
											cacheEasyPlacePosition(pos.up(), false, miliseconds);
										}
										interactBlock(mc, new BlockHitResult(hitVec, Direction.UP, npos, false)); //place block
										io.github.eatmyvenom.litematicin.utils.InventoryUtils.decrementCount(isCreative);
										sleepWhenRequired(mc);
									}
									continue;
								} else if (canPlaceFace(FacingData.getFacingData(stateSchematic), stateSchematic, primaryFacing, horizontalFacing)) { // no gui
									Direction required = fi.dy.masa.malilib.util.BlockUtils.getFirstPropertyFacingValue(stateSchematic);
									required = applyPlacementFacing(stateSchematic, required, stateClient);
									Vec3d hitVec = applyHitVec(npos, stateSchematic, required);
									if (doSchematicWorldPickBlock(mc, stateSchematic, pos)) {
										cacheEasyPlacePosition(pos, false);
										interact++;
										if (stateSchematic.contains(RedstoneTorchBlock.LIT) && !stateSchematic.get(RedstoneTorchBlock.LIT)) {
											cacheEasyPlacePosition(pos.up(), false, 700);
										}
										interactBlock(mc, new BlockHitResult(hitVec, required, npos, false)); //place block
										io.github.eatmyvenom.litematicin.utils.InventoryUtils.decrementCount(isCreative);
										sleepWhenRequired(mc);
									}
									continue;
								}
							} else if (blockSchematic instanceof TrapdoorBlock) { //check direction is opposite of player's
								Direction trapdoor = stateSchematic.get(TrapdoorBlock.FACING);
								if (horizontalFacing.getOpposite() == trapdoor) {
									if (doSchematicWorldPickBlock(mc, stateSchematic, pos)) {
										cacheEasyPlacePosition(pos, false);
										interactBlock(mc, new BlockHitResult(Vec3d.of(pos),
											stateSchematic.get(TrapdoorBlock.FACING).getOpposite(), pos, false)); //place block
										io.github.eatmyvenom.litematicin.utils.InventoryUtils.decrementCount(isCreative);
										sleepWhenRequired(mc);
										interact++;
									}
									continue;
								}
							} else if (blockSchematic instanceof GrindstoneBlock) {
								Direction direction = stateSchematic.get(GrindstoneBlock.FACING);
								if ((primaryFacing.getAxis() == Direction.Axis.Y && horizontalFacing == direction) || (primaryFacing.getAxis() != Direction.Axis.Y && horizontalFacing == direction.getOpposite())) {
									if (doSchematicWorldPickBlock(mc, stateSchematic, pos)) {
										cacheEasyPlacePosition(pos, false);
										interactBlock(mc, new BlockHitResult(Vec3d.of(pos),
											stateSchematic.get(GrindstoneBlock.FACING).getOpposite(), pos, false)); //place block
										io.github.eatmyvenom.litematicin.utils.InventoryUtils.decrementCount(isCreative);
										sleepWhenRequired(mc);
										interact++;
									}
								}
								continue;
							} else { //Only end rod.
								if (blockSchematic instanceof EndRodBlock) {
									if (doSchematicWorldPickBlock(mc, stateSchematic, pos)) {
										cacheEasyPlacePosition(pos, false);
										interactBlock(mc, new BlockHitResult(Vec3d.ofCenter(pos),
											stateSchematic.get(EndRodBlock.FACING), pos, false)); //place block
										io.github.eatmyvenom.litematicin.utils.InventoryUtils.decrementCount(isCreative);
										interact++;
										sleepWhenRequired(mc);
									}
								}
								continue;
							}

						} //End of trapdoor / wall mounted blocks

						Vec3d hitPos;
						// Carpet Accurate Placement protocol support, plus BlockSlab support
						if (CanUseProtocol && IsBlockSupportedCarpet(stateSchematic.getBlock())) {
							hitPos = applyCarpetProtocolHitVec(npos, stateSchematic);
						} else {
							hitPos = applyHitVec(npos, stateSchematic, side);
						}

						// Mark that this position has been handled (use the non-offset position that is
						// checked above)
						BlockHitResult hitResult = new BlockHitResult(hitPos, side, npos, false);

						//System.out.printf("pos: %s side: %s, hit: %s\n", pos, side, hitPos);
						// pos, side, hitPos
						if (stateSchematic.getBlock() instanceof SnowBlock) {
							stateClient = mc.world.getBlockState(npos);
							if (stateClient.isAir() || stateClient.getBlock() instanceof SnowBlock
								&& stateClient.get(SnowBlock.LAYERS) < stateSchematic.get(SnowBlock.LAYERS)) {
								side = Direction.UP;
								hitResult = new BlockHitResult(hitPos, side, npos, false);
								if (doSchematicWorldPickBlock(mc, stateSchematic, pos)) {
									cacheEasyPlacePosition(pos, false);
									interact++;
									interactBlock(mc, hitResult); //SNOW LAYERS
									io.github.eatmyvenom.litematicin.utils.InventoryUtils.decrementCount(isCreative);
									sleepWhenRequired(mc);
								}
							}
							continue;
						}
						//finally places block
						if (smartRedstone) {
							if (stateSchematic.contains(RedstoneTorchBlock.LIT) && !stateSchematic.get(RedstoneTorchBlock.LIT)) {
								cacheEasyPlacePosition(pos.up(), false, 700);
							}
							Set<BlockPos> shouldCache = ObserverCantAvoidPos(mc, world, pos);
							if (!shouldCache.isEmpty()) {
								shouldCache.forEach(a -> {
									MessageHolder.sendDebugMessage("Caching position " + a.toShortString() + " because observer can't avoid ");
									cacheEasyPlacePosition(a, true, (int) Math.ceil(Math.sqrt(a.getSquaredDistance(pos)) * 100));
								});
							}
						}
						if (!PRINTER_FAKE_ROTATION.getBooleanValue() || PRINTER_ACCURATE_BLOCK_PLACEMENT.getBooleanValue()) { //Accurateblockplacement, or vanilla but no fake
							if (doSchematicWorldPickBlock(mc, stateSchematic, pos)) {
								MessageHolder.sendOrderMessage("Places block " + blockSchematic + " at " + pos.toShortString());
								interactBlock(mc, hitResult); //PLACE BLOCK
								io.github.eatmyvenom.litematicin.utils.InventoryUtils.decrementCount(isCreative);
								cacheEasyPlacePosition(pos, false);
								sleepWhenRequired(mc);
								interact++;
							}
							continue;
						} else {
							if (!(sBlock instanceof FluidBlock)) {
								if (interact < maxInteract && FakeAccurateBlockPlacement.request(stateSchematic, pos)) {
									interact++;
								}
							}
						}
						if (stateSchematic.getBlock() instanceof SlabBlock
							&& stateSchematic.get(SlabBlock.TYPE) == SlabType.DOUBLE) {
							stateClient = mc.world.getBlockState(npos);

							if (stateClient.getBlock() instanceof SlabBlock
								&& stateClient.get(SlabBlock.TYPE) != SlabType.DOUBLE) {
								side = applyPlacementFacing(stateSchematic, sideOrig, stateClient);
								hitResult = new BlockHitResult(hitPos, side, npos, false);
								if (doSchematicWorldPickBlock(mc, stateSchematic, pos)) {
									interactBlock(mc, hitResult); //double slab
									io.github.eatmyvenom.litematicin.utils.InventoryUtils.decrementCount(isCreative);
									cacheEasyPlacePosition(pos, false);
									sleepWhenRequired(mc);
									interact++;
								}
								continue;
							}
						}
						if (stateSchematic.getBlock() instanceof SeaPickleBlock
							&& stateSchematic.get(SeaPickleBlock.PICKLES) > 1) {
							stateClient = mc.world.getBlockState(npos);
							if (stateClient.getBlock() instanceof SeaPickleBlock
								&& stateClient.get(SeaPickleBlock.PICKLES) < stateSchematic.get(SeaPickleBlock.PICKLES)) {
								side = applyPlacementFacing(stateSchematic, sideOrig, stateClient);
								hitResult = new BlockHitResult(hitPos, side, npos, false);
								if (doSchematicWorldPickBlock(mc, stateSchematic, pos)) {
									interactBlock(mc, hitResult); //double slab
									io.github.eatmyvenom.litematicin.utils.InventoryUtils.decrementCount(isCreative);
									cacheEasyPlacePosition(pos, false);
									sleepWhenRequired(mc);
									interact++;
								}
								continue;
							}
						}

						if (interact >= maxInteract) {
							if (shouldSleepLonger) {
								shouldSleepLonger = false;
								lastPlaced = Math.max(lastPlaced, new Date().getTime() + PRINTER_SLEEP_STACK_EMPTIED.getIntegerValue());
							} else {
								lastPlaced = Math.max(lastPlaced, new Date().getTime());
							}
							return ActionResult.SUCCESS;
						}

					} else {
						MessageHolder.sendUniqueMessage(mc.player, sBlock.getTranslationKey() + " can't be picked !!");
					}
				}
			}

		}

		if (interact > 0) {
			if (shouldSleepLonger) {
				shouldSleepLonger = false;
				lastPlaced = Math.max(lastPlaced, new Date().getTime() + PRINTER_SLEEP_STACK_EMPTIED.getIntegerValue());
			} else {
				lastPlaced = Math.max(lastPlaced, new Date().getTime());
			}
			return ActionResult.SUCCESS;
		}
		if (!(mc.player.getMainHandStack().getItem() instanceof BlockItem) && !(mc.player.getOffHandStack().getItem() instanceof BlockItem)) {
			return ActionResult.PASS;
		}
		return ActionResult.FAIL;
	}

	private static boolean willFall(BlockState stateSchematic, World clientWorld, BlockPos pos) {
		if (stateSchematic.getBlock() instanceof ScaffoldingBlock) {
			return !stateSchematic.getBlock().canPlaceAt(stateSchematic, clientWorld, pos);
		}
		return false;
	}

	/*
		returns if redstone block should not be placed (before piston)
	 */
	private static boolean isQCable(MinecraftClient mc, World world, BlockPos pos) {
		BlockPos downPos = pos.down();
		for (Direction direction : BedrockBreaker.HORIZONTAL) {
			BlockPos offsetPos = downPos.offset(direction);
			BlockState stateClient = mc.world.getBlockState(offsetPos);
			BlockState stateSchematic = world.getBlockState(offsetPos);
			if (!(stateSchematic.getBlock() instanceof PistonBlock)) {
				continue;
			}
			if (stateSchematic.get(PistonBlock.EXTENDED)) {
				continue;
			}
			if (stateClient.isAir()) { //very basic qc
				return true;
			} else if (!hasNoUpdatableState(mc, world, offsetPos)) {
				return true;
			} else if (stateClient.getBlock() instanceof PistonBlock && stateSchematic.get(PistonBlock.FACING).equals(Direction.UP)) {
				if (!world.getBlockState(offsetPos.up()).getBlock().equals(mc.world.getBlockState(offsetPos.up()).getBlock())) {
					return true;
				}
			}
		}
		BlockState stateSchematic = world.getBlockState(downPos.down());
		return stateSchematic.getBlock() instanceof PistonBlock && !stateSchematic.get(PistonBlock.EXTENDED) && !world.getBlockState(downPos).getBlock().equals(mc.world.getBlockState(downPos).getBlock());
	}

	private static boolean hasNoUpdatableState(MinecraftClient mc, World world, BlockPos pos) {
		for (Direction direction : Direction.values()) {
			if (world.getBlockState(pos.offset(direction)) != mc.world.getBlockState(pos.offset(direction))) {
				if (!isNoteBlockInstrumentError(mc, world, pos.offset(direction)) && !isDoorHingeError(mc, world, pos.offset(direction))) {
					if (world.isAir(pos.offset(direction)) && mc.world.isAir(pos.offset(direction))) {
						continue;
					}
					return false;
				}
			}
		}
		return true;
	}

	private static boolean hasNearbyRedirectDust(MinecraftClient mc, World world, BlockPos pos) { //temporary code, just direct redirection check nearby
		for (Direction direction : Direction.values()) {
			if (!isCorrectDustState(mc, world, pos.offset(direction))) {
				return true;
			}
			if (direction.getAxis() != Direction.Axis.Y && !isCorrectDustState(mc, world, pos.offset(direction, 2))) {
				return true;
			}
			if (!isCorrectDustState(mc, world, pos.offset(direction).up())) {
				return true;
			}
			if (direction.getAxis() != Direction.Axis.Y && !isCorrectDustState(mc, world, pos.offset(direction, 2).up())) {
				return true;
			}
		}
		return false;
	}

	private static BlockPos hasNearbyRedirectDustPos(MinecraftClient mc, World world, BlockPos pos) { //temporary code, just direct redirection check nearby
		for (Direction direction : Direction.values()) {
			if (!isCorrectDustState(mc, world, pos.offset(direction))) {
				return pos.offset(direction);
			}
			if (!isCorrectDustState(mc, world, pos.offset(direction, 2))) {
				return pos.offset(direction, 2);
			}
			if (!isCorrectDustState(mc, world, pos.offset(direction).up())) {
				return pos.offset(direction).up();
			}
			if (!isCorrectDustState(mc, world, pos.offset(direction, 2).up())) {
				return pos.offset(direction, 2).up();
			}
		}
		return null;
	}

	private static boolean cantAvoidExtend(World world, BlockPos pos, World schematicWorld) {
		if (!schematicWorld.getBlockState(pos).get(PistonBlock.EXTENDED)) {
			return willExtendInWorld(world, pos, schematicWorld.getBlockState(pos).get(PistonBlock.FACING));
		}
		return false;
	}

	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	private static boolean isCorrectDustState(MinecraftClient mc, World world, BlockPos pos) {
		BlockState ClientState = mc.world.getBlockState(pos);
		BlockState SchematicState = world.getBlockState(pos);
		if (!SchematicState.isOf(Blocks.REDSTONE_WIRE)) {
			return true;
		}
		if (!ClientState.isOf(Blocks.REDSTONE_WIRE)) {
			return false;
		}
		return SchematicState.get(RedstoneWireBlock.WIRE_CONNECTION_EAST) == ClientState.get(RedstoneWireBlock.WIRE_CONNECTION_EAST) &&
			SchematicState.get(RedstoneWireBlock.WIRE_CONNECTION_WEST) == ClientState.get(RedstoneWireBlock.WIRE_CONNECTION_WEST) &&
			SchematicState.get(RedstoneWireBlock.WIRE_CONNECTION_SOUTH) == ClientState.get(RedstoneWireBlock.WIRE_CONNECTION_SOUTH) &&
			SchematicState.get(RedstoneWireBlock.WIRE_CONNECTION_NORTH) == ClientState.get(RedstoneWireBlock.WIRE_CONNECTION_NORTH) &&
			Objects.equals(SchematicState.get(RedstoneWireBlock.POWER) == 0, ClientState.get(RedstoneWireBlock.POWER) == 0);
	}

	private static boolean shouldExtendQC(MinecraftClient mc, World world, BlockPos pos) {
		return willExtendInWorld(mc.world, pos, world.getBlockState(pos).get(PistonBlock.FACING)) == world.getBlockState(pos).get(PistonBlock.EXTENDED);
	}

	/*
		returns if piston is powered + but it's not extended in schematic, can be BUD or direct power
	 */
	private static boolean shouldSuppressExtend(World world, BlockPos pos) {
		return willExtendInWorld(world, pos, world.getBlockState(pos).get(PistonBlock.FACING)) && !world.getBlockState(pos).get(PistonBlock.EXTENDED);
	}

	/*
		returns if piston is DIRECTLY powered by redstone block, can't be solved via QC references.
	 */
	private static boolean directlyPowered(World schematicWorld, BlockPos pos, Direction pistonFace) {
		for (Direction lv : Direction.values()) {
			if (lv == pistonFace) {
				continue;
			}
			if (schematicWorld.getBlockState(pos.offset(lv)).isOf(Blocks.REDSTONE_BLOCK)) {
				return true;
			}
		}
		return false;
	}

	private static boolean willExtendInWorld(World world, BlockPos pos, Direction pistonFace) {
		for (Direction lv : Direction.values()) {
			if (lv == pistonFace || !world.isEmittingRedstonePower(pos.offset(lv), lv)) {
				continue;
			}
			//Observer client error wtf?
			boolean hasObserver = false;
			for (Direction dir : Direction.values()) {
				BlockState observerState = world.getBlockState(pos.offset(lv).offset(dir));
				if (observerState.isOf(Blocks.OBSERVER)) {
					if (observerState.get(ObserverBlock.POWERED)) {
						hasObserver = true;
						break;
					}
				}
			}
			BlockState adjState = world.getBlockState(pos.offset(lv));
			if (adjState.isOf(Blocks.OBSERVER)) {
				if (adjState.get(ObserverBlock.POWERED)) {
					hasObserver = true;
				}
			}
			if (hasObserver) {
				continue;
			}
			return true;
		}
		if (world.isEmittingRedstonePower(pos, Direction.DOWN)) {
			return true;
		}
		BlockPos lv2 = pos.up();
		for (Direction lv3 : Direction.values()) {
			if (lv3 == Direction.DOWN || !world.isEmittingRedstonePower(lv2.offset(lv3), lv3)) {
				continue;
			}
			BlockState qcState = world.getBlockState(lv2.offset(lv3));
			if (qcState.isOf(Blocks.OBSERVER) && qcState.get(ObserverBlock.FACING) == lv3 && qcState.get(ObserverBlock.POWERED)) {
				continue;
			}
			return true;
		}
		return false;
	}

	/* * *
	returns if block is observer output but observer can't avoid update
	If its true, then block should be placed after observer update is done
	Case A : Observer is facing wall attached : observer - wall - output
	Case B : Observer is facing Noteblock from horizontal : observer - block below noteblock - noteblock - output
	Case C : Observer is facing wire connected to observer's up offset
	 * * */
	@SuppressWarnings({"ConstantConditions"})
	private static BlockPos isObserverCantAvoidOutput(MinecraftClient mc, World schematicWorld, BlockPos pos) {
		if (isQCableBlock(schematicWorld.getBlockState(pos))) {
			if (schematicWorld.getBlockState(pos.up(2)).isOf(Blocks.OBSERVER) && schematicWorld.getBlockState(pos.up(2)).get(ObserverBlock.FACING) == Direction.UP) {
				if (mc.world.getBlockState(pos.up(3)) != schematicWorld.getBlockState(pos.up(3)) || mc.world.getBlockState(pos.up(2)).contains(ObserverBlock.POWERED) && mc.world.getBlockState(pos.up(2)).get(ObserverBlock.POWERED)) {
					MessageHolder.sendDebugMessage("Position at " + pos.toShortString() + " has observer that will QC, but not watching correct state");
					return pos.up(3);
				}
			}
		}
		for (Direction direction : Direction.values()) {
			BlockState offsetState = schematicWorld.getBlockState(pos.offset(direction));
			if (offsetState.getBlock() instanceof ObserverBlock && offsetState.get(ObserverBlock.FACING) == direction) {
				Pair<Boolean, BlockPos> value = isWatchingCorrectState(mc, schematicWorld, pos.offset(direction), null, false);
				if (!value.getLeft()) {
					return pos.offset(direction);
				}
			}
			//QC
			if (direction == Direction.UP || direction == Direction.DOWN || !isQCableBlock(schematicWorld, pos)) {
				continue;
			}
			//Horizontal,
			BlockPos qcPos = pos.offset(direction).up();
			BlockState qcState = schematicWorld.getBlockState(qcPos);
			BlockState existingState = mc.world.getBlockState(qcPos);
			if (qcState.getBlock() instanceof ObserverBlock && !existingState.isOf(Blocks.OBSERVER) && qcState.get(ObserverBlock.FACING) == direction) {
				Pair<Boolean, BlockPos> value = isWatchingCorrectState(mc, schematicWorld, qcPos, null, false);
				if (!value.getLeft()) {
					return pos.offset(direction);
				}
			}
			// again, QC + powerable block uhh
			else if (qcState.isSolidBlock(schematicWorld, qcPos)) {
				qcPos = qcPos.offset(direction);
				qcState = schematicWorld.getBlockState(qcPos);
				if (qcState.getBlock() instanceof ObserverBlock && !existingState.isOf(Blocks.OBSERVER) && qcState.get(ObserverBlock.FACING) == direction) {
					Pair<Boolean, BlockPos> value = isWatchingCorrectState(mc, schematicWorld, qcPos, null, false);
					if (!value.getLeft()) {
						return pos.offset(direction);
					}
				}
			}
		}
		return null;
	}

	private static void sleepWhenRequired(MinecraftClient mc) {
		if (!USE_INVENTORY_CACHE.getBooleanValue()) {
			return;
		}
		if (PRINTER_SLEEP_STACK_EMPTIED.getIntegerValue() > 0 && io.github.eatmyvenom.litematicin.utils.InventoryUtils.lastCount <= 0) {
			shouldSleepLonger = true;
			lastPlaced = new Date().getTime() + PRINTER_SLEEP_STACK_EMPTIED.getIntegerValue();
			MessageHolder.sendUniqueMessageActionBar(mc.player, "Sleeping because stack is emptied!");
			isSleeping = true;
		}
	}

	private static boolean isQCableBlock(World world, BlockPos pos) {
		Block block = world.getBlockState(pos).getBlock();
		return (!PRINTER_AVOID_CHECK_ONLY_PISTONS.getBooleanValue() && block instanceof DispenserBlock) || block instanceof PistonBlock;
	}

	private static boolean isQCableBlock(BlockState blockState) {
		Block block = blockState.getBlock();
		return (!PRINTER_AVOID_CHECK_ONLY_PISTONS.getBooleanValue() && block instanceof DispenserBlock) || block instanceof PistonBlock;
	}

	/***
	 *
	 * @param mc : client
	 * @param schematicWorld : schematic world
	 * @param pos : BlockPos
	 * @param recursive : Sets of position checked
	 * @param allowFirst : direct search of wallmount / walls / etc. at first
	 * @return Entry : correct / position caused
	 */
	private static Pair<Boolean, BlockPos> isWatchingCorrectState(MinecraftClient mc, World schematicWorld, BlockPos pos, Set<Long> recursive, boolean allowFirst) {
		//observer, then recursive
		if (recursive == null) {
			recursive = new HashSet<>();
		}
		if (recursive.contains(pos.asLong())) {
			return new Pair<>(true, pos);
		}
		BlockState clientState = mc.world.getBlockState(pos);
		BlockState schematicState = schematicWorld.getBlockState(pos);
		if (schematicState.getBlock() instanceof ObserverBlock) {
			Direction facing = schematicState.get(ObserverBlock.FACING);
			recursive.add(pos.asLong());
			if (allowFirst && ObserverCantAvoid(mc, schematicWorld, facing, pos)) {
				return new Pair<>(true, pos);
			} else {
				Pair<Boolean, BlockPos> entry = isWatchingCorrectState(mc, schematicWorld, pos.offset(facing), recursive, allowFirst);
				if (entry.getLeft()) {
					return entry;
				} else {
					return new Pair<>(false, pos);
				}
			}
		}// virtual observers then go recursive
		else {
			if (schematicState == Blocks.VOID_AIR.getDefaultState() || schematicState == Blocks.BARRIER.getDefaultState() || schematicState.isAir()) {
				return new Pair<>(true, pos);
			} else if (clientState != schematicState) {
				//but check wire...
				if (isNoteBlockInstrumentError(mc, schematicWorld, pos) || isDoorHingeError(mc, schematicWorld, pos)) {
					return new Pair<>(true, pos);
				}
				if (isClientPowerError(mc, schematicWorld, clientState, schematicState, pos)) {
					return new Pair<>(true, pos);
				}
				return new Pair<>(false, pos);
			}
		}
		return new Pair<>(true, pos);
	}

	private static boolean isClientPowerError(MinecraftClient mc, World world, BlockState clientState, BlockState schematicState, BlockPos pos) {
		//handles client error, mostly, dropper being powered directly, hopper being powered etc
		if (clientState.getBlock() != schematicState.getBlock()) {
			return false;
		}
		if (schematicState.isOf(Blocks.DROPPER) || schematicState.isOf(Blocks.DISPENSER)) {
			if (schematicState.get(DropperBlock.TRIGGERED) != clientState.get(DropperBlock.TRIGGERED)) {
				boolean isReceiving = mc.world.isReceivingRedstonePower(pos) || mc.world.isReceivingRedstonePower(pos.up());
				if (isReceiving != clientState.get(DropperBlock.TRIGGERED)) {
					mc.world.setBlockState(pos, clientState.with(DropperBlock.TRIGGERED, isReceiving));
				}
				return mc.world.getBlockState(pos) == schematicState;
			}
		} else if (schematicState.isOf(Blocks.NOTE_BLOCK)) { //special case
			if (schematicState.get(NoteBlock.POWERED) != clientState.get(NoteBlock.POWERED)) {
				boolean isReceiving = mc.world.isReceivingRedstonePower(pos);
				mc.world.setBlockState(pos, clientState.with(NoteBlock.POWERED, isReceiving)); //lets fix
				return isNoteBlockInstrumentError(mc, world, pos);
			}
		} else if (schematicState.isOf(Blocks.HOPPER)) {
			if (schematicState.get(HopperBlock.ENABLED) != clientState.get(HopperBlock.ENABLED)) {
				boolean isReceiving = mc.world.isReceivingRedstonePower(pos);
				mc.world.setBlockState(pos, clientState.with(HopperBlock.ENABLED, !isReceiving));
				return mc.world.getBlockState(pos) == schematicState;
			}
		}
		return false;
	}

	private static boolean ObserverCantAvoid(MinecraftClient mc, World world, Direction facingSchematic, BlockPos pos) {
		//returns true if observer should be placed regardless of state
		BlockPos posOffset = pos.offset(facingSchematic);
		BlockState OffsetStateSchematic = world.getBlockState(posOffset);
		Block offsetBlock = OffsetStateSchematic.getBlock();
		if (OffsetStateSchematic.isOf(Blocks.NOTE_BLOCK)) {
			if (isNoteBlockInstrumentError(mc, world, posOffset) || isDoorHingeError(mc, world, posOffset)) {
				//everything is correct but litematica error
				return true;
			}
		}
		if (facingSchematic.equals(Direction.UP)) {
			return offsetBlock instanceof WallBlock || offsetBlock instanceof ComparatorBlock || offsetBlock instanceof DoorBlock ||
				offsetBlock instanceof RepeaterBlock || offsetBlock instanceof FallingBlock ||
				offsetBlock instanceof AbstractRailBlock || offsetBlock instanceof NoteBlock ||
				offsetBlock instanceof BubbleColumnBlock || offsetBlock instanceof RedstoneWireBlock ||
				((offsetBlock instanceof WallMountedBlock) && OffsetStateSchematic.get(WallMountedBlock.FACE) == WallMountLocation.FLOOR);
		} else if (facingSchematic.equals(Direction.DOWN)) {
			return offsetBlock instanceof WallBlock || offsetBlock instanceof WallMountedBlock && OffsetStateSchematic.get(WallMountedBlock.FACE) == WallMountLocation.CEILING;
		} else {
			return offsetBlock instanceof WallBlock || offsetBlock instanceof PaneBlock || offsetBlock instanceof FenceBlock || OffsetStateSchematic.isOf(Blocks.IRON_BARS) || offsetBlock instanceof WallMountedBlock &&
				OffsetStateSchematic.get(WallMountedBlock.FACE) == WallMountLocation.WALL && OffsetStateSchematic.get(WallMountedBlock.FACING) == facingSchematic || hasDustOrAscendingRails(world, facingSchematic, pos);
		}
	}

	private static boolean hasDustOrAscendingRails(World schematicWorld, Direction watching, BlockPos observerPos) {
		BlockPos possible = observerPos.offset(watching);
		BlockState state = schematicWorld.getBlockState(possible);
		if (state.isOf(Blocks.REDSTONE_WIRE)) {
			//ascending_'opposite' directions
			//watching.getOpposite should have 'up'
			WireConnection connection = state.get(RedstoneWireBlock.DIRECTION_TO_WIRE_CONNECTION_PROPERTY.get(watching.getOpposite()));
			return connection == WireConnection.UP;

		} else if (state.getBlock() instanceof PoweredRailBlock) {
			switch (watching) {
				case NORTH : {
					return state.get(PoweredRailBlock.SHAPE) == RailShape.ASCENDING_SOUTH;
				}
				case SOUTH : {
					return state.get(PoweredRailBlock.SHAPE) == RailShape.ASCENDING_NORTH;
				}
				case EAST : {
					return state.get(PoweredRailBlock.SHAPE) == RailShape.ASCENDING_WEST;
				}
				case WEST : {
					return state.get(PoweredRailBlock.SHAPE) == RailShape.ASCENDING_EAST;
				}
				default : {
					return false;
				}
			}
		}
		return false;
	}

	private static List<BlockPos> getNeighborsExcept(BlockPos pos, Direction except) {
		List<BlockPos> retVal = new ArrayList<>(5);
		for (Direction direction : Direction.values()) {
			if (direction == except.getOpposite()) {
				continue;
			}
			retVal.add(pos.offset(direction));
		}
		return retVal;
	}

	/*
		if block is observer updating block, then return position to not place until its finished
	 */
	private static Set<BlockPos> ObserverCantAvoidPos(MinecraftClient mc, World world, BlockPos pos) {
		//returns true if observer should be placed regardless of state
		BlockPos posOffset;
		Set<BlockPos> relatedPos = new HashSet<>(6);
		BlockState offsetStateSchematic;
		BlockState targetState = world.getBlockState(pos);
		Block block = targetState.getBlock();
		if (block instanceof ComparatorBlock || block instanceof RepeaterBlock || block instanceof FallingBlock ||
			block instanceof AbstractRailBlock || block instanceof RedstoneWireBlock || block instanceof DoorBlock ||
			((block instanceof WallMountedBlock) && targetState.get(WallMountedBlock.FACE) == WallMountLocation.FLOOR)) {
			//check downward
			if (world.getBlockState(pos.down()).isOf(Blocks.OBSERVER) && world.getBlockState(pos.down()).get(ObserverBlock.FACING) == Direction.UP) {
				relatedPos.add(pos.down(2));
				relatedPos.addAll(getNeighborsExcept(pos, Direction.DOWN));
				if (world.getBlockState(pos.down(3)).getBlock() instanceof PistonBlock) {
					relatedPos.add(pos.down(3));
				}
				return relatedPos;
			}
		}
		if (block instanceof NoteBlock) {
			if (!isNoteBlockInstrumentError(mc, world, pos)) {
				relatedPos.add(pos.down(2));
				relatedPos.addAll(getNeighborsExcept(pos, Direction.DOWN));
				if (world.getBlockState(pos.down(3)).getBlock() instanceof PistonBlock) {
					relatedPos.add(pos.down(3));
				}
				return relatedPos;
			}
		} else if (block instanceof DoorBlock) {
			if (!isDoorHingeError(mc, world, pos)) {
				relatedPos.add(pos.down(2));
				relatedPos.addAll(getNeighborsExcept(pos, Direction.DOWN));
				if (world.getBlockState(pos.down(3)).getBlock() instanceof PistonBlock) {
					relatedPos.add(pos.down(3));
				}
				return relatedPos;
			}
		}
		for (Direction direction : Direction.values()) {
			posOffset = pos.offset(direction);
			offsetStateSchematic = world.getBlockState(posOffset);
			if (offsetStateSchematic.isOf(Blocks.OBSERVER) && offsetStateSchematic.get(ObserverBlock.FACING) == direction.getOpposite()) {
				if (block instanceof WallBlock) {
					relatedPos.add(pos.offset(direction, 2));
					relatedPos.addAll(getNeighborsExcept(pos, direction));
					if (world.getBlockState(pos.offset(direction, 2).down()).getBlock() instanceof PistonBlock) {
						relatedPos.add(pos.offset(direction, 2).down());
					}
				} else if (block instanceof WallMountedBlock) {
					if (targetState.get(WallMountedBlock.FACE) == WallMountLocation.CEILING && direction == Direction.UP ||
						targetState.get(WallMountedBlock.FACE) == WallMountLocation.FLOOR && direction == Direction.DOWN ||
						targetState.get(WallMountedBlock.FACE) == WallMountLocation.WALL && targetState.get(WallMountedBlock.FACING) == direction.getOpposite()
					) {
						relatedPos.add(pos.offset(direction, 2));
						relatedPos.addAll(getNeighborsExcept(pos, direction));
						if (world.getBlockState(pos.offset(direction, 2).down()).getBlock() instanceof PistonBlock) {
							relatedPos.add(pos.offset(direction, 2).down());
						}
					}
				} else if (block instanceof PoweredRailBlock || block instanceof RedstoneWireBlock) {
					if (hasDustOrAscendingRails(world, direction.getOpposite(), pos)) {
						relatedPos.add(pos.offset(direction, 2));
						relatedPos.addAll(getNeighborsExcept(pos, direction));
					}
				}
			}
		}
		return relatedPos;
	}


	private static boolean shouldAvoidPlaceCart(BlockPos pos, World schematicWorld) {
		//avoids TNT priming
		for (Direction direction : Direction.values()) {
			if (schematicWorld.getBlockState(pos.down().offset(direction)).isOf(Blocks.TNT)) {
				return true;
			}
		}
		return false;
	}

	// returns should call continue in loop
	@SuppressWarnings({"ConstantConditions"})
	private static boolean placeCart(BlockState state, MinecraftClient client, BlockPos pos) {
		if (state.isOf(Blocks.DETECTOR_RAIL) && state.get(DetectorRailBlock.POWERED) != client.world.getBlockState(pos).get(DetectorRailBlock.POWERED) && canPickItem(client, Items.MINECART.getDefaultStack()) && client.player.getPos().distanceTo(Vec3d.of(pos)) < 4.5) {
			Vec3d clickPos = Vec3d.of(pos).add(0.5, 0.125, 0.5);
			if (!FakeAccurateBlockPlacement.canHandleOther(Items.MINECART)) {
				return false;
			}
			if (doSchematicWorldPickBlock(client, Items.MINECART.getDefaultStack())) {
				ActionResult actionResult = interactBlock(client, new BlockHitResult(clickPos, Direction.UP, pos, false)); //place block
				if (actionResult.isAccepted()) {
					cacheEasyPlacePosition(pos, false, 600);
					return true;
				}
			}
			return false;
		}
		return false;
	}

	@SuppressWarnings({"ConstantConditions"})
	private static void placeGrindStone(BlockState state, MinecraftClient client, BlockPos pos) {
		if (PRINTER_FAKE_ROTATION.getBooleanValue()) {
			FakeAccurateBlockPlacement.request(state, pos);
			return;
			//place in air
		}
		if (!canAttachGrindstone(state, client, pos) && !isFacingCorrectly(state, client.player)) {
			return;
		}
		Direction side = state.get(GrindstoneBlock.FACING);
		WallMountLocation location = state.get(GrindstoneBlock.FACE);
		BlockPos clickPos;
		Vec3d hitVec;
		if (canAttachGrindstone(state, client, pos)) {
			//offset positions
			if (location == WallMountLocation.CEILING) {
				clickPos = pos.up();
			} else if (location == WallMountLocation.FLOOR) {
				clickPos = pos.down();
			} else {
				clickPos = pos.offset(side.getOpposite());
			}
			hitVec = Vec3d.ofCenter(clickPos).add(Vec3d.of(side.getVector()).multiply(0.5));
			if (doSchematicWorldPickBlock(client, state, pos)) {
				cacheEasyPlacePosition(pos, false);
				interactBlock(client, new BlockHitResult(hitVec, side, clickPos, false)); //place block
			}
		} else {
			if (isFacingCorrectly(state, client.player)) {
				hitVec = Vec3d.ofCenter(pos);
				clickPos = pos;
				if (doSchematicWorldPickBlock(client, state, pos)) {
					cacheEasyPlacePosition(pos, false);
					interactBlock(client, new BlockHitResult(hitVec, side, clickPos, false)); //place block
				}
			}
		}
	}

	@SuppressWarnings({"ConstantConditions"})
	private static boolean canAttachGrindstone(BlockState state, MinecraftClient client, BlockPos pos) {
		if (PRINTER_FAKE_ROTATION.getBooleanValue()) {
			return true;
			//place in air.
		}
		Direction facing = state.get(WallMountedBlock.FACING);
		WallMountLocation location = state.get(WallMountedBlock.FACE);
		//case ceil
		if (location == WallMountLocation.CEILING) {
			return !client.world.getBlockState(pos.up()).getMaterial().isReplaceable() && client.player.getHorizontalFacing() == facing.getOpposite() && !hasGui(client.world.getBlockState(pos.up()).getBlock()) || client.player.shouldCancelInteraction();
		} else if (location == WallMountLocation.FLOOR) {
			return !client.world.getBlockState(pos.down()).getMaterial().isReplaceable() && client.player.getHorizontalFacing() == facing.getOpposite() && !hasGui(client.world.getBlockState(pos.down()).getBlock()) || client.player.shouldCancelInteraction();
		} else {
			return !client.world.getBlockState(pos.offset(facing.getOpposite())).getMaterial().isReplaceable() && !hasGui(client.world.getBlockState(pos.offset(facing.getOpposite())).getBlock()) || client.player.shouldCancelInteraction();
		}
	}

	private static boolean isFacingCorrectly(BlockState state, ClientPlayerEntity player) {
		//if we can't attach, use player's directions
		Direction facing = state.get(WallMountedBlock.FACING);
		WallMountLocation location = state.get(WallMountedBlock.FACE);
		Direction[] facingOrder = Direction.getEntityFacingOrder(player);
		if (location == WallMountLocation.CEILING) {
			//primary should be UP
			//secondary should be same as facing
			return facingOrder[0] == Direction.UP && player.getHorizontalFacing() == facing;
		} else if (location == WallMountLocation.FLOOR) {
			return facingOrder[0] == Direction.DOWN && player.getHorizontalFacing() == facing;
		} else {
			return facingOrder[0] == facing.getOpposite();
		}
	}

	private static void placeTrapDoor(BlockState state, MinecraftClient client, BlockPos pos) {
		//check if it can be clicked on face, then place inside block
		Direction side = state.get(TrapdoorBlock.FACING);
		BlockPos clickPos;
		Vec3d hitVec;
		if (client.world.getBlockState(pos.offset(side.getOpposite())).getMaterial().isReplaceable()) {
			//place inside block
			clickPos = pos;
			if (client.player.getHorizontalFacing().getOpposite() == side) {
				side = state.get(TrapdoorBlock.HALF) == BlockHalf.TOP ? Direction.DOWN : Direction.UP;
			} else {
				return;
			}
			hitVec = Vec3d.of(clickPos).add(0.5, 0.5, 0.5);
		} else {
			clickPos = pos.offset(side.getOpposite());
			hitVec = Vec3d.of(clickPos).add(0.5, 0.5, 0.5).add(Vec3d.of(side.getVector()).multiply(0.5));
		}
		if (doSchematicWorldPickBlock(client, state, pos)) {
			cacheEasyPlacePosition(pos, false);
			interactBlock(client, new BlockHitResult(hitVec, side, clickPos, false)); //place block
		}
	}

	private static boolean isNoteBlockInstrumentError(MinecraftClient mc, World world, BlockPos pos) {
		BlockState stateA = world.getBlockState(pos);
		BlockState stateB = mc.world.getBlockState(pos);
		return stateA.isOf(Blocks.NOTE_BLOCK) && stateB.isOf(Blocks.NOTE_BLOCK) &&
			stateA.get(NoteBlock.POWERED) == stateB.get(NoteBlock.POWERED) &&
			world.getBlockState(pos.down()).getMaterial().isReplaceable() == mc.world.getBlockState(pos.offset(Direction.DOWN)).getMaterial().isReplaceable();
	}

	private static boolean isDoorHingeError(MinecraftClient mc, World world, BlockPos pos) {
		BlockState stateA = world.getBlockState(pos);
		BlockState stateB = mc.world.getBlockState(pos);
		return stateA.contains(DoorBlock.HINGE) && stateB.contains(DoorBlock.HINGE) &&
			stateA.get(DoorBlock.POWERED) == stateB.get(DoorBlock.POWERED) &&
			stateA.get(DoorBlock.FACING) == stateB.get(DoorBlock.FACING) &&
			stateA.get(DoorBlock.OPEN) == stateB.get(DoorBlock.OPEN) &&
			stateA.get(DoorBlock.HALF) == stateB.get(DoorBlock.HALF);
	}

	private static boolean ObserverUpdateOrder(MinecraftClient mc, World world, BlockPos pos, Box selectedBox) {
		//returns true if observer should not be placed
		boolean ExplicitObserver = PRINTER_OBSERVER_AVOID_ALL.getBooleanValue();
		BlockState stateSchematic = world.getBlockState(pos);
		BlockPos posOffset;
		BlockState OffsetStateSchematic;
		BlockState OffsetStateClient;
		if (stateSchematic.get(ObserverBlock.POWERED)) {
			return false;
		}
		Direction facingSchematic = fi.dy.masa.malilib.util.BlockUtils.getFirstPropertyFacingValue(stateSchematic);
		assert facingSchematic != null;
		boolean observerCantAvoid = ObserverCantAvoid(mc, world, facingSchematic, pos);
		if (observerCantAvoid) {
			return false;
		}
		posOffset = pos.offset(facingSchematic);
		if (!isPositionWithinBox(selectedBox, posOffset)) {
			return false;
		}
		OffsetStateSchematic = world.getBlockState(posOffset);
		OffsetStateClient = mc.world.getBlockState(posOffset);
		if (OffsetStateSchematic.isOf(Blocks.BARRIER)) {
			return false;
		}
		if (OffsetStateSchematic.isOf(Blocks.OBSERVER) && OffsetStateSchematic.get(ObserverBlock.FACING) == facingSchematic.getOpposite()) {
			return false;
		}
		if (OffsetStateSchematic.getBlock() instanceof DoorBlock && OffsetStateClient.getBlock() instanceof DoorBlock &&
			OffsetStateSchematic.get(DoorBlock.POWERED) == OffsetStateClient.get(DoorBlock.POWERED) &&
			OffsetStateSchematic.get(DoorBlock.FACING) == OffsetStateClient.get(DoorBlock.FACING)) {
			return false;
		}
		if (ExplicitObserver) {
			if (OffsetStateSchematic.isOf(Blocks.BARRIER) || OffsetStateClient.isAir() && OffsetStateSchematic.isAir() || OffsetStateSchematic.isOf(Blocks.VOID_AIR)) {
				return false;
			} //cave air wtf
			if (isClientPowerError(mc, world, OffsetStateClient, OffsetStateSchematic, posOffset)) {
				return false;
			}
			return !OffsetStateSchematic.toString().equals(OffsetStateClient.toString());
		}
		return !OffsetStateClient.getBlock().equals(OffsetStateSchematic.getBlock());
	}

	private static BlockPos ObserverUpdateOrderPos(MinecraftClient mc, World world, BlockPos pos) {
		//returns true if observer should not be placed
		boolean ExplicitObserver = PRINTER_OBSERVER_AVOID_ALL.getBooleanValue();
		BlockState stateSchematic = world.getBlockState(pos);
		BlockPos posOffset;
		BlockState OffsetStateSchematic;
		BlockState OffsetStateClient;
		if (stateSchematic.get(ObserverBlock.POWERED)) {
			return null;
		}
		Direction facingSchematic = fi.dy.masa.malilib.util.BlockUtils.getFirstPropertyFacingValue(stateSchematic);
		assert facingSchematic != null;
		boolean observerCantAvoid = ObserverCantAvoid(mc, world, facingSchematic, pos);
		if (observerCantAvoid) {
			return null;
		}
		posOffset = pos.offset(facingSchematic);
		assert pos != posOffset;
		OffsetStateSchematic = world.getBlockState(posOffset);
		OffsetStateClient = mc.world.getBlockState(posOffset);
		if (OffsetStateSchematic.isOf(Blocks.BARRIER)) {
			return null;
		} else if (OffsetStateSchematic.isOf(Blocks.OBSERVER) && OffsetStateSchematic.get(ObserverBlock.FACING) == facingSchematic.getOpposite()) {
			return null;
		} else if (OffsetStateSchematic.getBlock() instanceof DoorBlock && OffsetStateClient.getBlock() instanceof DoorBlock &&
			OffsetStateSchematic.get(DoorBlock.POWERED) == OffsetStateClient.get(DoorBlock.POWERED) &&
			OffsetStateSchematic.get(DoorBlock.FACING) == OffsetStateClient.get(DoorBlock.FACING)) //hinge error
		{
			return null;
		}
		if (ExplicitObserver) {
			if (OffsetStateSchematic.isOf(Blocks.BARRIER) || OffsetStateClient.isAir() && OffsetStateSchematic.isAir()) {
				return null;
			} //cave air wtf
			if (!OffsetStateSchematic.toString().equals(OffsetStateClient.toString())) {
				if (isClientPowerError(mc, world, OffsetStateClient, OffsetStateSchematic, posOffset)) {
					return null;
				}
				return posOffset;
			}
		}

		if (!OffsetStateClient.getBlock().equals(OffsetStateSchematic.getBlock())) {
			return posOffset;
		}
		return null;
	}

	/*
	 * Checks if the block can be placed in the correct orientation if player is
	 * facing a certain direction Don't place block if orientation will be wrong
	 */
	private static boolean canPlaceFace(FacingData facedata, BlockState stateSchematic,
	                                    Direction primaryFacing, Direction horizontalFacing) {
		if (stateSchematic.isOf(Blocks.GRINDSTONE)) {
			return true;
		}
		Direction facing = fi.dy.masa.malilib.util.BlockUtils.getFirstPropertyFacingValue(stateSchematic);
		if (stateSchematic.getBlock() instanceof AbstractRailBlock) {
			facing = convertRailShapetoFace(stateSchematic);
		}
		if (facing != null && facedata != null) {
			// backward compatibility, JAVA_8 can't use enhanced switch
			switch (facedata.type) {
				case 0: // All directions (ie, observers and pistons)
					if (facedata.isReversed) {
						return facing.getOpposite() == primaryFacing;
					} else {
						return facing == primaryFacing;
					}

				case 1: // Only Horizontal directions (ie, repeaters and comparators)
					if (facedata.isReversed) {
						return facing.getOpposite() == horizontalFacing;
					} else {
						return facing == horizontalFacing;
					}
				case 2: // Wall mountable, such as a lever, only use player direction if not on wall.
					return stateSchematic.get(WallMountedBlock.FACE) == WallMountLocation.WALL
						|| (facing == horizontalFacing && stateSchematic.get(WallMountedBlock.FACE) == WallMountLocation.CEILING ? (primaryFacing == Direction.UP && horizontalFacing == stateSchematic.get(WallMountedBlock.FACING)) : (primaryFacing == Direction.DOWN && horizontalFacing == stateSchematic.get(WallMountedBlock.FACING)));
				case 3: //rotated, why, anvil, WNES order
					return horizontalFacing.rotateYClockwise() == facing;
				case 4: //rails
					return facing == horizontalFacing || facing == horizontalFacing.getOpposite();
				//return facing == horizontalFacing || facing == horizontalFacing.getOpposite();
				default: // Ignore rest -> TODO: Other blocks like anvils, etc...
					return true;
			}
		} else {
			if (stateSchematic.getBlock() instanceof TorchBlock && !(stateSchematic.getBlock() instanceof WallTorchBlock) && !(stateSchematic.getBlock() instanceof WallRedstoneTorchBlock)) {
				return Direction.DOWN == primaryFacing;
			}
			return true;
		}
	}

	private static boolean isReplaceableFluidSource(BlockState checkState) {
		return checkState.getBlock() instanceof FluidBlock && checkState.get(FluidBlock.LEVEL) == 0 ||
			checkState.getBlock() instanceof BubbleColumnBlock ||
			checkState.isOf(Blocks.SEAGRASS) || checkState.isOf(Blocks.TALL_SEAGRASS) ||
			checkState.getBlock() instanceof Waterloggable && checkState.get(Properties.WATERLOGGED) && checkState.getMaterial().isReplaceable();
	}

	static boolean isReplaceableWaterFluidSource(BlockState checkState) {
		return checkState.isOf(Blocks.SEAGRASS) || checkState.isOf(Blocks.TALL_SEAGRASS) ||
			checkState.isOf(Blocks.WATER) && checkState.contains(FluidBlock.LEVEL) && checkState.get(FluidBlock.LEVEL) == 0 ||
			checkState.getBlock() instanceof BubbleColumnBlock ||
			checkState.getBlock() instanceof Waterloggable && checkState.contains(Properties.WATERLOGGED) && checkState.get(Properties.WATERLOGGED) && checkState.getMaterial().isReplaceable();
	}

	private static boolean containsWaterloggable(BlockState state) {
		return state.getBlock() instanceof Waterloggable && state.get(Properties.WATERLOGGED);
	}

	private static boolean requiresMoreAction(BlockState stateSchematic, BlockState stateClient) {
		// Return true if current state requires more action to be taken
		Block blockSchematic = stateSchematic.getBlock();
		if (blockSchematic instanceof SeaPickleBlock && stateSchematic.get(SeaPickleBlock.PICKLES) > 1) {
			Block blockClient = stateClient.getBlock();

			if (blockClient instanceof SeaPickleBlock && !Objects.equals(stateClient.get(SeaPickleBlock.PICKLES), stateSchematic.get(SeaPickleBlock.PICKLES))) {
				return blockSchematic != blockClient;
			}
		}
		if (blockSchematic instanceof SnowBlock) {
			Block blockClient = stateClient.getBlock();

			if (blockClient instanceof SnowBlock && stateClient.get(SnowBlock.LAYERS) < stateSchematic.get(SnowBlock.LAYERS)) {
				return false;
			}
		}
		if (blockSchematic instanceof SlabBlock && stateSchematic.get(SlabBlock.TYPE) == SlabType.DOUBLE) {
			Block blockClient = stateClient.getBlock();

			if (blockClient instanceof SlabBlock && stateClient.get(SlabBlock.TYPE) != SlabType.DOUBLE) {
				return blockSchematic != blockClient;
			}
		}
		if (blockSchematic instanceof ComposterBlock && stateSchematic.get(ComposterBlock.LEVEL) > 0 && stateClient.getBlock() instanceof ComposterBlock) {
			return !Objects.equals(stateClient.get(ComposterBlock.LEVEL), stateSchematic.get(ComposterBlock.LEVEL));
		}
		Block blockClient = stateClient.getBlock();
		if (blockClient instanceof SnowBlock && stateClient.get(SnowBlock.LAYERS) < 3 && !(stateSchematic.getBlock() instanceof SnowBlock)) {
			return false;
		}
		if (stateClient.isOf(Blocks.DIRT) && stateSchematic.isOf(Blocks.DIRT_PATH)) {
			return true;
		}
		// finally
		return !stateClient.isAir() && !stateClient.getMaterial().isReplaceable();
	}

	/**
	 * Apply hit vectors (used to be Carpet hit vec protocol, but I think it is
	 * uneccessary now with orientation/states programmed in)
	 *
	 * @param pos   BlockPos
	 * @param state BlockState
	 * @param side  random side
	 * @return Vec3d
	 */
	public static Vec3d applyHitVec(BlockPos pos, BlockState state, Direction side) {

		double dx;
		double dy;
		double dz;
		Block block = state.getBlock();

		/*
		 * I don't know if this is needed, just doing to mimic client According to the
		 * MC protocol wiki, the protocol expects a 1 on a side that is clicked
		 */
		Vec3d clickPos = Vec3d.of(pos);
		if (!(block instanceof GrindstoneBlock) && block instanceof WallMountedBlock || block instanceof TorchBlock || block instanceof WallSkullBlock
			|| block instanceof LadderBlock
			|| block instanceof TripwireHookBlock || block instanceof WallSignBlock ||
			block instanceof EndRodBlock || block instanceof DeadCoralFanBlock) {
			if (block instanceof DeadCoralFanBlock && !(block instanceof DeadCoralWallFanBlock)) {
				side = Direction.UP;
				clickPos = Vec3d.ofCenter(pos.down()).add(Vec3d.of(side.getVector()).multiply(0.5));
			} else if (block instanceof TorchBlock && !(block instanceof WallTorchBlock) && !(block instanceof WallRedstoneTorchBlock)) {
				side = Direction.UP;
				clickPos = Vec3d.ofCenter(pos.down()).add(Vec3d.of(side.getVector()).multiply(0.5));
			} else if (side == null || state.contains(WallMountedBlock.FACE) && state.get(WallMountedBlock.FACE) != WallMountLocation.WALL) {
				if (state.contains(WallMountedBlock.FACE) && state.get(WallMountedBlock.FACE) == WallMountLocation.CEILING) {
					side = Direction.DOWN;
				} else {
					side = Direction.UP;
				}
				clickPos = clickPos.add(0.5, 0.5, 0.5).add(Vec3d.of(side.getVector()).multiply(0.5));
			} else {
				clickPos = clickPos.add(0.5, 0.5, 0.5).add(Vec3d.of(side.getVector()).multiply(0.5));
			}
			//We are here because we can't use protocol.
		}
		dx = clickPos.x;
		dy = clickPos.y;
		dz = clickPos.z;
		if (block instanceof StairsBlock) {
			if (state.get(StairsBlock.HALF) == BlockHalf.TOP) {
				dy += 0.9;
			} else {
				dy += 0;
			}
		} else if (block instanceof SlabBlock && state.get(SlabBlock.TYPE) != SlabType.DOUBLE) {
			if (state.get(SlabBlock.TYPE) == SlabType.TOP) {
				dy += 0.9;
			} else {
				dy += 0;
			}
		} else if (block instanceof TrapdoorBlock) {
			if (state.get(TrapdoorBlock.HALF) == BlockHalf.TOP) {
				dy += 0.9;
			} else {
				dy += 0;
			}
		}
		return new Vec3d(dx, dy, dz);
	}

	private static boolean canBypass(MinecraftClient mc, World world, BlockPos pos) {
		Direction direction = world.getBlockState(pos).get(ObserverBlock.FACING);
		BlockPos posOffset = pos.offset(direction.getOpposite());
		return world.getBlockState(posOffset) == null || world.getBlockState(posOffset).isAir() || (!hasPowerRelatedState(mc.world.getBlockState(posOffset).getBlock())) && mc.world.getBlockState(posOffset).getBlock().getName() == world.getBlockState(posOffset).getBlock().getName();

	}

	private static boolean hasGui(Block checkGui) {
		return checkGui instanceof CraftingTableBlock || checkGui instanceof GrindstoneBlock || checkGui instanceof LeverBlock || checkGui instanceof TrapdoorBlock ||
			checkGui instanceof ButtonBlock || checkGui instanceof DoorBlock || checkGui instanceof FenceGateBlock ||
			checkGui instanceof BedBlock || checkGui instanceof NoteBlock || checkGui instanceof BlockWithEntity;
	}

	private static boolean hasPowerRelatedState(Block block) {
		return block instanceof LeavesBlock || block instanceof FluidBlock || block instanceof ObserverBlock || block instanceof PistonBlock || block instanceof PoweredRailBlock || block instanceof DetectorRailBlock ||
			block instanceof DispenserBlock || block instanceof AbstractRedstoneGateBlock || block instanceof LeverBlock || block instanceof TrapdoorBlock || block instanceof RedstoneTorchBlock ||
			block instanceof DoorBlock || block instanceof RedstoneWireBlock || block instanceof RedstoneOreBlock || block instanceof RedstoneLampBlock || block instanceof NoteBlock || block instanceof FenceGateBlock ||
			block instanceof ScaffoldingBlock;
	}

	/*
		returns if its block that can update neighbors
	 */
	private static boolean hasWrongStateNearby(MinecraftClient mc, World schematicWorld, BlockPos pos) {
		for (Direction direction : Direction.values()) {
			BlockPos checkPos = pos.offset(direction);
			if (hasPowerRelatedState(schematicWorld.getBlockState(checkPos).getBlock()) && schematicWorld.getBlockState(checkPos) != mc.world.getBlockState(checkPos)) {
				return true;
			}
		}
		return false;
	}

	private static String hasWrongStateNearbyReason(MinecraftClient mc, World schematicWorld, BlockPos pos) {
		for (Direction direction : Direction.values()) {
			BlockPos checkPos = pos.offset(direction);
			if (hasPowerRelatedState(schematicWorld.getBlockState(checkPos).getBlock()) && schematicWorld.getBlockState(checkPos) != mc.world.getBlockState(checkPos)) {
				return "!" + checkPos.toShortString() + " STATE " + schematicWorld.getBlockState(checkPos).toString() + " does not match with current state : " + mc.world.getBlockState(checkPos).toString() + "!";
			}
		}
		return null;
	}

	private static BlockPos hasWrongStateNearbyPos(MinecraftClient mc, World schematicWorld, BlockPos pos) {
		for (Direction direction : Direction.values()) {
			BlockPos checkPos = pos.offset(direction);
			if (hasPowerRelatedState(schematicWorld.getBlockState(checkPos).getBlock()) && schematicWorld.getBlockState(checkPos) != mc.world.getBlockState(checkPos)) {
				return checkPos;
			}
		}
		return null;
	}

	public static Vec3d applyTorchHitVec(BlockPos pos, Vec3d hitVecIn, Direction side) {
		double x = pos.getX();
		double y = pos.getY();
		double z = pos.getZ();

		double dx = hitVecIn.getX();
		double dy = hitVecIn.getY();
		double dz = hitVecIn.getZ();
		if (side == Direction.UP) {
			dy = 1;
		} else if (side == Direction.DOWN) {
			dy = -1;
		} else if (side == Direction.EAST) {
			dx = 1;
		} else if (side == Direction.WEST) {
			dx = -1;
		} else if (side == Direction.SOUTH) {
			dz = 1;
		} else if (side == Direction.NORTH) {
			dz = -1;
		}
		return new Vec3d(x + dx, y + dy, z + dz);
	}

	private static void updateSignText(MinecraftClient mc, World schematicWorld, BlockPos pos) {
		if (isPositionCached(pos, false)) {
			return;
		}
		if (mc.currentScreen instanceof SignEditScreen || !schematicWorld.getBlockState(pos).isIn(BlockTags.SIGNS) || signCache.contains(pos.asLong())) {
			return;
		}
		BlockEntity entity = schematicWorld.getBlockEntity(pos);
		if (entity == null) {
			return;
		}
		BlockEntity clientEntity = mc.world.getBlockEntity(pos);
		if (clientEntity == null) {
			return;
		}
		//#if MC>=11900
		if (entity instanceof SignBlockEntity signBlockEntity && clientEntity instanceof SignBlockEntity clientSignEntity) {
			if (clientSignEntity.getTextOnRow(0, false).getContent() != TextContent.EMPTY || clientSignEntity.getTextOnRow(1, false).getContent() != TextContent.EMPTY ||
				clientSignEntity.getTextOnRow(2, false).getContent() != TextContent.EMPTY ||
				clientSignEntity.getTextOnRow(3, false).getContent() != TextContent.EMPTY) {
				MessageHolder.sendDebugMessage("Text already exists in " + pos.toShortString());
				signCache.add(pos.asLong());
				return;
			}
			MessageHolder.sendDebugMessage("Tries to copy sign text in " + pos.toShortString());
			signCache.add(pos.asLong());
			mc.getNetworkHandler().sendPacket(new UpdateSignC2SPacket(signBlockEntity.getPos(), signBlockEntity.getTextOnRow(0, false).getString(), signBlockEntity.getTextOnRow(1, false).getString(), signBlockEntity.getTextOnRow(2, false).getString(), signBlockEntity.getTextOnRow(3, false).getString()));
		}
		//#elseif MC>=11700
		//$$if (entity instanceof SignBlockEntity signBlockEntity && clientEntity instanceof SignBlockEntity clientSignEntity) {
		//$$	if (clientSignEntity.getTextOnRow(0, false) != LiteralText.EMPTY || clientSignEntity.getTextOnRow(1, false) != LiteralText.EMPTY ||
		//$$		clientSignEntity.getTextOnRow(2, false) != LiteralText.EMPTY ||
		//$$		clientSignEntity.getTextOnRow(3, false) != LiteralText.EMPTY) {
		//$$		MessageHolder.sendDebugMessage("Text already exists in " + pos.toShortString());
		//$$		signCache.add(pos.asLong());
		//$$		return;
		//$$	}
		//$$	MessageHolder.sendDebugMessage("Tries to copy sign text in " + pos.toShortString());
		//$$	signCache.add(pos.asLong());
		//$$	mc.getNetworkHandler().sendPacket(new UpdateSignC2SPacket(signBlockEntity.getPos(), signBlockEntity.getTextOnRow(0, false).getString(), signBlockEntity.getTextOnRow(1, false).getString(), signBlockEntity.getTextOnRow(2, false).getString(), signBlockEntity.getTextOnRow(3, false).getString()));
		//$$}
		//#else
		//$$if (entity instanceof SignBlockEntity && clientEntity instanceof SignBlockEntity) {
		//$$	SignBlockEntity signBlockEntity = (SignBlockEntity) entity;
		//$$	SignBlockEntity clientSignEntity = (SignBlockEntity) clientEntity;
		//$$	if (clientSignEntity.getTextOnRow(0) != LiteralText.EMPTY || clientSignEntity.getTextOnRow(1) != LiteralText.EMPTY ||
		//$$		clientSignEntity.getTextOnRow(2) != LiteralText.EMPTY ||
		//$$		clientSignEntity.getTextOnRow(3) != LiteralText.EMPTY) {
		//$$		MessageHolder.sendDebugMessage("Text already exists in " + pos.toShortString());
		//$$		signCache.add(pos.asLong());
		//$$		return;
		//$$	}
		//$$	MessageHolder.sendDebugMessage("Tries to copy sign text in " + pos.toShortString());
		//$$	signCache.add(pos.asLong());
		//$$	mc.getNetworkHandler().sendPacket(new UpdateSignC2SPacket(signBlockEntity.getPos(), signBlockEntity.getTextOnRow(0).getString(), signBlockEntity.getTextOnRow(1).getString(), signBlockEntity.getTextOnRow(2).getString(), signBlockEntity.getTextOnRow(3).getString()));
		//$$ }
		//#endif
	}

	/*
	 * Gets the direction necessary to build the block oriented correctly. TODO:
	 * Need a better way to do this.
	 */
	private static Boolean IsBlockSupportedCarpet(Block SchematicBlock) {
		if (SchematicBlock instanceof WallMountedBlock || SchematicBlock instanceof WallSkullBlock ||
			SchematicBlock instanceof AbstractRailBlock || SchematicBlock instanceof TorchBlock || SchematicBlock instanceof DeadCoralFanBlock) {
			return false;
		}
		return ADVANCED_ACCURATE_BLOCK_PLACEMENT.getBooleanValue() || SchematicBlock instanceof GlazedTerracottaBlock || SchematicBlock instanceof ObserverBlock || SchematicBlock instanceof RepeaterBlock || SchematicBlock instanceof TrapdoorBlock ||
			SchematicBlock instanceof ComparatorBlock || SchematicBlock instanceof DispenserBlock || SchematicBlock instanceof PistonBlock || SchematicBlock instanceof StairsBlock;
	} //Current carpet extra does not handle other facingBlocks, gnembon please update it

	static Direction applyPlacementFacing(BlockState stateSchematic, Direction side, BlockState stateClient) {
		Block blockSchematic = stateSchematic.getBlock();
		Block blockClient = stateClient.getBlock();

		if (blockSchematic instanceof SlabBlock) {
			if (stateSchematic.get(SlabBlock.TYPE) == SlabType.DOUBLE && blockClient instanceof SlabBlock
				&& stateClient.get(SlabBlock.TYPE) != SlabType.DOUBLE) {
				if (stateClient.get(SlabBlock.TYPE) == SlabType.TOP) {
					return Direction.DOWN;
				} else {
					return Direction.UP;
				}
			}
			// Single slab
			else {
				return Direction.NORTH;
			}
		} else if (/*blockSchematic instanceof LogBlock ||*/ blockSchematic instanceof PillarBlock) {
			Direction.Axis axis = stateSchematic.get(PillarBlock.AXIS);
			// Logs and pillars only have 3 directions that are important
			if (axis == Direction.Axis.X) {
				return Direction.WEST;
			} else if (axis == Direction.Axis.Y) {
				return Direction.DOWN;
			} else if (axis == Direction.Axis.Z) {
				return Direction.NORTH;
			}

		} else if (blockSchematic instanceof WallSignBlock) {
			return stateSchematic.get(WallSignBlock.FACING);
		} else if (blockSchematic instanceof WallSkullBlock) {
			return stateSchematic.get(WallSignBlock.FACING);
		} else if (blockSchematic instanceof SignBlock) {
			return Direction.UP;
		} else if (blockSchematic instanceof WallMountedBlock) {
			WallMountLocation location = stateSchematic.get(WallMountedBlock.FACE);
			if (location == WallMountLocation.FLOOR) {
				return Direction.UP;
			} else if (location == WallMountLocation.CEILING) {
				return Direction.DOWN;
			} else {
				return stateSchematic.get(WallMountedBlock.FACING);
			}
		} else if (blockSchematic instanceof DeadCoralWallFanBlock) {
			return stateSchematic.get(DeadCoralWallFanBlock.FACING);
		} else if (blockSchematic instanceof DeadCoralFanBlock) {
			return Direction.UP;
		} else if (blockSchematic instanceof HopperBlock) {
			return stateSchematic.get(HopperBlock.FACING).getOpposite();
		//#if MC>=11700
		} else if (blockSchematic instanceof LightningRodBlock) {
			return stateSchematic.get(LightningRodBlock.FACING);
		//#endif
		}  else if (stateSchematic.isIn(BlockTags.SHULKER_BOXES)) {
			return stateSchematic.get(ShulkerBoxBlock.FACING);
		} else if (blockSchematic instanceof TorchBlock) {
			if (blockSchematic instanceof WallTorchBlock || blockSchematic instanceof WallRedstoneTorchBlock) {
				return stateSchematic.get(WallTorchBlock.FACING);
			} else {
				return Direction.UP;
			}
		} else if (blockSchematic instanceof LadderBlock) {
			return stateSchematic.get(LadderBlock.FACING);
		} else if (blockSchematic instanceof TrapdoorBlock) {
			if (PRINTER_ACCURATE_BLOCK_PLACEMENT.getBooleanValue()) {
				return Direction.UP; //Placement State fixing first
			}
			return stateSchematic.get(TrapdoorBlock.FACING);
		} else if (blockSchematic instanceof TripwireHookBlock) {
			return stateSchematic.get(TripwireHookBlock.FACING);
		} else if (blockSchematic instanceof EndRodBlock) {
			return stateSchematic.get(EndRodBlock.FACING);
		} else if (blockSchematic instanceof AnvilBlock) {
			if (ADVANCED_ACCURATE_BLOCK_PLACEMENT.getBooleanValue() || PRINTER_ACCURATE_BLOCK_PLACEMENT.getBooleanValue() && IsBlockSupportedCarpet(blockSchematic)) {
				return stateSchematic.get(AnvilBlock.FACING);
			}
			return stateSchematic.get(AnvilBlock.FACING).rotateYCounterclockwise();
		} else if (blockSchematic instanceof AbstractRailBlock) {
			return convertRailShapetoFace(stateSchematic);
		}

		// TODO: Add more for other blocks
		return side;
	}

	public static Direction convertRailShapetoFace(BlockState state) {
		String RailShape;
		if (state.getBlock() instanceof RailBlock) {
			RailShape = state.get(RailBlock.SHAPE).toString();
		} else {
			RailShape = state.get(PoweredRailBlock.SHAPE).toString();
		}
		if (RailShape.contains("east") || RailShape.contains("west")) {
			return Direction.EAST;
		} else {
			return Direction.NORTH;
		}
	}

	public static boolean isPositionCached(BlockPos pos, boolean useClicked) {
		long currentTime = System.nanoTime();
		boolean cached = false;
		//#if MC>=11900
		for (Pair<Long, Boolean> keys : List.copyOf(positionCache.keySet())) {
		//#else
		//$$ ArrayList<Pair<Long, Boolean>> keySet = new ArrayList<>(positionCache.keySet());
		//$$ for (Pair<Long, Boolean> keys : keySet) {
		//#endif
			PositionCache val = positionCache.get(keys);
			boolean expired = val.hasExpired(currentTime);

			if (expired) {
				positionCache.remove(keys);
			} else if (val.getPos().equals(pos)) {
				// Item placement and "using"/"clicking" (changing delay for repeaters) are
				// diffferent
				if (!useClicked || val.hasClicked) {
					cached = true;
				}
				// Keep checking and removing old entries if there are a fair amount
				if (positionCache.size() < 16) {
					break;
				}
			}
		}
		return cached;
	}

	public static void cacheEasyPlacePosition(BlockPos pos, boolean useClicked) {
		PositionCache item = new PositionCache(pos, System.nanoTime(), useClicked ? EASY_PLACE_CACHE_TIME.getIntegerValue() * 1000000L : 2800000000L);
		// TODO: Create a separate cache for clickable items, as this just makes
		// duplicates
		if (useClicked) {
			item.hasClicked = true;
		}
		Pair<Long, Boolean> entry = new Pair<>(pos.asLong(), useClicked);
		if (positionCache.containsKey(entry)) {
			PositionCache value = positionCache.get(entry);
			if (item.timeout > value.timeout) {
				positionCache.put(entry, item);
			}
		} else {
			positionCache.put(entry, item);
		}
	}

	public static void cacheEasyPlacePosition(BlockPos pos, boolean useClicked, int miliseconds) {
		PositionCache item = new PositionCache(pos, System.nanoTime(), miliseconds * 1000000L);
		// TODO: Create a separate cache for clickable items, as this just makes
		// duplicates
		if (useClicked) {
			item.hasClicked = true;
		}
		Pair<Long, Boolean> entry = new Pair<>(pos.asLong(), useClicked);
		if (positionCache.containsKey(entry)) {
			PositionCache value = positionCache.get(entry);
			if (item.timeout > value.timeout) {
				positionCache.put(entry, item);
			}
		} else {
			positionCache.put(entry, item);
		}
	}

	public static Vec3d applyCarpetProtocolHitVec(BlockPos pos, BlockState state) {
		//#if MC>=11700
		if (Configs.Generic.EASY_PLACE_PROTOCOL.getOptionListValue() == EasyPlaceProtocol.V3) {
		//#else
		//$$ if (Configs.Generic.EASY_PLACE_PROTOCOL_V3.getBooleanValue()) {
		//#endif
			return WorldUtils.applyPlacementProtocolV3(pos, state, new Vec3d(pos.getX(), pos.getY(), pos.getZ()));
		}

		double code = 0;
		double y = pos.getY();
		double z = pos.getZ();
		Block block = state.getBlock();
		Direction facing = fi.dy.masa.malilib.util.BlockUtils.getFirstPropertyFacingValue(state);
		int railEnumCode = getRailShapeOrder(state);
		final int propertyIncrement = 16;
		if (facing == null && railEnumCode == 32 && !(block instanceof SlabBlock)) {
			return new Vec3d(pos.getX(), y, z);
		}
		if (facing != null) {
			code = facing.getId();
		} else if (railEnumCode != 32) {
			code = railEnumCode;
		}
		if (block instanceof RepeaterBlock) {
			code += ((state.get(RepeaterBlock.DELAY))) * (propertyIncrement);
		} else if (block instanceof TrapdoorBlock && state.get(TrapdoorBlock.HALF) == BlockHalf.TOP) {
			code += propertyIncrement;
		} else if (block instanceof ComparatorBlock && state.get(ComparatorBlock.MODE) == ComparatorMode.SUBTRACT) {
			code += propertyIncrement;
		} else if (block instanceof StairsBlock && state.get(StairsBlock.HALF) == BlockHalf.TOP) {
			code += propertyIncrement;
		} else if (block instanceof SlabBlock && state.get(SlabBlock.TYPE) != SlabType.DOUBLE) {
			if (state.get(SlabBlock.TYPE) == SlabType.TOP) //side should not be down
			{
				y += 0.9;
				//code += propertyIncrement; //slab type by protocol soon?
			}
		}
		if (code >= 0) {
			return new Vec3d(code * 2 + 2 + pos.getX(), y, z);
		}
		return new Vec3d(pos.getX(), y, z);
	}

	public static Integer getRailShapeOrder(BlockState state) {
		Block stateBlock = state.getBlock();
		if (stateBlock instanceof AbstractRailBlock) {
			if (stateBlock instanceof RailBlock) {
				return state.get(RailBlock.SHAPE).ordinal();
			} else if (stateBlock instanceof DetectorRailBlock) {
				return state.get(DetectorRailBlock.SHAPE).ordinal();
			} else {
				return state.get(PoweredRailBlock.SHAPE).ordinal();
			}
		} else {
			return 32;
		}
	}


	public static class PositionCache {
		private final BlockPos pos;
		private final long timeout;
		public boolean hasClicked = false;

		private PositionCache(BlockPos pos, long time, long timeout) {
			this.pos = pos;
			this.timeout = time + timeout;
		}

		public BlockPos getPos() {
			return this.pos;
		}

		public boolean hasExpired(long currentTime) {
			return currentTime > this.timeout;
		}
	}
}
