package com.mekylei.transactionprocessing.mensageria.evento;

import com.mekylei.transactionprocessing.compartilhado.evento.EventoDominio;
import com.mekylei.transactionprocessing.configuracao.kafka.TopicosProperties;
import com.mekylei.transactionprocessing.transacao.dominio.TipoEventoTransacao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitários para {@link TransacaoEventoRouter}.
 *
 * <p>Objetivo:</p>
 * <ul>
 *     <li>Validar o comportamento esperado de {@link TransacaoEventoRouter} nos cenários exercitados pela suíte.</li>
 *     <li>Preservar regras de negócio, contratos, integrações ou invariantes aplicáveis à classe testada.</li>
 *     <li>Garantir regressão funcional para alterações futuras relacionadas a {@code TransacaoEventoRouter}.</li>
 * </ul>
 *
 * <p>Cenários cobertos:</p>
 * <ul>
 *     <li>Deve resolver tópico para transação iniciada.</li>
 *     <li>Deve resolver tópico para transação concluida.</li>
 *     <li>Deve resolver tópico para transação falhou.</li>
 *     <li>Deve resolver tópico para transação estornada.</li>
 *     <li>Deve resolver tópicos configurados.</li>
 *     <li>Deve usar ID agregado como chave.</li>
 *     <li>ResolveChave deve ser determinístico para o mesmo evento.</li>
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
@DisplayName("Transacao Evento Router")
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
    @DisplayName("deve resolver tópico para transação iniciada")
    void deveResolverTopicoParaTransacaoIniciada() {
        assertThat(router.resolveTopico(TipoEventoTransacao.TRANSACAO_INICIADA))
                .isEqualTo(TRANSACOES_INICIADAS);
    }

    @Test
    @DisplayName("deve resolver tópico para transação concluida")
    void deveResolverTopicoParaTransacaoConcluida() {
        assertThat(router.resolveTopico(TipoEventoTransacao.TRANSACAO_CONCLUIDA))
                .isEqualTo(TRANSACOES_CONCLUIDAS);
    }

    @Test
    @DisplayName("deve resolver tópico para transação falhou")
    void deveResolverTopicoParaTransacaoFalhou() {
        assertThat(router.resolveTopico(TipoEventoTransacao.TRANSACAO_FALHOU))
                .isEqualTo(TRANSACOES_FALHAS);
    }

    @Test
    @DisplayName("deve resolver tópico para transação estornada")
    void deveResolverTopicoParaTransacaoEstornada() {
        assertThat(router.resolveTopico(TipoEventoTransacao.TRANSACAO_ESTORNADA))
                .isEqualTo(TRANSACOES_ESTORNADAS);
    }

    @Test
    @DisplayName("deve resolver tópicos configurados")
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
    @DisplayName("deve usar ID agregado como chave")
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
    @DisplayName("resolveChave deve ser determinístico para o mesmo evento")
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
