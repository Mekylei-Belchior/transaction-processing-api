package com.mekylei.transactionprocessing.infraestrutura.entidade;

import com.mekylei.transactionprocessing.auditoria.AuditoriaListener;
import com.mekylei.transactionprocessing.transacao.dominio.TipoTransacao;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "limite")
@EntityListeners(AuditoriaListener.class)
public class LimiteTransacionalEntity {

    @Id
    @Column(name = "id", updatable = false)
    private UUID id;

    @Column(name = "id_conta")
    private UUID idConta;

    @Column(name = "tipo")
    @Enumerated(EnumType.STRING)
    private TipoTransacao tipo;

    @Column(name = "limite_diario")
    private BigDecimal limiteDiario;

    @Column(name = "limite_utilizado")
    private BigDecimal limiteTransacao;

    @Column(name = "utilizado_hoje")
    private BigDecimal utilizadoHoje;

    @Column(name = "data_referencia")
    private LocalDate dataReferencia;

    public LimiteTransacionalEntity() {
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

    public TipoTransacao getTipo() {
        return tipo;
    }

    public void setTipo(TipoTransacao tipo) {
        this.tipo = tipo;
    }

    public BigDecimal getLimiteDiario() {
        return limiteDiario;
    }

    public void setLimiteDiario(BigDecimal limiteDiario) {
        this.limiteDiario = limiteDiario;
    }

    public BigDecimal getLimiteTransacao() {
        return limiteTransacao;
    }

    public void setLimiteTransacao(BigDecimal limiteTransacao) {
        this.limiteTransacao = limiteTransacao;
    }

    public BigDecimal getUtilizadoHoje() {
        return utilizadoHoje;
    }

    public void setUtilizadoHoje(BigDecimal utilizadoHoje) {
        this.utilizadoHoje = utilizadoHoje;
    }

    public LocalDate getDataReferencia() {
        return dataReferencia;
    }

    public void setDataReferencia(LocalDate dataReferencia) {
        this.dataReferencia = dataReferencia;
    }
}
