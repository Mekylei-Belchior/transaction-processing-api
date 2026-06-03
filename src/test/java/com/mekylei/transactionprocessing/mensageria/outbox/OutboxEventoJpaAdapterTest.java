package com.mekylei.transactionprocessing.mensageria.outbox;

import com.mekylei.transactionprocessing.compartilhado.evento.EventoDominio;
import com.mekylei.transactionprocessing.infraestrutura.entidade.OutboxEventoEntity;
import com.mekylei.transactionprocessing.infraestrutura.entidade.StatusOutboxEvento;
import com.mekylei.transactionprocessing.infraestrutura.persistencia.OutboxEventoJpaAdapter;
import com.mekylei.transactionprocessing.infraestrutura.repositorio.OutboxEventoJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para {@link OutboxEventoJpaAdapter}.
 *
 * <p>Objetivo:</p>
 * <ul>
 *     <li>Validar o comportamento esperado de {@link OutboxEventoJpaAdapter} nos cenários exercitados pela suíte.</li>
 *     <li>Preservar regras de negócio, contratos, integrações ou invariantes aplicáveis à classe testada.</li>
 *     <li>Garantir regressão funcional para alterações futuras relacionadas a {@code OutboxEventoJpaAdapter}.</li>
 * </ul>
 *
 * <p>Cenários cobertos:</p>
 * <ul>
 *     <li>Deve persistir evento com status pendente.</li>
 *     <li>Deve marcar evento como publicado.</li>
 *     <li>Deve lançar exceção ao marcar publicado evento inexistente.</li>
 *     <li>Deve marcar falha incrementando tentativas.</li>
 *     <li>Deve lançar exceção ao marcar falha de evento inexistente.</li>
 *     <li>Deve usar nome da classe como erro quando mensagem nula.</li>
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
@DisplayName("Outbox Evento Jpa Adapter")
class OutboxEventoJpaAdapterTest {

    @Mock
    private OutboxEventoJpaRepository repository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OutboxEventoJpaAdapter adapter;

    @Test
    @DisplayName("deve persistir evento com status pendente")
    void devePersistirEventoComStatusPendente() {
        UUID idEvento = UUID.randomUUID();
        UUID idAgregado = UUID.randomUUID();
        UUID idCorrelacao = UUID.randomUUID();
        Instant ocorridoEm = Instant.now();
        String topico = "transacoes.iniciadas";
        String chave = idEvento.toString();

        EventoDominio evento = mock(EventoDominio.class);
        when(evento.idEvento()).thenReturn(idEvento);
        when(evento.tipoEvento()).thenReturn("TransacaoIniciada");
        when(evento.tipoAgregado()).thenReturn("Transacao");
        when(evento.idAgregado()).thenReturn(idAgregado);
        when(evento.idCorrelacao()).thenReturn(idCorrelacao);
        when(evento.ocorridoEm()).thenReturn(ocorridoEm);
        when(objectMapper.valueToTree(any())).thenReturn(new ObjectMapper().createObjectNode());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ArgumentCaptor<OutboxEventoEntity> captor = ArgumentCaptor.forClass(OutboxEventoEntity.class);

        adapter.salvar(evento, topico, chave);

        verify(repository).save(captor.capture());
        OutboxEventoEntity persistido = captor.getValue();

        assertThat(persistido.getStatus()).isEqualTo(StatusOutboxEvento.PENDENTE);
        assertThat(persistido.getTentativas()).isZero();
        assertThat(persistido.getTopico()).isEqualTo(topico);
        assertThat(persistido.getChave()).isEqualTo(chave);
        assertThat(persistido.getId()).isEqualTo(idEvento);
        assertThat(persistido.getTipoEvento()).isEqualTo("TransacaoIniciada");
        assertThat(persistido.getIdAgregado()).isEqualTo(idAgregado);
        assertThat(persistido.getIdCorrelacao()).isEqualTo(idCorrelacao);
        assertThat(persistido.getCriadoEm()).isNotNull();
        assertThat(persistido.getProximaTentativaEm()).isNotNull();
        assertThat(persistido.getPublicadoEm()).isNull();
        assertThat(persistido.getUltimoErro()).isNull();
    }

