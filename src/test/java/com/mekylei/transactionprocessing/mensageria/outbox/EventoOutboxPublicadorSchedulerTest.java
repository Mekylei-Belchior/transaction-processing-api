package com.mekylei.transactionprocessing.mensageria.outbox;

import com.mekylei.transactionprocessing.configuracao.kafka.OutboxProperties;
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
    private OutboxEventoRepository eventoRepository;

    @Mock
    private KafkaEventoProdutor eventoProdutor;

    private OutboxProperties properties;
    private EventoOutboxPublicador publicador;

    @BeforeEach
    void setUp() {
        properties = new OutboxProperties(50, Duration.ofSeconds(30), 5000L);
        publicador = new EventoOutboxPublicador(eventoRepository, eventoProdutor, properties);
    }

    @Test
    @DisplayName("não deve interagir com produtor quando não há eventos pendentes")
    void naoDeveInteragirComProdutorQuandoNaoHaEventosPendentes() {
        when(eventoRepository.buscarParaPublicacao(properties.lotePublicacao())).thenReturn(List.of());

        publicador.publicarPendentes();

        verifyNoInteractions(eventoProdutor);
        verify(eventoRepository).buscarParaPublicacao(properties.lotePublicacao());
        verifyNoMoreInteractions(eventoRepository);
    }

    @Test
    @DisplayName("deve publicar evento pendente e marcar como publicado")
    void devePublicarEventoPendenteEMarcarComoPublicado() {
        UUID id = UUID.randomUUID();
        OutboxEvento evento = criarEvento(id, "TransacaoIniciada");
        when(eventoRepository.buscarParaPublicacao(anyInt())).thenReturn(List.of(evento));

        publicador.publicarPendentes();

        verify(eventoProdutor).enviar(evento);
        verify(eventoRepository).marcarPublicado(id);
        verify(eventoRepository, never()).marcarFalha(any(), any(), any());
    }

    @Test
    @DisplayName("deve marcar falha quando produtor lança exceção")
    void deveMarcarFalhaQuandoProdutorLancaExcecao() {
        UUID id = UUID.randomUUID();
        OutboxEvento evento = criarEvento(id, "TransacaoFalhou");
        when(eventoRepository.buscarParaPublicacao(anyInt())).thenReturn(List.of(evento));
        doThrow(new RuntimeException("Kafka indisponível")).when(eventoProdutor).enviar(evento);

        publicador.publicarPendentes();

        verify(eventoRepository, never()).marcarPublicado(any());
        verify(eventoRepository).marcarFalha(
                eq(id), any(RuntimeException.class), eq(properties.intervaloReprocessamento()));
    }

    @Test
    @DisplayName("deve continuar processando próximos eventos após uma falha")
    void deveContinuarProcessandoProximosEventosAposUmaFalha() {
        UUID idFalhou = UUID.randomUUID();
        UUID idSucesso = UUID.randomUUID();
        OutboxEvento eventoFalhou = criarEvento(idFalhou, "TransacaoIniciada");
        OutboxEvento eventoSucesso = criarEvento(idSucesso, "TransacaoConcluida");

        when(eventoRepository.buscarParaPublicacao(anyInt()))
                .thenReturn(List.of(eventoFalhou, eventoSucesso));
        doThrow(new RuntimeException("Kafka indisponível")).when(eventoProdutor).enviar(eventoFalhou);

        publicador.publicarPendentes();

        verify(eventoRepository).marcarFalha(eq(idFalhou), any(), any());
        verify(eventoRepository).marcarPublicado(idSucesso);
    }

    @Test
    @DisplayName("deve respeitar tamanho de lote na busca")
    void deveRespeitarTamanhoDeLoteNaBusca() {
        OutboxProperties propriedadesLote10 = new OutboxProperties(10, Duration.ofSeconds(30), 5000L);
        EventoOutboxPublicador publicadorLote = new EventoOutboxPublicador(
                eventoRepository, eventoProdutor, propriedadesLote10);
        when(eventoRepository.buscarParaPublicacao(10)).thenReturn(List.of());

        publicadorLote.publicarPendentes();

        verify(eventoRepository).buscarParaPublicacao(10);
    }

    @Test
    @DisplayName("deve publicar múltiplos eventos no mesmo lote")
    void devePublicarMultiplosEventosNoMesmoLote() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();
        OutboxEvento e1 = criarEvento(id1, "TransacaoIniciada");
        OutboxEvento e2 = criarEvento(id2, "TransacaoConcluida");
        OutboxEvento e3 = criarEvento(id3, "TransacaoFalhou");

        when(eventoRepository.buscarParaPublicacao(anyInt())).thenReturn(List.of(e1, e2, e3));

        publicador.publicarPendentes();

        verify(eventoProdutor).enviar(e1);
        verify(eventoProdutor).enviar(e2);
        verify(eventoProdutor).enviar(e3);
        verify(eventoRepository).marcarPublicado(id1);
        verify(eventoRepository).marcarPublicado(id2);
        verify(eventoRepository).marcarPublicado(id3);
        verify(eventoRepository, never()).marcarFalha(any(), any(), any());
    }

    @Test
    @DisplayName("deve usar intervalo de reprocessamento correto na falha")
    void deveUsarIntervaloDeReprocessamentoCorretoNaFalha() {
        Duration intervaloEsperado = Duration.ofMinutes(2);
        OutboxProperties propriedadesCustom = new OutboxProperties(50, intervaloEsperado, 5000L);
        EventoOutboxPublicador publicadorCustom = new EventoOutboxPublicador(
                eventoRepository, eventoProdutor, propriedadesCustom);

        UUID id = UUID.randomUUID();
        OutboxEvento evento = criarEvento(id, "TransacaoEstornada");
        when(eventoRepository.buscarParaPublicacao(anyInt())).thenReturn(List.of(evento));
        doThrow(new RuntimeException("erro")).when(eventoProdutor).enviar(evento);

        publicadorCustom.publicarPendentes();

        verify(eventoRepository).marcarFalha(eq(id), any(), eq(intervaloEsperado));
    }

    private OutboxEvento criarEvento(UUID id, String tipoEvento) {
        return new OutboxEvento(id, tipoEvento, "transacoes.outbox", id.toString(), null, 0);
    }
}
