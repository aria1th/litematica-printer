package io.github.eatmyvenom.litematicin.utils;

//see https://github.com/senseiwells/EssentialClient/blob/1.19.x/src/main/java/me/senseiwells/essentialclient/feature/BetterAccurateBlockPlacement.java

import fi.dy.masa.litematica.materials.MaterialCache;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import io.github.eatmyvenom.litematicin.LitematicaMixinMod;
import net.minecraft.block.*;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.WallMountLocation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.state.property.Properties;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Date;
import java.util.HashSet;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import static io.github.eatmyvenom.litematicin.LitematicaMixinMod.*;

public class FakeAccurateBlockPlacement {

	// We implement FIFO Queue structure with responsible ticks.
	// By config, we define 'wait tick' between block placements
	public static Direction fakeDirection = null;
	public static boolean shouldReturnValue = false;
	public static int requestedTicks = -3;
	public static float fakeYaw = 0;
	public static float fakePitch = 0;
	private static BlockState stateGrindStone = null;
	private static float previousFakeYaw = 0;
	private static float previousFakePitch = 0;
	private static int tickElapsed = 0;
	private static int blockPlacedInTick = 0;
	private static BlockState handlingState = null;
	public static Item currentHandling = Items.AIR;
	private static final Queue<PosWithBlock> waitingQueue = new ArrayBlockingQueue<>(1) {
	};
	private static final HashSet<Block> warningSet = new HashSet<>();

	// Cancel when handling
	public static boolean isHandling() {
		return requestedTicks >= 0;
	}

	public static boolean canHandleOther() {
		return currentHandling == null || currentHandling == Items.AIR;
	}

	/*
		returns if item can be handled
	 */
	public static boolean canHandleOther(Item item) {
		if (canHandleOther()) {
			return true;
		}
		return currentHandling == item;
	}


	//I can implement anti-anti cheat, because anti cheats are just checking rotations being too accurate / fast, just interpolating is enough...
	//But I won't. Just follow server rules :shrug:
	public static void tick(ClientPlayNetworkHandler clientPlayNetworkHandler, ClientPlayerEntity playerEntity) {
		tickElapsed = 0;
		if (playerEntity == null || clientPlayNetworkHandler == null) {
			requestedTicks = -3;
			handlingState = null;
			fakeDirection = null;
			return;
		}
		if (requestedTicks >= -1) {
			if (fakeYaw != previousFakeYaw || fakePitch != previousFakePitch) {
				sendLookPacket(clientPlayNetworkHandler, playerEntity);
				previousFakePitch = fakePitch;
				previousFakeYaw = fakeYaw;
			}
			//we send this at last tick
		}
		if (requestedTicks <= -1) {
			currentHandling = Items.AIR;
			stateGrindStone = null;
			handlingState = null;
		}
		if (requestedTicks <= -3) {
			requestedTicks = -3;
			fakeDirection = null;
			previousFakePitch = playerEntity.getPitch();
			previousFakeYaw = playerEntity.getYaw();
		}
		if (requestedTicks == 0 && PRINTER_ONLY_FAKE_ROTATION_MODE.getBooleanValue()){
			placeFromQueue();
		}
		requestedTicks = requestedTicks - 1;
		blockPlacedInTick = 0;
	}
	public static void placeFromQueue() {
		if (requestedTicks != 0) {
			return;
		}
		PosWithBlock obj = waitingQueue.poll();
		if (obj != null) {
			MessageHolder.sendOrderMessage("found block to place");
			if (canPlace(obj.blockState, obj.pos)) {
				placeBlock(obj.pos, obj.blockState);
				return;
			}
			else {
				MessageHolder.sendOrderMessage("found block to place but can't place");
			}
		}
		waitingQueue.clear();
	}
	public static boolean emptyWaitingQueue() {
		if (requestedTicks != 0) {
			return false;
		}
		PosWithBlock obj = waitingQueue.poll();
		if (obj != null) {
			if (canPlace(obj.blockState, obj.pos)) {
				return placeBlock(obj.pos, obj.blockState);
			}
		}
		waitingQueue.clear();
		return false;
	}

	public static void sendLookPacket(ClientPlayNetworkHandler networkHandler, ClientPlayerEntity playerEntity) {
		networkHandler.sendPacket(
			new PlayerMoveC2SPacket.LookAndOnGround(
				fakeYaw,
				fakePitch,
				playerEntity.isOnGround()
			)
		);
		//System.out.print(fakeYaw);
		//System.out.print(fakePitch);
	}

