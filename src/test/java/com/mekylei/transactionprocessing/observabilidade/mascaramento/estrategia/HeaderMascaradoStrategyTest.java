package com.mekylei.transactionprocessing.observabilidade.mascaramento.estrategia;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitários para {@link HeaderMascaradoStrategy}.
 *
 * <p>Objetivo:</p>
 * <ul>
 *     <li>Validar o comportamento esperado de {@link HeaderMascaradoStrategy} nos cenários exercitados pela suíte.</li>
 *     <li>Preservar regras de negócio, contratos, integrações ou invariantes aplicáveis à classe testada.</li>
 *     <li>Garantir regressão funcional para alterações futuras relacionadas a {@code HeaderMascaradoStrategy}.</li>
 * </ul>
 *
 * <p>Cenários cobertos:</p>
 * <ul>
 *     <li>Deve mascarar header authorization.</li>
 *     <li>Deve mascarar headers definidos como sensíveis.</li>
 *     <li>Não deve alterar headers não sensíveis.</li>
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
@DisplayName("Header Mascarado Strategy")
class HeaderMascaradoStrategyTest {

    private HeaderMascaradoStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new HeaderMascaradoStrategy();
    }

    @Test
    @DisplayName("deve mascarar header authorization")
    void deveMascararHeaderAuthorization() {
        String resultado = strategy.mascarar("Authorization: segredo");

        assertThat(resultado).isEqualTo("Authorization: ****");
    }

    @Test
    @DisplayName("deve mascarar headers definidos como sensíveis")
    void deveMascararHeadersDefinidosComoSensiveis() {
        String resultado = strategy.mascarar("Bearer abc.def-123");

        assertThat(resultado).isEqualTo("Bearer ****");
    }

    @Test
    @DisplayName("não deve alterar headers não sensíveis")
    void naoDeveAlterarHeadersNaoSensiveis() {
        String header = "Content-Type: application/json";

        String resultado = strategy.mascarar(header);

        assertThat(resultado).isEqualTo(header);
    }
}
