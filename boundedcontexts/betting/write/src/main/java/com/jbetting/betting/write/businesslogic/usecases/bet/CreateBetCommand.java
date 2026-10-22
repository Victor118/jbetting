package com.jbetting.betting.write.businesslogic.usecases.bet;

import java.time.Instant;
import java.util.List;

public record CreateBetCommand(String betId, String creatorId, String title, List<String> outcomes, String description, Instant closesAt) {
}
