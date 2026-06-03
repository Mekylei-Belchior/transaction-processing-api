package com.mekylei.transactionprocessing.observabilidade.mascaramento.estrategia;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MensagemMascaradaStrategyTest {

    private MensagemMascaradaStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new MensagemMascaradaStrategy();
    }

    @Test
    void deveMascararCpfEmMensagemLivre() {
        String resultado = strategy.mascarar("CPF: 123.456.789-09 transferiu valor=100.00");

        assertThat(resultado).contains("123.***.***-09");
    }

    @Test
    void deveMascararContaEmMensagemLivre() {
        String resultado = strategy.mascarar("numeroConta=123456-7 realizou transferencia");

        assertThat(resultado).isEqualTo("numeroConta=**** realizou transferencia");
    }

    @Test
    void naoDeveAlterarMensagemSemDadosSensiveis() {
        String mensagem = "Transacao concluida com sucesso";

        String resultado = strategy.mascarar(mensagem);

        assertThat(resultado).isEqualTo(mensagem);
    }
}
