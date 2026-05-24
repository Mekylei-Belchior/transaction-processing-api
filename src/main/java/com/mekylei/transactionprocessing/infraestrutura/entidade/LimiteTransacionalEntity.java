package com.mekylei.transactionprocessing.infraestrutura.entidade;

import com.mekylei.transactionprocessing.transacao.dominio.TipoTransacao;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "limite")
public class LimiteTransacionalEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "id_conta", nullable = false)
    private UUID idConta;

    @Column(name = "tipo", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private TipoTransacao tipo;

    @Column(name = "limite_diario", nullable = false, precision = 15, scale = 2)
    private BigDecimal limiteDiario;

    @Column(name = "limite_utilizado", nullable = false, precision = 15, scale = 2)
    private BigDecimal limiteTransacao;

    @Column(name = "utilizado_hoje", nullable = false, precision = 15, scale = 2)
    private BigDecimal utilizadoHoje;

    @Column(name = "data_referencia", nullable = false)
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
