package com.ampznetwork.herobrine.trigger;

import jakarta.persistence.AttributeConverter;
import lombok.Value;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.comroid.annotations.Instance;
import org.comroid.api.Polyfill;
import org.comroid.api.func.util.Event;
import org.comroid.commands.autofill.IAutoFillProvider;
import org.comroid.commands.impl.CommandUsage;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Stream;

@Value
public class DiscordTrigger<T extends GenericEvent> implements Function<Event.Bus<? super GenericEvent>, Event.Bus<T>> {
    private static final Map<String, DiscordTrigger<?>>       values     = new ConcurrentHashMap<>();
    public static final  Map<String, DiscordTrigger<?>>       VALUES     = Collections.unmodifiableMap(values);
    public static final  DiscordTrigger<GuildMemberJoinEvent> ON_JOIN    = new DiscordTrigger<>(GuildMemberJoinEvent.class);
    public static final  DiscordTrigger<MessageReceivedEvent> ON_MESSAGE = new DiscordTrigger<>(MessageReceivedEvent.class);

    public static <T extends GenericEvent> DiscordTrigger<T> valueOf(String key) {
        return VALUES.entrySet()
                .stream()
                .filter(e -> e.getKey().equals(key))
                .findAny()
                .map(Map.Entry::getValue)
                .map(Polyfill::<DiscordTrigger<T>>uncheckedCast)
                .orElse(null);
    }

    Class<T> eventType;

    public DiscordTrigger(Class<T> eventType) {
        this.eventType = eventType;

        values.put(eventType.getSimpleName(), this);
    }

    @Override
    public Event.Bus<T> apply(Event.Bus<? super GenericEvent> bus) {
        return bus.flatMap(eventType);
    }

    public enum AutoFillNames implements IAutoFillProvider {
        @Instance INSTANCE;

        @Override
        public Stream<? extends CharSequence> autoFill(CommandUsage usage, String argName, String currentValue) {
            return VALUES.keySet().stream();
        }
    }

    @jakarta.persistence.Converter(autoApply = true)
    public static final class Converter implements AttributeConverter<DiscordTrigger<?>, String> {
        @Override
        public String convertToDatabaseColumn(DiscordTrigger<?> attribute) {
            return attribute.eventType.getSimpleName();
        }

        @Override
        public DiscordTrigger<?> convertToEntityAttribute(String dtype) {
            return VALUES.get(dtype);
        }
    }
}
