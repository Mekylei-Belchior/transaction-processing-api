package com.mekylei.transactionprocessing.compartilhado.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RegraNegocioException.class)
    public ProblemDetail trataRegraNegocioException(RegraNegocioException e) {
        logger.warn("Regra de negócio violada: codigo={} mensagem={}", e.getCodigoErro(), e.getMensagem());

        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, e.getMensagem());
        problema.setTitle("Violação de Regra de Negócio");
        problema.setProperty("Código de Erro", e.getCodigoErro());
        problema.setProperty("Horário", Instant.now());

        return problema;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail trataValidationException(MethodArgumentNotValidException e) {
        logger.warn("Erro de validação: {}", e.getMessage());

        var campos = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();

        ProblemDetail problema = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problema.setTitle("Dados Inválidos");
        problema.setProperty("Campos", campos);
        problema.setProperty("Horário", Instant.now());

        return problema;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail trataErroGenericoException(Exception e) {
        logger.error("Erro inesperado: {}", e.getMessage(), e);

        ProblemDetail problema = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problema.setTitle("Erro Interno");
        problema.setDetail("Um erro inesperado ocorreu. Consulte o suporte com o horário da ocorrência.");
        problema.setProperty("Horário da ocorrência", Instant.now());

        return problema;
    }
}
