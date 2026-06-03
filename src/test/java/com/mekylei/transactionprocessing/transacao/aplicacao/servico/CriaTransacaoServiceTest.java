package com.mekylei.transactionprocessing.transacao.aplicacao.servico;

import com.mekylei.transactionprocessing.compartilhado.dominio.ValorMonetario;
import com.mekylei.transactionprocessing.compartilhado.evento.EventoDominio;
import com.mekylei.transactionprocessing.transacao.aplicacao.porta.evento.EventoPublicador;
import com.mekylei.transactionprocessing.transacao.aplicacao.porta.repositorio.TransacaoRepository;
import com.mekylei.transactionprocessing.transacao.dominio.StatusTransacao;
import com.mekylei.transactionprocessing.transacao.dominio.TipoTransacao;
import com.mekylei.transactionprocessing.transacao.dominio.Transacao;
import com.mekylei.transactionprocessing.transacao.dominio.evento.TransacaoIniciadaEvento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes unitários para {@link CriaTransacaoService}.
 *
 * <p>Objetivo:</p>
 * <ul>
 *     <li>Validar o comportamento esperado de {@link CriaTransacaoService} nos cenários exercitados pela suíte.</li>
 *     <li>Preservar regras de negócio, contratos, integrações ou invariantes aplicáveis à classe testada.</li>
 *     <li>Garantir regressão funcional para alterações futuras relacionadas a {@code CriaTransacaoService}.</li>
 * </ul>
 *
 * <p>Cenários cobertos:</p>
 * <ul>
 *     <li>Deve salvar transação na repository e retornar transação salva.</li>
 *     <li>Deve publicar evento transação iniciada após persistir.</li>
 *     <li>Deve criar transação com todos os campos preenchidos.</li>
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
@DisplayName("Cria Transacao Service")
class CriaTransacaoServiceTest {

    @Mock
    private TransacaoRepository repository;

    @Mock
    private EventoPublicador eventoPublicador;

    private CriaTransacaoService service;

    @BeforeEach
    void setUp() {
        service = new CriaTransacaoService(repository, eventoPublicador);
    }

    @Test
    @DisplayName("deve salvar transação na repository e retornar transação salva")
    void deveSalvarTransacaoNaRepositoryERetornarTransacaoSalva() {
        BigDecimal valor = new BigDecimal("100.00");
        UUID idContaOrigem = UUID.randomUUID();
        UUID idIdempotencia = UUID.randomUUID();
        Transacao transacaoSalva = transacao(valor, TipoTransacao.PIX, idContaOrigem, "email@email.com", idIdempotencia);

        when(repository.save(any(Transacao.class))).thenReturn(transacaoSalva);

        Transacao resultado = service.cria(valor, TipoTransacao.PIX, idContaOrigem, "email@email.com", idIdempotencia);

        ArgumentCaptor<Transacao> transacaoCaptor = ArgumentCaptor.forClass(Transacao.class);
        verify(repository).save(transacaoCaptor.capture());
        assertThat(transacaoCaptor.getValue().getStatus()).isEqualTo(StatusTransacao.PENDENTE);
        assertThat(resultado).isSameAs(transacaoSalva);
    }

    @Test
    @DisplayName("deve publicar evento transação iniciada após persistir")
    void devePublicarEventoTransacaoIniciadaAposPersistir() {
        BigDecimal valor = new BigDecimal("100.00");
        UUID idContaOrigem = UUID.randomUUID();
        UUID idIdempotencia = UUID.randomUUID();
        Transacao transacaoSalva = transacao(valor, TipoTransacao.TED, idContaOrigem, "123456", idIdempotencia);

        when(repository.save(any(Transacao.class))).thenReturn(transacaoSalva);

        service.cria(valor, TipoTransacao.TED, idContaOrigem, "123456", idIdempotencia);

        ArgumentCaptor<EventoDominio> eventoCaptor = ArgumentCaptor.forClass(EventoDominio.class);
        verify(eventoPublicador).publica(eventoCaptor.capture());
        assertThat(eventoCaptor.getValue()).isInstanceOf(TransacaoIniciadaEvento.class);
        assertThat(eventoCaptor.getValue().idAgregado()).isEqualTo(transacaoSalva.getId());
    }

    @Test
    @DisplayName("deve criar transação com todos os campos preenchidos")
    void deveCriarTransacaoComTodosOsCamposPreenchidos() {
        BigDecimal valor = new BigDecimal("100.00");
        UUID idContaOrigem = UUID.randomUUID();
        UUID idIdempotencia = UUID.randomUUID();
        String contaDestino = "email@email.com";
        Transacao transacaoSalva = transacao(valor, TipoTransacao.PIX, idContaOrigem, contaDestino, idIdempotencia);

        when(repository.save(any(Transacao.class))).thenReturn(transacaoSalva);

        service.cria(valor, TipoTransacao.PIX, idContaOrigem, contaDestino, idIdempotencia);

        ArgumentCaptor<Transacao> transacaoCaptor = ArgumentCaptor.forClass(Transacao.class);
        verify(repository).save(transacaoCaptor.capture());
        Transacao persistida = transacaoCaptor.getValue();
        assertThat(persistida.getValor().valor()).isEqualByComparingTo(valor);
        assertThat(persistida.getTipo()).isEqualTo(TipoTransacao.PIX);
        assertThat(persistida.getIdContaOrigem()).isEqualTo(idContaOrigem);
        assertThat(persistida.getContaDestino()).isEqualTo(contaDestino);
        assertThat(persistida.getIdIdempotencia()).isEqualTo(idIdempotencia);
    }

    private Transacao transacao(BigDecimal valor, TipoTransacao tipo, UUID idContaOrigem, String contaDestino,
                                UUID idIdempotencia) {
        return Transacao.builder()
                .valor(ValorMonetario.paraReal(valor))
                .tipo(tipo)
                .idContaOrigem(idContaOrigem)
                .contaDestino(contaDestino)
                .idIdempotencia(idIdempotencia)
                .build();
    }
}
