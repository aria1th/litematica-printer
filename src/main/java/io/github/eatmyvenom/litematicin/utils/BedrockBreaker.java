package io.github.eatmyvenom.litematicin.utils;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.TorchBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

import static io.github.eatmyvenom.litematicin.LitematicaMixinMod.*;

// since 1.19, you can't swap items too fast (huh)
@SuppressWarnings("ConstantConditions")
public class BedrockBreaker {
	public static long lastPlaced = new Date().getTime();
	public static Long CurrentTick = 0L;
	static List<Direction> HORIZONTAL = List.of(Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH);
	private static final Map<Long, PositionCache> targetPosMap = new LinkedHashMap<>();
	static int rangeX = EASY_PLACE_MODE_RANGE_X.getIntegerValue();
	static int rangeY = EASY_PLACE_MODE_RANGE_Y.getIntegerValue();
	static int rangeZ = EASY_PLACE_MODE_RANGE_Z.getIntegerValue();
	static int MaxReach = Math.max(Math.max(rangeX, rangeY), rangeZ);

	public static void clear() {
		targetPosMap.clear();
		positionStorage.clear();
	}

	private static boolean shouldExtend(World world, BlockPos pos, Direction pistonFace) {
		for (Direction direction : Direction.values()) {
			if (direction != pistonFace && world.isEmittingRedstonePower(pos.offset(direction), direction)) {
				return true;
			}
		}
		if (world.isEmittingRedstonePower(pos, Direction.DOWN)) {
			return true;
		} else {
			BlockPos blockPos = pos.up();
			for (Direction qcDirections : Direction.values()) {
				if (qcDirections != Direction.DOWN && world.isEmittingRedstonePower(blockPos.offset(qcDirections), qcDirections)) {
					return true;
				}
			}
			return false;
		}
	}

	@Nullable
	public static TorchPath getPistonTorchPosDir(MinecraftClient mc, BlockPos bedrockPos) {
		for (Direction lv : Direction.values()) {
			if (!ACCURATE_BLOCK_PLACEMENT.getBooleanValue()) {
				if (lv != Direction.DOWN && lv != Direction.UP) {
					continue;
				}
			}
			BlockPos pistonPos = bedrockPos.offset(lv);
			if (!mc.world.getBlockState(pistonPos).isAir() || !isBlockPosinYRange(pistonPos)) {
				continue;
			}
			for (Direction pistonFacing : Direction.values()) {
				if (pistonFacing.getOpposite() == lv) {
					continue;
				}
				BlockPos checkAir = pistonPos.offset(pistonFacing);
				if (!isBlockPosinYRange(checkAir)) {
					continue;
				}
				if (shouldExtend(mc.world, pistonPos, pistonFacing)) {
					continue;
				}
				if (mc.world.getBlockState(checkAir).isAir() || mc.world.getBlockState(checkAir).getMaterial().isReplaceable()) {
					TorchData torchdata = getPossiblePowerableTorchPosFace(mc, bedrockPos, pistonPos, checkAir);
					if (torchdata != null) {
						TorchPath torchPath = new TorchPath(torchdata.TorchPos, torchdata.Torchfacing, pistonPos, pistonFacing, lv.getOpposite());
						if (torchdata.SlimePos != null) {
							torchPath.slimePos = torchdata.SlimePos;
						}
						return torchPath;
					}
				}
			}
		}
		return null;
	}

	public static boolean isBlockPosinYRange(BlockPos checkPos) {
		return (checkPos.getY() < Printer.worldTopY && Printer.worldBottomY < checkPos.getY());
	}

