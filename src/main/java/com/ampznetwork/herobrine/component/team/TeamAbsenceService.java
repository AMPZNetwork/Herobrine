package com.ampznetwork.herobrine.component.team;

import com.ampznetwork.herobrine.component.team.user.AbsenceInfo;
import com.ampznetwork.herobrine.component.team.user.TeamMemberInfo;
import com.ampznetwork.herobrine.repo.AbsenceInfoRepository;
import com.ampznetwork.herobrine.repo.TeamMemberInfoRepository;
import com.ampznetwork.herobrine.util.Constant;
import lombok.extern.java.Log;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import org.comroid.annotations.Description;
import org.comroid.interaction.annotation.Completion;
import org.comroid.interaction.annotation.ContextFilter;
import org.comroid.interaction.annotation.Interaction;
import org.comroid.interaction.annotation.Parameter;
import org.comroid.interaction.model.Response;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Integer.*;

@Log
@Service
@Interaction(value = "team-absence", filter = { @ContextFilter(key = "guild") })
public class TeamAbsenceService {
    private static final Pattern ABSENCE_DATE = Pattern.compile("(?<day>\\d{1,2})([./_ -]+(?<month>\\d{1,2})\\.?)?(\\s+(?<hour>[0-2][0-9]))");
    private static final Pattern ABSENCE_TIME = Pattern.compile("(?<hour>\\d{1,2})([.:/_ -]+(?<minute>[0-5]?[0-9]))?");

    @Autowired TeamMemberService        memberService;
    @Autowired AbsenceInfoRepository    absences;
    @Autowired TeamMemberInfoRepository members;

    @Interaction
    @Description("Create a new absence period")
    public Optional<EmbedBuilder> create(
            Member member, @Parameter @Description("A short description for the absence") String reason,
            @Parameter @Description("`dd.mm.` with optional `hh` or `hh:mm` format") String start,
            @Parameter @Description("`dd.mm.` with optional `hh` or `hh:mm` format") @Nullable String end,
            @Parameter AbsenceInfo.@Nullable Repetition repetition
    ) {
        absences.findById(new AbsenceInfo.Key(member, reason)).ifPresent($ -> {
            throw Response.of("An absence with this reason already exists");
        });

        var memberInfo = members.findById(new TeamMemberInfo.Key(member))
                .or(() -> memberService.init(member))
                .orElseThrow(() -> Response.of("You are not a team member"));

        var frame   = new AbsenceInfo.TimeFrame(parseAbsenceTime(start), parseAbsenceTime(end), repetition);
        var absence = new AbsenceInfo(member.getGuild().getIdLong(), member.getIdLong(), reason, frame);

        absences.save(absence);
        memberInfo.getAbsences().add(absence);
        members.save(memberInfo);

        return Optional.of(absence.toEmbed().setTitle("New absence entry created").setColor(Constant.COLOR_SUCCESS));
    }

    @Interaction
    @Description("Remove an existing absence period")
    public void remove(Member member, @Parameter(completion = @Completion(provider = AbsenceInfo.ContextualReasonProvider.class)) String reason) {
        var key = new AbsenceInfo.Key(member, reason);

        if (!absences.existsById(key)) throw Response.of("No absence period with reason: " + reason);

        absences.deleteById(key);
    }

    @Contract("null -> null; !null -> _")
    private static LocalDateTime parseAbsenceTime(@Nullable String parse) {
        if (parse == null) return null;

        String  buf;
        Matcher matcher;
        var     time = LocalDateTime.now().withMinute(0);

        matcher = ABSENCE_DATE.matcher(parse);
        if (matcher.matches()) {
            buf  = matcher.group("day");
            time = time.withDayOfMonth(parseInt(buf));

            buf = matcher.group("month");
            if (buf != null) time = time.withMonth(parseInt(buf));

            buf = matcher.group("hour");
            if (buf != null) time = time.withHour(parseInt(buf));

            return time;
        }

        matcher = ABSENCE_TIME.matcher(parse);
        if (matcher.matches()) {
            buf  = matcher.group("hour");
            time = time.withHour(parseInt(buf));

            buf = matcher.group("minute");
            if (buf != null) time = time.withMinute(parseInt(buf));

            return time;
        }

        throw Response.of("Cannot parse date/time from string: " + parse);
    }
}
