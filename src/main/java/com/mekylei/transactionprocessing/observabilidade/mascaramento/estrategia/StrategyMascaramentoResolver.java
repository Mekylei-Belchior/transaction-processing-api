package com.mekylei.transactionprocessing.observabilidade.mascaramento.estrategia;

import com.mekylei.transactionprocessing.observabilidade.logging.TipoCampoMascarado;

import java.util.EnumMap;
import java.util.Map;

public class StrategyMascaramentoResolver {

    private static final Map<TipoCampoMascarado, MascaraStrategy> STRATEGIES =
            new EnumMap<>(Map.of(
                    TipoCampoMascarado.MESSAGE, new MensagemMascaradaStrategy(),
                    TipoCampoMascarado.STACKTRACE, new StacktraceMascaradoStrategy(),
                    TipoCampoMascarado.JSON, new JsonMascaradoStrategy(),
                    TipoCampoMascarado.HEADER, new HeaderMascaradoStrategy()
            ));

    public static MascaraStrategy resolve(TipoCampoMascarado tipo) {
        return STRATEGIES.get(tipo);
    }
}
