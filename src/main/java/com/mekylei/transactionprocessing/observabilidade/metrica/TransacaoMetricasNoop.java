package com.mekylei.transactionprocessing.observabilidade.metrica;

import com.mekylei.transactionprocessing.compartilhado.dominio.TipoTransacao;
import com.mekylei.transactionprocessing.transacao.dominio.StatusTransacao;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import java.math.BigDecimal;

public class TransacaoMetricasNoop implements TransacaoMetricasPort {

    @Override
    public Timer.Sample iniciarSample() {
        return Timer.start(new SimpleMeterRegistry());
    }

    @Override
    public void registrarDuracao(TipoTransacao tipo, Timer.Sample sample) {
    }

    @Override
    public void registrarTransacaoCriada(TipoTransacao tipo, StatusTransacao status) {
    }

    @Override
    public void registrarTransacaoProcessada(TipoTransacao tipo, StatusTransacao status) {
    }

    @Override
    public void registrarValor(TipoTransacao tipo, BigDecimal valor) {
    }

    @Override
    public void registrarSaldoInsuficiente() {
    }

    @Override
    public void registrarLimiteExcedido(TipoTransacao tipo) {
    }

    @Override
    public void registrarIdempotenciaHit() {
    }
}
