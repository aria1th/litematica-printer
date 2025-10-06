package io.github.eatmyvenom.litematicin.mixin.quasiEssentialClient;

import io.github.eatmyvenom.litematicin.utils.FakeAccurateBlockPlacement;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static io.github.eatmyvenom.litematicin.utils.FakeAccurateBlockPlacement.shouldModifyValues;

//see https://github.com/senseiwells/EssentialClient/blob/1.19.x/src/main/java/me/senseiwells/essentialclient/mixins/betterAccurateBlockPlacement/ClientPlayerEntityMixin.java for reference!!

@Mixin(value = ClientPlayerEntity.class, priority = 1200)
public abstract class ClientPlayerEntityMixin extends PlayerEntity {
	public ClientPlayerEntityMixin(World world, GameProfile gameProfile) {
		super(world, gameProfile);
	}

	@Unique
	private boolean canSendPacketNormally() {
		// if FakeAccurateBlockPlacement is active, then return false
		return !shouldModifyValues();
	}

	@Redirect(method = "sendMovementPackets", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;sendPacket(Lnet/minecraft/network/packet/Packet;)V", ordinal = 0), require = 0)
	private void onSendPacketFull(ClientPlayNetworkHandler clientPlayNetworkHandler, Packet<?> packet) {
		if (canSendPacketNormally()) {
			clientPlayNetworkHandler.sendPacket(packet);
			return;
		}
		clientPlayNetworkHandler.sendPacket(new PlayerMoveC2SPacket.Full(
			this.getX(), this.hasVehicle()? -999.0D : this.getY(), this.getZ(),
			FakeAccurateBlockPlacement.fakeYaw,
			FakeAccurateBlockPlacement.fakePitch,
			this.isOnGround(),
			false
		));
	}

	@Redirect(method = "sendMovementPackets", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;sendPacket(Lnet/minecraft/network/packet/Packet;)V", ordinal = 2), require = 0)
	private void onSendPacketLookAndOnGround(ClientPlayNetworkHandler clientPlayNetworkHandler, Packet<?> packet) {
		if (canSendPacketNormally()) {
			clientPlayNetworkHandler.sendPacket(packet);
			return;
		}
		clientPlayNetworkHandler.sendPacket(
			new PlayerMoveC2SPacket.LookAndOnGround(
				FakeAccurateBlockPlacement.fakeYaw,
				FakeAccurateBlockPlacement.fakePitch,
				this.isOnGround(),
				false
			)
		);
	}
}