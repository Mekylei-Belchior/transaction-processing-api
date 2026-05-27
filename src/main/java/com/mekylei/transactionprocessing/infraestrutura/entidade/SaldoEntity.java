package com.mekylei.transactionprocessing.infraestrutura.entidade;

import com.mekylei.transactionprocessing.auditoria.AuditoriaListener;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "saldo")
@EntityListeners(AuditoriaListener.class)
public class SaldoEntity {

    @Id
    @Column(name = "id", updatable = false)
    private UUID id;

    @Column(name = "id_conta")
    private UUID idConta;

    @Column(name = "disponivel")
    private BigDecimal disponivel;

    @Column(name = "bloqueado")
    private BigDecimal bloqueado;

    @Version
    @Column(name = "versao")
    private Long versao;

    @Column(name = "atualizado_em")
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
