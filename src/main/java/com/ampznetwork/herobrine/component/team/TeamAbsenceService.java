package com.ampznetwork.herobrine.component.team;

import com.ampznetwork.herobrine.component.team.user.AbsenceInfo;
import com.ampznetwork.herobrine.component.team.user.TeamMemberInfo;
import com.ampznetwork.herobrine.repo.AbsenceInfoRepository;
import com.ampznetwork.herobrine.repo.TeamMemberInfoRepository;
import com.ampznetwork.herobrine.util.Constant;
import lombok.extern.java.Log;
import net.dv8tion.jda.api.components.checkbox.Checkbox;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.modals.Modal;
import org.comroid.annotations.Description;
import org.comroid.interaction.annotation.Completion;
import org.comroid.interaction.annotation.ContextFilter;
import org.comroid.interaction.annotation.Interaction;
import org.comroid.interaction.annotation.Parameter;
import org.comroid.interaction.model.Response;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@Log
@Service
@Interaction(value = "team-absence", filter = { @ContextFilter(key = "guild") })
public class TeamAbsenceService {
    public static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("[dd.MM[.[yyyy]]][ ][kk[:mm]]");

    public static final String INTERACTION_CREATE = "tas_create_";
    public static final String INTERACTION_EDIT   = "tas_edit_";

    public static final String OPTION_REASON            = "option_reason";
    public static final String OPTION_TIME_START        = "option_time_start";
    public static final String OPTION_TIME_END          = "option_time_end";
    public static final String OPTION_TIME_REPETITION   = "option_time_repetition";
    public static final String OPTION_TIME_REPEAT_UNTIL = "option_time_repeat_until";
    public static final String OPTION_TIME_ENTIRE_DAY   = "option_time_entire_day";

    public static final String PLACEHOLDER_DATETIME = "date like `dd.MM(.yyyy)` or time like `hh(:mm)` or a mix of both";

    @Autowired TeamMemberService        memberService;
    @Autowired AbsenceInfoRepository    absences;
    @Autowired TeamMemberInfoRepository members;

    @Interaction
    @Description("Create a new absence period")
    public Modal.Builder create(Member member, @Parameter String reason) {
        return openEditor(reason, null);
    }

    @Interaction
    @Description("Edit an existing absence period")
    public Optional<Modal.Builder> edit(
            Member member,
            @Parameter(completion = @Completion(provider = AbsenceInfo.ContextualReasonProvider.class)) String reason
    ) {
        return absences.findById(new AbsenceInfo.Key(member, reason)).map(info -> openEditor(info.getReason(), info));
    }

    @Interaction
    @Description("Remove an existing absence period")
    public void remove(Member member, @Parameter(completion = @Completion(provider = AbsenceInfo.ContextualReasonProvider.class)) String reason) {
        var key = new AbsenceInfo.Key(member, reason);

        if (!absences.existsById(key)) throw Response.of("No absence period with reason: " + reason);

        absences.deleteById(key);
    }

