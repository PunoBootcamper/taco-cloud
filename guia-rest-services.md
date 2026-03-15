# Guía de REST Services con Spring

## 7. Creating REST services

Esta guía cubre los conceptos para crear y consumir servicios REST con Spring Boot, incluyendo controladores RESTful, Spring Data REST para auto-generación de endpoints, y RestTemplate para consumir APIs externas.

---

## 7.1 Writing RESTful controllers

### @RestController - Base de APIs REST

**@RestController vs @Controller**

```java
// Opción 1: @RestController (recomendado para APIs REST)
@RestController
@RequestMapping("/api/ingredients")
public class IngredientController {
    
    @GetMapping
    public Iterable<Ingredient> allIngredients() {
        return ingredientRepo.findAll();
        // Retorna JSON automáticamente
    }
}

// Opción 2: @Controller + @ResponseBody en cada método
@Controller
@RequestMapping("/api/ingredients")
public class IngredientController {
    
    @GetMapping
    public @ResponseBody Iterable<Ingredient> allIngredients() {
        return ingredientRepo.findAll();
    }
}
```

**¿Qué hace @RestController?**
- Equivale a `@Controller` + `@ResponseBody` en todos los métodos
- Serializa automáticamente objetos Java a JSON (vía Jackson)
- Los métodos NO retornan nombres de vistas, sino datos directamente

---

### 7.1.1 Retrieving data from the server (GET)

#### GET - Obtener Colección

**Ejemplo 1: Listar todos los recursos**
```java
package com.example.tacocloud.controllers.rest;

import com.example.tacocloud.domain.Taco;
import com.example.tacocloud.repository.TacoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping(path = "/api/tacos", produces = "application/json")
@CrossOrigin(origins = "http://localhost:8080")
@RequiredArgsConstructor
public class TacoController {
    
    private final TacoRepository tacoRepo;
    
    // GET /api/tacos
    @GetMapping
    public Iterable<Taco> allTacos() {
        log.info("Fetching all tacos");
        return tacoRepo.findAll();
    }
}
```

**Anotaciones clave:**
- `produces = "application/json"`: Define el Content-Type de la respuesta
- `@CrossOrigin`: Habilita CORS para llamadas desde otros dominios
- `@RequiredArgsConstructor`: Constructor con inyección de dependencias (Lombok)

**Ejemplo 2: GET con paginación**
```java
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@RestController
@RequestMapping(path = "/api/tacos", produces = "application/json")
@RequiredArgsConstructor
public class TacoController {
    
    private final TacoRepository tacoRepo;
    
    // GET /api/tacos?recent
    @GetMapping(params = "recent")
    public Iterable<Taco> recentTacos() {
        PageRequest page = PageRequest.of(
            0,                              // Página 0 (primera)
            12,                             // 12 elementos
            Sort.by("createdAt").descending() // Ordenar por fecha desc
        );
        return tacoRepo.findAll(page).getContent();
    }
}
```

**Notas sobre paginación:**
- `PageRequest.of(pageNumber, pageSize, sort)`
- Las páginas son 0-indexed (página 0 = primera)
- El repositorio debe extender `JpaRepository` (no `CrudRepository`)

**Ejemplo 3: GET con múltiples criterios**
```java
@RestController
@RequestMapping("/api/tacos")
@RequiredArgsConstructor
public class TacoController {
    
    private final TacoRepository tacoRepo;
    
    // GET /api/tacos?page=0&size=10&sort=name
    @GetMapping
    public Page<Taco> getTacos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sortBy) {
        
        PageRequest pageRequest = PageRequest.of(
            page, size, Sort.by(sortBy)
        );
        return tacoRepo.findAll(pageRequest);
    }
}
```

#### GET - Obtener Recurso Individual

**Ejemplo 1: GET por ID con Optional**
```java
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/tacos")
@RequiredArgsConstructor
public class TacoController {
    
    private final TacoRepository tacoRepo;
    
    // GET /api/tacos/5
    @GetMapping("/{id}")
    public ResponseEntity<Taco> tacoById(@PathVariable("id") Long id) {
        Optional<Taco> optTaco = tacoRepo.findById(id);
        
        if (optTaco.isPresent()) {
            return new ResponseEntity<>(optTaco.get(), HttpStatus.OK);
        }
        
        return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
    }
}
```

**Ejemplo 2: Usando Optional.map (forma funcional)**
```java
@GetMapping("/{id}")
public ResponseEntity<Taco> tacoById(@PathVariable("id") Long id) {
    Optional<Taco> optTaco = tacoRepo.findById(id);
    
    return optTaco
        .map(taco -> new ResponseEntity<>(taco, HttpStatus.OK))
        .orElseGet(() -> new ResponseEntity<>(null, HttpStatus.NOT_FOUND));
}
```

**Ejemplo 3: Usar @ResponseStatus en caso de no encontrado**
```java
import org.springframework.web.server.ResponseStatusException;

@GetMapping("/{id}")
public Taco tacoById(@PathVariable("id") Long id) {
    return tacoRepo.findById(id)
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND, "Taco not found"
        ));
}
```

**ResponseEntity - Control completo de la respuesta:**
- Status code HTTP (200 OK, 404 NOT_FOUND, etc.)
- Headers personalizados
- Cuerpo de la respuesta

---

### 7.1.2 Sending data to the server (POST)

#### POST - Crear Recursos

