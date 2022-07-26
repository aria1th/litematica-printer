package io.github.eatmyvenom.litematicin.utils;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;

import java.util.HashSet;

import static io.github.eatmyvenom.litematicin.LitematicaMixinMod.DEBUG_EXTRA_MESSAGE;
import static io.github.eatmyvenom.litematicin.LitematicaMixinMod.DEBUG_MESSAGE;

class MessageHolder{
	private static final HashSet<String> uniqueStrings = new HashSet<>();
	public static void sendDebugMessage(ClientPlayerEntity player, String string){
		if(DEBUG_EXTRA_MESSAGE.getBooleanValue()){
			player.sendMessage(Text.of(string));
		}
	}
	public static void sendMessageUncheckedUnique(ClientPlayerEntity player, String string){
		if (!uniqueStrings.contains(string)) {
			player.sendMessage(Text.of(string));
			uniqueStrings.add(string);
		}
	}
	public static void sendUniqueMessage(ClientPlayerEntity player, String string){
		if (!DEBUG_MESSAGE.getBooleanValue()){
			uniqueStrings.clear();
			return;
		}
		if (!uniqueStrings.contains(string)) {
			player.sendMessage(Text.of(string));
			uniqueStrings.add(string);
		}
	}
	public static void sendUniqueMessage(ClientPlayerEntity player, Object object){
		String string = object.toString();
		sendUniqueMessage(player, string);
	}
	public static void sendUniqueMessageActionBar(ClientPlayerEntity player, String string){
		if (!DEBUG_MESSAGE.getBooleanValue()){
			return;
		}
		if (!uniqueStrings.contains(string)) {
			player.sendMessage(Text.of(string), true);
			uniqueStrings.add(string);
		}
	}
}