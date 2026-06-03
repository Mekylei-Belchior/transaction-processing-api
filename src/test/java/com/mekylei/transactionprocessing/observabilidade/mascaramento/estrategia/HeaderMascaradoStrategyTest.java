package com.mekylei.transactionprocessing.observabilidade.mascaramento.estrategia;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HeaderMascaradoStrategyTest {

    private HeaderMascaradoStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new HeaderMascaradoStrategy();
    }

    @Test
    void deveMascararHeaderAuthorization() {
        String resultado = strategy.mascarar("Authorization: segredo");

        assertThat(resultado).isEqualTo("Authorization: ****");
    }

    @Test
    void deveMascararHeadersDefinidosComoSensiveis() {
        String resultado = strategy.mascarar("Bearer abc.def-123");

        assertThat(resultado).isEqualTo("Bearer ****");
    }

    @Test
    void naoDeveAlterarHeadersNaoSensiveis() {
        String header = "Content-Type: application/json";

        String resultado = strategy.mascarar(header);

        assertThat(resultado).isEqualTo(header);
    }
}
