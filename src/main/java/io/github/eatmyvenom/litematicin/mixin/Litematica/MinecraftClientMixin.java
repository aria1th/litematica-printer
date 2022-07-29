package io.github.eatmyvenom.litematicin.mixin.Litematica;

import io.github.eatmyvenom.litematicin.utils.BedrockBreaker;
import io.github.eatmyvenom.litematicin.utils.ItemInputs;
import io.github.eatmyvenom.litematicin.utils.Printer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
	@Shadow
	public HitResult crosshairTarget;

	@Shadow
	@Nullable
	public ClientWorld world;

	// On join a new world/server
	@Inject(at = @At("HEAD"), method = "joinWorld")
	public void joinWorld(ClientWorld world, CallbackInfo ci) {
		Printer.worldBottomY = world.getBottomY();
		Printer.worldTopY = world.getTopY();
	}

	@Inject(at = @At("HEAD"), method = "tick")
	public void onPrinterTickCount(CallbackInfo info) {
		BedrockBreaker.tick();
	}

	@Inject(at = @At("HEAD"), method = "doItemUse")
	public void getIfBlockEntity(CallbackInfo info) {
		if (crosshairTarget != null && crosshairTarget.getType() == HitResult.Type.BLOCK && this.world.getBlockEntity(((BlockHitResult) crosshairTarget).getBlockPos()) != null) {
			ItemInputs.clickedPos = ((BlockHitResult) crosshairTarget).getBlockPos();
		} else {
			ItemInputs.clickedPos = null;
		}
	}
}
