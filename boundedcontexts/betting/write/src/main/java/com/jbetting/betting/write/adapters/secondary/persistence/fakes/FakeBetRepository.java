package com.jbetting.betting.write.adapters.secondary.persistence.fakes;

import com.jbetting.betting.write.businesslogic.models.bet.Bet;
import com.jbetting.betting.write.businesslogic.models.bet.ports.BetRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class FakeBetRepository  implements BetRepository {

    public Map<String,Bet> store = new HashMap<String, Bet>();

    @Override
    public void save(Bet bet) {
        store.put(bet.betId(),bet);
    }

    public Optional<Bet> findById(String betId){
        Bet bet = store.get(betId);
        return Optional.ofNullable(bet);
    }
}
