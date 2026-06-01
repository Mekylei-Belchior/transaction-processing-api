package com.mekylei.transactionprocessing.infraestrutura.entidade;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "evento_processado",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_evento_processado_evento_grupo",
                columnNames = {"id_evento", "grupo_consumidor"}
        )
)
public class EventoProcessadoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "id_evento")
    private UUID idEvento;

    @Column(name = "id_correlacao")
    private UUID idCorrelacao;

    @Column(name = "grupo_consumidor")
    private String grupoConsumidor;

    @Column(name = "topico")
    private String topico;

    @Column(name = "processado_em")
    private Instant processadoEm;

    public EventoProcessadoEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getIdEvento() {
        return idEvento;
    }

    public void setIdEvento(UUID idEvento) {
        this.idEvento = idEvento;
    }

    public UUID getIdCorrelacao() {
        return idCorrelacao;
    }

    public void setIdCorrelacao(UUID idCorrelacao) {
        this.idCorrelacao = idCorrelacao;
    }

    public String getGrupoConsumidor() {
        return grupoConsumidor;
    }

    public void setGrupoConsumidor(String grupoConsumidor) {
        this.grupoConsumidor = grupoConsumidor;
    }

    public String getTopico() {
        return topico;
    }

    public void setTopico(String topico) {
        this.topico = topico;
    }

    public Instant getProcessadoEm() {
        return processadoEm;
    }

    public void setProcessadoEm(Instant processadoEm) {
        this.processadoEm = processadoEm;
    }
}
