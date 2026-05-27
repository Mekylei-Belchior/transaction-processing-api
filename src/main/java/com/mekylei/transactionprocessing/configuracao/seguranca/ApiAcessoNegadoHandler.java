package com.mekylei.transactionprocessing.configuracao.seguranca;


import com.mekylei.transactionprocessing.compartilhado.util.DateTimeUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

import static com.mekylei.transactionprocessing.compartilhado.constantes.ProblemaDetailConstantes.CODIGO_ERRO_PROPERTY;
import static com.mekylei.transactionprocessing.compartilhado.constantes.ProblemaDetailConstantes.HORARIO_PROPERTY;

@Component
public class ApiAcessoNegadoHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public ApiAcessoNegadoHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(@NonNull HttpServletRequest request,
                       HttpServletResponse response,
                       @NonNull AccessDeniedException exception) throws IOException {

        ProblemDetail problema = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        problema.setTitle("Acesso negado");
        problema.setDetail("Sem permissão para executar esta operação.");
        problema.setProperty(HORARIO_PROPERTY, DateTimeUtil.agoraFormatoBr());
        problema.setProperty(CODIGO_ERRO_PROPERTY, "ACESSO_NEGADO");

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        objectMapper.writeValue(response.getOutputStream(), problema);
    }
}