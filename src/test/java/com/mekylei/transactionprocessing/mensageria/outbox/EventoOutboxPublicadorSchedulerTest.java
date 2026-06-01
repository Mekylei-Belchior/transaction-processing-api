package com.mekylei.transactionprocessing.mensageria.outbox;

import com.mekylei.transactionprocessing.configuracao.kafka.OutboxProperties;
import com.mekylei.transactionprocessing.infraestrutura.entidade.OutboxEventoEntity;
import com.mekylei.transactionprocessing.infraestrutura.entidade.StatusOutboxEvento;
import com.mekylei.transactionprocessing.infraestrutura.persistencia.OutboxEventoJpaAdapter;
import com.mekylei.transactionprocessing.mensageria.produtor.KafkaEventoProdutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
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
    void naoDeveInteragirComProdutorQuandoNaoHaEventosPendentes() {
        when(eventoJpaAdapter.buscarParaPublicacao(properties.lotePublicacao())).thenReturn(List.of());

        publicador.publicarPendentes();

        verifyNoInteractions(eventoProdutor);
        verify(eventoJpaAdapter).buscarParaPublicacao(properties.lotePublicacao());
        verifyNoMoreInteractions(eventoJpaAdapter);
    }

    @Test
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
    void deveRespeitarTamanhoDeLoteNaBusca() {
        OutboxProperties propriedadesLote10 = new OutboxProperties(10, Duration.ofSeconds(30), 5000L);
        EventoOutboxPublicador publicadorLote = new EventoOutboxPublicador(
                eventoJpaAdapter, eventoProdutor, propriedadesLote10);
        when(eventoJpaAdapter.buscarParaPublicacao(10)).thenReturn(List.of());

        publicadorLote.publicarPendentes();

        verify(eventoJpaAdapter).buscarParaPublicacao(10);
    }

    @Test
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
