package com.mekylei.transactionprocessing.configuracao.spring.filter;

import com.mekylei.transactionprocessing.compartilhado.util.CorrelacaoIdUtil;
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
public class CorrelacaoIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String headerValue = request.getHeader(CorrelacaoIdUtil.CORRELACAO_HEADER);
        if (headerValue != null && !headerValue.isBlank()) {
            CorrelacaoIdUtil.set(UUID.fromString(headerValue));
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            CorrelacaoIdUtil.remover();
        }
    }
}
