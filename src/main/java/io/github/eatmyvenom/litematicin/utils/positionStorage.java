package io.github.eatmyvenom.litematicin.utils;

import net.minecraft.util.math.BlockPos;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import net.minecraft.world.World;
import net.minecraft.util.math.Vec3d;

public class positionStorage{
    private final static Map<Long, Boolean> positionMap = new LinkedHashMap<Long, Boolean> ();
    public static boolean hasPos(BlockPos pos)
    {
        Long asLong = pos.asLong();
        if(positionMap.containsKey(asLong)){
            return positionMap.get(asLong);
        }
        return false;
    }
    public static void registerPos(BlockPos pos, Boolean val)
    {
        positionMap.put(pos.asLong(), val);
    }
    public static void refresh(World world){
        for (Long longPos : positionMap.keySet().stream().filter(longPos->world.getBlockState(BlockPos.fromLong(longPos)).isAir()).collect(Collectors.toList())){
            positionMap.remove(longPos);
        }

    }
    public static ArrayList<BlockPos> getFalseMarkedPos()
    {
        ArrayList<BlockPos> FalseMarkedList = new ArrayList<BlockPos>();
        for (Long position : positionMap.keySet()) {
            if (!positionMap.get(position)) {
                FalseMarkedList.add(BlockPos.fromLong(position));
            }
        }
        return FalseMarkedList;
    }
    public static ArrayList<BlockPos> getFalseMarkedHasBlockPosinAttackRange(World world, Vec3d pos, int attackRange)
    {
        ArrayList<BlockPos> FalseMarkedList = new ArrayList<BlockPos>();
        for (Long position : positionMap.keySet()) {
            BlockPos blockPos = BlockPos.fromLong(position);
            if (!positionMap.get(position) && !world.getBlockState(blockPos).isAir() && blockPos.isWithinDistance(pos, attackRange)) {
                FalseMarkedList.add(blockPos);
            }
        }
        return FalseMarkedList;
    }
}