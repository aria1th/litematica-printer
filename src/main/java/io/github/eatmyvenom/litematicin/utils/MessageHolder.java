package io.github.eatmyvenom.litematicin.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
//#if MC<11900
//$$ import net.minecraft.text.LiteralText;
//#endif
import java.util.HashSet;
import java.util.Objects;

import static io.github.eatmyvenom.litematicin.LitematicaMixinMod.*;

public class MessageHolder {
	private static final HashSet<String> uniqueStrings = new HashSet<>();
	private static final HashSet<String> uniqueStringsAlways = new HashSet<>();
	private static final HashSet<String> uniquePacketInfos = new HashSet<>();
	private static final HashSet<String> errorLogger = new HashSet<>();
	private static String orderPreviousMessage = "";

	public static void sendPacketOrders(String string) {
		if (uniquePacketInfos.contains(string)) {
			return;
		}
		uniquePacketInfos.add(string);
		send(string);
	}

	private static void send(String string) {
		final ClientPlayerEntity player = MinecraftClient.getInstance().player;
		//#if MC>=12102
		//$$ player.sendMessage(Text.of(string), false);
		//#elseif MC>=11900
		player.sendMessage(Text.of(string));
		//#else
		//$$player.sendMessage(new LiteralText(string), false);
		//#endif
	}

	private static void send(String string, boolean actionbar) {
		final ClientPlayerEntity player = MinecraftClient.getInstance().player;
		//#if MC>=11900
		player.sendMessage(Text.of(string), actionbar);
		//#else
		//$$player.sendMessage(new LiteralText(string), actionbar);
		//#endif
	}

	public static void sendDebugMessage(ClientPlayerEntity player, String string) {
		if (DEBUG_EXTRA_MESSAGE.getBooleanValue()) {
			send(string);
		}
	}

	public static void sendDebugMessage(String string) {
		if (DEBUG_EXTRA_MESSAGE.getBooleanValue()) {
			send(string);
		}
	}

	public static void sendOrderMessage(String string) {
		if (DEBUG_ORDER_PLACEMENTS.getBooleanValue() && !Objects.equals(orderPreviousMessage, string)) {
			orderPreviousMessage = string;
			send(string);
		}
	}

	public static void sendUniqueDebugMessage(ClientPlayerEntity player, String string) {
		if (DEBUG_EXTRA_MESSAGE.getBooleanValue()) {
			if (!uniqueStrings.contains(string)) {
				send(string);
				uniqueStrings.add(string);
			}
		}
	}

	public static void sendUniqueDebugMessage(String string) {
		if (DEBUG_EXTRA_MESSAGE.getBooleanValue()) {
			if (!uniqueStrings.contains(string)) {
				send(string);
				uniqueStrings.add(string);
			}
		}
	}

	public static void sendMessageUncheckedUnique(ClientPlayerEntity player, String string) {
		if (!errorLogger.contains(string)) {
			send(string);
			errorLogger.add(string);
		}
	}

	public static void sendMessageUncheckedUnique(String string) {
		if (!errorLogger.contains(string)) {
			send(string);
			errorLogger.add(string);
		}
	}

	public static void sendMessageUnchecked(String string) {
		send(string);
	}

	public static void sendUniqueMessage(ClientPlayerEntity player, String string) {
		if (!DEBUG_MESSAGE.getBooleanValue()) {
			uniqueStrings.clear();
			return;
		}
		if (!uniqueStrings.contains(string)) {
			send(string);
			uniqueStrings.add(string);
		}
	}

	public static void sendUniqueMessageAlways(String string) {
		if (!uniqueStringsAlways.contains(string)) {
			send(string);
			uniqueStringsAlways.add(string);
		}
	}

	public static void sendUniqueMessage(ClientPlayerEntity player, Object object) {
		String string = object.toString();
		sendUniqueMessage(player, string);
	}

	public static void sendUniqueMessageActionBar(ClientPlayerEntity player, String string) {
		if (!DEBUG_MESSAGE.getBooleanValue()) {
			return;
		}
		if (!uniqueStrings.contains(string)) {
			send(string, true);
			uniqueStrings.add(string);
		}
	}
	public static void sendDebugMessageActionBar(ClientPlayerEntity player, String string) {
		if (!DEBUG_MESSAGE.getBooleanValue()) {
			return;
		}
		send(string, true);
		uniqueStrings.add(string);
	}
}