	/*
	Pure request function by yaw pitch direction
	 */
	public static boolean request(float yaw, float pitch, Direction direction, int duration, boolean force) {
		if (isHandling()) {
			if (!force) {
				return false;
			}
		}
		fakeDirection = direction;
		fakeYaw = yaw;
		fakePitch = pitch;
		requestedTicks = duration;
		// we might need it instantly
		final MinecraftClient minecraftClient = MinecraftClient.getInstance();
		final ClientPlayNetworkHandler networkHandler = minecraftClient.getNetworkHandler();
		final ClientPlayerEntity playerEntity = minecraftClient.player;
		if (networkHandler != null && playerEntity != null) {
			sendLookPacket(networkHandler, playerEntity);
			return true;
		} else {
			return false;
		}
	}

	private static boolean canPlaceWallMounted(BlockState blockState) {
		if (blockState.getBlock() instanceof TorchBlock) {
			if (blockState.getBlock() instanceof WallTorchBlock || blockState.getBlock() instanceof WallRedstoneTorchBlock) {
				return fakeDirection == blockState.get(WallTorchBlock.FACING).getOpposite();
			}
			return fakeDirection == Direction.DOWN;
		}
		if (blockState.getBlock() instanceof WallMountedBlock) {
			//so we have 2 properties, looking at down / up as first direction, horizontals as second direction.
			WallMountLocation location = blockState.get(WallMountedBlock.FACE);
			if (location == WallMountLocation.WALL) {
				return true;
			}
			Direction facingSecond = blockState.get(WallMountedBlock.FACING);
			return fakeDirection == facingSecond;
		} else {
			return true;
		}
	}

	private static boolean requestGrindStone(BlockState state, BlockPos blockPos) {
		Direction facing = state.get(WallMountedBlock.FACING);
		WallMountLocation location = state.get(WallMountedBlock.FACE);
		float fy = 0;
		float fp = 0;
		Direction lookRefdir;
		if (location == WallMountLocation.CEILING) {
			//primary should be UP
			//secondary should be same as facing
			fp = -90;
			lookRefdir = facing;
		} else if (location == WallMountLocation.FLOOR) {
			fp = 90;
			lookRefdir = facing;
		} else {
			fp = 0;
			lookRefdir = facing.getOpposite();
		}
		if (lookRefdir == Direction.EAST) {
			fy = -87;
		} else if (lookRefdir == Direction.WEST) {
			fy = 87;
		} else if (lookRefdir == Direction.NORTH) {
			fy = 177;
		} else if (lookRefdir == Direction.SOUTH) {
			fy = 3;
		}
		if (isHandling()) {
			if (requestedTicks == 0 && stateGrindStone != null && canPlace(state, blockPos)) {
				//instant place
				placeBlock(blockPos, state);
				return true;
			}
			return false;
		}
		stateGrindStone = state;
		if (waitingQueue.isEmpty()) {
			if (waitingQueue.offer(new PosWithBlock(blockPos, state))) {
				request(fy, fp, lookRefdir, LitematicaMixinMod.FAKE_ROTATION_TICKS.getIntegerValue(), false);
			}
			return true;
		}
		return false;
	}

	public static Direction getPlayerFacing() {
		if (fakeYaw == -87) {
			return Direction.EAST;
		} else if (fakeYaw == 87) {
			return Direction.WEST;
		} else if (fakeYaw == 177) {
			return Direction.NORTH;
		} else if (fakeYaw == 3) {
			return Direction.SOUTH;
		}
		return null;
	}

	public static Direction[] getFacingOrder() {
		float theta = fakePitch * 0.017453292F;
		float omega = -fakeYaw * 0.017453292F;
		float unitHorizontal = MathHelper.cos(theta);
		float yVector = -MathHelper.sin(theta);
		float xVector = unitHorizontal * MathHelper.sin(omega);
		float zVector = unitHorizontal * MathHelper.cos(omega);
		float yScalar = Math.abs(yVector);
		float xScalar = Math.abs(xVector);
		float zScalar = Math.abs(zVector);
		Direction directionX = xVector > 0.0F ? Direction.EAST : Direction.WEST;
		Direction directionY = yVector > 0.0F ? Direction.UP : Direction.DOWN;
		Direction directionZ = zVector > 0.0F ? Direction.SOUTH : Direction.NORTH;
		if (xScalar > zScalar) {
			if (yScalar > xScalar) {
				return listClosest(directionY, directionX, directionZ);
			} else {
				return zScalar > yScalar ? listClosest(directionX, directionZ, directionY) : listClosest(directionX, directionY, directionZ);
			}
		} else if (yScalar > zScalar) {
			return listClosest(directionY, directionZ, directionX);
		} else {
			return xScalar > yScalar ? listClosest(directionZ, directionX, directionY) : listClosest(directionZ, directionY, directionX);
		}
	}

