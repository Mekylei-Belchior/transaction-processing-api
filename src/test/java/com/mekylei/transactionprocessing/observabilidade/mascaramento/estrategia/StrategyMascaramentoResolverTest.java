package com.mekylei.transactionprocessing.observabilidade.mascaramento.estrategia;

import com.mekylei.transactionprocessing.observabilidade.logging.TipoCampoMascarado;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitários para {@link StrategyMascaramentoResolver}.
 *
 * <p>Objetivo:</p>
 * <ul>
 *     <li>Validar o comportamento esperado de {@link StrategyMascaramentoResolver} nos cenários exercitados pela suíte.</li>
 *     <li>Preservar regras de negócio, contratos, integrações ou invariantes aplicáveis à classe testada.</li>
 *     <li>Garantir regressão funcional para alterações futuras relacionadas a {@code StrategyMascaramentoResolver}.</li>
 * </ul>
 *
 * <p>Cenários cobertos:</p>
 * <ul>
 *     <li>Deve resolver strategy para cada tipo registrado.</li>
 * </ul>
 *
 * <p>Cenários não cobertos:</p>
 * <ul>
 *     <li>Testes de carga, resiliência distribuída e validações de infraestrutura externas ao escopo da classe.</li>
 * </ul>
 *
 * @author Mekylei Belchior
 * @since 1.0
 */
@DisplayName("Strategy Mascaramento Resolver")
class StrategyMascaramentoResolverTest {

    @Test
    @DisplayName("deve resolver strategy para cada tipo registrado")
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
