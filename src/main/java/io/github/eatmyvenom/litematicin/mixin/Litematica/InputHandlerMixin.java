package io.github.eatmyvenom.litematicin.mixin.Litematica;

import fi.dy.masa.litematica.event.InputHandler;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import io.github.eatmyvenom.litematicin.LitematicaMixinMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(value= InputHandler.class, remap = false)
public class InputHandlerMixin {
	@Redirect(method="addHotkeys", at = @At(value = "FIELD", target = "Lfi/dy/masa/litematica/config/Hotkeys;HOTKEY_LIST:Ljava/util/List;"))
	private List<ConfigHotkey> getHotkeyList() {
		return LitematicaMixinMod.getHotkeyList();
	}

	@Redirect(method = "addKeysToMap", at = @At(value = "FIELD", target = "Lfi/dy/masa/litematica/config/Hotkeys;HOTKEY_LIST:Ljava/util/List;"))
	private List<ConfigHotkey> getHotkeyList2() {
		return LitematicaMixinMod.getHotkeyList();
	}
}