**Ejemplo 1: POST básico con @ResponseStatus**
```java
import org.springframework.web.bind.annotation.ResponseStatus;

@RestController
@RequestMapping("/api/tacos")
@RequiredArgsConstructor
public class TacoController {
    
    private final TacoRepository tacoRepo;
    
    // POST /api/tacos
    @PostMapping(consumes = "application/json")
    @ResponseStatus(HttpStatus.CREATED)  // 201 Created
    public Taco postTaco(@RequestBody Taco taco) {
        return tacoRepo.save(taco);
    }
}
```

**@RequestBody:**
- Deserializa el JSON del body a objeto Java
- Jackson convierte automáticamente

**Ejemplo 2: POST con ResponseEntity y Location header**
```java
import org.springframework.web.util.UriComponentsBuilder;
import java.net.URI;

@RestController
@RequestMapping("/api/tacos")
@RequiredArgsConstructor
public class TacoController {
    
    private final TacoRepository tacoRepo;
    
    @PostMapping(consumes = "application/json")
    public ResponseEntity<Taco> postTaco(@RequestBody Taco taco,
                                         UriComponentsBuilder ucb) {
        Taco saved = tacoRepo.save(taco);
        
        // Construir URI del recurso creado
        // Resultado: http://localhost:8080/api/tacos/42
        URI locationUri = ucb
            .path("/api/tacos/{id}")
            .buildAndExpand(saved.getId())
            .toUri();
        
        // Retornar 201 Created con Location header
        return ResponseEntity
            .created(locationUri)
            .body(saved);
    }
}
```

**UriComponentsBuilder:**
- Inyectado automáticamente por Spring
- Construye URIs del recurso creado
- Usado en header `Location` (indica ubicación del nuevo recurso)

**Ejemplo 3: POST con validación**
```java
import javax.validation.Valid;
import org.springframework.validation.Errors;

@RestController
@RequestMapping("/api/tacos")
@RequiredArgsConstructor
public class TacoController {
    
    private final TacoRepository tacoRepo;
    
    @PostMapping(consumes = "application/json")
    public ResponseEntity<?> postTaco(@Valid @RequestBody Taco taco,
                                      Errors errors,
                                      UriComponentsBuilder ucb) {
        
        // Validar errores
        if (errors.hasErrors()) {
            return ResponseEntity
                .badRequest()
                .body(errors.getAllErrors());
        }
        
        Taco saved = tacoRepo.save(taco);
        
        URI locationUri = ucb
            .path("/api/tacos/{id}")
            .buildAndExpand(saved.getId())
            .toUri();
        
        return ResponseEntity.created(locationUri).body(saved);
    }
}
```

**Ejemplo 4: POST de entidad relacionada**
```java
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderApiController {
    
    private final OrderRepository orderRepo;
    
    // POST /api/orders
    @PostMapping(consumes = "application/json")
    public ResponseEntity<TacoOrder> postOrder(@RequestBody TacoOrder order,
                                               UriComponentsBuilder ucb) {
        // Asignar fecha de creación
        order.setPlacedAt(new Date());
        
        TacoOrder saved = orderRepo.save(order);
        
        URI locationUri = ucb
            .path("/api/orders/{id}")
            .buildAndExpand(saved.getId())
            .toUri();
        
        return ResponseEntity.created(locationUri).body(saved);
    }
}
```

---

### 7.1.3 Updating data on the server (PUT/PATCH)

#### PUT - Reemplazar Recurso Completo

**Ejemplo 1: PUT básico**
```java
@RestController
@RequestMapping("/api/tacos")
@RequiredArgsConstructor
public class TacoController {
    
    private final TacoRepository tacoRepo;
    
    // PUT /api/tacos/5
    @PutMapping(path = "/{id}", consumes = "application/json")
    public Taco putTaco(@PathVariable("id") Long id,
                        @RequestBody Taco taco) {
        taco.setId(id);  // Asegurar que el ID coincida con la URL
        return tacoRepo.save(taco);
    }
}
```

**Ejemplo 2: PUT con validación de existencia**
```java
@PutMapping(path = "/{id}", consumes = "application/json")
public ResponseEntity<Taco> putTaco(@PathVariable("id") Long id,
                                    @RequestBody Taco taco) {
    
    // Verificar que el taco existe
    if (!tacoRepo.existsById(id)) {
        return ResponseEntity.notFound().build();
    }
    
    taco.setId(id);
    Taco updated = tacoRepo.save(taco);
    
    return ResponseEntity.ok(updated);
}
```

#### PATCH - Actualización Parcial

**Ejemplo 1: PATCH de Order (actualización parcial)**
```java
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderApiController {
    
    private final OrderRepository orderRepo;
    
    // PATCH /api/orders/5
    @PatchMapping(path = "/{orderId}", consumes = "application/json")
    public TacoOrder patchOrder(@PathVariable("orderId") Long orderId,
                                @RequestBody TacoOrder patch) {
        
        TacoOrder order = orderRepo.findById(orderId).get();
        
        // Actualizar solo campos no nulos
        if (patch.getDeliveryName() != null) {
            order.setDeliveryName(patch.getDeliveryName());
        }
        if (patch.getDeliveryStreet() != null) {
            order.setDeliveryStreet(patch.getDeliveryStreet());
        }
        if (patch.getDeliveryCity() != null) {
            order.setDeliveryCity(patch.getDeliveryCity());
        }
        if (patch.getDeliveryState() != null) {
            order.setDeliveryState(patch.getDeliveryState());
        }
        if (patch.getDeliveryZip() != null) {
            order.setDeliveryZip(patch.getDeliveryZip());
        }
        if (patch.getCcNumber() != null) {
            order.setCcNumber(patch.getCcNumber());
        }
        if (patch.getCcExpiration() != null) {
            order.setCcExpiration(patch.getCcExpiration());
        }
        if (patch.getCcCVV() != null) {
            order.setCcCVV(patch.getCcCVV());
        }
        
        return orderRepo.save(order);
    }
}
```