	private static Direction[] listClosest(Direction first, Direction second, Direction third) {
		return new Direction[]{first, second, third, third.getOpposite(), second.getOpposite(), first.getOpposite()};
	}

	/***
	 *
	 * @param blockState : Block object(terracotta, etc...)
	 * @param blockPos : Block Position
	 * @return boolean : if its registered and just can place it.
	 * example : boolean canContinue = FakeAccurateBlockPlacement.request(SchematicState, BlockPos)
	 */
	synchronized public static boolean request(BlockState blockState, BlockPos blockPos) {
		// instant
		if (!canPlace(blockState, blockPos) || blockState.isAir() || MaterialCache.getInstance().getRequiredBuildItemForState(blockState, SchematicWorldHandler.getSchematicWorld(), blockPos).getItem() == Items.AIR) {
			MessageHolder.sendOrderMessage("Cannot place "+ blockState.toString() + " at " + blockPos.toShortString());
			return false;
		}
		if (blockState.isOf(Blocks.GRINDSTONE)) {
			return requestGrindStone(blockState, blockPos);
		}
		if (blockState.isOf(Blocks.HOPPER) || blockState.isIn(BlockTags.SHULKER_BOXES) || blockState.isOf(Blocks.LIGHTNING_ROD) || blockState.isOf(Blocks.END_ROD)) {
			placeBlock(blockPos, blockState);
			return true;
		}
		if (!blockState.contains(Properties.FACING) && !blockState.contains(Properties.HORIZONTAL_FACING) && !(blockState.getBlock() instanceof AbstractRailBlock) && !(blockState.getBlock() instanceof TorchBlock)) {
			placeBlock(blockPos, blockState);
			return true; //without facing properties
		}
		FacingData facingData = FacingData.getFacingData(blockState);
		if (facingData == null && !(blockState.getBlock() instanceof AbstractRailBlock) && !(blockState.getBlock() instanceof TorchBlock)) {
			if (!warningSet.contains(blockState.getBlock())) {
				warningSet.add(blockState.getBlock());
				System.out.printf("WARN : Block %s is not found\n", blockState.getBlock().toString());
			}
			placeBlock(blockPos, blockState);
			return true;
		}
		Direction facing = fi.dy.masa.malilib.util.BlockUtils.getFirstPropertyFacingValue(blockState); //facing of block itself
		if (facing == null && blockState.getBlock() instanceof AbstractRailBlock) {
			facing = Printer.convertRailShapetoFace(blockState);
		} else if (blockState.getBlock() instanceof TorchBlock) {
			if (blockState.getBlock() instanceof WallTorchBlock || blockState.getBlock() instanceof WallRedstoneTorchBlock) {
				facing = blockState.get(WallTorchBlock.FACING).getOpposite();
			} else {
				facing = Direction.DOWN;
			}
		}
		if (facing == null) {
			//System.out.println(blockState);
			placeBlock(blockPos, blockState);
			return true;
		}
		//assume player is looking at north
		boolean reversed = facingData != null && facingData.isReversed;
		int order = facingData == null ? 0 : facingData.type;
		Direction direction1 = facing;
		float fy = 0, fp = 12;
		if (order == 0 || order == 1) {
			direction1 = reversed ? facing.getOpposite() : facing;
		} else if (order == 2) {
			facing = blockState.get(WallMountedBlock.FACING);
			direction1 = blockState.contains(WallMountedBlock.FACE) && blockState.get(WallMountedBlock.FACE) == WallMountLocation.WALL ? facing.getOpposite() : facing;
			if (blockState.get(WallMountedBlock.FACE) == WallMountLocation.CEILING) {
				fp = -90;
			} else if (blockState.get(WallMountedBlock.FACE) == WallMountLocation.FLOOR) {
				fp = 90;
			} else {
				fp = 12;
			}
		} else if (order == 3) {
			direction1 = facing.rotateYCounterclockwise();
		}
		if (order != 2 && (direction1 == null || (requestedTicks == 0 && fakeDirection == direction1 && fy == fakeYaw && fp == fakePitch)) && canPlaceWallMounted(blockState)) {
			placeBlock(blockPos, blockState);
			return true;
		}
		Direction lookRefdir = direction1;
		if (lookRefdir == Direction.UP) {
			fp = -90;
		} else if (lookRefdir == Direction.DOWN) {
			fp = 90;
		} else if (lookRefdir == Direction.EAST) {
			fy = -87;
		} else if (lookRefdir == Direction.WEST) {
			fy = 87;
		} else if (lookRefdir == Direction.NORTH) {
			fy = 177;
		} else if (lookRefdir == Direction.SOUTH) {
			fy = 3;
		} else {
			fy = 0;
			fp = 12;
		}
		if (LitematicaMixinMod.FAKE_ROTATION_TICKS.getIntegerValue() == 0) {
			//instant place
			if (lookRefdir != fakeDirection) {
				if (tickElapsed > LitematicaMixinMod.FAKE_ROTATION_LIMIT.getIntegerValue()) {
					return false;
				}
				tickElapsed += 1;
				request(fy, fp, lookRefdir, LitematicaMixinMod.FAKE_ROTATION_TICKS.getIntegerValue(), true);
				placeBlock(blockPos, blockState);
			} else {
				placeBlock(blockPos, blockState);
			}
			return true;
		} else {
			//delay
			if (isHandling() && (lookRefdir != fakeDirection || fp != fakePitch || fy != fakeYaw || !canPlaceWallMounted(blockState))) {
				MessageHolder.sendOrderMessage("Cannot handle "+ blockState + " at " + blockPos.toShortString());
				return false;
			}
			if (requestedTicks == 0 && fakeDirection == lookRefdir && fp == fakePitch && fy == fakeYaw) {
				placeBlock(blockPos, blockState);
				return true;
			}
			if (waitingQueue.isEmpty()) {
				request(fy, fp, lookRefdir, LitematicaMixinMod.FAKE_ROTATION_TICKS.getIntegerValue(), false);
				pickFirst(blockState, blockPos);
				boolean offered = waitingQueue.offer(new PosWithBlock(blockPos, blockState));
				if (offered){
					MessageHolder.sendOrderMessage("Offered "+ blockState + " at " + blockPos.toShortString());
				}
				else {
					MessageHolder.sendOrderMessage("Cannot offer "+ blockState + " at " + blockPos.toShortString());
				}
				return false;
			}
		}
		return false;
	}

