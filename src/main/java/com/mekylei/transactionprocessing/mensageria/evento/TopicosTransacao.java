package com.mekylei.transactionprocessing.mensageria.evento;

public final class TopicosTransacao {

    private TopicosTransacao() {
    }

    public static final String TRANSACOES_INICIADAS = "transacoes.iniciadas";
    public static final String TRANSACOES_CONCLUIDAS = "transacoes.concluidas";
    public static final String TRANSACOES_ESTORNADAS = "transacoes.estornadas";
    public static final String TRANSACOES_FALHAS = "transacoes.falhas";
}
