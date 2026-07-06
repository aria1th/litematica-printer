package io.github.eatmyvenom.litematicin.mixin.Litematica;

import net.minecraft.client.renderer.chunk.VisGraph;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.litematica.render.schematic.BlockModelRendererSchematic;
import fi.dy.masa.litematica.render.schematic.ChunkMeshDataSchematic;
import fi.dy.masa.litematica.render.schematic.ChunkCacheSchematic;
import fi.dy.masa.litematica.render.schematic.ChunkRenderDataSchematic;
import fi.dy.masa.litematica.render.schematic.ChunkRenderDispatcherBuffers;
import fi.dy.masa.litematica.render.schematic.ChunkRendererSchematicVbo;
import fi.dy.masa.litematica.render.schematic.FluidModelRendererSchematic;
import fi.dy.masa.litematica.render.schematic.IBlockOutputSchematic;

import static io.github.eatmyvenom.litematicin.LitematicaMixinMod.RENDER_ONLY_HOLDING_ITEMS;
import static io.github.eatmyvenom.litematicin.utils.InventoryUtils.ITEMS;

@Mixin(value = ChunkRendererSchematicVbo.class, priority = 1200)
public class ChunkRendererSchematicVboMixin {

	@Shadow
	protected ChunkCacheSchematic schematicWorldView;

	@Inject(method = "renderBlocksAndOverlay", at = @At("HEAD"), cancellable = true, remap = false)
	private void onRenderBlocksAndOverlay(BlockModelRendererSchematic blockRenderer, FluidModelRendererSchematic fluidRenderer, BlockPos pos, ChunkRenderDataSchematic data, ChunkMeshDataSchematic meshData, ChunkRenderDispatcherBuffers pack, IBlockOutputSchematic blockOutput, Vec3 cameraPos, VisGraph visibilityGraph, CallbackInfo ci) {
		if (!RENDER_ONLY_HOLDING_ITEMS.getBooleanValue()) return;
		BlockState stateSchematic = this.schematicWorldView.getBlockState(pos);
		Item item = stateSchematic.getBlock().asItem();
		if (!ITEMS.contains(item)) {
			ci.cancel();
		}
	}

}
