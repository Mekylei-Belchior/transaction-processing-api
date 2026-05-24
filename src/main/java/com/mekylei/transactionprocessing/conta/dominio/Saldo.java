package com.mekylei.transactionprocessing.conta.dominio;

import com.mekylei.transactionprocessing.compartilhado.exception.SaldoInsuficienteException;
import com.mekylei.transactionprocessing.transacao.dominio.vo.ValorMonetario;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class Saldo {

    private final UUID id;
    private final UUID idConta;
    private final BigDecimal disponivel;
    private final BigDecimal bloqueado;
    private final Long versao;
    private final Instant atualizadoEm;

    private Saldo(Builder builder) {
        this.id = builder.id;
        this.idConta = builder.idConta;
        this.disponivel = builder.disponivel;
        this.bloqueado = builder.bloqueado;
        this.versao = builder.versao;
        this.atualizadoEm = builder.atualizadoEm;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Retorna nova instância com saldo debitado.
     * Lança SaldoInsuficienteException se disponivel < valor (invariante de domínio).
     */
    public Saldo debitar(ValorMonetario valor) {
        BigDecimal novoDisponivel = this.disponivel.subtract(valor.valor());
        if (novoDisponivel.compareTo(BigDecimal.ZERO) < 0) {
            throw new SaldoInsuficienteException(this.idConta, this.disponivel, valor.valor());
        }
        return new Builder()
                .id(this.id)
                .idConta(this.idConta)
                .disponivel(novoDisponivel)
                .bloqueado(this.bloqueado)
                .versao(this.versao)
                .atualizadoEm(Instant.now())
                .build();
    }

    /**
     * Retorna nova instância com saldo creditado (ex: estorno).
     */
    public Saldo creditar(ValorMonetario valor) {
        return new Builder()
                .id(this.id)
                .idConta(this.idConta)
                .disponivel(this.disponivel.add(valor.valor()))
                .bloqueado(this.bloqueado)
                .versao(this.versao)
                .atualizadoEm(Instant.now())
                .build();
    }

    public UUID getId() {
        return id;
    }

    public UUID getIdConta() {
        return idConta;
    }

    public BigDecimal getDisponivel() {
        return disponivel;
    }

    public BigDecimal getBloqueado() {
        return bloqueado;
    }

    public Long getVersao() {
        return versao;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }

    public static final class Builder {
        private UUID id;
        private UUID idConta;
        private BigDecimal disponivel;
        private BigDecimal bloqueado = BigDecimal.ZERO;
        private Long versao;
        private Instant atualizadoEm;

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder idConta(UUID idConta) {
            this.idConta = idConta;
            return this;
        }

        public Builder disponivel(BigDecimal v) {
            this.disponivel = v;
            return this;
        }

        public Builder bloqueado(BigDecimal v) {
            this.bloqueado = v;
            return this;
        }

        public Builder versao(Long versao) {
            this.versao = versao;
            return this;
        }

        public Builder atualizadoEm(Instant t) {
            this.atualizadoEm = t;
            return this;
        }

        public Saldo build() {
            if (idConta == null) throw new IllegalStateException("'idConta' é obrigatório");
            if (disponivel == null) throw new IllegalStateException("'disponivel' é obrigatório");
            if (disponivel.compareTo(BigDecimal.ZERO) < 0) throw new IllegalStateException("'disponivel' não pode ser negativo");

            if (id == null) id = UUID.randomUUID();
            if (atualizadoEm == null) atualizadoEm = Instant.now();

            return new Saldo(this);
        }
    }
}