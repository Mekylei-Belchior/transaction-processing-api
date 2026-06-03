package com.mekylei.transactionprocessing.conta.aplicacao.servico;

import com.mekylei.transactionprocessing.compartilhado.exception.RegraNegocioException;
import com.mekylei.transactionprocessing.conta.aplicacao.porta.repositorio.LimiteRepository;
import com.mekylei.transactionprocessing.conta.dominio.LimiteTransacional;
import com.mekylei.transactionprocessing.transacao.dominio.TipoTransacao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
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

/**
 * Testes unitários para {@link LimiteService}.
 *
 * <p>Objetivo:</p>
 * <ul>
 *     <li>Validar o comportamento esperado de {@link LimiteService} nos cenários exercitados pela suíte.</li>
 *     <li>Preservar regras de negócio, contratos, integrações ou invariantes aplicáveis à classe testada.</li>
 *     <li>Garantir regressão funcional para alterações futuras relacionadas a {@code LimiteService}.</li>
 * </ul>
 *
 * <p>Cenários cobertos:</p>
 * <ul>
 *     <li>Deve validar limite sem lock pessimista.</li>
 *     <li>Deve falhar ao validar limite não configurado.</li>
 *     <li>Deve falhar ao validar limite por transação excedido sem lock pessimista.</li>
 *     <li>Deve falhar ao validar limite diário excedido sem lock pessimista.</li>
 *     <li>Deve decrementar utilizado com lock pessimista.</li>
 *     <li>Deve ignorar decremento quando limite não configurado.</li>
 *     <li>Deve falhar ao decrementar quando limite diário excedido e não salvar.</li>
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
@DisplayName("Limite Service")
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
    @DisplayName("deve validar limite sem lock pessimista")
    void deveValidarLimiteSemLockPessimista() {
        LimiteTransacional limite = limiteComUtilizadoHoje("100.00");
        when(limiteRepository.findByIdContaAndTipo(idConta, TipoTransacao.PIX)).thenReturn(Optional.of(limite));

        service.validarLimite(idConta, TipoTransacao.PIX, new BigDecimal("50.00"));

        verify(limiteRepository).findByIdContaAndTipo(idConta, TipoTransacao.PIX);
        verify(limiteRepository, never()).findByIdContaAndTipoForUpdate(idConta, TipoTransacao.PIX);
        verify(limiteRepository, never()).save(limite);
    }

    @Test
    @DisplayName("deve falhar ao validar limite não configurado")
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
    @DisplayName("deve falhar ao validar limite por transação excedido sem lock pessimista")
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
    @DisplayName("deve falhar ao validar limite diário excedido sem lock pessimista")
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
    @DisplayName("deve decrementar utilizado com lock pessimista")
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
    @DisplayName("deve ignorar decremento quando limite não configurado")
    void deveIgnorarDecrementoQuandoLimiteNaoConfigurado() {
        when(limiteRepository.findByIdContaAndTipoForUpdate(idConta, TipoTransacao.PIX)).thenReturn(Optional.empty());

        service.decrementarUtilizado(idConta, TipoTransacao.PIX, new BigDecimal("50.00"));

        verify(limiteRepository).findByIdContaAndTipoForUpdate(idConta, TipoTransacao.PIX);
        verify(limiteRepository, never()).findByIdContaAndTipo(idConta, TipoTransacao.PIX);
        verify(limiteRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("deve falhar ao decrementar quando limite diário excedido e não salvar")
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
