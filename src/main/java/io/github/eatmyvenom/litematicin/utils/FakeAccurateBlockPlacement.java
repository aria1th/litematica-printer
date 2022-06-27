package io.github.eatmyvenom.litematicin.utils;

//see https://github.com/senseiwells/EssentialClient/blob/1.19.x/src/main/java/me/senseiwells/essentialclient/feature/BetterAccurateBlockPlacement.java

import fi.dy.masa.litematica.util.InventoryUtils;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.*;
import net.minecraft.block.enums.WallMountLocation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;

public class FakeAccurateBlockPlacement{

	// We implement FIFO Queue structure with responsible ticks.
	// By config, we define 'wait tick' between block placements
	public static Direction fakeDirection = null;
	public static int requestedTicks = 0;
	public static float fakeYaw = 0;
	public static float fakePitch = 0;
	private static float previousFakeYaw = 0;
	private static float previousFakePitch = 0;
	private static int tickElapsed = 0;
	private static final Queue<PosWithBlock> waitingQueue = new ArrayDeque<>(1);
	private static final HashSet<Block> warningSet = new HashSet<>();
	// Cancel when handling
	public static boolean isHandling(){
		return requestedTicks > 0;
	}
	// should be called every client tick.
	static {
		ClientTickEvents.END_CLIENT_TICK.register(FakeAccurateBlockPlacement::tick);
	}

	public static void tick(MinecraftClient minecraftClient){
		ClientPlayNetworkHandler clientPlayNetworkHandler = minecraftClient.getNetworkHandler();
		ClientPlayerEntity playerEntity = minecraftClient.player;
		if (playerEntity == null || clientPlayNetworkHandler == null){
			return;
		}
		if (requestedTicks > 0){
			if (fakeYaw != previousFakeYaw || fakePitch != previousFakePitch){
				sendLookPacket(clientPlayNetworkHandler, playerEntity);
				previousFakePitch = fakePitch;
				previousFakeYaw = fakeYaw;
			}
			requestedTicks --;
			if (requestedTicks == 1){
				PosWithBlock obj = waitingQueue.poll();
				if (obj != null) {
					placeBlock(obj.pos, obj.blockState);
					System.out.print(obj);
				}
			}
		}
		else {
			requestedTicks = 0;
			fakeDirection = null;
		}
	}

	public static void sendLookPacket(ClientPlayNetworkHandler networkHandler, ClientPlayerEntity playerEntity){
		networkHandler.sendPacket(
			new PlayerMoveC2SPacket.LookAndOnGround(
				fakeYaw,
				fakePitch,
				playerEntity.isOnGround()
			)
		);
		System.out.print(fakeYaw);
		System.out.print(fakePitch);
	}
	/*
	Pure request function by yaw pitch direction
	 */
	public static boolean request(float yaw, float pitch, Direction direction, int duration, boolean force){
		if (isHandling()){
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
			if (force) {
				sendLookPacket(networkHandler, playerEntity);
			}
			return true;
		}
		else {
			return false;
		}
	}

	/***
	 *
	 * @param blockState : Block object(terracotta, etc...)
	 * @param blockPos : Block Position
	 * @return boolean : if its registered and just can place it.
	 * example : boolean canContinue = FakeAccurateBlockPlacement.request(SchematicState, BlockPos)
	 */
	public static boolean request(BlockState blockState, BlockPos blockPos){
		// instant
		if (blockState.isOf(Blocks.HOPPER)){
			placeBlock(blockPos,blockState);
			return true;
		}
		if (!blockState.contains(Properties.FACING) && !blockState.contains(Properties.HORIZONTAL_FACING) && !blockState.contains(Properties.AXIS) && !blockState.contains(Properties.HORIZONTAL_AXIS)){
			placeBlock(blockPos,blockState);
			return true; //without facing properties
		}
		//delay
		if (isHandling()){
			return false;
		}
		FacingData facingData = FacingData.getFacingData(blockState);
		if (facingData == null){
			if (!warningSet.contains(blockState.getBlock())){
				warningSet.add(blockState.getBlock());
				System.out.printf("WARN : Block %s is not found\n", blockState.getBlock().toString());
			}
			return false;
		}
		Direction facing = fi.dy.masa.malilib.util.BlockUtils.getFirstPropertyFacingValue(blockState); //facing of block itself
		if (facing == null){
			System.out.print("facing was null\n");
			System.out.println(blockState);
			placeBlock(blockPos, blockState);
			return true;
		}
		//assume player is looking at north
		boolean reversed = facingData.isReversed;
		int order = facingData.type;
		Direction direction1 = null;
		if (order == 0 || order == 1){
			direction1 = reversed ? facing.getOpposite() : facing;
		}
		else if (order == 2){
			direction1 = blockState.contains(WallMountedBlock.FACE) && blockState.get(WallMountedBlock.FACE) == WallMountLocation.WALL ? null : facing;
		}
		else if (order == 3){
			direction1 = facing.rotateYCounterclockwise();
		}
		if (direction1 == null){
			System.out.print("direction was null\n");
			System.out.println(blockState);
			placeBlock(blockPos,blockState);
			return true;
		}
		float fy, fp;
		Direction lookRefdir = direction1;
		if (lookRefdir  == Direction.UP) {
			fy = 0;
			fp = -90;
		}
		else if (lookRefdir  == Direction.DOWN) {
			fy = 0;
			fp = 90;
		}
		else if (lookRefdir  == Direction.EAST) {
			fy = -90;
			fp = 0;
		}
		else if (lookRefdir  == Direction.WEST) {
			fy = 90;
			fp = 0;
		}
		else if (lookRefdir  == Direction.NORTH) {
			fy = 180;
			fp = 0;
		}
		else if (lookRefdir  == Direction.SOUTH) {
			fy = 0;
			fp = 0;
		}
		else {
			fy = 0;
			fp = 0;
		}
		request(fy, fp, lookRefdir, 2, false);
		waitingQueue.add(new PosWithBlock(blockPos, blockState));
		System.out.print("Que added\n");
		System.out.print(lookRefdir);
		return true;
	}
	private static boolean placeBlock(BlockPos pos, BlockState blockState){
		final MinecraftClient minecraftClient = MinecraftClient.getInstance();
		final ClientPlayerEntity player = minecraftClient.player;
		final ClientPlayerInteractionManager interactionManager = minecraftClient.interactionManager;
		Direction sideOrig = Direction.NORTH;
		Vec3d hitPos = new Vec3d(pos.getX(), pos.getY(), pos.getZ());
		Direction side = Printer.applyPlacementFacing(blockState, sideOrig, minecraftClient.world.getBlockState(pos));
		Vec3d appliedHitVec = Printer.applyHitVec(pos, blockState, hitPos, side);
		BlockHitResult blockHitResult = new BlockHitResult(appliedHitVec, side, pos, false);
		ItemStack stack = blockState.getBlock().asItem().getDefaultStack();
		int slotNum = minecraftClient.player.getInventory().getSlotWithStack(stack);
		if (slotNum == -1){
			return false;
		}
		InventoryUtils.setPickedItemToHand(stack, minecraftClient);
		System.out.print("Interacted via fake application\n");
		interactionManager.interactBlock(player, Hand.MAIN_HAND, blockHitResult);
		return true;
	}
	// we just record pos + block and put in queue.
	private record PosWithBlock(BlockPos pos,BlockState blockState){
	}
}