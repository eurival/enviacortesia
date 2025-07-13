package br.art.cinex.enviacortesia.domain.dto;

import java.io.Serializable;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * Payload enviado para o tópico <cortesia-para-emitir>.
 */
@Data
@Builder                // facilita criação via builder()
@NoArgsConstructor      // necessário para Jackson desserializar
@AllArgsConstructor     // necessário para o builder funcionar
public class CortesiaGeracaoRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Praça é obrigatória")
    private String praca;

    @NotNull(message = "Quantidade é obrigatória")
    @Min(value = 1, message = "Quantidade deve ser maior que zero")
    private Integer quantidade;

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email deve ter formato válido")
    private String email;

    @NotBlank(message = "Solicitante é obrigatório")
    private String quemSolicitou;

    @NotBlank(message = "Destinação é obrigatória")
    private String destinacao;

    @NotNull(message = "Validade de impressão é obrigatória")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") // garante ISO no JSON
    private LocalDate validadeImpressao;

    /** Formato do arquivo final gerado (pdf ou zip). */
    @Builder.Default        // para vir preenchido mesmo usando builder()
    private String formato = "zip";
}
