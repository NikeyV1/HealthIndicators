package io.github.adytech99.healthindicators.util;

import io.github.adytech99.healthindicators.enums.MessageTypeEnum;
import io.github.adytech99.healthindicators.config.ModConfig;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class ConfigUtils {
    public static void sendMessage(LocalPlayer player, Component text) {
        if (isSendMessage()) {
            boolean overlay = ModConfig.HANDLER.instance().message_type == MessageTypeEnum.ACTIONBAR;
            player.displayClientMessage(text, overlay);
        }
    }

    public static void sendMessage(ServerPlayer player, Component text) {
        if (isSendMessage()) {
            boolean overlay = ModConfig.HANDLER.instance().message_type == MessageTypeEnum.ACTIONBAR;
            player.displayClientMessage(text, overlay);
        }
    }

    public static void sendOverlayMessage(LocalPlayer player, Component text) {
        if (isSendMessage()) {
            player.displayClientMessage(text, true);
        }
    }

    public static boolean isSendMessage() {
        return ModConfig.HANDLER.instance().message_type != MessageTypeEnum.NONE;
    }

}
