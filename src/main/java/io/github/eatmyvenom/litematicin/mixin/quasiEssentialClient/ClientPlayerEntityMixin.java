package io.github.eatmyvenom.litematicin.mixin.quasiEssentialClient;

import com.mojang.authlib.GameProfile;
import io.github.eatmyvenom.litematicin.utils.FakeAccurateBlockPlacement;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.Packet;
import net.minecraft.network.encryption.PlayerPublicKey;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

//see https://github.com/senseiwells/EssentialClient/blob/1.19.x/src/main/java/me/senseiwells/essentialclient/mixins/betterAccurateBlockPlacement/ClientPlayerEntityMixin.java for reference!!

@Mixin(value = ClientPlayerEntity.class, priority = 1200)
public abstract class ClientPlayerEntityMixin extends PlayerEntity {
	public ClientPlayerEntityMixin(World world, BlockPos pos, float yaw, GameProfile gameProfile) {
		super(world, pos, yaw, gameProfile);
	}

	@Redirect(method = "sendMovementPackets", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;sendPacket(Lnet/minecraft/network/Packet;)V", ordinal = 1), require = 0)
	private void onSendPacketVehicle(ClientPlayNetworkHandler clientPlayNetworkHandler, Packet<?> packet) {
		if (FakeAccurateBlockPlacement.requestedTicks <= -3 || FakeAccurateBlockPlacement.fakeDirection == null) {
			clientPlayNetworkHandler.sendPacket(packet);
			return;
		}
		clientPlayNetworkHandler.sendPacket(new PlayerMoveC2SPacket.Full(
			this.getX(), -999.0D, this.getZ(),
			FakeAccurateBlockPlacement.fakeYaw,
			FakeAccurateBlockPlacement.fakePitch,
			this.isOnGround()
		));
	}

	@Redirect(method = "sendMovementPackets", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;sendPacket(Lnet/minecraft/network/Packet;)V", ordinal = 2), require = 0)
	private void onSendPacketAll(ClientPlayNetworkHandler clientPlayNetworkHandler, Packet<?> packet) {
		if (FakeAccurateBlockPlacement.requestedTicks <= -3 || FakeAccurateBlockPlacement.fakeDirection == null) {
			clientPlayNetworkHandler.sendPacket(packet);
			return;
		}
		clientPlayNetworkHandler.sendPacket(new PlayerMoveC2SPacket.Full(
			this.getX(), this.getY(), this.getZ(),
			FakeAccurateBlockPlacement.fakeYaw,
			FakeAccurateBlockPlacement.fakePitch,
			this.isOnGround()
		));
	}

	@Redirect(method = "sendMovementPackets", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;sendPacket(Lnet/minecraft/network/Packet;)V", ordinal = 3), require = 0)
	private void onSendPacketPositionAndOnGround(ClientPlayNetworkHandler clientPlayNetworkHandler, Packet<?> packet) {
		if (FakeAccurateBlockPlacement.requestedTicks <= -3 || FakeAccurateBlockPlacement.fakeDirection == null) {
			clientPlayNetworkHandler.sendPacket(packet);
			return;
		}
		clientPlayNetworkHandler.sendPacket(
			new PlayerMoveC2SPacket.LookAndOnGround(
				FakeAccurateBlockPlacement.fakeYaw,
				FakeAccurateBlockPlacement.fakePitch,
				this.isOnGround()
			)
		);
	}

	@Redirect(method = "sendMovementPackets", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;sendPacket(Lnet/minecraft/network/Packet;)V", ordinal = 4), require = 0)
	private void onSendPacketLookAndOnGround(ClientPlayNetworkHandler clientPlayNetworkHandler, Packet<?> packet) {
		if (FakeAccurateBlockPlacement.requestedTicks <= -3 || FakeAccurateBlockPlacement.fakeDirection == null) {
			clientPlayNetworkHandler.sendPacket(packet);
			return;
		}
		clientPlayNetworkHandler.sendPacket(
			new PlayerMoveC2SPacket.LookAndOnGround(
				FakeAccurateBlockPlacement.fakeYaw,
				FakeAccurateBlockPlacement.fakePitch,
				this.isOnGround()
			)
		);
	}
	@Redirect(method = "sendMovementPackets", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;sendPacket(Lnet/minecraft/network/Packet;)V", ordinal = 5), require = 0)
	private void onSendPacketOnGroundOnly(ClientPlayNetworkHandler clientPlayNetworkHandler, Packet<?> packet) {
		if (FakeAccurateBlockPlacement.requestedTicks <= -3 || FakeAccurateBlockPlacement.fakeDirection == null) {
			clientPlayNetworkHandler.sendPacket(packet);
			return;
		}
		clientPlayNetworkHandler.sendPacket(
			new PlayerMoveC2SPacket.LookAndOnGround(
				FakeAccurateBlockPlacement.fakeYaw,
				FakeAccurateBlockPlacement.fakePitch,
				this.isOnGround()
			)
		);
	}
}