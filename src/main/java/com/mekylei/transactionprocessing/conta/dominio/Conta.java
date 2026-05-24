package com.mekylei.transactionprocessing.conta.dominio;

import java.time.Instant;
import java.util.UUID;

public class Conta {

    private final UUID id;
    private final String numeroConta;
    private final String agencia;
    private final UUID idCliente;
    private final TipoConta tipo;
    private final StatusConta status;
    private final Instant criadoEm;

    private Conta(Builder builder) {
        this.id = builder.id;
        this.numeroConta = builder.numeroConta;
        this.agencia = builder.agencia;
        this.idCliente = builder.idCliente;
        this.tipo = builder.tipo;
        this.status = builder.status;
        this.criadoEm = builder.criadoEm;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean estaAtiva() {
        return StatusConta.ATIVA.equals(this.status);
    }

    public UUID getId() {
        return id;
    }

    public String getNumeroConta() {
        return numeroConta;
    }

    public String getAgencia() {
        return agencia;
    }

    public UUID getIdCliente() {
        return idCliente;
    }

    public TipoConta getTipo() {
        return tipo;
    }

    public StatusConta getStatus() {
        return status;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public static final class Builder {
        private UUID id;
        private String numeroConta;
        private String agencia;
        private UUID idCliente;
        private TipoConta tipo;
        private StatusConta status;
        private Instant criadoEm;

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder numeroConta(String numeroConta) {
            this.numeroConta = numeroConta;
            return this;
        }

        public Builder agencia(String agencia) {
            this.agencia = agencia;
            return this;
        }

        public Builder idCliente(UUID idCliente) {
            this.idCliente = idCliente;
            return this;
        }

        public Builder tipo(TipoConta tipo) {
            this.tipo = tipo;
            return this;
        }

        public Builder status(StatusConta status) {
            this.status = status;
            return this;
        }

        public Builder criadoEm(Instant criadoEm) {
            this.criadoEm = criadoEm;
            return this;
        }

        public Conta build() {
            if (numeroConta == null || numeroConta.isBlank()) throw new IllegalStateException("'numeroConta' é obrigatório");
            if (agencia == null || agencia.isBlank()) throw new IllegalStateException("'agencia' é obrigatório");
            if (idCliente == null) throw new IllegalStateException("'idCliente' é obrigatório");
            if (tipo == null) throw new IllegalStateException("'tipo' é obrigatório");

            if (status == null) status = StatusConta.ATIVA;
            if (id == null) id = UUID.randomUUID();
            if (criadoEm == null) criadoEm = Instant.now();

            return new Conta(this);
        }
    }
}