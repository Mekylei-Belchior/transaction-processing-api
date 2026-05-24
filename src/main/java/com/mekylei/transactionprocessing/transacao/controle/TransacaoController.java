package com.mekylei.transactionprocessing.transacao.controle;

import com.mekylei.transactionprocessing.transacao.aplicacao.servico.ConsultaTransacaoService;
import com.mekylei.transactionprocessing.transacao.aplicacao.servico.ProcessaTransacaoService;
import com.mekylei.transactionprocessing.transacao.controle.dto.TransacaoRequisicao;
import com.mekylei.transactionprocessing.transacao.controle.dto.TransacaoResposta;
import com.mekylei.transactionprocessing.transacao.dominio.TipoTransacao;
import com.mekylei.transactionprocessing.transacao.dominio.Transacao;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transacoes")
@Tag(name = "Transações", description = "Endpoints para processamento de transações bancárias")
public class TransacaoController {

    private final ProcessaTransacaoService processaTransacaoService;
    private final ConsultaTransacaoService consultaTransacaoService;

    public TransacaoController(ProcessaTransacaoService processaTransacaoService,
                               ConsultaTransacaoService consultaTransacaoService) {
        this.processaTransacaoService = processaTransacaoService;
        this.consultaTransacaoService = consultaTransacaoService;
    }

    @PostMapping("/pix")
    @Operation(
            summary = "Processar PIX",
            description = "Realiza uma transação PIX. Usa contaDestino como chave PIX.")
    public ResponseEntity<TransacaoResposta> processaPix(
            @Valid @RequestBody TransacaoRequisicao requisicao,
            @RequestHeader(value = "X-Idempotency-Key") UUID idIdempotencia) {
        Transacao transacao = processaTransacaoService.processa(
                requisicao.valor(),
                TipoTransacao.PIX,
                requisicao.idContaOrigem(),
                requisicao.contaDestino(),
                idIdempotencia);

        return ResponseEntity.status(HttpStatus.CREATED).body(TransacaoResposta.aPartirDe(transacao));
    }

    @PostMapping("/ted")
    @Operation(
            summary = "Processar TED",
            description = "Realiza uma TED. Disponível apenas em horário bancário (06h-17h BRT).")
    public ResponseEntity<TransacaoResposta> processaTed(
            @Valid @RequestBody TransacaoRequisicao requisicao,
            @RequestHeader(value = "X-Idempotency-Key") UUID idIdempotencia) {
        Transacao transacao = processaTransacaoService.processa(
                requisicao.valor(),
                TipoTransacao.TED,
                requisicao.idContaOrigem(),
                requisicao.contaDestino(),
                idIdempotencia);

        return ResponseEntity.status(HttpStatus.CREATED).body(TransacaoResposta.aPartirDe(transacao));
    }

    @PostMapping("/tef")
    @Operation(
            summary = "Processar TEF",
            description = "Realiza uma TEF entre contas do mesmo banco. Requer autorização antifraude.")
    public ResponseEntity<TransacaoResposta> processaTef(
            @Valid @RequestBody TransacaoRequisicao requisicao,
            @RequestHeader(value = "X-Idempotency-Key") UUID idIdempotencia) {
        Transacao transacao = processaTransacaoService.processa(
                requisicao.valor(),
                TipoTransacao.TEF,
                requisicao.idContaOrigem(),
                requisicao.contaDestino(),
                idIdempotencia);

        return ResponseEntity.status(HttpStatus.CREATED).body(TransacaoResposta.aPartirDe(transacao));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Consultar status de transação",
            description = "Consultar o estado atual de uma transação por ID")
    public ResponseEntity<TransacaoResposta> consultaStatus(@PathVariable UUID id) {
        Transacao transacao = consultaTransacaoService.consultar(id);
        return ResponseEntity.ok(TransacaoResposta.aPartirDe(transacao));
    }
}
