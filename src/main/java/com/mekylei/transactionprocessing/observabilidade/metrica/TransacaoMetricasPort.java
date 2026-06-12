package com.mekylei.transactionprocessing.observabilidade.metrica;

import com.mekylei.transactionprocessing.compartilhado.dominio.TipoTransacao;
import com.mekylei.transactionprocessing.transacao.dominio.StatusTransacao;
import io.micrometer.core.instrument.Timer;

import java.math.BigDecimal;

public interface TransacaoMetricasPort {

    Timer.Sample iniciarSample();

    void registrarDuracao(TipoTransacao tipo, Timer.Sample sample);

    void registrarTransacaoCriada(TipoTransacao tipo, StatusTransacao status);

    void registrarTransacaoProcessada(TipoTransacao tipo, StatusTransacao status);

    void registrarValor(TipoTransacao tipo, BigDecimal valor);

    void registrarSaldoInsuficiente();

    void registrarLimiteExcedido(TipoTransacao tipo);

    void registrarIdempotenciaHit();
}
