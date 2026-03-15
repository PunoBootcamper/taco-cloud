package com.example.tacocloud.client;

import com.example.tacocloud.domain.Ingredient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Cliente REST para consumir la API de TacoCloud
 * 
 * Demuestra diferentes formas de usar RestTemplate:
 * - getForObject: Obtener objeto directamente
 * - getForEntity: Obtener ResponseEntity con headers
 * - postForObject: POST y retornar objeto
 * - postForLocation: POST y retornar URI del recurso creado
 * - postForEntity: POST y retornar ResponseEntity
 * - put: Actualizar recurso
 * - delete: Eliminar recurso
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TacoCloudClient {
    
    private final RestTemplate rest;
    
    // ====================================================================
    // GET - Obtener recursos
    // ====================================================================
    
    /**
     * Forma 1: getForObject con parámetro variable
     * 
     * URL: http://localhost:8080/ingredients/{id}
     * Parámetros: ingredientId se pasa como argumento variable
     */
    public Ingredient getIngredientById_Variant1(String ingredientId) {
        return rest.getForObject("http://localhost:8080/ingredients/{id}",
                                 Ingredient.class, ingredientId);
    }
    
    /**
     * Forma 2: getForObject con Map de variables
     * 
     * URL: http://localhost:8080/ingredients/{id}
     * Parámetros: ingredientId se pasa en un Map
     */
    public Ingredient getIngredientById_Variant2(String ingredientId) {
        Map<String, String> urlVariables = new HashMap<>();
        urlVariables.put("id", ingredientId);
        return rest.getForObject("http://localhost:8080/ingredients/{id}",
                                 Ingredient.class, urlVariables);
    }
    
    /**
     * Forma 3: getForObject con URI construida manualmente
     * 
     * Usa UriComponentsBuilder para construir la URI completa
     */
    public Ingredient getIngredientById_Variant3(String ingredientId) {
        Map<String, String> urlVariables = new HashMap<>();
        urlVariables.put("id", ingredientId);
        URI url = UriComponentsBuilder
                    .fromHttpUrl("http://localhost:8080/ingredients/{id}")
                    .build(urlVariables);
        return rest.getForObject(url, Ingredient.class);
    }
    
    /**
     * Forma 4: getForEntity - Obtiene ResponseEntity con headers
     * 
     * Útil cuando necesitas acceder a headers HTTP (fecha, etags, etc.)
     */
    public Ingredient getIngredientById_WithHeaders(String ingredientId) {
        ResponseEntity<Ingredient> responseEntity =
            rest.getForEntity("http://localhost:8080/ingredients/{id}",
                              Ingredient.class, ingredientId);
        
        log.info("Fetched time: {}", responseEntity.getHeaders().getDate());
        log.info("Status code: {}", responseEntity.getStatusCode());
        log.info("Content-Type: {}", responseEntity.getHeaders().getContentType());
        
        return responseEntity.getBody();
    }
    
    // ====================================================================
    // POST - Crear recursos
    // ====================================================================
    
    /**
     * Forma 1: postForObject - POST y retorna el objeto creado
     * 
     * Envía el ingrediente al servidor y retorna el ingrediente creado
     */
    public Ingredient createIngredient_ReturnObject(Ingredient ingredient) {
        return rest.postForObject("http://localhost:8080/ingredients",
                                  ingredient, Ingredient.class);
    }
    
    /**
     * Forma 2: postForLocation - POST y retorna la URI del recurso creado
     * 
     * Útil cuando solo necesitas la ubicación del recurso creado
     * Retorna el valor del header "Location"
     */
    public URI createIngredient_ReturnLocation(Ingredient ingredient) {
        return rest.postForLocation("http://localhost:8080/ingredients",
                                    ingredient);
    }
    
    /**
     * Forma 3: postForEntity - POST y retorna ResponseEntity completo
     * 
     * Acceso a headers, status code y body
     */
    public Ingredient createIngredient_ReturnEntity(Ingredient ingredient) {
        ResponseEntity<Ingredient> responseEntity =
            rest.postForEntity("http://localhost:8080/ingredients",
                               ingredient,
                               Ingredient.class);
        
        log.info("New resource created at {}", 
                 responseEntity.getHeaders().getLocation());
        log.info("Status code: {}", responseEntity.getStatusCode());
        
        return responseEntity.getBody();
    }
    
    // ====================================================================
    // PUT - Actualizar recursos
    // ====================================================================
    
    /**
     * put - Actualizar un ingrediente existente
     * 
     * PUT no retorna valor (void)
     * El servidor debe retornar 204 No Content
     */
    public void updateIngredient(Ingredient ingredient) {
        rest.put("http://localhost:8080/ingredients/{id}",
                 ingredient, ingredient.getId());
        
        log.info("Ingredient {} updated successfully", ingredient.getId());
    }
    
    // ====================================================================
    // DELETE - Eliminar recursos
    // ====================================================================
    
    /**
     * delete - Eliminar un ingrediente
     * 
     * DELETE no retorna valor (void)
     * El servidor debe retornar 204 No Content
     */
    public void deleteIngredient(Ingredient ingredient) {
        rest.delete("http://localhost:8080/ingredients/{id}",
                    ingredient.getId());
        
        log.info("Ingredient {} deleted successfully", ingredient.getId());
    }
    
    /**
     * delete por ID - Forma alternativa
     */
    public void deleteIngredientById(String ingredientId) {
        rest.delete("http://localhost:8080/ingredients/{id}", ingredientId);
        log.info("Ingredient {} deleted successfully", ingredientId);
    }
    
    // ====================================================================
    // Métodos de conveniencia (recomendados para uso real)
    // ====================================================================
    
    /**
     * Método recomendado para obtener ingrediente por ID
     */
    public Ingredient getIngredientById(String ingredientId) {
        return getIngredientById_Variant1(ingredientId);
    }
    
    /**
     * Método recomendado para crear ingrediente
     */
    public Ingredient createIngredient(Ingredient ingredient) {
        return createIngredient_ReturnObject(ingredient);
    }
}
