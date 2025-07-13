package br.art.cinex.enviacortesia.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import br.art.cinex.enviacortesia.domain.dto.CortesiaGeracaoRequest;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CortesiaSolicitacaoProducer {

    private static final Logger log = LoggerFactory.getLogger(CortesiaSolicitacaoProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.cortesia-para-emitir}")
    private String topic;

    public void enviarSolicitacao(CortesiaGeracaoRequest req) {
        String chave = Optional.ofNullable(req.getEmail())
                               .orElse("anon")                           // fallback
                               .toLowerCase();

        kafkaTemplate.send(topic, chave, req)
                     .whenComplete((res, ex) -> {
                         if (ex != null) {
                             log.error("❌ Falha ao enviar solicitação [{}]: {}", chave, ex.getMessage(), ex);
                         } else {
                             log.info ("✅ Solicitação enviada – tópico {}, partição {}, offset {}",
                                       res.getRecordMetadata().topic(),
                                       res.getRecordMetadata().partition(),
                                       res.getRecordMetadata().offset());
                         }
                     });
    }
}
