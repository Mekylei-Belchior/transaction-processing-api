package com.mekylei.transactionprocessing.mensageria.outbox;

import com.mekylei.transactionprocessing.compartilhado.evento.EventoDominio;
import com.mekylei.transactionprocessing.infraestrutura.persistencia.OutboxEventoJpaAdapter;
import com.mekylei.transactionprocessing.mensageria.evento.TransacaoEventoRouter;
import com.mekylei.transactionprocessing.transacao.dominio.TipoEventoTransacao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventoOutboxPublicadorTest {

    private static final String TRANSACOES_INICIADAS = "transacoes.iniciadas";
    private static final String TRANSACOES_CONCLUIDAS = "transacoes.concluidas";
    private static final String TRANSACOES_FALHAS = "transacoes.falhas";

    @Mock
    private OutboxEventoJpaAdapter adapter;

    @Mock
    private TransacaoEventoRouter router;

    @InjectMocks
    private DominioEventoOutboxPublicador publicador;

    @Test
    void deveDelegarPersistenciaAoAdapterComTopicoEChaveResolvidos() {
        EventoDominio evento = mock(EventoDominio.class);
        String topicoEsperado = TRANSACOES_INICIADAS;
        String chaveEsperada = UUID.randomUUID().toString();

        when(evento.tipoEvento()).thenReturn(TipoEventoTransacao.TRANSACAO_INICIADA.tipoEvento());
        when(router.resolveTopico(TipoEventoTransacao.TRANSACAO_INICIADA)).thenReturn(topicoEsperado);
        when(router.resolveChave(evento)).thenReturn(chaveEsperada);

        publicador.publica(evento);

        verify(adapter).salvar(evento, topicoEsperado, chaveEsperada);
        verifyNoMoreInteractions(adapter);
    }

    @Test
    void deveChamarResolveTopicoEResolveChaveParaCadaEvento() {
        EventoDominio evento = mock(EventoDominio.class);
        when(evento.tipoEvento()).thenReturn(TipoEventoTransacao.TRANSACAO_CONCLUIDA.tipoEvento());
        when(router.resolveTopico(TipoEventoTransacao.TRANSACAO_CONCLUIDA)).thenReturn(TRANSACOES_CONCLUIDAS);
        when(router.resolveChave(evento)).thenReturn(UUID.randomUUID().toString());

        publicador.publica(evento);

        verify(router).resolveTopico(TipoEventoTransacao.TRANSACAO_CONCLUIDA);
        verify(router).resolveChave(evento);
    }

    @Test
    void devePropagaTopicosDiferentes() {
        EventoDominio eventoFalha = mock(EventoDominio.class);
        String chave = UUID.randomUUID().toString();

        when(eventoFalha.tipoEvento()).thenReturn(TipoEventoTransacao.TRANSACAO_FALHOU.tipoEvento());
        when(router.resolveTopico(TipoEventoTransacao.TRANSACAO_FALHOU)).thenReturn(TRANSACOES_FALHAS);
        when(router.resolveChave(eventoFalha)).thenReturn(chave);

        publicador.publica(eventoFalha);

        verify(adapter).salvar(eventoFalha, TRANSACOES_FALHAS, chave);
    }
}
