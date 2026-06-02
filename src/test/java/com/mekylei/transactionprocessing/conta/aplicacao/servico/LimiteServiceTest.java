package com.mekylei.transactionprocessing.conta.aplicacao.servico;

import com.mekylei.transactionprocessing.compartilhado.exception.RegraNegocioException;
import com.mekylei.transactionprocessing.conta.aplicacao.porta.repositorio.LimiteRepository;
import com.mekylei.transactionprocessing.conta.dominio.LimiteTransacional;
import com.mekylei.transactionprocessing.transacao.dominio.TipoTransacao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LimiteServiceTest {

    @Mock
    private LimiteRepository limiteRepository;

    private LimiteService service;
    private UUID idConta;

    @BeforeEach
    void setUp() {
        service = new LimiteService(limiteRepository);
        idConta = UUID.randomUUID();
    }

    @Test
    void deveValidarLimiteSemLockPessimista() {
        LimiteTransacional limite = limiteComUtilizadoHoje("100.00");
        when(limiteRepository.findByIdContaAndTipo(idConta, TipoTransacao.PIX)).thenReturn(Optional.of(limite));

        service.validarLimite(idConta, TipoTransacao.PIX, new BigDecimal("50.00"));

        verify(limiteRepository).findByIdContaAndTipo(idConta, TipoTransacao.PIX);
        verify(limiteRepository, never()).findByIdContaAndTipoForUpdate(idConta, TipoTransacao.PIX);
        verify(limiteRepository, never()).save(limite);
    }

    @Test
    void deveFalharAoValidarLimiteNaoConfigurado() {
        when(limiteRepository.findByIdContaAndTipo(idConta, TipoTransacao.PIX)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.validarLimite(idConta, TipoTransacao.PIX, new BigDecimal("50.00")))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Limite transacional não configurado")
                .hasFieldOrPropertyWithValue("codigoErro", "LIMITE_NAO_CONFIGURADO");

        verify(limiteRepository).findByIdContaAndTipo(idConta, TipoTransacao.PIX);
        verify(limiteRepository, never()).findByIdContaAndTipoForUpdate(idConta, TipoTransacao.PIX);
    }

    @Test
    void deveFalharAoValidarLimitePorTransacaoExcedidoSemLockPessimista() {
        LimiteTransacional limite = limiteComUtilizadoHoje("100.00");
        when(limiteRepository.findByIdContaAndTipo(idConta, TipoTransacao.PIX)).thenReturn(Optional.of(limite));

        assertThatThrownBy(() -> service.validarLimite(idConta, TipoTransacao.PIX, new BigDecimal("501.00")))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("excede o limite por transação")
                .hasFieldOrPropertyWithValue("codigoErro", "LIMITE_POR_TRANSACAO_EXCEDIDO");

        verify(limiteRepository).findByIdContaAndTipo(idConta, TipoTransacao.PIX);
        verify(limiteRepository, never()).findByIdContaAndTipoForUpdate(idConta, TipoTransacao.PIX);
    }

    @Test
    void deveFalharAoValidarLimiteDiarioExcedidoSemLockPessimista() {
        LimiteTransacional limite = limiteComUtilizadoHoje("900.00");
        when(limiteRepository.findByIdContaAndTipo(idConta, TipoTransacao.PIX)).thenReturn(Optional.of(limite));

        assertThatThrownBy(() -> service.validarLimite(idConta, TipoTransacao.PIX, new BigDecimal("150.00")))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("excede o limite diário")
                .hasFieldOrPropertyWithValue("codigoErro", "LIMITE_DIARIO_EXCEDIDO");

        verify(limiteRepository).findByIdContaAndTipo(idConta, TipoTransacao.PIX);
        verify(limiteRepository, never()).findByIdContaAndTipoForUpdate(idConta, TipoTransacao.PIX);
    }

    @Test
    void deveDecrementarUtilizadoComLockPessimista() {
        LimiteTransacional limite = limiteComUtilizadoHoje("100.00");
        when(limiteRepository.findByIdContaAndTipoForUpdate(idConta, TipoTransacao.PIX)).thenReturn(Optional.of(limite));

        service.decrementarUtilizado(idConta, TipoTransacao.PIX, new BigDecimal("50.00"));

        ArgumentCaptor<LimiteTransacional> limiteCaptor = ArgumentCaptor.forClass(LimiteTransacional.class);
        verify(limiteRepository).findByIdContaAndTipoForUpdate(idConta, TipoTransacao.PIX);
        verify(limiteRepository, never()).findByIdContaAndTipo(idConta, TipoTransacao.PIX);
        verify(limiteRepository).save(limiteCaptor.capture());

        LimiteTransacional atualizado = limiteCaptor.getValue();
        assertThat(atualizado.getId()).isEqualTo(limite.getId());
        assertThat(atualizado.getIdConta()).isEqualTo(idConta);
        assertThat(atualizado.getTipo()).isEqualTo(TipoTransacao.PIX);
        assertThat(atualizado.getUtilizadoHoje()).isEqualByComparingTo(new BigDecimal("150.00"));
    }

    @Test
    void deveIgnorarDecrementoQuandoLimiteNaoConfigurado() {
        when(limiteRepository.findByIdContaAndTipoForUpdate(idConta, TipoTransacao.PIX)).thenReturn(Optional.empty());

        service.decrementarUtilizado(idConta, TipoTransacao.PIX, new BigDecimal("50.00"));

        verify(limiteRepository).findByIdContaAndTipoForUpdate(idConta, TipoTransacao.PIX);
        verify(limiteRepository, never()).findByIdContaAndTipo(idConta, TipoTransacao.PIX);
        verify(limiteRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deveFalharAoDecrementarQuandoLimiteDiarioExcedidoENaoSalvar() {
        LimiteTransacional limite = limiteComUtilizadoHoje("900.00");
        when(limiteRepository.findByIdContaAndTipoForUpdate(idConta, TipoTransacao.PIX)).thenReturn(Optional.of(limite));

        assertThatThrownBy(() -> service.decrementarUtilizado(idConta, TipoTransacao.PIX, new BigDecimal("150.00")))
                .isInstanceOf(RegraNegocioException.class)
                .hasFieldOrPropertyWithValue("codigoErro", "LIMITE_DIARIO_EXCEDIDO");

        verify(limiteRepository).findByIdContaAndTipoForUpdate(idConta, TipoTransacao.PIX);
        verify(limiteRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private LimiteTransacional limiteComUtilizadoHoje(String utilizadoHoje) {
        return LimiteTransacional.builder()
                .idConta(idConta)
                .tipo(TipoTransacao.PIX)
                .limiteDiario(new BigDecimal("1000.00"))
                .limiteTransacao(new BigDecimal("500.00"))
                .utilizadoHoje(new BigDecimal(utilizadoHoje))
                .versao(0L)
                .build();
    }

}
