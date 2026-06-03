package com.mekylei.transactionprocessing.conta.dominio;

import com.mekylei.transactionprocessing.compartilhado.dominio.ValorMonetario;
import com.mekylei.transactionprocessing.compartilhado.exception.RegraNegocioException;
import com.mekylei.transactionprocessing.transacao.dominio.TipoTransacao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes unitários para {@link LimiteTransacional}.
 *
 * <p>Objetivo:</p>
 * <ul>
 *     <li>Validar o comportamento esperado de {@link LimiteTransacional} nos cenários exercitados pela suíte.</li>
 *     <li>Preservar regras de negócio, contratos, integrações ou invariantes aplicáveis à classe testada.</li>
 *     <li>Garantir regressão funcional para alterações futuras relacionadas a {@code LimiteTransacional}.</li>
 * </ul>
 *
 * <p>Cenários cobertos:</p>
 * <ul>
 *     <li>Deve não lançar exceção quando valor está dentro dos limites.</li>
 *     <li>Deve lançar RegraNegocioException quando valor acima do limite por transação.</li>
 *     <li>Deve lançar RegraNegocioException quando utilizado hoje mais valor acima do limite diário.</li>
 *     <li>Deve retornar nova instância com utilizadoHoje incrementado quando valor válido.</li>
 *     <li>Deve lançar RegraNegocioException quando validar é violado.</li>
 *     <li>Deve manter original imutável quando decrementar.</li>
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
@DisplayName("LimiteTransacional")
class LimiteTransacionalTest {

    @Nested
    @DisplayName("validar")
    class Validar {

        @Test
        @DisplayName("deve não lançar exceção quando valor está dentro dos limites")
        void deve_nao_lancar_excecao_quando_valor_dentro_dos_limites() {
            LimiteTransacional limite = limiteComUtilizadoHoje(new BigDecimal("100.00"));

            assertThatNoException()
                    .isThrownBy(() -> limite.validar(ValorMonetario.paraReal(new BigDecimal("200.00"))));
        }

        @Test
        @DisplayName("deve lançar RegraNegocioException quando valor acima do limite por transação")
        void deve_lancar_regra_negocio_exception_quando_valor_acima_do_limite_por_transacao() {
            LimiteTransacional limite = limiteComUtilizadoHoje(BigDecimal.ZERO);

            assertThatThrownBy(() -> limite.validar(ValorMonetario.paraReal(new BigDecimal("500.01"))))
                    .isInstanceOf(RegraNegocioException.class)
                    .extracting("codigoErro")
                    .isEqualTo("LIMITE_POR_TRANSACAO_EXCEDIDO");
        }

        @Test
        @DisplayName("deve lançar RegraNegocioException quando utilizado hoje mais valor acima do limite diário")
        void deve_lancar_regra_negocio_exception_quando_utilizado_hoje_mais_valor_acima_do_limite_diario() {
            LimiteTransacional limite = limiteComUtilizadoHoje(new BigDecimal("900.00"));

            assertThatThrownBy(() -> limite.validar(ValorMonetario.paraReal(new BigDecimal("100.01"))))
                    .isInstanceOf(RegraNegocioException.class)
                    .extracting("codigoErro")
                    .isEqualTo("LIMITE_DIARIO_EXCEDIDO");
        }
    }

    @Nested
    @DisplayName("decrementar")
    class Decrementar {

        @Test
        @DisplayName("deve retornar nova instância com utilizadoHoje incrementado quando valor válido")
        void deve_retornar_nova_instancia_com_utilizado_hoje_incrementado_quando_valor_valido() {
            LimiteTransacional limite = limiteComUtilizadoHoje(new BigDecimal("100.00"));

            LimiteTransacional decrementado = limite.decrementar(ValorMonetario.paraReal(new BigDecimal("250.00")));

            assertThat(decrementado).isNotSameAs(limite);
            assertThat(decrementado.getUtilizadoHoje()).isEqualByComparingTo(new BigDecimal("350.00"));
        }

        @Test
        @DisplayName("deve lançar RegraNegocioException quando validar é violado")
        void deve_lancar_regra_negocio_exception_quando_validar_violado() {
            LimiteTransacional limite = limiteComUtilizadoHoje(new BigDecimal("900.00"));

            assertThatThrownBy(() -> limite.decrementar(ValorMonetario.paraReal(new BigDecimal("100.01"))))
                    .isInstanceOf(RegraNegocioException.class)
                    .extracting("codigoErro")
                    .isEqualTo("LIMITE_DIARIO_EXCEDIDO");
        }

        @Test
        @DisplayName("deve manter original imutável quando decrementar")
        void deve_manter_original_imutavel_quando_decrementar() {
            LimiteTransacional limite = limiteComUtilizadoHoje(new BigDecimal("100.00"));

            LimiteTransacional decrementado = limite.decrementar(ValorMonetario.paraReal(new BigDecimal("250.00")));

            assertThat(decrementado).isNotSameAs(limite);
            assertThat(limite.getUtilizadoHoje()).isEqualByComparingTo(new BigDecimal("100.00"));
            assertThat(decrementado.getUtilizadoHoje()).isEqualByComparingTo(new BigDecimal("350.00"));
        }
    }

    private static LimiteTransacional limiteComUtilizadoHoje(BigDecimal utilizadoHoje) {
        return LimiteTransacional.builder()
                .id(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .idConta(UUID.fromString("22222222-2222-2222-2222-222222222222"))
                .tipo(TipoTransacao.PIX)
                .limiteDiario(new BigDecimal("1000.00"))
                .limiteTransacao(new BigDecimal("500.00"))
                .utilizadoHoje(utilizadoHoje)
                .dataReferencia(LocalDate.of(2026, 3, 1))
                .versao(1L)
                .build();
    }
}
