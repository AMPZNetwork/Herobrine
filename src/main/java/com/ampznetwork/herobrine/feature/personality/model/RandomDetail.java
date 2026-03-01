package com.ampznetwork.herobrine.feature.personality.model;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Random;

@Data
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class RandomDetail {
    int chance, limes;

    public boolean check() {
        return check(new Random());
    }

    public boolean check(Random rng) {
        return rng.nextInt(limes) < chance;
    }

    @Override
    public String toString() {
        return "`%d in %d` chance".formatted(chance, limes);
    }
}
