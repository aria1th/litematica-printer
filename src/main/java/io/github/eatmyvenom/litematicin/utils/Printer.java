package io.github.eatmyvenom.litematicin.utils;

import static io.github.eatmyvenom.litematicin.LitematicaMixinMod.EASY_PLACE_MODE_BREAK_BLOCKS;
import static io.github.eatmyvenom.litematicin.LitematicaMixinMod.EASY_PLACE_MODE_REDSTONE_ORDERS;
import static io.github.eatmyvenom.litematicin.LitematicaMixinMod.EASY_PLACE_MODE_DELAY;
import static io.github.eatmyvenom.litematicin.LitematicaMixinMod.EASY_PLACE_MODE_HOTBAR_ONLY;
import static io.github.eatmyvenom.litematicin.LitematicaMixinMod.EASY_PLACE_MODE_MAX_BLOCKS;
import static io.github.eatmyvenom.litematicin.LitematicaMixinMod.EASY_PLACE_MODE_RANGE_X;
import static io.github.eatmyvenom.litematicin.LitematicaMixinMod.EASY_PLACE_MODE_RANGE_Y;
import static io.github.eatmyvenom.litematicin.LitematicaMixinMod.EASY_PLACE_MODE_RANGE_Z;
import static io.github.eatmyvenom.litematicin.LitematicaMixinMod.FLIPPIN_CACTUS; //now only applied for rail blocks, sometimes observer flipping can help redstone order too.
import static io.github.eatmyvenom.litematicin.LitematicaMixinMod.CLEAR_AREA_MODE;
import static io.github.eatmyvenom.litematicin.LitematicaMixinMod.BEDROCK_BREAKING;
import static io.github.eatmyvenom.litematicin.LitematicaMixinMod.CLEAR_AREA_MODE_COBBLESTONE;
import static io.github.eatmyvenom.litematicin.LitematicaMixinMod.CLEAR_AREA_MODE_SNOWPREVENT;
import static io.github.eatmyvenom.litematicin.LitematicaMixinMod.ACCURATE_BLOCK_PLACEMENT;
import static io.github.eatmyvenom.litematicin.LitematicaMixinMod.EASY_PLACE_MODE_USE_COMPOSTER;
import static io.github.eatmyvenom.litematicin.LitematicaMixinMod.ADVANCED_ACCURATE_BLOCK_PLACEMENT;
import static io.github.eatmyvenom.litematicin.LitematicaMixinMod.EASY_PLACE_MODE_OBSERVER_EXPLICIT_ORDER;

import java.util.*;

import com.google.common.collect.ImmutableMap;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.materials.MaterialCache;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager.PlacementPart;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement.RequiredEnabled;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.util.EntityUtils;
import fi.dy.masa.litematica.util.InventoryUtils;
import fi.dy.masa.litematica.util.RayTraceUtils;
import fi.dy.masa.litematica.util.RayTraceUtils.RayTraceWrapper;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.malilib.util.IntBoundingBox;
import fi.dy.masa.malilib.util.LayerRange;
import fi.dy.masa.malilib.util.SubChunkPos;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import net.minecraft.block.*;
import net.minecraft.fluid.LavaFluid;
import net.minecraft.fluid.WaterFluid;
import net.minecraft.item.BlockItem;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

import net.minecraft.block.enums.ComparatorMode;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.block.enums.SlabType;
import net.minecraft.block.enums.WallMountLocation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.World;

public class Printer {

    private static final Map<Class<? extends Block>, FacingData> facingMap = new LinkedHashMap<Class<? extends Block>, FacingData>();
    private static final List<PositionCache> positionCache = new ArrayList<>();
    // For printing delay
    public static long lastPlaced = new Date().getTime();
    public static Breaker breaker = new Breaker();
    public static int worldBottomY = 0;
    public static int worldTopY = 256;
    private static boolean setupFacing = false;

    private static void addFD(final Class<? extends Block> c, FacingData data) {
        facingMap.put(c, data);
    }

    private static void setUpFacingData() {
        setupFacing = true;

        /*
         * 0 = Normal up/down/east/west/south/north directions 1 = Horizontal directions
         * 2 = Wall Attactchable block
         *
         *
         * TODO: THIS CODE MUST BE CLEANED UP.
         */

        // All directions, reverse of what player is facing
        addFD(PistonBlock.class, new FacingData(0, true));
        addFD(DispenserBlock.class, new FacingData(0, true));
        addFD(DropperBlock.class, new FacingData(0, true));

        // All directions, normal direction of player
        addFD(ObserverBlock.class, new FacingData(0, false));

        // Horizontal directions, normal direction
        addFD(StairsBlock.class, new FacingData(1, false));
        addFD(DoorBlock.class, new FacingData(1, false));
        addFD(BedBlock.class, new FacingData(1, false));
        addFD(FenceGateBlock.class, new FacingData(1, false));

        // Horizontal directions, reverse of what player is facing
        addFD(BarrelBlock.class, new FacingData(1, true));
        addFD(ChestBlock.class, new FacingData(1, true));
        addFD(RepeaterBlock.class, new FacingData(1, true));
        addFD(ComparatorBlock.class, new FacingData(1, true));
        addFD(EnderChestBlock.class, new FacingData(1, true));
        addFD(FurnaceBlock.class, new FacingData(1, true));
        addFD(GlazedTerracottaBlock.class, new FacingData(1, true));
        addFD(LecternBlock.class, new FacingData(1, true));
        addFD(LoomBlock.class, new FacingData(1, true));
        addFD(BeehiveBlock.class, new FacingData(1, true));
        addFD(StonecutterBlock.class, new FacingData(1, true));
        addFD(CarvedPumpkinBlock.class, new FacingData(1, true));
        addFD(PumpkinBlock.class, new FacingData(1, true));
        addFD(EndPortalFrameBlock.class, new FacingData(1, true));

        // Top/bottom placable side mountable blocks
        addFD(LeverBlock.class, new FacingData(2, false));
        addFD(AbstractButtonBlock.class, new FacingData(2, false));
        //addFD(BellBlock.class, new FacingData(2, false));
        //addFD(GrindstoneBlock.class, new FacingData(2, false));

        // Anvils
        addFD(AnvilBlock.class, new FacingData(3, true));
        // Rails
        addFD(AbstractRailBlock.class, new FacingData(4, false));
    }

    // TODO: This must be moved to another class and not be static.
    private static FacingData getFacingData(BlockState state) {
        if (!setupFacing) {
            setUpFacingData();
        }
        Block block = state.getBlock();
        for (final Class<? extends Block> c : facingMap.keySet()) {
            if (c.isInstance(block)) {
                return facingMap.get(c);
            }
        }
        return null;
    }