**PUT vs PATCH:**

| Aspecto | PUT | PATCH |
|---------|-----|-------|
| Propósito | Reemplazar recurso completo | Actualizar campos específicos |
| Campos no enviados | Se setean a null o default | Se mantienen sin cambios |
| Idempotencia | Sí | Sí (generalmente) |
| Uso típico | Actualización completa | Actualización parcial |

**Ejemplo 2: PATCH con Optional y manejo de errores**
```java
@PatchMapping(path = "/{orderId}", consumes = "application/json")
public ResponseEntity<TacoOrder> patchOrder(
        @PathVariable("orderId") Long orderId,
        @RequestBody TacoOrder patch) {
    
    Optional<TacoOrder> optOrder = orderRepo.findById(orderId);
    
    if (optOrder.isEmpty()) {
        return ResponseEntity.notFound().build();
    }
    
    TacoOrder order = optOrder.get();
    
    // Actualizar campos no nulos
    if (patch.getDeliveryName() != null) {
        order.setDeliveryName(patch.getDeliveryName());
    }
    if (patch.getDeliveryStreet() != null) {
        order.setDeliveryStreet(patch.getDeliveryStreet());
    }
    // ... más campos
    
    TacoOrder updated = orderRepo.save(order);
    return ResponseEntity.ok(updated);
}
```

---

### 7.1.4 Deleting data from the server (DELETE)

**Ejemplo 1: DELETE básico**
```java
@RestController
@RequestMapping("/api/tacos")
@RequiredArgsConstructor
public class TacoController {
    
    private final TacoRepository tacoRepo;
    
    // DELETE /api/tacos/5
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)  // 204 No Content
    public void deleteTaco(@PathVariable("id") Long id) {
        tacoRepo.deleteById(id);
    }
}
```

**HTTP 204 No Content:**
- Indica operación exitosa
- Sin cuerpo en la respuesta (vacío)

**Ejemplo 2: DELETE con validación de existencia**
```java
@DeleteMapping("/{id}")
public ResponseEntity<Void> deleteTaco(@PathVariable("id") Long id) {
    
    if (!tacoRepo.existsById(id)) {
        return ResponseEntity.notFound().build();
    }
    
    tacoRepo.deleteById(id);
    return ResponseEntity.noContent().build();  // 204
}
```

**Ejemplo 3: DELETE con soft delete**
```java
@RestController
@RequestMapping("/api/tacos")
@RequiredArgsConstructor
public class TacoController {
    
    private final TacoRepository tacoRepo;
    
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTaco(@PathVariable("id") Long id) {
        Taco taco = tacoRepo.findById(id).orElseThrow();
        
        // Soft delete: marcar como eliminado, no borrar físicamente
        taco.setDeleted(true);
        taco.setDeletedAt(new Date());
        tacoRepo.save(taco);
    }
}
```

---

### Controlador REST Completo - Ejemplo Integrado

