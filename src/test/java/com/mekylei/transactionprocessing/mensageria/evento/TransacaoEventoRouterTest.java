package com.mekylei.transactionprocessing.mensageria.evento;

import com.mekylei.transactionprocessing.compartilhado.evento.EventoDominio;
import com.mekylei.transactionprocessing.configuracao.kafka.TopicosProperties;
import com.mekylei.transactionprocessing.transacao.dominio.TipoEventoTransacao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TransacaoEventoRouterTest {

    private static final String TRANSACOES_INICIADAS = "transacoes.iniciadas";
    private static final String TRANSACOES_CONCLUIDAS = "transacoes.concluidas";
    private static final String TRANSACOES_ESTORNADAS = "transacoes.estornadas";
    private static final String TRANSACOES_FALHAS = "transacoes.falhas";

    private TransacaoEventoRouter router;

    @BeforeEach
    void setUp() {
        router = new TransacaoEventoRouter(new TopicosProperties(null, null, null, null));
    }

    @Test
    void deveResolverTopicoParaTransacaoIniciada() {
        assertThat(router.resolveTopico(TipoEventoTransacao.TRANSACAO_INICIADA))
                .isEqualTo(TRANSACOES_INICIADAS);
    }

    @Test
    void deveResolverTopicoParaTransacaoConcluida() {
        assertThat(router.resolveTopico(TipoEventoTransacao.TRANSACAO_CONCLUIDA))
                .isEqualTo(TRANSACOES_CONCLUIDAS);
    }

    @Test
    void deveResolverTopicoParaTransacaoFalhou() {
        assertThat(router.resolveTopico(TipoEventoTransacao.TRANSACAO_FALHOU))
                .isEqualTo(TRANSACOES_FALHAS);
    }

    @Test
    void deveResolverTopicoParaTransacaoEstornada() {
        assertThat(router.resolveTopico(TipoEventoTransacao.TRANSACAO_ESTORNADA))
                .isEqualTo(TRANSACOES_ESTORNADAS);
    }

    @Test
    void deveResolverTopicosConfigurados() {
        TransacaoEventoRouter routerConfigurado = new TransacaoEventoRouter(new TopicosProperties(
                "topico.iniciada",
                "topico.concluida",
                "topico.estornada",
                "topico.falhou"
        ));

        assertThat(routerConfigurado.resolveTopico(TipoEventoTransacao.TRANSACAO_INICIADA))
                .isEqualTo("topico.iniciada");
        assertThat(routerConfigurado.resolveTopico(TipoEventoTransacao.TRANSACAO_CONCLUIDA))
                .isEqualTo("topico.concluida");
        assertThat(routerConfigurado.resolveTopico(TipoEventoTransacao.TRANSACAO_ESTORNADA))
                .isEqualTo("topico.estornada");
        assertThat(routerConfigurado.resolveTopico(TipoEventoTransacao.TRANSACAO_FALHOU))
                .isEqualTo("topico.falhou");
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

}
