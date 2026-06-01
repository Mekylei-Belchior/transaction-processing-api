package com.mekylei.transactionprocessing.mensageria.evento;

import com.mekylei.transactionprocessing.compartilhado.evento.EventoDominio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TransacaoEventoRouterTest {

    private TransacaoEventoRouter router;

    @BeforeEach
    void setUp() {
        router = new TransacaoEventoRouter();
    }

    @Test
    void deveResolverTopicoParaTransacaoIniciada() {
        EventoDominio evento = eventoComTipo("TransacaoIniciada");
        assertThat(router.resolveTopico(evento)).isEqualTo(TopicosTransacao.TRANSACOES_INICIADAS);
    }

    @Test
    void deveResolverTopicoParaTransacaoConcluida() {
        EventoDominio evento = eventoComTipo("TransacaoConcluida");
        assertThat(router.resolveTopico(evento)).isEqualTo(TopicosTransacao.TRANSACOES_CONCLUIDAS);
    }

    @Test
    void deveResolverTopicoParaTransacaoFalhou() {
        EventoDominio evento = eventoComTipo("TransacaoFalhou");
        assertThat(router.resolveTopico(evento)).isEqualTo(TopicosTransacao.TRANSACOES_FALHAS);
    }

    @Test
    void deveResolverTopicoParaTransacaoEstornada() {
        EventoDominio evento = eventoComTipo("TransacaoEstornada");
        assertThat(router.resolveTopico(evento)).isEqualTo(TopicosTransacao.TRANSACOES_ESTORNADAS);
    }

    @Test
    void deveUsarIdAgregadoComoChave() {
        UUID idAgregado = UUID.randomUUID();
        EventoDominio evento = new EventoDominio() {
            @Override public UUID idEvento() { return UUID.randomUUID(); }
            @Override public UUID idAgregado() { return idAgregado; }
            @Override public UUID idCorrelacao() { return UUID.randomUUID(); }
            @Override public String tipoEvento() { return "TransacaoIniciada"; }
            @Override public String tipoAgregado() { return "Transacao"; }
            @Override public Instant ocorridoEm() { return Instant.now(); }
        };

        assertThat(router.resolveChave(evento)).isEqualTo(idAgregado.toString());
    }

    @Test
    void resolveChave_deveSerDeterministicoParaOMesmoEvento() {
        UUID idEvento = UUID.randomUUID();
        UUID idAgregado = UUID.randomUUID();

        EventoDominio evento = new EventoDominio() {
            @Override public UUID idEvento() { return idEvento; }
            @Override public UUID idAgregado() { return idAgregado; }
            @Override public UUID idCorrelacao() { return UUID.randomUUID(); }
            @Override public String tipoEvento() { return "TransacaoIniciada"; }
            @Override public String tipoAgregado() { return "Transacao"; }
            @Override public Instant ocorridoEm() { return Instant.now(); }
        };

        assertThat(router.resolveChave(evento))
                .isEqualTo(router.resolveChave(evento))
                .isEqualTo(idAgregado.toString());
    }

    private EventoDominio eventoComTipo(String tipo) {
        return new EventoDominio() {
            @Override public UUID idEvento() { return UUID.randomUUID(); }
            @Override public UUID idAgregado() { return UUID.randomUUID(); }
            @Override public UUID idCorrelacao() { return UUID.randomUUID(); }
            @Override public String tipoEvento() { return tipo; }
            @Override public String tipoAgregado() { return "Transacao"; }
            @Override public Instant ocorridoEm() { return Instant.now(); }
        };
    }
}