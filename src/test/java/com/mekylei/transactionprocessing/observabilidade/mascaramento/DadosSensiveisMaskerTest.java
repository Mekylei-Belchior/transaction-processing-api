package com.mekylei.transactionprocessing.observabilidade.mascaramento;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitários para {@link DadosSensiveisMasker}.
 *
 * <p>Objetivo:</p>
 * <ul>
 *     <li>Validar o comportamento esperado de {@link DadosSensiveisMasker} nos cenários exercitados pela suíte.</li>
 *     <li>Preservar regras de negócio, contratos, integrações ou invariantes aplicáveis à classe testada.</li>
 *     <li>Garantir regressão funcional para alterações futuras relacionadas a {@code DadosSensiveisMasker}.</li>
 * </ul>
 *
 * <p>Cenários cobertos:</p>
 * <ul>
 *     <li>Deve mascarar usando o resolver.</li>
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
@DisplayName("Dados Sensiveis Masker")
class DadosSensiveisMaskerTest {

    @Test
    @DisplayName("deve mascarar usando o resolver")
    void deveMascararUsandoOResolver() {
        String resultado = DadosSensiveisMasker.mascarar(
                "CPF 123.456.789-09 com numeroConta=123456-7 e Authorization: segredo");

        assertThat(resultado)
                .contains("123.***.***-09")
                .contains("numeroConta=****")
                .contains("Authorization: ****");
    }
}
