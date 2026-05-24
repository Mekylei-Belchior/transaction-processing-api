package com.mekylei.transactionprocessing.compartilhado.exception;

public class ApiException extends RuntimeException implements BaseException {

    private final String codigoErro;

    public ApiException(String codigoErro, String mensagem) {
        super(mensagem);
        this.codigoErro = codigoErro;
    }

    public ApiException(String codigoErro, String mensagem, Throwable causa) {
        super(mensagem, causa);
        this.codigoErro = codigoErro;
    }

    public String getCodigoErro() {
        return codigoErro;
    }

    public String getMensagem() {
        return super.getMessage();
    }

    @Override
    public String toString() {
        return String.format("[%s] %s", codigoErro, getMessage());
    }

}
