package com.mekylei.transactionprocessing.compartilhado.exception;

public class RegraNegocioException extends RuntimeException {
    private final String codigoErro;

    public RegraNegocioException(String codigoErro, String mensagem) {
        super(mensagem);
        this.codigoErro = codigoErro;
    }

    public RegraNegocioException(String codigoErro, String mensagem, Throwable causa) {
        super(mensagem, causa);
        this.codigoErro = codigoErro;
    }

    public String getCodigoErro() {
        return codigoErro;
    }

    public String getMensagem() {
        return getMessage();
    }
}
