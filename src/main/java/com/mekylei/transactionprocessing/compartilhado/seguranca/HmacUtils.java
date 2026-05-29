package com.mekylei.transactionprocessing.compartilhado.seguranca;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

public final class HmacUtils {

    private static final String ALGORITMO = "HmacSHA256";

    private HmacUtils() {
    }

    public static String gerarHmacSha256(String valor, String chave) {
        try {
            Mac mac = Mac.getInstance(ALGORITMO);

            SecretKeySpec secretKey = new SecretKeySpec(chave.getBytes(StandardCharsets.UTF_8), ALGORITMO);
            mac.init(secretKey);

            byte[] hmacBytes = mac.doFinal(valor.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hmacBytes);

        } catch (Exception e) {
            throw new IllegalStateException("Erro ao gerar HMAC", e);
        }
    }
}