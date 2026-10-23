package com.jbetting.betting.write.businesslogic.models.bet;

public record Title(String value) {

    public Title {
        if (value.isBlank()){
            throw new IllegalArgumentException("title required");
        }
    }
}
