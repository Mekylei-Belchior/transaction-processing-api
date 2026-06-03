package com.mekylei.transactionprocessing.compartilhado.seguranca;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes unitários para {@link HmacUtils}.
 *
 * <p>Objetivo:</p>
 * <ul>
 *     <li>Validar o comportamento esperado de {@link HmacUtils} nos cenários exercitados pela suíte.</li>
 *     <li>Preservar regras de negócio, contratos, integrações ou invariantes aplicáveis à classe testada.</li>
 *     <li>Garantir regressão funcional para alterações futuras relacionadas a {@code HmacUtils}.</li>
 * </ul>
 *
 * <p>Cenários cobertos:</p>
 * <ul>
 *     <li>Deve retornar hex lowercase de 64 caracteres (SHA-256 = 32 bytes = 64 hex).</li>
 *     <li>Deve ser determinístico: mesmas entradas sempre produzem o mesmo HMAC.</li>
 *     <li>Chave diferente deve produzir HMAC distinto para o mesmo valor.</li>
 *     <li>Valor diferente deve produzir HMAC distinto (efeito avalanche).</li>
 *     <li>Deve suportar string vazia como valor e retornar HMAC de 64 chars.</li>
 *     <li>HMAC não deve conter o valor original (propriedade unidirecional).</li>
 *     <li>Deve lançar IllegalStateException para valor null.</li>
 *     <li>Deve lançar IllegalStateException para chave null.</li>
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
@DisplayName("HmacUtils")
class HmacUtilsTest {

    private static final String CHAVE = "chave-hmac-testes-unitarios-32b!";

    // ─────────────────────────────────────────────────────────────────────────
    // gerarHmacSha256
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("gerarHmacSha256 — cálculo do blind index")
    class GerarHmacSha256 {

        @Test
        @DisplayName("deve retornar hex lowercase de 64 caracteres (SHA-256 = 32 bytes = 64 hex)")
        void deve_retornar_hex_lowercase_64_chars() {
            String resultado = HmacUtils.gerarHmacSha256("0001-12345-6", CHAVE);

            assertThat(resultado)
                    .isNotNull()
                    .hasSize(64)
                    .matches("[0-9a-f]{64}");
        }

        @Test
        @DisplayName("deve ser determinístico: mesmas entradas sempre produzem o mesmo HMAC")
        void deve_ser_deterministico() {
            String resultado1 = HmacUtils.gerarHmacSha256("agencia-001", CHAVE);
            String resultado2 = HmacUtils.gerarHmacSha256("agencia-001", CHAVE);

            assertThat(resultado1).isEqualTo(resultado2);
        }

        @Test
        @DisplayName("chave diferente deve produzir HMAC distinto para o mesmo valor")
        void chave_diferente_produz_hmac_distinto() {
            String resultado1 = HmacUtils.gerarHmacSha256("0001-12345-6", CHAVE);
            String resultado2 = HmacUtils.gerarHmacSha256("0001-12345-6", "outra-chave-completamente-dif!");

            assertThat(resultado1).isNotEqualTo(resultado2);
        }

        @Test
        @DisplayName("valor diferente deve produzir HMAC distinto (efeito avalanche)")
        void valor_diferente_produz_hmac_distinto() {
            String resultado1 = HmacUtils.gerarHmacSha256("0001-12345-6", CHAVE);
            String resultado2 = HmacUtils.gerarHmacSha256("0001-12345-7", CHAVE);

            assertThat(resultado1).isNotEqualTo(resultado2);
        }

        @Test
        @DisplayName("deve suportar string vazia como valor e retornar HMAC de 64 chars")
        void deve_suportar_string_vazia_como_valor() {
            String resultado = HmacUtils.gerarHmacSha256("", CHAVE);

            assertThat(resultado)
                    .isNotNull()
                    .hasSize(64)
                    .matches("[0-9a-f]{64}");
        }

        @Test
        @DisplayName("HMAC não deve conter o valor original (propriedade unidirecional)")
        void hmac_nao_deve_conter_valor_original() {
            String valor = "conta-poupanca-0001-12345-6";
            String resultado = HmacUtils.gerarHmacSha256(valor, CHAVE);

            assertThat(resultado).doesNotContain(valor);
        }

        @Test
        @DisplayName("deve lançar IllegalStateException para valor null")
        void deve_lancar_excecao_para_valor_null() {
            assertThatThrownBy(() -> HmacUtils.gerarHmacSha256(null, CHAVE))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("HMAC");
        }

        @Test
        @DisplayName("deve lançar IllegalStateException para chave null")
        void deve_lancar_excecao_para_chave_null() {
            assertThatThrownBy(() -> HmacUtils.gerarHmacSha256("valor", null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("HMAC");
        }
    }
}
