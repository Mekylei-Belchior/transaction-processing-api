package com.mekylei.transactionprocessing.compartilhado.exception;

public class RegraNegocioException extends ApiException {

    public RegraNegocioException(String codigoErro, String mensagem) {
        super(codigoErro, mensagem);
    }

    public RegraNegocioException(String codigoErro, String mensagem, Throwable causa) {
        super(codigoErro, mensagem, causa);
    }

}
