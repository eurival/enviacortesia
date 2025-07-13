package br.art.cinex.enviacortesia.domain.dto;


import org.primefaces.model.StreamedContent;

import lombok.Data;

@Data
public class CortesiaGeracaoResponse {
    private boolean sucesso;
    private String mensagem;
    private StreamedContent arquivo;
    private TipoMensagem tipoMensagem;

    public enum TipoMensagem {
        SUCESSO, ERRO, AVISO
    }

    public static CortesiaGeracaoResponse sucesso(String mensagem, StreamedContent arquivo) {
        CortesiaGeracaoResponse response = new CortesiaGeracaoResponse();
        response.setSucesso(true);
        response.setMensagem(mensagem);
        response.setArquivo(arquivo);
        response.setTipoMensagem(TipoMensagem.SUCESSO);
        return response;
    }

    public static CortesiaGeracaoResponse erro(String mensagem) {
        CortesiaGeracaoResponse response = new CortesiaGeracaoResponse();
        response.setSucesso(false);
        response.setMensagem(mensagem);
        response.setTipoMensagem(TipoMensagem.ERRO);
        return response;
    }

    public static CortesiaGeracaoResponse aviso(String mensagem) {
        CortesiaGeracaoResponse response = new CortesiaGeracaoResponse();
        response.setSucesso(false);
        response.setMensagem(mensagem);
        response.setTipoMensagem(TipoMensagem.AVISO);
        return response;
    }
}