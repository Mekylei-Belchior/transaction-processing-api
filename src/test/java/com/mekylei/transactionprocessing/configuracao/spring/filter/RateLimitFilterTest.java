package com.mekylei.transactionprocessing.configuracao.spring.filter;

import com.mekylei.transactionprocessing.configuracao.seguranca.RateLimitResposta;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Testes unitários para {@link RateLimitFilter}.
 *
 * <p>Objetivo:</p>
 * <ul>
 *     <li>Validar o comportamento esperado de {@link RateLimitFilter} nos cenários exercitados pela suíte.</li>
 *     <li>Preservar regras de negócio, contratos, integrações ou invariantes aplicáveis à classe testada.</li>
 *     <li>Garantir regressão funcional para alterações futuras relacionadas a {@code RateLimitFilter}.</li>
 * </ul>
 *
 * <p>Cenários cobertos:</p>
 * <ul>
 *     <li>Deve permitir requisição dentro do limite.</li>
 *     <li>Deve bloquear requisição quando limite excedido.</li>
 *     <li>Deve contar separadamente por IP.</li>
 *     <li>Deve usar X-Forwarded-For como chave.</li>
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
@ExtendWith(MockitoExtension.class)
@DisplayName("Rate Limit Filter")
class RateLimitFilterTest {

    @Mock
    private RateLimitResposta rateLimitResposta;

    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RateLimitFilter(rateLimitResposta, 1);
    }

    @Test
    @DisplayName("deve permitir requisição dentro do limite")
    void devePermitirRequisicaoDentroDoLimite() throws ServletException, IOException {
        MockHttpServletRequest request = requestComRemoteAddr("10.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(rateLimitResposta, never()).definir(response);
    }

    @Test
    @DisplayName("deve bloquear requisição quando limite excedido")
    void deveBloquearRequisicaoQuandoLimiteExcedido() throws ServletException, IOException {
        MockHttpServletRequest primeiraRequest = requestComRemoteAddr("10.0.0.1");
        MockHttpServletRequest segundaRequest = requestComRemoteAddr("10.0.0.1");
        MockHttpServletResponse primeiraResponse = new MockHttpServletResponse();
        MockHttpServletResponse segundaResponse = new MockHttpServletResponse();
        FilterChain primeiroChain = mock(FilterChain.class);
        FilterChain segundoChain = mock(FilterChain.class);

        filter.doFilter(primeiraRequest, primeiraResponse, primeiroChain);
        filter.doFilter(segundaRequest, segundaResponse, segundoChain);

        verify(primeiroChain).doFilter(primeiraRequest, primeiraResponse);
        verify(segundoChain, never()).doFilter(segundaRequest, segundaResponse);
        verify(rateLimitResposta).definir(segundaResponse);
    }

    @Test
    @DisplayName("deve contar separadamente por IP")
    void deveContarSeparadamentePorIP() throws ServletException, IOException {
        MockHttpServletRequest requestIpA = requestComRemoteAddr("10.0.0.1");
        MockHttpServletRequest requestIpB = requestComRemoteAddr("10.0.0.2");
        MockHttpServletResponse responseIpA = new MockHttpServletResponse();
        MockHttpServletResponse responseIpB = new MockHttpServletResponse();
        FilterChain chainIpA = mock(FilterChain.class);
        FilterChain chainIpB = mock(FilterChain.class);

        filter.doFilter(requestIpA, responseIpA, chainIpA);
        filter.doFilter(requestIpB, responseIpB, chainIpB);

        verify(chainIpA).doFilter(requestIpA, responseIpA);
        verify(chainIpB).doFilter(requestIpB, responseIpB);
    }

    @Test
    @DisplayName("deve usar X-Forwarded-For como chave")
    void deveUsarXForwardedForComoChave() throws ServletException, IOException {
        MockHttpServletRequest requestIpA = requestComRemoteAddr("192.168.0.10");
        MockHttpServletRequest requestIpB = requestComRemoteAddr("192.168.0.10");
        requestIpA.addHeader("X-Forwarded-For", "10.0.0.1, 192.168.0.1");
        requestIpB.addHeader("X-Forwarded-For", "10.0.0.2, 192.168.0.1");
        MockHttpServletResponse responseIpA = new MockHttpServletResponse();
        MockHttpServletResponse responseIpB = new MockHttpServletResponse();
        FilterChain chainIpA = mock(FilterChain.class);
        FilterChain chainIpB = mock(FilterChain.class);

        filter.doFilter(requestIpA, responseIpA, chainIpA);
        filter.doFilter(requestIpB, responseIpB, chainIpB);

        verify(chainIpA).doFilter(requestIpA, responseIpA);
        verify(chainIpB).doFilter(requestIpB, responseIpB);
    }

    private MockHttpServletRequest requestComRemoteAddr(String remoteAddr) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddr);
        return request;
    }
}
