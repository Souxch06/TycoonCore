package fr.valoriatycoon.config;

import java.util.Objects;
import java.util.function.Supplier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;

/** Renders configurable MiniMessage entries without allowing placeholder text to inject markup. */
public final class MessageService {
    private final Supplier<org.bukkit.configuration.file.YamlConfiguration> messagesSupplier;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public MessageService(Supplier<org.bukkit.configuration.file.YamlConfiguration> messagesSupplier) {
        this.messagesSupplier = Objects.requireNonNull(messagesSupplier, "messagesSupplier");
    }

    public void send(CommandSender recipient, String key, TagResolver... placeholders) {
        recipient.sendMessage(component(key, true, placeholders));
    }

    public Component component(String key, boolean withPrefix, TagResolver... placeholders) {
        var messages = messagesSupplier.get();
        String template = messages.getString(key, "<red>Missing message: " + key + "</red>");
        Component body = render(template, placeholders);
        if (!withPrefix) {
            return body;
        }
        String prefix = messages.getString("prefix", "");
        return render(prefix).append(body);
    }

    /** Renders a trusted configuration template with injection-safe dynamic placeholders. */
    public Component render(String template, TagResolver... placeholders) {
        return miniMessage.deserialize(template, TagResolver.resolver(placeholders));
    }
}
