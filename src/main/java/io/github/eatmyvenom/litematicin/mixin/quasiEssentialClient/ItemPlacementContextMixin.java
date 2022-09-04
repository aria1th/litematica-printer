package io.github.eatmyvenom.litematicin.mixin.quasiEssentialClient;

import io.github.eatmyvenom.litematicin.LitematicaMixinMod;
import io.github.eatmyvenom.litematicin.utils.FakeAccurateBlockPlacement;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ItemPlacementContext.class, priority = 1200)
public class ItemPlacementContextMixin {

	@Inject(method = "getPlayerLookDirection", at = @At("HEAD"), cancellable = true, require = 0)
	private void onGetDirection(CallbackInfoReturnable<Direction> cir) {
		if (FakeAccurateBlockPlacement.fakeDirection != null && FakeAccurateBlockPlacement.requestedTicks > -3) {
			cir.setReturnValue(FakeAccurateBlockPlacement.getFacingOrder()[0]);
		}
	}

	@Inject(method = "getVerticalPlayerLookDirection", at = @At("HEAD"), cancellable = true, require = 0)
	private void onGetVerticalDirection(CallbackInfoReturnable<Direction> cir) {
		if (FakeAccurateBlockPlacement.fakeDirection != null && FakeAccurateBlockPlacement.requestedTicks > -3 && FakeAccurateBlockPlacement.fakeDirection.getAxis() == Direction.Axis.Y) {
			cir.setReturnValue(FakeAccurateBlockPlacement.fakeDirection);
		}
	}

	@Redirect(method = "getPlacementDirections", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/Direction;getEntityFacingOrder(Lnet/minecraft/entity/Entity;)[Lnet/minecraft/util/math/Direction;"), require = 0)
	private Direction[] onGetArrayDirections(Entity entity) {
		if (!LitematicaMixinMod.DISABLE_SINGLEPLAYER_HANDLE.getBooleanValue() && FakeAccurateBlockPlacement.fakeDirection != null && FakeAccurateBlockPlacement.requestedTicks > -3) {
			return FakeAccurateBlockPlacement.getFacingOrder();
		}
		return Direction.getEntityFacingOrder(entity);
	}
}