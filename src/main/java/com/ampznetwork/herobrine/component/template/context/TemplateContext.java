package com.ampznetwork.herobrine.component.template.context;

import com.ampznetwork.herobrine.component.template.model.decl.CodeBlock;
import com.ampznetwork.herobrine.component.template.model.expr.Expression;
import lombok.Setter;
import lombok.Value;
import lombok.experimental.NonFinal;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.comroid.api.map.StringKeyMap;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;

@Value
public class TemplateContext {
    CodeBlock                 body;
    Map<CharSequence, Object> variables;
    Map<CharSequence, Object> constants;
    @NonFinal @Setter           Expression[] arguments   = new Expression[0];
    @NonFinal @Setter @Nullable Object       returnValue = null;

    public TemplateContext(CodeBlock body, Map<CharSequence, Object> constants) {
        this(body, Map.of(), constants);
    }

    private TemplateContext(CodeBlock body, Map<CharSequence, Object> variables, Map<CharSequence, Object> constants) {
        this.body      = body;
        this.variables = new StringKeyMap<>(variables);
        this.constants = Collections.unmodifiableMap(new StringKeyMap<>(constants));
    }

    @SuppressWarnings("RedundantUnmodifiable")
    public void innerContext(Consumer<TemplateContext> action) {
        var inner = new TemplateContext(null, Collections.unmodifiableMap(constants), Map.copyOf(variables));
        action.accept(inner);
    }

    public MessageCreateBuilder evaluate() {
        body.execute(this);

        return buildMessageData();
    }

    private MessageCreateBuilder buildMessageData() {
        var message = new MessageCreateBuilder();

        var content = variables.getOrDefault("message.content", null);
        if (content != null) message.setContent(String.valueOf(content));

        var embedData = variables.entrySet()
                .stream()
                .filter(e -> e.getKey().toString().startsWith("message.embed"))
                .toList();

        if (!embedData.isEmpty()) {
            var embed = new EmbedBuilder();

            for (var entry : embedData) {
                var key = entry.getKey().toString().substring("message.embed".length() + 1);

                EmbedComponentReference.valueOf(key).accept(embed, entry.getValue());
            }

            message.addEmbeds(embed.build());
        }

        return message;
    }
}
