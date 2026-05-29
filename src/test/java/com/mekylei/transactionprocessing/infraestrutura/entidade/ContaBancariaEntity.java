package com.mekylei.transactionprocessing.infraestrutura.entidade;

import com.mekylei.transactionprocessing.compartilhado.util.CriptografiaConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * Entidade exclusiva para testes de integração do CriptografiaConverter.
 *
 * Não utiliza @EntityListeners(AuditoriaListener.class) intencionalmente para
 * isolar o comportamento do converter sem dependências de auditoria no contexto
 * de teste. Os campos numeroConta e titular exercitam o converter com
 * dados representativos de informações bancárias sensíveis.
 */
@Entity
@Table(name = "conta_bancaria_test")
public class ContaBancariaEntity {

    @Id
    @Column(name = "id", updatable = false)
    private UUID id;

    @Convert(converter = CriptografiaConverter.class)
    @Column(name = "numero_conta", nullable = false)
    private String numeroConta;

    @Convert(converter = CriptografiaConverter.class)
    @Column(name = "titular", nullable = false)
    private String titular;

    public ContaBancariaEntity() {
    }

    public ContaBancariaEntity(UUID id, String numeroConta, String titular) {
        this.id = id;
        this.numeroConta = numeroConta;
        this.titular = titular;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getNumeroConta() {
        return numeroConta;
    }

    public void setNumeroConta(String numeroConta) {
        this.numeroConta = numeroConta;
    }

    public String getTitular() {
        return titular;
    }

    public void setTitular(String titular) {
        this.titular = titular;
    }
}
