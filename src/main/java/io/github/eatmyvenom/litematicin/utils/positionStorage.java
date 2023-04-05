package io.github.eatmyvenom.litematicin.utils;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
//#if MC<11700
//$$ import java.util.stream.Collectors;
//#endif
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class positionStorage {
	private final static Map<Long, Boolean> positionMap = new LinkedHashMap<>();

	public static void clear() {
		positionMap.clear();
	}

	public static boolean hasPos(BlockPos pos) {
		Long asLong = pos.asLong();
		if (positionMap.containsKey(asLong)) {
			return positionMap.get(asLong);
		}
		return false;
	}

	public static void registerPos(BlockPos pos, Boolean val) {
		positionMap.put(pos.asLong(), val);
	}

	public static void refresh(World world) {
		//#if MC<11700
		//$$ for (Long longPos : positionMap.keySet().stream().filter(longPos -> !positionMap.get(longPos) && !match(world.getBlockState(BlockPos.fromLong(longPos)).getBlock())).collect(Collectors.toList())) {
		//#else
		for (Long longPos : positionMap.keySet().stream().filter(longPos -> !positionMap.get(longPos) && !match(world.getBlockState(BlockPos.fromLong(longPos)).getBlock())).toList()) {
		//#endif
			positionMap.remove(longPos);
		}
	}

	private static boolean match(Block block) {
		return block == Blocks.PISTON || block == Blocks.REDSTONE_TORCH || block == Blocks.SLIME_BLOCK;
	}

	public static ArrayList<BlockPos> getFalseMarkedHasBlockPosInAttackRange(World world, Vec3d pos, int attackRange) {
		ArrayList<BlockPos> FalseMarkedList = new ArrayList<>();
		for (Long position : positionMap.keySet()) {
			BlockPos blockPos = BlockPos.fromLong(position);
			if (!positionMap.get(position) && match(world.getBlockState(blockPos).getBlock()) && blockPos.isWithinDistance(pos, attackRange)) {
				FalseMarkedList.add(blockPos);
			}
		}
		return FalseMarkedList;
	}
}