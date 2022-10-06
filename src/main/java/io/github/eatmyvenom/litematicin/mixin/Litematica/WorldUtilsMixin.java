package io.github.eatmyvenom.litematicin.mixin.Litematica;

import fi.dy.masa.litematica.util.WorldUtils;
import io.github.eatmyvenom.litematicin.LitematicaMixinMod;
import io.github.eatmyvenom.litematicin.utils.MessageHolder;
import io.github.eatmyvenom.litematicin.utils.Printer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static io.github.eatmyvenom.litematicin.LitematicaMixinMod.PRINTER_ONLY_FAKE_ROTATION_MODE;

@Mixin(value = WorldUtils.class, remap = false, priority = 1010)
public class WorldUtilsMixin {
	private static boolean hasSent = false;

	/**
	 * @author joe mama // fixed by AngelBottomless
	 */
	//@Overwrite
	@Inject(
		method = "doEasyPlaceAction",
		at = @At("HEAD"),
		cancellable = true
	)
	private static void onDoEasyPlaceAction(MinecraftClient mc, CallbackInfoReturnable<ActionResult> cir) {
		if (mc.player == null) {
			return;
		}
		if (PRINTER_ONLY_FAKE_ROTATION_MODE.getBooleanValue()) {
			cir.setReturnValue(Printer.doEasyPlaceFakeRotation(mc));
		}
		else {
			if (LitematicaMixinMod.PRINTER_OFF.getBooleanValue()) {
				return;
			}
			ActionResult defaultResult = ActionResult.SUCCESS;
			try {
				defaultResult = Printer.doPrinterAction(mc);
			} catch (NullPointerException e) {
				//in case of NPE, print log instead
				MessageHolder.sendMessageUncheckedUnique(mc.player, e.getMessage());
				if (!hasSent && mc.player != null) {
					mc.player.sendMessage(Text.of("Null pointer exception has occured, please upload log at https://github.com/aria1th/litematica-printer/issues"));
					hasSent = true;
				}
			} catch (AssertionError e) {
				MessageHolder.sendOrderMessage("Order error happened " + e.getMessage());
				MessageHolder.sendMessageUncheckedUnique(mc.player, "Order Error Happened " + e.getMessage());
				cir.setReturnValue(ActionResult.FAIL);
				return;
			}
			cir.setReturnValue(defaultResult);
			//return defaultResult;
		}
	}
}
