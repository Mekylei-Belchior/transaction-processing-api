package com.mekylei.transactionprocessing.compartilhado.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelacaoUtilTest {

    @AfterEach
    void tearDown() {
        CorrelacaoUtil.remover();
    }

    @Test
    void definir_deveArmazenarERetornarUUID() {
        UUID idCorrelacao = UUID.randomUUID();

        UUID resultado = CorrelacaoUtil.definir(idCorrelacao);

        assertThat(resultado).isEqualTo(idCorrelacao);
        assertThat(CorrelacaoUtil.obter()).isEqualTo(idCorrelacao);
    }

    @Test
    void obter_deveRetornarNullQuandoNaoDefinido() {
        CorrelacaoUtil.remover();

        assertThat(CorrelacaoUtil.obter()).isNull();
    }

    @Test
    void remover_deveLimparOValorArmazenado() {
        CorrelacaoUtil.definir(UUID.randomUUID());

        CorrelacaoUtil.remover();

        assertThat(CorrelacaoUtil.obter()).isNull();
    }

    @Test
    void definir_comNullDeveGerarNovoUUID() {
        UUID resultado = CorrelacaoUtil.definir(null);

        assertThat(resultado).isNotNull();
        assertThat(CorrelacaoUtil.obter()).isEqualTo(resultado);
    }

    @Test
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
