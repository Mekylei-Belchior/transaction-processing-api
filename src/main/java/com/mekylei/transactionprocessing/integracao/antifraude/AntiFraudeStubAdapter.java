package com.mekylei.transactionprocessing.integracao.antifraude;

import com.mekylei.transactionprocessing.transacao.aplicacao.porta.integracao.AntiFraudeGateway;
import com.mekylei.transactionprocessing.transacao.dominio.Transacao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class AntiFraudeStubAdapter implements AntiFraudeGateway {

    private static final Logger logger = LoggerFactory.getLogger(AntiFraudeStubAdapter.class);

    private final BigDecimal limiteAprovacaoAutomatica;

    public AntiFraudeStubAdapter(
            @Value("${antifraude.limite-aprovacao-automatica:10000.00}") BigDecimal limiteAprovacaoAutomatica
    ) {
        this.limiteAprovacaoAutomatica = limiteAprovacaoAutomatica;
    }

    @Override
    public boolean autorizar(Transacao transacao) {
        BigDecimal valor = transacao.getValor().valor();
        boolean autorizado = valor.compareTo(limiteAprovacaoAutomatica) <= 0;

        if (autorizado) {
            logger.debug("TEF autorizado pelo stub: id={}, valor=R${}", transacao.getId(), valor);
        } else {
            logger.warn("TEF rejeitado pelo stub (valor R${} acima do limite R${}): id={}",
                    valor, limiteAprovacaoAutomatica, transacao.getId());
        }

        return autorizado;
    }
}
