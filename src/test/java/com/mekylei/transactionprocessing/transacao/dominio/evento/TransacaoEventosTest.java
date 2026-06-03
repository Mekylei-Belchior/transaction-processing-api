package com.mekylei.transactionprocessing.transacao.dominio.evento;

import com.mekylei.transactionprocessing.compartilhado.dominio.ValorMonetario;
import com.mekylei.transactionprocessing.transacao.dominio.TipoEventoTransacao;
import com.mekylei.transactionprocessing.transacao.dominio.TipoTransacao;
import com.mekylei.transactionprocessing.transacao.dominio.Transacao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TransacaoEventos")
class TransacaoEventosTest {

    @Nested
    @DisplayName("TransacaoIniciadaEvento")
    class TransacaoIniciada {

        @Test
        @DisplayName("deve mapear campos corretamente quando criado de transação")
        void deve_mapear_campos_corretamente_quando_criado_de_transacao() {
            Transacao transacao = transacaoValida();

            TransacaoIniciadaEvento evento = TransacaoIniciadaEvento.de(transacao);

            assertThat(evento.idAgregado()).isEqualTo(transacao.getId());
            assertThat(evento.idCorrelacao()).isEqualTo(transacao.getIdCorrelacao());
            assertThat(evento.idIdempotencia()).isEqualTo(transacao.getIdIdempotencia());
            assertThat(evento.idContaOrigem()).isEqualTo(transacao.getIdContaOrigem());
            assertThat(evento.contaDestino()).isEqualTo(transacao.getContaDestino());
            assertThat(evento.tipo()).isEqualTo(transacao.getTipo());
            assertThat(evento.valor()).isEqualByComparingTo(transacao.getValor().valor());
            assertThat(evento.moeda()).isEqualTo("BRL");
            assertThat(evento.ocorridoEm()).isEqualTo(transacao.getCriadoEm());
        }

        @Test
        @DisplayName("deve retornar metadados corretos quando consultar evento")
        void deve_retornar_metadados_corretos_quando_consultar_evento() {
            Transacao transacao = transacaoValida();

            TransacaoIniciadaEvento evento = TransacaoIniciadaEvento.de(transacao);

            assertThat(evento.tipoEvento()).isEqualTo(TipoEventoTransacao.TRANSACAO_INICIADA.tipoEvento());
            assertThat(evento.tipoAgregado()).isEqualTo("Transacao");
            assertThat(evento.idEvento()).isNotNull().isNotEqualTo(transacao.getId());
        }
    }

    @Nested
    @DisplayName("TransacaoConcluidaEvento")
    class TransacaoConcluida {

        @Test
        @DisplayName("deve mapear campos corretamente quando criado de transação")
        void deve_mapear_campos_corretamente_quando_criado_de_transacao() {
            Transacao transacao = transacaoValida();

            TransacaoConcluidaEvento evento = TransacaoConcluidaEvento.de(transacao);

            assertThat(evento.idAgregado()).isEqualTo(transacao.getId());
            assertThat(evento.idCorrelacao()).isEqualTo(transacao.getIdCorrelacao());
            assertThat(evento.idIdempotencia()).isEqualTo(transacao.getIdIdempotencia());
            assertThat(evento.idContaOrigem()).isEqualTo(transacao.getIdContaOrigem());
            assertThat(evento.tipo()).isEqualTo(transacao.getTipo());
            assertThat(evento.valor()).isEqualByComparingTo(transacao.getValor().valor());
            assertThat(evento.moeda()).isEqualTo("BRL");
            assertThat(evento.ocorridoEm()).isEqualTo(transacao.getCriadoEm());
        }

        @Test
        @DisplayName("deve retornar metadados corretos quando consultar evento")
        void deve_retornar_metadados_corretos_quando_consultar_evento() {
            Transacao transacao = transacaoValida();

            TransacaoConcluidaEvento evento = TransacaoConcluidaEvento.de(transacao);

            assertThat(evento.tipoEvento()).isEqualTo(TipoEventoTransacao.TRANSACAO_CONCLUIDA.tipoEvento());
            assertThat(evento.tipoAgregado()).isEqualTo("Transacao");
            assertThat(evento.idEvento()).isNotNull().isNotEqualTo(transacao.getId());
        }
    }

    @Nested
    @DisplayName("TransacaoFalhouEvento")
    class TransacaoFalhou {

