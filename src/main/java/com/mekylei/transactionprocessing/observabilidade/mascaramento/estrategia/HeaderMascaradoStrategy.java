package com.mekylei.transactionprocessing.observabilidade.mascaramento.estrategia;

import com.mekylei.transactionprocessing.observabilidade.mascaramento.MascaraPadrao;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HeaderMascaradoStrategy implements MascaraStrategy {

    private static final Set<String> CAMPOS_SENSIVEIS = Set.of(
            "bearer",
            "authorization"
    );

    private static final Set<MascaraPadrao> PADROES = Set.of(

            // Bearer token
            new MascaraPadrao(
                    Pattern.compile("(Bearer\\s+)[A-Za-z0-9-._~+/]+=*", Pattern.CASE_INSENSITIVE),
                    "$1****"
            ),

            // Authorization
            new MascaraPadrao(
                    Pattern.compile("(Authorization[=: ]+)([^\\s,;]+)", Pattern.CASE_INSENSITIVE),
                    "$1****"
            )
    );

    @Override
    public String mascarar(String valor) {
        if (valor == null) return null;

        String lower = valor.toLowerCase();
        boolean possuiIndicador = CAMPOS_SENSIVEIS.stream().anyMatch(lower::contains);

        if (!possuiIndicador) {
            return valor;
        }

        String resultado = valor;

        for (MascaraPadrao mascara : PADROES) {
            Matcher matcher = mascara.pattern().matcher(resultado);
            resultado = matcher.replaceAll(mascara.substituto());
        }

        return resultado;
    }

}
