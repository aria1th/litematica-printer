package io.github.eatmyvenom.litematicin.mixin.Litematica;

import com.google.common.collect.ImmutableList;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import io.github.eatmyvenom.litematicin.LitematicaMixinMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(value = Configs.class, remap = false)
public class ConfigsMixin {
    @Redirect(method = "loadFromFile", at = @At(value = "FIELD", target = "Lfi/dy/masa/litematica/config/Configs$Generic;OPTIONS:Lcom/google/common/collect/ImmutableList;"))
    private static ImmutableList<IConfigBase> moreOptions() {
        return LitematicaMixinMod.betterList;
    }

    @Redirect(method = "loadFromFile", at = @At(value = "FIELD", target = "Lfi/dy/masa/litematica/config/Hotkeys;HOTKEY_LIST:Ljava/util/List;"))
    private static List<ConfigHotkey> moreHotkeyOptions() {
        return LitematicaMixinMod.hotkeyList;
    }

    @Redirect(method = "saveToFile", at = @At(value = "FIELD", target = "Lfi/dy/masa/litematica/config/Configs$Generic;OPTIONS:Lcom/google/common/collect/ImmutableList;"))
    private static ImmutableList<IConfigBase> moreeOptions() {
        return LitematicaMixinMod.betterList;
    }

    @Redirect(method = "saveToFile", at = @At(value = "FIELD", target = "Lfi/dy/masa/litematica/config/Hotkeys;HOTKEY_LIST:Ljava/util/List;"))
    private static List<ConfigHotkey> moreeHotkeyOptions() {
        return LitematicaMixinMod.hotkeyList;
    }
}
