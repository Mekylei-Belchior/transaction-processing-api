package com.mekylei.transactionprocessing.configuracao.spring.filter;

import com.mekylei.transactionprocessing.compartilhado.constantes.HttpConstantes;
import com.mekylei.transactionprocessing.compartilhado.util.CorrelacaoUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ContextoRequisicaoFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String headerValue = request.getHeader(HttpConstantes.CORRELACAO_HEADER);
        if (headerValue != null && !headerValue.isBlank()) {
            CorrelacaoUtil.definir(UUID.fromString(headerValue));
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            CorrelacaoUtil.remover();
        }
    }
}