```java
package com.example.tacocloud.controllers.rest;

import com.example.tacocloud.domain.Taco;
import com.example.tacocloud.repository.TacoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Optional;

/**
 * REST Controller para el recurso Taco
 * 
 * Endpoints:
 * - GET /api/tacos - Lista todos los tacos
 * - GET /api/tacos?recent - Lista los 12 más recientes
 * - GET /api/tacos/{id} - Obtiene un taco específico
 * - POST /api/tacos - Crea un nuevo taco
 * - PUT /api/tacos/{id} - Actualiza un taco completo
 * - PATCH /api/tacos/{id} - Actualiza parcialmente un taco
 * - DELETE /api/tacos/{id} - Elimina un taco
 */
@Slf4j
@RestController
@RequestMapping(path = "/api/tacos", produces = "application/json")
@CrossOrigin(origins = "http://localhost:8080")
@RequiredArgsConstructor
public class TacoController {
    
    private final TacoRepository tacoRepo;
    
    // GET /api/tacos
    @GetMapping
    public Iterable<Taco> allTacos() {
        log.info("Fetching all tacos");
        return tacoRepo.findAll();
    }
    
    // GET /api/tacos?recent
    @GetMapping(params = "recent")
    public Iterable<Taco> recentTacos() {
        log.info("Fetching recent tacos");
        PageRequest page = PageRequest.of(
            0, 12, Sort.by("createdAt").descending()
        );
        return tacoRepo.findAll(page).getContent();
    }
    
    // GET /api/tacos/{id}
    @GetMapping("/{id}")
    public ResponseEntity<Taco> tacoById(@PathVariable("id") Long id) {
        log.info("Fetching taco with id {}", id);
        Optional<Taco> optTaco = tacoRepo.findById(id);
        
        return optTaco
            .map(taco -> new ResponseEntity<>(taco, HttpStatus.OK))
            .orElseGet(() -> new ResponseEntity<>(null, HttpStatus.NOT_FOUND));
    }
    
    // POST /api/tacos
    @PostMapping(consumes = "application/json")
    public ResponseEntity<Taco> postTaco(@RequestBody Taco taco,
                                         UriComponentsBuilder ucb) {
        log.info("Creating new taco: {}", taco.getName());
        Taco saved = tacoRepo.save(taco);
        
        URI locationUri = ucb
            .path("/api/tacos/{id}")
            .buildAndExpand(saved.getId())
            .toUri();
        
        log.info("Taco created with id {}", saved.getId());
        return ResponseEntity.created(locationUri).body(saved);
    }
    
    // PUT /api/tacos/{id}
    @PutMapping(path = "/{id}", consumes = "application/json")
    public ResponseEntity<Taco> putTaco(@PathVariable("id") Long id,
                                        @RequestBody Taco taco) {
        log.info("Updating taco with id {}", id);
        
        if (!tacoRepo.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        
        taco.setId(id);
        Taco updated = tacoRepo.save(taco);
        return ResponseEntity.ok(updated);
    }
    
    // PATCH /api/tacos/{id}
    @PatchMapping(path = "/{id}", consumes = "application/json")
    public ResponseEntity<Taco> patchTaco(@PathVariable("id") Long id,
                                          @RequestBody Taco patch) {
        log.info("Patching taco with id {}", id);
        
        Optional<Taco> optTaco = tacoRepo.findById(id);
        if (optTaco.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Taco taco = optTaco.get();
        
        if (patch.getName() != null) {
            taco.setName(patch.getName());
        }
        if (patch.getIngredients() != null) {
            taco.setIngredients(patch.getIngredients());
        }
        
        Taco updated = tacoRepo.save(taco);
        return ResponseEntity.ok(updated);
    }
    
    // DELETE /api/tacos/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTaco(@PathVariable("id") Long id) {
        log.info("Deleting taco with id {}", id);
        
        if (!tacoRepo.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        
        tacoRepo.deleteById(id);
        return ResponseEntity.noContent().build();  // 204
    }
}
```

---

## 7.2 Enabling data-backed services (Spring Data REST)

### ¿Qué es Spring Data REST?

**Spring Data REST** auto-genera endpoints REST basados en repositorios JPA, eliminando la necesidad de escribir controllers manualmente.

**Características:**
- ✅ Auto-generación de endpoints CRUD
- ✅ HATEOAS (Hypermedia As The Engine Of Application State)
- ✅ Paginación y ordenamiento automáticos
- ✅ Búsquedas personalizadas
- ✅ Cero código (o mínimo)

### Agregar Spring Data REST

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-rest</artifactId>
</dependency>
```

**Sin ningún código adicional**, Spring Data REST crea automáticamente endpoints para todos los repositorios:

```java
// Esto es todo lo que necesitas
public interface TacoRepository extends JpaRepository<Taco, Long> {
}
```

**Endpoints generados automáticamente:**

| Método | URL | Descripción |
|--------|-----|-------------|
| GET | `/tacos` | Lista de tacos (paginados) |
| GET | `/tacos/{id}` | Taco específico |
| POST | `/tacos` | Crear taco |
| PUT | `/tacos/{id}` | Re emplazar taco |
| PATCH | `/tacos/{id}` | Actualizar taco parcialmente |
| DELETE | `/tacos/{id}` | Eliminar taco |

### Respuesta HATEOAS

**GET /tacos**
```json
{
  "_embedded": {
    "tacos": [
      {
        "name": "Carnitas Taco",
        "createdAt": "2021-08-10T12:30:00.000+00:00",
        "_links": {
          "self": {
            "href": "http://localhost:8080/tacos/1"
          },
          "taco": {
            "href": "http://localhost:8080/tacos/1"
          },
          "ingredients": {
            "href": "http://localhost:8080/tacos/1/ingredients"
          }
        }
      },
      {
        "name": "Veggie Taco",
        "createdAt": "2021-08-10T13:00:00.000+00:00",
        "_links": {
          "self": {
            "href": "http://localhost:8080/tacos/2"
          },
          "taco": {
            "href": "http://localhost:8080/tacos/2"
          },
          "ingredients": {
            "href": "http://localhost:8080/tacos/2/ingredients"
          }
        }
      }
    ]
  },
  "_links": {
    "self": {
      "href": "http://localhost:8080/tacos{?page,size,sort}",
      "templated": true
    },
    "profile": {
      "href": "http://localhost:8080/profile/tacos"
    },
    "search": {
      "href": "http://localhost:8080/tacos/search"
    }
  },
  "page": {
    "size": 20,
    "totalElements": 45,
    "totalPages": 3,
    "number": 0
  }
}
```

**Ventajas de HATEOAS:**
- Clientes descubren endpoints disponibles
- Enlaces a recursos relacionados
- Documentación auto-generada
- Evolución de API sin romper clientes

---

### 7.2.1 Adjusting resource paths and relation names

#### Cambiar Ruta Base

**application.properties:**
```properties
# Configurar ruta base para todos los endpoints
spring.data.rest.base-path=/api
```

Ahora todos los endpoints estarán bajo `/api`:
- `/api/tacos`
- `/api/ingredients`
- `/api/orders`

#### Personalizar Recursos con @RestResource

**En la entidad:**
```java
package com.example.tacocloud.domain;

import lombok.Data;
import org.springframework.data.rest.core.annotation.RestResource;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

