package com.mekylei.transactionprocessing.observabilidade.mascaramento.estrategia;

import com.mekylei.transactionprocessing.observabilidade.logging.TipoCampoMascarado;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StrategyMascaramentoResolverTest {

    @Test
    void deveResolverStrategyParaCadaTipoRegistrado() {
        assertThat(StrategyMascaramentoResolver.resolve(TipoCampoMascarado.MESSAGE))
                .isInstanceOf(MensagemMascaradaStrategy.class);
        assertThat(StrategyMascaramentoResolver.resolve(TipoCampoMascarado.STACKTRACE))
                .isInstanceOf(StacktraceMascaradoStrategy.class);
        assertThat(StrategyMascaramentoResolver.resolve(TipoCampoMascarado.JSON))
                .isInstanceOf(JsonMascaradoStrategy.class);
        assertThat(StrategyMascaramentoResolver.resolve(TipoCampoMascarado.HEADER))
                .isInstanceOf(HeaderMascaradoStrategy.class);
    }
}
