package com.mekylei.transactionprocessing.mensageria.outbox;

import com.mekylei.transactionprocessing.configuracao.kafka.OutboxProperties;
import com.mekylei.transactionprocessing.infraestrutura.entidade.OutboxEventoEntity;
import com.mekylei.transactionprocessing.infraestrutura.entidade.StatusOutboxEvento;
import com.mekylei.transactionprocessing.infraestrutura.persistencia.OutboxEventoJpaAdapter;
import com.mekylei.transactionprocessing.mensageria.produtor.KafkaEventoProdutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para {@link EventoOutboxPublicadorScheduler}.
 *
 * <p>Objetivo:</p>
 * <ul>
 *     <li>Validar o comportamento esperado de {@link EventoOutboxPublicadorScheduler} nos cenários exercitados pela suíte.</li>
 *     <li>Preservar regras de negócio, contratos, integrações ou invariantes aplicáveis à classe testada.</li>
 *     <li>Garantir regressão funcional para alterações futuras relacionadas a {@code EventoOutboxPublicadorScheduler}.</li>
 * </ul>
 *
 * <p>Cenários cobertos:</p>
 * <ul>
 *     <li>Não deve interagir com produtor quando não há eventos pendentes.</li>
 *     <li>Deve publicar evento pendente e marcar como publicado.</li>
 *     <li>Deve marcar falha quando produtor lança exceção.</li>
 *     <li>Deve continuar processando próximos eventos após uma falha.</li>
 *     <li>Deve respeitar tamanho de lote na busca.</li>
 *     <li>Deve publicar múltiplos eventos no mesmo lote.</li>
 *     <li>Deve usar intervalo de reprocessamento correto na falha.</li>
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
@ExtendWith(MockitoExtension.class)
@DisplayName("Evento Outbox Publicador Scheduler")
class EventoOutboxPublicadorSchedulerTest {

    @Mock
    private OutboxEventoJpaAdapter eventoJpaAdapter;

    @Mock
    private KafkaEventoProdutor eventoProdutor;

    private OutboxProperties properties;
    private EventoOutboxPublicador publicador;

    @BeforeEach
    void setUp() {
        properties = new OutboxProperties(50, Duration.ofSeconds(30), 5000L);
        publicador = new EventoOutboxPublicador(eventoJpaAdapter, eventoProdutor, properties);
    }

    @Test
    @DisplayName("não deve interagir com produtor quando não há eventos pendentes")
    void naoDeveInteragirComProdutorQuandoNaoHaEventosPendentes() {
        when(eventoJpaAdapter.buscarParaPublicacao(properties.lotePublicacao())).thenReturn(List.of());

        publicador.publicarPendentes();

        verifyNoInteractions(eventoProdutor);
        verify(eventoJpaAdapter).buscarParaPublicacao(properties.lotePublicacao());
        verifyNoMoreInteractions(eventoJpaAdapter);
    }

    @Test
    @DisplayName("deve publicar evento pendente e marcar como publicado")
    void devePublicarEventoPendenteEMarcarComoPublicado() {
        UUID id = UUID.randomUUID();
        OutboxEventoEntity evento = criarEvento(id, "TransacaoIniciada");
        when(eventoJpaAdapter.buscarParaPublicacao(anyInt())).thenReturn(List.of(evento));

        publicador.publicarPendentes();

        verify(eventoProdutor).enviar(evento);
        verify(eventoJpaAdapter).marcarPublicado(id);
        verify(eventoJpaAdapter, never()).marcarFalha(any(), any(), any());
    }

    @Test
    @DisplayName("deve marcar falha quando produtor lança exceção")
    void deveMarcarFalhaQuandoProdutorLancaExcecao() {
        UUID id = UUID.randomUUID();
        OutboxEventoEntity evento = criarEvento(id, "TransacaoFalhou");
        when(eventoJpaAdapter.buscarParaPublicacao(anyInt())).thenReturn(List.of(evento));
        doThrow(new RuntimeException("Kafka indisponível")).when(eventoProdutor).enviar(evento);

        publicador.publicarPendentes();

        verify(eventoJpaAdapter, never()).marcarPublicado(any());
        verify(eventoJpaAdapter).marcarFalha(
                eq(id), any(RuntimeException.class), eq(properties.intervaloReprocessamento()));
    }

