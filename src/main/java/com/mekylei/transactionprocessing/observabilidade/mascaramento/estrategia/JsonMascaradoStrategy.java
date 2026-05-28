package com.mekylei.transactionprocessing.observabilidade.mascaramento.estrategia;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.*;

public final class JsonMascaradoStrategy implements MascaraStrategy {

    private static final String MASCARA = "****";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Set<String> CAMPOS_SENSIVEIS = Set.of(
            "cpf",
            "documento",
            "conta",
            "agencia",
            "token",
            "authorization",
            "senha",
            "password",
            "secret"
    );

    @Override
    public String mascarar(String valor) {
        if (valor == null || valor.isBlank()) return valor;

        try {
            JsonNode root = MAPPER.readTree(valor);

            if (root == null) {
                return valor;
            }

            mascararIterativamente(root);

            return MAPPER.writeValueAsString(root);

        } catch (RuntimeException e) {
            // Não falha o fluxo de logging caso o payload não seja um JSON válido.
            return valor;
        }
    }

    private void mascararIterativamente(JsonNode root) {
        Deque<JsonNode> pilha = new ArrayDeque<>();
        pilha.push(root);

        while (!pilha.isEmpty()) {
            JsonNode nodeAtual = pilha.pop();

            if (nodeAtual instanceof ObjectNode objectNode) {
                processarObjeto(objectNode, pilha);
            } else if (nodeAtual instanceof ArrayNode arrayNode) {
                processarArray(arrayNode, pilha);
            }
        }
    }

    private void processarObjeto(ObjectNode objectNode, Deque<JsonNode> pilha) {
        List<String> camposParaMascarar = new ArrayList<>();

        for (Map.Entry<String, JsonNode> entry : objectNode.properties()) {
            String campo = normalizar(entry.getKey());
            JsonNode valor = entry.getValue();

            if (ehCampoSensivel(campo)) {
                camposParaMascarar.add(entry.getKey());
            } else if (valor != null && valor.isContainer()) {
                pilha.push(valor);
            }
        }

        for (String campo : camposParaMascarar) {
            objectNode.put(campo, MASCARA);
        }
    }

    private void processarArray(ArrayNode arrayNode, Deque<JsonNode> pilha) {
        for (JsonNode elemento : arrayNode) {
            if (elemento != null && elemento.isContainer()) {
                pilha.push(elemento);
            }
        }
    }

    private boolean ehCampoSensivel(String campo) {
        return CAMPOS_SENSIVEIS.contains(campo);
    }

    private String normalizar(String valor) {
        return valor == null ? null : valor.toLowerCase(Locale.ROOT);
    }
}