@Data
@Entity
@RestResource(rel = "tacos", path = "tacos")
public class Taco {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    
    private String name;
    
    @ManyToMany(targetEntity = Ingredient.class)
    private List<Ingredient> ingredients;
    
    private Date createdAt;
}
```

**@RestResource parámetros:**
- `rel`: Nombre de la relación en `_links` (nombre lógico)
- `path`: Parte de la URL (ruta física)

#### Personalizar Repositorio con @RepositoryRestResource

```java
package com.example.tacocloud.repository;

import com.example.tacocloud.domain.Taco;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(
    collectionResourceRel = "tacos",  // Nombre en _embedded y _links
    path = "tacos"                    // URL path
)
public interface TacoRepository extends JpaRepository<Taco, Long> {
}
```

**Ejemplo: Personalizar nombre de recurso**
```java
@RepositoryRestResource(
    collectionResourceRel = "design",  // /api/design en lugar de /api/tacos
    path = "design"
)
public interface TacoRepository extends JpaRepository<Taco, Long> {
}
```

**Ejemplo: Excluir repositorio de Spring Data REST**
```java
@RepositoryRestResource(exported = false)
public interface InternalRepository extends JpaRepository<InternalData, Long> {
    // Este repositorio NO tendrá endpoints REST
}
```

---

### 7.2.2 Paging and sorting

#### Paginación Automática

Spring Data REST pagina automáticamente todas las colecciones:

```
GET /api/tacos
```

Respuesta incluye metadatos de paginación:
```json
{
  "page": {
    "size": 20,
    "totalElements": 100,
    "totalPages": 5,
    "number": 0
  }
}
```

**Parámetros de paginación:**
```
GET /api/tacos?page=0&size=5
GET /api/tacos?page=1&size=10
GET /api/tacos?page=2&size=20
```

| Parámetro | Descripción | Default |
|-----------|-------------|---------|
| `page` | Número de página (0-indexed) | 0 |
| `size` | Elementos por página | 20 |
| `sort` | Campo y dirección de ordenamiento | - |

#### Ordenamiento (Sorting)

**Ordenar por un campo:**
```
GET /api/tacos?sort=name,asc
GET /api/tacos?sort=createdAt,desc
```

**Ordenar por múltiples campos:**
```
GET /api/tacos?sort=name,asc&sort=createdAt,desc
```

**Combinar paginación y ordenamiento:**
```
GET /api/tacos?page=0&size=10&sort=name,asc
GET /api/tacos?page=1&size=5&sort=createdAt,desc
```

#### Configurar Valores por Defecto

**application.properties:**
```properties
# Configurar paginación
spring.data.rest.default-page-size=10
spring.data.rest.max-page-size=100
```

**O en código (más control):**
```java
package com.example.tacocloud.config;

import com.example.tacocloud.domain.Ingredient;
import com.example.tacocloud.domain.Taco;
import com.example.tacocloud.domain.TacoOrder;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

@Configuration
public class DataRestConfig implements RepositoryRestConfigurer {
    
    @Override
    public void configureRepositoryRestConfiguration(
            RepositoryRestConfiguration config, CorsRegistry cors) {
        
        // Configurar paginación
        config.setDefaultPageSize(10);
        config.setMaxPageSize(100);
        
        // Exponer IDs en JSON (por defecto están ocultos)
        config.exposeIdsFor(Taco.class);
        config.exposeIdsFor(Ingredient.class);
        config.exposeIdsFor(TacoOrder.class);
        
        // Configurar CORS
        cors.addMapping("/**")
            .allowedOrigins("http://localhost:8080")
            .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE")
            .allowedHeaders("*");
    }
}
```

#### Métodos de Búsqueda Personalizados

```java
package com.example.tacocloud.repository;

import com.example.tacocloud.domain.Taco;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.Date;
import java.util.List;

@RepositoryRestResource(collectionResourceRel = "tacos", path = "tacos")
public interface TacoRepository extends JpaRepository<Taco, Long> {
    
    // Genera: GET /tacos/search/findByNameContaining?name=Carne
    List<Taco> findByNameContaining(@Param("name") String name);
    
    // Genera: GET /tacos/search/findByCreatedAtBetween?start=...&end=...
    List<Taco> findByCreatedAtBetween(
        @Param("start") Date startDate,
        @Param("end") Date endDate
    );
    
