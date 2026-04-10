package com.ampznetwork.herobrine.component.user.tags.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Member;

import java.util.stream.Stream;

@Entity
@IdClass(UserTagProvider.Key.class)
public class UserTagProvider {
    @Id long    guildId;
    @Id UserTag tag;
    @Id Method  method;
    @Id long    meta;

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

    public record Key(long guildId, UserTag tag, Method method, long meta) {}
}
