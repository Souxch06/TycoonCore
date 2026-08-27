package com.awaitquality.api.bungee.chat;

import java.util.Objects;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class ChatUtil {
    @Deprecated
    public static BaseComponent[] format(String string) {
        return TextComponent.fromLegacyText((String)ChatColor.translateAlternateColorCodes((char)'&', (String)string.replace("\\u", "\n")));
    }

    public static void sendMessage(CommandSender commandSender, String string) {
        if (commandSender == null) {
            return;
        }
        if (commandSender instanceof ProxiedPlayer) {
            ProxiedPlayer proxiedPlayer = (ProxiedPlayer)commandSender;
            if (proxiedPlayer.isConnected()) {
                proxiedPlayer.sendMessage(ChatUtil.format(string));
            }
        } else {
            commandSender.sendMessage(ChatUtil.format(string));
        }
    }

    public static void sendMessage(ProxiedPlayer proxiedPlayer, String string, boolean bl) {
        if (bl) {
            if (proxiedPlayer != null) {
                ChatUtil.sendMessage((CommandSender)proxiedPlayer, string);
            }
        } else {
            ChatUtil.sendMessage((CommandSender)Objects.requireNonNull(proxiedPlayer), string);
        }
    }

    public static void sendMessage(ProxiedPlayer proxiedPlayer, boolean bl, String ... stringArray) {
        if (bl) {
            if (proxiedPlayer != null) {
                for (String string : stringArray) {
                    ChatUtil.sendMessage((CommandSender)proxiedPlayer, string);
                }
            }
        } else {
            ChatUtil.sendMessage((CommandSender)Objects.requireNonNull(proxiedPlayer), stringArray);
        }
    }

    public static void sendMessagef(ProxiedPlayer proxiedPlayer, String string, String ... stringArray) {
        ChatUtil.sendMessage((CommandSender)proxiedPlayer, String.format(string, stringArray));
    }

    public static void sendMessage(CommandSender commandSender, String ... stringArray) {
        for (String string : stringArray) {
            ChatUtil.sendMessage(commandSender, string);
        }
    }
}

