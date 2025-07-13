package br.art.cinex.enviacortesia.domain.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CortesiaProcessamentoResponse {

    /**
     * ID único da requisição para rastreamento
     */
    private String requestId;

    /**
     * Email do solicitante
     */
    private String email;

    /**
     * Quantidade de cortesias solicitadas
     */
    private Integer quantidade;

    /**
     * Praça solicitada
     */
    private String praca;

    /**
     * Indica se o processamento foi bem-sucedido
     */
    private boolean sucesso;

    /**
     * Mensagem de retorno (sucesso ou erro)
     */
    private String mensagem;

    /**
     * Detalhes do erro (apenas em caso de falha)
     */
    private String erro;

    /**
     * Data e hora do processamento
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dataProcessamento;

    /**
     * Tempo de processamento em milissegundos
     */
    private Long tempoProcessamentoMs;

    /**
     * Formato do relatório gerado (pdf, zip)
     */
    private String formato;

    /**
     * Tamanho do arquivo gerado em bytes
     */
    private Long tamanhoArquivoBytes;

    /**
     * Número de páginas/imagens geradas
     */
    private Integer numeroPaginas;

    /**
     * Status detalhado do processamento
     */
    private StatusProcessamento status;

    /**
     * Informações adicionais sobre o processamento
     */
    private String observacoes;

    /**
     * Enum para status detalhado
     */
    public enum StatusProcessamento {
        PROCESSANDO("Processando solicitação"),
        SUCESSO("Processamento concluído com sucesso"),
        ERRO_VALIDACAO("Erro de validação dos dados"),
        ERRO_DISPONIBILIDADE("Cortesias insuficientes"),
        ERRO_RELATORIO("Erro na geração do relatório"),
        ERRO_EMAIL("Erro no envio do email"),
        ERRO_INTERNO("Erro interno do sistema"),
        CANCELADO("Processamento cancelado");

        private final String descricao;

        StatusProcessamento(String descricao) {
            this.descricao = descricao;
        }

        public String getDescricao() {
            return descricao;
        }
    }

    // Métodos de conveniência para criação de respostas

    /**
     * Cria resposta de sucesso
     */
    public static CortesiaProcessamentoResponse sucesso(String requestId, String email, 
                                                       Integer quantidade, String praca, 
                                                       String mensagem) {
        return CortesiaProcessamentoResponse.builder()
                .requestId(requestId)
                .email(email)
                .quantidade(quantidade)
                .praca(praca)
                .sucesso(true)
                .mensagem(mensagem)
                .status(StatusProcessamento.SUCESSO)
                .dataProcessamento(LocalDateTime.now())
                .build();
    }

    /**
     * Cria resposta de erro
     */
    public static CortesiaProcessamentoResponse erro(String requestId, String email, 
                                                    Integer quantidade, String praca, 
                                                    String mensagem, String erro, 
                                                    StatusProcessamento status) {
        return CortesiaProcessamentoResponse.builder()
                .requestId(requestId)
                .email(email)
                .quantidade(quantidade)
                .praca(praca)
                .sucesso(false)
                .mensagem(mensagem)
                .erro(erro)
                .status(status != null ? status : StatusProcessamento.ERRO_INTERNO)
                .dataProcessamento(LocalDateTime.now())
                .build();
    }

    /**
     * Cria resposta de erro de validação
     */
    public static CortesiaProcessamentoResponse erroValidacao(String requestId, String mensagem) {
        return CortesiaProcessamentoResponse.builder()
                .requestId(requestId)
                .sucesso(false)
                .mensagem(mensagem)
                .status(StatusProcessamento.ERRO_VALIDACAO)
                .dataProcessamento(LocalDateTime.now())
                .build();
    }

    /**
     * Cria resposta de erro de disponibilidade
     */
    public static CortesiaProcessamentoResponse erroDisponibilidade(String requestId, String email,
                                                                   Integer quantidade, String praca,
                                                                   Integer disponivel) {
        String mensagem = String.format("Cortesias insuficientes. Solicitadas: %d, Disponíveis: %d", 
                                       quantidade, disponivel);
        
        return CortesiaProcessamentoResponse.builder()
                .requestId(requestId)
                .email(email)
                .quantidade(quantidade)
                .praca(praca)
                .sucesso(false)
                .mensagem(mensagem)
                .status(StatusProcessamento.ERRO_DISPONIBILIDADE)
                .dataProcessamento(LocalDateTime.now())
                .observacoes(String.format("Disponível: %d cortesias", disponivel))
                .build();
    }

    // Métodos utilitários

    /**
     * Define o tempo de processamento
     */
    public CortesiaProcessamentoResponse comTempoProcessamento(long inicioMs) {
        this.tempoProcessamentoMs = System.currentTimeMillis() - inicioMs;
        return this;
    }

    /**
     * Define informações do arquivo gerado
     */
    public CortesiaProcessamentoResponse comInformacoesArquivo(String formato, long tamanhoBytes, int numeroPaginas) {
        this.formato = formato;
        this.tamanhoArquivoBytes = tamanhoBytes;
        this.numeroPaginas = numeroPaginas;
        return this;
    }

    /**
     * Adiciona observações
     */
    public CortesiaProcessamentoResponse comObservacoes(String observacoes) {
        this.observacoes = observacoes;
        return this;
    }

    /**
     * Verifica se é uma resposta de sucesso
     */
    public boolean isSucesso() {
        return sucesso;
    }

    /**
     * Verifica se é uma resposta de erro
     */
    public boolean isErro() {
        return !sucesso;
    }

    /**
     * Obtém resumo do processamento
     */
    public String getResumo() {
        if (sucesso) {
            return String.format("✅ Sucesso: %d cortesias geradas para %s (%s)", 
                               quantidade, email, praca);
        } else {
            return String.format("❌ Erro: %s", mensagem);
        }
    }

    /**
     * Obtém informações detalhadas para log
     */
    public String getDetalhesLog() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("RequestId: %s", requestId));
        sb.append(String.format(", Email: %s", email));
        sb.append(String.format(", Quantidade: %d", quantidade));
        sb.append(String.format(", Praça: %s", praca));
        sb.append(String.format(", Status: %s", status));
        sb.append(String.format(", Sucesso: %s", sucesso));
        
        if (tempoProcessamentoMs != null) {
            sb.append(String.format(", Tempo: %dms", tempoProcessamentoMs));
        }
        
        if (tamanhoArquivoBytes != null) {
            sb.append(String.format(", Tamanho: %d bytes", tamanhoArquivoBytes));
        }
        
        if (numeroPaginas != null) {
            sb.append(String.format(", Páginas: %d", numeroPaginas));
        }
        
        return sb.toString();
    }

    @Override
    public String toString() {
        return String.format("CortesiaProcessamentoResponse{requestId='%s', email='%s', sucesso=%s, status=%s}", 
                           requestId, email, sucesso, status);
    }
}
