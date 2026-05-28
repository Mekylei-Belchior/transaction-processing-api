package com.mekylei.transactionprocessing.observabilidade.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import com.mekylei.transactionprocessing.observabilidade.mascaramento.estrategia.MascaraStrategy;
import com.mekylei.transactionprocessing.observabilidade.mascaramento.estrategia.StrategyMascaramentoResolver;
import net.logstash.logback.composite.AbstractJsonProvider;
import net.logstash.logback.composite.JsonWritingUtils;
import tools.jackson.core.JsonGenerator;

public class MascaradoJsonProvider extends AbstractJsonProvider<ILoggingEvent> {

    private String fieldName;
    private TipoCampoMascarado tipo;

    @Override
    public void writeTo(JsonGenerator generator, ILoggingEvent event) {
        String valor = extrairValor(event);

        if (valor == null || valor.isBlank()) {
            return;
        }

        MascaraStrategy strategy = StrategyMascaramentoResolver.resolve(tipo);
        valor = strategy.mascarar(valor);

        JsonWritingUtils.writeStringField(generator, fieldName, valor);
    }

    private String extrairValor(ILoggingEvent event) {
        if (tipo == null) {
            return null;
        }

        return switch (tipo) {
            case MESSAGE, JSON, HEADER -> event.getFormattedMessage();
            case STACKTRACE -> {
                if (event.getThrowableProxy() == null) {
                    yield null;
                }
                yield ThrowableProxyUtil.asString(event.getThrowableProxy());
            }
        };
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public void setTipo(String tipo) {
        this.tipo = TipoCampoMascarado.valueOf(tipo.toUpperCase());
    }
}