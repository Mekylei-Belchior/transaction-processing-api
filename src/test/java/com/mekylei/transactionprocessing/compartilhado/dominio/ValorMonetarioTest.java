package com.mekylei.transactionprocessing.compartilhado.dominio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ValorMonetario")
class ValorMonetarioTest {

    @Nested
    @DisplayName("Criação")
    class Criacao {

        @Test
        @DisplayName("deve lançar IllegalArgumentException quando valor é null")
        void deve_lancar_illegal_argument_exception_quando_valor_null() {
            assertThatThrownBy(() -> new ValorMonetario(null, Currency.getInstance("BRL")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("valor monetário");
        }

        @Test
        @DisplayName("deve lançar IllegalArgumentException quando valor é zero")
        void deve_lancar_illegal_argument_exception_quando_valor_zero() {
            assertThatThrownBy(() -> new ValorMonetario(BigDecimal.ZERO, Currency.getInstance("BRL")))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("deve lançar IllegalArgumentException quando valor é negativo")
        void deve_lancar_illegal_argument_exception_quando_valor_negativo() {
            assertThatThrownBy(() -> new ValorMonetario(new BigDecimal("-0.01"), Currency.getInstance("BRL")))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("deve lançar IllegalArgumentException quando moeda é null")
        void deve_lancar_illegal_argument_exception_quando_moeda_null() {
            assertThatThrownBy(() -> new ValorMonetario(BigDecimal.TEN, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("moeda");
        }

        @Test
        @DisplayName("deve criar com escala de duas casas decimais quando valor positivo")
        void deve_criar_com_escala_de_duas_casas_decimais_quando_valor_positivo() {
            ValorMonetario valor = new ValorMonetario(new BigDecimal("10.005"), Currency.getInstance("BRL"));

            assertThat(valor.valor()).isEqualByComparingTo(new BigDecimal("10.01"));
            assertThat(valor.valor().scale()).isEqualTo(2);
            assertThat(valor.moeda()).isEqualTo(Currency.getInstance("BRL"));
        }
    }

    @Nested
    @DisplayName("Fábricas")
    class Fabricas {

        @Test
        @DisplayName("deve criar em real quando usar paraReal")
        void deve_criar_em_real_quando_usar_para_real() {
            ValorMonetario valor = ValorMonetario.paraReal(new BigDecimal("25.50"));

            assertThat(valor.valor()).isEqualByComparingTo(new BigDecimal("25.50"));
            assertThat(valor.moeda()).isEqualTo(Currency.getInstance("BRL"));
        }
    }
}
