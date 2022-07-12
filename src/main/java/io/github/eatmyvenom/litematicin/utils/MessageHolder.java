package io.github.eatmyvenom.litematicin.utils;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;

import java.util.HashSet;

import static io.github.eatmyvenom.litematicin.LitematicaMixinMod.DEBUG_MESSAGE;

class MessageHolder{
	private static final HashSet<String> uniqueStrings = new HashSet<>();
	public static void sendUniqueMessage(ClientPlayerEntity player, String string){
		if (!DEBUG_MESSAGE.getBooleanValue()){
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
}