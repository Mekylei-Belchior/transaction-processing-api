package com.mekylei.transactionprocessing.transacao.aplicacao.servico;

import com.mekylei.transactionprocessing.IntegrationTestBase;
import com.mekylei.transactionprocessing.infraestrutura.entidade.TransacaoEntity;
import com.mekylei.transactionprocessing.transacao.dominio.StatusTransacao;
import com.mekylei.transactionprocessing.transacao.dominio.TipoTransacao;
import com.mekylei.transactionprocessing.transacao.dominio.Transacao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de integração para {@link EstornoTransacaoService}.
 *
 * <p>Objetivo:</p>
 * <ul>
 *     <li>Validar o estorno de transações concluídas via endpoint HTTP e PostgreSQL real.</li>
 *     <li>Garantir aplicação das regras de elegibilidade, segurança e erros de recurso inexistente.</li>
 *     <li>Verificar atualização persistida do status da transação estornada.</li>
 * </ul>
 *
 * <p>Cenários cobertos:</p>
 * <ul>
 *     <li>Deve estornar transação concluída com role gerente.</li>
 *     <li>Deve rejeitar estorno de transação já estornada.</li>
 *     <li>Deve negar estorno com role cliente.</li>
 *     <li>Deve retornar 404 ao estornar transação inexistente.</li>
 * </ul>
 *
 * <p>Cenários não cobertos:</p>
 * <ul>
 *     <li>Estorno parcial, chargeback externo e testes de carga concorrente.</li>
 * </ul>
 *
 * @author Mekylei Belchior
 * @since 1.0
 */
@DisplayName("Estorno Integration")
class EstornoIntegrationTest extends IntegrationTestBase {

    @Test
    @DisplayName("deve estornar transação concluída com role gerente")
    void deveEstornarTransacaoConcluidaComRoleGerente() throws Exception {
        UUID idContaOrigem = criarContaAtivaComSaldoELimites(new BigDecimal("1000.00"), TipoTransacao.PIX);
        Transacao transacao = criarTransacaoConcluida(idContaOrigem, new BigDecimal("100.00"), TipoTransacao.PIX);

        mockMvc.perform(post("/api/v1/transacoes/{id}/estorno", transacao.getId())
                        .with(jwtToken("GERENTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(estornoRequisicaoJson("Transação não reconhecida")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idTransacaoOriginal").value(transacao.getId().toString()))
                .andExpect(jsonPath("$.status").value("ESTORNADA"));

        assertThat(transacaoJpaRepository.findById(transacao.getId()))
                .isPresent()
                .get()
                .extracting(TransacaoEntity::getStatus)
                .isEqualTo(StatusTransacao.ESTORNADA);
    }

    @Test
    @DisplayName("deve rejeitar estorno de transação já estornada")
    void deveRejeitarEstornoDeTransacaoJaEstornada() throws Exception {
        UUID idContaOrigem = criarContaAtivaComSaldoELimites(new BigDecimal("1000.00"), TipoTransacao.PIX);
        Transacao transacao = criarTransacaoConcluida(idContaOrigem, new BigDecimal("100.00"), TipoTransacao.PIX);
        transacaoJpaAdapter.update(transacao.comStatus(StatusTransacao.ESTORNADA));

        mockMvc.perform(post("/api/v1/transacoes/{id}/estorno", transacao.getId())
                        .with(jwtToken("GERENTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(estornoRequisicaoJson("Transação não reconhecida")))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.codigoErro").value("ESTORNO_INVALIDO"));
    }

    @Test
    @DisplayName("deve negar estorno com role cliente")
    void deveNegarEstornoComRoleCliente() throws Exception {
        UUID idContaOrigem = criarContaAtivaComSaldoELimites(new BigDecimal("1000.00"), TipoTransacao.PIX);
        Transacao transacao = criarTransacaoConcluida(idContaOrigem, new BigDecimal("100.00"), TipoTransacao.PIX);

        mockMvc.perform(post("/api/v1/transacoes/{id}/estorno", transacao.getId())
                        .with(jwtToken("CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(estornoRequisicaoJson("Transação não reconhecida")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("deve retornar 404 ao estornar transação inexistente")
    void deveRetornar404AoEstornarTransacaoInexistente() throws Exception {
        mockMvc.perform(post("/api/v1/transacoes/{id}/estorno", UUID.randomUUID())
                        .with(jwtToken("GERENTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(estornoRequisicaoJson("Transação não reconhecida")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigoErro").value("TRANSACAO_NAO_ENCONTRADA"));
    }
}
