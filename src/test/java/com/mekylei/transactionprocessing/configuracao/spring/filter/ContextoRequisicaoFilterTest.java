package com.mekylei.transactionprocessing.configuracao.spring.filter;

import com.mekylei.transactionprocessing.auditoria.DadosAuditoria;
import com.mekylei.transactionprocessing.auditoria.porta.AuditoriaContextWriter;
import com.mekylei.transactionprocessing.compartilhado.constantes.HeadersHttp;
import com.mekylei.transactionprocessing.compartilhado.util.CorrelacaoUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ContextoRequisicaoFilterTest {

    @Mock
    private AuditoriaContextWriter auditoriaContextWriter;

    @Mock
    private FilterChain filterChain;

    private ContextoRequisicaoFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        filter = new ContextoRequisicaoFilter(auditoriaContextWriter);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @AfterEach
    void tearDown() {
        CorrelacaoUtil.remover();
        SecurityContextHolder.clearContext();
    }

    @Test
    void deveGerarNovoIdCorrelacaoQuandoHeaderAusente() throws ServletException, IOException {
        AtomicReference<UUID> idDuranteFiltro = capturarCorrelacaoDuranteFilterChain();

        filter.doFilter(request, response, filterChain);

        assertThat(idDuranteFiltro.get()).isNotNull();
    }

    @Test
    void deveUsarIdCorrelacaoDoHeaderQuandoPresente() throws ServletException, IOException {
        UUID idCorrelacao = UUID.randomUUID();
        request.addHeader(HeadersHttp.CORRELACAO_HEADER, idCorrelacao.toString());
        AtomicReference<UUID> idDuranteFiltro = capturarCorrelacaoDuranteFilterChain();

        filter.doFilter(request, response, filterChain);

        assertThat(idDuranteFiltro.get()).isEqualTo(idCorrelacao);
    }

    @Test
    void deveIgnorarIdCorrelacaoInvalidoEGerarNovo() throws ServletException, IOException {
        request.addHeader(HeadersHttp.CORRELACAO_HEADER, "nao-e-uuid");
        AtomicReference<UUID> idDuranteFiltro = capturarCorrelacaoDuranteFilterChain();

        filter.doFilter(request, response, filterChain);

        assertThat(idDuranteFiltro.get()).isNotNull();
    }

    @Test
    void deveLimparCorrelacaoAposRequisicao() throws ServletException, IOException {
        filter.doFilter(request, response, filterChain);

        assertThat(CorrelacaoUtil.obter()).isNull();
    }

    @Test
    void deveExtrairIpDoXForwardedForQuandoPresente() throws ServletException, IOException {
        request.addHeader(HeadersHttp.IP_ORIGEM_HEADER, "10.0.0.1, 192.168.0.1");

        filter.doFilter(request, response, filterChain);

        ArgumentCaptor<DadosAuditoria> captor = ArgumentCaptor.forClass(DadosAuditoria.class);
        verify(auditoriaContextWriter).definir(captor.capture());
        assertThat(captor.getValue().ipOrigem()).isEqualTo("10.0.0.1");
    }

    @Test
    void deveUsarRemoteAddrQuandoXForwardedForAusente() throws ServletException, IOException {
        request.setRemoteAddr("172.16.0.10");

        filter.doFilter(request, response, filterChain);

        ArgumentCaptor<DadosAuditoria> captor = ArgumentCaptor.forClass(DadosAuditoria.class);
        verify(auditoriaContextWriter).definir(captor.capture());
        assertThat(captor.getValue().ipOrigem()).isEqualTo("172.16.0.10");
    }

    @Test
    void devePropagamentoDoFilterChainMesmoSeLancarExcecao() throws ServletException, IOException {
        doThrow(new ServletException("falha no chain"))
                .when(filterChain)
                .doFilter(request, response);

        assertThatThrownBy(() -> filter.doFilter(request, response, filterChain))
                .isInstanceOf(ServletException.class)
                .hasMessage("falha no chain");
        assertThat(CorrelacaoUtil.obter()).isNull();
    }

    private AtomicReference<UUID> capturarCorrelacaoDuranteFilterChain() throws ServletException, IOException {
        AtomicReference<UUID> idDuranteFiltro = new AtomicReference<>();
        doAnswer(invocation -> {
            idDuranteFiltro.set(CorrelacaoUtil.obter());
            return null;
        }).when(filterChain).doFilter(request, response);
        return idDuranteFiltro;
    }
}
