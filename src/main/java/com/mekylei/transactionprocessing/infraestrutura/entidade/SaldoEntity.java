package com.mekylei.transactionprocessing.infraestrutura.entidade;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "saldo")
public class SaldoEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "id_conta", nullable = false, unique = true)
    private UUID idConta;

    @Column(name = "disponivel", nullable = false, precision = 15, scale = 2)
    private BigDecimal disponivel;

    @Column(name = "bloqueado", nullable = false, precision = 15, scale = 2)
    private BigDecimal bloqueado;

    @Version
    @Column(name = "versao", nullable = false)
    private Long versao;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    public SaldoEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getIdConta() {
        return idConta;
    }

    public void setIdConta(UUID idConta) {
        this.idConta = idConta;
    }

    public BigDecimal getDisponivel() {
        return disponivel;
    }

    public void setDisponivel(BigDecimal disponivel) {
        this.disponivel = disponivel;
    }

    public BigDecimal getBloqueado() {
        return bloqueado;
    }

    public void setBloqueado(BigDecimal bloqueado) {
        this.bloqueado = bloqueado;
    }

    public Long getVersao() {
        return versao;
    }

    public void setVersao(Long versao) {
        this.versao = versao;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoEm(Instant atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }
}