	/***
	 *
	 * @param state : blockState with Facing, calculates if direction is correct and item is correct for given state
	 * @return : can place or not
	 */
	public static boolean canPlace(BlockState state, BlockPos pos) {
		if (!FAKE_ROTATION_BETA.getBooleanValue()) {
			return true;
		}
		if (canHandleOther(MaterialCache.getInstance().getRequiredBuildItemForState(state, SchematicWorldHandler.getSchematicWorld(), pos).getItem())) {
			if (state.isOf(Blocks.GRINDSTONE)) {
				if (stateGrindStone != null) {
					return stateGrindStone.get(GrindstoneBlock.FACE) == state.get(GrindstoneBlock.FACE) && stateGrindStone.get(GrindstoneBlock.FACING) == state.get(GrindstoneBlock.FACING);
				}
				return false;
			} else if (handlingState != null && (handlingState.getBlock() instanceof FacingBlock || handlingState.getBlock() instanceof HorizontalFacingBlock && !(handlingState.getBlock() instanceof WallMountedBlock))) {
				Direction handling = fi.dy.masa.malilib.util.BlockUtils.getFirstPropertyFacingValue(handlingState);
				Direction other = fi.dy.masa.malilib.util.BlockUtils.getFirstPropertyFacingValue(state);
				return handling == other;
			}
			return true;
		}
		return false;
	}

