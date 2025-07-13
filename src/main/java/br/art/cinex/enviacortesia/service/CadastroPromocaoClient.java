package br.art.cinex.enviacortesia.service;



import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.primefaces.model.FilterMeta;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import br.art.cinex.enviacortesia.domain.dto.CadastropromocaoDTO;
 
@Component
public class CadastroPromocaoClient extends ApiService {

    private static final String ENDPOINT = "/cadastropromocaos";

    /** devolve *todos* os pendentes até o tamanho solicitado (ex.: 1000) */
    public List<CadastropromocaoDTO> buscarNaoEnviados(int size) {

        Map<String, FilterMeta> filtros = Map.of(
            "cupomEnviado.equals",
            FilterMeta.builder()
                      .field("cupomEnviado.equals")
                      .filterValue(false)
                      .build()
        );

        return fetchEntities(
                ENDPOINT,
                0,                       // sempre página 0
                size,                    // tamanho desejado
                null,                    // sort
                filtros,
                new ParameterizedTypeReference<List<CadastropromocaoDTO>>() {});
    }

    /** PUT cupomEnviado = true */
    public CadastropromocaoDTO marcarEnviado(CadastropromocaoDTO dto) throws Exception {
        dto.setCupomEnviado(true);
        return updateEntity(ENDPOINT, dto.getId(), dto, CadastropromocaoDTO.class);
    }
}
