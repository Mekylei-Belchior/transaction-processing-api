package com.mekylei.transactionprocessing.mensageria.outbox;

import com.mekylei.transactionprocessing.compartilhado.evento.EventoDominio;
import com.mekylei.transactionprocessing.mensageria.evento.TransacaoEventoRouter;
import com.mekylei.transactionprocessing.transacao.dominio.TipoEventoTransacao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.*;

/**
 * Testes unitários para {@link EventoOutboxPublicador}.
 *
 * <p>Objetivo:</p>
 * <ul>
 *     <li>Validar o comportamento esperado de {@link EventoOutboxPublicador} nos cenários exercitados pela suíte.</li>
 *     <li>Preservar regras de negócio, contratos, integrações ou invariantes aplicáveis à classe testada.</li>
 *     <li>Garantir regressão funcional para alterações futuras relacionadas a {@code EventoOutboxPublicador}.</li>
 * </ul>
 *
 * <p>Cenários cobertos:</p>
 * <ul>
 *     <li>Deve delegar persistência ao adapter com tópico e chave resolvidos.</li>
 *     <li>Deve chamar resolve tópico e resolve chave para cada evento.</li>
 *     <li>Deve propaga tópicos diferentes.</li>
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
@DisplayName("Evento Outbox Publicador")
class EventoOutboxPublicadorTest {

    private static final String TRANSACOES_INICIADAS = "transacoes.iniciadas";
    private static final String TRANSACOES_CONCLUIDAS = "transacoes.concluidas";
    private static final String TRANSACOES_FALHAS = "transacoes.falhas";

    @Mock
    private OutboxEventoRepository repository;

    @Mock
    private TransacaoEventoRouter router;

    @InjectMocks
    private DominioEventoOutboxPublicador publicador;

    @Test
    @DisplayName("deve delegar persistência ao adapter com tópico e chave resolvidos")
    void deveDelegarPersistenciaAoAdapterComTopicoEChaveResolvidos() {
        EventoDominio evento = mock(EventoDominio.class);
        String topicoEsperado = TRANSACOES_INICIADAS;
        String chaveEsperada = UUID.randomUUID().toString();

        when(evento.tipoEvento()).thenReturn(TipoEventoTransacao.TRANSACAO_INICIADA.tipoEvento());
        when(router.resolveTopico(TipoEventoTransacao.TRANSACAO_INICIADA)).thenReturn(topicoEsperado);
        when(router.resolveChave(evento)).thenReturn(chaveEsperada);

        publicador.publica(evento);

        verify(repository).salvar(evento, topicoEsperado, chaveEsperada);
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("deve chamar resolve tópico e resolve chave para cada evento")
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
    @DisplayName("deve propaga tópicos diferentes")
    void devePropagaTopicosDiferentes() {
        EventoDominio eventoFalha = mock(EventoDominio.class);
        String chave = UUID.randomUUID().toString();

        when(eventoFalha.tipoEvento()).thenReturn(TipoEventoTransacao.TRANSACAO_FALHOU.tipoEvento());
        when(router.resolveTopico(TipoEventoTransacao.TRANSACAO_FALHOU)).thenReturn(TRANSACOES_FALHAS);
        when(router.resolveChave(eventoFalha)).thenReturn(chave);

        publicador.publica(eventoFalha);

        verify(repository).salvar(eventoFalha, TRANSACOES_FALHAS, chave);
    }
}
