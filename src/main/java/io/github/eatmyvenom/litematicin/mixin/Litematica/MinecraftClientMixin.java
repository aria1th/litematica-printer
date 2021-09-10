package io.github.eatmyvenom.litematicin.mixin.Litematica;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.eatmyvenom.litematicin.utils.Printer;
import io.github.eatmyvenom.litematicin.utils.BedrockBreaker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;


@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
	
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

}
