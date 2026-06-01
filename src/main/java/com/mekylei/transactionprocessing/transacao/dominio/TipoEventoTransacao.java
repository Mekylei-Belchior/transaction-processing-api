package com.mekylei.transactionprocessing.transacao.dominio;

public enum TipoEventoTransacao {
    TRANSACAO_INICIADA("TransacaoIniciada"),
    TRANSACAO_CONCLUIDA("TransacaoConcluida"),
    TRANSACAO_ESTORNADA("TransacaoEstornada"),
    TRANSACAO_FALHOU("TransacaoFalhou");

    private final String tipoEvento;

    TipoEventoTransacao(String tipoEvento) {
        this.tipoEvento = tipoEvento;
    }

    public String tipoEvento() {
        return tipoEvento;
    }
}
