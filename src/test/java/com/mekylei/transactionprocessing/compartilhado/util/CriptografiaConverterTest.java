package com.mekylei.transactionprocessing.compartilhado.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes unitários para CriptografiaConverter.
 *
 * Objetivo: validar toda a lógica de criptografia AES-256-GCM sem contexto Spring.
 * Cada cenário é construído diretamente via construtor, garantindo isolamento total
 * e execução rápida sem overhead de infraestrutura.
 *
 * Por que AES/GCM exige esses cenários?
 *  - IV randômico (nonce): reutilizar IV com a mesma chave em GCM rompe a segurança
 *    catastroficamente; o teste garante que cada criptografia gera IV diferente.
 *  - Authentication tag: GCM fornece criptografia autenticada (AEAD); qualquer
 *    adulteração no ciphertext ou no IV causa falha na verificação da tag — proteção
 *    crítica contra ataques de bit-flipping em dados bancários.
 *  - Isolamento de chaves: dados cifrados com uma chave não podem ser decifrados por
 *    outra; fundamental para rotação segura de chaves em produção.
 */
@DisplayName("CriptografiaConverter")
class CriptografiaConverterTest {

    static final String CHAVE_VALIDA_32_BYTES = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    static final String CHAVE_DIFERENTE_32_BYTES = "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY=";

    static final String CHAVE_INVALIDA_16_BYTES = "MDEyMzQ1Njc4OWFiY2RlZg==";

    private static final int IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BYTES = 16;

    private CriptografiaConverter converter;

