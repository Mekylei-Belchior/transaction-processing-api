package com.mekylei.transactionprocessing.observabilidade.mascaramento.estrategia;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitários para {@link MensagemMascaradaStrategy}.
 *
 * <p>Objetivo:</p>
 * <ul>
 *     <li>Validar o comportamento esperado de {@link MensagemMascaradaStrategy} nos cenários exercitados pela suíte.</li>
 *     <li>Preservar regras de negócio, contratos, integrações ou invariantes aplicáveis à classe testada.</li>
 *     <li>Garantir regressão funcional para alterações futuras relacionadas a {@code MensagemMascaradaStrategy}.</li>
 * </ul>
 *
 * <p>Cenários cobertos:</p>
 * <ul>
 *     <li>Deve mascarar CPF em mensagem livre.</li>
 *     <li>Deve mascarar conta em mensagem livre.</li>
 *     <li>Não deve alterar mensagem sem dados sensíveis.</li>
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
@DisplayName("Mensagem Mascarada Strategy")
class MensagemMascaradaStrategyTest {

    private MensagemMascaradaStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new MensagemMascaradaStrategy();
    }

    @Test
    @DisplayName("deve mascarar CPF em mensagem livre")
    void deveMascararCpfEmMensagemLivre() {
        String resultado = strategy.mascarar("CPF: 123.456.789-09 transferiu valor=100.00");

        assertThat(resultado).contains("123.***.***-09");
    }

    @Test
    @DisplayName("deve mascarar conta em mensagem livre")
    void deveMascararContaEmMensagemLivre() {
        String resultado = strategy.mascarar("numeroConta=123456-7 realizou transferencia");

        assertThat(resultado).isEqualTo("numeroConta=**** realizou transferencia");
    }

    @Test
    @DisplayName("não deve alterar mensagem sem dados sensíveis")
    void naoDeveAlterarMensagemSemDadosSensiveis() {
        String mensagem = "Transacao concluida com sucesso";

        String resultado = strategy.mascarar(mensagem);

        assertThat(resultado).isEqualTo(mensagem);
    }
}
