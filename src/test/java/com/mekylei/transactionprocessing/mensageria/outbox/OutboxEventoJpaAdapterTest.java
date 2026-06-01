package com.mekylei.transactionprocessing.mensageria.outbox;

import com.mekylei.transactionprocessing.compartilhado.evento.EventoDominio;
import com.mekylei.transactionprocessing.infraestrutura.entidade.OutboxEventoEntity;
import com.mekylei.transactionprocessing.infraestrutura.entidade.StatusOutboxEvento;
import com.mekylei.transactionprocessing.infraestrutura.persistencia.OutboxEventoJpaAdapter;
import com.mekylei.transactionprocessing.infraestrutura.repositorio.OutboxEventoJpaRepository;
import org.junit.jupiter.api.Test;
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

@ExtendWith(MockitoExtension.class)
class OutboxEventoJpaAdapterTest {

    @Mock
    private OutboxEventoJpaRepository repository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OutboxEventoJpaAdapter adapter;

    @Test
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
    void deveLancarExcecaoAoMarcarPublicadoEventoInexistente() {
        UUID idEvento = UUID.randomUUID();
        when(repository.findById(idEvento)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.marcarPublicado(idEvento))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(idEvento.toString());
    }

    @Test
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
    void deveLancarExcecaoAoMarcarFalhaDeEventoInexistente() {
        UUID idEvento = UUID.randomUUID();
        when(repository.findById(idEvento)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.marcarFalha(idEvento, new RuntimeException("erro"), Duration.ofSeconds(10)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(idEvento.toString());
    }

    @Test
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