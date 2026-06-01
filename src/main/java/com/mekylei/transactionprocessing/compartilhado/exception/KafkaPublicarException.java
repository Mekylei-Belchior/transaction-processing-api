package com.mekylei.transactionprocessing.compartilhado.exception;

public class KafkaPublicarException extends ApiException {

    public KafkaPublicarException(String codigoErro, String mensagem) {
        super(codigoErro, mensagem);
    }

    public KafkaPublicarException(String codigoErro, String mensagem, Throwable causa) {
        super(codigoErro, mensagem, causa);
    }
}
