package io.github.eatmyvenom.litematicin.utils;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class positionStorage {
	private final static Map<Long, Boolean> positionMap = new LinkedHashMap<Long, Boolean>();

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
		for (Long longPos : positionMap.keySet().stream().filter(longPos -> world.getBlockState(BlockPos.fromLong(longPos)).isAir()).collect(Collectors.toList())) {
			positionMap.remove(longPos);
		}

	}

	public static ArrayList<BlockPos> getFalseMarkedHasBlockPosInAttackRange(World world, Vec3d pos, int attackRange) {
		ArrayList<BlockPos> FalseMarkedList = new ArrayList<>();
		for (Long position : positionMap.keySet()) {
			BlockPos blockPos = BlockPos.fromLong(position);
			if (!positionMap.get(position) && !world.getBlockState(blockPos).isAir() && world.getBlockState(blockPos).getHardness(world, blockPos) != -1 && blockPos.isWithinDistance(pos, attackRange)) {
				FalseMarkedList.add(blockPos);
			}
		}
		return FalseMarkedList;
	}
}