	@Nullable
	public static TorchData getPossiblePowerableTorchPosFace(MinecraftClient mc, BlockPos pos1, BlockPos pistonPos, BlockPos pos2) {
		World world = mc.world;
		boolean forceSlimeBlock = BEDROCK_BREAKING_FORCE_TORCH.getBooleanValue();
		for (Direction hd : HORIZONTAL) { //normal 4 dir
			BlockPos torchCheck = pistonPos.offset(hd);
			if (!isBlockPosinYRange(torchCheck)) {
				continue;
			}
			if (torchCheck.equals(pos1) || torchCheck.equals(pos2)) {
				continue;
			}
			if (!world.getBlockState(torchCheck).isAir() && !world.getBlockState(torchCheck).getMaterial().isReplaceable()) {
				continue;
			}
			// check torch can be placed
			if (!world.getBlockState(torchCheck.down()).isOf(Blocks.PISTON) && TorchBlock.sideCoversSmallSquare(world, torchCheck.down(), Direction.DOWN)) {
				return new TorchData(torchCheck, Direction.UP);
			} else if (forceSlimeBlock && canPlaceSlime(mc)) {
				BlockPos slimePos = torchCheck.down();
				if (slimePos.equals(pos2)) {
					continue;
				}
				if (world.getBlockState(slimePos).isAir() || world.getBlockState(slimePos).getMaterial().isReplaceable()) {
					//placeSlime(mc, slimePos);
					TorchData torchData = new TorchData(torchCheck, Direction.UP);
					torchData.registerSlimePos(slimePos);
					return torchData;
				}
			} else {
				for (Direction hd2 : HORIZONTAL) {
					if (hd2 == hd) {
						continue;
					}
					if (canPlaceAt(hd2, world, torchCheck)) {
						return new TorchData(torchCheck, hd2);
					}
				}
			}
		}
		for (Direction hd : HORIZONTAL) { //qc
			BlockPos torchCheck = pistonPos.up().offset(hd);
			if (!isBlockPosinYRange(torchCheck)) {
				continue;
			}
			if (torchCheck.equals(pos1) || torchCheck.equals(pos2)) {
				continue;
			}
			if (!world.getBlockState(torchCheck).isAir() && !world.getBlockState(torchCheck).getMaterial().isReplaceable()) {
				continue;
			}
			// check torch can be placed
			if (!world.getBlockState(torchCheck.down()).isOf(Blocks.PISTON) && TorchBlock.sideCoversSmallSquare(world, torchCheck.down(), Direction.DOWN)) {
				return new TorchData(torchCheck, Direction.UP);
			} else if (forceSlimeBlock && canPlaceSlime(mc)) {
				BlockPos slimePos = torchCheck.down();
				if (slimePos.equals(pos2)) {
					continue;
				}
				if (!isBlockPosinYRange(slimePos)) {
					continue;
				}
				if (world.getBlockState(slimePos).isAir() || world.getBlockState(slimePos).getMaterial().isReplaceable()) {
					TorchData torchData = new TorchData(torchCheck, Direction.UP);
					//placeSlime(mc, slimePos);
					torchData.registerSlimePos(slimePos);
					return torchData;
				}
			} else {
				for (Direction hd2 : HORIZONTAL) {
					if (hd2 == hd) {
						continue;
					}
					if (canPlaceAt(hd2, world, torchCheck)) {
						return new TorchData(torchCheck, hd2);
					}
				}
			}
		}
		BlockPos torchCheck = pistonPos.down(); // down
		if (pos2 != torchCheck && isBlockPosinYRange(torchCheck)) {
			if (!world.getBlockState(torchCheck).isAir() && !world.getBlockState(torchCheck).getMaterial().isReplaceable()) {
				return null;
			}
			if (!world.getBlockState(torchCheck.down()).isOf(Blocks.PISTON) && TorchBlock.sideCoversSmallSquare(world, torchCheck.down(), Direction.DOWN)) {
				return new TorchData(torchCheck, Direction.UP);
			} else if (forceSlimeBlock && canPlaceSlime(mc)) {
				BlockPos slimePos = torchCheck.down();
				if (slimePos.equals(pos2)) {
					return null;
				}
				if (isBlockPosinYRange(slimePos) && world.getBlockState(slimePos).isAir() || world.getBlockState(slimePos).getMaterial().isReplaceable()) {
					TorchData torchData = new TorchData(torchCheck, Direction.UP);
					torchData.registerSlimePos(slimePos);
					return torchData;
				}
			}
		}
		return null;
	}

