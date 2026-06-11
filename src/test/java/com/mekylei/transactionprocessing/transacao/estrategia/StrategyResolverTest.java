package com.mekylei.transactionprocessing.transacao.estrategia;

import com.mekylei.transactionprocessing.compartilhado.exception.RegraNegocioException;
import com.mekylei.transactionprocessing.compartilhado.dominio.TipoTransacao;
import com.mekylei.transactionprocessing.transacao.dominio.Transacao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes unitários para {@link StrategyResolver}.
 *
 * <p>Objetivo:</p>
 * <ul>
 *     <li>Validar o comportamento esperado de {@link StrategyResolver} nos cenários exercitados pela suíte.</li>
 *     <li>Preservar regras de negócio, contratos, integrações ou invariantes aplicáveis à classe testada.</li>
 *     <li>Garantir regressão funcional para alterações futuras relacionadas a {@code StrategyResolver}.</li>
 * </ul>
 *
 * <p>Cenários cobertos:</p>
 * <ul>
 *     <li>Resolve deve retornar strategy correta para Pix.</li>
 *     <li>Resolve deve retornar strategy correta para TED.</li>
 *     <li>Resolve deve retornar strategy correta para TEF.</li>
 *     <li>Resolve deve lançar regra negocio exception quando nenhuma strategy suporta.</li>
 *     <li>Resolve deve usar primeira strategy que suporta.</li>
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
@DisplayName("Strategy Resolver")
class StrategyResolverTest {

    @Test
    @DisplayName("resolve deve retornar strategy correta para Pix")
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
    @DisplayName("resolve deve retornar strategy correta para TED")
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
    @DisplayName("resolve deve retornar strategy correta para TEF")
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
    @DisplayName("resolve deve lançar regra negocio exception quando nenhuma strategy suporta")
    void resolve_deveLancarRegraNegocioExceptionQuandoNenhumaStrategySuporta() {
        StrategyResolver resolver = new StrategyResolver(List.of(strategyQueNaoSuporta()));

        assertThatThrownBy(() -> resolver.resolve(TipoTransacao.PIX))
                .isInstanceOf(RegraNegocioException.class)
                .hasFieldOrPropertyWithValue("codigoErro", "STRATEGY_NAO_ENCONTRADA");
    }

    @Test
    @DisplayName("resolve deve usar primeira strategy que suporta")
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
