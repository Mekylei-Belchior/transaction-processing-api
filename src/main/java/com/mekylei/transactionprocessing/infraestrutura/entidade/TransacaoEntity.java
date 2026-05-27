package com.mekylei.transactionprocessing.infraestrutura.entidade;


import com.mekylei.transactionprocessing.auditoria.aplicacao.AuditoriaListener;
import com.mekylei.transactionprocessing.transacao.dominio.StatusTransacao;
import com.mekylei.transactionprocessing.transacao.dominio.TipoTransacao;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transacao")
@EntityListeners(AuditoriaListener.class)
public class TransacaoEntity {

    @Id
    @Column(name = "id", updatable = false)
    private UUID id;

    @Column(name = "id_correlacao")
    private UUID idCorrelacao;

    @Column(name = "id_idempotencia")
    private UUID idIdempotencia;

    @Column(name = "valor")
    private BigDecimal valor;

    @Column(name = "moeda")
    private String moeda;

    @Column(name = "tipo")
    @Enumerated(EnumType.STRING)
    private TipoTransacao tipo;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private StatusTransacao status;

    @Column(name = "criado_em", updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em")
    private Instant atualizadoEm;

    @Column(name = "id_conta_origem")
    private UUID idContaOrigem;

    @Column(name = "conta_destino")
    private String contaDestino;

    @Version
    @Column(name = "versao")
    private Long versao;

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

    public UUID getIdIdempotencia() {
        return idIdempotencia;
    }

    public void setIdIdempotencia(UUID idIdempotencia) {
        this.idIdempotencia = idIdempotencia;
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

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoEm(Instant atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }

    public UUID getIdContaOrigem() {
        return idContaOrigem;
    }

    public void setIdContaOrigem(UUID idContaOrigem) {
        this.idContaOrigem = idContaOrigem;
    }

    public String getContaDestino() {
        return contaDestino;
    }

    public void setContaDestino(String contaDestino) {
        this.contaDestino = contaDestino;
    }

    public Long getVersao() {
        return versao;
    }

    public void setVersao(Long versao) {
        this.versao = versao;
    }
}
