package com.mekylei.transactionprocessing.conta.dominio;

import com.mekylei.transactionprocessing.compartilhado.dominio.ValorMonetario;
import com.mekylei.transactionprocessing.compartilhado.exception.SaldoInsuficienteException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes unitários para {@link Saldo}.
 *
 * <p>Objetivo:</p>
 * <ul>
 *     <li>Validar o comportamento esperado de {@link Saldo} nos cenários exercitados pela suíte.</li>
 *     <li>Preservar regras de negócio, contratos, integrações ou invariantes aplicáveis à classe testada.</li>
 *     <li>Garantir regressão funcional para alterações futuras relacionadas a {@code Saldo}.</li>
 * </ul>
 *
 * <p>Cenários cobertos:</p>
 * <ul>
 *     <li>Deve retornar nova instância com disponível reduzido quando valor menor que disponível.</li>
 *     <li>Deve retornar nova instância com disponível zero quando valor igual ao disponível.</li>
 *     <li>Deve lançar SaldoInsuficienteException quando valor maior que disponível.</li>
 *     <li>Deve manter original imutável quando debitar.</li>
 *     <li>Deve retornar nova instância com disponível incrementado quando creditar.</li>
 *     <li>Deve manter original imutável quando creditar.</li>
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
@DisplayName("Saldo")
class SaldoTest {

    @Nested
    @DisplayName("debitar")
    class Debitar {

        @Test
        @DisplayName("deve retornar nova instância com disponível reduzido quando valor menor que disponível")
        void deve_retornar_nova_instancia_com_disponivel_reduzido_quando_valor_menor_que_disponivel() {
            Saldo saldo = saldoComDisponivel(new BigDecimal("100.00"));

            Saldo debitado = saldo.debitar(ValorMonetario.paraReal(new BigDecimal("40.00")));

            assertThat(debitado).isNotSameAs(saldo);
            assertThat(debitado.getDisponivel()).isEqualByComparingTo(new BigDecimal("60.00"));
        }

        @Test
        @DisplayName("deve retornar nova instância com disponível zero quando valor igual ao disponível")
        void deve_retornar_nova_instancia_com_disponivel_zero_quando_valor_igual_ao_disponivel() {
            Saldo saldo = saldoComDisponivel(new BigDecimal("100.00"));

            Saldo debitado = saldo.debitar(ValorMonetario.paraReal(new BigDecimal("100.00")));

            assertThat(debitado).isNotSameAs(saldo);
            assertThat(debitado.getDisponivel()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("deve lançar SaldoInsuficienteException quando valor maior que disponível")
        void deve_lancar_saldo_insuficiente_exception_quando_valor_maior_que_disponivel() {
            Saldo saldo = saldoComDisponivel(new BigDecimal("100.00"));

            assertThatThrownBy(() -> saldo.debitar(ValorMonetario.paraReal(new BigDecimal("100.01"))))
                    .isInstanceOf(SaldoInsuficienteException.class)
                    .extracting("codigoErro")
                    .isEqualTo("SALDO_INSUFICIENTE");
        }

        @Test
        @DisplayName("deve manter original imutável quando debitar")
        void deve_manter_original_imutavel_quando_debitar() {
            Saldo saldo = saldoComDisponivel(new BigDecimal("100.00"));

            Saldo debitado = saldo.debitar(ValorMonetario.paraReal(new BigDecimal("25.00")));

            assertThat(debitado).isNotSameAs(saldo);
            assertThat(saldo.getDisponivel()).isEqualByComparingTo(new BigDecimal("100.00"));
            assertThat(debitado.getDisponivel()).isEqualByComparingTo(new BigDecimal("75.00"));
        }
    }

    @Nested
    @DisplayName("creditar")
    class Creditar {

        @Test
        @DisplayName("deve retornar nova instância com disponível incrementado quando creditar")
        void deve_retornar_nova_instancia_com_disponivel_incrementado_quando_creditar() {
            Saldo saldo = saldoComDisponivel(new BigDecimal("100.00"));

            Saldo creditado = saldo.creditar(ValorMonetario.paraReal(new BigDecimal("30.00")));

            assertThat(creditado).isNotSameAs(saldo);
            assertThat(creditado.getDisponivel()).isEqualByComparingTo(new BigDecimal("130.00"));
        }

        @Test
        @DisplayName("deve manter original imutável quando creditar")
        void deve_manter_original_imutavel_quando_creditar() {
            Saldo saldo = saldoComDisponivel(new BigDecimal("100.00"));

            Saldo creditado = saldo.creditar(ValorMonetario.paraReal(new BigDecimal("30.00")));

            assertThat(creditado).isNotSameAs(saldo);
            assertThat(saldo.getDisponivel()).isEqualByComparingTo(new BigDecimal("100.00"));
            assertThat(creditado.getDisponivel()).isEqualByComparingTo(new BigDecimal("130.00"));
        }
    }

    private static Saldo saldoComDisponivel(BigDecimal disponivel) {
        return Saldo.builder()
                .id(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .idConta(UUID.fromString("22222222-2222-2222-2222-222222222222"))
                .disponivel(disponivel)
                .bloqueado(new BigDecimal("10.00"))
                .versao(1L)
                .atualizadoEm(Instant.parse("2026-03-01T08:00:00Z"))
                .build();
    }
}
