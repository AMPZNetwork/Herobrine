package com.ampznetwork.herobrine.component.user.tags.model;

import com.ampznetwork.herobrine.repo.UserTagProviderRepository;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Member;
import org.comroid.annotations.Instance;
import org.comroid.interaction.annotation.Completion;
import org.comroid.interaction.model.InteractionContext;
import org.comroid.interaction.node.ParameterNode;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.stream.Stream;

import static com.ampznetwork.herobrine.util.ApplicationContextProvider.*;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@IdClass(UserTagProvider.Key.class)
@Table(uniqueConstraints = { @UniqueConstraint(columnNames = { "guild_id", "tag" }) })
public class UserTagProvider {
    @Id long    guildId;
    @Id UserTag tag;
    @Id Method  method;
    @Id long    meta;

    public UserTagProvider(Key key) {
        this(key.guildId, key.tag, key.method, key.meta);
    }

    public Stream<UserTag> find(Member member) {
        var guild = member.getGuild();

        if (guild.getIdLong() != guildId) throw new IllegalArgumentException("Member %s is not in guild with ID %d".formatted(member, guildId));

        return Stream.of(member).filter(m -> method.test(m, meta)).map($ -> tag);
    }

    /// a method of obtaining meta keys from the guild - todo: more providers (linked accounts, ..)
    public enum Method {
        ROLE {
            @Override
            protected boolean test(Member member, long meta) {
                return member.getRoles().stream().mapToLong(ISnowflake::getIdLong).anyMatch(x -> x == meta);
            }
        };

        protected abstract boolean test(Member member, long meta);
    }

    public enum ContextualUserTagProvider implements Completion.Provider {
        @Instance INSTANCE;

        @Override
        public Stream<Completion.Option> findCompletionOptions(InteractionContext context, ParameterNode parameter, String currentValue) {
            var guild = context.child(Guild.class).orElseThrow();

            var paramMethod    = getParameter(context, Method.class).orElseThrow();
            var selectedMethod = (@Nullable Method) context.getParameter(paramMethod);

            var paramMentionable    = getParameter(context, IMentionable.class).orElseThrow();
            var selectedMentionable = (@Nullable IMentionable) context.getParameter(paramMentionable);

            return bean(UserTagProviderRepository.class).findContextualUserTags(guild.getIdLong(), selectedMethod, selectedMentionable.getIdLong())
                    .stream()
                    .map(Enum::name)
                    .map(Completion.Option::new);
        }
    }

    public enum ContextualUserTagMethodProvider implements Completion.Provider {
        @Instance INSTANCE;

        @Override
        public Stream<Completion.Option> findCompletionOptions(InteractionContext context, ParameterNode parameter, String currentValue) {
            var guild = context.child(Guild.class).orElseThrow();

            var paramTag    = getParameter(context, UserTag.class).orElseThrow();
            var selectedTag = (@Nullable UserTag) context.getParameter(paramTag);

            var paramMentionable    = getParameter(context, IMentionable.class).orElseThrow();
            var selectedMentionable = (@Nullable IMentionable) context.getParameter(paramMentionable);

            return bean(UserTagProviderRepository.class).findContextualUserTagMethods(guild.getIdLong(), selectedTag, selectedMentionable.getIdLong())
                    .stream()
                    .map(Enum::name)
                    .map(Completion.Option::new);
        }
    }

    private static Optional<ParameterNode> getParameter(InteractionContext context, Class<?> type) {
        return context.getNode().getParameters().stream().filter(node -> node.getReflect().getType().equals(type)).findAny();
    }

    public record Key(long guildId, UserTag tag, Method method, long meta) {}
}
