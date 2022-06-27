package io.github.eatmyvenom.litematicin.mixin.Litematica;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import fi.dy.masa.litematica.util.WorldUtils;
import io.github.eatmyvenom.litematicin.utils.Printer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.ActionResult;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = WorldUtils.class, remap = false, priority = 1010)
public class WorldUtilsMixin {
    /**
     * @author joe mama // fixed by AngelBottomless
     */
    //@Overwrite
    @Overwrite()
    private static ActionResult doEasyPlaceAction(MinecraftClient mc)
    {
		ActionResult defaultResult =  ActionResult.SUCCESS;
		try {
			defaultResult =  Printer.doPrinterAction(mc);
		}
		catch (NullPointerException e){
			//in case of NPE, print log instead
			e.printStackTrace();
		}
		return defaultResult;
    }
}
