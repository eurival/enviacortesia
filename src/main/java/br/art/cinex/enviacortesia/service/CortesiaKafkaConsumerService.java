package br.art.cinex.enviacortesia.service;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import br.art.cinex.enviacortesia.domain.dto.CortesiaGeracaoRequest;
import br.art.cinex.enviacortesia.domain.dto.CortesiaProcessamentoResponse;
import br.art.cinex.enviacortesia.domain.dto.CortesiaProcessamentoResponse.StatusProcessamento;
import jakarta.validation.Valid;

@Service
@Validated
public class CortesiaKafkaConsumerService {

    private static final Logger logger = LoggerFactory.getLogger(CortesiaKafkaConsumerService.class);

    @Autowired
    private CortesiaProcessamentoService cortesiaProcessamentoService;

    @Autowired
    private CortesiaKafkaProducerService cortesiaKafkaProducerService;

    /**
     * Consumer principal para processar solicitações de cortesia
     */
    @KafkaListener(
        topics = "cortesia-para-emitir", 
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void processarSolicitacaoCortesia(
            @Payload @Valid CortesiaGeracaoRequest request,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(value = KafkaHeaders.RECEIVED_KEY, required = false) String key,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment,
            ConsumerRecord<String, CortesiaGeracaoRequest> consumerRecord) {

        long inicioProcessamento = System.currentTimeMillis();
        String requestId = gerarRequestId(request, offset);

        logger.info("=== NOVA SOLICITAÇÃO RECEBIDA ===");
        logger.info("RequestId: {}", requestId);
        logger.info("Tópico: {}, Partição: {}, Offset: {}, Key: {}", topic, partition, offset, key);
        logger.info("Timestamp da mensagem: {}", consumerRecord.timestamp());
        logger.info("Solicitação: {}", request);
        
        CortesiaProcessamentoResponse response = null;
        
        try {
            // 1. Validar request básico
            if (!validarRequestBasico(request, requestId)) {
                response = CortesiaProcessamentoResponse.erroValidacao(requestId, "Dados da solicitação são inválidos");
                enviarResposta(response);
                acknowledgment.acknowledge();
                return;
            }

            // 2. Log de início do processamento
            logger.info("🚀 Iniciando processamento [{}] - Praça: {}, Quantidade: {}, Email: {}", 
                       requestId, request.getPraca(), request.getQuantidade(), request.getEmail());

            // 3. Processar solicitação
            response = cortesiaProcessamentoService.processarSolicitacao(request);
            response.setRequestId(requestId);
            response = response.comTempoProcessamento(inicioProcessamento);

            // 4. Log do resultado
            if (response.isSucesso()) {
                logger.info("✅ Processamento [{}] concluído com sucesso: {}", requestId, response.getMensagem());
                logger.info("📊 Métricas: {}", response.getDetalhesLog());
            } else {
                logger.warn("⚠️ Processamento [{}] falhou: {}", requestId, response.getMensagem());
                if (response.getErro() != null) {
                    logger.warn("🔍 Detalhes do erro: {}", response.getErro());
                }
            }

            // 5. Enviar resposta
            enviarResposta(response);

            // 6. Confirmar processamento
            acknowledgment.acknowledge();
            logger.info("✅ Mensagem [{}] confirmada no Kafka", requestId);

        } catch (Exception e) {
            logger.error("❌ Erro crítico no processamento [{}]: {}", requestId, e.getMessage(), e);
            
            // Criar resposta de erro
            response = CortesiaProcessamentoResponse.erro(
                requestId, 
                request != null ? request.getEmail() : "unknown",
                request != null ? request.getQuantidade() : 0,
                request != null ? request.getPraca() : "unknown",
                "Erro interno do sistema",
                e.getMessage(),
                StatusProcessamento.ERRO_INTERNO
            ).comTempoProcessamento(inicioProcessamento);
            
            // Tentar enviar resposta de erro
            try {
                enviarResposta(response);
            } catch (Exception ex) {
                logger.error("❌ Erro ao enviar resposta de erro [{}]: {}", requestId, ex.getMessage(), ex);
            }
            
            // Confirmar mesmo com erro para evitar reprocessamento infinito
            acknowledgment.acknowledge();
            logger.info("⚠️ Mensagem [{}] confirmada após erro para evitar reprocessamento", requestId);
            
        } finally {
            long tempoTotal = System.currentTimeMillis() - inicioProcessamento;
            logger.info("⏱️ Tempo total de processamento [{}]: {}ms", requestId, tempoTotal);
            logger.info("=== PROCESSAMENTO [{}] FINALIZADO ===\n", requestId);
        }
    }

    /**
     * Consumer para processar respostas (se necessário para monitoramento)
     */
    @KafkaListener(
        topics = "cortesia-processada", 
        groupId = "${spring.kafka.consumer.group-id}-monitor",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void monitorarRespostas(
            @Payload CortesiaProcessamentoResponse response,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        logger.info("📊 Monitoramento - Resposta recebida: {}", response.getRequestId());
        logger.info("📈 Status: {}, Sucesso: {}, Tempo: {}ms", 
                   response.getStatus(), response.isSucesso(), response.getTempoProcessamentoMs());
        
        // Aqui você pode implementar lógica de monitoramento, métricas, etc.
        // Por exemplo: salvar em banco de dados, enviar para sistema de métricas, etc.
        
        acknowledgment.acknowledge();
    }

    /**
     * Valida os dados básicos da requisição
     */
    private boolean validarRequestBasico(CortesiaGeracaoRequest request, String requestId) {
        if (request == null) {
            logger.error("❌ [{}] Request é null", requestId);
            return false;
        }

        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            logger.error("❌ [{}] Email é obrigatório", requestId);
            return false;
        }

        if (!isValidEmail(request.getEmail())) {
            logger.error("❌ [{}] Email inválido: {}", requestId, request.getEmail());
            return false;
        }

        if (request.getQuantidade() == null || request.getQuantidade() <= 0) {
            logger.error("❌ [{}] Quantidade deve ser maior que zero. Valor: {}", requestId, request.getQuantidade());
            return false;
        }

        if (request.getQuantidade() > 1000) { // Limite máximo
            logger.error("❌ [{}] Quantidade excede o limite máximo (1000). Valor: {}", requestId, request.getQuantidade());
            return false;
        }

        if (request.getPraca() == null || request.getPraca().trim().isEmpty()) {
            logger.error("❌ [{}] Praça é obrigatória", requestId);
            return false;
        }

        if (request.getValidadeImpressao() == null) {
            logger.error("❌ [{}] Validade de impressão é obrigatória", requestId);
            return false;
        }

        if (request.getFormato() == null || request.getFormato().trim().isEmpty()) {
            logger.warn("⚠️ [{}] Formato não especificado, usando 'zip' como padrão", requestId);
            request.setFormato("zip");
        }

        String formato = request.getFormato().toLowerCase();
        if (!formato.equals("pdf") && !formato.equals("zip")) {
            logger.error("❌ [{}] Formato inválido: {}. Deve ser 'pdf' ou 'zip'", requestId, request.getFormato());
            return false;
        }

        logger.debug("✅ [{}] Validação básica passou", requestId);
        return true;
    }

    /**
     * Valida formato do email
     */
    private boolean isValidEmail(String email) {
        return email != null && 
               email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    /**
     * Envia resposta para o tópico de respostas
     */
    private void enviarResposta(CortesiaProcessamentoResponse response) {
        try {
            cortesiaKafkaProducerService.enviarResposta(response);
            logger.debug("📤 Resposta enviada para o tópico: {}", response.getRequestId());
        } catch (Exception e) {
            logger.error("❌ Erro ao enviar resposta [{}]: {}", response.getRequestId(), e.getMessage(), e);
            // Não relançar a exceção para não afetar o acknowledgment
        }
    }

    /**
     * Gera ID único para a requisição
     */
    private String gerarRequestId(CortesiaGeracaoRequest request, long offset) {
        if (request == null) {
            return String.format("REQ_NULL_%d_%d", offset, System.currentTimeMillis());
        }
        
        return String.format("REQ_%s_%s_%d_%d_%d", 
                Math.abs(request.getEmail().hashCode()) % 10000,
                request.getPraca().replaceAll("\\s+", "").toUpperCase(),
                request.getQuantidade(),
                offset,
                System.currentTimeMillis() % 100000);
    }

    /**
     * Método para health check do consumer
     */
    public boolean isHealthy() {
        try {
            // Verificar se os serviços dependentes estão funcionando
            return cortesiaProcessamentoService != null && 
                   cortesiaKafkaProducerService != null;
        } catch (Exception e) {
            logger.error("❌ Health check falhou: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Obtém estatísticas do consumer (se necessário)
     */
    public String getEstatisticas() {
        // Implementar se necessário para monitoramento
        return String.format("Consumer ativo - Timestamp: %d", System.currentTimeMillis());
    }
}
