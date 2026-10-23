package com.jbetting.betting.write.businesslogic.usecases.bet;

import com.jbetting.betting.write.businesslogic.models.bet.Bet;
import com.jbetting.betting.write.businesslogic.models.bet.Title;
import com.jbetting.betting.write.businesslogic.models.bet.ports.BetRepository;

public class CreateBetCommandHandler {

    private final BetRepository betRepository;

    public CreateBetCommandHandler(BetRepository betRepository){
        this.betRepository = betRepository;
    }

    public void handle(CreateBetCommand command) {
        var bet = new Bet(command.betId(),command.creatorId(),new Title(command.title()),command.outcomes(),command.description(),command.closesAt());
        betRepository.save(bet);
    }
}
