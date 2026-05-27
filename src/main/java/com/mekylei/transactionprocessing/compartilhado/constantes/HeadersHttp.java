package com.mekylei.transactionprocessing.compartilhado.constantes;

public final class HeadersHttp {

    private HeadersHttp() {
    }

    public static final String CORRELACAO_HEADER = "X-Correlation-Id";
    public static final String IDEMPOTENCIA_HEADER = "X-Idempotency-Key";
    public static final String IP_ORIGEM_HEADER = "X-Forwarded-For";
}
