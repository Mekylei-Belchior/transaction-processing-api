package com.mekylei.transactionprocessing.compartilhado.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitários para {@link CalendarioStubBacenService}.
 *
 * <p>Objetivo:</p>
 * <ul>
 *     <li>Validar o comportamento esperado de {@link CalendarioStubBacenService} nos cenários exercitados pela suíte.</li>
 *     <li>Preservar regras de negócio, contratos, integrações ou invariantes aplicáveis à classe testada.</li>
 *     <li>Garantir regressão funcional para alterações futuras relacionadas a {@code CalendarioStubBacenService}.</li>
 * </ul>
 *
 * <p>Cenários cobertos:</p>
 * <ul>
 *     <li>IsDiaUtil deve retornar false para sábado.</li>
 *     <li>IsDiaUtil deve retornar false para domingo.</li>
 *     <li>IsDiaUtil deve retornar true para segunda feira.</li>
 *     <li>IsDiaUtil deve retornar false para feriado nacional.</li>
 *     <li>IsDiaUtil deve retornar true para dia util não feriado.</li>
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
@DisplayName("Calendario Stub Bacen Service")
class CalendarioStubBacenServiceTest {

    private CalendarioStubBacenService service;

    @BeforeEach
    void setUp() {
        service = new CalendarioStubBacenService();
    }

    @Test
    @DisplayName("isDiaUtil deve retornar false para sábado")
    void isDiaUtil_deveRetornarFalseParaSabado() {
        assertThat(service.isDiaUtil(LocalDate.of(2026, 6, 6))).isFalse();
    }

    @Test
    @DisplayName("isDiaUtil deve retornar false para domingo")
    void isDiaUtil_deveRetornarFalseParaDomingo() {
        assertThat(service.isDiaUtil(LocalDate.of(2026, 6, 7))).isFalse();
    }

    @Test
    @DisplayName("isDiaUtil deve retornar true para segunda feira")
    void isDiaUtil_deveRetornarTrueParaSegundaFeira() {
        assertThat(service.isDiaUtil(LocalDate.of(2026, 6, 8))).isTrue();
    }

    @Test
    @DisplayName("isDiaUtil deve retornar false para feriado nacional")
    void isDiaUtil_deveRetornarFalseParaFeriadoNacional() {
        assertThat(service.isDiaUtil(LocalDate.of(2026, 4, 21))).isFalse();
    }

    @Test
    @DisplayName("isDiaUtil deve retornar true para dia util não feriado")
    void isDiaUtil_deveRetornarTrueParaDiaUtilNaoFeriado() {
        assertThat(service.isDiaUtil(LocalDate.of(2026, 6, 3))).isTrue();
    }
}
