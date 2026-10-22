package com.jbetting.betting.write.businesslogic.models.bet.ports;

import com.jbetting.betting.write.businesslogic.models.bet.Bet;

public interface BetRepository {

    void save(Bet bet);
}
