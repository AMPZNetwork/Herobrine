package com.ampznetwork.herobrine.component.user.tags.model;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import java.util.Arrays;
import java.util.stream.Stream;

@FieldDefaults(makeFinal = true, level = AccessLevel.PUBLIC)
public enum UserTag {
    VIP, Team, Helper(Team), Staff(Team), Moderator(Staff), Admin(Moderator);

    UserTag[] inherits;

    UserTag(UserTag... inherits) {
        this.inherits = inherits;
    }

    public Stream<UserTag> expand() {
        return Stream.concat(Stream.of(this), Arrays.stream(inherits));
    }
}
