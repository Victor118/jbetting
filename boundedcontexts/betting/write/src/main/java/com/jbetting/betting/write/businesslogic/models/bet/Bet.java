package com.jbetting.betting.write.businesslogic.models.bet;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

public record Bet(String betId, String creatorId, Title title, List<String> outcomes, String description,
                  Instant closesAt) {

    private static final int MIN_OUTCOME_COUNT = 2;
    private static final int MAX_OUTCOME_COUNT = 100;

    public Bet {
        if (outcomes.size() < MIN_OUTCOME_COUNT || outcomes.size() > MAX_OUTCOME_COUNT) {
            throw new IllegalArgumentException("invalid number of outcomes");
        }
        if (outcomes.stream().anyMatch(String::isBlank)) {
            throw new IllegalArgumentException("outcome label required");
        }
        if (outcomes.stream().map((s) -> s.toLowerCase(Locale.ROOT)).distinct().count() != outcomes.size()) {
            throw new IllegalArgumentException("duplicated outcomes");
        }
    }
}
