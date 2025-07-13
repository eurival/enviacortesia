package br.art.cinex.enviacortesia.domain;


import java.io.Serializable;
 
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

 

import lombok.Data;

@Data
public class Cortesia implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long sequencia;
    private String codigoBarras;
    private LocalDate validade;
    private String status;
    private String praca;
    private User solicitante;
    private LocalDate dataEmissao;
    private String emailEmissao;
    private String destinacao;
    private String quemsolicitou;
    private String validadeImpressao;
 
    public String getFormattedValidade() {
        if (validadeImpressao == null || validadeImpressao.isEmpty()) {
            return "";
        }
        try {
            LocalDate data = LocalDate.parse(validadeImpressao);
            DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            // Converte a string para LocalDate
          //  LocalDate date = LocalDate.parse(validadeImpressao, inputFormatter);
            // Formata a data no novo formato
            return data.format(outputFormatter);
        } catch (DateTimeParseException e) {
            // Se ocorrer um erro ao converter a data, pode retornar a string original ou
            // outra mensagem
            return validadeImpressao; // Ou retornar uma mensagem de erro, ex: "Data inválida"
        }
    }

    public String getFormattedDataEmissao() {
        if (dataEmissao == null) {
            return "";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return dataEmissao.format(formatter);
    }
}
