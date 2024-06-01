package io.github.eatmyvenom.litematicin.mixin.Litematica;

import java.util.Set;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;
import io.github.eatmyvenom.litematicin.LitematicaMixinMod;
import fi.dy.masa.litematica.render.schematic.ChunkCacheSchematic;
import fi.dy.masa.litematica.render.schematic.ChunkRenderDataSchematic;
import fi.dy.masa.litematica.render.schematic.ChunkRendererSchematicVbo;

import static io.github.eatmyvenom.litematicin.utils.InventoryUtils.ITEMS;

@Mixin(value = ChunkRendererSchematicVbo.class, priority = 1200)
public class ChunkRendererSchematicVboMixin
{

	@Shadow
	protected ChunkCacheSchematic schematicWorldView;

	@Inject(method = "renderBlocksAndOverlay", at = @At("HEAD"), cancellable = true, remap = false)
	private void onRenderBlocksAndOverlay(BlockPos pos, ChunkRenderDataSchematic data, Set<BlockEntity> tileEntities, Set<RenderLayer> usedLayers, MatrixStack matrixStack, CallbackInfo ci) {
		if (!LitematicaMixinMod.RENDER_ONLY_HOLDING_ITEMS.getBooleanValue()) return;
		BlockState stateSchematic = this.schematicWorldView.getBlockState(pos);
		Item item = stateSchematic.getBlock().asItem();
		if (!ITEMS.contains(item)) {
			ci.cancel();
		}
	}

}
