package com.mekylei.transactionprocessing.configuracao;


import com.mekylei.transactionprocessing.transacao.estrategia.StrategyResolver;
import com.mekylei.transactionprocessing.transacao.estrategia.TransacaoStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class StrategyConfig {

    @Bean
    public StrategyResolver transacaoStrategy(List<TransacaoStrategy> strategies) {
        return new StrategyResolver(strategies);
    }
}
