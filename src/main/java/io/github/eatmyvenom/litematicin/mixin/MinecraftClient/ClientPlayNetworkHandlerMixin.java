package io.github.eatmyvenom.litematicin.mixin.MinecraftClient;

import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.tool.ToolMode;
import io.github.eatmyvenom.litematicin.LitematicaMixinMod;
import io.github.eatmyvenom.litematicin.utils.Printer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.s2c.play.DisconnectS2CPacket;
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
	private static boolean isSynced = false;

	/*
			SyncHandler defined at ServerPlayerEntity is responsible to sync, but actually client process actions and executes too. so Server sync does not match with client, which causes desync.
			We can see ghost items in this context, especially with high ping. But, if there's no packet loss, whatever client has executed will be done in order correctly, like click recipe -> press Q in result slot even if its empty.
			So server sync is not totally required for most cases.
		 */
	@Inject(method = "onScreenHandlerSlotUpdate", at = @At("HEAD"), cancellable = true, require = 0)
	private void onUpdateSlots(ScreenHandlerSlotUpdateS2CPacket packet, CallbackInfo ci) {
		final ClientPlayerEntity player = this.client.player;
		if (!isSynced && player != null) {
			isSynced = true;
			while (packet.getRevision() != player.currentScreenHandler.getRevision()) {
				player.currentScreenHandler.nextRevision();
			}
			//player.sendMessage(Text.of("Matched rev on start : "+ packet.getRevision()));
			return;
		}
		if (player != null && LitematicaMixinMod.DEBUG_PACKET_SYNC.getBooleanValue()) {
			int rev = player.currentScreenHandler.getRevision();
			//MessageHolder.sendPacketOrders("Recieved packet of revision " + packet.getRevision() + " current revision is " + rev);
			if (LitematicaMixinMod.DISABLE_SYNC.getBooleanValue()) {
				if (!(this.client.currentScreen instanceof CreativeInventoryScreen) && shouldCancel(rev, packet.getRevision())) {
					ci.cancel();
				} else {
					while (packet.getRevision() != player.currentScreenHandler.getRevision()) {
						player.currentScreenHandler.nextRevision();
					}
					if (packet.getSlot() == -1) {
						//okay wtf? server is actually trying to disconnect client.
						if (packet.getSyncId() == -1 && !(this.client.currentScreen instanceof CreativeInventoryScreen)) {
							this.client.execute(() -> player.currentScreenHandler.setCursorStack(packet.getItemStack()));
						}
						return;
					}
					this.client.execute(() -> player.currentScreenHandler.setStackInSlot(packet.getSlot(), packet.getRevision(), packet.getItemStack()));
				}
				//MessageHolder.sendMessageUnchecked("Cancelled ");
			}
			return;
		}
		cancelIfRequired(ci);
	}
	//#if MC<=12001
	@Inject(method = "onDisconnect", at = @At("HEAD"))
	private void handleDisconnect(DisconnectS2CPacket packet, CallbackInfo ci) {
		isSynced = false;
	}
	//#endif

	@Inject(method = "onUpdateSelectedSlot", at = @At("HEAD"), cancellable = true, require = 0)
	private void onUpdateSelectSlots(UpdateSelectedSlotS2CPacket packet, CallbackInfo ci) {
		cancelIfRequired(ci);
	}

	private void cancelIfRequired(CallbackInfo ci) {
		if (Printer.isSleeping) {
			return;
		}
		if (DataManager.getToolMode() != ToolMode.REBUILD && Configs.Generic.EASY_PLACE_MODE.getBooleanValue() && Configs.Generic.EASY_PLACE_HOLD_ENABLED.getBooleanValue() && Hotkeys.EASY_PLACE_ACTIVATION.getKeybind().isKeybindHeld()) {
			if (LitematicaMixinMod.DISABLE_SYNC.getBooleanValue()) {
				ci.cancel();
			}
		}
	}

	private static boolean shouldCancel(int current, int packet) {
		if (current == packet) {
			return false;
		}
		int abs = Math.abs(current - packet);
		if (abs > 1024 && abs < 32760) {
			return false;
		}
		return (Math.abs(current - packet) > 32760) == (current < packet);
	}

}