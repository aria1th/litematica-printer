package io.github.eatmyvenom.litematicin.mixin.Litematica;

import fi.dy.masa.litematica.render.schematic.*;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

import static io.github.eatmyvenom.litematicin.LitematicaMixinMod.RENDER_ONLY_HOLDING_ITEMS;
import static io.github.eatmyvenom.litematicin.utils.InventoryUtils.ITEMS;

@Mixin(value = ChunkRendererSchematicVbo.class, priority = 1200)
public class ChunkRendererSchematicVboMixin {

	@Shadow
	protected ChunkCacheSchematic schematicWorldView;

	@Inject(method = "renderBlocksAndOverlay", at = @At("HEAD"), cancellable = true, remap = false)
	//#if MC >= 12100
	//$$ private void onRenderBlocksAndOverlay(BlockPos pos, ChunkRenderDataSchematic data, BufferAllocatorCache allocators, Set<BlockEntity> tileEntities, Set<RenderLayer> usedLayers, MatrixStack matrixStack, CallbackInfo ci) {
	//#else
	private void onRenderBlocksAndOverlay(BlockPos pos, ChunkRenderDataSchematic data, Set<BlockEntity> tileEntities, Set<RenderLayer> usedLayers, MatrixStack matrices, BufferBuilderCache buffers, CallbackInfo ci) {
	//#endif
		if (!RENDER_ONLY_HOLDING_ITEMS.getBooleanValue()) return;
		BlockState stateSchematic = this.schematicWorldView.getBlockState(pos);
		Item item = stateSchematic.getBlock().asItem();
		if (!ITEMS.contains(item)) {
			ci.cancel();
		}
	}

}
