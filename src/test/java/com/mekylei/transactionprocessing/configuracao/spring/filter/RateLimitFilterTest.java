package com.mekylei.transactionprocessing.configuracao.spring.filter;

import com.mekylei.transactionprocessing.configuracao.seguranca.RateLimitResposta;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock
    private RateLimitResposta rateLimitResposta;

    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RateLimitFilter(rateLimitResposta, 1);
    }

    @Test
    void devePermitirRequisicaoDentroDoLimite() throws ServletException, IOException {
        MockHttpServletRequest request = requestComRemoteAddr("10.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(rateLimitResposta, never()).definir(response);
    }

    @Test
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
