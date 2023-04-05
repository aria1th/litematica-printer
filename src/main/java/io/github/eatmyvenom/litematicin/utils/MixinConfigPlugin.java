package io.github.eatmyvenom.litematicin.utils;

import com.google.common.collect.Lists;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Optional;
import java.util.Set;

//see IMixinConfigPlugin
public class MixinConfigPlugin implements IMixinConfigPlugin {
	@Override
	public void onLoad(String mixinPackage) {
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		List<String> fakeLookMixins = Lists.newArrayList(
			"io.github.eatmyvenom.litematicin.mixin.EssentialClient.FakeLookMixin"
		);
		List<String> desyncOptions = Lists.newArrayList(
			"io.github.eatmyvenom.litematicin.mixin.MinecraftClient.ClientPlayerInteractionManagerMixin",
			"io.github.eatmyvenom.litematicin.mixin.MinecraftClient.ClientPlayNetworkHandlerMixin"
		);

		if (fakeLookMixins.contains(mixinClassName)) {
			Optional<ModContainer> container = FabricLoader.getInstance().getModContainer("essential-client");
			return container.isPresent();
		}
		if (desyncOptions.contains(mixinClassName)) {
			Optional<ModContainer> container = FabricLoader.getInstance().getModContainer("matchrevisions");
			return !container.isPresent(); // Java 8 compatibility
		}
		return true;
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {

	}

	@Override
	public List<String> getMixins() {
		return null;
	}

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

	}

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

	}
}