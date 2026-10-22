package com.jbetting.betting.unit.write.businesslogic.usecases.bet;


import com.jbetting.betting.write.adapters.secondary.persistence.fakes.FakeBetRepository;
import com.jbetting.betting.write.businesslogic.models.bet.Bet;
import com.jbetting.betting.write.businesslogic.usecases.bet.CreateBetCommand;
import com.jbetting.betting.write.businesslogic.usecases.bet.CreateBetCommandHandler;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;


public class CreateBetCommandHandlerTest {


    FakeBetRepository betRepository = new FakeBetRepository();


    @Test
    void should_create_bet() {

        //given
        String aliceEmail = "alice@gmail.com";
        var createBetCommandHandler = new CreateBetCommandHandler(betRepository);
        var betId = "bet-1";
        String title = "Who gonna win ?";
        String description = "This a bet on the result of a tennis match between Sinner";
        Instant closesAt = Instant.parse("2026-10-22T14:00:00Z");
        //when
        createBetCommandHandler.handle(new CreateBetCommand(betId, aliceEmail, title, List.of("Sinner", "Alcaraz"), description, closesAt));
        //then
        Optional<Bet> bet = betRepository.findById(betId);
        assertThat(bet).contains(new Bet(betId, aliceEmail, title, List.of("Sinner", "Alcaraz"), description, closesAt));

    }
}
