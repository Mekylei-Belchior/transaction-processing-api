package com.mekylei.transactionprocessing.observabilidade.logging;

import ch.qos.logback.classic.pattern.MessageConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.mekylei.transactionprocessing.observabilidade.mascaramento.DadosSensiveisMasker;

public class LogMascaramentoConverter extends MessageConverter {

    @Override
    public String convert(ILoggingEvent event) {

        String mensagemOriginal = event.getFormattedMessage();

        return DadosSensiveisMasker.mascarar(mensagemOriginal);
    }
}