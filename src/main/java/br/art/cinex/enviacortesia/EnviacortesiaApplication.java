package br.art.cinex.enviacortesia;



import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import br.art.cinex.enviacortesia.service.CortesiaKafkaProducerService;
import jakarta.annotation.PreDestroy;

@SpringBootApplication
@EnableKafka
@EnableAsync
@EnableScheduling
public class EnviacortesiaApplication implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(EnviacortesiaApplication.class);

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${server.port}")
    private String serverPort;

    @Autowired
    private Environment environment;

    @Autowired
    private CortesiaKafkaProducerService producerService;

    /**
     * Método principal da aplicação
     */
    public static void main(String[] args) {
        try {
            // Configurar timezone e propriedades do sistema
            TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"));
            System.setProperty("file.encoding", "UTF-8");
            System.setProperty("java.awt.headless", "true");
            
            logger.info("🚀 Iniciando Cortesia Processor Service...");
            
            SpringApplication.run(EnviacortesiaApplication.class, args);
            
        } catch (Exception e) {
            logger.error("💥 Erro fatal ao iniciar a aplicação: {}", e.getMessage(), e);
            System.exit(1);
        }
    }

    @Override
    public void run(String... args) throws Exception {
        logger.info("🔧 Executando verificações pós-inicialização...");
        
        if (args.length > 0) {
            logger.info("📋 Argumentos recebidos: {}", String.join(", ", args));
        }
        
        logger.info("✅ Verificações concluídas");
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        logger.info("🎉 ========================================");
        logger.info("🎉 CORTESIA PROCESSOR SERVICE INICIADO!");
        logger.info("🎉 ========================================");
        logger.info("📱 Aplicação: {}", applicationName);
        logger.info("🌐 Porta: {}", serverPort);
        logger.info("☕ Java Version: {}", System.getProperty("java.version"));
        
        // Exibir configurações principais
        String kafkaBootstrap = environment.getProperty("spring.kafka.bootstrap-servers");
        String consumerGroup = environment.getProperty("spring.kafka.consumer.group-id");
        logger.info("📡 Kafka Bootstrap: {}", kafkaBootstrap);
        logger.info("👥 Consumer Group: {}", consumerGroup);
        
        logger.info("🎯 Aplicação pronta para processar solicitações de cortesia!");
        logger.info("📊 Monitoramento: http://localhost:{}/actuator/health", 
                   environment.getProperty("management.server.port", "8081"));
    }

    @PreDestroy
    public void onShutdown() {
        logger.info("🛑 Iniciando shutdown da aplicação...");
        
        try {
            if (producerService != null) {
                logger.info("📤 Fazendo flush do producer Kafka...");
                producerService.flush();
            }
            
            Thread.sleep(2000);
            logger.info("✅ Shutdown concluído com sucesso");
            
        } catch (Exception e) {
            logger.error("❌ Erro durante o shutdown: {}", e.getMessage(), e);
        }
    }
}
