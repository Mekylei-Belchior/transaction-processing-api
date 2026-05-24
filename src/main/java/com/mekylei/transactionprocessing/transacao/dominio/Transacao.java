package com.mekylei.transactionprocessing.transacao.dominio;

import com.mekylei.transactionprocessing.transacao.dominio.vo.ValorMonetario;

import java.time.Instant;
import java.util.UUID;

public class Transacao {
    private final UUID id;
    private final UUID idCorrelacao;
    private final UUID idIdempotencia;
    private final ValorMonetario valor;
    private final TipoTransacao tipo;
    private final StatusTransacao status;
    private final Instant criadoEm;
    private final UUID idContaOrigem;
    private final String contaDestino;

    private Transacao(Builder builder) {
        this.id = builder.id;
        this.idCorrelacao = builder.idCorrelacao;
        this.idIdempotencia = builder.idIdempotencia;
        this.valor = builder.valor;
        this.tipo = builder.tipo;
        this.status = builder.status;
        this.criadoEm = builder.criadoEm;
        this.idContaOrigem = builder.idContaOrigem;
        this.contaDestino = builder.contaDestino;
    }

    public static Builder builder() {
        return new Builder();
    }

    public UUID getId() {
        return id;
    }

    public UUID getIdCorrelacao() {
        return idCorrelacao;
    }

    public UUID getIdIdempotencia() {
        return idIdempotencia;
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

    public UUID getIdContaOrigem() {
        return idContaOrigem;
    }

    public String getContaDestino() {
        return contaDestino;
    }

    public Transacao comStatus(StatusTransacao statusAtual) {
        return new Builder()
                .status(statusAtual)
                .id(this.id)
                .idCorrelacao(this.idCorrelacao)
                .idIdempotencia(this.idIdempotencia)
                .tipo(this.tipo)
                .valor(this.valor)
                .idContaOrigem(this.idContaOrigem)
                .contaDestino(this.contaDestino)
                .criadoEm(this.criadoEm)
                .build();
    }

    public static final class Builder {
        private UUID id;
        private UUID idCorrelacao;
        private UUID idIdempotencia;
        private ValorMonetario valor;
        private TipoTransacao tipo;
        private StatusTransacao status;
        private Instant criadoEm;
        private UUID idContaOrigem;
        private String contaDestino;

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder idCorrelacao(UUID idCorrelacao) {
            this.idCorrelacao = idCorrelacao;
            return this;
        }

        public Builder idIdempotencia(UUID idIdempotencia) {
            this.idIdempotencia = idIdempotencia;
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

        public Builder idContaOrigem(UUID idContaOrigem) {
            this.idContaOrigem = idContaOrigem;
            return this;
        }

        public Builder contaDestino(String contaDestino) {
            this.contaDestino = contaDestino;
            return this;
        }

        public Transacao build() {
            if (valor == null) throw new IllegalStateException("O 'valor' deve ser fornecido");
            if (tipo == null) throw new IllegalStateException("O 'tipo' deve ser fornecido");
            if (idContaOrigem == null) throw new IllegalStateException("A 'idContaOrigem' deve ser fornecido");
            if (contaDestino == null || contaDestino.isBlank())
                throw new IllegalStateException("A 'contaDestino' deve ser fornecido");

            if (id == null) id = UUID.randomUUID();
            if (status == null) status = StatusTransacao.PENDENTE;
            if (criadoEm == null) criadoEm = Instant.now();

            return new Transacao(this);
        }

    }

}
