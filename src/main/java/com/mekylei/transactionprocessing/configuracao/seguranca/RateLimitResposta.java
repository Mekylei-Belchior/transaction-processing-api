package com.mekylei.transactionprocessing.configuracao.seguranca;


import com.mekylei.transactionprocessing.compartilhado.util.DateTimeUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

import static com.mekylei.transactionprocessing.compartilhado.constantes.ProblemaDetailConstantes.CODIGO_ERRO_PROPERTY;
import static com.mekylei.transactionprocessing.compartilhado.constantes.ProblemaDetailConstantes.HORARIO_PROPERTY;

@Component
public class RateLimitResposta {

    private final ObjectMapper objectMapper;

    public RateLimitResposta(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void definir(HttpServletResponse response) throws IOException {
        ProblemDetail problema = ProblemDetail.forStatus(HttpStatus.TOO_MANY_REQUESTS);
        problema.setTitle("Limite excedido");
        problema.setDetail("Limite de requisições excedido. Tente novamente em instantes.");
        problema.setProperty(HORARIO_PROPERTY, DateTimeUtil.agoraFormatoBr());
        problema.setProperty(CODIGO_ERRO_PROPERTY, "LIMITE_EXCEDIDO");

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", "60");

        objectMapper.writeValue(response.getOutputStream(), problema);
    }
}
