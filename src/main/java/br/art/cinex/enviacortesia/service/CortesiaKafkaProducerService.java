package br.art.cinex.enviacortesia.service;

 
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import br.art.cinex.enviacortesia.domain.dto.CortesiaProcessamentoResponse;

@Service
public class CortesiaKafkaProducerService {

    private static final Logger logger = LoggerFactory.getLogger(CortesiaKafkaProducerService.class);

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.cortesia-processada:cortesia-processada}")
    private String topicoResposta;

    @Value("${app.kafka.topics.cortesia-erro:cortesia-erro}")
    private String topicoErro;

    @Value("${app.kafka.producer.timeout-seconds:30}")
    private int timeoutSeconds;

    /**
     * Envia resposta de processamento para o tópico apropriado
     */
    public void enviarResposta(CortesiaProcessamentoResponse response) {
        if (response == null) {
            logger.error("❌ Tentativa de enviar resposta null");
            return;
        }

        String topico = response.isSucesso() ? topicoResposta : topicoErro;
        String chave = gerarChaveResposta(response);

        logger.info("📤 Enviando resposta [{}] para tópico: {}", response.getRequestId(), topico);
        logger.debug("📋 Detalhes da resposta: {}", response.getDetalhesLog());

        try {
            // Criar record com headers customizados
            ProducerRecord<String, Object> record = criarRecordResposta(topico, chave, response);

            // Enviar de forma assíncrona com callback
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(record);

            // Configurar callbacks
            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    tratarErroEnvio(response, topico, ex);
                } else {
                    tratarSucessoEnvio(response, topico, result);
                }
            });

            // Aguardar com timeout (opcional, para casos críticos)
            if (response.getStatus() == CortesiaProcessamentoResponse.StatusProcessamento.ERRO_INTERNO) {
                try {
                    future.get(timeoutSeconds, TimeUnit.SECONDS);
                    logger.debug("✅ Resposta crítica [{}] enviada com confirmação", response.getRequestId());
                } catch (Exception e) {
                    logger.error("❌ Timeout ao enviar resposta crítica [{}]: {}", response.getRequestId(), e.getMessage());
                }
            }

        } catch (Exception e) {
            logger.error("❌ Erro ao preparar envio da resposta [{}]: {}", response.getRequestId(), e.getMessage(), e);
            // Tentar envio de emergência
            tentarEnvioEmergencia(response, e);
        }
    }

    /**
     * Envia resposta de sucesso
     */
    public void enviarRespostaSucesso(CortesiaProcessamentoResponse response) {
        logger.info("✅ Enviando resposta de sucesso [{}]", response.getRequestId());
        enviarResposta(response);
    }

    /**
     * Envia resposta de erro
     */
    public void enviarRespostaErro(CortesiaProcessamentoResponse response) {
        logger.warn("⚠️ Enviando resposta de erro [{}]: {}", response.getRequestId(), response.getMensagem());
        enviarResposta(response);
    }

    /**
     * Envia notificação de erro crítico
     */
    public void enviarErroCritico(String requestId, String email, String erro, Exception exception) {
        logger.error("🚨 Enviando erro crítico [{}]", requestId);

        CortesiaProcessamentoResponse response = CortesiaProcessamentoResponse.erro(
            requestId,
            email,
            null,
            null,
            "Erro crítico no sistema",
            erro,
            CortesiaProcessamentoResponse.StatusProcessamento.ERRO_INTERNO
        );

        if (exception != null) {
            response.setObservacoes("Exception: " + exception.getClass().getSimpleName() + " - " + exception.getMessage());
        }

        enviarResposta(response);
    }

    /**
     * Cria record do Kafka com headers customizados
     */
    private ProducerRecord<String, Object> criarRecordResposta(String topico, String chave, CortesiaProcessamentoResponse response) {
        ProducerRecord<String, Object> record = new ProducerRecord<>(topico, chave, response);

        // Adicionar headers customizados
        record.headers().add("requestId", response.getRequestId().getBytes());
        record.headers().add("email", response.getEmail() != null ? response.getEmail().getBytes() : "unknown".getBytes());
        record.headers().add("sucesso", String.valueOf(response.isSucesso()).getBytes());
        if (response != null && response.getStatus() != null) {
            record.headers().add("status", response.getStatus().name().getBytes());
        } else {
            // Valor padrão quando status é null
            record.headers().add("status", "UNKNOWN".getBytes());
            logger.warn("Status da resposta é null, usando valor padrão: UNKNOWN");
        }
        record.headers().add("timestamp", String.valueOf(System.currentTimeMillis()).getBytes());

        if (response.getFormato() != null) {
            record.headers().add("formato", response.getFormato().getBytes());
        }

        if (response.getQuantidade() != null) {
            record.headers().add("quantidade", response.getQuantidade().toString().getBytes());
        }

        if (response.getPraca() != null) {
            record.headers().add("praca", response.getPraca().getBytes());
        }

        logger.debug("📋 Record criado com {} headers", record.headers().toArray().length);
        return record;
    }

    /**
     * Gera chave para particionamento das respostas
     */
    private String gerarChaveResposta(CortesiaProcessamentoResponse response) {
        if (response.getEmail() != null) {
            // Usar hash do email para garantir que respostas do mesmo usuário vão para a mesma partição
            return String.format("email_%d", Math.abs(response.getEmail().hashCode()) % 1000);
        } else if (response.getRequestId() != null) {
            return String.format("req_%s", response.getRequestId());
        } else {
            return String.format("unknown_%d", System.currentTimeMillis() % 1000);
        }
    }

    /**
     * Trata sucesso no envio
     */
    private void tratarSucessoEnvio(CortesiaProcessamentoResponse response, String topico, SendResult<String, Object> result) {
        RecordMetadata metadata = result.getRecordMetadata();
        
        logger.info("✅ Resposta [{}] enviada com sucesso", response.getRequestId());
        logger.debug("📊 Detalhes: Tópico={}, Partição={}, Offset={}, Timestamp={}", 
                    metadata.topic(), metadata.partition(), metadata.offset(), metadata.timestamp());

        // Métricas de sucesso (implementar se necessário)
        registrarMetricaSucesso(response, metadata);
    }

    /**
     * Trata erro no envio
     */
    private void tratarErroEnvio(CortesiaProcessamentoResponse response, String topico, Throwable ex) {
        logger.error("❌ Erro ao enviar resposta [{}] para tópico {}: {}", 
                    response.getRequestId(), topico, ex.getMessage(), ex);

        // Métricas de erro (implementar se necessário)
        registrarMetricaErro(response, ex);

        // Tentar reenvio em caso de erro recuperável
        if (isErroRecuperavel(ex)) {
            logger.info("🔄 Tentando reenvio para resposta [{}]", response.getRequestId());
            tentarReenvio(response, topico);
        }
    }

    /**
     * Verifica se o erro é recuperável
     */
    private boolean isErroRecuperavel(Throwable ex) {
        String mensagem = ex.getMessage().toLowerCase();
        
        // Erros de rede/timeout são recuperáveis
        return mensagem.contains("timeout") || 
               mensagem.contains("connection") || 
               mensagem.contains("network") ||
               mensagem.contains("retriable");
    }

    /**
     * Tenta reenvio da resposta
     */
    private void tentarReenvio(CortesiaProcessamentoResponse response, String topico) {
        try {
            // Aguardar um pouco antes do reenvio
            Thread.sleep(1000);
            
            String chave = gerarChaveResposta(response);
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topico, chave, response);
            
            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    logger.error("❌ Reenvio falhou para [{}]: {}", response.getRequestId(), ex.getMessage());
                } else {
                    logger.info("✅ Reenvio bem-sucedido para [{}]", response.getRequestId());
                }
            });
            
        } catch (Exception e) {
            logger.error("❌ Erro no reenvio para [{}]: {}", response.getRequestId(), e.getMessage());
        }
    }

    /**
     * Tenta envio de emergência em caso de falha crítica
     */
    private void tentarEnvioEmergencia(CortesiaProcessamentoResponse response, Exception erroOriginal) {
        try {
            logger.warn("🚨 Tentando envio de emergência para [{}]", response.getRequestId());
            
            // Criar resposta simplificada
            CortesiaProcessamentoResponse respostaEmergencia = CortesiaProcessamentoResponse.builder()
                .requestId(response.getRequestId())
                .email(response.getEmail())
                .sucesso(false)
                .mensagem("Erro no sistema de mensageria")
                .erro("Falha ao enviar resposta original: " + erroOriginal.getMessage())
                .status(CortesiaProcessamentoResponse.StatusProcessamento.ERRO_INTERNO)
                .build();

            // Envio simples sem callbacks
            kafkaTemplate.send(topicoErro, response.getRequestId(), respostaEmergencia);
            
            logger.info("🚨 Envio de emergência realizado para [{}]", response.getRequestId());
            
        } catch (Exception e) {
            logger.error("💥 Falha total no envio de emergência para [{}]: {}", response.getRequestId(), e.getMessage());
        }
    }

    /**
     * Registra métrica de sucesso
     */
    private void registrarMetricaSucesso(CortesiaProcessamentoResponse response, RecordMetadata metadata) {
        // Implementar integração com sistema de métricas (Micrometer, Prometheus, etc.)
        logger.debug("📈 Métrica de sucesso registrada para [{}]", response.getRequestId());
    }

    /**
     * Registra métrica de erro
     */
    private void registrarMetricaErro(CortesiaProcessamentoResponse response, Throwable ex) {
        // Implementar integração com sistema de métricas
        logger.debug("📉 Métrica de erro registrada para [{}]: {}", response.getRequestId(), ex.getClass().getSimpleName());
    }

    /**
     * Flush manual do producer (para casos críticos)
     */
    public void flush() {
        try {
            kafkaTemplate.flush();
            logger.debug("🔄 Producer flush executado");
        } catch (Exception e) {
            logger.error("❌ Erro no flush do producer: {}", e.getMessage());
        }
    }

    /**
     * Health check do producer
     */
    public boolean isHealthy() {
        try {
            // Tentar enviar uma mensagem de teste (opcional)
            return kafkaTemplate != null;
        } catch (Exception e) {
            logger.error("❌ Health check do producer falhou: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Obtém métricas do producer
     */
    public String getMetricas() {
        try {
            return String.format("Producer ativo - Tópicos: [%s, %s]", topicoResposta, topicoErro);
        } catch (Exception e) {
            return "Erro ao obter métricas: " + e.getMessage();
        }
    }

    /**
     * Método para teste/debug - envia mensagem de teste
     */
    public void enviarMensagemTeste() {
        CortesiaProcessamentoResponse teste = CortesiaProcessamentoResponse.builder()
            .requestId("TEST_" + System.currentTimeMillis())
            .email("teste@exemplo.com")
            .sucesso(true)
            .mensagem("Mensagem de teste")
            .status(CortesiaProcessamentoResponse.StatusProcessamento.SUCESSO)
            .build();

        enviarResposta(teste);
        logger.info("📤 Mensagem de teste enviada");
    }




    
}
