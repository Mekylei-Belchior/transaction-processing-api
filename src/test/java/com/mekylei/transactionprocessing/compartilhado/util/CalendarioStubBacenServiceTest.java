package com.mekylei.transactionprocessing.compartilhado.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class CalendarioStubBacenServiceTest {

    private CalendarioStubBacenService service;

    @BeforeEach
    void setUp() {
        service = new CalendarioStubBacenService();
    }

    @Test
    void isDiaUtil_deveRetornarFalseParaSabado() {
        assertThat(service.isDiaUtil(LocalDate.of(2026, 6, 6))).isFalse();
    }

    @Test
    void isDiaUtil_deveRetornarFalseParaDomingo() {
        assertThat(service.isDiaUtil(LocalDate.of(2026, 6, 7))).isFalse();
    }

    @Test
    void isDiaUtil_deveRetornarTrueParaSegundaFeira() {
        assertThat(service.isDiaUtil(LocalDate.of(2026, 6, 8))).isTrue();
    }

    @Test
    void isDiaUtil_deveRetornarFalseParaFeriadoNacional() {
        assertThat(service.isDiaUtil(LocalDate.of(2026, 4, 21))).isFalse();
    }

    @Test
    void isDiaUtil_deveRetornarTrueParaDiaUtilNaoFeriado() {
        assertThat(service.isDiaUtil(LocalDate.of(2026, 6, 3))).isTrue();
    }
}
