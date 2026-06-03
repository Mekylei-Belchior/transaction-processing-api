package com.mekylei.transactionprocessing.transacao.estrategia;

import com.mekylei.transactionprocessing.compartilhado.exception.RegraNegocioException;
import com.mekylei.transactionprocessing.transacao.dominio.TipoTransacao;
import com.mekylei.transactionprocessing.transacao.dominio.Transacao;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StrategyResolverTest {

    @Test
    void resolve_deveRetornarStrategyCorretaParaPIX() {
        TransacaoStrategy pixStrategy = strategyQueSuporta(TipoTransacao.PIX);
        StrategyResolver resolver = new StrategyResolver(List.of(
                strategyQueSuporta(TipoTransacao.TED),
                pixStrategy,
                strategyQueSuporta(TipoTransacao.TEF)
        ));

        TransacaoStrategy resolvida = resolver.resolve(TipoTransacao.PIX);

        assertThat(resolvida).isSameAs(pixStrategy);
    }

    @Test
    void resolve_deveRetornarStrategyCorretaParaTED() {
        TransacaoStrategy tedStrategy = strategyQueSuporta(TipoTransacao.TED);
        StrategyResolver resolver = new StrategyResolver(List.of(
                strategyQueSuporta(TipoTransacao.PIX),
                tedStrategy,
                strategyQueSuporta(TipoTransacao.TEF)
        ));

        TransacaoStrategy resolvida = resolver.resolve(TipoTransacao.TED);

        assertThat(resolvida).isSameAs(tedStrategy);
    }

    @Test
    void resolve_deveRetornarStrategyCorretaParaTEF() {
        TransacaoStrategy tefStrategy = strategyQueSuporta(TipoTransacao.TEF);
        StrategyResolver resolver = new StrategyResolver(List.of(
                strategyQueSuporta(TipoTransacao.PIX),
                strategyQueSuporta(TipoTransacao.TED),
                tefStrategy
        ));

        TransacaoStrategy resolvida = resolver.resolve(TipoTransacao.TEF);

        assertThat(resolvida).isSameAs(tefStrategy);
    }

    @Test
    void resolve_deveLancarRegraNegocioExceptionQuandoNenhumaStrategySuporta() {
        StrategyResolver resolver = new StrategyResolver(List.of(strategyQueNaoSuporta()));

        assertThatThrownBy(() -> resolver.resolve(TipoTransacao.PIX))
                .isInstanceOf(RegraNegocioException.class)
                .hasFieldOrPropertyWithValue("codigoErro", "STRATEGY_NAO_ENCONTRADA");
    }

    @Test
    void resolve_deveUsarPrimeiraStrategyQueSuporta() {
        TransacaoStrategy primeira = strategyQueSuporta(TipoTransacao.PIX);
        TransacaoStrategy segunda = strategyQueSuporta(TipoTransacao.PIX);
        StrategyResolver resolver = new StrategyResolver(List.of(primeira, segunda));

        TransacaoStrategy resolvida = resolver.resolve(TipoTransacao.PIX);

        assertThat(resolvida).isSameAs(primeira);
    }

    private TransacaoStrategy strategyQueSuporta(TipoTransacao tipoSuportado) {
        return new TransacaoStrategy() {
            @Override
            public boolean suporta(TipoTransacao tipoTransacao) {
                return tipoSuportado == tipoTransacao;
            }

            @Override
            public Transacao processa(Transacao transacao) {
                return transacao;
            }
        };
    }

    private TransacaoStrategy strategyQueNaoSuporta() {
        return new TransacaoStrategy() {
            @Override
            public boolean suporta(TipoTransacao tipoTransacao) {
                return false;
            }

            @Override
            public Transacao processa(Transacao transacao) {
                return transacao;
            }
        };
    }

}
