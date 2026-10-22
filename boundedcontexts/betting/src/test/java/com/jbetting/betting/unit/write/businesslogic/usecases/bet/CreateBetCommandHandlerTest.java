package com.jbetting.betting.unit.write.businesslogic.usecases.bet;


import com.jbetting.betting.write.adapters.secondary.persistence.fakes.FakeBetRepository;
import com.jbetting.betting.write.businesslogic.models.bet.Bet;
import com.jbetting.betting.write.businesslogic.usecases.bet.CreateBetCommand;
import com.jbetting.betting.write.businesslogic.usecases.bet.CreateBetCommandHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class CreateBetCommandHandlerTest {

    static final String BET_ID = "bet-1";
    static final String ALICE_EMAIL = "alice@gmail.com";
    static final String SIMPLE_TITLE = "will it happen ?";
    static final List<String> SIMPLE_OUTCOMES = List.of("yes", "no");
    static final Instant SIMPLE_CLOSES_AT = Instant.parse("2026-10-22T14:00:00Z");
    static final String SIMPLE_DESCRIPTION = "This a bet on the result of an event";

    FakeBetRepository betRepository = new FakeBetRepository();
    CreateBetCommandHandler handler = new CreateBetCommandHandler(betRepository);


    @Test
    void should_create_bet() {

        //given
        CreateBetCommand command = givenASimpleCreateBetCommand();
        //when
        handler.handle(command);
        //then
        Optional<Bet> bet = betRepository.findById(BET_ID);
        assertThat(bet).contains(new Bet(BET_ID, ALICE_EMAIL, SIMPLE_TITLE, SIMPLE_OUTCOMES, SIMPLE_DESCRIPTION, SIMPLE_CLOSES_AT));

    }


    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void should_refuse_the_creation_when_the_title_is_blank(String title) {

        CreateBetCommand command = givenCreateBetCommandWithTitle(title);
        assertThatThrownBy(() -> handler.handle(command)).isInstanceOf(IllegalArgumentException.class).hasMessage("title required");
    }

    @ParameterizedTest
    @MethodSource("invalidOutcomes")
    void should_refuse_the_creation_when_the_number_of_outcomes_is_invalid(List<String> outcomes) {
        var command = givenCreateBetCommandWithOutcomes(outcomes);
        assertThatThrownBy(() -> handler.handle(command)).isInstanceOf(IllegalArgumentException.class).hasMessage("invalid number of outcomes");
    }
    @Test
    void should_refuse_the_creation_when_an_outcome_is_blank(){
        var command = givenCreateBetCommandWithOutcomes(List.of("yes","   "));
        assertThatThrownBy(() -> handler.handle(command)).isInstanceOf(IllegalArgumentException.class).hasMessage("outcome label required");
    }

    private static CreateBetCommand givenCreateBetCommandWithTitle(String title) {

        return new CreateBetCommand(BET_ID, ALICE_EMAIL, title, SIMPLE_OUTCOMES, SIMPLE_DESCRIPTION, SIMPLE_CLOSES_AT);
    }

    private static CreateBetCommand givenCreateBetCommandWithOutcomes(List<String> outcomes) {

        return new CreateBetCommand(BET_ID, ALICE_EMAIL, SIMPLE_TITLE, outcomes, SIMPLE_DESCRIPTION, SIMPLE_CLOSES_AT);
    }

    private static CreateBetCommand givenASimpleCreateBetCommand() {
        return new CreateBetCommand(BET_ID, ALICE_EMAIL, SIMPLE_TITLE, SIMPLE_OUTCOMES, SIMPLE_DESCRIPTION, SIMPLE_CLOSES_AT);
    }

    static Stream<List<String>> invalidOutcomes(){
        var outcomes101 = IntStream.rangeClosed(1, 101).mapToObj(i -> "issue" + i).toList();
        return Stream.of(
                List.of("yes"),
                outcomes101
        );
    }


}
