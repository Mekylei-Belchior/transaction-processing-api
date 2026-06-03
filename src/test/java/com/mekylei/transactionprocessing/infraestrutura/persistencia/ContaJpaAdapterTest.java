package com.mekylei.transactionprocessing.infraestrutura.persistencia;

import com.mekylei.transactionprocessing.compartilhado.seguranca.HmacService;
import com.mekylei.transactionprocessing.compartilhado.util.CriptografiaConverter;
import com.mekylei.transactionprocessing.configuracao.persistencia.HmacProperties;
import com.mekylei.transactionprocessing.conta.dominio.Conta;
import com.mekylei.transactionprocessing.conta.dominio.StatusConta;
import com.mekylei.transactionprocessing.conta.dominio.TipoConta;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitários para {@link ContaJpaAdapter}.
 *
 * <p>Objetivo:</p>
 * <ul>
 *     <li>Validar o comportamento esperado de {@link ContaJpaAdapter} nos cenários exercitados pela suíte.</li>
 *     <li>Preservar regras de negócio, contratos, integrações ou invariantes aplicáveis à classe testada.</li>
 *     <li>Garantir regressão funcional para alterações futuras relacionadas a {@code ContaJpaAdapter}.</li>
 * </ul>
 *
 * <p>Cenários cobertos:</p>
 * <ul>
 *     <li>FindById deve retornar conta salva.</li>
 *     <li>FindByNumeroConta deve encontrar por HMAC index.</li>
 *     <li>FindByNumeroConta deve retornar empty para número inexistente.</li>
 * </ul>
 *
 * <p>Cenários não cobertos:</p>
 * <ul>
 *     <li>Testes de carga, resiliência distribuída e validações de infraestrutura externas ao escopo da classe.</li>
 * </ul>
 *
 * @author Mekylei Belchior
 * @since 1.0
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({ContaJpaAdapter.class, HmacService.class, CriptografiaConverter.class})
@EnableConfigurationProperties(HmacProperties.class)
@DisplayName("Conta Jpa Adapter")
class ContaJpaAdapterTest {

    @Autowired
    private ContaJpaAdapter adapter;

    @Test
    @DisplayName("findById deve retornar conta salva")
    void findById_deveRetornarContaSalva() {
        Conta salva = adapter.save(novaConta("00012345"));

        Optional<Conta> encontrada = adapter.findById(salva.getId());

        assertThat(encontrada).isPresent();
        assertThat(encontrada.get().getId()).isEqualTo(salva.getId());
        assertThat(encontrada.get().getNumeroConta()).isEqualTo("00012345");
        assertThat(encontrada.get().getStatus()).isEqualTo(StatusConta.ATIVA);
    }

    @Test
    @DisplayName("findByNumeroConta deve encontrar por HMAC index")
    void findByNumeroConta_deveEncontrarPorHmacIndex() {
        Conta salva = adapter.save(novaConta("00098765"));

        Optional<Conta> encontrada = adapter.findByNumeroContaHmac("00098765");

        assertThat(encontrada).isPresent();
        assertThat(encontrada.get().getId()).isEqualTo(salva.getId());
        assertThat(encontrada.get().getNumeroConta()).isEqualTo("00098765");
    }

    @Test
    @DisplayName("findByNumeroConta deve retornar empty para número inexistente")
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
