package com.mekylei.transactionprocessing.compartilhado.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitários para {@link CorrelacaoUtil}.
 *
 * <p>Objetivo:</p>
 * <ul>
 *     <li>Validar o comportamento esperado de {@link CorrelacaoUtil} nos cenários exercitados pela suíte.</li>
 *     <li>Preservar regras de negócio, contratos, integrações ou invariantes aplicáveis à classe testada.</li>
 *     <li>Garantir regressão funcional para alterações futuras relacionadas a {@code CorrelacaoUtil}.</li>
 * </ul>
 *
 * <p>Cenários cobertos:</p>
 * <ul>
 *     <li>Definir deve armazenar e retornar UUID.</li>
 *     <li>Obter deve retornar null quando não definido.</li>
 *     <li>Remover deve limpar o valor armazenado.</li>
 *     <li>Definir com null deve gerar novo UUID.</li>
 *     <li>Deve isolamento de threads.</li>
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
@DisplayName("Correlacao Util")
class CorrelacaoUtilTest {

    @AfterEach
    void tearDown() {
        CorrelacaoUtil.remover();
    }

    @Test
    @DisplayName("definir deve armazenar e retornar UUID")
    void definir_deveArmazenarERetornarUUID() {
        UUID idCorrelacao = UUID.randomUUID();

        UUID resultado = CorrelacaoUtil.definir(idCorrelacao);

        assertThat(resultado).isEqualTo(idCorrelacao);
        assertThat(CorrelacaoUtil.obter()).isEqualTo(idCorrelacao);
    }

    @Test
    @DisplayName("obter deve retornar null quando não definido")
    void obter_deveRetornarNullQuandoNaoDefinido() {
        CorrelacaoUtil.remover();

        assertThat(CorrelacaoUtil.obter()).isNull();
    }

    @Test
    @DisplayName("remover deve limpar o valor armazenado")
    void remover_deveLimparOValorArmazenado() {
        CorrelacaoUtil.definir(UUID.randomUUID());

        CorrelacaoUtil.remover();

        assertThat(CorrelacaoUtil.obter()).isNull();
    }

    @Test
    @DisplayName("definir com null deve gerar novo UUID")
    void definir_comNullDeveGerarNovoUUID() {
        UUID resultado = CorrelacaoUtil.definir(null);

        assertThat(resultado).isNotNull();
        assertThat(CorrelacaoUtil.obter()).isEqualTo(resultado);
    }

    @Test
    @DisplayName("deve isolamento de threads")
    void isolamentoDeThreads() throws InterruptedException {
        UUID idThreadA = UUID.randomUUID();
        UUID idThreadB = UUID.randomUUID();
        CountDownLatch ambasDefiniram = new CountDownLatch(2);
        CountDownLatch liberarLeitura = new CountDownLatch(1);
        AtomicReference<UUID> resultadoThreadA = new AtomicReference<>();
        AtomicReference<UUID> resultadoThreadB = new AtomicReference<>();

        Thread threadA = new Thread(() -> definirELer(idThreadA, ambasDefiniram, liberarLeitura, resultadoThreadA));
        Thread threadB = new Thread(() -> definirELer(idThreadB, ambasDefiniram, liberarLeitura, resultadoThreadB));

        threadA.start();
        threadB.start();
        ambasDefiniram.await();
        liberarLeitura.countDown();
        threadA.join();
        threadB.join();

        assertThat(resultadoThreadA.get()).isEqualTo(idThreadA);
        assertThat(resultadoThreadB.get()).isEqualTo(idThreadB);
    }

    private void definirELer(UUID idCorrelacao,
                             CountDownLatch ambasDefiniram,
                             CountDownLatch liberarLeitura,
                             AtomicReference<UUID> resultado) {
        try {
            CorrelacaoUtil.definir(idCorrelacao);
            ambasDefiniram.countDown();
            liberarLeitura.await();
            resultado.set(CorrelacaoUtil.obter());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            CorrelacaoUtil.remover();
        }
    }
}
