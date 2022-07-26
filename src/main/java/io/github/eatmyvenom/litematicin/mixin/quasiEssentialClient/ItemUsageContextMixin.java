package io.github.eatmyvenom.litematicin.mixin.quasiEssentialClient;

import io.github.eatmyvenom.litematicin.utils.FakeAccurateBlockPlacement;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ItemUsageContext.class, priority = 1010)
public class ItemUsageContextMixin {
	@Inject(method = "getPlayerFacing", at = @At("HEAD"), cancellable = true, require = 0)
	private void onGetFacing(CallbackInfoReturnable<Direction> cir) {
		Direction direction = FakeAccurateBlockPlacement.fakeDirection;
		if (direction != null && FakeAccurateBlockPlacement.requestedTicks > -3 && FakeAccurateBlockPlacement.fakeDirection.getAxis() != Direction.Axis.Y) {
			cir.setReturnValue(direction);
		}
	}
	@Inject(method = "getPlayerYaw", at = @At("HEAD"), cancellable = true, require = 0)
	private void onGetYaw(CallbackInfoReturnable<Float> cir){
		if (FakeAccurateBlockPlacement.requestedTicks > -3){
			cir.setReturnValue(FakeAccurateBlockPlacement.fakeYaw);
		}
	}
}