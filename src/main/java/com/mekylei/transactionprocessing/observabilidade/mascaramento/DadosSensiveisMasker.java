package com.mekylei.transactionprocessing.observabilidade.mascaramento;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DadosSensiveisMasker {

    private DadosSensiveisMasker() {
    }

    private static final List<String> INDICADORES = List.of(
            "cpf",
            "conta",
            "agencia",
            "saldo",
            "valor",
            "amount",
            "bearer",
            "authorization",
            "token",
            "email"
    );

    private static final List<MascaraPadrao> PADROES = List.of(

            // CPF
            new MascaraPadrao(
                    Pattern.compile("\\b(\\d{3})\\.?\\d{3}\\.?\\d{3}-?(\\d{2})\\b"),
                    "$1.***.***-$2"
            ),

            // Conta bancária
            new MascaraPadrao(
                    Pattern.compile("(?i)(valor|saldo|amount)[=: ]+([0-9]+(?:\\.[0-9]{2})?)", Pattern.CASE_INSENSITIVE),
                    "$1=****"
            ),

            // Agência
            new MascaraPadrao(
                    Pattern.compile("\\bagencia[=: ]+(\\d{2,})", Pattern.CASE_INSENSITIVE),
                    "agencia=****"
            ),

            // Bearer token
            new MascaraPadrao(
                    Pattern.compile("(Bearer\\s+)[A-Za-z0-9-._~+/]+=*", Pattern.CASE_INSENSITIVE),
                    "$1****"
            ),

            // Authorization
            new MascaraPadrao(
                    Pattern.compile("(Authorization[=: ]+)([^\\s,;]+)", Pattern.CASE_INSENSITIVE),
                    "$1****"
            ),

            // Valores monetários altos
            new MascaraPadrao(
                    Pattern.compile("R\\$\\s?([0-9]{4,}[\\d.,]*)"),
                    "R$ ****"
            ),

            // e-mail
            new MascaraPadrao(
                    Pattern.compile(
                            "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b"
                    ),
                    "****@****"
            )
    );

    public static String mascarar(String mensagem) {
        if (mensagem == null) return null;

        String lower = mensagem.toLowerCase();
        boolean possuiIndicador = INDICADORES.stream().anyMatch(lower::contains);

        if (!possuiIndicador) {
            return mensagem;
        }

        String resultado = mensagem;

        for (MascaraPadrao mascara : PADROES) {
            Matcher matcher = mascara.pattern().matcher(resultado);
            resultado = matcher.replaceAll(mascara.substituto());
        }

        return resultado;
    }

}