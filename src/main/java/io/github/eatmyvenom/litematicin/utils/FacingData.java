package io.github.eatmyvenom.litematicin.utils;


import net.minecraft.block.BarrelBlock;
import net.minecraft.block.Block;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.*;

import java.util.LinkedHashMap;
import java.util.Map;

public class FacingData{
	public int type;
	public boolean isReversed;

	public FacingData (int type, boolean isReversed) {
			this.type = type;
			this.isReversed = isReversed;
	}
	private static final Map<Class<? extends Block>, FacingData> facingMap = new LinkedHashMap<>();
	private static boolean setupFacing = false;

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
		addFD(AbstractRailBlock.class, new FacingData(0, false));
		// Horizontal directions, normal direction
		addFD(StairsBlock.class, new FacingData(1, false));
		addFD(DoorBlock.class, new FacingData(1, false));
		addFD(BedBlock.class, new FacingData(1, false));
		addFD(FenceGateBlock.class, new FacingData(1, false));

		// Horizontal directions, reverse of what player is facing
		addFD(TrapdoorBlock.class, new FacingData(1, true)); //actually it is used when side is not horizontal
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
		//addFD(AbstractButtonBlock.class, new FacingData(2, false));
		//addFD(BellBlock.class, new FacingData(2, false));
		addFD(GrindstoneBlock.class, new FacingData(2, true));

		// Anvils
		addFD(AnvilBlock.class, new FacingData(3, true));
		// Rails
		addFD(AbstractRailBlock.class, new FacingData(4, false));
	}
	private static void addFD(final Class<? extends Block> c, FacingData data) {
		facingMap.put(c, data);
	}

	public static FacingData getFacingData(BlockState state) {
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
}