	public static void removeScheduledPos(MinecraftClient mc) {
		for (Long position : targetPosMap.keySet().stream().filter(position ->
			targetPosMap.get(position) != null && CurrentTick - targetPosMap.get(position).SysTime > 200L && targetPosMap.get(position).isIdle()).collect(Collectors.toList())) {
			targetPosMap.remove(position);
		}
		for (Long position : targetPosMap.keySet().stream().filter(position ->
			targetPosMap.get(position).canSafeRemove(mc.world)).collect(Collectors.toList())) {
			targetPosMap.remove(position);
		}
	}

	public static boolean canPlaceAt(Direction lv, World world, BlockPos pos) {
		BlockPos lv2 = pos.offset(lv.getOpposite());
		BlockState lv3 = world.getBlockState(lv2);
		if (lv3.isOf(Blocks.PISTON)) {
			return false;
		}
		return lv3.isSideSolidFullSquare(world, lv2, lv);
	}

	public static void placePiston(MinecraftClient mc, BlockPos pos, Direction facing) {
		final ItemStack PistonStack = Items.PISTON.getDefaultStack();
		InventoryUtils.swapToItem(mc, PistonStack);
		MessageHolder.sendDebugMessage("Places piston at %s with facing %s".formatted(pos.toShortString(), facing));
		//mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot));
		if (ACCURATE_BLOCK_PLACEMENT.getBooleanValue()) {
			placeViaCarpet(mc, pos, facing);
		} else {
			placeViaPacketReversed(mc, pos, facing, false);
		}
	}

