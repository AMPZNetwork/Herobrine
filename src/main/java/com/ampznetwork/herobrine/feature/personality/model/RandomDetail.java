package com.ampznetwork.herobrine.feature.personality.model;

import jakarta.persistence.Embeddable;
import lombok.Data;

import java.util.Random;

@Data
@Embeddable
public class RandomDetail {
    int chance, in;

    public boolean check() {
        return check(new Random());
    }

    public boolean check(Random rng) {
        return rng.nextInt(in) < chance;
    }
}
