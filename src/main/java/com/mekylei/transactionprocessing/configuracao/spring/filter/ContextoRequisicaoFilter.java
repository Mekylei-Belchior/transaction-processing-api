package com.mekylei.transactionprocessing.configuracao.spring.filter;

import com.mekylei.transactionprocessing.auditoria.DadosAuditoria;
import com.mekylei.transactionprocessing.auditoria.porta.AuditoriaContextWriter;
import com.mekylei.transactionprocessing.compartilhado.constantes.HeadersHttp;
import com.mekylei.transactionprocessing.compartilhado.util.CorrelacaoUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class ContextoRequisicaoFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(ContextoRequisicaoFilter.class);

    private final AuditoriaContextWriter auditoriaContextWriter;

    public ContextoRequisicaoFilter(AuditoriaContextWriter auditoriaContextWriter) {
        this.auditoriaContextWriter = auditoriaContextWriter;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String ipOrigem = resolverIpOrigem(request);
        UUID idCorrelacao = CorrelacaoUtil.definir(resolverCorrelacao(request));
        Optional<UUID> idOperador = resolverIdOperador();

        auditoriaContextWriter.definir(new DadosAuditoria(ipOrigem, idCorrelacao, idOperador));

        try {
            filterChain.doFilter(request, response);
        } finally {
            CorrelacaoUtil.remover();
        }
    }

    private String resolverIpOrigem(HttpServletRequest request) {
        String forwardedFor = request.getHeader(HeadersHttp.IP_ORIGEM_HEADER);
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private UUID resolverCorrelacao(HttpServletRequest request) {
        String correlationId = request.getHeader(HeadersHttp.CORRELACAO_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(correlationId);
        } catch (IllegalArgumentException e) {
            logger.warn("X-Correlation-Id inválido recebido, ignorando: valor='{}'", correlationId);
            return null;
        }
    }

    private Optional<UUID> resolverIdOperador() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            try {
                return Optional.of(UUID.fromString(jwt.getSubject()));
            } catch (IllegalArgumentException e) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }
}