    @BeforeEach
    void setUp() {
        converter = new CriptografiaConverter(CHAVE_VALIDA_32_BYTES);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Construtor
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Construtor — validação da chave")
    class Construtor {

        @Test
        @DisplayName("deve aceitar chave de exatamente 32 bytes (AES-256)")
        void deve_aceitar_chave_de_32_bytes() {
            assertThatNoException()
                    .isThrownBy(() -> new CriptografiaConverter(CHAVE_VALIDA_32_BYTES));
        }

        @Test
        @DisplayName("deve lançar IllegalArgumentException para chave de 16 bytes (AES-128 não permitido)")
        void deve_rejeitar_chave_de_16_bytes() {
            assertThatThrownBy(() -> new CriptografiaConverter(CHAVE_INVALIDA_16_BYTES))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("32 bytes");
        }

        @Test
        @DisplayName("deve lançar IllegalArgumentException para chave de 64 bytes")
        void deve_rejeitar_chave_de_64_bytes() {
            String chave64Bytes = Base64.getEncoder().encodeToString(new byte[64]);

            assertThatThrownBy(() -> new CriptografiaConverter(chave64Bytes))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("32 bytes");
        }

        @Test
        @DisplayName("deve lançar exceção para Base64 inválido no construtor")
        void deve_rejeitar_base64_invalido() {
            assertThatThrownBy(() -> new CriptografiaConverter("nao-e-base64-valido!!!@#$"))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("deve lançar IllegalArgumentException para chave de 1 byte")
        void deve_rejeitar_chave_de_1_byte() {
            String chaveUmByte = Base64.getEncoder().encodeToString(new byte[1]);

            assertThatThrownBy(() -> new CriptografiaConverter(chaveUmByte))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("32 bytes");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // convertToDatabaseColumn
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("convertToDatabaseColumn — criptografia")
    class ConvertToDatabaseColumn {

        @Test
        @DisplayName("deve retornar null quando plaintext é null")
        void deve_retornar_null_quando_plaintext_null() {
            String resultado = converter.convertToDatabaseColumn(null);

            assertThat(resultado).isNull();
        }

        @Test
        @DisplayName("deve produzir saída Base64 bem formada")
        void deve_produzir_saida_base64_valida() {
            String resultado = converter.convertToDatabaseColumn("qualquer-valor");

            assertThat(resultado).isNotNull().isNotEmpty();
            assertThatNoException()
                    .isThrownBy(() -> Base64.getDecoder().decode(resultado));
        }

        @Test
        @DisplayName("deve gerar criptografias distintas para o mesmo plaintext (IV randômico por chamada)")
        void deve_gerar_criptografias_distintas_para_mesmo_plaintext() {
            String plaintext = "conta-12345";

            String cifra1 = converter.convertToDatabaseColumn(plaintext);
            String cifra2 = converter.convertToDatabaseColumn(plaintext);

            assertThat(cifra1).isNotEqualTo(cifra2);
        }

        @Test
        @DisplayName("texto criptografado não deve conter o plaintext (segurança semântica)")
        void texto_criptografado_nao_deve_conter_o_plaintext() {
            String plaintext = "123.456.789-00";

            String cifra = converter.convertToDatabaseColumn(plaintext);

            assertThat(cifra).doesNotContain(plaintext);
        }

        @Test
        @DisplayName("payload deve ter tamanho mínimo de IV (12) + plaintext + tag GCM (16) codificados em Base64")
        void payload_deve_ter_tamanho_minimo_esperado() {
            String plaintext = "abc";
            int tamanhoDecodificadoEsperado =
                    IV_LENGTH_BYTES + plaintext.getBytes().length + GCM_TAG_LENGTH_BYTES;

            byte[] decodificado = Base64.getDecoder()
                    .decode(converter.convertToDatabaseColumn(plaintext));

            assertThat(decodificado.length).isGreaterThanOrEqualTo(tamanhoDecodificadoEsperado);
        }

        @Test
        @DisplayName("deve criptografar string vazia e produzir payload não nulo")
        void deve_criptografar_string_vazia() {
            String resultado = converter.convertToDatabaseColumn("");

            assertThat(resultado).isNotNull().isNotEmpty();
        }

        @Test
        @DisplayName("deve suportar caracteres com acentos (UTF-8 Latin Extended)")
        void deve_suportar_caracteres_com_acentos() {
            String plaintext = "Olá! Ação Bancária — Ônus Inútil — Cônjuge Específico";

            String cifra = converter.convertToDatabaseColumn(plaintext);
            String decifrado = converter.convertToEntityAttribute(cifra);

            assertThat(decifrado).isEqualTo(plaintext);
        }

        @Test
        @DisplayName("deve suportar emojis (UTF-8 supplementary plane, 4 bytes por caractere)")
        void deve_suportar_emojis() {
            String plaintext = "Transação PIX 💸 aprovada ✅ R$ 1.000,00 🏦🔒";

            String cifra = converter.convertToDatabaseColumn(plaintext);
            String decifrado = converter.convertToEntityAttribute(cifra);

            assertThat(decifrado).isEqualTo(plaintext);
        }

        @Test
        @DisplayName("deve suportar caracteres especiais e símbolos ASCII")
        void deve_suportar_caracteres_especiais_ascii() {
            String plaintext = "!@#$%^&*()_+-=[]{}|;':\",./<>?~`\\";

            String cifra = converter.convertToDatabaseColumn(plaintext);
            String decifrado = converter.convertToEntityAttribute(cifra);

            assertThat(decifrado).isEqualTo(plaintext);
        }

        @Test
        @DisplayName("deve criptografar strings longas (10.000 caracteres)")
        void deve_criptografar_strings_longas() {
            String plaintext = "A".repeat(10_000);

            String cifra = converter.convertToDatabaseColumn(plaintext);
            String decifrado = converter.convertToEntityAttribute(cifra);

            assertThat(decifrado).isEqualTo(plaintext);
        }

        @Test
        @DisplayName("payload codificado deve ser maior que o plaintext original")
        void payload_codificado_deve_ser_maior_que_o_plaintext() {
            String plaintext = "chave-pix@email.com";

            String cifra = converter.convertToDatabaseColumn(plaintext);

            assertThat(cifra.length()).isGreaterThan(plaintext.length());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // convertToEntityAttribute
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("convertToEntityAttribute — descriptografia")
    class ConvertToEntityAttribute {

        @Test
        @DisplayName("deve retornar null quando encrypted é null")
        void deve_retornar_null_quando_encrypted_null() {
            String resultado = converter.convertToEntityAttribute(null);

            assertThat(resultado).isNull();
        }

        @Test
        @DisplayName("deve descriptografar valor previamente criptografado corretamente")
        void deve_descriptografar_valor_corretamente() {
            String plaintext = "agencia-0001-conta-12345-6";

            String cifra = converter.convertToDatabaseColumn(plaintext);
            String resultado = converter.convertToEntityAttribute(cifra);

            assertThat(resultado).isEqualTo(plaintext);
        }

        @Test
        @DisplayName("deve lançar IllegalStateException para Base64 inválido")
        void deve_lancar_excecao_para_base64_invalido() {
            assertThatThrownBy(() -> converter.convertToEntityAttribute("n@o-é-base64-válido!!!"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("descriptografar");
        }

        @Test
        @DisplayName("deve lançar IllegalStateException para payload adulterado (falha GCM authentication tag)")
        void deve_lancar_excecao_para_payload_adulterado() {
            String plaintext = "dado-bancario-sensivel";
            byte[] combined = Base64.getDecoder()
                    .decode(converter.convertToDatabaseColumn(plaintext));
            combined[combined.length - 1] ^= 0xFF;
            String payloadAdulterado = Base64.getEncoder().encodeToString(combined);

            assertThatThrownBy(() -> converter.convertToEntityAttribute(payloadAdulterado))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("descriptografar");
        }

        @Test
        @DisplayName("deve lançar IllegalStateException ao decifrar com chave diferente da usada na criptografia")
        void deve_lancar_excecao_ao_usar_chave_diferente() {
            String plaintext = "CPF:123.456.789-00";
            String cifra = converter.convertToDatabaseColumn(plaintext);

            CriptografiaConverter converterComChaveDiferente =
                    new CriptografiaConverter(CHAVE_DIFERENTE_32_BYTES);

            assertThatThrownBy(() -> converterComChaveDiferente.convertToEntityAttribute(cifra))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("descriptografar");
        }

        @Test
        @DisplayName("deve lançar IllegalStateException para payload menor que o IV mínimo (12 bytes)")
        void deve_lancar_excecao_para_payload_menor_que_iv() {
            String payloadCurto = Base64.getEncoder().encodeToString(new byte[11]);

            assertThatThrownBy(() -> converter.convertToEntityAttribute(payloadCurto))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("deve lançar IllegalStateException para IV adulterado (authentication tag falha)")
        void deve_lancar_excecao_para_iv_adulterado() {
            String plaintext = "chave-pix-aleatoria";
            byte[] combined = Base64.getDecoder()
                    .decode(converter.convertToDatabaseColumn(plaintext));
            combined[0] ^= 0xFF;
            String payloadComIvAdulterado = Base64.getEncoder().encodeToString(combined);

            assertThatThrownBy(() -> converter.convertToEntityAttribute(payloadComIvAdulterado))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("descriptografar");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Round-trip
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Round-trip criptografia → descriptografia")
    class RoundTrip {

        @Test
        @DisplayName("deve preservar plaintext ASCII após ciclo completo encrypt → decrypt")
        void deve_preservar_plaintext_ascii_apos_roundtrip() {
            String plaintext = "agencia-0001-conta-98765-4";

            assertThat(converter.convertToEntityAttribute(
                    converter.convertToDatabaseColumn(plaintext)))
                    .isEqualTo(plaintext);
        }

        @Test
        @DisplayName("deve preservar texto unicode multilíngue após roundtrip")
        void deve_preservar_texto_unicode_multilingue_apos_roundtrip() {
            String plaintext = "中文 العربية Ελληνικά 日本語 한국어 \u00e9\u00e0\u00fc";

            assertThat(converter.convertToEntityAttribute(
                    converter.convertToDatabaseColumn(plaintext)))
                    .isEqualTo(plaintext);
        }

        @Test
        @DisplayName("deve processar múltiplos valores bancários independentes corretamente")
        void deve_processar_multiplos_valores_bancarios() {
            String[] plaintexts = {
                "CPF:123.456.789-00",
                "CNPJ:12.345.678/0001-90",
                "conta-poupanca-0001-98765-4",
                "chave-pix@banco.com.br",
                "R$ 150.000,00"
            };

            for (String plaintext : plaintexts) {
                String decifrado = converter.convertToEntityAttribute(
                        converter.convertToDatabaseColumn(plaintext));
                assertThat(decifrado)
                        .as("Round-trip falhou para: %s", plaintext)
                        .isEqualTo(plaintext);
            }
        }

        @Test
        @DisplayName("dois conversores com a mesma chave devem ser interoperáveis")
        void dois_conversores_com_mesma_chave_devem_ser_interop() {
            CriptografiaConverter converterA = new CriptografiaConverter(CHAVE_VALIDA_32_BYTES);
            CriptografiaConverter converterB = new CriptografiaConverter(CHAVE_VALIDA_32_BYTES);
            String plaintext = "dado-sensivel-compartilhado";

            String cifra = converterA.convertToDatabaseColumn(plaintext);
            String decifrado = converterB.convertToEntityAttribute(cifra);

            assertThat(decifrado).isEqualTo(plaintext);
        }
    }
}
