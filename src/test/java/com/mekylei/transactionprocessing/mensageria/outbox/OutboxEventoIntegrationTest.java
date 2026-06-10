package com.mekylei.transactionprocessing.mensageria.outbox;

import com.jayway.jsonpath.JsonPath;
import com.mekylei.transactionprocessing.IntegrationTestBase;
import com.mekylei.transactionprocessing.compartilhado.constantes.HeadersHttp;
import com.mekylei.transactionprocessing.infraestrutura.entidade.OutboxEventoEntity;
import com.mekylei.transactionprocessing.infraestrutura.entidade.StatusOutboxEvento;
import com.mekylei.transactionprocessing.transacao.dominio.TipoTransacao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de integração para o fluxo de outbox transacional.
 *
 * <p>Objetivo:</p>
 * <ul>
 *     <li>Validar que eventos de domínio são persistidos no outbox ao finalizar transações com sucesso.</li>
 *     <li>Garantir que o payload salvo em PostgreSQL jsonb contém os campos essenciais do agregado.</li>
 *     <li>Comprovar que transação e evento outbox são gravados no mesmo fluxo transacional.</li>
 * </ul>
 *
 * <p>Cenários cobertos:</p>
 * <ul>
 *     <li>Deve criar evento outbox ao processar PIX com sucesso.</li>
 *     <li>Deve persistir payload JSON válido contendo o ID da transação.</li>
 *     <li>Deve persistir transação e outbox no mesmo processamento.</li>
 * </ul>
 *
 * <p>Cenários não cobertos:</p>
 * <ul>
 *     <li>Envio real ao Kafka, reprocessamento de falhas e ordenação entre múltiplos agregados.</li>
 * </ul>
 *
 * @author Mekylei Belchior
 * @since 1.0
 */
@DisplayName("Outbox Evento Integration")
class OutboxEventoIntegrationTest extends IntegrationTestBase {

    @Test
    @DisplayName("deve criar evento outbox ao processar PIX com sucesso")
    void deveCriarEventoOutboxAoProcessarPixComSucesso() throws Exception {
        UUID idContaOrigem = criarContaAtivaComSaldoELimites(new BigDecimal("1000.00"), TipoTransacao.PIX);

        MvcResult resultado = mockMvc.perform(post("/api/v1/transacoes/pix")
                        .with(jwtToken("CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HeadersHttp.IDEMPOTENCIA_HEADER, UUID.randomUUID())
                        .content(transacaoRequisicaoJson(new BigDecimal("150.00"), idContaOrigem, "pix-" + UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETADA"))
                .andReturn();

        UUID idTransacao = UUID.fromString(JsonPath.read(resultado.getResponse().getContentAsString(), "$.id"));
        List<OutboxEventoEntity> eventos = eventosDaTransacao(idTransacao);

        assertThat(eventos).isNotEmpty();
        assertThat(eventos)
                .extracting(OutboxEventoEntity::getStatus)
                .containsAnyOf(StatusOutboxEvento.PENDENTE, StatusOutboxEvento.PUBLICADO);
    }

    @Test
    @DisplayName("deve persistir payload JSON válido contendo o ID da transação")
    void devePersistirPayloadJsonValidoContendoIdDaTransacao() throws Exception {
        UUID idContaOrigem = criarContaAtivaComSaldoELimites(new BigDecimal("1000.00"), TipoTransacao.PIX);

        MvcResult resultado = mockMvc.perform(post("/api/v1/transacoes/pix")
                        .with(jwtToken("CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HeadersHttp.IDEMPOTENCIA_HEADER, UUID.randomUUID())
                        .content(transacaoRequisicaoJson(new BigDecimal("50.00"), idContaOrigem, "pix-" + UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andReturn();

        UUID idTransacao = UUID.fromString(JsonPath.read(resultado.getResponse().getContentAsString(), "$.id"));
        OutboxEventoEntity evento = eventosDaTransacao(idTransacao).getFirst();

        assertThat(evento.getPayload()).isNotNull();
        assertThat(evento.getPayload().get("idAgregado").asString()).isEqualTo(idTransacao.toString());
    }

    @Test
    @DisplayName("deve persistir transação e outbox no mesmo processamento")
    void devePersistirTransacaoEOutboxNoMesmoProcessamento() throws Exception {
        UUID idContaOrigem = criarContaAtivaComSaldoELimites(new BigDecimal("1000.00"), TipoTransacao.PIX);

        MvcResult resultado = mockMvc.perform(post("/api/v1/transacoes/pix")
                        .with(jwtToken("CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HeadersHttp.IDEMPOTENCIA_HEADER, UUID.randomUUID())
                        .content(transacaoRequisicaoJson(new BigDecimal("75.00"), idContaOrigem, "pix-" + UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andReturn();

        UUID idTransacao = UUID.fromString(JsonPath.read(resultado.getResponse().getContentAsString(), "$.id"));

        assertThat(transacaoJpaRepository.findById(idTransacao)).isPresent();
        assertThat(eventosDaTransacao(idTransacao)).isNotEmpty();
    }

    private List<OutboxEventoEntity> eventosDaTransacao(UUID idTransacao) {
        return outboxEventoJpaRepository.findAll().stream()
                .filter(evento -> idTransacao.equals(evento.getIdAgregado()))
                .toList();
    }
}
