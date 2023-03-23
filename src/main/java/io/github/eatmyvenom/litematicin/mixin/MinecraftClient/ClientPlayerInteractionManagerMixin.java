package io.github.eatmyvenom.litematicin.mixin.MinecraftClient;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {
	@Shadow
	@Final
	private MinecraftClient client;

	@Inject(method = "clickSlot", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;sendPacket(Lnet/minecraft/network/packet/Packet;)V"), require = 1)
	private void getNextRevision(int syncId, int slotId, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo ci) {
		player.currentScreenHandler.nextRevision();
	}

	@Inject(method = "clickCreativeStack", at = @At("TAIL"), require = 1)
	private void getNextRevision(ItemStack stack, int slotId, CallbackInfo ci) {
		final ClientPlayerEntity player = this.client.player;
		if (player != null) {
			if (!(player.currentScreenHandler instanceof CreativeInventoryScreen.CreativeScreenHandler) && !player.getInventory().getStack(slotId).equals(stack)) {
				player.currentScreenHandler.nextRevision();
				//player.sendMessage(Text.of("Slot was "+ slotId+ " stack was " +stack));
				player.currentScreenHandler.setStackInSlot(slotId, player.currentScreenHandler.getRevision(), stack);
			}
		}
	}
}