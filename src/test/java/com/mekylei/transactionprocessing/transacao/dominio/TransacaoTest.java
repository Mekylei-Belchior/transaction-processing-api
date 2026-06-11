package com.mekylei.transactionprocessing.transacao.dominio;

import com.mekylei.transactionprocessing.compartilhado.dominio.TipoTransacao;
import com.mekylei.transactionprocessing.compartilhado.dominio.ValorMonetario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitários para {@link Transacao}.
 *
 * <p>Objetivo:</p>
 * <ul>
 *     <li>Validar o comportamento esperado de {@link Transacao} nos cenários exercitados pela suíte.</li>
 *     <li>Preservar regras de negócio, contratos, integrações ou invariantes aplicáveis à classe testada.</li>
 *     <li>Garantir regressão funcional para alterações futuras relacionadas a {@code Transacao}.</li>
 * </ul>
 *
 * <p>Cenários cobertos:</p>
 * <ul>
 *     <li>Deve criar objeto quando campos obrigatórios são fornecidos.</li>
 *     <li>Deve retornar nova instância quando status muda.</li>
 *     <li>Deve preservar todos os outros campos quando status muda.</li>
 *     <li>Deve manter original imutável quando status é encadeado.</li>
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
@DisplayName("Transacao")
class TransacaoTest {

    @Nested
    @DisplayName("Builder")
    class Builder {

        @Test
        @DisplayName("deve criar objeto quando campos obrigatórios são fornecidos")
        void deve_criar_objeto_quando_campos_obrigatorios_fornecidos() {
            UUID idContaOrigem = UUID.randomUUID();
            ValorMonetario valor = ValorMonetario.paraReal(new BigDecimal("100.00"));

            Transacao transacao = Transacao.builder()
                    .valor(valor)
                    .tipo(TipoTransacao.PIX)
                    .idContaOrigem(idContaOrigem)
                    .contaDestino("0001-123456")
                    .build();

            assertThat(transacao.getId()).isNotNull();
            assertThat(transacao.getValor()).isEqualTo(valor);
            assertThat(transacao.getTipo()).isEqualTo(TipoTransacao.PIX);
            assertThat(transacao.getStatus()).isEqualTo(StatusTransacao.PENDENTE);
            assertThat(transacao.getCriadoEm()).isNotNull();
            assertThat(transacao.getIdContaOrigem()).isEqualTo(idContaOrigem);
            assertThat(transacao.getContaDestino()).isEqualTo("0001-123456");
        }
    }

    @Nested
    @DisplayName("comStatus")
    class ComStatus {

        @Test
        @DisplayName("deve retornar nova instância quando status muda")
        void deve_retornar_nova_instancia_quando_status_muda() {
            Transacao original = transacaoValida();

            Transacao alterada = original.comStatus(StatusTransacao.PROCESSANDO);

            assertThat(alterada).isNotSameAs(original);
            assertThat(alterada.getStatus()).isEqualTo(StatusTransacao.PROCESSANDO);
            assertThat(original.getStatus()).isEqualTo(StatusTransacao.PENDENTE);
        }

        @Test
        @DisplayName("deve preservar todos os outros campos quando status muda")
        void deve_preservar_todos_os_outros_campos_quando_status_muda() {
            Transacao original = transacaoValida();

            Transacao alterada = original.comStatus(StatusTransacao.COMPLETADA);

            assertThat(alterada.getId()).isEqualTo(original.getId());
            assertThat(alterada.getIdCorrelacao()).isEqualTo(original.getIdCorrelacao());
            assertThat(alterada.getIdIdempotencia()).isEqualTo(original.getIdIdempotencia());
            assertThat(alterada.getValor()).isEqualTo(original.getValor());
            assertThat(alterada.getTipo()).isEqualTo(original.getTipo());
            assertThat(alterada.getCriadoEm()).isEqualTo(original.getCriadoEm());
            assertThat(alterada.getIdContaOrigem()).isEqualTo(original.getIdContaOrigem());
            assertThat(alterada.getContaDestino()).isEqualTo(original.getContaDestino());
        }

        @Test
        @DisplayName("deve manter original imutável quando status é encadeado")
        void deve_manter_original_imutavel_quando_status_encadeado() {
            Transacao original = transacaoValida();

            Transacao alterada = original
                    .comStatus(StatusTransacao.PROCESSANDO)
                    .comStatus(StatusTransacao.FALHOU);

            assertThat(original.getStatus()).isEqualTo(StatusTransacao.PENDENTE);
            assertThat(alterada.getStatus()).isEqualTo(StatusTransacao.FALHOU);
            assertThat(alterada).isNotSameAs(original);
        }
    }

    private static Transacao transacaoValida() {
        return Transacao.builder()
                .id(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .idCorrelacao(UUID.fromString("22222222-2222-2222-2222-222222222222"))
                .idIdempotencia(UUID.fromString("33333333-3333-3333-3333-333333333333"))
                .valor(ValorMonetario.paraReal(new BigDecimal("150.25")))
                .tipo(TipoTransacao.TED)
                .status(StatusTransacao.PENDENTE)
                .criadoEm(Instant.parse("2026-01-15T10:15:30Z"))
                .idContaOrigem(UUID.fromString("44444444-4444-4444-4444-444444444444"))
                .contaDestino("0001-987654")
                .build();
    }
}
