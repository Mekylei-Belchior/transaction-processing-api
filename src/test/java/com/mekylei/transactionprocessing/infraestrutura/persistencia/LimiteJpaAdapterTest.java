package com.mekylei.transactionprocessing.infraestrutura.persistencia;

import com.mekylei.transactionprocessing.conta.dominio.LimiteTransacional;
import com.mekylei.transactionprocessing.transacao.dominio.TipoTransacao;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(LimiteJpaAdapter.class)
class LimiteJpaAdapterTest {

    @Autowired
    private LimiteJpaAdapter adapter;

    @Test
    void findByIdContaAndTipo_deveRetornarLimiteCorreto() {
        UUID idConta = UUID.randomUUID();
        LimiteTransacional pix = adapter.save(novoLimite(idConta, TipoTransacao.PIX));
        adapter.save(novoLimite(idConta, TipoTransacao.TED));

        Optional<LimiteTransacional> encontrado = adapter.findByIdContaAndTipo(idConta, TipoTransacao.PIX);

        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getId()).isEqualTo(pix.getId());
        assertThat(encontrado.get().getTipo()).isEqualTo(TipoTransacao.PIX);
    }

    @Test
    void findByIdContaAndTipoForUpdate_deveRetornarLimite() {
        LimiteTransacional salvo = adapter.save(novoLimite(UUID.randomUUID(), TipoTransacao.TEF));

        Optional<LimiteTransacional> encontrado =
                adapter.findByIdContaAndTipoForUpdate(salvo.getIdConta(), TipoTransacao.TEF);

        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getId()).isEqualTo(salvo.getId());
        assertThat(encontrado.get().getIdConta()).isEqualTo(salvo.getIdConta());
    }

    @Test
    void save_deveAtualizarLimiteExistente() {
        LimiteTransacional salvo = adapter.save(novoLimite(UUID.randomUUID(), TipoTransacao.PIX));
        LimiteTransacional alterado = LimiteTransacional.builder()
                .id(salvo.getId())
                .idConta(salvo.getIdConta())
                .tipo(salvo.getTipo())
                .limiteDiario(new BigDecimal("5000.00"))
                .limiteTransacao(new BigDecimal("1000.00"))
                .utilizadoHoje(new BigDecimal("300.00"))
                .dataReferencia(salvo.getDataReferencia())
                .versao(salvo.getVersao())
                .build();

        adapter.save(alterado);

        Optional<LimiteTransacional> encontrado = adapter.findByIdContaAndTipo(salvo.getIdConta(), salvo.getTipo());
        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getLimiteDiario()).isEqualByComparingTo("5000.00");
        assertThat(encontrado.get().getLimiteTransacao()).isEqualByComparingTo("1000.00");
        assertThat(encontrado.get().getUtilizadoHoje()).isEqualByComparingTo("300.00");
    }

    private LimiteTransacional novoLimite(UUID idConta, TipoTransacao tipo) {
        return LimiteTransacional.builder()
                .idConta(idConta)
                .tipo(tipo)
                .limiteDiario(new BigDecimal("3000.00"))
                .limiteTransacao(new BigDecimal("500.00"))
                .utilizadoHoje(BigDecimal.ZERO)
                .build();
    }
}