	synchronized private static boolean placeBlock(BlockPos pos, BlockState blockState) {
		if (!pickFirst(blockState, pos)) {
			return false;
		}
		MessageHolder.sendDebugMessage("Handling placeBlock for " + pos.toShortString() + " and state " + blockState.toString());
		if (blockPlacedInTick > PRINTER_MAX_BLOCKS.getIntegerValue()) {
			MessageHolder.sendDebugMessage("Handling placeBlock failed due to limiting max block" + pos.toShortString());
			return false;
		}
		final MinecraftClient minecraftClient = MinecraftClient.getInstance();
		final ClientPlayerEntity player = minecraftClient.player;
		final ClientPlayerInteractionManager interactionManager = minecraftClient.interactionManager;
		if (!minecraftClient.world.getBlockState(pos).getMaterial().isReplaceable()) {
			MessageHolder.sendDebugMessage("Client block position was not replaceable at " + pos.toShortString());
			return true;
		}
		Direction sideOrig = Direction.NORTH;
		Direction side = Printer.applyPlacementFacing(blockState, sideOrig, minecraftClient.world.getBlockState(pos));
		Vec3d appliedHitVec = Printer.applyHitVec(pos, blockState, side);
		//Trapdoor actually occasionally refers to player and UP DOWN wtf
		if (blockState.getBlock() instanceof TrapdoorBlock) {
			side = blockState.get(TrapdoorBlock.HALF) == BlockHalf.BOTTOM ? Direction.UP : Direction.DOWN;
			appliedHitVec = Vec3d.of(pos);
		} else if (blockState.getBlock() instanceof GrindstoneBlock) {
			appliedHitVec = Vec3d.ofCenter(pos);
			if (blockState.get(GrindstoneBlock.FACE) == WallMountLocation.CEILING) {
				side = Direction.DOWN;
			} else if (blockState.get(GrindstoneBlock.FACE) == WallMountLocation.FLOOR) {
				side = Direction.UP;
			}
		} else if (blockState.getBlock() instanceof TorchBlock) {
			appliedHitVec = Vec3d.ofCenter(pos); //follows player looking
		}
		BlockHitResult blockHitResult = new BlockHitResult(appliedHitVec, side, pos, true);
		ItemStack pickedItem = MaterialCache.getInstance().getRequiredBuildItemForState(blockState, SchematicWorldHandler.getSchematicWorld(), pos);
		if (pickedItem.getItem() == currentHandling && Printer.doSchematicWorldPickBlock(minecraftClient, blockState, pos)) {
			MessageHolder.sendOrderMessage("Placing " + blockState.getBlock().getTranslationKey() + " at " + pos.toShortString() + " stack at hand is " + player.getMainHandStack());

			MessageHolder.sendDebugMessage(player, "Placing " + blockState.getBlock().getTranslationKey() + " at " + pos.toShortString() + " facing : " + fi.dy.masa.malilib.util.BlockUtils.getFirstPropertyFacingValue(blockState));
			MessageHolder.sendDebugMessage(player, "Player facing is set to : " + fakeDirection + " Yaw : " + fakeYaw + " Pitch : " + fakePitch + " ticks : " + requestedTicks + " for pos " + pos.toShortString());
			interactionManager.interactBlock(player, Hand.MAIN_HAND, blockHitResult);
			InventoryUtils.decrementCount();
			blockPlacedInTick++;
			if (InventoryUtils.lastCount <= 0 && SLEEP_AFTER_CONSUME.getIntegerValue() > 0) {
				shouldReturnValue = true;
				Printer.lastPlaced = new Date().getTime() + SLEEP_AFTER_CONSUME.getIntegerValue();
			}
			Printer.cacheEasyPlacePosition(pos, false);
			return true;
		}
		MessageHolder.sendDebugMessage("Handling placeBlock failed due to pickBlock assertion failure" + pos.toShortString() + " wanted item :" + pickedItem.getItem() + " current handling : " + currentHandling.asItem());
		return false;
	}

	private static boolean pickFirst(BlockState blockState, BlockPos pos) {
		final MinecraftClient minecraftClient = MinecraftClient.getInstance();
		if (Printer.doSchematicWorldPickBlock(minecraftClient, blockState, pos)) {
			currentHandling = MaterialCache.getInstance().getRequiredBuildItemForState(blockState, SchematicWorldHandler.getSchematicWorld(), pos).getItem();
			handlingState = blockState;
			requestedTicks = 0;
			return true;
		}
		return false;
	}

	// we just record pos + block and put in queue.
	private record PosWithBlock(BlockPos pos, BlockState blockState) {
	}

}