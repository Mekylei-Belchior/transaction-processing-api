package com.mekylei.transactionprocessing.configuracao.spring.filter;

import com.mekylei.transactionprocessing.configuracao.seguranca.RateLimitResposta;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final RateLimitResposta rateLimitResposta;
    private final int requisicoesPorMinuto;


    public RateLimitFilter(RateLimitResposta rateLimitResposta,
                           @Value("${app.rate-limit.requests-per-minute:60}") int requisicoesPorMinuto) {
        this.rateLimitResposta = rateLimitResposta;
        this.requisicoesPorMinuto = requisicoesPorMinuto;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String chave = this.resolverChaveCliente(request);
        Bucket bucket = buckets.computeIfAbsent(chave, k -> this.criarBucket());

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            rateLimitResposta.definir(response);
        }
    }

    private String resolverChaveCliente(HttpServletRequest request) {
        // Respeita X-Forwarded-For para ambientes com reverse proxy/load balancer
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            // Apenas o primeiro IP da cadeia é o cliente original
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private Bucket criarBucket() {
        Bandwidth bandwidth = Bandwidth.builder()
                .capacity(requisicoesPorMinuto)
                .refillGreedy(requisicoesPorMinuto, Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(bandwidth).build();
    }
}