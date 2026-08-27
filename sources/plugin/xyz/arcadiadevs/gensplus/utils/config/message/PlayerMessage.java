/*
 * Décompilé avec CFR 0.152.
 * 
 * Impossible de charger les classes suivantes :
 *  net.md_5.bungee.api.ChatMessageType
 *  net.md_5.bungee.chat.ComponentSerializer
 *  org.bukkit.Bukkit
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 */
package xyz.arcadiadevs.gensplus.utils.config.message;

import com.awaitquality.api.spigot.chat.ChatUtil;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xyz.arcadiadevs.gensplus.GensPlus;
import xyz.arcadiadevs.gensplus.utils.ActionBarUtil;
import xyz.arcadiadevs.gensplus.utils.ServerVersion;
import xyz.arcadiadevs.gensplus.utils.config.message.Messages;

public class PlayerMessage {
    private final Messages message;
    private final List<String> format;

    public PlayerMessage(Messages messages) {
        this.message = messages;
        this.format = this.message.getCached();
    }

    public PlayerMessage format(Object ... objectArray) {
        this.format.replaceAll(string -> this.apply((String)string, objectArray));
        return this;
    }

    private String apply(String string, Object ... objectArray) {
        int n = 0;
        while (n < objectArray.length) {
            string = string.replace("%" + String.valueOf(objectArray[n]) + "%", objectArray[n + 1].toString());
            n += 2;
        }
        return ChatUtil.translate(string);
    }

    public String getAsString() {
        StringBuilder stringBuilder = new StringBuilder();
        boolean bl = false;
        for (String string : this.format) {
            if (bl) {
                stringBuilder.append("\n");
            }
            stringBuilder.append(string);
            bl = true;
        }
        return stringBuilder.toString();
    }

    public void send(Collection<? extends Player> collection) {
        collection.forEach(this::send);
    }

    public void send(CommandSender commandSender) {
        if (this.format.isEmpty()) {
            return;
        }
        this.format.forEach(arg_0 -> ((CommandSender)commandSender).sendMessage(arg_0));
    }

    public void send(boolean bl) {
        if (this.format.isEmpty() && !bl) {
            return;
        }
        this.format.forEach(Bukkit::broadcastMessage);
    }

    public void sendInActionBar(Player player) {
        ActionBarUtil.sendActionBar(player, this.format.get(0));
    }

    public void sendAsJson(Player player) {
        for (String string : this.format) {
            try {
                if (ServerVersion.isServerVersionAtLeast(ServerVersion.V1_9)) {
                    player.spigot().sendMessage(ChatMessageType.CHAT, ComponentSerializer.parse((String)string));
                    continue;
                }
                this.send((CommandSender)player);
            }
            catch (RuntimeException runtimeException) {
                GensPlus.getInstance().getLogger().log(Level.WARNING, "Impossible d'analyser le message brut envoyé au joueur. Vérifiez que la syntaxe est correcte");
                GensPlus.getInstance().getLogger().log(Level.WARNING, "Message : " + string);
                runtimeException.printStackTrace();
            }
        }
    }
}

