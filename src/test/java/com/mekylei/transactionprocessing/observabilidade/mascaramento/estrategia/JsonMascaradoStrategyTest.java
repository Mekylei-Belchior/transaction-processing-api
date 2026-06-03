package com.mekylei.transactionprocessing.observabilidade.mascaramento.estrategia;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JsonMascaradoStrategyTest {

    private JsonMascaradoStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new JsonMascaradoStrategy();
    }

    @Test
    void deveMascararCpfEmJsonBody() {
        String resultado = strategy.mascarar("{\"cpf\":\"123.456.789-09\"}");

        assertThat(resultado).isEqualTo("{\"cpf\":\"****\"}");
    }

    @Test
    void deveMascararNumeroContaEmJsonBody() {
        String resultado = strategy.mascarar("{\"conta\":\"0001-12345-6\"}");

        assertThat(resultado).isEqualTo("{\"conta\":\"****\"}");
    }

    @Test
    void naoDeveAlterarJsonSemCamposSensiveis() {
        String json = "{\"nome\":\"Maria\",\"status\":\"ATIVA\"}";

        String resultado = strategy.mascarar(json);

        assertThat(resultado).isEqualTo(json);
    }

    @Test
    void deveMascararMultiplosCamposSensiveis() {
        String resultado = strategy.mascarar(
                "{\"cpf\":\"123.456.789-09\",\"dados\":{\"token\":\"abc123\"},\"items\":[{\"agencia\":\"0001\"}]}");

        assertThat(resultado)
                .contains("\"cpf\":\"****\"")
                .contains("\"token\":\"****\"")
                .contains("\"agencia\":\"****\"");
    }
}
