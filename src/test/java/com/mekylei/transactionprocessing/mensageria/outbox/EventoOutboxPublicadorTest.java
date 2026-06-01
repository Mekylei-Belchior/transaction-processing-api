package com.mekylei.transactionprocessing.mensageria.outbox;

import com.mekylei.transactionprocessing.compartilhado.evento.EventoDominio;
import com.mekylei.transactionprocessing.infraestrutura.persistencia.OutboxEventoJpaAdapter;
import com.mekylei.transactionprocessing.mensageria.evento.TopicosTransacao;
import com.mekylei.transactionprocessing.mensageria.evento.TransacaoEventoRouter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventoOutboxPublicadorTest {

    @Mock
    private OutboxEventoJpaAdapter adapter;

    @Mock
    private TransacaoEventoRouter router;

    @InjectMocks
    private DominioEventoOutboxPublicador publicador;

    @Test
    void deveDelegarPersistenciaAoAdapterComTopicoEChaveResolvidos() {
        EventoDominio evento = mock(EventoDominio.class);
        String topicoEsperado = TopicosTransacao.TRANSACOES_INICIADAS;
        String chaveEsperada = UUID.randomUUID().toString();

        when(router.resolveTopico(evento)).thenReturn(topicoEsperado);
        when(router.resolveChave(evento)).thenReturn(chaveEsperada);

        publicador.publica(evento);

        verify(adapter).salvar(evento, topicoEsperado, chaveEsperada);
        verifyNoMoreInteractions(adapter);
    }

    @Test
    void deveChamarResolveTopicoEResolveChaveParaCadaEvento() {
        EventoDominio evento = mock(EventoDominio.class);
        when(router.resolveTopico(evento)).thenReturn(TopicosTransacao.TRANSACOES_CONCLUIDAS);
        when(router.resolveChave(evento)).thenReturn(UUID.randomUUID().toString());

        publicador.publica(evento);

        verify(router).resolveTopico(evento);
        verify(router).resolveChave(evento);
    }

    @Test
    void devePropagaTopicosDiferentes() {
        EventoDominio eventoFalha = mock(EventoDominio.class);
        String chave = UUID.randomUUID().toString();

        when(router.resolveTopico(eventoFalha)).thenReturn(TopicosTransacao.TRANSACOES_FALHAS);
        when(router.resolveChave(eventoFalha)).thenReturn(chave);

        publicador.publica(eventoFalha);

        verify(adapter).salvar(eventoFalha, TopicosTransacao.TRANSACOES_FALHAS, chave);
    }
}