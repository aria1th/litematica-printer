package io.github.eatmyvenom.litematicin.mixin.Litematica;

import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import fi.dy.masa.litematica.util.WorldUtils;
import io.github.eatmyvenom.litematicin.utils.Printer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.ActionResult;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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
    private static void onDoEasyPlaceAction(MinecraftClient mc, CallbackInfoReturnable<ActionResult> cir)
    {
		ActionResult defaultResult =  ActionResult.SUCCESS;
		try {
			defaultResult =  Printer.doPrinterAction(mc);
		}
		catch (NullPointerException e){
			//in case of NPE, print log instead
			e.printStackTrace();
			if (!hasSent && mc.player != null){
				mc.player.sendMessage(Text.of("Null pointer exception has occured, please upload log at https://github.com/aria1th/litematica-printer/issues"));
				hasSent = true;
			}
		}
		cir.setReturnValue(defaultResult);
		//return defaultResult;
    }
}
