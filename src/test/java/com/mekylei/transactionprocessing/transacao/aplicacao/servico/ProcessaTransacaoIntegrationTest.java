package com.mekylei.transactionprocessing.transacao.aplicacao.servico;

import com.jayway.jsonpath.JsonPath;
import com.mekylei.transactionprocessing.IntegrationTestBase;
import com.mekylei.transactionprocessing.compartilhado.constantes.HeadersHttp;
import com.mekylei.transactionprocessing.compartilhado.util.CalendarioStubBacenService;
import com.mekylei.transactionprocessing.infraestrutura.entidade.TransacaoEntity;
import com.mekylei.transactionprocessing.transacao.dominio.StatusTransacao;
import com.mekylei.transactionprocessing.transacao.dominio.TipoTransacao;
import com.mekylei.transactionprocessing.transacao.dominio.Transacao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de integração para {@link ProcessaTransacaoService}.
 *
 * <p>Objetivo:</p>
 * <ul>
 *     <li>Validar o processamento end-to-end de transações via HTTP, Spring Security, serviços e PostgreSQL real.</li>
 *     <li>Garantir que idempotência, consulta e tratamento de erros preservem o contrato REST da aplicação.</li>
 *     <li>Exercitar persistência real de transações, saldos, limites e eventos outbox com Flyway.</li>
 * </ul>
 *
 * <p>Cenários cobertos:</p>
 * <ul>
 *     <li>Deve processar PIX com sucesso.</li>
 *     <li>Deve processar TED com sucesso em horário bancário.</li>
 *     <li>Deve processar TEF com sucesso quando antifraude autoriza.</li>
 *     <li>Deve rejeitar TEF quando antifraude recusa.</li>
 *     <li>Deve retornar a mesma transação para requisição idempotente repetida.</li>
 *     <li>Deve retornar 404 ao consultar transação inexistente.</li>
 *     <li>Deve consultar transação existente.</li>
 *     <li>Deve retornar 401 quando a requisição não possui autenticação.</li>
 * </ul>
 *
 * <p>Cenários não cobertos:</p>
 * <ul>
 *     <li>Publicação real em Kafka, contratos externos do BACEN e testes de carga.</li>
 * </ul>
 *
 * @author Mekylei Belchior
 * @since 1.0
 */
@DisplayName("Processa Transacao Integration")
class ProcessaTransacaoIntegrationTest extends IntegrationTestBase {

    private static final ZoneId BRASIL_TIMEZONE = ZoneId.of("America/Sao_Paulo");

    @MockitoBean
    private CalendarioStubBacenService calendarioStubBacenService;

