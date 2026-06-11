package com.mekylei.transactionprocessing.transacao.estrategia;


import com.mekylei.transactionprocessing.compartilhado.exception.RegraNegocioException;
import com.mekylei.transactionprocessing.compartilhado.dominio.TipoTransacao;

import java.util.List;

public class StrategyResolver {

    private final List<TransacaoStrategy> strategies;

    public StrategyResolver(List<TransacaoStrategy> strategies) {
        this.strategies = strategies;
    }

    public TransacaoStrategy resolve(TipoTransacao tipoTransacao) {
        return strategies.stream()
                .filter(strategy -> strategy.suporta(tipoTransacao))
                .findFirst()
                .orElseThrow(() -> new RegraNegocioException(
                        "STRATEGY_NAO_ENCONTRADA",
                        "Nenhuma strategy registrada para o tipo: " + tipoTransacao));
    }
}