    @Test
    @DisplayName("deve marcar evento como publicado")
    void deveMarcarEventoComoPublicado() {
        UUID idEvento = UUID.randomUUID();
        OutboxEventoEntity entity = new OutboxEventoEntity();
        entity.setId(idEvento);
        entity.setStatus(StatusOutboxEvento.PENDENTE);
        entity.setTentativas(0);

        when(repository.findById(idEvento)).thenReturn(Optional.of(entity));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        adapter.marcarPublicado(idEvento);

        ArgumentCaptor<OutboxEventoEntity> captor = ArgumentCaptor.forClass(OutboxEventoEntity.class);
        verify(repository).save(captor.capture());

        assertThat(captor.getValue().getStatus()).isEqualTo(StatusOutboxEvento.PUBLICADO);
        assertThat(captor.getValue().getPublicadoEm()).isNotNull();
        assertThat(captor.getValue().getUltimoErro()).isNull();
    }

    @Test
    @DisplayName("deve lançar exceção ao marcar publicado evento inexistente")
    void deveLancarExcecaoAoMarcarPublicadoEventoInexistente() {
        UUID idEvento = UUID.randomUUID();
        when(repository.findById(idEvento)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.marcarPublicado(idEvento))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(idEvento.toString());
    }

    @Test
    @DisplayName("deve marcar falha incrementando tentativas")
    void deveMarcarFalhaIncrementandoTentativas() {
        UUID idEvento = UUID.randomUUID();
        OutboxEventoEntity entity = new OutboxEventoEntity();
        entity.setId(idEvento);
        entity.setStatus(StatusOutboxEvento.PENDENTE);
        entity.setTentativas(2);

        when(repository.findById(idEvento)).thenReturn(Optional.of(entity));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RuntimeException erro = new RuntimeException("Kafka indispon\u00edvel");
        Duration intervalo = Duration.ofSeconds(30);

        adapter.marcarFalha(idEvento, erro, intervalo);

        ArgumentCaptor<OutboxEventoEntity> captor = ArgumentCaptor.forClass(OutboxEventoEntity.class);
        verify(repository).save(captor.capture());

        OutboxEventoEntity salvo = captor.getValue();
        assertThat(salvo.getStatus()).isEqualTo(StatusOutboxEvento.FALHOU);
        assertThat(salvo.getTentativas()).isEqualTo(3);
        assertThat(salvo.getUltimoErro()).contains("Kafka indispon\u00edvel");
        assertThat(salvo.getProximaTentativaEm()).isAfterOrEqualTo(Instant.now().minusSeconds(1));
    }

    @Test
    @DisplayName("deve lançar exceção ao marcar falha de evento inexistente")
    void deveLancarExcecaoAoMarcarFalhaDeEventoInexistente() {
        UUID idEvento = UUID.randomUUID();
        when(repository.findById(idEvento)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.marcarFalha(idEvento, new RuntimeException("erro"), Duration.ofSeconds(10)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(idEvento.toString());
    }

    @Test
    @DisplayName("deve usar nome da classe como erro quando mensagem nula")
    void deveUsarNomeDaClasseComoErroQuandoMensagemNula() {
        UUID idEvento = UUID.randomUUID();
        OutboxEventoEntity entity = new OutboxEventoEntity();
        entity.setId(idEvento);
        entity.setStatus(StatusOutboxEvento.PENDENTE);
        entity.setTentativas(0);

        when(repository.findById(idEvento)).thenReturn(Optional.of(entity));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        adapter.marcarFalha(idEvento, new RuntimeException((String) null), Duration.ofSeconds(10));

        ArgumentCaptor<OutboxEventoEntity> captor = ArgumentCaptor.forClass(OutboxEventoEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getUltimoErro()).isEqualTo("RuntimeException");
    }
}