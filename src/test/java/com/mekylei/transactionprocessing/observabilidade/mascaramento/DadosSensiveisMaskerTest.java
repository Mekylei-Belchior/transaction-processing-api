package com.mekylei.transactionprocessing.observabilidade.mascaramento;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DadosSensiveisMaskerTest {

    @Test
    void deveMascararUsandoOResolver() {
        String resultado = DadosSensiveisMasker.mascarar(
                "CPF 123.456.789-09 com numeroConta=123456-7 e Authorization: segredo");

        assertThat(resultado)
                .contains("123.***.***-09")
                .contains("numeroConta=****")
                .contains("Authorization: ****");
    }
}
