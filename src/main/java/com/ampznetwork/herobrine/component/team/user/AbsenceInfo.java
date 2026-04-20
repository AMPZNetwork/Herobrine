package com.ampznetwork.herobrine.component.team.user;

import com.ampznetwork.herobrine.repo.TeamMemberInfoRepository;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import org.comroid.annotations.Instance;
import org.comroid.api.attr.Named;
import org.comroid.api.text.Markdown;
import org.comroid.interaction.annotation.Completion;
import org.comroid.interaction.model.InteractionContext;
import org.comroid.interaction.node.ParameterNode;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.ampznetwork.herobrine.util.ApplicationContextProvider.*;
import static java.time.LocalDateTime.*;

/**
 * todo: the entire repetition system is WIP and subject to further optimization
 * the maths aint mathing for me right now
 * god save us
 */
@Entity
@NoArgsConstructor
@AllArgsConstructor
@IdClass(AbsenceInfo.Key.class)
public class AbsenceInfo {
    @Id long   guildId;
    @Id long   userId;
    @Id String reason;
    TimeFrame timeFrame;

    public boolean isAcute() {
        return timeFrame.repetition() == null ? !timeFrame.isPassed() : timeFrame.repeat().anyMatch(TimeFrame::isAcute);
    }

    @Override
    public String toString() {
        return timeFrame.toString() + " (%s)".formatted(reason);
    }

    public EmbedBuilder toEmbed() {
        var embed     = new EmbedBuilder().setDescription(reason);
        var formatter = timeFrame.repetition == null ? TimeFrame.RECENT : timeFrame.repetition.formatter;

        return embed.addField("Start time", formatter.format(timeFrame.startDateTime), false)
                .addField("End time", timeFrame.endDateTime == null ? Markdown.Italic.apply("indefinitely") : formatter.format(timeFrame.endDateTime), false)
                .addField("Repeats", timeFrame.repetition == null ? "no" : timeFrame.repetition.getName(), false);
    }

    @RequiredArgsConstructor
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    public enum Repetition implements Named {
        Daily(ChronoUnit.DAYS, DateTimeFormatter.ofPattern("HH:mm")),
        Weekly(ChronoUnit.WEEKS, DateTimeFormatter.ofPattern("E HH")),
        Monthly(ChronoUnit.MONTHS, DateTimeFormatter.ofPattern("dd. HH")),
        Yearly(ChronoUnit.YEARS, DateTimeFormatter.ofPattern("dd.MM."));

        ChronoUnit        unit;
        DateTimeFormatter formatter;

        public Stream<TimeFrame> repeat(TimeFrame timeFrame) {
            return repeat(timeFrame, 99_999);
        }

        /// god help us, hope this wont give massive overhead later
        public Stream<TimeFrame> repeat(TimeFrame timeFrame, int futureRepetitions) {
            return IntStream.range(0, remainingRepetitions(timeFrame.repeatUntil))
                    .mapToObj(offset -> accumulate(timeFrame, offset))
                    .filter(Predicate.not(TimeFrame::isPassed))
                    .limit(futureRepetitions);
        }

        /// todo: test this
        private int remainingRepetitions(@Nullable LocalDateTime until) {
            return until == null ? Integer.MAX_VALUE : Math.toIntExact(unit.between(now(), until));
        }

        private TimeFrame accumulate(TimeFrame original, int offset) {
            return new TimeFrame(original.startDateTime.plus(offset, unit),
                    original.endDateTime == null ? null : original.endDateTime.plus(offset, unit),
                    this,
                    original.repeatUntil,
                    original.entireDay);
        }
    }

    public enum ContextualReasonProvider implements Completion.Provider.OfStrings {
        @Instance INSTANCE;

        @Override
        public Stream<String> findCompletionValues(InteractionContext context, ParameterNode parameter, String currentValue) {
            return context.components(Member.class)
                    .map(TeamMemberInfo.Key::new)
                    .flatMap(key -> bean(TeamMemberInfoRepository.class).findById(key).stream())
                    .flatMap(info -> info.absences.stream())
                    .map(info -> info.reason)
                    .distinct();
        }
    }

    @Embeddable
    public record TimeFrame(
            LocalDateTime startDateTime,
            @Nullable LocalDateTime endDateTime,
            @Nullable Repetition repetition,
            @Nullable LocalDateTime repeatUntil,
            boolean entireDay
    ) {
        public static final DateTimeFormatter RECENT = DateTimeFormatter.ofPattern("HH:mm");

        public boolean isPassed() {
            return entireDay
                   ? (endDateTime == null ? startDateTime : endDateTime).withHour(23).withMinute(59).isBefore(now())
                   : endDateTime != null && endDateTime.isBefore(now());
        }

        public boolean isAcute() {
            return (entireDay ? startDateTime.withHour(23).withMinute(59) : startDateTime).isAfter(now()) && !isPassed();
        }

        public boolean isExpired() {
            return repetition == null ? isPassed() : repeatUntil != null && repeat().allMatch(TimeFrame::isPassed);
        }

        public Stream<TimeFrame> repeat() {
            return repetition == null ? Stream.of(this) : repetition.repeat(this);
        }

        @Override
        public @NonNull String toString() {
            var formatter = repetition == null ? RECENT : repetition.formatter;

            return "%sfrom %s %s".formatted(repetition == null ? "" : repetition.name() + ' ',
                    formatter.format(startDateTime),
                    endDateTime == null ? "indefinitely" : "until " + formatter.format(endDateTime));
        }
    }

    public record Key(long guildId, long userId, String reason) {
        public Key(Member member, String reason) {
            this(member.getGuild().getIdLong(), member.getIdLong(), reason);
        }
    }
}
