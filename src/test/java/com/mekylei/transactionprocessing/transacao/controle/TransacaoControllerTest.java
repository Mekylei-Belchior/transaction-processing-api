package com.mekylei.transactionprocessing.transacao.controle;

import com.mekylei.transactionprocessing.auditoria.porta.AuditoriaContextWriter;
import com.mekylei.transactionprocessing.compartilhado.constantes.HeadersHttp;
import com.mekylei.transactionprocessing.compartilhado.dominio.ValorMonetario;
import com.mekylei.transactionprocessing.compartilhado.exception.RecursoNaoEncontradoException;
import com.mekylei.transactionprocessing.compartilhado.exception.RegraNegocioException;
import com.mekylei.transactionprocessing.compartilhado.exception.SaldoInsuficienteException;
import com.mekylei.transactionprocessing.configuracao.seguranca.ApiAcessoNegadoHandler;
import com.mekylei.transactionprocessing.configuracao.seguranca.ApiAutenticacaoEntryPoint;
import com.mekylei.transactionprocessing.configuracao.seguranca.JwtClaimsConverter;
import com.mekylei.transactionprocessing.configuracao.seguranca.RateLimitResposta;
import com.mekylei.transactionprocessing.configuracao.seguranca.SecurityConfig;
import com.mekylei.transactionprocessing.transacao.aplicacao.servico.ConsultaTransacaoService;
import com.mekylei.transactionprocessing.transacao.aplicacao.servico.EstornoTransacaoService;
import com.mekylei.transactionprocessing.transacao.aplicacao.servico.ProcessaTransacaoService;
import com.mekylei.transactionprocessing.transacao.controle.dto.EstornoResposta;
import com.mekylei.transactionprocessing.transacao.dominio.StatusTransacao;
import com.mekylei.transactionprocessing.transacao.dominio.TipoTransacao;
import com.mekylei.transactionprocessing.transacao.dominio.Transacao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes unitários para {@link TransacaoController}.
 *
 * <p>Objetivo:</p>
 * <ul>
 *     <li>Validar o comportamento esperado de {@link TransacaoController} nos cenários exercitados pela suíte.</li>
 *     <li>Preservar regras de negócio, contratos, integrações ou invariantes aplicáveis à classe testada.</li>
 *     <li>Garantir regressão funcional para alterações futuras relacionadas a {@code TransacaoController}.</li>
 * </ul>
 *
 * <p>Cenários cobertos:</p>
 * <ul>
 *     <li>Deve retornar 201 quando Pix processado com sucesso.</li>
 *     <li>Deve retornar 401 quando sem autenticação.</li>
 *     <li>Deve retornar 400 quando body inválido.</li>
 *     <li>Deve retornar 400 quando header idempotência ausente.</li>
 *     <li>Deve retornar 201 quando TED processado com sucesso.</li>
 *     <li>Deve retornar 422 quando TED fora do horário.</li>
 *     <li>Deve retornar 201 quando TEF processado com sucesso.</li>
 *     <li>Deve retornar 200 quando transação encontrada.</li>
 *     <li>Deve retornar 404 quando transação não encontrada.</li>
 *     <li>Deve retornar 200 quando estorno com sucesso.</li>
 *     <li>Deve retornar 422 quando transação não elegível.</li>
 *     <li>Deve retornar 403 quando role insuficiente.</li>
 *     <li>Deve retornar 422 com problem detail quando regra negocio exception.</li>
 *     <li>Deve retornar 400 com problem detail quando method argument not valid exception.</li>
 *     <li>Deve retornar 422 com problem detail quando saldo insuficiente exception.</li>
 *     <li>Deve retornar 409 com problem detail quando object optimistic locking failure exception.</li>
 *     <li>Deve retornar 409 com problem detail quando data integrity violation exception.</li>
 *     <li>Deve retornar 500 com problem detail quando exception genérica.</li>
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
@WebMvcTest(TransacaoController.class)
@ActiveProfiles("test")
@Import({SecurityConfig.class, ApiAutenticacaoEntryPoint.class, ApiAcessoNegadoHandler.class, JwtClaimsConverter.class})
@DisplayName("Transacao Controller")
class TransacaoControllerTest {

