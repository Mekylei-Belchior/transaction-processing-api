package com.mekylei.transactionprocessing.configuracao.seguranca;

public enum RoleTransacao {
    CLIENTE,
    OPERADOR,
    GERENTE,
    ADMIN,
    SERVICO_INTERNO;

    public String getSpringRole() {
        return "ROLE_" + this.name();
    }
}