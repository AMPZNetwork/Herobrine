package com.ampznetwork.herobrine.feature.template.context;

import com.ampznetwork.herobrine.feature.template.model.decl.func.Function;
import com.ampznetwork.herobrine.feature.template.model.expr.Expression;
import com.ampznetwork.herobrine.feature.template.types.TemplateObjectInstance;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Singular;
import lombok.Value;
import org.comroid.api.data.bind.DataStructure;
import org.comroid.api.func.util.Optionals;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Value
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Reference implements Expression, CharSequence {
    @Singular          List<Part>       keys;
    @Default @Nullable List<Expression> arguments = null;

    @Override
    public String toSerializedString() {
        if (keys.isEmpty()) return "";
        var buf = new StringBuilder(keys.getFirst().key);
        for (var i = 1; i < keys.size(); i++) {
            var part = keys.get(i);
            buf.append(part.nullable ? "?" : "").append('.').append(part.key);
        }
        if (arguments != null) {
            buf.append('(');
            for (var iterator = arguments.iterator(); iterator.hasNext(); ) {
                var argument = iterator.next();
                buf.append(argument.toSerializedString());
                if (iterator.hasNext()) buf.append(", ");
            }
            buf.append(")");
        }
        return buf.toString();
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof CharSequence other && hashCode() == other.hashCode();
    }

    @Override
    public @NonNull String toString() {
        return keys.stream().map(Part::key).collect(Collectors.joining("."));
    }

    @Override
    public int length() {
        return toString().length();
    }

    @Override
    public char charAt(int index) {
        return toString().charAt(index);
    }

    @Override
    public @NonNull CharSequence subSequence(int start, int end) {
        return toString().subSequence(start, end);
    }

    @Override
    public Object evaluate(TemplateContext context) {
        final var key = toString();

        if (arguments == null) {
            // call property
            return context.findVariable(key)
                    .or(() -> keys.size() == 1
                              ? Optional.empty()
                              : context.findVariable(keys.getFirst().key).flatMap(it -> {
                                  var target = new Object[]{ it };

                                  for (var i = 1; i < keys.size(); i++) {
                                      var struct = DataStructure.of(target[0].getClass());
                                      var part   = keys.get(i).key;

                                      target[0] = target[0] instanceof TemplateObjectInstance toi
                                                  ? toi.get(part)
                                                  : struct.getProperty(part)
                                                          .map(p -> p.getFrom(target[0]))
                                                          .orElse(null);

                                      if (target[0] == null) break;
                                  }

                                  return Optional.ofNullable(target[0]);
                              }))
                    .orElse(null);
        }

        context.setArguments(arguments.toArray(Expression[]::new));
        context.findVariable(key).flatMap(Optionals.cast(Function.class)).ifPresent(func -> func.execute(context));

        return context.getReturnValue();
    }

    public record Part(String key, boolean nullable) {}
}