    @Test
    @DisplayName("deve continuar processando próximos eventos após uma falha")
    void deveContinuarProcessandoProximosEventosAposUmaFalha() {
        UUID idFalhou = UUID.randomUUID();
        UUID idSucesso = UUID.randomUUID();
        OutboxEventoEntity eventoFalhou = criarEvento(idFalhou, "TransacaoIniciada");
        OutboxEventoEntity eventoSucesso = criarEvento(idSucesso, "TransacaoConcluida");

        when(eventoJpaAdapter.buscarParaPublicacao(anyInt()))
                .thenReturn(List.of(eventoFalhou, eventoSucesso));
        doThrow(new RuntimeException("Kafka indisponível")).when(eventoProdutor).enviar(eventoFalhou);

        publicador.publicarPendentes();

        verify(eventoJpaAdapter).marcarFalha(eq(idFalhou), any(), any());
        verify(eventoJpaAdapter).marcarPublicado(idSucesso);
    }

    @Test
    @DisplayName("deve respeitar tamanho de lote na busca")
    void deveRespeitarTamanhoDeLoteNaBusca() {
        OutboxProperties propriedadesLote10 = new OutboxProperties(10, Duration.ofSeconds(30), 5000L);
        EventoOutboxPublicador publicadorLote = new EventoOutboxPublicador(
                eventoJpaAdapter, eventoProdutor, propriedadesLote10);
        when(eventoJpaAdapter.buscarParaPublicacao(10)).thenReturn(List.of());

        publicadorLote.publicarPendentes();

        verify(eventoJpaAdapter).buscarParaPublicacao(10);
    }

    @Test
    @DisplayName("deve publicar múltiplos eventos no mesmo lote")
    void devePublicarMultiplosEventosNoMesmoLote() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();
        OutboxEventoEntity e1 = criarEvento(id1, "TransacaoIniciada");
        OutboxEventoEntity e2 = criarEvento(id2, "TransacaoConcluida");
        OutboxEventoEntity e3 = criarEvento(id3, "TransacaoFalhou");

        when(eventoJpaAdapter.buscarParaPublicacao(anyInt())).thenReturn(List.of(e1, e2, e3));

        publicador.publicarPendentes();

        verify(eventoProdutor).enviar(e1);
        verify(eventoProdutor).enviar(e2);
        verify(eventoProdutor).enviar(e3);
        verify(eventoJpaAdapter).marcarPublicado(id1);
        verify(eventoJpaAdapter).marcarPublicado(id2);
        verify(eventoJpaAdapter).marcarPublicado(id3);
        verify(eventoJpaAdapter, never()).marcarFalha(any(), any(), any());
    }

    @Test
    @DisplayName("deve usar intervalo de reprocessamento correto na falha")
    void deveUsarIntervaloDeReprocessamentoCorretoNaFalha() {
        Duration intervaloEsperado = Duration.ofMinutes(2);
        OutboxProperties propriedadesCustom = new OutboxProperties(50, intervaloEsperado, 5000L);
        EventoOutboxPublicador publicadorCustom = new EventoOutboxPublicador(
                eventoJpaAdapter, eventoProdutor, propriedadesCustom);

        UUID id = UUID.randomUUID();
        OutboxEventoEntity evento = criarEvento(id, "TransacaoEstornada");
        when(eventoJpaAdapter.buscarParaPublicacao(anyInt())).thenReturn(List.of(evento));
        doThrow(new RuntimeException("erro")).when(eventoProdutor).enviar(evento);

        publicadorCustom.publicarPendentes();

        verify(eventoJpaAdapter).marcarFalha(eq(id), any(), eq(intervaloEsperado));
    }

    private OutboxEventoEntity criarEvento(UUID id, String tipoEvento) {
        OutboxEventoEntity evento = new OutboxEventoEntity();
        evento.setId(id);
        evento.setTipoEvento(tipoEvento);
        evento.setStatus(StatusOutboxEvento.PENDENTE);
        return evento;
    }
}
