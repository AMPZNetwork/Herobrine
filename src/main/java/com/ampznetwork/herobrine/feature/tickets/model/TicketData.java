package com.ampznetwork.herobrine.feature.tickets.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.java.Log;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.comroid.api.func.WrappedFormattable;
import org.comroid.api.io.FileHandle;
import org.jspecify.annotations.Nullable;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Objects;
import java.util.logging.Level;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static com.ampznetwork.herobrine.util.ApplicationContextProvider.*;

@Log
@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@IdClass(TicketData.Key.class)
public class TicketData implements WrappedFormattable {
    public static final FileHandle TICKET_ARCHIVE_DIR = new FileHandle("./tickets", true);

    public static Stream<Role> mentionables(@Nullable TicketConfiguration config, @Nullable TicketTopic topic) {
        return LongStream.concat(Stream.ofNullable(config).filter(Objects::nonNull).mapToLong(TicketConfiguration::getTeamRoleId),
                        Stream.ofNullable(topic).filter(Objects::nonNull).mapToLong(TicketTopic::getHandlerRoleId))
                .distinct()
                .filter(id -> id > 0)
                .mapToObj(bean(JDA.class)::getRoleById)
                .filter(Objects::nonNull);
    }

    @Id                  long        guildId;
    @Id                  long        ticketId;
    @ManyToOne @Nullable TicketTopic topic;
    String title;
    @Column(length = 1800) String description;
    long authorId, threadId;
    TicketState state;

    @Override
    public String getPrimaryName() {
        return "Ticket #%d - `%s`".formatted(ticketId, state);
    }

    @Override
    public String getAlternateName() {
        return "Ticket #%d - `%s` - by <@%d>".formatted(ticketId, state, authorId);
    }

    public MessageCreateBuilder toInfoMessage(TicketConfiguration config) {
        return state.toInfoMessage(config, this);
    }

    @SneakyThrows
    public void archive() {
        var file = TICKET_ARCHIVE_DIR.createSubDir(String.valueOf(guildId)).createSubFile("%d_%d.md".formatted(ticketId, authorId));
        if (!file.getParentFile().mkdirs()) {
            log.log(Level.WARNING, "Could not create archival directory");
            return;
        }

        try (var fos = new FileOutputStream(file); var pw = new PrintStream(fos)) {
            pw.println("# Title: " + title);
            pw.println("## Topic: " + topic);
            pw.println("## Description: " + description);
            pw.println("### Author ID: " + authorId);
            pw.println("-# Thread ID: " + threadId);

            pw.println();
            pw.println("-- Message Backlog --");

            var thread = bean(JDA.class).getThreadChannelById(threadId);
            if (thread == null) {
                pw.println("**Could not fetch message history**");
                return;
            }

            thread.getIterableHistory().reverse().stream().map(msg -> "__%s:__ %s".formatted(msg.getAuthor(), msg.getContentRaw())).forEach(pw::println);
        }
    }

    public record Key(long guildId, long ticketId) {}
}
