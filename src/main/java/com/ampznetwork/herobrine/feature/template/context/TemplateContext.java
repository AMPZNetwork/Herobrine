package com.ampznetwork.herobrine.feature.template.context;

import com.ampznetwork.herobrine.feature.template.model.decl.CodeBlock;
import com.ampznetwork.herobrine.feature.template.model.expr.Expression;
import lombok.Setter;
import lombok.Value;
import lombok.experimental.NonFinal;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.comroid.api.map.StringKeyMap;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Value
public class TemplateContext {
    CodeBlock                 body;
    Map<CharSequence, Object> variables;
    Map<CharSequence, Object> constants;
    @NonFinal @Setter           Expression[] arguments   = new Expression[0];
    @NonFinal @Setter @Nullable Object       returnValue = null;

    public TemplateContext(CodeBlock body, Map<CharSequence, Object> constants) {
        this(body, constants, Map.of());
    }

    private TemplateContext(CodeBlock body, Map<CharSequence, Object> constants, Map<CharSequence, Object> variables) {
        this.body      = body;
        this.variables = new StringKeyMap<>(variables);
        this.constants = Collections.unmodifiableMap(new StringKeyMap<>(constants));
    }

    public void innerContext(Consumer<TemplateContext> action) {
        var inner = this;//new TemplateContext(null, constants, variables);
        action.accept(inner);
    }

    public MessageCreateBuilder evaluate() {
        body.execute(this);

        return buildMessageData();
    }

    private MessageCreateBuilder buildMessageData() {
        var message = new MessageCreateBuilder();

        var content = variables.getOrDefault("response.content", null);
        if (content != null) message.setContent(String.valueOf(content));

        var embedData = variables.entrySet()
                .stream()
                .filter(e -> e.getKey().toString().startsWith("response.embed"))
                .toList();

        if (!embedData.isEmpty()) {
            var embed = new EmbedBuilder();

            for (var entry : embedData) {
                var key = entry.getKey().toString().substring("response.embed".length() + 1);

                EmbedComponentReference.valueOf(key).accept(embed, entry.getValue());
            }

            message.addEmbeds(embed.build());
        }

        return message;
    }

    public Optional<Object> findVariable(CharSequence key) {
        return Stream.concat(constants.entrySet().stream(), variables.entrySet().stream())
                .filter(entry -> entry.getKey().equals(key))
                .map(Map.Entry::getValue)
                .findAny();
    }
}
