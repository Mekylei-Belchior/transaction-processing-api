package com.mekylei.transactionprocessing.infraestrutura.entidade;

import jakarta.persistence.*;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_evento")
public class OutboxEventoEntity {

    @Id
    @Column(name = "id", updatable = false)
    private UUID id;

    @Column(name = "tipo_evento")
    private String tipoEvento;

    @Column(name = "tipo_agregado")
    private String tipoAgregado;

    @Column(name = "id_agregado")
    private UUID idAgregado;

    @Column(name = "topico")
    private String topico;

    @Column(name = "chave")
    private String chave;

    @Column(name = "payload", columnDefinition = "jsonb")
    private JsonNode payload;

    @Column(name = "id_correlacao")
    private UUID idCorrelacao;

    @Column(name = "ocorrido_em")
    private Instant ocorridoEm;

    @Column(name = "criado_em")
    private Instant criadoEm;

    @Column(name = "publicado_em")
    private Instant publicadoEm;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private StatusOutboxEvento status;

    @Column(name = "tentativas")
    private int tentativas;

    @Column(name = "ultimo_erro")
    private String ultimoErro;

    @Column(name = "proxima_tentativa_em")
    private Instant proximaTentativaEm;

    public OutboxEventoEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTipoEvento() {
        return tipoEvento;
    }

    public void setTipoEvento(String tipoEvento) {
        this.tipoEvento = tipoEvento;
    }

    public String getTipoAgregado() {
        return tipoAgregado;
    }

    public void setTipoAgregado(String tipoAgregado) {
        this.tipoAgregado = tipoAgregado;
    }

    public UUID getIdAgregado() {
        return idAgregado;
    }

    public void setIdAgregado(UUID idAgregado) {
        this.idAgregado = idAgregado;
    }

    public String getTopico() {
        return topico;
    }

    public void setTopico(String topico) {
        this.topico = topico;
    }

    public String getChave() {
        return chave;
    }

    public void setChave(String chave) {
        this.chave = chave;
    }

    public JsonNode getPayload() {
        return payload;
    }

    public void setPayload(JsonNode payload) {
        this.payload = payload;
    }

    public UUID getIdCorrelacao() {
        return idCorrelacao;
    }

    public void setIdCorrelacao(UUID idCorrelacao) {
        this.idCorrelacao = idCorrelacao;
    }

    public Instant getOcorridoEm() {
        return ocorridoEm;
    }

    public void setOcorridoEm(Instant ocorridoEm) {
        this.ocorridoEm = ocorridoEm;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(Instant criadoEm) {
        this.criadoEm = criadoEm;
    }

    public Instant getPublicadoEm() {
        return publicadoEm;
    }

    public void setPublicadoEm(Instant publicadoEm) {
        this.publicadoEm = publicadoEm;
    }

    public StatusOutboxEvento getStatus() {
        return status;
    }

    public void setStatus(StatusOutboxEvento status) {
        this.status = status;
    }

    public int getTentativas() {
        return tentativas;
    }

    public void setTentativas(int tentativas) {
        this.tentativas = tentativas;
    }

    public String getUltimoErro() {
        return ultimoErro;
    }

    public void setUltimoErro(String ultimoErro) {
        this.ultimoErro = ultimoErro;
    }

    public Instant getProximaTentativaEm() {
        return proximaTentativaEm;
    }

    public void setProximaTentativaEm(Instant proximaTentativaEm) {
        this.proximaTentativaEm = proximaTentativaEm;
    }
}
