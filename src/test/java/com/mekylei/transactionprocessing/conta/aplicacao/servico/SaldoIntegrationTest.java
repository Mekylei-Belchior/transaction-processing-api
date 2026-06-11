package com.mekylei.transactionprocessing.conta.aplicacao.servico;

import com.mekylei.transactionprocessing.IntegrationTestBase;
import com.mekylei.transactionprocessing.compartilhado.constantes.HeadersHttp;
import com.mekylei.transactionprocessing.infraestrutura.entidade.SaldoEntity;
import com.mekylei.transactionprocessing.infraestrutura.entidade.TransacaoEntity;
import com.mekylei.transactionprocessing.transacao.dominio.StatusTransacao;
import com.mekylei.transactionprocessing.compartilhado.dominio.TipoTransacao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de integração para {@link SaldoService}.
 *
 * <p>Objetivo:</p>
 * <ul>
 *     <li>Validar regras de saldo em transações processadas via HTTP com PostgreSQL real.</li>
 *     <li>Garantir débito persistido após transação concluída.</li>
 *     <li>Exercitar controle concorrente para evitar perda de dados em débitos simultâneos.</li>
 * </ul>
 *
 * <p>Cenários cobertos:</p>
 * <ul>
 *     <li>Deve rejeitar transação com saldo insuficiente.</li>
 *     <li>Deve debitar saldo da conta de origem após transação concluída.</li>
 *     <li>Deve aceitar apenas um de dois débitos simultâneos sem perda de dados.</li>
 * </ul>
 *
 * <p>Cenários não cobertos:</p>
 * <ul>
 *     <li>Reserva de saldo, bloqueio judicial, crédito externo e testes de carga prolongados.</li>
 * </ul>
 *
 * @author Mekylei Belchior
 * @since 1.0
 */
@DisplayName("Saldo Integration")
class SaldoIntegrationTest extends IntegrationTestBase {

    @Test
    @DisplayName("deve rejeitar transação com saldo insuficiente")
    void deveRejeitarTransacaoComSaldoInsuficiente() throws Exception {
        UUID idContaOrigem = criarContaAtivaComSaldoELimites(new BigDecimal("50.00"), TipoTransacao.PIX);

        mockMvc.perform(post("/api/v1/transacoes/pix")
                        .with(jwtToken("CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HeadersHttp.IDEMPOTENCIA_HEADER, UUID.randomUUID())
                        .content(transacaoRequisicaoJson(new BigDecimal("100.00"), idContaOrigem, "pix-" + UUID.randomUUID())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Saldo insuficiente"))
                .andExpect(jsonPath("$.codigoErro").value("SALDO_INSUFICIENTE"));
    }

    @Test
    @DisplayName("deve debitar saldo da conta de origem após transação concluída")
    void deveDebitarSaldoDaContaOrigemAposTransacaoConcluida() throws Exception {
        UUID idContaOrigem = criarContaAtivaComSaldoELimites(new BigDecimal("1000.00"), TipoTransacao.PIX);

        mockMvc.perform(post("/api/v1/transacoes/pix")
                        .with(jwtToken("CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HeadersHttp.IDEMPOTENCIA_HEADER, UUID.randomUUID())
                        .content(transacaoRequisicaoJson(new BigDecimal("200.00"), idContaOrigem, "pix-" + UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETADA"));

        SaldoEntity saldo = saldoJpaRepository.findByIdConta(idContaOrigem).orElseThrow();

        assertThat(saldo.getDisponivel()).isEqualByComparingTo(new BigDecimal("800.00"));
    }

    @Test
    @DisplayName("deve aceitar apenas um de dois débitos simultâneos sem perda de dados")
    void deveAceitarApenasUmDeDoisDebitosSimultaneosSemPerdaDeDados() {
        UUID idContaOrigem = criarContaAtivaComSaldoELimites(new BigDecimal("100.00"), TipoTransacao.PIX);
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        try {
            CompletableFuture<Integer> primeiroDebito = CompletableFuture.supplyAsync(
                    () -> processarPix(idContaOrigem, new BigDecimal("80.00")), executorService);
            CompletableFuture<Integer> segundoDebito = CompletableFuture.supplyAsync(
                    () -> processarPix(idContaOrigem, new BigDecimal("80.00")), executorService);

            List<Integer> statusHttp = List.of(primeiroDebito.join(), segundoDebito.join());
            SaldoEntity saldoFinal = saldoJpaRepository.findByIdConta(idContaOrigem).orElseThrow();
            List<TransacaoEntity> transacoesConcluidas = transacaoJpaRepository.findAll().stream()
                    .filter(transacao -> idContaOrigem.equals(transacao.getIdContaOrigem()))
                    .filter(transacao -> StatusTransacao.COMPLETADA.equals(transacao.getStatus()))
                    .toList();

            assertThat(statusHttp).contains(201);
            assertThat(statusHttp).anySatisfy(status -> assertThat(status).isIn(409, 422));
            assertThat(saldoFinal.getDisponivel()).isEqualByComparingTo(new BigDecimal("20.00"));
            assertThat(transacoesConcluidas).hasSize(1);
        } finally {
            executorService.shutdownNow();
        }
    }

    private int processarPix(UUID idContaOrigem, BigDecimal valor) {
        try {
            return mockMvc.perform(post("/api/v1/transacoes/pix")
                            .with(jwtToken("CLIENTE"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(HeadersHttp.IDEMPOTENCIA_HEADER, UUID.randomUUID())
                            .content(transacaoRequisicaoJson(valor, idContaOrigem, "pix-" + UUID.randomUUID())))
                    .andReturn()
                    .getResponse()
                    .getStatus();
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao processar PIX concorrente", e);
        }
    }
}
