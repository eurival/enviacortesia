package br.art.cinex.enviacortesia.service;

 

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import org.primefaces.model.SortMeta;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.SortOrder;

@Component 
@RequiredArgsConstructor
public abstract class ApiService {
    protected final RestTemplate restTemplate;
    private static final Logger logger = LoggerFactory.getLogger(ApiService.class);
   

    @Value("${cinex.api.base-url}")
    private String baseUrl ; // Ajuste conforme necessário
    @Value("${cinex.api.auth-url}")
    private String AUTH_URL ;

   // private int port = 8080; // Ajuste conforme necessário

    public ApiService() {
        this.restTemplate = new RestTemplate();
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        try {
            String token = obterToken();
            if (token != null && !token.isEmpty()) {
                headers.set("Authorization", "Bearer " + token);
            } else {
                logger.warn("Token de autenticação não encontrado.");
            }
        } catch (NullPointerException e) {
            logger.warn("Usuário não autenticado ou sem token.");
        }
        headers.set("Content-Type", "application/json");
        return headers;
    }
    protected String obterToken()  {
        String url = AUTH_URL;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String json = "{\"username\":\"admin\",\"password\":\"admin\"}";
        HttpEntity<String> request = new HttpEntity<>(json, headers);
        // Send request with headers
        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            String authHeader = response.getHeaders().getFirst("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                return authHeader.substring(7);
            } else {
                throw new RuntimeException("Invalid or missing Authorization header");
            }
        } else {
            throw new RuntimeException("Failed to obtain token. Status code: " + response.getStatusCodeValue());
        }
    }

    private HttpEntity<?> createHttpEntity() {
        return new HttpEntity<>(createHeaders());
    }

    public <T> List<T> fetchEntities(
        String endpoint,
        int page,
        int pageSize,
        Map<String, SortMeta> sortBy,
        Map<String, FilterMeta> filterBy,
        ParameterizedTypeReference<List<T>> responseType
    ) {
        HttpEntity<?> httpEntity = createHttpEntity();

        // Construção da URL com paginação e filtros
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.newInstance()
               // .scheme(schema)
                .host(baseUrl)
             //   .port(port)
                .path( endpoint)
                .queryParam("page", page)
                .queryParam("size", pageSize);

        // Adicionando parâmetros de ordenação
        if (sortBy != null) {
            sortBy.forEach((key, sortMeta) -> {
                String sortField = sortMeta.getField();
                String sortOrder = sortMeta.getOrder() == SortOrder.ASCENDING ? "asc" : "desc";
                uriBuilder.queryParam("sort", sortField + "," + sortOrder);
            });
        }

        // Adicionando filtros
        if (filterBy != null) {
            filterBy.forEach((key, filterMeta) -> {
                Object filterValue = filterMeta.getFilterValue();
                if (filterValue != null) {
                    uriBuilder.queryParam(key, filterValue);
                }
            });
        }

        String url = uriBuilder.build().toUriString();
        try {
            ResponseEntity<List<T>> response = restTemplate.exchange(
                    url, HttpMethod.GET, httpEntity, responseType);
            return response.getBody();
        } catch (Exception e) {
            logger.error("Erro ao buscar entidades: {}", e.getMessage());
            return null;
        }
    }

    public int countEntities(String endpoint, Map<String, FilterMeta> filterBy) {
        HttpEntity<?> httpEntity = createHttpEntity();

        // Construção da URL para contagem
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.newInstance()
        //        .scheme(schema)
                .host(baseUrl)
        //        .port(port)
                .path( endpoint + "/count");

        // Adicionando filtros
        if (filterBy != null) {
            filterBy.forEach((key, filterMeta) -> {
                Object filterValue = filterMeta.getFilterValue();
                if (filterValue != null) {
                    uriBuilder.queryParam(key, filterValue);
                }
            });
        }

        String url = uriBuilder.build().toUriString();
        try {
            ResponseEntity<Integer> response = restTemplate.exchange(
                    url, HttpMethod.GET, httpEntity, Integer.class);
            return response.getBody();
        } catch (Exception e) {
            logger.error("Erro ao contar entidades: {}", e.getMessage());
            return 0;
        }
    }

    public <T> T createEntity(String endpoint, T entity, Class<T> responseType) {
        HttpEntity<T> httpEntity = new HttpEntity<>(entity, createHeaders());

        String url = UriComponentsBuilder.newInstance()
           //     .scheme(schema)
                .host(baseUrl)
          //      .port(port)
                .path( endpoint)
                .toUriString();

        try {
            ResponseEntity<T> response = restTemplate.exchange(
                    url, HttpMethod.POST, httpEntity, responseType);
            return response.getBody();
        } catch (Exception e) {
            logger.error("Erro ao criar entidade: {}", e.getMessage());
            return null;
        }
    }

    public <T> T updateEntity(String endpoint, Long entityId, T entity, Class<T> responseType) {
        HttpEntity<T> httpEntity = new HttpEntity<>(entity, createHeaders());

        String url = UriComponentsBuilder.newInstance()
           //     .scheme(schema)
                .host(baseUrl)
         //       .port(port)
                .path( endpoint + "/" + entityId)
                .toUriString();

        try {
            ResponseEntity<T> response = restTemplate.exchange(
                    url, HttpMethod.PUT, httpEntity, responseType);
            return response.getBody();
        } catch (Exception e) {
            logger.error("Erro ao atualizar entidade: {}", e.getMessage());
            return null;
        }
    }

    public boolean deleteEntity(String endpoint, Long entityId) {
        HttpEntity<?> httpEntity = createHttpEntity();

        String url = UriComponentsBuilder.newInstance()
           //     .scheme(schema)
                .host(baseUrl)
         //       .port(port)
                .path( endpoint + "/" + entityId)
                .toUriString();

        try {
            restTemplate.exchange(url, HttpMethod.DELETE, httpEntity, Void.class);
            return true;
        } catch (Exception e) {
            logger.error("Erro ao deletar entidade: {}", e.getMessage());
            return false;
        }
    }

    public <T> T findEntityById(String endpoint, Long entityId, Class<T> responseType) {
        HttpEntity<?> httpEntity = createHttpEntity();

        String url = UriComponentsBuilder.newInstance()
          //      .scheme(schema)
                .host(baseUrl)
           //     .port(port)
                .path(endpoint + "/" + entityId)
                .toUriString();

        try {
            ResponseEntity<T> response = restTemplate.exchange(
                    url, HttpMethod.GET, httpEntity, responseType);
            return response.getBody();
        } catch (Exception e) {
            logger.error("Erro ao buscar entidade por ID: {}", e.getMessage());
            return null;
        }
    }
}
