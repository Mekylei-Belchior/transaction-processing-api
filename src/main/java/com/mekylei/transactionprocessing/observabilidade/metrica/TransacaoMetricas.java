package com.mekylei.transactionprocessing.observabilidade.metrica;

import com.mekylei.transactionprocessing.compartilhado.dominio.TipoTransacao;
import com.mekylei.transactionprocessing.transacao.dominio.StatusTransacao;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class TransacaoMetricas implements TransacaoMetricasPort {

    private final MeterRegistry meterRegistry;

    public TransacaoMetricas(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public Timer.Sample iniciarSample() {
        return Timer.start(meterRegistry);
    }

    @Override
    public void registrarDuracao(TipoTransacao tipo, Timer.Sample sample) {
        Timer timer = Timer.builder("transacao.duracao")
                .tag("tipo", tipo.name())
                .publishPercentileHistogram()
                .register(meterRegistry);
        sample.stop(timer);
    }

    @Override
    public void registrarTransacaoCriada(TipoTransacao tipo, StatusTransacao status) {
        Counter.builder("transacao.criada")
                .tag("tipo", tipo.name())
                .tag("status", status.name())
                .register(meterRegistry)
                .increment();
    }

    @Override
    public void registrarTransacaoProcessada(TipoTransacao tipo, StatusTransacao status) {
        Counter.builder("transacao.processada")
                .tag("tipo", tipo.name())
                .tag("status", status.name())
                .register(meterRegistry)
                .increment();
    }

    @Override
    public void registrarValor(TipoTransacao tipo, BigDecimal valor) {
        DistributionSummary.builder("transacao.valor")
                .tag("tipo", tipo.name())
                .baseUnit("BRL")
                .register(meterRegistry)
                .record(valor.doubleValue());
    }

    @Override
    public void registrarSaldoInsuficiente() {
        Counter.builder("saldo.insuficiente")
                .register(meterRegistry)
                .increment();
    }

    @Override
    public void registrarLimiteExcedido(TipoTransacao tipo) {
        Counter.builder("limite.excedido")
                .tag("tipo", tipo.name())
                .register(meterRegistry)
                .increment();
    }

    @Override
    public void registrarIdempotenciaHit() {
        Counter.builder("idempotencia.cache.hit")
                .register(meterRegistry)
                .increment();
    }
}