        @Test
        @DisplayName("deve mapear campos corretamente quando criado de transação")
        void deve_mapear_campos_corretamente_quando_criado_de_transacao() {
            Transacao transacao = transacaoValida();

            TransacaoFalhouEvento evento = TransacaoFalhouEvento.de(transacao, "Saldo insuficiente");

            assertThat(evento.idAgregado()).isEqualTo(transacao.getId());
            assertThat(evento.idCorrelacao()).isEqualTo(transacao.getIdCorrelacao());
            assertThat(evento.idIdempotencia()).isEqualTo(transacao.getIdIdempotencia());
            assertThat(evento.idContaOrigem()).isEqualTo(transacao.getIdContaOrigem());
            assertThat(evento.tipo()).isEqualTo(transacao.getTipo());
            assertThat(evento.valor()).isEqualByComparingTo(transacao.getValor().valor());
            assertThat(evento.moeda()).isEqualTo("BRL");
            assertThat(evento.motivo()).isEqualTo("Saldo insuficiente");
            assertThat(evento.ocorridoEm()).isEqualTo(transacao.getCriadoEm());
        }

        @Test
        @DisplayName("deve retornar metadados corretos quando consultar evento")
        void deve_retornar_metadados_corretos_quando_consultar_evento() {
            Transacao transacao = transacaoValida();

            TransacaoFalhouEvento evento = TransacaoFalhouEvento.de(transacao, "Saldo insuficiente");

            assertThat(evento.tipoEvento()).isEqualTo(TipoEventoTransacao.TRANSACAO_FALHOU.tipoEvento());
            assertThat(evento.tipoAgregado()).isEqualTo("Transacao");
            assertThat(evento.idEvento()).isNotNull().isNotEqualTo(transacao.getId());
        }
    }

    @Nested
    @DisplayName("TransacaoEstornadaEvento")
    class TransacaoEstornada {

        @Test
        @DisplayName("deve mapear campos corretamente quando criado de transação")
        void deve_mapear_campos_corretamente_quando_criado_de_transacao() {
            Transacao transacao = transacaoValida();

            TransacaoEstornadaEvento evento = TransacaoEstornadaEvento.de(transacao, "Solicitação do cliente");

            assertThat(evento.idAgregado()).isEqualTo(transacao.getId());
            assertThat(evento.idCorrelacao()).isEqualTo(transacao.getIdCorrelacao());
            assertThat(evento.idIdempotencia()).isEqualTo(transacao.getIdIdempotencia());
            assertThat(evento.idContaOrigem()).isEqualTo(transacao.getIdContaOrigem());
            assertThat(evento.tipo()).isEqualTo(transacao.getTipo());
            assertThat(evento.valor()).isEqualByComparingTo(transacao.getValor().valor());
            assertThat(evento.moeda()).isEqualTo("BRL");
            assertThat(evento.motivo()).isEqualTo("Solicitação do cliente");
            assertThat(evento.ocorridoEm()).isEqualTo(transacao.getCriadoEm());
        }

        @Test
        @DisplayName("deve retornar metadados corretos quando consultar evento")
        void deve_retornar_metadados_corretos_quando_consultar_evento() {
            Transacao transacao = transacaoValida();

            TransacaoEstornadaEvento evento = TransacaoEstornadaEvento.de(transacao, "Solicitação do cliente");

            assertThat(evento.tipoEvento()).isEqualTo(TipoEventoTransacao.TRANSACAO_ESTORNADA.tipoEvento());
            assertThat(evento.tipoAgregado()).isEqualTo("Transacao");
            assertThat(evento.idEvento()).isNotNull().isNotEqualTo(transacao.getId());
        }
    }

    private static Transacao transacaoValida() {
        return Transacao.builder()
                .id(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .idCorrelacao(UUID.fromString("22222222-2222-2222-2222-222222222222"))
                .idIdempotencia(UUID.fromString("33333333-3333-3333-3333-333333333333"))
                .valor(ValorMonetario.paraReal(new BigDecimal("75.55")))
                .tipo(TipoTransacao.PIX)
                .criadoEm(Instant.parse("2026-04-05T14:30:00Z"))
                .idContaOrigem(UUID.fromString("44444444-4444-4444-4444-444444444444"))
                .contaDestino("0001-123456")
                .build();
    }
}
