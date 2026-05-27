package com.mekylei.transactionprocessing.auditoria;

import java.util.Optional;
import java.util.UUID;

public record DadosAuditoria(String ipOrigem, UUID idCorrelacao, Optional<UUID> idOperador) {}