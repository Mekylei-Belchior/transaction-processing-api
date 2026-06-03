package com.mekylei.transactionprocessing.observabilidade.mascaramento.estrategia;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitários para {@link JsonMascaradoStrategy}.
 *
 * <p>Objetivo:</p>
 * <ul>
 *     <li>Validar o comportamento esperado de {@link JsonMascaradoStrategy} nos cenários exercitados pela suíte.</li>
 *     <li>Preservar regras de negócio, contratos, integrações ou invariantes aplicáveis à classe testada.</li>
 *     <li>Garantir regressão funcional para alterações futuras relacionadas a {@code JsonMascaradoStrategy}.</li>
 * </ul>
 *
 * <p>Cenários cobertos:</p>
 * <ul>
 *     <li>Deve mascarar CPF em JSON body.</li>
 *     <li>Deve mascarar número conta em JSON body.</li>
 *     <li>Não deve alterar JSON sem campos sensíveis.</li>
 *     <li>Deve mascarar múltiplos campos sensíveis.</li>
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
@DisplayName("Json Mascarado Strategy")
class JsonMascaradoStrategyTest {

    private JsonMascaradoStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new JsonMascaradoStrategy();
    }

    @Test
    @DisplayName("deve mascarar CPF em JSON body")
    void deveMascararCpfEmJsonBody() {
        String resultado = strategy.mascarar("{\"cpf\":\"123.456.789-09\"}");

        assertThat(resultado).isEqualTo("{\"cpf\":\"****\"}");
    }

    @Test
    @DisplayName("deve mascarar número conta em JSON body")
    void deveMascararNumeroContaEmJsonBody() {
        String resultado = strategy.mascarar("{\"conta\":\"0001-12345-6\"}");

        assertThat(resultado).isEqualTo("{\"conta\":\"****\"}");
    }

    @Test
    @DisplayName("não deve alterar JSON sem campos sensíveis")
    void naoDeveAlterarJsonSemCamposSensiveis() {
        String json = "{\"nome\":\"Maria\",\"status\":\"ATIVA\"}";

        String resultado = strategy.mascarar(json);

        assertThat(resultado).isEqualTo(json);
    }

    @Test
    @DisplayName("deve mascarar múltiplos campos sensíveis")
    void deveMascararMultiplosCamposSensiveis() {
        String resultado = strategy.mascarar(
                "{\"cpf\":\"123.456.789-09\",\"dados\":{\"token\":\"abc123\"},\"items\":[{\"agencia\":\"0001\"}]}");

        assertThat(resultado)
                .contains("\"cpf\":\"****\"")
                .contains("\"token\":\"****\"")
                .contains("\"agencia\":\"****\"");
    }
}
