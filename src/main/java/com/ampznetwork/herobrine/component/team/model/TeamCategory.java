package com.ampznetwork.herobrine.component.team.model;

import lombok.Getter;
import net.dv8tion.jda.api.components.selections.SelectOption;
import org.comroid.api.attr.Named;

@Getter
public enum TeamCategory implements Named, Comparable<TeamCategory> {
    Helper, Moderator, Admin;

    final SelectOption selectOption = SelectOption.of(name(), name());
}
