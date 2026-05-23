package com.mekylei.transactionprocessing.configuracao.spring.bean;


import com.mekylei.transactionprocessing.transacao.estrategia.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class StrategyConfig {

    @Bean
    public PixTransacaoStrategy pixTransacaoStrategy() {
        return new PixTransacaoStrategy();
    }

    @Bean
    public TedTransacaoStrategy tedTransacaoStrategy() {
        return new TedTransacaoStrategy();
    }

    @Bean
    public TefTransacaoStrategy tefTransacaoStrategy() {
        return new TefTransacaoStrategy();
    }

    @Bean
    public StrategyResolver transacaoStrategy(List<TransacaoStrategy> strategies) {
        return new StrategyResolver(strategies);
    }
}
