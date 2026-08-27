/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.md_5.bungee.api.ChatColor
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.command.CommandSender
 */
package com.awaitquality.api.spigot.chat;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class ChatUtil {
    public static String translate(String string) {
        string = ChatColor.translateAlternateColorCodes((char)'&', (String)string);
        Pattern pattern = Pattern.compile("&#([A-Fa-f0-9]{6})");
        Matcher matcher = pattern.matcher(string);
        StringBuilder stringBuilder = new StringBuilder();
        while (matcher.find()) {
            String string2 = matcher.group(1);
            matcher.appendReplacement(stringBuilder, net.md_5.bungee.api.ChatColor.of((String)("#" + string2)).toString());
        }
        return matcher.appendTail(stringBuilder).toString();
    }

    public static List<String> translate(List<String> list) {
        return list.stream().map(string -> ChatColor.translateAlternateColorCodes((char)'&', (String)string)).collect(Collectors.toList());
    }

    public static void sendMessage(CommandSender commandSender, String string) {
        commandSender.sendMessage(ChatUtil.translate(string));
    }

    public static void sendBroadcast(String string) {
        Bukkit.getServer().broadcastMessage(ChatUtil.translate(string));
    }
}

