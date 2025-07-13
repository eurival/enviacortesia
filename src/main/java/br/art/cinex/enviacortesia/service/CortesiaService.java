package br.art.cinex.enviacortesia.service;



import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
 
import org.primefaces.model.FilterMeta;
import org.primefaces.model.SortMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
 
import org.springframework.core.ParameterizedTypeReference;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.art.cinex.enviacortesia.domain.Cortesia;
 
 

@Service
@Transactional
public class CortesiaService extends ApiService {

    private static final Logger logger = LoggerFactory.getLogger(CortesiaService.class);
    private static final String ENDPOINT = "/cortesias";


    /**
     * Busca cortesias disponíveis para emissão
     * Equivalente ao método buscaTodasCortesias do código original
     */

    public List<Cortesia> buscarCortesiasDisponiveis(String praca, Integer quantidade) {
        Map<String, Object> param = new LinkedHashMap<>();
        param.put("status.contains", "Vendido");
        
        if (!praca.equals("Todas")) {
            param.put("praca.contains", praca);
        }

        return buscaTodasCortesias(param, 0, quantidade);
    }
    /**
     * Busca todas as cortesias com filtros (similar ao método original)
     */
    public List<Cortesia> buscaTodasCortesias(Map<String, Object> queryParams, int page, int pageSize) {
        Map<String, SortMeta> sortBy = new HashMap<>(); 
        Map<String, FilterMeta> filterBy = new HashMap<String, FilterMeta>();
        
        queryParams.forEach((filter, value) -> {
            FilterMeta filterMeta = FilterMeta.builder()
                .field(filter)
                .filterValue(value)
                .build();
            filterBy.put(filter, filterMeta);
        });

        // Exemplo de configuração de ordenação (adapte conforme necessário)
        sortBy.put("id", SortMeta.builder()
            .field("id")
            .order(org.primefaces.model.SortOrder.ASCENDING)
            .build());
            return fetchEntities(
                ENDPOINT,
                page,
                pageSize,
                sortBy,
                filterBy,
                new ParameterizedTypeReference<List<Cortesia>>() {}
            );
    }

    /**
     * Salva ou atualiza uma cortesia
     */
    public Cortesia save(Cortesia cortesia) throws Exception{
        try {
            return createEntity(ENDPOINT, cortesia, Cortesia.class);
        } catch (Exception e) {
            // Lida com exceções específicas e lança exceções personalizadas se necessário
            throw  new IllegalStateException("Erro ao salvar a cortesia. "+ e.getMessage());
        }
    }

    /**
     * Atualiza uma cortesia (equivalente ao método atualizar do código original)
     */
    public Cortesia atualizar(Cortesia cortesia) throws Exception {
        try {
            return updateEntity(ENDPOINT, cortesia.getId(), cortesia, Cortesia.class);
        } catch (Exception e) {
            throw new IllegalStateException("Erro ao salvar a cortesia."+ e.getMessage());
        }
    }

    /**
     * Busca cortesia por ID
     */
    public Cortesia findById(Long id) throws Exception {
        try {
            return findEntityById(ENDPOINT, id, Cortesia.class);
        } catch (Exception e) {
            // Lida com exceções específicas e lança exceções personalizadas se necessário
            throw new IllegalStateException("Erro ao buscar a cortesia por ID."+ e.getMessage());
        }
    }

    /**
     * Remove uma cortesia
     */
    public Boolean remover(Cortesia cortesia) {
        try {
            return deleteEntity(ENDPOINT, cortesia.getId());
        } catch (Exception e) {
            // Lida com exceções específicas e lança exceções personalizadas se necessário
            throw new IllegalStateException("Erro ao remover a cortesia."+ e.getMessage());
        }
    }

    /**
     * Busca cortesias por status
     */
 

    /**
     * Busca cortesias por praça
     */


    /**
     * Conta quantas cortesias estão disponíveis para uma praça
     */
 

    /**
     * Verifica se há cortesias suficientes disponíveis
     */
 

    /**
     * Atualiza o status de múltiplas cortesias
     */
 
}
