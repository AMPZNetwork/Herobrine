package com.ampznetwork.herobrine.component.team.model;

import lombok.Getter;
import net.dv8tion.jda.api.components.selections.SelectOption;
import org.comroid.api.attr.Named;

@Getter
public enum SupportLevel implements Named {
    Level_1, Level_2, Level_3, Specialist;

    final SelectOption selectOption = SelectOption.of(name(), name());
}
