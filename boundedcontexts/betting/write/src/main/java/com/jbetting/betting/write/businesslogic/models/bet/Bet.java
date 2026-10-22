package com.jbetting.betting.write.businesslogic.models.bet;

import java.time.Instant;
import java.util.List;

public record Bet(String betId, String creatorId, String title, List<String> outcomes, String description, Instant closesAt) {

    public Bet {
        if (title.isBlank()) {
            throw new IllegalArgumentException("title required");
        }
        if (outcomes.size() < 2){
            throw new IllegalArgumentException("invalid number of outcomes");
        }
    }
}
