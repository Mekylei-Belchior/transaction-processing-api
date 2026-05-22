package com.mekylei.transactionprocessing.transacao.dominio;

import java.time.Instant;

public class Transacao {
    private final String id;
    private final String idCorrelacao;
    private final ValorMonetario valor;
    private final TipoTransacao tipo;
    private final StatusTransacao status;
    private final Instant criadoEm;
    private final String contaOrigem;
    private final String contaDestino;

    private Transacao(Builder builder) {
        this.id = builder.id;
        this.idCorrelacao = builder.idCorrelacao;
        this.valor = builder.valor;
        this.tipo = builder.tipo;
        this.status = builder.status;
        this.criadoEm = builder.criadoEm;
        this.contaOrigem = builder.contaOrigem;
        this.contaDestino = builder.contaDestino;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getId() {
        return id;
    }

    public String getIdCorrelacao() {
        return idCorrelacao;
    }

    public ValorMonetario getValor() {
        return valor;
    }

    public TipoTransacao getTipo() {
        return tipo;
    }

    public StatusTransacao getStatus() {
        return status;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public String getContaOrigem() {
        return contaOrigem;
    }

    public String getContaDestino() {
        return contaDestino;
    }

    public Transacao comStatus(StatusTransacao statusAtual) {
        return new Builder()
                .status(statusAtual)
                .id(this.id)
                .idCorrelacao(this.idCorrelacao)
                .tipo(this.tipo)
                .valor(this.valor)
                .contaOrigem(this.contaOrigem)
                .contaDestino(this.contaDestino)
                .criadoEm(this.criadoEm)
                .build();
    }

    public static final class Builder {
        private String id;
        private String idCorrelacao;
        private ValorMonetario valor;
        private TipoTransacao tipo;
        private StatusTransacao status;
        private Instant criadoEm;
        private String contaOrigem;
        private String contaDestino;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder idCorrelacao(String idCorrelacao) {
            this.idCorrelacao = idCorrelacao;
            return this;
        }

        public Builder valor(ValorMonetario valor) {
            this.valor = valor;
            return this;
        }

        public Builder tipo(TipoTransacao tipo) {
            this.tipo = tipo;
            return this;
        }

        public Builder status(StatusTransacao status) {
            this.status = status;
            return this;
        }

        public Builder criadoEm(Instant criadoEm) {
            this.criadoEm = criadoEm;
            return this;
        }

        public Builder contaOrigem(String contaOrigem) {
            this.contaOrigem = contaOrigem;
            return this;
        }

        public Builder contaDestino(String contaDestino) {
            this.contaDestino = contaDestino;
            return this;
        }

        public Transacao build() {
            if (id == null) throw new IllegalStateException("O 'id' deve ser fornecido");
            if (valor == null) throw new IllegalStateException("O 'valor' deve ser fornecido");
            if (tipo == null) throw new IllegalStateException("O 'tipo' deve ser fornecido");

            if (status == null) status = StatusTransacao.PENDENTE;
            if (criadoEm == null) criadoEm = Instant.now();

            return new Transacao(this);
        }

    }

}