	public static void placePiston(MinecraftClient mc, BlockPos pos, Direction facing, boolean sync) {
		final ItemStack PistonStack = Items.PISTON.getDefaultStack();
		InventoryUtils.swapToItem(mc, PistonStack);
		if (sync) {
			mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot));
		}
		MessageHolder.sendDebugMessage("Places piston at %s with facing %s".formatted(pos.toShortString(), facing));
		//mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot));
		if (ACCURATE_BLOCK_PLACEMENT.getBooleanValue()) {
			placeViaCarpet(mc, pos, facing);
		} else {
			placeViaPacketReversed(mc, pos, facing, false);
		}
	}

	public static void placeSlime(MinecraftClient mc, BlockPos pos) {
		final ItemStack SlimeStack = Items.SLIME_BLOCK.getDefaultStack();
		InventoryUtils.swapToItem(mc, SlimeStack);
		MessageHolder.sendDebugMessage("Places slime at %s".formatted(pos.toShortString()));
		//mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot));
		placeViaCarpet(mc, pos, Direction.UP);
	}

	public static void placeViaCarpet(MinecraftClient mc, BlockPos pos, Direction facing) {
		positionStorage.registerPos(pos, true);
		Vec3d hitVec = new Vec3d(pos.getX() + 2 + (facing.getId() * 2), pos.getY(), pos.getZ());
		BlockHitResult hitResult = new BlockHitResult(hitVec, facing, pos, false);
		handleTweakPlacementPacket(mc, hitResult);
	}

	public static void placeViaPacketReversed(MinecraftClient mc, BlockPos pos, Direction facing, boolean ShouldOffset) {
		positionStorage.registerPos(pos, true);
		int px = pos.getX();
		int py = pos.getY();
		int pz = pos.getZ();
		Vec3d hitPos = new Vec3d(px, py, pz);
		if (ShouldOffset) {
			if (facing == Direction.DOWN) {
				py += 0;
			} else if (facing == Direction.UP) {
				py += 0;
			} else if (facing == Direction.NORTH) {
				pz += 1;
			} else if (facing == Direction.SOUTH) {
				pz -= 1;
			} else if (facing == Direction.EAST) {
				px -= 1;
			} else if (facing == Direction.WEST) {
				px += 1;
			}
		}
		BlockPos npos = new BlockPos(px, py, pz);
		if (ShouldOffset) {
			hitPos = Printer.applyTorchHitVec(npos, new Vec3d(0.5, 0.5, 0.5), facing);
			if (facing == Direction.DOWN) {
				facing = Direction.UP;
			}
		}
		float OriginPitch = mc.player.getPitch(1.0f);
		float OriginYaw = mc.player.getYaw(1.0f);
		if (facing == Direction.DOWN) {
			mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(OriginYaw, -90.0f, mc.player.isOnGround()));
		} else if (facing == Direction.UP) {
			mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(OriginYaw, 90.0f, mc.player.isOnGround()));
		} else if (facing == Direction.EAST) {
			mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(90.0f, OriginPitch, mc.player.isOnGround()));
		} else if (facing == Direction.WEST) {
			mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(-90.0f, OriginPitch, mc.player.isOnGround()));
		} else if (facing == Direction.NORTH) {
			mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(0.0f, OriginPitch, mc.player.isOnGround()));
		} else if (facing == Direction.SOUTH) {
			mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(180.0f, OriginPitch, mc.player.isOnGround()));
		}
		BlockHitResult hitResult = new BlockHitResult(hitPos, facing, npos, false);
		handleTweakPlacementPacket(mc, hitResult);
		mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(OriginYaw, OriginPitch, mc.player.isOnGround()));
	}

	public static void handleTweakPlacementPacket(MinecraftClient mc, BlockHitResult hitResult) {
		//mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, hitResult, 64));
		mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
	}

	public static void placeTorch(MinecraftClient mc, BlockPos pos, Direction torchFacing) {
		final ItemStack redstoneTorchStack = Items.REDSTONE_TORCH.getDefaultStack();
		InventoryUtils.swapToItem(mc, redstoneTorchStack);
		BlockPos npos;
		if (torchFacing.getAxis() == Direction.Axis.Y) {
			npos = pos.down();
			torchFacing = Direction.UP;
		} else {
			npos = pos.offset(torchFacing.getOpposite());
		}
		MessageHolder.sendDebugMessage("Places torch at %s with facing %s".formatted(pos.toShortString(), torchFacing));
		Vec3d hitVec = Vec3d.ofCenter(npos).add(Vec3d.of(torchFacing.getVector()).multiply(0.5));
		BlockHitResult hitResult = new BlockHitResult(hitVec, torchFacing, npos, false);
		MessageHolder.sendDebugMessage("Hitresult is %s %s".formatted(hitVec, npos.toShortString()));
		mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
		positionStorage.registerPos(pos, true);
	}


	public static boolean canProcess(MinecraftClient mc, BlockPos pos) {
		double SafetyDistance = BEDROCK_BREAKING_RANGE_SAFE.getIntegerValue();
		if (positionAnyNear(mc, pos, SafetyDistance)) {
			return false;
		}
		if (targetPosMap.containsKey(pos.asLong())) {
			return targetPosMap.get(pos.asLong()).isIdle();
		}
		return true;
	}

	public static boolean positionAnyNear(MinecraftClient mc, BlockPos pos, double distance) {
		for (Long position : targetPosMap.keySet()) {
			@Nullable PositionCache item = targetPosMap.get(position);
			if (item == null) {
				continue;
			}
			if (isPositionInRange(mc, BlockPos.fromLong(position)) && item.distanceLessThan(pos, distance) && !item.isIdle()) {
				return true;
			}
		}
		return false;
	}

	public static boolean isItemPrePared(MinecraftClient mc) {
		PlayerInventory inv = mc.player.getInventory();
		ItemStack PistonStack = Items.PISTON.getDefaultStack();
		ItemStack RedstoneTorchStack = Items.REDSTONE_TORCH.getDefaultStack();
		return inv.getSlotWithStack(PistonStack) != -1 && inv.getSlotWithStack(RedstoneTorchStack) != -1;
	}

	public static boolean canPlaceSlime(MinecraftClient mc) {
		PlayerInventory inv = mc.player.getInventory();
		ItemStack SlimeStack = Items.SLIME_BLOCK.getDefaultStack();
		return inv.getSlotWithStack(SlimeStack) != -1;
	}

	public static void switchTool(MinecraftClient mc) {
		int bestSlotId = Breaker.getBestItemSlotIdToMineState(mc, Blocks.PISTON.getDefaultState());
		if (bestSlotId == -1) {
			return;
		}
		ItemStack stack = mc.player.getInventory().getStack(bestSlotId);
		MessageHolder.sendDebugMessage("Swaps to Pickaxe " + stack);
		InventoryUtils.swapToItem(mc, stack);
		MessageHolder.sendDebugMessage("Holding stack " + mc.player.getMainHandStack());
		mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot));
	}


	public static void attackBlock(MinecraftClient mc, BlockPos pos, Direction direction) {
		if (mc.world.getBlockState(pos).isAir()) {
			return;
		}
		mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, direction, 64));
		//positionStorage.registerPos(pos, false);
	}


	public static boolean isBlockNotInstantBreakable(Block block) {
		return block.equals(Blocks.BEDROCK) || block.equals(Blocks.OBSIDIAN);
	}

	public static boolean isPositionInRange(MinecraftClient mc, BlockPos pos) {
		double pX = mc.player.getX();
		double pY = mc.player.getY();
		double pZ = mc.player.getZ();
		int aX = pos.getX();
		int aY = pos.getY();
		int aZ = pos.getZ();
		return (pX - aX) * (pX - aX) + (pY - aY) * (pY - aY) + (pZ - aZ) * (pZ - aZ) < MaxReach * MaxReach;
	}

	public static int processRemainder(MinecraftClient mc, int maxInteract) {
		//positionStorage.refresh(mc.world);
		int ret = 0;
		switchTool(mc);
		ArrayList<BlockPos> attackList = positionStorage.getFalseMarkedHasBlockPosInAttackRange(mc.world, mc.player.getPos(), MaxReach);
		for (BlockPos position : attackList) {
			if (ret >= maxInteract) {
				return ret;
			}
			attackBlock(mc, position, Direction.UP);
			ret++;
		}
		return ret;
	}

	synchronized public static int scheduledTickHandler(MinecraftClient mc, @Nullable BlockPos pos) {
		if (!isItemPrePared(mc)) {
			return 0;
		}
		rangeX = EASY_PLACE_MODE_RANGE_X.getIntegerValue();
		rangeY = EASY_PLACE_MODE_RANGE_Y.getIntegerValue();
		rangeZ = EASY_PLACE_MODE_RANGE_Z.getIntegerValue(); //reset range values
		int maxInteract = PRINTER_MAX_BLOCKS.getIntegerValue();
		int interacted = 0;
		interacted += BedrockBreaker.processRemainder(mc, maxInteract);
		if (interacted >= maxInteract) {
			return interacted;
		}
		MaxReach = Math.max(Math.max(rangeX, rangeY), rangeZ);
		removeScheduledPos(mc);
		if (pos != null && !targetPosMap.containsKey(pos.asLong()) && isPositionInRange(mc, pos) && canProcess(mc, pos) && new Date().getTime() - lastPlaced > 1000.0 * EASY_PLACE_MODE_DELAY.getDoubleValue()) {
			TorchPath torch = getPistonTorchPosDir(mc, pos);
			if (torch != null && torch.isAllPosInRange(mc)) {
				lastPlaced = new Date().getTime();
				BlockPos TorchPos = torch.TorchPos;
				Direction TorchFacing = torch.Torchfacing;
				BlockPos PistonPos = torch.PistonPos;
				Direction PistonFacing = torch.Pistonfacing;
				Direction PistonExtendFacing = torch.PistonBreakableFacing;
				BlockPos SlimePos = torch.slimePos;
				MessageHolder.sendDebugMessage("Will place Torch at %s, facing %s \n Piston at %s, Facing %s, and changes as %s \n Optional Slime at %s".formatted(TorchPos.toShortString(), TorchFacing, PistonPos.toShortString(), PistonFacing, PistonExtendFacing, SlimePos));
				if (SlimePos != null) {
					placeSlime(mc, SlimePos);
				}
				placeTorch(mc, TorchPos, TorchFacing);
				placePiston(mc, PistonPos, PistonFacing);
				interacted += 2;
				targetPosMap.put(pos.asLong(), new PositionCache(PistonPos, PistonExtendFacing, TorchPos, pos, SlimePos));
			}
		}
		for (Long posLong : targetPosMap.keySet()) {
			if (interacted >= maxInteract) {
				return interacted;
			}
			PositionCache item = targetPosMap.get(posLong);
			if (item == null || !item.isAllPosInRange(mc)) {
				continue;
			}
			interacted += item.doSomething(mc);
		}
		return interacted;
	}

	public static void tick() {
		CurrentTick += 1L;
	}

	public static class PositionCache {
		public final BlockPos pistonPos;
		public final Direction facing;
		public final BlockPos torchPos;
		public final BlockPos targetPos;
		public long SysTime;
		public final BlockPos slimePos;
		public PositionCache.State state;

		public enum State {
			WAIT,
			EXTENDED,
			IDLE,
			FAIL,
			DONE,
			CLEAR
		}

		private PositionCache(BlockPos pistonPos, Direction facing, BlockPos torchPos, BlockPos targetPos, BlockPos slimePos) {
			this.pistonPos = pistonPos;
			this.facing = facing;
			this.torchPos = torchPos;
			this.targetPos = targetPos;
			this.SysTime = CurrentTick;
			this.slimePos = slimePos;
			this.state = PositionCache.State.WAIT;
		}

		public void setFalse() {
			positionStorage.registerPos(this.pistonPos, false);
			positionStorage.registerPos(this.torchPos, false);
			if (this.slimePos != null) {
				positionStorage.registerPos(this.slimePos, false);
			}
		}

		public boolean isAllPosInRange(MinecraftClient mc) {
			return mc.player.squaredDistanceTo(Vec3d.ofCenter(this.pistonPos)) < MaxReach * MaxReach && mc.player.squaredDistanceTo(Vec3d.ofCenter(this.torchPos)) < MaxReach * MaxReach &&
				mc.player.squaredDistanceTo(Vec3d.ofCenter(this.targetPos)) < MaxReach * MaxReach && (this.slimePos == null || mc.player.squaredDistanceTo(Vec3d.ofCenter(this.slimePos)) < MaxReach * MaxReach);
		}

		public boolean canSafeRemove(World world) {
			return (this.state == State.DONE || this.state == State.CLEAR) && world.getBlockState(this.torchPos).isAir() &&
				world.getBlockState(this.pistonPos).isAir() && (this.slimePos == null || world.getBlockState(this.slimePos).isAir());
		}

		private void refresh(ClientWorld world) {
			switch (this.state) {
				case WAIT: {
					if (CurrentTick == this.SysTime + 1 || CurrentTick > this.SysTime + 4) {
						this.state = State.EXTENDED;
					}
				}
				case IDLE: {
					if (this.SysTime + BEDROCK_BREAKING_CLEAR_WAIT.getIntegerValue() < CurrentTick) {
						this.setFalse();
						this.state = world.getBlockState(this.targetPos).isOf(Blocks.BEDROCK) ? State.FAIL : State.DONE;
					}
				}
			}
		}

		public int doSomething(MinecraftClient mc) {
			refresh(mc.world);
			switch (this.state) {
				case EXTENDED -> {
					this.processBreaking(mc);
					return 4;
				}
				case FAIL -> {
					this.resetFailure(mc);
					return 3;
				}
			}
			return 0;
		}

		public boolean isIdle() {
			return this.state == State.CLEAR || this.state == State.DONE;
		}

		public boolean distanceLessThan(BlockPos ReferPos, double distance) {
			BlockPos pos = this.targetPos;
			int aX = pos.getX();
			int aY = pos.getY();
			int aZ = pos.getZ();
			int pX = ReferPos.getX();
			int pY = ReferPos.getY();
			int pZ = ReferPos.getZ();
			return ((pX - aX) * (pX - aX) + (pY - aY) * (pY - aY) + (pZ - aZ) * (pZ - aZ)) < distance * distance;
		}

		public void processBreaking(MinecraftClient mc) {
			switchTool(mc);
			if (slimePos != null && !mc.world.getBlockState(slimePos).isAir()) {
				attackBlock(mc, torchPos, Direction.UP);
				attackBlock(mc, slimePos, Direction.UP);
				MessageHolder.sendDebugMessage("Broke slime at " + slimePos.toShortString());
			} else {
				attackBlock(mc, torchPos, Direction.UP);
				MessageHolder.sendDebugMessage("Broke torch at " + torchPos.toShortString());
			}
			attackBlock(mc, pistonPos, Direction.UP);
			MessageHolder.sendDebugMessage("Broke piston at " + pistonPos.toShortString());

			placePiston(mc, pistonPos, facing, true);
			this.state = State.IDLE;
		}

		public void resetFailure(MinecraftClient mc) {
			switchTool(mc);
			if (slimePos != null && !mc.world.getBlockState(slimePos).isAir()) {
				attackBlock(mc, slimePos, Direction.UP);
				MessageHolder.sendDebugMessage("Broke slime at + (failure) " + slimePos.toShortString());
			} else {
				attackBlock(mc, torchPos, Direction.UP);
				MessageHolder.sendDebugMessage("Broke torch at + (failure) " + torchPos.toShortString());
			}
			MessageHolder.sendDebugMessage("Broke piston at + (failure) " + pistonPos.toShortString());
			attackBlock(mc, pistonPos, Direction.UP);
			this.state = State.CLEAR;
			this.setFalse();
		}
	}

	public static class TorchPath {
		private final BlockPos TorchPos;
		private final Direction Torchfacing;
		private final BlockPos PistonPos;
		private final Direction Pistonfacing;
		private final Direction PistonBreakableFacing;
		private BlockPos slimePos;

		public TorchPath(BlockPos TorchPos, Direction Torchfacing, BlockPos PistonPos, Direction Pistonfacing, Direction PistonBreakableFacing) {
			this.TorchPos = TorchPos;
			this.Torchfacing = Torchfacing;
			this.Pistonfacing = Pistonfacing;
			this.PistonPos = PistonPos;
			this.PistonBreakableFacing = PistonBreakableFacing;
		}

		public boolean isAllPosInRange(MinecraftClient mc) {
			return mc.player.squaredDistanceTo(Vec3d.ofCenter(this.TorchPos)) < MaxReach * MaxReach && mc.player.squaredDistanceTo(Vec3d.ofCenter(this.PistonPos)) < MaxReach * MaxReach &&
				(this.slimePos == null || mc.player.squaredDistanceTo(Vec3d.ofCenter(this.slimePos)) < MaxReach * MaxReach);
		}

	}

	public static class TorchData {
		private final BlockPos TorchPos;
		private final Direction Torchfacing;
		private BlockPos SlimePos = null;

		public TorchData(BlockPos TorchPos, Direction Torchfacing) {
			this.TorchPos = TorchPos;
			this.Torchfacing = Torchfacing;
		}

		public void registerSlimePos(BlockPos slimePos) {
			this.SlimePos = slimePos;
		}
	}
}
