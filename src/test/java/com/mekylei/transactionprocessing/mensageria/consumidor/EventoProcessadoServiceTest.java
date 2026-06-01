package com.mekylei.transactionprocessing.mensageria.consumidor;

import com.mekylei.transactionprocessing.infraestrutura.entidade.EventoProcessadoEntity;
import com.mekylei.transactionprocessing.infraestrutura.repositorio.EventoProcessadoJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventoProcessadoServiceTest {

    @Mock
    private EventoProcessadoJpaRepository repository;

    @InjectMocks
    private EventoProcessadoService service;

    private UUID idEvento;
    private UUID idCorrelacao;

    @BeforeEach
    void setUp() {
        idEvento = UUID.randomUUID();
        idCorrelacao = UUID.randomUUID();
    }

    @Test
    void deveRetornarTrueParaEventoNovo() {
        when(repository.existsByIdEventoAndGrupoConsumidor(idEvento, "grupo-teste")).thenReturn(false);

        boolean resultado = service.registrarSeNaoProcessado(idEvento, idCorrelacao, "grupo-teste", "topico-teste");

        assertThat(resultado).isTrue();
        verify(repository).saveAndFlush(any());
    }

    @Test
    void deveRetornarFalseParaEventoJaRegistrado() {
        when(repository.existsByIdEventoAndGrupoConsumidor(idEvento, "grupo-teste")).thenReturn(true);

        boolean resultado = service.registrarSeNaoProcessado(idEvento, idCorrelacao, "grupo-teste", "topico-teste");

        assertThat(resultado).isFalse();
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void deveRetornarFalseEmRaceConditionDeInsert() {
        when(repository.existsByIdEventoAndGrupoConsumidor(idEvento, "grupo-teste")).thenReturn(false);
        when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("constraint violation"));

        boolean resultado = service.registrarSeNaoProcessado(idEvento, idCorrelacao, "grupo-teste", "topico-teste");

        assertThat(resultado).isFalse();
    }

    @Test
    void devePersistirGruposConsumidoresIndependentes() {
        when(repository.existsByIdEventoAndGrupoConsumidor(idEvento, "grupo-a")).thenReturn(false);
        when(repository.existsByIdEventoAndGrupoConsumidor(idEvento, "grupo-b")).thenReturn(false);

        boolean resultadoA = service.registrarSeNaoProcessado(idEvento, idCorrelacao, "grupo-a", "topico-teste");
        boolean resultadoB = service.registrarSeNaoProcessado(idEvento, idCorrelacao, "grupo-b", "topico-teste");

        assertThat(resultadoA).isTrue();
        assertThat(resultadoB).isTrue();
        verify(repository, times(2)).saveAndFlush(any());
    }

    @Test
    void devePersistirCamposCorretosDaEntidade() {
        when(repository.existsByIdEventoAndGrupoConsumidor(idEvento, "grupo-teste")).thenReturn(false);

        ArgumentCaptor<EventoProcessadoEntity> captor = ArgumentCaptor.forClass(EventoProcessadoEntity.class);

        service.registrarSeNaoProcessado(idEvento, idCorrelacao, "grupo-teste", "topico-teste");

        verify(repository).saveAndFlush(captor.capture());
        EventoProcessadoEntity persistido = captor.getValue();
        assertThat(persistido.getIdEvento()).isEqualTo(idEvento);
        assertThat(persistido.getIdCorrelacao()).isEqualTo(idCorrelacao);
        assertThat(persistido.getGrupoConsumidor()).isEqualTo("grupo-teste");
        assertThat(persistido.getTopico()).isEqualTo("topico-teste");
        assertThat(persistido.getProcessadoEm()).isNotNull();
    }

    @Test
    void devePropagarsExcecaoNaoEsperadaDeSaveAndFlush() {
        when(repository.existsByIdEventoAndGrupoConsumidor(idEvento, "grupo-teste")).thenReturn(false);
        when(repository.saveAndFlush(any())).thenThrow(new RuntimeException("erro de infra inesperado"));

        assertThatThrownBy(() -> service.registrarSeNaoProcessado(idEvento, idCorrelacao, "grupo-teste", "topico-teste"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("erro de infra inesperado");
    }
}