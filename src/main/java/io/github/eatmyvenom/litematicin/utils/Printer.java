package io.github.eatmyvenom.litematicin.utils;

import static io.github.eatmyvenom.litematicin.LitematicaMixinMod.EASY_PLACE_MODE_BREAK_BLOCKS;
import static io.github.eatmyvenom.litematicin.LitematicaMixinMod.EASY_PLACE_MODE_DELAY;
import static io.github.eatmyvenom.litematicin.LitematicaMixinMod.EASY_PLACE_MODE_HOTBAR_ONLY;
import static io.github.eatmyvenom.litematicin.LitematicaMixinMod.EASY_PLACE_MODE_MAX_BLOCKS;
import static io.github.eatmyvenom.litematicin.LitematicaMixinMod.EASY_PLACE_MODE_RANGE_X;
import static io.github.eatmyvenom.litematicin.LitematicaMixinMod.EASY_PLACE_MODE_RANGE_Y;
import static io.github.eatmyvenom.litematicin.LitematicaMixinMod.EASY_PLACE_MODE_RANGE_Z;
import static io.github.eatmyvenom.litematicin.LitematicaMixinMod.FLIPPIN_CACTUS; //now only applied for rail blocks, sometimes observer flipping can help redstone order too.

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
import net.minecraft.block.AbstractButtonBlock;
import net.minecraft.block.AbstractRailBlock;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BeehiveBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CarvedPumpkinBlock;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.ComparatorBlock;
import net.minecraft.block.DetectorRailBlock;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.DropperBlock;
import net.minecraft.block.EndPortalFrameBlock;
import net.minecraft.block.EndRodBlock;
import net.minecraft.block.EnderChestBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.FurnaceBlock;
import net.minecraft.block.HopperBlock;
import net.minecraft.block.LadderBlock;
import net.minecraft.block.LecternBlock;
import net.minecraft.block.LeverBlock;
import net.minecraft.block.LoomBlock;
import net.minecraft.block.Material;
import net.minecraft.block.NoteBlock;
import net.minecraft.block.ObserverBlock;
import net.minecraft.block.PillarBlock;
import net.minecraft.block.PistonBlock;
import net.minecraft.block.PoweredRailBlock;
import net.minecraft.block.PumpkinBlock;
import net.minecraft.block.RailBlock;
import net.minecraft.block.RepeaterBlock;
import net.minecraft.block.SandBlock;
import net.minecraft.block.SnowBlock;
import net.minecraft.block.ConcretePowderBlock;
import net.minecraft.block.GravelBlock;
import net.minecraft.block.AnvilBlock;
import net.minecraft.block.DragonEggBlock;
import net.minecraft.block.RedstoneBlock;
import net.minecraft.block.SeaPickleBlock;
import net.minecraft.block.SignBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.StonecutterBlock;
import net.minecraft.block.TorchBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.TripwireHookBlock;
import net.minecraft.block.WallMountedBlock;
import net.minecraft.block.WallRedstoneTorchBlock;
import net.minecraft.block.WallSignBlock;
import net.minecraft.block.WallTorchBlock;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.block.enums.SlabType;
import net.minecraft.block.enums.WallMountLocation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class Printer {

    private static class FacingData {
        public int type;
        public boolean isReversed;

        FacingData(int type, boolean isrev) {
            this.type = type;
            this.isReversed = isrev;
        }
    }

    private static final Map<Class<? extends Block>, FacingData> facingMap = new LinkedHashMap<Class<? extends Block>, FacingData>();

    private static final List<PositionCache> positionCache = new ArrayList<>();
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
        addFD(ChestBlock.class, new FacingData(1, true));
        addFD(RepeaterBlock.class, new FacingData(1, true));
        addFD(ComparatorBlock.class, new FacingData(1, true));
        addFD(EnderChestBlock.class, new FacingData(1, true));
        addFD(FurnaceBlock.class, new FacingData(1, true));
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

        if (stack.isEmpty() == false) {
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


    public static ActionResult doAccuratePlacePrinter(MinecraftClient mc) {
	return null;
    }
    
    // For printing delay
    public static long lastPlaced = new Date().getTime();
    public static Breaker breaker = new Breaker();
    
    public static int worldBottomY = 0;
    public static int worldTopY = 256;

    @Environment(EnvType.CLIENT)
    public static ActionResult doPrinterAction(MinecraftClient mc) {
    	if (breaker.isBreakingBlock()) return ActionResult.SUCCESS;
    	if (new Date().getTime() < lastPlaced + 1000.0 * EASY_PLACE_MODE_DELAY.getDoubleValue()) return ActionResult.PASS;

    	
        RayTraceWrapper traceWrapper = RayTraceUtils.getGenericTrace(mc.world, mc.player, 6, true);
        if (traceWrapper == null) {
            return ActionResult.FAIL;
        }
        BlockHitResult trace = traceWrapper.getBlockHitResult();
        BlockPos tracePos = trace.getBlockPos();
        int posX = tracePos.getX();
        int posY = tracePos.getY();
        int posZ = tracePos.getZ();

        SubChunkPos cpos = new SubChunkPos(tracePos);
        List<PlacementPart> list = DataManager.getSchematicPlacementManager().getAllPlacementsTouchingSubChunk(cpos);

        if (list.isEmpty()) {
            return ActionResult.PASS;
        }
        int maxX = 0;
        int maxY = 0;
        int maxZ = 0;
        int minX = 0;
        int minY = 0;
        int minZ = 0;

        boolean foundBox = false;
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

        if (!foundBox) {
            return ActionResult.PASS;
        }
        LayerRange range = DataManager.getRenderLayerRange(); //add range following
        int rangeX = EASY_PLACE_MODE_RANGE_X.getIntegerValue();
        int rangeY = EASY_PLACE_MODE_RANGE_Y.getIntegerValue();
        int rangeZ = EASY_PLACE_MODE_RANGE_Z.getIntegerValue();
        int MaxReach = Math.max(Math.max(rangeX,rangeY),rangeZ);
        boolean breakBlocks = EASY_PLACE_MODE_BREAK_BLOCKS.getBooleanValue();
        boolean Flippincactus = FLIPPIN_CACTUS.getBooleanValue();
        ItemStack Mainhandstack = mc.player.getMainHandStack();
        boolean Cactus = Mainhandstack.getItem().getTranslationKey().contains((String) "cactus") && Flippincactus; 
        boolean MaxFlip = true;
        if (!Flippincactus || !Cactus) {MaxFlip = false;};
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

        toY = Math.max(Math.min(toY,worldTopY),worldBottomY);
        fromY = Math.max(Math.min(fromY, worldTopY),worldBottomY); 

        fromX = Math.max(fromX,(int)mc.player.getX() - rangeX);
        fromY = Math.max(fromY,(int)mc.player.getY() - rangeY);
        fromZ = Math.max(fromZ,(int)mc.player.getZ() - rangeZ);

        toX = Math.min(toX,(int)mc.player.getX() + rangeX);
        toY = Math.min(toY,(int)mc.player.getY() + rangeY);
        toZ = Math.min(toZ,(int)mc.player.getZ() + rangeZ);
        
        for (int x = fromX; x <= toX; x++) {
            for (int y = fromY; y <= toY; y++) {
                for (int z = fromZ; z <= toZ; z++) {


                    double dx = mc.player.getX() - x - 0.5;
                    double dy = mc.player.getY() - y - 0.5;
                    double dz = mc.player.getZ() - z - 0.5;

                    if (dx * dx + dy * dy + dz * dz > MaxReach * MaxReach) // Check if within reach distance
                        continue;

                    BlockPos pos = new BlockPos(x, y, z);
                    if (range.isPositionWithinRange(pos) == false)
                        continue;
                    BlockState stateSchematic = world.getBlockState(pos);
                    BlockState stateClient = mc.world.getBlockState(pos);
                    if (breakBlocks && stateSchematic != null && !stateClient.isAir() && !stateClient.getBlock().getTranslationKey().contains((String) "water") && !stateClient.getBlock().getTranslationKey().contains((String) "lava") && !stateClient.getBlock().getTranslationKey().contains((String) "column")) {
                        if (!stateClient.getBlock().getName().equals(stateSchematic.getBlock().getName()) && dx * dx + Math.pow(dy + 1.5,2) + dz * dz <= MaxReach * MaxReach) {
                            
                        	if (mc.player.getAbilities().creativeMode) {
                        		mc.interactionManager.attackBlock(pos, Direction.DOWN);
                                interact++;

                                if (interact >= maxInteract) {
                                	lastPlaced = new Date().getTime();
                                    return ActionResult.SUCCESS;
                                }
                        	} else { // For survival
				mc.interactionManager.attackBlock(pos, Direction.DOWN); //yes, this seemingly needless line adds functionality but paper would not allow it.
                            	breaker.startBreakingBlock(pos, mc); // it need to avoid unbreakable blocks and just added and lava, but its not block so somehow made it work
                            	return ActionResult.SUCCESS;
                        	}
                        }
                    }
                    if (stateSchematic.isAir())
                    continue;
                    
                    // Abort if there is already a block in the target position
                    if (MaxFlip|| printerCheckCancel(stateSchematic, stateClient, mc.player)) {


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
                                    if (sBlock instanceof RepeaterBlock) {
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
                                    } else if (sBlock instanceof ComparatorBlock) {
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
                                  
                                }  
                            }
                        } else if (MaxFlip) {
                            Block cBlock = stateClient.getBlock();
                            Block sBlock = stateSchematic.getBlock();
                            if (cBlock.getName().equals(sBlock.getName())) {
                                             boolean ShapeBoolean = false;
			  boolean ShouldFix = false;
                                             if (sBlock.getTranslationKey().contains((String) "rail")) {
				
				if (sBlock instanceof RailBlock) {
					String SchematicRailShape = stateSchematic.get(RailBlock.SHAPE).toString();
					String ClientRailShape = stateClient.get(RailBlock.SHAPE).toString();
					ShouldFix =SchematicRailShape!=ClientRailShape;
					ShapeBoolean =  SchematicRailShape!=ClientRailShape && ((SchematicRailShape== "south_west" ||SchematicRailShape  == "north_west"||SchematicRailShape  == "south_east"||SchematicRailShape  == "north_east") &&(ClientRailShape == "south_west" ||ClientRailShape  == "north_west"||ClientRailShape  == "south_east"||ClientRailShape  == "north_east") || (SchematicRailShape== "east_west" || SchematicRailShape == "north_south") &&(ClientRailShape== "east_west" ||ClientRailShape=="north_south" )); 
				} else {
					String SchematicRailShape = stateSchematic.get(PoweredRailBlock.SHAPE).toString();
					String ClientRailShape = stateClient.get(PoweredRailBlock.SHAPE).toString();
					ShouldFix =SchematicRailShape!=ClientRailShape;
                                                 		ShapeBoolean = SchematicRailShape!=ClientRailShape &&(SchematicRailShape== "east_west" || SchematicRailShape == "north_south") &&(ClientRailShape== "east_west" ||ClientRailShape=="north_south" ) ;}
                                             	
                                             }
			else if (sBlock instanceof ObserverBlock || sBlock instanceof PistonBlock) {
                                			String facingSchematicName = fi.dy.masa.malilib.util.BlockUtils.getFirstPropertyFacingValue(stateSchematic).getName();
                               			String facingClientName = fi.dy.masa.malilib.util.BlockUtils.getFirstPropertyFacingValue(stateClient).getName();
					ShouldFix =facingSchematicName!=facingClientName;
					ShapeBoolean =  (facingSchematicName == "north" &&facingClientName == "south" ) || (facingSchematicName == "east" &&facingClientName == "west" ) ||(facingSchematicName == "up" &&facingClientName == "down" ) ||
							(facingSchematicName == "south" &&facingClientName == "north" ) || (facingSchematicName == "west" &&facingClientName == "east" ) ||(facingSchematicName == "down" &&facingClientName == "up" ) ;
				} else if (sBlock instanceof RepeaterBlock || sBlock instanceof ComparatorBlock ||  sBlock instanceof FenceGateBlock || sBlock instanceof TrapdoorBlock) {ShapeBoolean = fi.dy.masa.malilib.util.BlockUtils.getFirstPropertyFacingValue(stateSchematic).getName() != fi.dy.masa.malilib.util.BlockUtils.getFirstPropertyFacingValue(stateClient).getName();}
                                             Direction side = Direction.UP;
                                             if (ShapeBoolean) {
                                                 Hand hand = Hand.MAIN_HAND;
                                                 Vec3d hitPos = new Vec3d(pos.getX()+0.5,pos.getY()+0.5,pos.getZ()+0.5);
                                                 BlockHitResult hitResult = new BlockHitResult(hitPos, side, pos, false);
                                                 mc.interactionManager.interactBlock(mc.player, mc.world, hand, hitResult);
                                                 interact++;
                                             } else if(breakBlocks&& ShouldFix) {mc.interactionManager.attackBlock(pos, Direction.DOWN);breaker.startBreakingBlock(pos, mc);
						return ActionResult.SUCCESS ;} 
			continue;
                                    };
                             } 
                        continue;
                    }
                    if (MaxFlip) {continue;};
                    if (isPositionCached(pos, false)) {
                        continue;
                    }

                    ItemStack stack = ((MaterialCache) MaterialCache.getInstance()).getRequiredBuildItemForState((BlockState)stateSchematic);
                    if (stack.isEmpty() == false && (mc.player.getAbilities().creativeMode || mc.player.getInventory().getSlotWithStack(stack) != -1)) {
                            Block sBlock = stateSchematic.getBlock();
                        if (stateSchematic == stateClient) {
                            continue;
                        } else if (sBlock instanceof SandBlock || sBlock instanceof DragonEggBlock || sBlock instanceof ConcretePowderBlock || sBlock instanceof GravelBlock || sBlock instanceof AnvilBlock) {
			BlockPos Offsetpos = new BlockPos(x, y-1, z);
                    		BlockState OffsetstateSchematic = world.getBlockState(Offsetpos);
                    		BlockState OffsetstateClient = mc.world.getBlockState(Offsetpos);
                                        if (OffsetstateClient.isAir() || (breakBlocks && !OffsetstateClient.getBlock().getName().equals(OffsetstateSchematic.getBlock().getName())) )
                                            continue;
                        }  if (sBlock instanceof RedstoneBlock) {
		if(isQCable(mc,world, pos)){continue;}
		}
                        Direction facing = fi.dy.masa.malilib.util.BlockUtils
                                .getFirstPropertyFacingValue(stateSchematic);
                        if (facing != null) {
                            FacingData facedata = getFacingData(stateSchematic);
                            if (!canPlaceFace(facedata, stateSchematic, mc.player, primaryFacing, horizontalFacing))
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
                        double offX = 0.5; // We dont really need this. But I did it anyway so that I could experiment
                                           // easily.
                        double offY = 0.5;
                        double offZ = 0.5;

                        Direction sideOrig = Direction.NORTH;
                        BlockPos npos = pos;
                        Direction side = applyPlacementFacing(stateSchematic, sideOrig, stateClient);
                        Block blockSchematic = stateSchematic.getBlock();
                        if (blockSchematic instanceof TorchBlock) {
                        BlockPos Offsetpos = new BlockPos(x, y-1, z);
                        BlockState OffsetstateSchematic = world.getBlockState(Offsetpos);
                        BlockState OffsetstateClient = mc.world.getBlockState(Offsetpos);
                                if(OffsetstateClient.getBlock().getTranslationKey().contains((String) "water") || OffsetstateClient.getBlock().getTranslationKey().contains((String) "lava") || OffsetstateClient.getBlock().getTranslationKey().contains((String) "column")){
                                continue;}
                        }
                        if (blockSchematic instanceof WallMountedBlock || blockSchematic instanceof TorchBlock
                                || blockSchematic instanceof LadderBlock || blockSchematic instanceof TrapdoorBlock
                                || blockSchematic instanceof TripwireHookBlock || blockSchematic instanceof SignBlock || blockSchematic instanceof EndRodBlock) {

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

                            if (doSchematicWorldPickBlock(true, mc, stateSchematic, pos) == false) {
                                return ActionResult.FAIL;
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

                        Vec3d hitPos = new Vec3d(offX, offY, offZ);
                        // Carpet Accurate Placement protocol support, plus BlockSlab support
                        hitPos = applyHitVec(npos, stateSchematic, hitPos, side);

                        // Mark that this position has been handled (use the non-offset position that is
                        // checked above)
                        cacheEasyPlacePosition(pos, false);

                        BlockHitResult hitResult = new BlockHitResult(hitPos, side, npos, false);

                        // System.out.printf("pos: %s side: %s, hit: %s\n", pos, side, hitPos);
                        // pos, side, hitPos

                        mc.interactionManager.interactBlock(mc.player, mc.world, hand, hitResult);
                        interact++;
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
                                && stateSchematic.get(SeaPickleBlock.PICKLES)>1) {
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
        return ActionResult.FAIL;
    }
    private static boolean isQCable(MinecraftClient mc,  World world, BlockPos pos) {
	BlockPos posoffset = pos.down();
	BlockPos poseast = posoffset.east();
	BlockPos poswest = posoffset.west();
	BlockPos posnorth = posoffset.north();
	BlockPos possouth = posoffset.south();
	Iterable<BlockPos> OffsetIterable = List.of(poseast, poswest, posnorth, possouth);
	for (BlockPos Position: OffsetIterable) 
		{BlockState stateClient = mc.world.getBlockState(Position);
		  BlockState stateSchematic = world.getBlockState(Position);
		  if(stateClient.isAir() && stateSchematic.getBlock() instanceof PistonBlock &&  stateSchematic.get(PistonBlock.EXTENDED).toString().contains("false")) {return true;} 
		 }
	return false;};



    /*
     * Checks if the block can be placed in the correct orientation if player is
     * facing a certain direction Dont place block if orientation will be wrong
     */
    private static boolean canPlaceFace(FacingData facedata, BlockState stateSchematic, PlayerEntity player,
            Direction primaryFacing, Direction horizontalFacing) {
        Direction facing = fi.dy.masa.malilib.util.BlockUtils.getFirstPropertyFacingValue(stateSchematic);
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
        if (blockSchematic instanceof SeaPickleBlock && stateSchematic.get(SeaPickleBlock.PICKLES) >1) {
            Block blockClient = stateClient.getBlock();

            if (blockClient instanceof SeaPickleBlock && stateClient.get(SeaPickleBlock.PICKLES) != stateSchematic.get(SeaPickleBlock.PICKLES)) {
                return blockSchematic != blockClient;
            }
        }
        if (blockSchematic instanceof SlabBlock && stateSchematic.get(SlabBlock.TYPE) == SlabType.DOUBLE) {
            Block blockClient = stateClient.getBlock();

            if (blockClient instanceof SlabBlock && stateClient.get(SlabBlock.TYPE) != SlabType.DOUBLE) {
                return blockSchematic != blockClient;
            }
        }
        Block blockClient = stateClient.getBlock();
        if (blockClient instanceof SnowBlock && stateClient.get(SnowBlock.LAYERS) <3) {
                return false;
        }
        if (stateClient.isAir() || stateClient.getBlock().getTranslationKey().contains((String) "water") || stateClient.getBlock().getTranslationKey().contains((String) "lava")|| stateClient.getBlock().getTranslationKey().contains((String) "column")) // This is a lot simpler than below. But slightly lacks functionality.
            return false;
        /*
         * if (trace.getType() != HitResult.Type.BLOCK) { return false; }
         */
        // BlockHitResult hitResult = (BlockHitResult) trace;
        // ItemPlacementContext ctx = new ItemPlacementContext(new
        // ItemUsageContext(player, Hand.MAIN_HAND, hitResult));

        // if (stateClient.canReplace(ctx) == false) {
        // return true;
        // }

        return true;
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
            dy = 0;
        } else if (side == Direction.EAST) {
            dx = 1;
        } else if (side == Direction.WEST) {
            dx = 0;
        } else if (side == Direction.SOUTH) {
            dz = 1;
        } else if (side == Direction.NORTH) {
            dz = 0;
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

    /*
     * Gets the direction necessary to build the block oriented correctly. TODO:
     * Need a better way to do this.
     */
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
        }

        // TODO: Add more for other blocks
        return side;
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