    // Con paginación
    Page<Taco> findByNameContaining(
        @Param("name") String name,
        Pageable pageable
    );
}
```

**Endpoint de búsqueda:**
```
GET /api/tacos/search
```

Respuesta:
```json
{
  "_links": {
    "findByNameContaining": {
      "href": "http://localhost:8080/api/tacos/search/findByNameContaining{?name,page,size,sort}",
      "templated": true
    },
    "findByCreatedAtBetween": {
      "href": "http://localhost:8080/api/tacos/search/findByCreatedAtBetween{?start,end}",
      "templated": true
    },
    "self": {
      "href": "http://localhost:8080/api/tacos/search"
    }
  }
}
```

**Uso:**
```
GET /api/tacos/search/findByNameContaining?name=Carnitas
GET /api/tacos/search/findByNameContaining?name=Taco&page=0&size=5
GET /api/tacos/search/findByCreatedAtBetween?start=2021-08-01&end=2021-08-31
```

---

## 7.3 Consuming REST services (RestTemplate)

### Configurar RestTemplate

**Paso 1: Crear @Bean de RestTemplate**
```java
package com.example.tacocloud.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestClientConfig {
    
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
```

**RestTemplate:**
- Cliente HTTP síncrono de Spring
- Simplifica llamadas REST
- Manejo automático de serialización/deserialización JSON
- Múltiples métodos para GET, POST, PUT, PATCH, DELETE

---

### 7.3.1 GETting resources

#### getForObject - Obtener Objeto Directamente

**Ejemplo 1: Parámetros variables (varargs)**
```java
package com.example.tacocloud.client;

import com.example.tacocloud.domain.Ingredient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class TacoCloudClient {
    
    private final RestTemplate rest;
    
    public Ingredient getIngredientById(String ingredientId) {
        return rest.getForObject(
            "http://localhost:8080/ingredients/{id}",
            Ingredient.class,
            ingredientId  // Reemplaza {id} en la URL
        );
    }
}
```

**Ejemplo 2: Map de parámetros**
```java
import java.util.HashMap;
import java.util.Map;

public Ingredient getIngredientById_Map(String ingredientId) {
    Map<String, String> urlVariables = new HashMap<>();
    urlVariables.put("id", ingredientId);
    
    return rest.getForObject(
        "http://localhost:8080/ingredients/{id}",
        Ingredient.class,
        urlVariables  // Map con variables de URL
    );
}
```

**Ejemplo 3: URI construida con UriComponentsBuilder**
```java
import org.springframework.web.util.UriComponentsBuilder;
import java.net.URI;

public Ingredient getIngredientById_URI(String ingredientId) {
    Map<String, String> urlVariables = new HashMap<>();
    urlVariables.put("id", ingredientId);
    
    URI url = UriComponentsBuilder
        .fromHttpUrl("http://localhost:8080/ingredients/{id}")
        .build(urlVariables);
    
    return rest.getForObject(url, Ingredient.class);
}
```

#### getForEntity - Obtener ResponseEntity (con headers)

```java
import org.springframework.http.ResponseEntity;

public Ingredient getIngredient_WithHeaders(String ingredientId) {
    ResponseEntity<Ingredient> responseEntity = rest.getForEntity(
        "http://localhost:8080/ingredients/{id}",
        Ingredient.class,
        ingredientId
    );
    
    // Acceso a headers HTTP
    log.info("Fetched time: {}", responseEntity.getHeaders().getDate());
    log.info("Status code: {}", responseEntity.getStatusCode());
    log.info("Content-Type: {}", responseEntity.getHeaders().getContentType());
    
    return responseEntity.getBody();
}
```

**getForObject vs getForEntity:**

| Aspecto | getForObject | getForEntity |
|---------|--------------|--------------|
| Retorna | Objeto directamente | ResponseEntity<T> |
| Acceso a headers | No | Sí |
| Acceso a status code | No | Sí |
| Uso típico | Solo necesitas el objeto | Necesitas metadata HTTP |

---

### 7.3.4 POSTing resource data

#### postForObject - Crear y Retornar Objeto

```java
public Ingredient createIngredient(Ingredient ingredient) {
    return rest.postForObject(
        "http://localhost:8080/ingredients",
        ingredient,       // Request body
        Ingredient.class  // Tipo de respuesta
    );
}
```

#### postForLocation - Crear y Retornar URI

```java
import java.net.URI;

public URI createIngredient_GetLocation(Ingredient ingredient) {
    return rest.postForLocation(
        "http://localhost:8080/ingredients",
        ingredient
    );
}

// Uso:
Ingredient newIngredient = new Ingredient("FLTO", "Flour Tortilla", Type.WRAP);
URI location = createIngredient_GetLocation(newIngredient);
// location = http://localhost:8080/ingredients/FLTO
```

**postForLocation:**
- Retorna el valor del header `Location`
- Útil cuando solo necesitas la URI del recurso creado

#### postForEntity - Crear y Retornar ResponseEntity

```java
public Ingredient createIngredient_FullResponse(Ingredient ingredient) {
    ResponseEntity<Ingredient> responseEntity = rest.postForEntity(
        "http://localhost:8080/ingredients",
        ingredient,
        Ingredient.class
    );
    
    log.info("New resource created at {}",
             responseEntity.getHeaders().getLocation());
    log.info("Status code: {}", responseEntity.getStatusCode());
    
    return responseEntity.getBody();
}
```

**Comparación POST:**

| Método | Retorna | Caso de uso |
|--------|---------|-------------|
| `postForObject()` | Objeto creado | Necesitas el objeto |
| `postForLocation()` | URI (Location header) | Solo necesitas la ubicación |
| `postForEntity()` | ResponseEntity completo | Necesitas objeto + headers + status |

---

### 7.3.2 PUTting resources

```java
public void updateIngredient(Ingredient ingredient) {
    rest.put(
        "http://localhost:8080/ingredients/{id}",
        ingredient,         // Request body
        ingredient.getId()  // Variable de URL
    );
    
    log.info("Ingredient {} updated", ingredient.getId());
}
```

**Nota:**
- `put()` no retorna valor (void)
- El servidor típicamente retorna 204 No Content

**Ejemplo avanzado: PUT con manejo de errores**
```java
public void updateIngredient_Safe(Ingredient ingredient) {
    try {
        rest.put(
            "http://localhost:8080/ingredients/{id}",
            ingredient,
            ingredient.getId()
        );
        log.info("Ingredient {} updated successfully", ingredient.getId());
    } catch (HttpClientErrorException e) {
        if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
            log.error("Ingredient {} not found", ingredient.getId());
        } else {
            log.error("Error updating ingredient: {}", e.getMessage());
        }
    }
}
```

---

### 7.3.3 DELETEing resources

```java
public void deleteIngredient(String ingredientId) {
    rest.delete(
        "http://localhost:8080/ingredients/{id}",
        ingredientId
    );
    
    log.info("Ingredient {} deleted", ingredientId);
}
```

**Nota:**
- `delete()` no retorna valor (void)
- El servidor típicamente retorna 204 No Content

**Ejemplo alternativo: DELETE por objeto**
```java
public void deleteIngredient(Ingredient ingredient) {
    rest.delete(
        "http://localhost:8080/ingredients/{id}",
        ingredient.getId()
    );
    log.info("Ingredient {} deleted", ingredient.getId());
}
```

---

### Cliente REST Completo - Ejemplo Integrado

```java
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
     */
    public Ingredient getIngredientById_Variant1(String ingredientId) {
        return rest.getForObject(
            "http://localhost:8080/ingredients/{id}",
            Ingredient.class,
            ingredientId
        );
    }
    
    /**
     * Forma 2: getForObject con Map de variables
     */
    public Ingredient getIngredientById_Variant2(String ingredientId) {
        Map<String, String> urlVariables = new HashMap<>();
        urlVariables.put("id", ingredientId);
        
        return rest.getForObject(
            "http://localhost:8080/ingredients/{id}",
            Ingredient.class,
            urlVariables
        );
    }
    
    /**
     * Forma 3: getForObject con URI construida
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
     * Forma 4: getForEntity - Con acceso a headers
     */
    public Ingredient getIngredientById_WithHeaders(String ingredientId) {
        ResponseEntity<Ingredient> responseEntity = rest.getForEntity(
            "http://localhost:8080/ingredients/{id}",
            Ingredient.class,
            ingredientId
        );
        
        log.info("Fetched time: {}", responseEntity.getHeaders().getDate());
        log.info("Status code: {}", responseEntity.getStatusCode());
        log.info("Content-Type: {}", responseEntity.getHeaders().getContentType());
        
        return responseEntity.getBody();
    }
    
    // ====================================================================
    // POST - Crear recursos
    // ====================================================================
    
    /**
     * Forma 1: postForObject - Retorna el objeto creado
     */
    public Ingredient createIngredient_ReturnObject(Ingredient ingredient) {
        return rest.postForObject(
            "http://localhost:8080/ingredients",
            ingredient,
            Ingredient.class
        );
    }
    
    /**
     * Forma 2: postForLocation - Retorna URI del recurso creado
     */
    public URI createIngredient_ReturnLocation(Ingredient ingredient) {
        return rest.postForLocation(
            "http://localhost:8080/ingredients",
            ingredient
        );
    }
    
    /**
     * Forma 3: postForEntity - Retorna ResponseEntity completo
     */
    public Ingredient createIngredient_ReturnEntity(Ingredient ingredient) {
        ResponseEntity<Ingredient> responseEntity = rest.postForEntity(
            "http://localhost:8080/ingredients",
            ingredient,
            Ingredient.class
        );
        
        log.info("New resource created at {}",
                 responseEntity.getHeaders().getLocation());
        log.info("Status code: {}", responseEntity.getStatusCode());
        
        return responseEntity.getBody();
    }
    
    // ====================================================================
    // PUT - Actualizar recursos
    // ====================================================================
    
    /**
     * put - Actualizar ingrediente existente
     */
    public void updateIngredient(Ingredient ingredient) {
        rest.put(
            "http://localhost:8080/ingredients/{id}",
            ingredient,
            ingredient.getId()
        );
        log.info("Ingredient {} updated successfully", ingredient.getId());
    }
    
    // ====================================================================
    // DELETE - Eliminar recursos
    // ====================================================================
    
    /**
     * delete - Eliminar ingrediente
     */
    public void deleteIngredient(String ingredientId) {
        rest.delete(
            "http://localhost:8080/ingredients/{id}",
            ingredientId
        );
        log.info("Ingredient {} deleted successfully", ingredientId);
    }
    
    // ====================================================================
    // Métodos recomendados para uso real
    // ====================================================================
    
    public Ingredient getIngredientById(String ingredientId) {
        return getIngredientById_Variant1(ingredientId);
    }
    
    public Ingredient createIngredient(Ingredient ingredient) {
        return createIngredient_ReturnObject(ingredient);
    }
}
```

---

## Tablas de Referencia Rápida

### Anotaciones REST

| Anotación | Propósito | Ejemplo |
|-----------|-----------|---------|
| `@RestController` | Marca clase como REST controller | `@RestController` |
| `@RequestMapping` | Define ruta base y configuración | `@RequestMapping("/api/tacos", produces="application/json")` |
| `@GetMapping` | HTTP GET | `@GetMapping` o `@GetMapping("/{id}")` |
| `@PostMapping` | HTTP POST | `@PostMapping(consumes="application/json")` |
| `@PutMapping` | HTTP PUT (reemplazar completo) | `@PutMapping("/{id}")` |
| `@PatchMapping` | HTTP PATCH (actualizar parcial) | `@PatchMapping("/{id}")` |
| `@DeleteMapping` | HTTP DELETE | `@DeleteMapping("/{id}")` |
| `@PathVariable` | Variable de URL/path | `@PathVariable("id") Long id` |
| `@RequestBody` | Cuerpo de request (JSON → Objeto) | `@RequestBody Taco taco` |
| `@ResponseStatus` | Define status HTTP de respuesta | `@ResponseStatus(HttpStatus.CREATED)` |
| `@CrossOrigin` | Habilitar CORS | `@CrossOrigin(origins="http://...")` |

### HTTP Status Codes

| Código | Constante | Significado | Cuándo Usar |
|--------|-----------|-------------|-------------|
| 200 | `HttpStatus.OK` | Éxito | GET, PUT, PATCH exitosos |
| 201 | `HttpStatus.CREATED` | Recurso creado | POST exitoso |
| 204 | `HttpStatus.NO_CONTENT` | Éxito sin contenido | DELETE, PUT sin retorno |
| 400 | `HttpStatus.BAD_REQUEST` | Request inválido | Errores de validación |
| 404 | `HttpStatus.NOT_FOUND` | No encontrado | GET por ID inexistente |
| 500 | `HttpStatus.INTERNAL_SERVER_ERROR` | Error del servidor | Excepciones no manejadas |

### RestTemplate - Métodos

| Método | HTTP | Retorna | Caso de Uso |
|--------|------|---------|-------------|
| `getForObject()` | GET | T (objeto) | Obtener recurso directamente |
| `getForEntity()` | GET | ResponseEntity<T> | Obtener + headers + status |
| `postForObject()` | POST | T (objeto creado) | Crear y obtener recurso |
| `postForLocation()` | POST | URI | Crear y obtener ubicación |
| `postForEntity()` | POST | ResponseEntity<T> | Crear + headers + status |
| `put()` | PUT | void | Actualizar recurso |
| `delete()` | DELETE | void | Eliminar recurso |

---

## Comparaciones

### @RestController vs Spring Data REST

| Aspecto | @RestController | Spring Data REST |
|---------|-----------------|------------------|
| **Código** | Escribir controller completo | Cero código (auto-generado) |
| **Control** | Total | Limitado |
| **HATEOAS** | Manual (opcional) | Automático |
| **Paginación** | Manual | Automática |
| **Búsquedas custom** | Manual | Métodos de repositorio |
| **Lógica compleja** | ✅ Soportado | ❌ No recomendado |
| **Personalización** | Total | Limitada |
| **Uso recomendado** | APIs complejas | CRUD simple, prototipos |

### PUT vs PATCH

| Aspecto | PUT | PATCH |
|---------|-----|-------|
| **Operación** | Reemplazar recurso completo | Actualizar campos específicos |
| **Campos no enviados** | Se pierden o setean a null | Se mantienen sin cambios |
| **Idempotencia** | Sí | Generalmente sí |
| **Complejidad** | Simple | Más complejo (validar nulls) |
| **Uso típico** | Actualización completa | Actualización parcial |

---

## Consejos y Buenas Prácticas

### RESTful Controllers

✅ **DO:**
- Usar `@RestController` en lugar de `@Controller` + `@ResponseBody`
- Retornar `ResponseEntity` cuando necesites control del status code
- Usar `@CrossOrigin` para habilitar CORS
- Validar input con `@Valid`
- Usar `Optional` y manejar casos de no encontrado
- Separar lógica de negocio en `@Service`
- Loguear operaciones importantes

❌ **DON'T:**
- No mezclar lógica de negocio en controllers
- No usar `@Controller` para REST APIs
- No ignorar manejo de excepciones
- No hardcodear URLs
- No exponer detalles de implementación en JSON

### Spring Data REST

✅ **DO:**
- Usar para CRUDs simples y prototipos
- Personalizar con `@RepositoryRestResource`
- Configurar paginación por defecto
- Exponer IDs cuando sea necesario
- Configurar base path (`/api`)

❌ **DON'T:**
- No usar para lógica de negocio compleja
- No exponer todos los repositorios sin control
- No depender solo de auto-generación en producción

### RestTemplate

✅ **DO:**
- Configurar como `@Bean` (reusable)
- Manejar excepciones (`RestClientException`, `HttpClientErrorException`)
- Usar `UriComponentsBuilder` para URLs complejas
- Implementar retry logic para servicios externos
- Configurar timeouts

❌ **DON'T:**
- No crear instancias nuevas en cada método
- No ignorar timeouts y conexiones
- No asumir que el servicio siempre está disponible
- No exponer credenciales en logs

---

## Resumen Ejecutivo

**Capítulo 7 en puntos clave:**

1. **@RestController** - Escribe APIs REST manualmente con control total sobre endpoints, validación, lógica de negocio
2. **Spring Data REST** - Auto-genera endpoints CRUD desde repositorios JPA, ideal para prototipos y CRUDs simples
3. **HATEOAS** - Agrega enlaces hipermedia a respuestas JSON, facilita descubrimiento de API
4. **RestTemplate** - Cliente HTTP para consumir APIs REST externas (GET, POST, PUT, DELETE)
5. **ResponseEntity** - Control completo sobre status codes, headers y body de respuestas HTTP

**Cuándo usar cada enfoque:**
- **@RestController**: APIs personalizadas, lógica compleja, control total
- **Spring Data REST**: CRUD rápido, prototipos, admin panels
- **RestTemplate**: Consumir APIs de terceros, comunicación entre microservicios

---

**Fin de la Guía - Capítulo 7: Creating REST Services**
