package com.mekylei.transactionprocessing.infraestrutura.persistencia;

import com.mekylei.transactionprocessing.compartilhado.seguranca.HmacService;
import com.mekylei.transactionprocessing.compartilhado.util.CriptografiaConverter;
import com.mekylei.transactionprocessing.configuracao.persistencia.HmacProperties;
import com.mekylei.transactionprocessing.conta.dominio.Conta;
import com.mekylei.transactionprocessing.conta.dominio.StatusConta;
import com.mekylei.transactionprocessing.conta.dominio.TipoConta;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({ContaJpaAdapter.class, HmacService.class, CriptografiaConverter.class})
@EnableConfigurationProperties(HmacProperties.class)
class ContaJpaAdapterTest {

    @Autowired
    private ContaJpaAdapter adapter;

    @Test
    void findById_deveRetornarContaSalva() {
        Conta salva = adapter.save(novaConta("00012345"));

        Optional<Conta> encontrada = adapter.findById(salva.getId());

        assertThat(encontrada).isPresent();
        assertThat(encontrada.get().getId()).isEqualTo(salva.getId());
        assertThat(encontrada.get().getNumeroConta()).isEqualTo("00012345");
        assertThat(encontrada.get().getStatus()).isEqualTo(StatusConta.ATIVA);
    }

    @Test
    void findByNumeroConta_deveEncontrarPorHmacIndex() {
        Conta salva = adapter.save(novaConta("00098765"));

        Optional<Conta> encontrada = adapter.findByNumeroContaHmac("00098765");

        assertThat(encontrada).isPresent();
        assertThat(encontrada.get().getId()).isEqualTo(salva.getId());
        assertThat(encontrada.get().getNumeroConta()).isEqualTo("00098765");
    }

    @Test
    void findByNumeroConta_deveRetornarEmptyParaNumeroInexistente() {
        adapter.save(novaConta("00055555"));

        Optional<Conta> encontrada = adapter.findByNumeroContaHmac("00000000");

        assertThat(encontrada).isEmpty();
    }

    private Conta novaConta(String numeroConta) {
        return Conta.builder()
                .numeroConta(numeroConta)
                .agencia("0001")
                .idCliente(UUID.randomUUID())
                .tipo(TipoConta.CORRENTE)
                .status(StatusConta.ATIVA)
                .build();
    }
}
