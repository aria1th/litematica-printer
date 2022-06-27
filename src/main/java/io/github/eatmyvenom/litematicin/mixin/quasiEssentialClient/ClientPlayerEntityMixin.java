package io.github.eatmyvenom.litematicin.mixin.quasiEssentialClient;

import com.mojang.authlib.GameProfile;
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
import io.github.eatmyvenom.litematicin.utils.FakeAccurateBlockPlacement;

//see https://github.com/senseiwells/EssentialClient/blob/1.19.x/src/main/java/me/senseiwells/essentialclient/mixins/betterAccurateBlockPlacement/ClientPlayerEntityMixin.java for reference!!

@Mixin(value = ClientPlayerEntity.class, priority = 1010)
public abstract class ClientPlayerEntityMixin extends PlayerEntity {
	public ClientPlayerEntityMixin(World world, BlockPos pos, float yaw, GameProfile gameProfile, @Nullable PlayerPublicKey publicKey) {
		super(world, pos, yaw, gameProfile, publicKey);
	}

	@Redirect(method = "sendMovementPackets", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;sendPacket(Lnet/minecraft/network/Packet;)V", ordinal = 3), require = 0)
	private void onSendPacketAll(ClientPlayNetworkHandler clientPlayNetworkHandler, Packet<?> packet) {
		if (!FakeAccurateBlockPlacement.isHandling()) {
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

	@Redirect(method = "sendMovementPackets", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;sendPacket(Lnet/minecraft/network/Packet;)V", ordinal = 5), require = 0)
	private void onSendPacketLook(ClientPlayNetworkHandler clientPlayNetworkHandler, Packet<?> packet) {
		if (!FakeAccurateBlockPlacement.isHandling()) {
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