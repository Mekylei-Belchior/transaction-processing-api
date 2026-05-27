package com.mekylei.transactionprocessing.auditoria;


import com.mekylei.transactionprocessing.auditoria.dominio.AcaoAuditoria;
import com.mekylei.transactionprocessing.auditoria.dominio.AuditoriaEvento;
import com.mekylei.transactionprocessing.auditoria.porta.AuditoriaContextGateway;
import com.mekylei.transactionprocessing.auditoria.porta.AuditoriaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

@Service
public class AuditoriaService {

    private static final Logger logger = LoggerFactory.getLogger(AuditoriaService.class);

    // UUID sentinela para operações de sistema (sem operador humano)
    private static final UUID SISTEMA = UUID.fromString("00000000-0000-0000-0000-000000000000");

    private final AuditoriaRepository auditoriaRepository;
    private final AuditoriaContextGateway auditoriaContextGateway;
    private final ObjectMapper objectMapper;

    public AuditoriaService(AuditoriaRepository auditoriaRepository,
                            AuditoriaContextGateway auditoriaContextGateway,
                            ObjectMapper objectMapper) {
        this.auditoriaRepository = auditoriaRepository;
        this.auditoriaContextGateway = auditoriaContextGateway;
        this.objectMapper = objectMapper;
    }

    public void registrar(AcaoAuditoria acao, String recurso, UUID idRecurso) {
        try {
            DadosAuditoria dados = auditoriaContextGateway.obter();

            AuditoriaEvento evento =
                    AuditoriaEvento.simples(
                            dados.idOperador().orElse(SISTEMA),
                            acao,
                            recurso,
                            idRecurso,
                            dados.idCorrelacao(),
                            dados.ipOrigem());

            auditoriaRepository.registrar(evento);

        } catch (RuntimeException e) {
            logger.error("Falha ao registrar evento de auditoria: acao={}, recurso={}, id={}",
                    acao, recurso, idRecurso, e);
        }
    }

    public void registrar(AcaoAuditoria acao, String recurso, UUID idRecurso, Object dadosAnteriores, Object dadosNovos) {
        try {
            DadosAuditoria dados = auditoriaContextGateway.obter();

            JsonNode anterior = toJson(dadosAnteriores);
            JsonNode novo = toJson(dadosNovos);

            AuditoriaEvento evento =
                    AuditoriaEvento.comDados(
                            dados.idOperador().orElse(SISTEMA),
                            acao,
                            recurso,
                            idRecurso,
                            dados.idCorrelacao(),
                            anterior,
                            novo,
                            dados.ipOrigem());

            auditoriaRepository.registrar(evento);

        } catch (Exception e) {
            logger.error("Falha ao registrar evento de auditoria com dados: acao={}, recurso={}, id={}",
                    acao, recurso, idRecurso, e);
        }
    }

    private JsonNode toJson(Object obj) {
        return obj == null
                ? null
                : objectMapper.valueToTree(obj);
    }
}
