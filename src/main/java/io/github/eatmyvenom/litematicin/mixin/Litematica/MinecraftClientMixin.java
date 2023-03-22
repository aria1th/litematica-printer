package io.github.eatmyvenom.litematicin.mixin.Litematica;

import io.github.eatmyvenom.litematicin.utils.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
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
public abstract class MinecraftClientMixin {
	@Shadow
	public HitResult crosshairTarget;

	@Shadow
	@Nullable
	public ClientWorld world;

	@Shadow
	@Nullable
	public abstract ClientPlayNetworkHandler getNetworkHandler();

	@Shadow
	@Nullable
	public ClientPlayerEntity player;

	// On join a new world/server
	@Inject(at = @At("HEAD"), method = "joinWorld")
	public void joinWorld(ClientWorld world, CallbackInfo ci) {
		Printer.worldBottomY = 0;
		Printer.worldTopY = 255;
	}

	@Inject(at = @At("HEAD"), method = "tick")
	public void onPrinterTickCount(CallbackInfo info) {
		BedrockBreaker.tick();
		InventoryUtils.tick();
		FakeAccurateBlockPlacement.tick(this.getNetworkHandler(), this.player);
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
