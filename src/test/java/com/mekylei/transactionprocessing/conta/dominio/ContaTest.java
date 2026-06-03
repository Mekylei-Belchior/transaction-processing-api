package com.mekylei.transactionprocessing.conta.dominio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Conta")
class ContaTest {

    @Nested
    @DisplayName("Builder")
    class Builder {

        @Test
        @DisplayName("deve criar conta quando campos são fornecidos")
        void deve_criar_conta_quando_campos_fornecidos() {
            UUID id = UUID.randomUUID();
            UUID idCliente = UUID.randomUUID();
            Instant criadoEm = Instant.parse("2026-02-01T12:00:00Z");

            Conta conta = Conta.builder()
                    .id(id)
                    .numeroConta("123456")
                    .agencia("0001")
                    .idCliente(idCliente)
                    .tipo(TipoConta.CORRENTE)
                    .status(StatusConta.ATIVA)
                    .criadoEm(criadoEm)
                    .build();

            assertThat(conta.getId()).isEqualTo(id);
            assertThat(conta.getNumeroConta()).isEqualTo("123456");
            assertThat(conta.getAgencia()).isEqualTo("0001");
            assertThat(conta.getIdCliente()).isEqualTo(idCliente);
            assertThat(conta.getTipo()).isEqualTo(TipoConta.CORRENTE);
            assertThat(conta.getStatus()).isEqualTo(StatusConta.ATIVA);
            assertThat(conta.getCriadoEm()).isEqualTo(criadoEm);
        }
    }

    @Nested
    @DisplayName("estaAtiva")
    class EstaAtiva {

        @Test
        @DisplayName("deve retornar true quando status é ATIVA")
        void deve_retornar_true_quando_status_ativa() {
            Conta conta = contaComStatus(StatusConta.ATIVA);

            assertThat(conta.estaAtiva()).isTrue();
        }

        @Test
        @DisplayName("deve retornar false quando status é BLOQUEADA")
        void deve_retornar_false_quando_status_bloqueada() {
            Conta conta = contaComStatus(StatusConta.BLOQUEADA);

            assertThat(conta.estaAtiva()).isFalse();
        }

        @Test
        @DisplayName("deve retornar false quando status é ENCERRADA")
        void deve_retornar_false_quando_status_encerrada() {
            Conta conta = contaComStatus(StatusConta.ENCERRADA);

            assertThat(conta.estaAtiva()).isFalse();
        }
    }

    private static Conta contaComStatus(StatusConta status) {
        return Conta.builder()
                .numeroConta("123456")
                .agencia("0001")
                .idCliente(UUID.randomUUID())
                .tipo(TipoConta.PAGAMENTO)
                .status(status)
                .build();
    }
}
