package br.art.cinex.enviacortesia.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import br.art.cinex.enviacortesia.domain.dto.CadastropromocaoDTO;
import br.art.cinex.enviacortesia.domain.dto.CortesiaGeracaoRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class CadastroPromocaoScheduler {

    private final CadastroPromocaoClient client;
    private final CortesiaSolicitacaoProducer producer;

    @Value("${app.cortesia.quantidade-padrao}")
    private Integer quantidadePadrao;

   // @Value("${app.cortesia.destinacao}")
    private String destinacao="Promoção de férias Cinex";

    @Value("${app.cortesia.validade-impressao}")
    private LocalDate validadeImpressao;

    private int pageSize = 100; // Ajuste conforme necessário

    /** roda a cada minuto (ajuste como quiser) */
    @Scheduled(fixedDelayString = "PT1M")
    public void processarCadastros() {

        try {
            int total = 0;
            List<CadastropromocaoDTO> pendentes;

            do {
                pendentes = client.buscarNaoEnviados(pageSize);
                if (pendentes ==null || pendentes.isEmpty()) {
                  //  log.info("✅ Nenhum cadastro pendente encontrado");
                   // log.info("✅ Nenhum cadastro pendente encontrado");
                    return; // sai se não houver pendentes
                }
                for (CadastropromocaoDTO dto : pendentes) {
                    producer.enviarSolicitacao(mapear(dto));
                    client.marcarEnviado(dto);
                    total++;
                }

            } while (!pendentes.isEmpty());   // para quando a página voltar vazia

            log.info("✅ Scheduler concluiu {} cadastros pendentes", total);

        } catch (Exception e) {
            log.error("❌ Erro no scheduler", e);
        }
    }

    private CortesiaGeracaoRequest mapear(CadastropromocaoDTO dto) {
        return CortesiaGeracaoRequest.builder()
                .praca("Online")
                .quantidade(quantidadePadrao)
                .email(dto.getEmail())
                .quemSolicitou(dto.getNome())
                .destinacao(destinacao)
                .validadeImpressao(validadeImpressao)
                .formato("pdf")
                .build();
    }
}