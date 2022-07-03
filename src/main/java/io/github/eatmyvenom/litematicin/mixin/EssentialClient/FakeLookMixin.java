package io.github.eatmyvenom.litematicin.mixin.EssentialClient;

import io.github.eatmyvenom.litematicin.utils.FakeAccurateBlockPlacement;
import me.senseiwells.essentialclient.feature.BetterAccurateBlockPlacement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BetterAccurateBlockPlacement.class, remap = false)
public class FakeLookMixin {
	@Shadow
	public static Direction fakeDirection;
	@Shadow
	public static int requestedTicks;
	@Shadow
	public static float fakeYaw;
	@Shadow
	public static float fakePitch;
	@Inject(
		method = "accurateBlockPlacementOnPress",
		at = @At("HEAD")
	)
	private static void onAccurateBlockPlacementOnPress(MinecraftClient client, CallbackInfo ci){
		if (requestedTicks > 0){
			FakeAccurateBlockPlacement.requestedTicks = requestedTicks;
			FakeAccurateBlockPlacement.fakePitch = fakePitch;
			FakeAccurateBlockPlacement.fakeYaw = fakeYaw;
			FakeAccurateBlockPlacement.fakeDirection = fakeDirection;
		}
	}
}