    /**
     * New doSchematicWorldPickBlock that allows you to choose which block you want
     */
    @Environment(EnvType.CLIENT)
    public static boolean doSchematicWorldPickBlock(boolean closest, MinecraftClient mc, BlockState preference,
                                                    BlockPos pos) {

        World world = SchematicWorldHandler.getSchematicWorld();

        ItemStack stack = MaterialCache.getInstance().getRequiredBuildItemForState(preference, world, pos);

        if (!stack.isEmpty()) {
            PlayerInventory inv = mc.player.getInventory();

            if (mc.player.getAbilities().creativeMode) {
                // BlockEntity te = world.getBlockEntity(pos);

                // The creative mode pick block with NBT only works correctly
                // if the server world doesn't have a TileEntity in that position.
                // Otherwise it would try to write whatever that TE is into the picked
                // ItemStack.
                // if (GuiBase.isCtrlDown() && te != null && mc.world.isAir(pos)) {
                // ItemUtils.storeTEInStack(stack, te);
                // }

                // InventoryUtils.setPickedItemToHand(stack, mc);

                // NOTE: I dont know why we have to pick block in creative mode. You can simply
                // just set the block
                mc.interactionManager.clickCreativeStack(stack, 36 + inv.selectedSlot);

                return true;
            } else {

                int slot = inv.getSlotWithStack(stack);
                boolean shouldPick = inv.selectedSlot != slot;
                boolean canPick = (slot != -1) || (slot < 9 && EASY_PLACE_MODE_HOTBAR_ONLY.getBooleanValue());

                if (shouldPick && canPick) {
                    InventoryUtils.setPickedItemToHand(stack, mc);
                }

                // return shouldPick == false || canPick;
            }
        }

        return true;
    }

    public static ActionResult doEasyPlaceNormally(MinecraftClient mc) { //force normal easyplace action, ignore condition checks
        RayTraceWrapper traceWrapper = RayTraceUtils.getGenericTrace(mc.world, mc.player, 6);
        if (traceWrapper == null){
            return ActionResult.PASS;
        }
        BlockHitResult trace = traceWrapper.getBlockHitResult();
        if (trace == null){
            return ActionResult.PASS;
        }
        World world = SchematicWorldHandler.getSchematicWorld();
        World clientWorld = mc.world;
        BlockPos blockPos = trace.getBlockPos();
        BlockState schematicState = world.getBlockState(blockPos);
        BlockState clientState = clientWorld.getBlockState(blockPos);
        if (schematicState.getBlock().getName().equals(clientState.getBlock().getName())) {return ActionResult.FAIL;}
        ItemStack stack = MaterialCache.getInstance().getRequiredBuildItemForState(schematicState);
        if (!stack.isEmpty()){
            InventoryUtils.schematicWorldPickBlock(stack, blockPos, world, mc);
            Hand hand = EntityUtils.getUsedHandForItem(mc.player, stack);
            if (hand == null)
            {
                return ActionResult.FAIL;
            }
            Vec3d hitPos = trace.getPos();
            Direction sideOrig = trace.getSide();
            Direction side = applyPlacementFacing(schematicState, sideOrig, clientState);
            if (ACCURATE_BLOCK_PLACEMENT.getBooleanValue()){
                hitPos = applyCarpetProtocolHitVec(blockPos,schematicState,hitPos);
            }
            else {
                hitPos = applyHitVec(blockPos,schematicState,hitPos,side);
            }
            BlockHitResult hitResult = new BlockHitResult(hitPos, side, blockPos, false);
            ActionResult actionResult = mc.interactionManager.interactBlock(mc.player, mc.world, hand, hitResult);
            if (actionResult.isAccepted()){
                return ActionResult.SUCCESS;
            }
            else {
                return ActionResult.FAIL;
            }
        }
        return ActionResult.FAIL;
    }



