package com.mekylei.transactionprocessing.infraestrutura.entidade;


import com.mekylei.transactionprocessing.transacao.dominio.StatusTransacao;
import com.mekylei.transactionprocessing.transacao.dominio.TipoTransacao;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transacao")
public class TransacaoEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "id_correlacao", nullable = false, length = 50)
    private UUID idCorrelacao;

    @Column(name = "valor", nullable = false, precision = 15, scale = 2)
    private BigDecimal valor;

    @Column(name = "moeda", nullable = false, length = 3)
    private String moeda;

    @Column(name = "tipo", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private TipoTransacao tipo;

    @Column(name = "status", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private StatusTransacao status;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "conta_origem", nullable = false, length = 100)
    private String contaOrigem;

    @Column(name = "conta_destino", nullable = false, length = 100)
    private String contaDestino;

    public TransacaoEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getIdCorrelacao() {
        return idCorrelacao;
    }

    public void setIdCorrelacao(UUID idCorrelacao) {
        this.idCorrelacao = idCorrelacao;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public String getMoeda() {
        return moeda;
    }

    public void setMoeda(String moeda) {
        this.moeda = moeda;
    }

    public TipoTransacao getTipo() {
        return tipo;
    }

    public void setTipo(TipoTransacao tipo) {
        this.tipo = tipo;
    }

    public StatusTransacao getStatus() {
        return status;
    }

    public void setStatus(StatusTransacao status) {
        this.status = status;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(Instant criadoEm) {
        this.criadoEm = criadoEm;
    }

    public String getContaOrigem() {
        return contaOrigem;
    }

    public void setContaOrigem(String contaOrigem) {
        this.contaOrigem = contaOrigem;
    }

    public String getContaDestino() {
        return contaDestino;
    }

    public void setContaDestino(String contaDestino) {
        this.contaDestino = contaDestino;
    }
}
