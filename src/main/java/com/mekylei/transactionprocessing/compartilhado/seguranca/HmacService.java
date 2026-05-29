package com.mekylei.transactionprocessing.compartilhado.seguranca;

import com.mekylei.transactionprocessing.configuracao.persistencia.HmacProperties;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Locale;

@Service
public class HmacService {

    private static final String ALGORITMO = "HmacSHA256";

    private final HmacProperties properties;

    public HmacService(HmacProperties properties) {
        this.properties = properties;
    }

    public String gerar(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }

        try {
            Mac mac = Mac.getInstance(ALGORITMO);
            SecretKeySpec secretKey = new SecretKeySpec(properties.chave().getBytes(StandardCharsets.UTF_8), ALGORITMO);

            mac.init(secretKey);
            byte[] hmacBytes = mac.doFinal(normalizar(valor).getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hmacBytes);

        } catch (Exception e) {
            throw new IllegalStateException("Erro ao gerar HMAC SHA-256", e);
        }
    }

    private String normalizar(String valor) {
        return valor.trim().toUpperCase(Locale.ROOT);
    }
}