    private static final UUID ID_TRANSACAO = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ID_CONTA_ORIGEM = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ID_CORRELACAO = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID ID_IDEMPOTENCIA = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final Instant CRIADO_EM = Instant.parse("2026-06-03T12:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProcessaTransacaoService processaTransacaoService;

    @MockitoBean
    private ConsultaTransacaoService consultaTransacaoService;

    @MockitoBean
    private EstornoTransacaoService estornoTransacaoService;

    @MockitoBean
    private AuditoriaContextWriter auditoriaContextWriter;

    @MockitoBean
    private RateLimitResposta rateLimitResposta;

    @Test
    @DisplayName("deve retornar 201 quando Pix processado com sucesso")
    void deveRetornar201QuandoPixProcessadoComSucesso() throws Exception {
        Transacao transacao = transacao(TipoTransacao.PIX, StatusTransacao.COMPLETADA);
        when(processaTransacaoService.processa(
                eq(new BigDecimal("150.00")),
                eq(TipoTransacao.PIX),
                eq(ID_CONTA_ORIGEM),
                eq("destino-pix"),
                eq(ID_IDEMPOTENCIA)))
                .thenReturn(transacao);

        mockMvc.perform(post("/api/v1/transacoes/pix")
                        .with(cliente())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HeadersHttp.IDEMPOTENCIA_HEADER, ID_IDEMPOTENCIA)
                        .content(transacaoRequisicaoJson("150.00", ID_CONTA_ORIGEM, "destino-pix")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(ID_TRANSACAO.toString()));
    }

    @Test
    @DisplayName("deve retornar 401 quando sem autenticação")
    void deveRetornar401QuandoSemAutenticacao() throws Exception {
        mockMvc.perform(post("/api/v1/transacoes/pix")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HeadersHttp.IDEMPOTENCIA_HEADER, ID_IDEMPOTENCIA)
                        .content(transacaoRequisicaoJson("150.00", ID_CONTA_ORIGEM, "destino-pix")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("deve retornar 400 quando body inválido")
    void deveRetornar400QuandoBodyInvalido() throws Exception {
        mockMvc.perform(post("/api/v1/transacoes/pix")
                        .with(cliente())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HeadersHttp.IDEMPOTENCIA_HEADER, ID_IDEMPOTENCIA)
                        .content("""
                                {
                                  "valor": null,
                                  "idContaOrigem": "%s",
                                  "contaDestino": "destino-pix"
                                }
                                """.formatted(ID_CONTA_ORIGEM)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.Campos[0]").exists());
    }

    @Test
    @DisplayName("deve retornar 400 quando header idempotência ausente")
    void deveRetornar400QuandoHeaderIdempotenciaAusente() throws Exception {
        mockMvc.perform(post("/api/v1/transacoes/pix")
                        .with(cliente())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transacaoRequisicaoJson("150.00", ID_CONTA_ORIGEM, "destino-pix")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.codigoErro").value("CABECALHO_AUSENTE"));
    }

    @Test
    @DisplayName("deve retornar 201 quando TED processado com sucesso")
    void deveRetornar201QuandoTedProcessadoComSucesso() throws Exception {
        Transacao transacao = transacao(TipoTransacao.TED, StatusTransacao.COMPLETADA);
        when(processaTransacaoService.processa(any(), eq(TipoTransacao.TED), any(), any(), any()))
                .thenReturn(transacao);

        mockMvc.perform(post("/api/v1/transacoes/ted")
                        .with(cliente())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HeadersHttp.IDEMPOTENCIA_HEADER, ID_IDEMPOTENCIA)
                        .content(transacaoRequisicaoJson("150.00", ID_CONTA_ORIGEM, "12345-6")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(ID_TRANSACAO.toString()));
    }

    @Test
    @DisplayName("deve retornar 422 quando TED fora do horário")
    void deveRetornar422QuandoTedForaDoHorario() throws Exception {
        when(processaTransacaoService.processa(any(), eq(TipoTransacao.TED), any(), any(), any()))
                .thenThrow(new RegraNegocioException("TED_FORA_DO_HORARIO", "TED fora do horário permitido"));

        mockMvc.perform(post("/api/v1/transacoes/ted")
                        .with(cliente())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HeadersHttp.IDEMPOTENCIA_HEADER, ID_IDEMPOTENCIA)
                        .content(transacaoRequisicaoJson("150.00", ID_CONTA_ORIGEM, "12345-6")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.codigoErro").value("TED_FORA_DO_HORARIO"));
    }

    @Test
    @DisplayName("deve retornar 201 quando TEF processado com sucesso")
    void deveRetornar201QuandoTefProcessadoComSucesso() throws Exception {
        Transacao transacao = transacao(TipoTransacao.TEF, StatusTransacao.COMPLETADA);
        when(processaTransacaoService.processa(any(), eq(TipoTransacao.TEF), any(), any(), any()))
                .thenReturn(transacao);

        mockMvc.perform(post("/api/v1/transacoes/tef")
                        .with(cliente())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HeadersHttp.IDEMPOTENCIA_HEADER, ID_IDEMPOTENCIA)
                        .content(transacaoRequisicaoJson("150.00", ID_CONTA_ORIGEM, "12345-6")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(ID_TRANSACAO.toString()));
    }

    @Test
    @DisplayName("deve retornar 200 quando transação encontrada")
    void deveRetornar200QuandoTransacaoEncontrada() throws Exception {
        when(consultaTransacaoService.consultar(ID_TRANSACAO))
                .thenReturn(transacao(TipoTransacao.PIX, StatusTransacao.COMPLETADA));

        mockMvc.perform(get("/api/v1/transacoes/{id}", ID_TRANSACAO)
                        .with(cliente()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ID_TRANSACAO.toString()));
    }

    @Test
    @DisplayName("deve retornar 404 quando transação não encontrada")
    void deveRetornar404QuandoTransacaoNaoEncontrada() throws Exception {
        when(consultaTransacaoService.consultar(ID_TRANSACAO))
                .thenThrow(new RecursoNaoEncontradoException("TRANSACAO_NAO_ENCONTRADA", "Transação não encontrada"));

        mockMvc.perform(get("/api/v1/transacoes/{id}", ID_TRANSACAO)
                        .with(cliente()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.codigoErro").value("TRANSACAO_NAO_ENCONTRADA"));
    }

    @Test
    @DisplayName("deve retornar 200 quando estorno com sucesso")
    void deveRetornar200QuandoEstornoComSucesso() throws Exception {
        EstornoResposta resposta = new EstornoResposta(
                ID_TRANSACAO,
                StatusTransacao.ESTORNADA,
                new BigDecimal("150.00"),
                CRIADO_EM);
        when(estornoTransacaoService.estornar(ID_TRANSACAO, "Transação não reconhecida"))
                .thenReturn(resposta);

        mockMvc.perform(post("/api/v1/transacoes/{id}/estorno", ID_TRANSACAO)
                        .with(gerente())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(estornoRequisicaoJson("Transação não reconhecida")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idTransacaoOriginal").value(ID_TRANSACAO.toString()));
    }

    @Test
    @DisplayName("deve retornar 422 quando transação não elegível")
    void deveRetornar422QuandoTransacaoNaoElegivel() throws Exception {
        when(estornoTransacaoService.estornar(ID_TRANSACAO, "Transação não reconhecida"))
                .thenThrow(new RegraNegocioException("ESTORNO_INVALIDO", "Transação não elegível para estorno"));

        mockMvc.perform(post("/api/v1/transacoes/{id}/estorno", ID_TRANSACAO)
                        .with(gerente())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(estornoRequisicaoJson("Transação não reconhecida")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.codigoErro").value("ESTORNO_INVALIDO"));
    }

    @Test
    @DisplayName("deve retornar 403 quando role insuficiente")
    void deveRetornar403QuandoRoleInsuficiente() throws Exception {
        mockMvc.perform(post("/api/v1/transacoes/pix")
                        .with(jwt().authorities(() -> "ROLE_INVALIDA"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HeadersHttp.IDEMPOTENCIA_HEADER, ID_IDEMPOTENCIA)
                        .content(transacaoRequisicaoJson("150.00", ID_CONTA_ORIGEM, "destino-pix")))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.codigoErro").value("ACESSO_NEGADO"));
    }

    @Test
    @DisplayName("deve retornar 422 com problem detail quando regra negocio exception")
    void deveRetornar422ComProblemDetailQuandoRegraNegocioException() throws Exception {
        when(processaTransacaoService.processa(any(), eq(TipoTransacao.PIX), any(), any(), any()))
                .thenThrow(new RegraNegocioException("REGRA_NEGOCIO", "Regra violada"));

        mockMvc.perform(post("/api/v1/transacoes/pix")
                        .with(cliente())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HeadersHttp.IDEMPOTENCIA_HEADER, ID_IDEMPOTENCIA)
                        .content(transacaoRequisicaoJson("150.00", ID_CONTA_ORIGEM, "destino-pix")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.codigoErro").value("REGRA_NEGOCIO"));
    }

    @Test
    @DisplayName("deve retornar 400 com problem detail quando method argument not valid exception")
    void deveRetornar400ComProblemDetailQuandoMethodArgumentNotValidException() throws Exception {
        mockMvc.perform(post("/api/v1/transacoes/pix")
                        .with(cliente())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HeadersHttp.IDEMPOTENCIA_HEADER, ID_IDEMPOTENCIA)
                        .content("""
                                {
                                  "valor": "0.00",
                                  "idContaOrigem": null,
                                  "contaDestino": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.Campos").isArray());
    }

    @Test
    @DisplayName("deve retornar 422 com problem detail quando saldo insuficiente exception")
    void deveRetornar422ComProblemDetailQuandoSaldoInsuficienteException() throws Exception {
        when(processaTransacaoService.processa(any(), eq(TipoTransacao.PIX), any(), any(), any()))
                .thenThrow(new SaldoInsuficienteException(ID_CONTA_ORIGEM, new BigDecimal("10.00"), new BigDecimal("150.00")));

        mockMvc.perform(post("/api/v1/transacoes/pix")
                        .with(cliente())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HeadersHttp.IDEMPOTENCIA_HEADER, ID_IDEMPOTENCIA)
                        .content(transacaoRequisicaoJson("150.00", ID_CONTA_ORIGEM, "destino-pix")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.codigoErro").value("SALDO_INSUFICIENTE"));
    }

    @Test
    @DisplayName("deve retornar 409 com problem detail quando object optimistic locking failure exception")
    void deveRetornar409ComProblemDetailQuandoObjectOptimisticLockingFailureException() throws Exception {
        when(estornoTransacaoService.estornar(ID_TRANSACAO, "Transação não reconhecida"))
                .thenThrow(new ObjectOptimisticLockingFailureException(Transacao.class, ID_TRANSACAO));

        mockMvc.perform(post("/api/v1/transacoes/{id}/estorno", ID_TRANSACAO)
                        .with(gerente())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(estornoRequisicaoJson("Transação não reconhecida")))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.codigoErro").value("CONFLITO_CONCORRENCIA"));
    }

    @Test
    @DisplayName("deve retornar 409 com problem detail quando data integrity violation exception")
    void deveRetornar409ComProblemDetailQuandoDataIntegrityViolationException() throws Exception {
        when(processaTransacaoService.processa(any(), eq(TipoTransacao.PIX), any(), any(), any()))
                .thenThrow(new DataIntegrityViolationException("Chave duplicada"));

        mockMvc.perform(post("/api/v1/transacoes/pix")
                        .with(cliente())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HeadersHttp.IDEMPOTENCIA_HEADER, ID_IDEMPOTENCIA)
                        .content(transacaoRequisicaoJson("150.00", ID_CONTA_ORIGEM, "destino-pix")))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.codigoErro").value("CONFLITO_DADOS"));
    }

    @Test
    @DisplayName("deve retornar 500 com problem detail quando exception genérica")
    void deveRetornar500ComProblemDetailQuandoExceptionGenerica() throws Exception {
        when(consultaTransacaoService.consultar(ID_TRANSACAO))
                .thenThrow(new RuntimeException("Erro inesperado"));

        mockMvc.perform(get("/api/v1/transacoes/{id}", ID_TRANSACAO)
                        .with(cliente()))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.codigoErro").value("ERRO_INTERNO_SERVIDOR"));
    }

    private static RequestPostProcessor cliente() {
        return jwt().authorities(() -> "ROLE_CLIENTE");
    }

    private static RequestPostProcessor gerente() {
        return jwt().authorities(() -> "ROLE_GERENTE");
    }

    private static Transacao transacao(TipoTransacao tipo, StatusTransacao status) {
        return Transacao.builder()
                .id(ID_TRANSACAO)
                .idCorrelacao(ID_CORRELACAO)
                .idIdempotencia(ID_IDEMPOTENCIA)
                .valor(ValorMonetario.paraReal(new BigDecimal("150.00")))
                .tipo(tipo)
                .status(status)
                .criadoEm(CRIADO_EM)
                .idContaOrigem(ID_CONTA_ORIGEM)
                .contaDestino("destino")
                .build();
    }

    private static String transacaoRequisicaoJson(String valor, UUID idContaOrigem, String contaDestino) {
        return """
                {
                  "valor": %s,
                  "idContaOrigem": "%s",
                  "contaDestino": "%s"
                }
                """.formatted(valor, idContaOrigem, contaDestino);
    }

    private static String estornoRequisicaoJson(String motivo) {
        return """
                {
                  "motivo": "%s"
                }
                """.formatted(motivo);
    }
}