    @Environment(EnvType.CLIENT)
    public static ActionResult doPrinterAction(MinecraftClient mc) {
        if (breaker.isBreakingBlock()) {
            return ActionResult.SUCCESS;
        }
        if (new Date().getTime() < lastPlaced + 1000.0 * EASY_PLACE_MODE_DELAY.getDoubleValue())
        {
            return ActionResult.PASS;
        }
        BlockPos tracePos = mc.player.getBlockPos();
        int posX = tracePos.getX();
        int posY = tracePos.getY();
        int posZ = tracePos.getZ();

        RayTraceWrapper traceWrapper = RayTraceUtils.getGenericTrace(mc.world, mc.player, 6);
        //RayTraceWrapper traceWrapper = RayTraceUtils.getGenericTrace(mc.world, mc.player, 6, true); previous litematica code
        if (traceWrapper!= null){
            BlockHitResult trace = traceWrapper.getBlockHitResult();
            tracePos = trace.getBlockPos();
            posX = tracePos.getX();
            posY = tracePos.getY();
            posZ = tracePos.getZ();
        }

        boolean ClearArea = CLEAR_AREA_MODE.getBooleanValue(); // if its true, will ignore everything and remove fluids.
        boolean UseCobble = CLEAR_AREA_MODE_COBBLESTONE.getBooleanValue() && ClearArea;
        boolean ClearSnow = CLEAR_AREA_MODE_SNOWPREVENT.getBooleanValue() && ClearArea;
        boolean CanUseProtocol = ACCURATE_BLOCK_PLACEMENT.getBooleanValue();
        boolean FillInventory = EASY_PLACE_MODE_USE_COMPOSTER.getBooleanValue();
        ItemStack ComposterItem = Items.PUMPKIN_PIE.getDefaultStack();
        SubChunkPos cpos = new SubChunkPos(tracePos);
        List<PlacementPart> list = DataManager.getSchematicPlacementManager().getAllPlacementsTouchingSubChunk(cpos);

        if (list.isEmpty() && !ClearArea) {
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
        if (rangeX == 0 && rangeY == 0 && rangeZ == 0 && traceWrapper != null){
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
            for (PlacementPart part : list) {
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
                                || posZ > boxZMax)
                            continue;
                        minX = boxXMin;
                        maxX = boxXMax;
                        minY = boxYMin;
                        maxY = boxYMax;
                        minZ = boxZMin;
                        maxZ = boxZMax;
                        foundBox = true;

                        break;
                    }

                    break;
                }
            }
        }

        if (!foundBox) {
            return ActionResult.PASS;
        }
        LayerRange range = DataManager.getRenderLayerRange(); //add range following
        int MaxReach = Math.max(Math.max(rangeX, rangeY), rangeZ);
        boolean breakBlocks = EASY_PLACE_MODE_BREAK_BLOCKS.getBooleanValue();
        boolean Flippincactus = FLIPPIN_CACTUS.getBooleanValue();
		boolean ExplicitObserver = EASY_PLACE_MODE_OBSERVER_EXPLICIT_ORDER.getBooleanValue();
        ItemStack Mainhandstack = mc.player.getMainHandStack();
        boolean Cactus = Mainhandstack.getItem().getTranslationKey().contains("cactus") && Flippincactus;
        boolean MaxFlip = Flippincactus && Cactus;
		boolean smartRedstone = EASY_PLACE_MODE_REDSTONE_ORDERS.getBooleanValue();
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

        int maxInteract = EASY_PLACE_MODE_MAX_BLOCKS.getIntegerValue();
        int interact = 0;
        boolean hasPicked = false;
        Text pickedBlock = null;

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
                    boolean shouldReverse = false;

                    double dx = mc.player.getX() - x - 0.5;
                    double dy = mc.player.getY() - y - 0.5;
                    double dz = mc.player.getZ() - z - 0.5;

                    if (dx * dx + dy * dy + dz * dz > MaxReach * MaxReach) // Check if within reach distance
                        continue;

                    BlockPos pos = new BlockPos(x, y, z);
                    if (!range.isPositionWithinRange(pos) && !ClearArea)
                        continue;
                    BlockState stateSchematic = world.getBlockState(pos);
                    BlockState stateClient = mc.world.getBlockState(pos);
                    if (!ClearArea && breakBlocks && stateSchematic != null && !(stateClient.getBlock() instanceof SnowBlock) &&
                            !stateClient.isAir() && !(stateClient.getBlock() instanceof FluidDrainable || stateClient.getBlock() instanceof FluidBlock)  &&
                            !stateClient.isOf(Blocks.PISTON_HEAD) && !stateClient.isOf(Blocks.MOVING_PISTON)) {
                        if (!stateClient.getBlock().getName().equals(stateSchematic.getBlock().getName()) ||
                                (stateClient.getBlock() instanceof SlabBlock && stateSchematic.getBlock() instanceof SlabBlock && stateClient.get(SlabBlock.TYPE)!= stateSchematic.get(SlabBlock.TYPE))
                                && dx * dx + Math.pow(dy + 1.5, 2) + dz * dz <= MaxReach * MaxReach) {

                            if (mc.player.getAbilities().creativeMode) {
                                mc.interactionManager.attackBlock(pos, Direction.DOWN);
                                interact++;

                                if (interact >= maxInteract) {
                                    lastPlaced = new Date().getTime();
                                    return ActionResult.SUCCESS;
                                }
                            } else if (BedrockBreaker.isBlockNotInstantBreakable(stateClient.getBlock()) && BEDROCK_BREAKING.getBooleanValue()) {
                                BedrockBreaker.scheduledTickHandler(mc, pos);
                                continue;

                            } else if (BEDROCK_BREAKING.getBooleanValue()) {
                                BedrockBreaker.scheduledTickHandler(mc, null);
                                continue;
                            } else if (!positionStorage.hasPos(pos)){ // For survival
	                            boolean replaceable = mc.world.getBlockState(pos).getMaterial().isReplaceable();
								if (!replaceable && mc.world.getBlockState(pos).getHardness(world, pos) == -1){continue;}
                                if (replaceable || mc.world.getBlockState(pos).getHardness(world, pos) ==0) {
									mc.interactionManager.attackBlock(pos, Direction.DOWN);
									return ActionResult.SUCCESS;
								}
                                if(!replaceable) {
									breaker.startBreakingBlock(pos, mc);
								} // it need to avoid unbreakable blocks and just added and lava, but its not block so somehow made it work
                                return ActionResult.SUCCESS;
                            }
                        }
                    }
                    if (BEDROCK_BREAKING.getBooleanValue() || (mc.player.isSneaking() && breakBlocks)) {continue;} // don't process other actions
                    if (!(stateSchematic.getBlock() instanceof NetherPortalBlock) && stateSchematic.isAir() && !ClearArea)
                        continue;

                    // Abort if there is already a block in the target position
                    if (!ClearArea && (MaxFlip || printerCheckCancel(stateSchematic, stateClient, mc.player))) {


                        /*
                         * Sometimes, blocks have other states like the delay on a repeater. So, this
                         * code clicks the block until the state is the same I don't know if Schematica
                         * does this too, I just did it because I work with a lot of redstone
                         */
                        if (!MaxFlip && !stateClient.isAir() && !mc.player.isSneaking() && !isPositionCached(pos, true)) {
                            Block cBlock = stateClient.getBlock();
                            Block sBlock = stateSchematic.getBlock();

                            if (cBlock.getName().equals(sBlock.getName())) {
                                Direction facingSchematic = fi.dy.masa.malilib.util.BlockUtils
                                        .getFirstPropertyFacingValue(stateSchematic);
                                Direction facingClient = fi.dy.masa.malilib.util.BlockUtils
                                        .getFirstPropertyFacingValue(stateClient);

                                if (facingSchematic == facingClient) {
                                    int clickTimes = 0;
                                    Direction side = Direction.NORTH;
                                    if (sBlock instanceof RepeaterBlock && !ACCURATE_BLOCK_PLACEMENT.getBooleanValue()) {
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
                                    } else if (sBlock instanceof ComparatorBlock &&!ACCURATE_BLOCK_PLACEMENT.getBooleanValue()) {
                                        if (stateSchematic.get(ComparatorBlock.MODE) != stateClient
                                                .get(ComparatorBlock.MODE))
                                            clickTimes = 1;
                                        side = Direction.UP;
                                    } else if (sBlock instanceof LeverBlock) {
                                        if (stateSchematic.get(LeverBlock.POWERED) != stateClient
                                                .get(LeverBlock.POWERED))
                                            clickTimes = 1;

                                        /*
                                         * I dont know if this direction code is needed. I am just doing it anyway to
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
                                                .get(TrapdoorBlock.OPEN) != stateClient.get(TrapdoorBlock.OPEN))
                                            clickTimes = 1;
                                    } else if (sBlock instanceof FenceGateBlock) {
                                        if (stateSchematic.get(FenceGateBlock.OPEN) != stateClient
                                                .get(FenceGateBlock.OPEN))
                                            clickTimes = 1;
                                    } else if (sBlock instanceof DoorBlock) {
                                        if (stateClient.getMaterial() != Material.METAL && stateSchematic
                                                .get(DoorBlock.OPEN) != stateClient.get(DoorBlock.OPEN))
                                            clickTimes = 1;
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
                                        int level = stateClient.get(ComposterBlock.LEVEL);
                                        int Schematiclevel = stateSchematic.get(ComposterBlock.LEVEL);
                                        if (level != Schematiclevel && !(level == 7 && Schematiclevel == 8)) {
                                            Hand hand = Hand.MAIN_HAND;
                                            if (mc.player.getInventory().getSlotWithStack(ComposterItem) != -1) {
                                                InventoryUtils.setPickedItemToHand(ComposterItem, mc);
                                            }
                                            Vec3d hitPos = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                                            BlockHitResult hitResult = new BlockHitResult(hitPos, side, pos, false);
                                            mc.interactionManager.interactBlock(mc.player, mc.world, hand, hitResult);
                                            lastPlaced = new Date().getTime();
                                            return ActionResult.SUCCESS;
                                        } else {
                                            cacheEasyPlacePosition(pos, true);
                                        }
                                    }


                                    for (int i = 0; i < clickTimes; i++) // Click on the block a few times
                                    {
                                        Hand hand = Hand.MAIN_HAND;

                                        Vec3d hitPos = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);

                                        BlockHitResult hitResult = new BlockHitResult(hitPos, side, pos, false);

                                        mc.interactionManager.interactBlock(mc.player, mc.world, hand, hitResult);
                                        interact++;
                                    }

                                    if (clickTimes > 0) {
                                        cacheEasyPlacePosition(pos, true);
                                    }

                                } //can place vanilla
                            }
                        } else if (!ClearArea && MaxFlip) {
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
                                        ShapeBoolean = !Objects.equals(SchematicRailShape, ClientRailShape) && ((Objects.equals(SchematicRailShape, "south_west") || SchematicRailShape == "north_west" || SchematicRailShape == "south_east" || SchematicRailShape == "north_east") && (ClientRailShape == "south_west" || ClientRailShape == "north_west" || ClientRailShape == "south_east" || ClientRailShape == "north_east") || (SchematicRailShape == "east_west" || SchematicRailShape == "north_south") && (ClientRailShape == "east_west" || ClientRailShape == "north_south"));
                                    } else {
                                        String SchematicRailShape = stateSchematic.get(PoweredRailBlock.SHAPE).toString();
                                        String ClientRailShape = stateClient.get(PoweredRailBlock.SHAPE).toString();
                                        ShouldFix = !Objects.equals(SchematicRailShape, ClientRailShape);
                                        ShapeBoolean = !Objects.equals(SchematicRailShape, ClientRailShape) && (Objects.equals(SchematicRailShape, "east_west") || SchematicRailShape == "north_south") && (ClientRailShape == "east_west" || ClientRailShape == "north_south");
                                    }
                                } else if (sBlock instanceof ObserverBlock || sBlock instanceof PistonBlock || sBlock instanceof RepeaterBlock || sBlock instanceof ComparatorBlock || sBlock instanceof FenceGateBlock || sBlock instanceof TrapdoorBlock) {
                                    Direction facingSchematic = fi.dy.masa.malilib.util.BlockUtils.getFirstPropertyFacingValue(stateSchematic);
                                    Direction facingClient = fi.dy.masa.malilib.util.BlockUtils.getFirstPropertyFacingValue(stateClient);
                                    ShouldFix = facingSchematic != facingClient;
                                    ShapeBoolean = facingClient.getOpposite().equals(facingSchematic);
                                }
                                Direction side = Direction.UP;
                                if (ShapeBoolean) {
                                    Hand hand = Hand.MAIN_HAND;
                                    Vec3d hitPos = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                                    BlockHitResult hitResult = new BlockHitResult(hitPos, side, pos, false);
                                    mc.interactionManager.interactBlock(mc.player, mc.world, hand, hitResult);
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
                    if (!ClearArea && MaxFlip) {
                        continue;
                    }
                    if (isPositionCached(pos, false)) {
                        continue;
                    }
                    ItemStack stack = MaterialCache.getInstance().getRequiredBuildItemForState(stateSchematic);
                    Block cBlock = stateClient.getBlock();
                    Block sBlock = stateSchematic.getBlock();
                    if (ClearArea) {
                        if (cBlock instanceof BubbleColumnBlock || cBlock instanceof SeagrassBlock || stateClient.getFluidState().getFluid() instanceof WaterFluid && stateClient.contains(FluidBlock.LEVEL) && stateClient.get(FluidBlock.LEVEL) == 0) {
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
                    } else if (sBlock instanceof NetherPortalBlock) {
                        stack = Items.FIRE_CHARGE.getDefaultStack();
						if(mc.player.getInventory().getSlotWithStack(stack) == -1){
							stack = Items.FLINT_AND_STEEL.getDefaultStack();
						}
                    }

                    if ((ClearArea || !stack.isEmpty()) && (mc.player.getAbilities().creativeMode || mc.player.getInventory().getSlotWithStack(stack) != -1)) {

                        if (ClearArea) {
                            Hand hand = Hand.MAIN_HAND;
                            if (ClearArea && mc.player.getInventory().getSlotWithStack(stack) != -1) {
                                InventoryUtils.setPickedItemToHand(stack, mc);
                                Vec3d hitPos = new Vec3d(0.5, 0.5, 0.5);
                                BlockHitResult hitResult = new BlockHitResult(hitPos, Direction.UP, pos, false);
                                mc.interactionManager.interactBlock(mc.player, mc.world, hand, hitResult);
                                if (cBlock instanceof FluidDrainable || cBlock instanceof SnowBlock || cBlock instanceof FluidBlock) {
                                    lastPlaced = new Date().getTime();
                                    continue;
                                }
                                continue;
                            }
                        }
                        if (ClearArea) {
                            continue;
                        }
                        if (stateSchematic == stateClient) {
                            continue;
                        } else if (sBlock instanceof SandBlock || sBlock instanceof DragonEggBlock || sBlock instanceof ConcretePowderBlock || sBlock instanceof GravelBlock || sBlock instanceof AnvilBlock) {
                            BlockPos Offsetpos = new BlockPos(x, y - 1, z);
                            BlockState OffsetstateSchematic = world.getBlockState(Offsetpos);
                            BlockState OffsetstateClient = mc.world.getBlockState(Offsetpos);
                            if (OffsetstateClient.isAir() || (breakBlocks && !OffsetstateClient.getBlock().getName().equals(OffsetstateSchematic.getBlock().getName()))) {
                                continue;
                            }
                        }
                        if (smartRedstone && sBlock instanceof RedstoneBlock) {
                            if (isQCable(mc, world, pos)) {
                                continue;
                            }
                        } else if (smartRedstone && sBlock instanceof PistonBlock) {
                            if (!ShouldExtendQC(mc, world, pos) || hasNearbyRedirectDust(mc, world, pos)) {
                                continue;
                            }
                            if (cantAvoidExtend(mc.world, pos, world) ){
                                continue;
                            }
                        } else if (smartRedstone && sBlock instanceof ObserverBlock) {
                            if (ObserverUpdateOrder(mc, world, pos)) {
                                if (FLIPPIN_CACTUS.getBooleanValue() && canBypass(mc, world, pos)){
                                    shouldReverse = true;
                                    stateSchematic = stateSchematic.with(ObserverBlock.FACING, stateSchematic.get(ObserverBlock.FACING).getOpposite());
                                }
                                else {
                                    continue;
                                }
                            }
                        } else if (ExplicitObserver){
							BlockPos observerPos = isObserverOutput(mc, world, pos);
							if(observerPos != null && ObserverUpdateOrder(mc, world, observerPos)){
								continue;
							}
                        }
                        if (sBlock instanceof NetherPortalBlock && !sBlock.getName().equals(cBlock.getName())) {
                            Hand hand = Hand.MAIN_HAND;
                            BlockPos Offsetpos = new BlockPos(x, y - 1, z);
                            BlockState OffsetstateSchematic = world.getBlockState(Offsetpos);
                            BlockState OffsetstateClient = mc.world.getBlockState(Offsetpos);
                            if (mc.player.getInventory().getSlotWithStack(stack) == -1 || OffsetstateClient.isAir() || (!OffsetstateClient.getBlock().getName().equals(OffsetstateSchematic.getBlock().getName()))) {
                                continue;
                            }
                            InventoryUtils.setPickedItemToHand(stack, mc);
                            Vec3d hitPos = new Vec3d(0.5, 0.5, 0.5);
                            BlockHitResult hitResult = new BlockHitResult(hitPos, Direction.DOWN, new BlockPos(x, y + 1, z), false);
                            mc.interactionManager.interactBlock(mc.player, mc.world, hand, hitResult);
                            lastPlaced = new Date().getTime();
                            return ActionResult.SUCCESS; //wait for next tick
                        }
                        Direction facing = fi.dy.masa.malilib.util.BlockUtils
                                .getFirstPropertyFacingValue(stateSchematic);
                        if (facing != null) {
                            facing = facing.getOpposite();
                        }
                        if (stateSchematic.getBlock() instanceof AbstractRailBlock) {
                            facing = convertRailShapetoFace(stateSchematic);
                        }
                        if (facing != null) {
                            FacingData facedata = getFacingData(stateSchematic);
                            if (!(CanUseProtocol && IsBlockSupportedCarpet(stateSchematic.getBlock())) && !canPlaceFace(facedata, stateSchematic, mc.player, primaryFacing, horizontalFacing))
                                continue;

                            if ((stateSchematic.getBlock() instanceof DoorBlock
                                    && stateSchematic.get(DoorBlock.HALF) == DoubleBlockHalf.UPPER)
                                    || (stateSchematic.getBlock() instanceof BedBlock
                                    && stateSchematic.get(BedBlock.PART) == BedPart.HEAD)

                            ) {
                                continue;
                            }
                        }

                        // Exception for signs (edge case)
                        if (stateSchematic.getBlock() instanceof SignBlock
                                && !(stateSchematic.getBlock() instanceof WallSignBlock)) {
                            if ((MathHelper.floor((double) ((180.0F + mc.player.getYaw()) * 16.0F / 360.0F) + 0.5D)
                                    & 15) != stateSchematic.get(SignBlock.ROTATION))
                                continue;

                        }

                        Direction sideOrig = Direction.NORTH;
                        BlockPos npos = pos;
                        Direction side = applyPlacementFacing(stateSchematic, sideOrig, stateClient);
                        Block blockSchematic = stateSchematic.getBlock();
                        if (blockSchematic instanceof TorchBlock) {
                            BlockPos Offsetpos = new BlockPos(x, y - 1, z);
                            BlockState OffsetstateSchematic = world.getBlockState(Offsetpos);
                            BlockState OffsetstateClient = mc.world.getBlockState(Offsetpos);
                            if (OffsetstateClient.getMaterial().isReplaceable()) {
                                continue;
                            }
                        }
                        if (blockSchematic instanceof WallMountedBlock || blockSchematic instanceof TorchBlock || blockSchematic instanceof WallSkullBlock
                                || blockSchematic instanceof LadderBlock || (blockSchematic instanceof TrapdoorBlock && !CanUseProtocol)
                                || blockSchematic instanceof TripwireHookBlock || blockSchematic instanceof SignBlock ||
                                blockSchematic instanceof EndRodBlock || blockSchematic instanceof DeadCoralWallFanBlock) {

                            /*
                             * Some blocks, especially wall mounted blocks must be placed on another for
                             * directionality to work Basically, the block pos sent must be a "clicked"
                             * block.
                             */
                            int px = pos.getX();
                            int py = pos.getY();
                            int pz = pos.getZ();

                            if (side == Direction.DOWN) {
                                py += 1;
                            } else if (side == Direction.UP) {
                                py += -1;
                            } else if (side == Direction.NORTH) {
                                pz += 1;
                            } else if (side == Direction.SOUTH) {
                                pz += -1;
                            } else if (side == Direction.EAST) {
                                px += -1;
                            } else if (side == Direction.WEST) {
                                px += 1;
                            }

                            npos = new BlockPos(px, py, pz);

                            BlockState clientStateItem = mc.world.getBlockState(npos);

                            if (clientStateItem == null || clientStateItem.isAir()) {
                                if (!(blockSchematic instanceof TrapdoorBlock)) {
                                    continue;
                                }
                                BlockPos testPos;

                                /*
                                 * Trapdoors are special. They can also be placed on top, or below another block
                                 */
                                if (stateSchematic.get(TrapdoorBlock.HALF) == BlockHalf.TOP) {
                                    testPos = new BlockPos(pos.getX(), pos.getY() + 1, pos.getZ());
                                    side = Direction.DOWN;
                                } else {
                                    testPos = new BlockPos(pos.getX(), pos.getY() - 1, pos.getZ());
                                    side = Direction.UP;
                                }
                                BlockState clientStateItemTest = mc.world.getBlockState(testPos);

                                if (clientStateItemTest == null || clientStateItemTest.isAir()) {
                                    BlockState schematicNItem = world.getBlockState(npos);

                                    BlockState schematicTItem = world.getBlockState(testPos);

                                    /*
                                     * If possible, it is always best to attatch the trapdoor to an actual block
                                     * that exists on the world But other times, it can't be helped
                                     */
                                    if ((schematicNItem != null && !schematicNItem.isAir())
                                            || (schematicTItem != null && !schematicTItem.isAir()))
                                        continue;
                                    npos = pos;
                                } else
                                    npos = testPos;

                                // If trapdoor is placed from top or bottom, directionality is decided by player
                                // direction
                                if (stateSchematic.get(TrapdoorBlock.FACING).getOpposite() != horizontalFacing) {
                                    continue;
                                }

                            }

                        }

                        // Abort if the required item was not able to be pick-block'd
                        if (!hasPicked) {
                            if (!doSchematicWorldPickBlock(true, mc, stateSchematic, pos)) {
                                continue;
                            }
                            hasPicked = true;
                            pickedBlock = stateSchematic.getBlock().getName();
                        } else if (pickedBlock != null && !pickedBlock.equals(stateSchematic.getBlock().getName())) {
                            continue;
                        }

                        Hand hand = EntityUtils.getUsedHandForItem(mc.player, stack);

                        // Abort if a wrong item is in the player's hand
                        if (hand == null) {
                            continue;
                        }

                        Vec3d hitPos = new Vec3d(npos.getX(), npos.getY() , npos.getZ());
                        // Carpet Accurate Placement protocol support, plus BlockSlab support
                        if (CanUseProtocol && IsBlockSupportedCarpet(stateSchematic.getBlock())) {
                            hitPos = applyCarpetProtocolHitVec(npos, stateSchematic, hitPos);
                        } else {
                            hitPos = applyHitVec(npos, stateSchematic, hitPos, side);
                        }

                        // Mark that this position has been handled (use the non-offset position that is
                        // checked above)
                        cacheEasyPlacePosition(pos, false);
                        float originYaw = mc.player.getYaw(1.0f);
                        if (stateSchematic.getBlock() instanceof AbstractRailBlock && !ADVANCED_ACCURATE_BLOCK_PLACEMENT.getBooleanValue()) {
                            float yaw;
                            if (facing == Direction.NORTH) {
                                yaw = 0f;
                            } else {
                                yaw = 90f;
                            }
                            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(yaw, mc.player.getPitch(1.0f), mc.player.isOnGround()));
                        }
                        BlockHitResult hitResult = new BlockHitResult(hitPos, side, npos, false);

                        //System.out.printf("pos: %s side: %s, hit: %s\n", pos, side, hitPos);
                        // pos, side, hitPos
                        if (stateSchematic.getBlock() instanceof SnowBlock) {
                            stateClient = mc.world.getBlockState(npos);
                            if (stateClient.isAir() || stateClient.getBlock() instanceof SnowBlock
                                    && stateClient.get(SnowBlock.LAYERS) < stateSchematic.get(SnowBlock.LAYERS)) {
                                side = Direction.UP;
                                hitResult = new BlockHitResult(hitPos, side, npos, false);
                                mc.interactionManager.interactBlock(mc.player, mc.world, hand, hitResult);
                                interact++;
                            }
                            continue;
                        }
                        mc.interactionManager.interactBlock(mc.player, mc.world, hand, hitResult);
                        interact++;
                        if (stateSchematic.getBlock() instanceof AbstractRailBlock && !ADVANCED_ACCURATE_BLOCK_PLACEMENT.getBooleanValue()) {
                            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(originYaw, mc.player.getPitch(1.0f), mc.player.isOnGround()));
                        }
                        if (stateSchematic.getBlock() instanceof SlabBlock
                                && stateSchematic.get(SlabBlock.TYPE) == SlabType.DOUBLE) {
                            stateClient = mc.world.getBlockState(npos);

                            if (stateClient.getBlock() instanceof SlabBlock
                                    && stateClient.get(SlabBlock.TYPE) != SlabType.DOUBLE) {
                                side = applyPlacementFacing(stateSchematic, sideOrig, stateClient);
                                hitResult = new BlockHitResult(hitPos, side, npos, false);
                                mc.interactionManager.interactBlock(mc.player, mc.world, hand, hitResult);
                                interact++;
                            }
                        }
                        if (stateSchematic.getBlock() instanceof SeaPickleBlock
                                && stateSchematic.get(SeaPickleBlock.PICKLES) > 1) {
                            stateClient = mc.world.getBlockState(npos);
                            if (stateClient.getBlock() instanceof SeaPickleBlock
                                    && stateClient.get(SeaPickleBlock.PICKLES) < stateSchematic.get(SeaPickleBlock.PICKLES)) {
                                side = applyPlacementFacing(stateSchematic, sideOrig, stateClient);
                                hitResult = new BlockHitResult(hitPos, side, npos, false);
                                mc.interactionManager.interactBlock(mc.player, mc.world, hand, hitResult);
                                interact++;
                            }
                        }

                        if (interact >= maxInteract) {
                            lastPlaced = new Date().getTime();
                            return ActionResult.SUCCESS;
                        }

                    }

                }
            }

        }

        if (interact > 0) {
            lastPlaced = new Date().getTime();
            return ActionResult.SUCCESS;
        }
		if(!(mc.player.getMainHandStack().getItem() instanceof BlockItem) && !(mc.player.getOffHandStack().getItem() instanceof BlockItem)){
			return ActionResult.PASS;
		}
        return ActionResult.FAIL;
    }

    private static boolean isQCable(MinecraftClient mc, World world, BlockPos pos) {
        BlockPos posoffset = pos.down();
        BlockPos poseast = posoffset.east();
        BlockPos poswest = posoffset.west();
        BlockPos posnorth = posoffset.north();
        BlockPos possouth = posoffset.south();
        Iterable<BlockPos> OffsetIterable = List.of(poseast, poswest, posnorth, possouth);
        for (BlockPos Position : OffsetIterable) {
            BlockState stateClient = mc.world.getBlockState(Position);
            BlockState stateSchematic = world.getBlockState(Position);
            if (stateSchematic.getBlock() instanceof PistonBlock && (stateClient.isAir() && !stateSchematic.get(PistonBlock.EXTENDED) ||
                    (stateClient.getBlock() instanceof PistonBlock && stateSchematic.get(PistonBlock.FACING).equals(Direction.UP) && !world.getBlockState(Position.up()).getBlock().equals(mc.world.getBlockState(Position.up()).getBlock())))) {
                return true;
            }
        }
        BlockState stateSchematic = world.getBlockState(posoffset.down());
        return stateSchematic.getBlock() instanceof PistonBlock && !stateSchematic.get(PistonBlock.EXTENDED) && !world.getBlockState(posoffset).getBlock().equals(mc.world.getBlockState(posoffset).getBlock());
    }
    private static boolean hasNearbyRedirectDust(MinecraftClient mc, World world, BlockPos pos){ //temporary code, just direct redirection check nearby
        for (Direction direction : Direction.values()){
            if (!isCorrectDustState(mc, world, pos.offset(direction))){
                return true;
            }
            if (!isCorrectDustState(mc, world, pos.offset(direction,2))){
                return true;
            }
            if (!isCorrectDustState(mc, world, pos.offset(direction).up())){
                return true;
            }
            if (!isCorrectDustState(mc, world, pos.offset(direction,2).up())){
                return true;
            }
        }
        return false;
    }
    private static boolean cantAvoidExtend(World world, BlockPos pos, World schematicWorld){
        if (!schematicWorld.getBlockState(pos).get(PistonBlock.EXTENDED)){
            return shouldExtend(world, pos, schematicWorld.getBlockState(pos).get(PistonBlock.FACING));
        }
        return false;
    }

    private static boolean isCorrectDustState(MinecraftClient mc, World world, BlockPos pos){
        BlockState ClientState = mc.world.getBlockState(pos);
        BlockState SchematicState = world.getBlockState(pos);
        if (!SchematicState.isOf(Blocks.REDSTONE_WIRE)){
            return true;
        }
        if (!ClientState.isOf(Blocks.REDSTONE_WIRE)){
            return false;
        }
        return SchematicState.get(RedstoneWireBlock.WIRE_CONNECTION_EAST) == ClientState.get(RedstoneWireBlock.WIRE_CONNECTION_EAST) &&
                SchematicState.get(RedstoneWireBlock.WIRE_CONNECTION_WEST) == ClientState.get(RedstoneWireBlock.WIRE_CONNECTION_WEST) &&
                SchematicState.get(RedstoneWireBlock.WIRE_CONNECTION_SOUTH) == ClientState.get(RedstoneWireBlock.WIRE_CONNECTION_SOUTH) &&
                SchematicState.get(RedstoneWireBlock.WIRE_CONNECTION_NORTH) == ClientState.get(RedstoneWireBlock.WIRE_CONNECTION_NORTH) &&
                Objects.equals(SchematicState.get(RedstoneWireBlock.POWER)==0, ClientState.get(RedstoneWireBlock.POWER)==0);
    }
    private static boolean ShouldExtendQC(MinecraftClient mc, World world, BlockPos pos) {

        return shouldExtend(mc.world, pos, world.getBlockState(pos).get(PistonBlock.FACING)) == world.getBlockState(pos).get(PistonBlock.EXTENDED);
    }

    private static boolean shouldExtend(World world, BlockPos pos, Direction pistonFace) {
        for (Direction lv : Direction.values()) {
            if (lv == pistonFace || !world.isEmittingRedstonePower(pos.offset(lv), lv)) continue;
            return true;
        }
        if (world.isEmittingRedstonePower(pos, Direction.DOWN)) {
            return true;
        }
        BlockPos lv2 = pos.up();
        for (Direction lv3 : Direction.values()) {
            if (lv3 == Direction.DOWN || !world.isEmittingRedstonePower(lv2.offset(lv3), lv3)) continue;
            return true;
        }
        return false;
    }
	private static BlockPos isObserverOutput(MinecraftClient mc, World schematicWorld, BlockPos pos){
		for (Direction direction : Direction.values()){
			BlockState offsetState = schematicWorld.getBlockState(pos.offset(direction));
			if (!mc.world.getBlockState(pos.offset(direction)).isOf(Blocks.OBSERVER) && offsetState.getBlock() instanceof ObserverBlock && offsetState.get(ObserverBlock.FACING) == direction){
				return pos.offset(direction);
			}
		}
		return null;
	}

    private static boolean ObserverUpdateOrder(MinecraftClient mc, World world, BlockPos pos) {
        boolean ExplicitObserver = EASY_PLACE_MODE_OBSERVER_EXPLICIT_ORDER.getBooleanValue();
        BlockState stateSchematic = world.getBlockState(pos);
        BlockPos Posoffset = pos;
        BlockState OffsetStateSchematic;
        BlockState OffsetStateClient;
        if (stateSchematic.get(ObserverBlock.POWERED)) {
            return false;
        }
        Direction facingSchematic = fi.dy.masa.malilib.util.BlockUtils.getFirstPropertyFacingValue(stateSchematic);
        assert facingSchematic != null;
        if (facingSchematic.equals(Direction.UP)) {
            Posoffset = pos.up();
            OffsetStateSchematic = world.getBlockState(Posoffset);
            OffsetStateClient = mc.world.getBlockState(Posoffset);
            if (OffsetStateSchematic.getBlock() instanceof WallBlock || OffsetStateSchematic.getBlock() instanceof ComparatorBlock ||
                    OffsetStateSchematic.getBlock() instanceof RepeaterBlock || OffsetStateSchematic.getBlock() instanceof FallingBlock ||
                    OffsetStateSchematic.getBlock() instanceof AbstractRailBlock ||
                    OffsetStateSchematic.getBlock() instanceof BubbleColumnBlock || OffsetStateSchematic.getBlock() instanceof RedstoneWireBlock ||
                    ((OffsetStateSchematic.getBlock() instanceof WallMountedBlock) && OffsetStateSchematic.get(WallMountedBlock.FACE) == WallMountLocation.FLOOR)) {
                return false;
            }
        }
        else if (facingSchematic.equals(Direction.DOWN)) {
            Posoffset = pos.down();
            OffsetStateSchematic = world.getBlockState(Posoffset);
            OffsetStateClient = mc.world.getBlockState(Posoffset);
            if (OffsetStateSchematic.getBlock() instanceof WallBlock || OffsetStateSchematic.getBlock() instanceof WallMountedBlock && OffsetStateSchematic.get(WallMountedBlock.FACE) == WallMountLocation.CEILING) {
                return false;
            }
        }
        else {
            Posoffset = pos.offset(facingSchematic);
            OffsetStateSchematic = world.getBlockState(Posoffset);
            OffsetStateClient = mc.world.getBlockState(Posoffset);
            if (OffsetStateSchematic.getBlock() instanceof WallBlock || OffsetStateSchematic.getBlock() instanceof WallMountedBlock &&
                    OffsetStateSchematic.get(WallMountedBlock.FACE) == WallMountLocation.WALL && OffsetStateSchematic.get(WallMountedBlock.FACING) == facingSchematic) {
                return false;
            }
        }
        OffsetStateSchematic = world.getBlockState(Posoffset);
        OffsetStateClient = mc.world.getBlockState(Posoffset);
        if (ExplicitObserver) {
            return !OffsetStateSchematic.toString().equals(OffsetStateClient.toString());
        }
        return !OffsetStateClient.getBlock().equals(OffsetStateSchematic.getBlock());
    }

    /*
     * Checks if the block can be placed in the correct orientation if player is
     * facing a certain direction Dont place block if orientation will be wrong
     */
    private static boolean canPlaceFace(FacingData facedata, BlockState stateSchematic, PlayerEntity player,
                                        Direction primaryFacing, Direction horizontalFacing) {
        Direction facing = fi.dy.masa.malilib.util.BlockUtils.getFirstPropertyFacingValue(stateSchematic);
        if (stateSchematic.getBlock() instanceof AbstractRailBlock) {
            facing = convertRailShapetoFace(stateSchematic);
        }
        if (facing != null && facedata != null) {

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
                            || facing == horizontalFacing;
                case 3: //rotated, why, anvil, WNES order
                    return horizontalFacing.rotateYClockwise() == facing;
                case 4: //rails
                    return true;
                //return facing == horizontalFacing || facing == horizontalFacing.getOpposite();
                default: // Ignore rest -> TODO: Other blocks like anvils, etc...
                    return true;
            }
        } else {
            return true;
        }
    }

    private static boolean printerCheckCancel(BlockState stateSchematic, BlockState stateClient,
                                              PlayerEntity player) {
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
        // This is a lot simpler than below. But slightly lacks functionality.
        return !stateClient.isAir() && !stateClient.getMaterial().isReplaceable();
        /*
         * if (trace.getType() != HitResult.Type.BLOCK) { return false; }
         */
        // BlockHitResult hitResult = (BlockHitResult) trace;
        // ItemPlacementContext ctx = new ItemPlacementContext(new
        // ItemUsageContext(player, Hand.MAIN_HAND, hitResult));

        // if (stateClient.canReplace(ctx) == false) {
        // return true;
        // }
    }

    /**
     * Apply hit vectors (used to be Carpet hit vec protocol, but I think it is
     * uneccessary now with orientation/states programmed in)
     *
     * @param pos
     * @param state
     * @param hitVecIn
     * @return
     */
    public static Vec3d applyHitVec(BlockPos pos, BlockState state, Vec3d hitVecIn, Direction side) {
        double x = pos.getX();
        double y = pos.getY();
        double z = pos.getZ();

        double dx = hitVecIn.getX();
        double dy = hitVecIn.getY();
        double dz = hitVecIn.getZ();
        Block block = state.getBlock();

        /*
         * I dont know if this is needed, just doing to mimick client According to the
         * MC protocol wiki, the protocol expects a 1 on a side that is clicked
         */
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

        if (block instanceof StairsBlock) {
            if (state.get(StairsBlock.HALF) == BlockHalf.TOP) {
                dy = 0.9;
            } else {
                dy = 0;
            }
        } else if (block instanceof SlabBlock && state.get(SlabBlock.TYPE) != SlabType.DOUBLE) {
            if (state.get(SlabBlock.TYPE) == SlabType.TOP) {
                dy = 0.9;
            } else {
                dy = 0;
            }
        } else if (block instanceof TrapdoorBlock) {
            if (state.get(TrapdoorBlock.HALF) == BlockHalf.TOP) {
                dy = 0.9;
            } else {
                dy = 0;
            }
        }
        return new Vec3d(x + dx, y + dy, z + dz);
    }
    private static boolean canBypass(MinecraftClient mc, World world, BlockPos pos){
        Direction direction = world.getBlockState(pos).get(ObserverBlock.FACING);
        BlockPos posOffset = pos.offset(direction.getOpposite());
        return world.getBlockState(posOffset) == null || world.getBlockState(posOffset).isAir()|| (!hasPowerRelatedState(mc.world.getBlockState(posOffset).getBlock())) && mc.world.getBlockState(posOffset).getBlock().getName() == world.getBlockState(posOffset).getBlock().getName();

    }
    private static boolean hasPowerRelatedState(Block block){
        return block instanceof LeavesBlock || block instanceof FluidBlock || block instanceof ObserverBlock || block instanceof PistonBlock || block instanceof PoweredRailBlock || block instanceof DetectorRailBlock ||
                block instanceof DispenserBlock || block instanceof AbstractRedstoneGateBlock || block instanceof LeverBlock || block instanceof TrapdoorBlock || block instanceof RedstoneTorchBlock ||
                block instanceof DoorBlock || block instanceof RedstoneWireBlock || block instanceof RedstoneOreBlock || block instanceof RedstoneLampBlock || block instanceof NoteBlock || block instanceof FenceGateBlock ||
                block instanceof ScaffoldingBlock;
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
    /*
     * Gets the direction necessary to build the block oriented correctly. TODO:
     * Need a better way to do this.
     */
    private static Boolean IsBlockSupportedCarpet(Block SchematicBlock) {
        if (SchematicBlock instanceof WallMountedBlock || SchematicBlock instanceof WallSkullBlock) {
            return false;
        }
        return ADVANCED_ACCURATE_BLOCK_PLACEMENT.getBooleanValue() || SchematicBlock instanceof GlazedTerracottaBlock || SchematicBlock instanceof ObserverBlock || SchematicBlock instanceof RepeaterBlock || SchematicBlock instanceof TrapdoorBlock ||
                SchematicBlock instanceof ComparatorBlock || SchematicBlock instanceof DispenserBlock || SchematicBlock instanceof PistonBlock || SchematicBlock instanceof StairsBlock;
    }

    private static Direction applyPlacementFacing(BlockState stateSchematic, Direction side, BlockState stateClient) {
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
        } else if (blockSchematic instanceof HopperBlock) {
            return stateSchematic.get(HopperBlock.FACING).getOpposite();
        } else if (blockSchematic instanceof TorchBlock) {

            if (blockSchematic instanceof WallTorchBlock) {
                return stateSchematic.get(WallTorchBlock.FACING);
            } else if (blockSchematic instanceof WallRedstoneTorchBlock) {
                return stateSchematic.get(WallRedstoneTorchBlock.FACING);
            } else {
                return Direction.UP;
            }
        } else if (blockSchematic instanceof LadderBlock) {
            return stateSchematic.get(LadderBlock.FACING);
        } else if (blockSchematic instanceof TrapdoorBlock) {
            return stateSchematic.get(TrapdoorBlock.FACING);
        } else if (blockSchematic instanceof TripwireHookBlock) {
            return stateSchematic.get(TripwireHookBlock.FACING);
        } else if (blockSchematic instanceof EndRodBlock) {
            return stateSchematic.get(EndRodBlock.FACING);
        } else if (blockSchematic instanceof AnvilBlock) {
            if (ADVANCED_ACCURATE_BLOCK_PLACEMENT.getBooleanValue() || ACCURATE_BLOCK_PLACEMENT.getBooleanValue() && IsBlockSupportedCarpet(blockSchematic)) {
                return stateSchematic.get(AnvilBlock.FACING);
            }
            return stateSchematic.get(AnvilBlock.FACING).rotateYCounterclockwise();
        } else if (blockSchematic instanceof AbstractRailBlock) {
            return convertRailShapetoFace(stateSchematic);
        }

        // TODO: Add more for other blocks
        return side;
    }

    private static Direction convertRailShapetoFace(BlockState state) {
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

        for (int i = 0; i < positionCache.size(); ++i) {
            PositionCache val = positionCache.get(i);
            boolean expired = val.hasExpired(currentTime);

            if (expired) {
                positionCache.remove(i);
                --i;
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

    private static void cacheEasyPlacePosition(BlockPos pos, boolean useClicked) {
        PositionCache item = new PositionCache(pos, System.nanoTime(), useClicked ? 1000000000 : 2000000000);
        // TODO: Create a separate cache for clickable items, as this just makes
        // duplicates
        if (useClicked)
            item.hasClicked = true;
        positionCache.add(item);
    }

    public static Vec3d applyCarpetProtocolHitVec(BlockPos pos, BlockState state, Vec3d hitVecIn)
    {
        double code = hitVecIn.x;
        double y = hitVecIn.y;
        double z = hitVecIn.z;
        Block block = state.getBlock();
        Direction facing = fi.dy.masa.malilib.util.BlockUtils.getFirstPropertyFacingValue(state);
        Integer railEnumCode = getRailShapeOrder(state);
        final int propertyIncrement = 16;
        double relX = hitVecIn.x - pos.getX();
        if (facing == null && railEnumCode == 32 && !(block instanceof SlabBlock))
        {
            return new Vec3d (code, y, z);
        }
        if (facing != null)
        {
            code = facing.getId();
        }
        else if (railEnumCode != 32)
        {
            code = railEnumCode;
        }
        if (block instanceof RepeaterBlock)
        {
            code += ((state.get(RepeaterBlock.DELAY))) * (propertyIncrement);
        }
        else if (block instanceof TrapdoorBlock && state.get(TrapdoorBlock.HALF) == BlockHalf.TOP)
        {
            code += propertyIncrement;
        }
        else if (block instanceof ComparatorBlock && state.get(ComparatorBlock.MODE) == ComparatorMode.SUBTRACT)
        {
            code += propertyIncrement;
        }
        else if (block instanceof StairsBlock && state.get(StairsBlock.HALF) == BlockHalf.TOP)
        {
            code += propertyIncrement;
        }
        else if (block instanceof SlabBlock && state.get(SlabBlock.TYPE) != SlabType.DOUBLE)
        {
            if (state.get(SlabBlock.TYPE) == SlabType.TOP) //side should not be down
            {
                y = pos.getY() + 0.9;
                //code += propertyIncrement; //slab type by protocol soon?
            }
            else
            {
                y = pos.getY();
            }
        }
        return new Vec3d(code * 2 + 2 + pos.getX(), y, z);
    }
    public static Integer getRailShapeOrder(BlockState state)
    {
        Block stateBlock = state.getBlock();
        if (stateBlock instanceof AbstractRailBlock)
        {
            if (stateBlock instanceof RailBlock)
            {
                return state.get(RailBlock.SHAPE).ordinal();
            }
            else if (stateBlock instanceof DetectorRailBlock)
            {
                return state.get(DetectorRailBlock.SHAPE).ordinal();
            }
            else
            {
                return state.get(PoweredRailBlock.SHAPE).ordinal();
            }
        }
        else
        {
            return 32;
        }
    }
    private static class FacingData {
        public int type;
        public boolean isReversed;

        FacingData(int type, boolean isrev) {
            this.type = type;
            this.isReversed = isrev;
        }
    }

    public static class PositionCache {
        private final BlockPos pos;
        private final long time;
        private final long timeout;
        public boolean hasClicked = false;

        private PositionCache(BlockPos pos, long time, long timeout) {
            this.pos = pos;
            this.time = time;
            this.timeout = timeout;
        }

        public BlockPos getPos() {
            return this.pos;
        }

        public boolean hasExpired(long currentTime) {
            return currentTime - this.time > this.timeout;
        }
    }
}