    @Test
    @DisplayName("deve processar PIX com sucesso")
    void deveProcessarPixComSucesso() throws Exception {
        UUID idContaOrigem = criarContaAtivaComSaldoELimites(new BigDecimal("1000.00"), TipoTransacao.PIX);
        UUID idIdempotencia = UUID.randomUUID();

        MvcResult resultado = mockMvc.perform(post("/api/v1/transacoes/pix")
                        .with(jwtToken("CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HeadersHttp.IDEMPOTENCIA_HEADER, idIdempotencia)
                        .content(transacaoRequisicaoJson(new BigDecimal("150.00"), idContaOrigem, "pix-" + UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("PIX"))
                .andExpect(jsonPath("$.status").value("COMPLETADA"))
                .andReturn();

        UUID idTransacao = UUID.fromString(JsonPath.read(resultado.getResponse().getContentAsString(), "$.id"));
        TransacaoEntity transacao = transacaoJpaRepository.findById(idTransacao).orElseThrow();

        assertThat(transacao.getStatus()).isEqualTo(StatusTransacao.COMPLETADA);
        assertThat(transacao.getIdIdempotencia()).isEqualTo(idIdempotencia);
    }

    @Test
    @DisplayName("deve processar TED com sucesso em horário bancário")
    void deveProcessarTedComSucessoEmHorarioBancario() throws Exception {
        UUID idContaOrigem = criarContaAtivaComSaldoELimites(new BigDecimal("1000.00"), TipoTransacao.TED);
        when(calendarioStubBacenService.isDiaUtil(any(LocalDate.class))).thenReturn(true);
        LocalTime horarioValido = LocalTime.of(10, 0);

        try (MockedStatic<LocalTime> localTime = mockStatic(LocalTime.class, CALLS_REAL_METHODS)) {
            localTime.when(() -> LocalTime.now(BRASIL_TIMEZONE)).thenReturn(horarioValido);

            mockMvc.perform(post("/api/v1/transacoes/ted")
                            .with(jwtToken("OPERADOR"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(HeadersHttp.IDEMPOTENCIA_HEADER, UUID.randomUUID())
                            .content(transacaoRequisicaoJson(new BigDecimal("120.00"), idContaOrigem, "341-12345-6")))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.tipo").value("TED"))
                    .andExpect(jsonPath("$.status").value("COMPLETADA"));
        }
    }

    @Test
    @DisplayName("deve processar TEF com sucesso quando antifraude autoriza")
    void deveProcessarTefComSucessoQuandoAntifraudeAutoriza() throws Exception {
        UUID idContaOrigem = criarContaAtivaComSaldoELimites(new BigDecimal("1000.00"), TipoTransacao.TEF);

        MvcResult resultado = mockMvc.perform(post("/api/v1/transacoes/tef")
                        .with(jwtToken("CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HeadersHttp.IDEMPOTENCIA_HEADER, UUID.randomUUID())
                        .content(transacaoRequisicaoJson(new BigDecimal("900.00"), idContaOrigem, "0001-54321-0")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("TEF"))
                .andExpect(jsonPath("$.status").value("COMPLETADA"))
                .andReturn();

        UUID idTransacao = UUID.fromString(JsonPath.read(resultado.getResponse().getContentAsString(), "$.id"));

        assertThat(transacaoJpaRepository.findById(idTransacao))
                .isPresent()
                .get()
                .extracting(TransacaoEntity::getStatus)
                .isEqualTo(StatusTransacao.COMPLETADA);
    }

    @Test
    @DisplayName("deve rejeitar TEF quando antifraude recusa")
    void deveRejeitarTefQuandoAntifraudeRecusa() throws Exception {
        UUID idContaOrigem = criarContaAtivaComSaldoELimites(
                new BigDecimal("20000.00"),
                new BigDecimal("50000.00"),
                new BigDecimal("50000.00"),
                TipoTransacao.TEF);

        mockMvc.perform(post("/api/v1/transacoes/tef")
                        .with(jwtToken("CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HeadersHttp.IDEMPOTENCIA_HEADER, UUID.randomUUID())
                        .content(transacaoRequisicaoJson(new BigDecimal("10000.01"), idContaOrigem, "0001-54321-0")))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.detail").value(containsString("antifraude")))
                .andExpect(jsonPath("$.codigoErro").value("TEF_RECUSADO_ANTIFRAUDE"));
    }

    @Test
    @DisplayName("deve retornar a mesma transação para requisição idempotente repetida")
    void deveRetornarMesmaTransacaoParaRequisicaoIdempotenteRepetida() throws Exception {
        UUID idContaOrigem = criarContaAtivaComSaldoELimites(new BigDecimal("1000.00"), TipoTransacao.PIX);
        UUID idIdempotencia = UUID.randomUUID();
        String corpo = transacaoRequisicaoJson(new BigDecimal("75.00"), idContaOrigem, "pix-" + UUID.randomUUID());

        MvcResult primeiraResposta = mockMvc.perform(post("/api/v1/transacoes/pix")
                        .with(jwtToken("CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HeadersHttp.IDEMPOTENCIA_HEADER, idIdempotencia)
                        .content(corpo))
                .andExpect(status().isCreated())
                .andReturn();

        MvcResult segundaResposta = mockMvc.perform(post("/api/v1/transacoes/pix")
                        .with(jwtToken("CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HeadersHttp.IDEMPOTENCIA_HEADER, idIdempotencia)
                        .content(corpo))
                .andExpect(status().isCreated())
                .andReturn();

        String idPrimeira = JsonPath.read(primeiraResposta.getResponse().getContentAsString(), "$.id");
        String idSegunda = JsonPath.read(segundaResposta.getResponse().getContentAsString(), "$.id");

        assertThat(idSegunda).isEqualTo(idPrimeira);
        assertThat(transacaoJpaRepository.count()).isEqualTo(1);
        assertThat(transacaoJpaRepository.findByIdIdempotencia(idIdempotencia)).isPresent();
    }

    @Test
    @DisplayName("deve retornar 404 ao consultar transação inexistente")
    void deveRetornar404AoConsultarTransacaoInexistente() throws Exception {
        mockMvc.perform(get("/api/v1/transacoes/{id}", UUID.randomUUID())
                        .with(jwtToken("CLIENTE")))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.codigoErro").value("TRANSACAO_NAO_ENCONTRADA"));
    }

    @Test
    @DisplayName("deve consultar transação existente")
    void deveConsultarTransacaoExistente() throws Exception {
        UUID idContaOrigem = criarContaAtivaComSaldoELimites(new BigDecimal("1000.00"), TipoTransacao.PIX);
        Transacao transacao = criarTransacaoConcluida(idContaOrigem, new BigDecimal("88.90"), TipoTransacao.PIX);

        mockMvc.perform(get("/api/v1/transacoes/{id}", transacao.getId())
                        .with(jwtToken("CLIENTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(transacao.getId().toString()))
                .andExpect(jsonPath("$.idContaOrigem").value(idContaOrigem.toString()))
                .andExpect(jsonPath("$.tipo").value("PIX"))
                .andExpect(jsonPath("$.status").value("COMPLETADA"));
    }

    @Test
    @DisplayName("deve retornar 401 quando a requisição não possui autenticação")
    void deveRetornar401QuandoRequisicaoNaoPossuiAutenticacao() throws Exception {
        mockMvc.perform(post("/api/v1/transacoes/pix")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HeadersHttp.IDEMPOTENCIA_HEADER, UUID.randomUUID())
                        .content(transacaoRequisicaoJson(new BigDecimal("10.00"), UUID.randomUUID(), "pix")))
                .andExpect(status().isUnauthorized());
    }
}
