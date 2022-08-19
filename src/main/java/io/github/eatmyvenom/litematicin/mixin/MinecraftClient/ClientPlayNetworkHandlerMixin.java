package io.github.eatmyvenom.litematicin.mixin.MinecraftClient;

import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.tool.ToolMode;
import io.github.eatmyvenom.litematicin.LitematicaMixinMod;
import io.github.eatmyvenom.litematicin.utils.Printer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
	@Shadow
	@Final
	private MinecraftClient client;

	@Inject(method = "onScreenHandlerSlotUpdate", at = @At("HEAD"), cancellable = true, require = 0)
	private void onUpdateSlots(ScreenHandlerSlotUpdateS2CPacket packet, CallbackInfo ci) {
		if (Printer.isSleeping) {
			return;
		}
		if (DataManager.getToolMode() != ToolMode.REBUILD && Configs.Generic.EASY_PLACE_MODE.getBooleanValue() && Configs.Generic.EASY_PLACE_HOLD_ENABLED.getBooleanValue() && Hotkeys.EASY_PLACE_ACTIVATION.getKeybind().isKeybindHeld()) {
			if (LitematicaMixinMod.DISABLE_SYNC.getBooleanValue()) {
				ci.cancel();
			}
		}
	}

	@Inject(method = "onUpdateSelectedSlot", at = @At("HEAD"), cancellable = true, require = 0)
	private void onUpdateSelectSlots(UpdateSelectedSlotS2CPacket packet, CallbackInfo ci) {
		if (Printer.isSleeping) {
			return;
		}
		if (DataManager.getToolMode() != ToolMode.REBUILD && Configs.Generic.EASY_PLACE_MODE.getBooleanValue() && Configs.Generic.EASY_PLACE_HOLD_ENABLED.getBooleanValue() && Hotkeys.EASY_PLACE_ACTIVATION.getKeybind().isKeybindHeld()) {
			if (LitematicaMixinMod.DISABLE_SYNC.getBooleanValue()) {
				ci.cancel();
			}
		}
	}
}