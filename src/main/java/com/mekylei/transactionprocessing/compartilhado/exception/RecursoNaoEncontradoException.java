package com.mekylei.transactionprocessing.compartilhado.exception;

public class RecursoNaoEncontradoException extends ApiException {

    public RecursoNaoEncontradoException(String codigoErro, String mensagem) {
        super(codigoErro, mensagem);
    }

    public RecursoNaoEncontradoException(String codigoErro, String mensagem, Throwable causa) {
        super(codigoErro, mensagem, causa);
    }

}
