package com.mekylei.transactionprocessing.mensageria.consumidor;

import com.mekylei.transactionprocessing.mensageria.aplicacao.EventoProcessadoService;
import com.mekylei.transactionprocessing.mensageria.aplicacao.porta.EventoProcessadoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes unitários para {@link EventoProcessadoService}.
 *
 * <p>Objetivo:</p>
 * <ul>
 *     <li>Validar o comportamento esperado de {@link EventoProcessadoService} nos cenários exercitados pela suíte.</li>
 *     <li>Preservar regras de negócio, contratos, integrações ou invariantes aplicáveis à classe testada.</li>
 *     <li>Garantir regressão funcional para alterações futuras relacionadas a {@code EventoProcessadoService}.</li>
 * </ul>
 *
 * <p>Cenários cobertos:</p>
 * <ul>
 *     <li>Deve delegar registro de evento processado para a porta de aplicação.</li>
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
@DisplayName("Evento Processado Service")
class EventoProcessadoServiceTest {

    @Mock
    private EventoProcessadoRepository repository;

    @InjectMocks
    private EventoProcessadoService service;

    @Test
    @DisplayName("deve delegar registro de evento processado para a porta")
    void deveDelegarRegistroDeEventoProcessadoParaPorta() {
        UUID idEvento = UUID.randomUUID();
        UUID idCorrelacao = UUID.randomUUID();

        when(repository.registrarSeNaoProcessado(idEvento, idCorrelacao, "grupo-teste", "topico-teste"))
                .thenReturn(true);

        boolean resultado = service.registrarSeNaoProcessado(idEvento, idCorrelacao, "grupo-teste", "topico-teste");

        assertThat(resultado).isTrue();
        verify(repository).registrarSeNaoProcessado(idEvento, idCorrelacao, "grupo-teste", "topico-teste");
    }
}
