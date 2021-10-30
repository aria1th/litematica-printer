package io.github.eatmyvenom.litematicin.utils;

import static io.github.eatmyvenom.litematicin.LitematicaMixinMod.ACCURATE_BLOCK_PLACEMENT;
import static io.github.eatmyvenom.litematicin.LitematicaMixinMod.EASY_PLACE_MODE_RANGE_X;
import static io.github.eatmyvenom.litematicin.LitematicaMixinMod.EASY_PLACE_MODE_RANGE_Y;
import static io.github.eatmyvenom.litematicin.LitematicaMixinMod.EASY_PLACE_MODE_RANGE_Z;
import static io.github.eatmyvenom.litematicin.LitematicaMixinMod.EASY_PLACE_MODE_DELAY;
import static io.github.eatmyvenom.litematicin.LitematicaMixinMod.BEDROCK_BREAKING_FORCE_TORCH;

import fi.dy.masa.litematica.util.InventoryUtils;

import org.jetbrains.annotations.Nullable;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.PistonBlock;
import net.minecraft.block.SlimeBlock;
import net.minecraft.block.TorchBlock;
import net.minecraft.block.WallTorchBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemConvertible;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BedrockBreaker {
    private final static List<PositionCache> positionCache = new ArrayList<>();
    public static long lastPlaced = new Date().getTime();
    public static boolean Lock = false;
    public static Long CurrentTick = 0L;
    static List<Direction> HORIZONTAL = List.of(Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH);
    private static final Map<Long, PositionCache> targetPosMap = new LinkedHashMap<Long, PositionCache>();
    static int rangeX = EASY_PLACE_MODE_RANGE_X.getIntegerValue();
    static int rangeY = EASY_PLACE_MODE_RANGE_Y.getIntegerValue();
    static int rangeZ = EASY_PLACE_MODE_RANGE_Z.getIntegerValue();
    static int MaxReach = Math.max(Math.max(rangeX, rangeY), rangeZ);
    @Nullable
    public static TorchPath getPistonTorchPosDir(MinecraftClient mc, BlockPos bedrockPos) {
        for (Direction lv : Direction.values()) {
            if (!ACCURATE_BLOCK_PLACEMENT.getBooleanValue()) {
                if (lv != Direction.DOWN && lv != Direction.UP) {continue;}
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

    public static boolean isBlockPosinYRange(BlockPos checkPos){
        return (checkPos.getY() < Printer.worldTopY && Printer.worldBottomY < checkPos.getY());
    }
    @Nullable
    public static TorchData getPossiblePowerableTorchPosFace(MinecraftClient mc, BlockPos pos1, BlockPos PistonPos, BlockPos pos2) {
        World world = mc.world;
        boolean forceSlimeBlock = BEDROCK_BREAKING_FORCE_TORCH.getBooleanValue();
        for (Direction hd : HORIZONTAL) { //normal 4 dir
            BlockPos torchCheck = PistonPos.offset(hd);
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
            if (TorchBlock.sideCoversSmallSquare(world, torchCheck.down(), Direction.DOWN)) {
                return new TorchData(torchCheck, Direction.DOWN);
            } else if (forceSlimeBlock && canPlaceSlime(mc)) {
                BlockPos slimePos = torchCheck.down();
                if (slimePos.equals(pos2)) {continue;}
                if (world.getBlockState(slimePos).isAir() || world.getBlockState(slimePos).getMaterial().isReplaceable()) {
                    //placeSlime(mc, slimePos);
                    TorchData torchData = new TorchData(torchCheck, Direction.DOWN);
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
            BlockPos torchCheck = PistonPos.up().offset(hd);
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
            if (TorchBlock.sideCoversSmallSquare(world, torchCheck.down(), Direction.DOWN)) {
                return new TorchData(torchCheck, Direction.DOWN);
            } else if (forceSlimeBlock && canPlaceSlime(mc)) {
                BlockPos slimePos = torchCheck.down();
                if (slimePos.equals(pos2)) {continue;}
                if (!isBlockPosinYRange(slimePos)) {
                    continue;
                }
                if (world.getBlockState(slimePos).isAir() || world.getBlockState(slimePos).getMaterial().isReplaceable()) {
                    TorchData torchData = new TorchData(torchCheck, Direction.DOWN);
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
        BlockPos torchCheck = PistonPos.down(); // down
        if (pos2 != torchCheck && isBlockPosinYRange(torchCheck)) {
            if (!world.getBlockState(torchCheck).isAir() && !world.getBlockState(torchCheck).getMaterial().isReplaceable()) {
                return null;
            }
            if (TorchBlock.sideCoversSmallSquare(world, torchCheck.down(), Direction.DOWN)) {
                return new TorchData(torchCheck, Direction.DOWN);
            } else if (forceSlimeBlock && canPlaceSlime(mc)) {
                BlockPos slimePos = torchCheck.down();
                if (slimePos.equals(pos2)) {return null;}
                if (isBlockPosinYRange(slimePos) && world.getBlockState(slimePos).isAir() || world.getBlockState(slimePos).getMaterial().isReplaceable()) {
                    TorchData torchData = new TorchData(torchCheck, Direction.DOWN);
                    //placeSlime(mc, slimePos);
                    torchData.registerSlimePos(slimePos);
                    return torchData;
                }
            }
        }
        return null;
    }

    public static void removeScheduledPos(MinecraftClient mc) {
        for (Long position : targetPosMap.keySet().stream().filter(position ->
                targetPosMap.get(position) != null && CurrentTick - targetPosMap.get(position).getSysTime() > 200L && targetPosMap.get(position).isClear()).collect(Collectors.toList())){
            targetPosMap.remove(position);
        }
        for (Long position : targetPosMap.keySet().stream().filter(position ->
                targetPosMap.get(position).canSafeRemove(mc.world)).collect(Collectors.toList())){
            targetPosMap.remove(position);
        }
        /*
         for (Long position : targetPosMap.keySet()) {
            PositionCache item = targetPosMap.get(position);
            if (item != null && CurrentTick - item.getSysTime() > 10000L && item.isClear() ) {
                targetPosMap.put(position,null);
            }
        }
        */
    }

    public static boolean canPlaceAt(Direction lv, World world, BlockPos pos) {
        BlockPos lv2 = pos.offset(lv.getOpposite());
        BlockState lv3 = world.getBlockState(lv2);
        return lv3.isSideSolidFullSquare(world, lv2, lv);
    }

    public static void placePiston(MinecraftClient mc, BlockPos pos, Direction facing) {
        if (positionStorage.hasPos(pos)){return;}
        ItemStack PistonStack = Items.PISTON.getDefaultStack();
        InventoryUtils.setPickedItemToHand(PistonStack, mc);
        mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot));
        if (ACCURATE_BLOCK_PLACEMENT.getBooleanValue()) {
            placeViaCarpet(mc, pos, facing);
        } else {
            placeViaPacketReversed(mc, pos, facing, false);
        }
    }

    public static void placeSlime(MinecraftClient mc, BlockPos pos) {
        ItemStack SlimeStack = Items.SLIME_BLOCK.getDefaultStack();
        InventoryUtils.setPickedItemToHand(SlimeStack, mc);
        mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot));
        placeViaCarpet(mc, pos, Direction.UP);
    }

    public static void placeViaCarpet(MinecraftClient mc, BlockPos pos, Direction facing) {
        positionStorage.registerPos(pos, true);
        Vec3d hitVec = new Vec3d(pos.getX() + 2 + (facing.getId() * 2), pos.getY(), pos.getZ());
        Hand hand = Hand.MAIN_HAND;
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
                pz += -1;
            } else if (facing == Direction.EAST) {
                px += -1;
            } else if (facing == Direction.WEST) {
                px += 1;
            }
        }
        BlockPos npos = new BlockPos(px, py, pz);
        Vec3d hitVec = new Vec3d(px, py, pz);
        if (ShouldOffset) {
            hitPos = Printer.applyTorchHitVec(npos, new Vec3d(0.5,0.5,0.5), facing);
            if (facing == Direction.DOWN){
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
        Hand hand = Hand.MAIN_HAND;
        BlockHitResult hitResult = new BlockHitResult(hitPos, facing, npos, false);
        handleTweakPlacementPacket(mc, hitResult);
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(OriginYaw, OriginPitch, mc.player.isOnGround()));
    }

    public static void handleTweakPlacementPacket(MinecraftClient mc, BlockHitResult hitResult) {
        mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, hitResult));
    }

    public static void placeTorch(MinecraftClient mc, BlockPos pos, Direction TorchFacing) {
        ItemStack RedstoneTorchStack = Items.REDSTONE_TORCH.getDefaultStack();
        InventoryUtils.setPickedItemToHand(RedstoneTorchStack, mc);
        mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot));
        placeViaPacketReversed(mc, pos, TorchFacing, true);
    }


    public static boolean isPistonPowered(MinecraftClient mc, BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);
        if (state.getBlock() instanceof PistonBlock) {
            return state.get(PistonBlock.EXTENDED);
        }
        return false;
    }

    public static boolean canProcess(MinecraftClient mc, BlockPos pos) {
        double SafetyDistance = 6.0f;
        if (positionAnyNear(mc, pos, SafetyDistance)) {
            return false;
        }
        if (targetPosMap.containsKey(pos.asLong())) {
            return targetPosMap.get(pos.asLong()).isFail();
        }
        return true;
    }

    public static boolean positionAnyNear(MinecraftClient mc,BlockPos pos, double distance) {
        //targetPosMap.keySet().stream().anyMatch(longPos -> pos.isWithinDistance(BlockPos.fromLong(longPos),distance) && targetPosMap.get(longPos)!= null && !targetPosMap.get(longPos).isClear() )
        for (Long position : targetPosMap.keySet()) {
            @Nullable PositionCache item = targetPosMap.get(position);
            if (item == null) {continue;}
            if (isPositionInRange(mc, BlockPos.fromLong(position)) && item.distanceLessThan(pos, distance) && !item.isClear()) {
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
        //if (inv.getSlotWithStack(Breakable)== -1 && inv.getSlotWithStack(Alternative)==-1){
        //    System.out.println("no pickaxe");
        //    return false;
        //}
    }
    public static boolean canPlaceSlime(MinecraftClient mc) {
        PlayerInventory inv = mc.player.getInventory();
        ItemStack SlimeStack = Items.SLIME_BLOCK.getDefaultStack();
        return inv.getSlotWithStack(SlimeStack) != -1;
    }

    public static void switchTool(MinecraftClient mc, BlockPos pos) {
        int bestSlotId = getBestItemSlotIdToMineBlock(mc);
        PlayerInventory inv = mc.player.getInventory();
        if (mc.player.getInventory().selectedSlot != bestSlotId) {
            mc.player.getInventory().selectedSlot = bestSlotId;
        }
        mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(inv.selectedSlot));
    }
    public static int getBestItemSlotIdToMineBlock(MinecraftClient mc) {
        int bestSlot = 0;
        float bestSpeed = 0;
        BlockState state = Blocks.PISTON.getDefaultState();
        for (int i = 8; i >= 0; i--) {
            float speed = Breaker.getBlockBreakingSpeed(state, mc, i);
            if ((speed > bestSpeed && speed > 1.0F)
                    || (speed >= bestSpeed && !mc.player.getInventory().getStack(i).isDamageable())) {
                bestSlot = i;
                bestSpeed = speed;
            }
        }
        return bestSlot;
    }
    public static void ProcessBreaking(MinecraftClient mc, BlockPos pos, Direction facing, BlockPos torchPos, BlockPos slimePos) {
        //System.out.println(pos.toShortString()); //debug where?
        switchTool(mc, pos);
        if (slimePos != null && !mc.world.getBlockState(slimePos).isAir()) {
            attackBlock(mc, slimePos, Direction.UP);
        } else {
            attackBlock(mc, torchPos, Direction.UP);
        }
        attackBlock(mc, pos, Direction.UP);
        placePiston(mc, pos, facing);

    }

    public static void attackBlock(MinecraftClient mc, BlockPos pos, Direction direction) {
        if(mc.world.getBlockState(pos).isAir()){return;}
        //System.out.println(pos.toShortString());
        if (mc.interactionManager.attackBlock(pos,direction) && mc.world.getBlockState(pos).isAir()){
            positionStorage.registerPos(pos, false);}
        //mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, direction));
    }

    public static void resetFailure(MinecraftClient mc, PositionCache FailedCache) {
        BlockPos pos = FailedCache.getPos();
        BlockPos torchPos = FailedCache.getTorch();
        BlockPos slimePos = FailedCache.getSlimePos();
        if (slimePos != null && !mc.world.getBlockState(slimePos).isAir()) {
            attackBlock(mc, slimePos, Direction.UP);
        } else {
            attackBlock(mc, torchPos, Direction.UP);
        }
        switchTool(mc, pos);
        attackBlock(mc, pos, Direction.UP);
        FailedCache.setFalse();
    }
    public static boolean isBlockNotInstantBreakable (Block block) {
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
    public static void processRemainder(MinecraftClient mc){
        ArrayList<BlockPos> attackList = positionStorage.getFalseMarkedHasBlockPosinAttackRange(mc.world, mc.player.getPos(), MaxReach);
        for (BlockPos position : attackList) {
            attackBlock(mc, position, Direction.UP);
        }
    }
    public static void scheduledTickHandler(MinecraftClient mc, @Nullable BlockPos pos) {
        rangeX = EASY_PLACE_MODE_RANGE_X.getIntegerValue();
        rangeY = EASY_PLACE_MODE_RANGE_Y.getIntegerValue();
        rangeZ = EASY_PLACE_MODE_RANGE_Z.getIntegerValue(); //reset range values
        MaxReach = Math.max(Math.max(rangeX, rangeY), rangeZ);
        removeScheduledPos(mc);
        processRemainder(mc);
        if (pos != null && isPositionInRange(mc, pos)  && canProcess(mc, pos) && new Date().getTime() - lastPlaced > 1000.0 * EASY_PLACE_MODE_DELAY.getDoubleValue()
        ) {
            //System.out.println(pos.toShortString());
            TorchPath torch = getPistonTorchPosDir(mc, pos);
            if (torch != null && torch.isAllPosInRange(mc)) {
                lastPlaced = new Date().getTime();
                BlockPos TorchPos = torch.TorchPos;
                Direction TorchFacing = torch.Torchfacing;
                BlockPos PistonPos = torch.PistonPos;
                Direction PistonFacing = torch.Pistonfacing;
                Direction PistonExtendFacing = torch.PistonBreakableFacing;
                BlockPos SlimePos = torch.slimePos;
                if (SlimePos != null) {
                    placeSlime(mc, SlimePos);
                }
                placeTorch(mc, TorchPos, TorchFacing);
                placePiston(mc, PistonPos, PistonFacing);
                targetPosMap.put(pos.asLong(), new PositionCache(PistonPos, PistonExtendFacing, TorchPos, pos, SlimePos, lastPlaced));
            }
            positionStorage.refresh(mc.world);
        }
        for (Long posLong : targetPosMap.keySet()) {
            PositionCache item = targetPosMap.get(posLong);
            BlockPos position = BlockPos.fromLong(posLong);
            if (item == null || !item.isAllPosInRange(mc)) {continue;}
            if (!isPositionInRange(mc, position)  && !item.isAvailable()) { //
                continue;
            }
            if (!isItemPrePared(mc)) {
                break;
            }
            if (item.isClear()) {
                continue;
            }
            if (isPistonPowered(mc, item.getPos()) && !item.isHandled() && item.isAvailable()) {
                ProcessBreaking(mc, item.getPos(), item.getFacing(), item.getTorch(), item.getSlimePos());
                item.markHandle();
                item.updateSystime(new Date().getTime());
                continue;
            }
            if (!isPistonPowered(mc, item.getPos()) && !item.isHandled() && item.isAvailable()) {
                attackBlock(mc, item.getPos(), Direction.DOWN);
                resetFailure(mc, item);
                item.markFail();
                item.markClear();
                continue;
            }
            if (item.isHandled() && item.isAvailable() && isBlockNotInstantBreakable(mc.world.getBlockState(item.gettargetPos()).getBlock())) {
                item.markFail();
                attackBlock(mc, item.getPos(), Direction.DOWN);
                resetFailure(mc, item);
                item.markClear();
                continue;
            } else if (item.isHandled() && item.isAvailable() && !isBlockNotInstantBreakable(mc.world.getBlockState(item.gettargetPos()).getBlock())) {
                attackBlock(mc, item.getPos(), Direction.DOWN);
                resetFailure(mc, item);
                item.markClear();
                continue;
            }
            if (!item.isClear() && item.isAvailable()) {
                attackBlock(mc, item.getPos(), Direction.DOWN);
                item.markFail();
                resetFailure(mc, item);
                item.markClear();
            }
        }
    }

    public static void tick() {
        CurrentTick += 1L;
    }
    public static class PositionCache {
        private final BlockPos pos;
        private final Direction facing;
        private final BlockPos torchPos;
        private final BlockPos targetPos;
        private long SysTime;
        private boolean Handled;
        private boolean Fail;
        private boolean Clear;
        private BlockPos slimePos = null;

        private PositionCache(BlockPos pos, Direction facing, BlockPos torchPos, BlockPos targetPos, long SysTime) {
            this.pos = pos;
            this.facing = facing;
            this.torchPos = torchPos;
            this.targetPos = targetPos;
            this.SysTime = CurrentTick;
            //this.SysTime = SysTime;
            this.Handled = false;
            this.Fail = false;
            this.Clear = false;
        }
        private PositionCache(BlockPos pos, Direction facing, BlockPos torchPos, BlockPos targetPos, BlockPos slimePos, long SysTime) {
            this.pos = pos;
            this.facing = facing;
            this.torchPos = torchPos;
            this.targetPos = targetPos;
            this.SysTime = CurrentTick;
            //this.SysTime = SysTime;
            this.Handled = false;
            this.Fail = false;
            this.Clear = false;
            this.slimePos = slimePos;
        }
        public void setFalse(){
            positionStorage.registerPos(this.pos, false);
            positionStorage.registerPos(this.torchPos, false);
        }
        public BlockPos getPos() {
            return this.pos;
        }

        public BlockPos getSlimePos(){ return this.slimePos;}

        public BlockPos gettargetPos() {
            return this.targetPos;
        }

        public BlockPos getTorch() {
            return this.torchPos;
        }

        public Direction getFacing() {
            return this.facing;
        }

        public void markHandle() {
            this.Handled = true;
        }

        public void markFail() {
            this.Fail = true;
        }

        public void markClear() {
            this.Clear = true;
        }

        public void updateSystime(long now) {
            this.SysTime = CurrentTick;
            //this.SysTime = now;
        }
        public boolean isAllPosInRange(MinecraftClient mc){
            return mc.player.squaredDistanceTo(Vec3d.ofCenter(this.pos))< MaxReach* MaxReach && mc.player.squaredDistanceTo(Vec3d.ofCenter(this.torchPos))< MaxReach* MaxReach &&
                    mc.player.squaredDistanceTo(Vec3d.ofCenter(this.targetPos))< MaxReach* MaxReach && (this.slimePos == null || mc.player.squaredDistanceTo(Vec3d.ofCenter(this.slimePos))< MaxReach* MaxReach );
        }
        public boolean canSafeRemove(World world){
            return world.getBlockState(this.targetPos).isAir() && world.getBlockState(this.torchPos).isAir() && world.getBlockState(this.pos).isAir() && (this.slimePos == null || world.getBlockState(this.slimePos).isAir());
        }
        public boolean isHandled() {
            return this.Handled;
        }

        public boolean isFail() {
            return this.Fail;
        }

        public boolean isClear() {
            return this.Clear;
        }

        public boolean isAvailable() {
            return CurrentTick - this.SysTime > Math.max(4, (int) (20 * EASY_PLACE_MODE_DELAY.getDoubleValue()));
            //return new Date().getTime() - SysTime > 2000.0 * EASY_PLACE_MODE_DELAY.getDoubleValue();
        }
        public long getSysTime() {
            return this.SysTime;
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
        public double distance(BlockPos ReferPos, double distance) {
            BlockPos pos = this.targetPos;
            int aX = pos.getX();
            int aY = pos.getY();
            int aZ = pos.getZ();
            int pX = ReferPos.getX();
            int pY = ReferPos.getY();
            int pZ = ReferPos.getZ();
            return (pX - aX) * (pX - aX) + (pY - aY) * (pY - aY) + (pZ - aZ) * (pZ - aZ);
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
        public TorchPath(BlockPos TorchPos, Direction Torchfacing, BlockPos PistonPos, Direction Pistonfacing, Direction PistonBreakableFacing, BlockPos slimePos) {
            this.TorchPos = TorchPos;
            this.Torchfacing = Torchfacing;
            this.Pistonfacing = Pistonfacing;
            this.PistonPos = PistonPos;
            this.slimePos = slimePos;
            this.PistonBreakableFacing = PistonBreakableFacing;
        }
        public void toStr() {
            System.out.println("torch");
            System.out.println(this.TorchPos);
            System.out.println(this.Torchfacing);
            System.out.println("piston");
            System.out.println(this.PistonPos);
            System.out.println(this.Pistonfacing);
            System.out.println("piston2");
            System.out.println(this.PistonBreakableFacing);
            System.out.println("slimepos");
            System.out.println(this.slimePos);
        }
        public boolean isAllPosInRange(MinecraftClient mc){
            return  mc.player.squaredDistanceTo(Vec3d.ofCenter(this.TorchPos))< MaxReach* MaxReach && mc.player.squaredDistanceTo(Vec3d.ofCenter(this.PistonPos))< MaxReach* MaxReach &&
                    (this.slimePos == null || mc.player.squaredDistanceTo(Vec3d.ofCenter(this.slimePos))< MaxReach* MaxReach );
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
