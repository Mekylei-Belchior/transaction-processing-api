package com.mekylei.transactionprocessing.transacao.estrategia;

import com.mekylei.transactionprocessing.compartilhado.dominio.ValorMonetario;
import com.mekylei.transactionprocessing.compartilhado.exception.RegraNegocioException;
import com.mekylei.transactionprocessing.compartilhado.util.CalendarioStubBacenService;
import com.mekylei.transactionprocessing.transacao.dominio.StatusTransacao;
import com.mekylei.transactionprocessing.compartilhado.dominio.TipoTransacao;
import com.mekylei.transactionprocessing.transacao.dominio.Transacao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Testes unitários para {@link TedTransacaoStrategy}.
 *
 * <p>Objetivo:</p>
 * <ul>
 *     <li>Validar o comportamento esperado de {@link TedTransacaoStrategy} nos cenários exercitados pela suíte.</li>
 *     <li>Preservar regras de negócio, contratos, integrações ou invariantes aplicáveis à classe testada.</li>
 *     <li>Garantir regressão funcional para alterações futuras relacionadas a {@code TedTransacaoStrategy}.</li>
 * </ul>
 *
 * <p>Cenários cobertos:</p>
 * <ul>
 *     <li>Suporta deve retornar true para TED.</li>
 *     <li>Suporta deve retornar false para Pix.</li>
 *     <li>Suporta deve retornar false para TEF.</li>
 *     <li>Processa deve lançar regra negocio exception quando dia não util.</li>
 *     <li>Processa deve retornar transação COMPLETADA quando dia util e horário permitido.</li>
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
@ExtendWith(MockitoExtension.class)
@DisplayName("Ted Transacao Strategy")
class TedTransacaoStrategyTest {

    private static final LocalTime BANCO_TED_INICIO = LocalTime.of(6, 0);
    private static final LocalTime BANCO_TED_FIM = LocalTime.of(17, 0);
    private static final ZoneId BRASIL_TIMEZONE = ZoneId.of("America/Sao_Paulo");

    @Mock
    private CalendarioStubBacenService calendarioService;

    private TedTransacaoStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new TedTransacaoStrategy(calendarioService);
    }

    @Test
    @DisplayName("suporta deve retornar true para TED")
    void suporta_deveRetornarTrueParaTED() {
        assertThat(strategy.suporta(TipoTransacao.TED)).isTrue();
    }

    @Test
    @DisplayName("suporta deve retornar false para Pix")
    void suporta_deveRetornarFalseParaPIX() {
        assertThat(strategy.suporta(TipoTransacao.PIX)).isFalse();
    }

    @Test
    @DisplayName("suporta deve retornar false para TEF")
    void suporta_deveRetornarFalseParaTEF() {
        assertThat(strategy.suporta(TipoTransacao.TEF)).isFalse();
    }

    @Test
    @DisplayName("processa deve lançar regra negocio exception quando dia não util")
    void processa_deveLancarRegraNegocioExceptionQuandoDiaNaoUtil() {
        when(calendarioService.isDiaUtil(any(LocalDate.class))).thenReturn(false);

        assertThatThrownBy(() -> strategy.processa(transacao(TipoTransacao.TED)))
                .isInstanceOf(RegraNegocioException.class)
                .hasFieldOrPropertyWithValue("codigoErro", "TED_DIA_NAO_UTIL");
    }

    @Test
    @DisplayName("processa deve retornar transação COMPLETADA quando dia util e horário permitido")
    @Tag("horario-dependente")
    void processa_deveRetornarTransacaoCOMPLETADAQuandoDiaUtilEHorarioPermitido() {
        LocalTime agora = LocalTime.now(BRASIL_TIMEZONE);
        assumeTrue(!agora.isBefore(BANCO_TED_INICIO) && !agora.isAfter(BANCO_TED_FIM),
                "TedTransacaoStrategy valida LocalTime.now diretamente; executar apenas entre 06:00 e 17:00 BRT");

        when(calendarioService.isDiaUtil(any(LocalDate.class))).thenReturn(true);
        Transacao transacao = transacao(TipoTransacao.TED);

        Transacao processada = strategy.processa(transacao);

        assertThat(processada).isNotSameAs(transacao);
        assertThat(processada.getStatus()).isEqualTo(StatusTransacao.COMPLETADA);
        assertThat(transacao.getStatus()).isEqualTo(StatusTransacao.PENDENTE);
    }

    private Transacao transacao(TipoTransacao tipo) {
        return Transacao.builder()
                .idContaOrigem(UUID.randomUUID())
                .contaDestino("0001-12345-6")
                .tipo(tipo)
                .valor(ValorMonetario.paraReal(new BigDecimal("100.00")))
                .build();
    }

}
