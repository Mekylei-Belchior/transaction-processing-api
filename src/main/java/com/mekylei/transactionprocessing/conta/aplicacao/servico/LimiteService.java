package com.mekylei.transactionprocessing.conta.aplicacao.servico;

import com.mekylei.transactionprocessing.conta.aplicacao.porta.repositorio.LimiteRepository;
import com.mekylei.transactionprocessing.conta.dominio.LimiteTransacional;
import com.mekylei.transactionprocessing.conta.dominio.TipoConta;
import com.mekylei.transactionprocessing.transacao.dominio.vo.ValorMonetario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
public class LimiteService {

    private final LimiteRepository limiteRepository;

    public LimiteService(LimiteRepository limiteRepository) {
        this.limiteRepository = limiteRepository;
    }

    @Transactional(readOnly = true)
    public void validarLimite(UUID idConta, TipoConta tipoConta, BigDecimal valor) {
        limiteRepository.findByIdContaAndTipo(idConta, tipoConta)
                .ifPresent(limite -> limite.validar(ValorMonetario.paraReal(valor)));
    }

    @Transactional
    public void decrementarUtilizado(UUID idConta, TipoConta tipoConta, BigDecimal valor) {
        Optional<LimiteTransacional> limiteOptional = limiteRepository.findByIdContaAndTipo(idConta, tipoConta);
        limiteOptional.ifPresent(limite -> {
            LimiteTransacional atualizado = limite.decrementar(ValorMonetario.paraReal(valor));
            limiteRepository.save(atualizado);
        });
    }
}
