package br.art.cinex.enviacortesia.service;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.art.cinex.enviacortesia.domain.Cortesia;
import br.art.cinex.enviacortesia.domain.User;
import br.art.cinex.enviacortesia.domain.dto.CortesiaGeracaoRequest;
import br.art.cinex.enviacortesia.domain.dto.CortesiaProcessamentoResponse;


@Service
public class CortesiaProcessamentoService {

    private static final Logger logger = LoggerFactory.getLogger(CortesiaProcessamentoService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    private CortesiaService cortesiaService;

    @Autowired
    private RelatorioService relatorioService;

    @Autowired
    private EmailService emailService;

    @Transactional
    public CortesiaProcessamentoResponse processarSolicitacao(CortesiaGeracaoRequest request) {
        String requestId = gerarRequestId(request);
        
        try {
            logger.info("Iniciando processamento [{}] para: {}", requestId, request.getEmail());

            // 1. Buscar cortesias disponíveis
            List<Cortesia> cortesiasDisponiveis = cortesiaService
                    .buscarCortesiasDisponiveis("Todas", request.getQuantidade());

            logger.info("Encontradas {} cortesias disponíveis para praça: {}", 
                       cortesiasDisponiveis.size(), request.getPraca());

            // 2. Verificar disponibilidade
            if (cortesiasDisponiveis.size() < request.getQuantidade()) {
                String mensagem = String.format("Cortesias insuficientes. Solicitadas: %d, Disponíveis: %d", 
                                               request.getQuantidade(), cortesiasDisponiveis.size());
                logger.warn("Processamento [{}] falhou: {}", requestId, mensagem);
                
                return CortesiaProcessamentoResponse.builder()
                        .requestId(requestId)
                        .email(request.getEmail())
                        .quantidade(request.getQuantidade())
                        .praca(request.getPraca())
                        .sucesso(false)
                        .mensagem(mensagem)
                        .dataProcessamento(LocalDateTime.now())
                        .build();
            }

            // 3. Preparar cortesias para geração
            List<Cortesia> cortesiasParaGerar = prepararCortesias(cortesiasDisponiveis, request);
            logger.info("Preparadas {} cortesias para geração", cortesiasParaGerar.size());

            // 4. Atualizar status das cortesias no banco
            atualizarStatusCortesias(cortesiasParaGerar, request);
            logger.info("Status das cortesias atualizado para 'Emitido'");

            // 5. Gerar relatório
            ByteArrayOutputStream relatorio = gerarRelatorio(cortesiasParaGerar, request.getFormato());
            logger.info("Relatório gerado com sucesso no formato: {}", request.getFormato());

            // 6. Enviar por email
            enviarEmailComRelatorio(request, relatorio);
            logger.info("Email enviado com sucesso para: {}", request.getEmail());

            // 7. Retornar resposta de sucesso
            String mensagemSucesso = String.format("%d cortesias geradas com sucesso! Enviadas para: %s", 
                                                  request.getQuantidade(), request.getEmail());
            
            logger.info("Processamento [{}] concluído com sucesso", requestId);
            
            return CortesiaProcessamentoResponse.builder()
                    .requestId(requestId)
                    .email(request.getEmail())
                    .quantidade(request.getQuantidade())
                    .praca(request.getPraca())
                    .sucesso(true)
                    .mensagem(mensagemSucesso)
                    .dataProcessamento(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            logger.error("Erro no processamento [{}]: {}", requestId, e.getMessage(), e);
            
            return CortesiaProcessamentoResponse.builder()
                    .requestId(requestId)
                    .email(request.getEmail())
                    .quantidade(request.getQuantidade())
                    .praca(request.getPraca())
                    .sucesso(false)
                    .mensagem("Erro interno no processamento")
                    .erro(e.getMessage())
                    .dataProcessamento(LocalDateTime.now())
                    .build();
        }
    }

    private List<Cortesia> prepararCortesias(List<Cortesia> cortesiasOriginais, CortesiaGeracaoRequest request) {
        List<Cortesia> cortesiasParaGerar = new ArrayList<>();
        
        for (int i = 0; i < request.getQuantidade(); i++) {
            Cortesia cortesiaOriginal = cortesiasOriginais.get(i);
            Cortesia cortesiaCopia = new Cortesia();
            
            // Copiar propriedades da cortesia original
            BeanUtils.copyProperties(cortesiaOriginal, cortesiaCopia);
            
            // Definir validade de impressão
            cortesiaCopia.setValidadeImpressao(request.getValidadeImpressao().format(DATE_FORMATTER));
            
            cortesiasParaGerar.add(cortesiaCopia);
        }
        
        return cortesiasParaGerar;
    }

    private void atualizarStatusCortesias(List<Cortesia> cortesias, CortesiaGeracaoRequest request) {
        User usuario = new User(); // Obter usuário do contexto ou serviço
        usuario.setId(1);
        cortesias.forEach(cortesia -> {
            cortesia.setStatus("Emitido");
            cortesia.setSolicitante(usuario); // ou pegar de algum contexto
            cortesia.setDataEmissao(LocalDate.now());
            cortesia.setEmailEmissao(request.getEmail());
            cortesia.setQuemsolicitou(request.getQuemSolicitou());
            cortesia.setDestinacao(request.getDestinacao());
            cortesia.setValidadeImpressao(request.getValidadeImpressao().format(DATE_FORMATTER));
            
            // Salvar no banco
            try {
                cortesiaService.atualizar(cortesia);
            } catch (Exception e) {
                logger.error("Erro ao atualizar cortesia ID {}: {}", cortesia.getId(), e.getMessage(), e);
                throw new RuntimeException("Erro ao atualizar cortesia", e);
            }

        });
    }

    private ByteArrayOutputStream gerarRelatorio(List<Cortesia> cortesias, String formato) throws Exception {
        if ("pdf".equalsIgnoreCase(formato)) {
            return relatorioService.gerarRelatorioPDF(cortesias);
        } else if ("zip".equalsIgnoreCase(formato)) {
            return relatorioService.gerarRelatorioZip(cortesias);
        } else {
            throw new IllegalArgumentException("Formato não suportado: " + formato);
        }
    }

    private void enviarEmailComRelatorio(CortesiaGeracaoRequest request, ByteArrayOutputStream relatorio) {
        String assunto = "Solicitação de Cortesias – Roleta CineX"; ;

        String texto = String.format(
            """
            Olá %s,

            Você se cadastrou na promoção **Roleta CineX**. Seguem em anexo as suas cortesias:

            • Quantidade...............: %d
            • Solicitante..............: %s
            • Destinação...............: %s
            • Validade.................: A partir de %s

            Bom filme! 🍿

            Atenciosamente,
            Sistema de Emissão de Cortesias
            CineX
            """,
            request.getQuemSolicitou(),          // %s – nome ou identificação do solicitante
            request.getQuantidade(),             // %d – quantidade de cortesias
            request.getQuemSolicitou(),          // %s – repetido se for mesmo campo; troque se houver outro
            request.getDestinacao(),             // %s – destinação (e.g., TESTE)
            request.getValidadeImpressao()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))  // %s – data
        );
        
        String nomeArquivo = String.format("cortesias_%s_%s.%s", 
                                          request.getPraca().replaceAll("\\s+", "_"),
                                          LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")),
                                          "zip".equalsIgnoreCase(request.getFormato()) ? "zip" : "pdf");
        
        emailService.enviarEmailComAnexo(request.getEmail(), assunto, texto, relatorio, nomeArquivo);
    }

    private String gerarRequestId(CortesiaGeracaoRequest request) {
        return String.format("REQ_%s_%s_%d_%d", 
                request.getEmail().hashCode() & 0x7fffffff, // Remove sinal negativo
                request.getPraca().replaceAll("\\s+", ""),
                request.getQuantidade(),
                System.currentTimeMillis());
    }
}
