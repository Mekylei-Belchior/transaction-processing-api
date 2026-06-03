package com.mekylei.transactionprocessing.compartilhado.exception;

import com.mekylei.transactionprocessing.compartilhado.util.DateTimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static com.mekylei.transactionprocessing.compartilhado.constantes.ProblemaDetailConstantes.CODIGO_ERRO_PROPERTY;
import static com.mekylei.transactionprocessing.compartilhado.constantes.ProblemaDetailConstantes.HORARIO_PROPERTY;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private ProblemDetail criaProblema(String titulo, HttpStatus status) {
        ProblemDetail problema = ProblemDetail.forStatus(status);
        problema.setTitle(titulo);
        problema.setProperty(HORARIO_PROPERTY, DateTimeUtil.agoraFormatoBr());

        return problema;
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ProblemDetail> trataRegraNegocioException(RegraNegocioException e) {
        logger.warn("Regra de negócio violada: codigo={} mensagem={}", e.getCodigoErro(), e.getMensagem());

        ProblemDetail problema = criaProblema("Violação de Regra de Negócio", HttpStatus.UNPROCESSABLE_CONTENT);
        problema.setDetail(e.getMensagem());
        problema.setProperty(CODIGO_ERRO_PROPERTY, e.getCodigoErro());

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(problema);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> trataValidationException(MethodArgumentNotValidException e) {
        logger.warn("Erro de validação: {}", e.getMessage());

        var campos = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();

        ProblemDetail problema = criaProblema("Dados Inválidos", HttpStatus.BAD_REQUEST);
        problema.setProperty("Campos", campos);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problema);
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ProblemDetail> trataRecursoNaoEncontrado(RecursoNaoEncontradoException e) {
        logger.error("Erro inesperado: {}", e.getMessage(), e);

        ProblemDetail problema = criaProblema("Recurso não encontrado", HttpStatus.NOT_FOUND);
        problema.setDetail(e.getMensagem());
        problema.setProperty(CODIGO_ERRO_PROPERTY, e.getCodigoErro());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problema);
    }

    @ExceptionHandler(SaldoInsuficienteException.class)
    public ResponseEntity<ProblemDetail> trataSaldoInsuficiente(SaldoInsuficienteException e) {
        logger.error("Erro inesperado: {}", e.getMessage(), e);

        ProblemDetail problema = criaProblema("Saldo insuficiente", HttpStatus.UNPROCESSABLE_CONTENT);
        problema.setDetail(e.getMensagem());
        problema.setProperty(CODIGO_ERRO_PROPERTY, e.getCodigoErro());

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(problema);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> trataViolacaoIntegridade(DataIntegrityViolationException e) {
        logger.warn("Violação de integridade de dados: {}", e.getMostSpecificCause().getMessage());

        ProblemDetail problema = criaProblema("Conflito de Dados", HttpStatus.CONFLICT);
        problema.setDetail("A requisição conflita com um registro existente. Verifique a chave de idempotência.");
        problema.setProperty(CODIGO_ERRO_PROPERTY, "CONFLITO_DADOS");

        return ResponseEntity.status(HttpStatus.CONFLICT).body(problema);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ProblemDetail> trataConflitoConcorrencia(ObjectOptimisticLockingFailureException e) {
        logger.warn("Conflito de concorrência ao atualizar recurso: {}", e.getMessage());

        ProblemDetail problema = criaProblema("Conflito de Concorrência", HttpStatus.CONFLICT);
        problema.setDetail("O recurso foi atualizado por outra requisição concorrente. Tente consultar o estado atual antes de repetir a operação.");
        problema.setProperty(CODIGO_ERRO_PROPERTY, "CONFLITO_CONCORRENCIA");

        return ResponseEntity.status(HttpStatus.CONFLICT).body(problema);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ProblemDetail> trataCabecalhoObrigatorioAusente(MissingRequestHeaderException e) {
        logger.warn("Cabeçalho obrigatório ausente: {}", e.getHeaderName());

        ProblemDetail problema = criaProblema("Cabeçalho obrigatório ausente", HttpStatus.BAD_REQUEST);
        problema.setDetail("O cabeçalho '" + e.getHeaderName() + "' é obrigatório.");
        problema.setProperty(CODIGO_ERRO_PROPERTY, "CABECALHO_AUSENTE");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problema);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> trataAcessoNegado(AccessDeniedException e) {
        logger.warn("Acesso negado: {}", e.getMessage());

        ProblemDetail problema = criaProblema("Acesso negado", HttpStatus.FORBIDDEN);
        problema.setDetail("Sem permissão para executar esta operação.");
        problema.setProperty(CODIGO_ERRO_PROPERTY, "ACESSO_NEGADO");

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problema);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> trataErroGenericoException(Exception e) {
        logger.error("Erro inesperado: {}", e.getMessage(), e);

        ProblemDetail problema = criaProblema("Erro Interno", HttpStatus.INTERNAL_SERVER_ERROR);
        problema.setDetail("Um erro inesperado ocorreu. Consulte o suporte com o horário da ocorrência.");
        problema.setProperty(CODIGO_ERRO_PROPERTY, "ERRO_INTERNO_SERVIDOR");

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problema);
    }

}
