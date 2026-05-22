package com.mekylei.transactionprocessing.transacao.aplicacao;

import com.mekylei.transactionprocessing.compartilhado.util.CorrelacaoIdUtil;
import com.mekylei.transactionprocessing.infraestrutura.persistencia.TransacaoJpaAdapter;
import com.mekylei.transactionprocessing.transacao.dominio.TipoTransacao;
import com.mekylei.transactionprocessing.transacao.dominio.Transacao;
import com.mekylei.transactionprocessing.transacao.dominio.ValorMonetario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class CriaTransacaoService {

    private static final Logger logger = LoggerFactory.getLogger(CriaTransacaoService.class);

    private final TransacaoJpaAdapter repository;

    public CriaTransacaoService(TransacaoJpaAdapter repository) {
        this.repository = repository;
    }

    public Transacao cria(BigDecimal valor, TipoTransacao tipoTransacao, String contaOrigem, String contaDestino) {
        String idCorrelacao = CorrelacaoIdUtil.gerar();

        logger.info("Criando transação: tipo={}, valor={}, idCorrelacao={}", tipoTransacao, valor, idCorrelacao);

        Transacao transacao = Transacao.builder()
                .tipo(tipoTransacao)
                .idCorrelacao(idCorrelacao)
                .valor(ValorMonetario.paraReal(valor))
                .contaOrigem(contaOrigem)
                .contaDestino(contaDestino)
                .build();

        repository.save(transacao);

        logger.info("Transação criada: id={}", transacao.getId());

        return transacao;
    }
}