    @EventListener
    public void on(ModalInteractionEvent event) {
        var modalId = event.getModalId();
        if (Stream.of(INTERACTION_CREATE, INTERACTION_EDIT).noneMatch(modalId::startsWith)) return;

        var member = Objects.requireNonNull(event.getMember(), "guild member");
        var create = INTERACTION_CREATE.equals(modalId);
        var reason = modalId.substring((create ? INTERACTION_CREATE : INTERACTION_EDIT).length() + 2);
        var key    = create ? null : new AbsenceInfo.Key(member, reason);
        AbsenceInfo.Builder           absenceBuilder;
        AbsenceInfo.TimeFrame.Builder timeBuilder;

        if (create) {
            absenceBuilder = AbsenceInfo.builder().guildId(member.getGuild().getIdLong()).userId(member.getIdLong());
            timeBuilder    = AbsenceInfo.TimeFrame.builder();
        } else {
            var old = absences.findById(key).orElse(null);
            if (old == null) {
                event.reply("No absence found with reason " + reason).setEphemeral(true).queue();
                return;
            }

            absenceBuilder = old.toBuilder();
            timeBuilder    = old.getTimeFrame().toBuilder();
        }

        TemporalAccessor temporal;
        var              mapping = event.getValue(OPTION_REASON);
        if (mapping != null) absenceBuilder.reason(mapping.getAsString());

        mapping = event.getValue(OPTION_TIME_START);
        if (mapping != null) {
            temporal = DATE_TIME_FORMAT.parse(mapping.getAsString());
            timeBuilder.startDateTime(LocalDateTime.from(temporal));
        }

        mapping = event.getValue(OPTION_TIME_END);
        if (mapping != null) {
            temporal = DATE_TIME_FORMAT.parse(mapping.getAsString());
            timeBuilder.endDateTime(LocalDateTime.from(temporal));
        }

        mapping = event.getValue(OPTION_TIME_REPETITION);
        if (mapping != null) timeBuilder.repetition(AbsenceInfo.Repetition.valueOf(mapping.getAsStringList().getFirst()));

        mapping = event.getValue(OPTION_TIME_REPEAT_UNTIL);
        if (mapping != null) {
            temporal = DATE_TIME_FORMAT.parse(mapping.getAsString());
            timeBuilder.repeatUntil(LocalDateTime.from(temporal));
        }

        mapping = event.getValue(OPTION_TIME_ENTIRE_DAY);
        if (mapping != null) timeBuilder.entireDay(mapping.getAsBoolean());

        var timeFrame  = timeBuilder.build();
        var absence    = absenceBuilder.timeFrame(timeFrame).build();
        var memberInfo = members.findById(new TeamMemberInfo.Key(member)).orElseGet(() -> memberService.init(member).orElseThrow());

        if (!create && absences.existsById(key)) absences.deleteById(key);

        absences.save(absence);
        memberInfo.getAbsences().add(absence);
        members.save(memberInfo);

        event.replyEmbeds(absence.toEmbed().setTitle("New absence entry created").setColor(Constant.COLOR_SUCCESS).build()).queue();
    }

    private Modal.Builder openEditor(@NonNull String reason, @Nullable AbsenceInfo info) {
        var create = info == null;
        var opt    = Optional.ofNullable(info);

        return Modal.create((create ? INTERACTION_CREATE : INTERACTION_EDIT) + reason, "Modifying absence")
                .addComponents(Label.of("Beginning",
                                TextInput.create(OPTION_TIME_START, TextInputStyle.SHORT)
                                        .setValue(opt.map(AbsenceInfo::getTimeFrame)
                                                .map(AbsenceInfo.TimeFrame::startDateTime)
                                                .map(DATE_TIME_FORMAT::format)
                                                .orElse(null))
                                        .setPlaceholder(PLACEHOLDER_DATETIME)
                                        .build()),
                        Label.of("Ending",
                                TextInput.create(OPTION_TIME_END, TextInputStyle.SHORT)
                                        .setValue(opt.map(AbsenceInfo::getTimeFrame)
                                                .map(AbsenceInfo.TimeFrame::endDateTime)
                                                .map(DATE_TIME_FORMAT::format)
                                                .orElse(null))
                                        .setPlaceholder(PLACEHOLDER_DATETIME)
                                        .setRequired(false)
                                        .build()),
                        Label.of("Repetition Pattern",
                                StringSelectMenu.create(OPTION_TIME_REPETITION)
                                        .addOptions(Arrays.stream(AbsenceInfo.Repetition.values()).map(AbsenceInfo.Repetition::getSelectOption).toList())
                                        .setDefaultOptions(opt.map(AbsenceInfo::getTimeFrame)
                                                .map(AbsenceInfo.TimeFrame::repetition)
                                                .map(AbsenceInfo.Repetition::getSelectOption)
                                                .stream()
                                                .toList())
                                        .setRequired(false)
                                        .build()),
                        Label.of("Repeat until",
                                TextInput.create(OPTION_TIME_REPEAT_UNTIL, TextInputStyle.SHORT)
                                        .setValue(opt.map(AbsenceInfo::getTimeFrame)
                                                .map(AbsenceInfo.TimeFrame::repeatUntil)
                                                .map(DATE_TIME_FORMAT::format)
                                                .orElse(null))
                                        .setPlaceholder(PLACEHOLDER_DATETIME)
                                        .setRequired(false)
                                        .build()),
                        Label.of("All day long", Checkbox.of(OPTION_TIME_ENTIRE_DAY)));
    }
}
