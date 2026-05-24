package com.mekylei.transactionprocessing.configuracao.spring.bean;


import com.mekylei.transactionprocessing.transacao.aplicacao.orquestracao.StrategyResolver;
import com.mekylei.transactionprocessing.transacao.aplicacao.porta.integracao.AntiFraudeGateway;
import com.mekylei.transactionprocessing.transacao.estrategia.PixTransacaoStrategy;
import com.mekylei.transactionprocessing.transacao.estrategia.TedTransacaoStrategy;
import com.mekylei.transactionprocessing.transacao.estrategia.TefTransacaoStrategy;
import com.mekylei.transactionprocessing.transacao.estrategia.TransacaoStrategy;
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
    public TefTransacaoStrategy tefTransacaoStrategy(AntiFraudeGateway gateway) {
        return new TefTransacaoStrategy(gateway);
    }

    @Bean
    public StrategyResolver transacaoStrategy(List<TransacaoStrategy> strategies) {
        return new StrategyResolver(strategies);
    }
}
