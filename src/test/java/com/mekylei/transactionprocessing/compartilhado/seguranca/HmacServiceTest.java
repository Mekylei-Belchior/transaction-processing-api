package com.mekylei.transactionprocessing.compartilhado.seguranca;

import com.mekylei.transactionprocessing.configuracao.persistencia.HmacProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitários para HmacService.
 *
 * HmacService é responsável por gerar os blind indexes armazenados em
 * numeroContaHmac e agenciaHmac na tabela conta. Esses campos permitem
 * que ContaJpaAdapter execute buscas exatas por numero de conta sem
 * precisar descriptografar todas as linhas ou expor o valor em texto claro.
 *
 * Comportamentos críticos validados:
 *  - Retorno null para entradas nulas ou em branco (não gera índice inválido).
 *  - Normalização (trim + uppercase) garante que "0001", "  0001  " e "0001"
 *    maiúsculo produzam o mesmo HMAC, evitando duplicatas nos índices.
 *  - Determinismo: o mesmo valor sempre produz o mesmo HMAC para a mesma chave.
 *  - Isolamento de chaves: rotação de chave invalida todos os blind indexes
 *    anteriores (HMACs distintos por chave).
 */
@DisplayName("HmacService")
class HmacServiceTest {

    private static final String CHAVE = "chave-hmac-testes-unitarios-32b!";
    private static final String CHAVE_DIFERENTE = "outra-chave-rotacao-seguranca!!";

    private HmacService service;

    @BeforeEach
    void setUp() {
        service = new HmacService(new HmacProperties(CHAVE));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // gerar — proteção de campos sensíveis (numeroContaHmac, agenciaHmac)
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("gerar — blind index para busca segura")
    class Gerar {

        @Test
        @DisplayName("deve retornar null para valor null (sem índice inválido no banco)")
        void deve_retornar_null_para_valor_null() {
            assertThat(service.gerar(null)).isNull();
        }

        @ParameterizedTest(name = "[{index}] blank: \"{0}\"")
        @ValueSource(strings = {"", "   ", "\t", "\n"})
        @DisplayName("deve retornar null para valores em branco")
        void deve_retornar_null_para_valor_blank(String valorBlank) {
            assertThat(service.gerar(valorBlank)).isNull();
        }

        @Test
        @DisplayName("deve retornar hex lowercase de 64 caracteres (HMAC-SHA256 = 32 bytes)")
        void deve_retornar_hex_de_64_chars() {
            String resultado = service.gerar("0001-12345-6");

            assertThat(resultado)
                    .isNotNull()
                    .hasSize(64)
                    .matches("[0-9a-f]{64}");
        }

        @Test
        @DisplayName("deve ser determinístico: mesmo valor sempre produz o mesmo blind index")
        void deve_ser_deterministico() {
            String resultado1 = service.gerar("0001-12345-6");
            String resultado2 = service.gerar("0001-12345-6");

            assertThat(resultado1).isEqualTo(resultado2);
        }

        @Test
        @DisplayName("deve normalizar com trim+uppercase: variações de espaço e caixa geram o mesmo HMAC")
        void deve_normalizar_com_trim_e_uppercase() {
            String hmacMinusculo = service.gerar("conta-123");
            String hmacComEspacos = service.gerar("  CONTA-123  ");

            assertThat(hmacMinusculo).isEqualTo(hmacComEspacos);
        }

        @Test
        @DisplayName("deve gerar mesmo blind index para numeroConta com e sem espaços laterais")
        void deve_normalizar_numero_conta() {
            String hmacOriginal = service.gerar("0001-12345-6");
            String hmacComEspacos = service.gerar("  0001-12345-6  ");

            assertThat(hmacOriginal).isEqualTo(hmacComEspacos);
        }

        @Test
        @DisplayName("deve gerar mesmo blind index para agencia com e sem espaços laterais")
        void deve_normalizar_agencia() {
            String hmacOriginal = service.gerar("0001");
            String hmacComEspacos = service.gerar("  0001  ");

            assertThat(hmacOriginal).isEqualTo(hmacComEspacos);
        }

        @Test
        @DisplayName("valores distintos de numeroConta devem gerar HMACs distintos (sem colisão)")
        void valores_distintos_geram_hmacs_distintos() {
            String hmac1 = service.gerar("0001-12345-6");
            String hmac2 = service.gerar("0001-99999-0");

            assertThat(hmac1).isNotEqualTo(hmac2);
        }

        @Test
        @DisplayName("valores distintos de agencia devem gerar HMACs distintos (sem colisão)")
        void agencias_distintas_geram_hmacs_distintos() {
            String hmac1 = service.gerar("0001");
            String hmac2 = service.gerar("0002");

            assertThat(hmac1).isNotEqualTo(hmac2);
        }

        @Test
        @DisplayName("HMAC não deve expor o valor original (propriedade unidirecional)")
        void hmac_nao_deve_expor_valor_original() {
            String numeroConta = "0001-12345-6";
            String resultado = service.gerar(numeroConta);

            assertThat(resultado).doesNotContain(numeroConta);
        }

        @Test
        @DisplayName("rotação de chave deve produzir HMACs distintos (blind indexes incompatíveis entre versões)")
        void rotacao_de_chave_invalida_blind_indexes_anteriores() {
            HmacService serviceChaveNova = new HmacService(new HmacProperties(CHAVE_DIFERENTE));

            String hmacChaveAtual = service.gerar("0001-12345-6");
            String hmacChaveNova = serviceChaveNova.gerar("0001-12345-6");

            assertThat(hmacChaveAtual).isNotEqualTo(hmacChaveNova);
        }
    }
}
