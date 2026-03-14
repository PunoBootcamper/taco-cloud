# 🔧 Guía de uso de @Profile en Spring Boot

## ¿Qué es @Profile?

Permite activar/desactivar beans y configuraciones según el entorno (desarrollo, producción, testing).

---

## 1. Activar un perfil

### Opción 1: En application.properties
```properties
spring.profiles.active=dev
```

### Opción 2: Como argumento JVM
```bash
java -jar app.jar -Dspring.profiles.active=prod
```

### Opción 3: Variable de entorno
```bash
export SPRING_PROFILES_ACTIVE=prod
```

### Opción 4: En IntelliJ IDEA
```
Run → Edit Configurations → Active profiles: dev
```

### Opción 5: En Maven
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

---

## 2. Usar @Profile en beans

### Bean para UN perfil específico
```java
@Bean
@Profile("dev")
public DataSource devDataSource() {
    return new H2DataSource();
}
```

### Bean para MÚLTIPLES perfiles
```java
@Bean
@Profile({"dev", "test"})
public String debugMode() {
    return "DEBUG_ENABLED";
}
```

### Bean para TODOS excepto uno (negación)
```java
@Bean
@Profile("!prod")
public String devTools() {
    return "DEV_TOOLS_ACTIVE";
}
```

### Bean SIN perfil (siempre activo)
```java
@Bean
public String commonConfig() {
    return "COMMON_CONFIG";  // Se carga en todos los perfiles
}
```

---

## 3. @Profile en clases

### Toda la configuración para un perfil
```java
@Configuration
@Profile("dev")
public class DevConfig {
    
    @Bean
    public CommandLineRunner loadData() {
        return args -> {
            System.out.println("Loading dev data...");
        };
    }
}
```

### Componentes específicos por perfil
```java
@Service
@Profile("prod")
public class ProdEmailService implements EmailService {
    // Envía emails reales
}

@Service
@Profile("dev")
public class MockEmailService implements EmailService {
    // Solo imprime en consola
}
```

---

## 4. Archivos de propiedades por perfil

### Nomenclatura
```
application.properties           → Común a todos
application-dev.properties       → Solo perfil "dev"
application-prod.properties      → Solo perfil "prod"
application-test.properties      → Solo perfil "test"
```

### Precedencia
```
application-{profile}.properties > application.properties
```

### Ejemplo application-dev.properties
```properties
spring.jpa.show-sql=true
logging.level.com.example=DEBUG
spring.h2.console.enabled=true
taco.orders.pageSize=5
```

### Ejemplo application-prod.properties
```properties
spring.jpa.show-sql=false
logging.level.com.example=INFO
spring.h2.console.enabled=false
taco.orders.pageSize=20
```

---

## 5. Múltiples perfiles activos

### Activar varios a la vez
```properties
spring.profiles.active=dev,debug,local
```

### Incluir perfiles adicionales
```properties
spring.profiles.include=common,metrics
```

---

## 6. Perfiles en YAML (application.yml)

```yaml
# Común
spring:
  application:
    name: taco-cloud

---
# Perfil dev
spring:
  config:
    activate:
      on-profile: dev
  jpa:
    show-sql: true
  
logging:
  level:
    com.example: DEBUG

---
# Perfil prod
spring:
  config:
    activate:
      on-profile: prod
  jpa:
    show-sql: false

logging:
  level:
    com.example: INFO
```

---

## 7. Ejemplo completo en el proyecto

### Ver ProfileExampleConfig.java
```java
@Configuration
public class ProfileExampleConfig {
    
    @Bean
    @Profile("dev")
    public CommandLineRunner dataLoaderDev() {
        return args -> log.info("🚀 DEV MODE");
    }
    
    @Bean
    @Profile("prod")
    public CommandLineRunner dataLoaderProd() {
        return args -> log.info("🏭 PROD MODE");
    }
}
```

### Probar cambios de perfil

#### 1. Activar perfil "dev"
```properties
# application.properties
spring.profiles.active=dev
```
**Salida esperada:**
```
🚀 PROFILE DEV ACTIVE - Loading development data...
Dev mode: Detailed logging enabled
```

#### 2. Activar perfil "prod"
```properties
# application.properties
spring.profiles.active=prod
```
**Salida esperada:**
```
🏭 PROFILE PROD ACTIVE - Production environment
```

---

## 8. Verificar perfil activo en código

```java
@Autowired
private Environment env;

public void checkActiveProfile() {
    String[] profiles = env.getActiveProfiles();
    log.info("Active profiles: {}", Arrays.toString(profiles));
    
    if (env.acceptsProfiles(Profiles.of("dev"))) {
        log.info("Running in development mode");
    }
}
```

---

## 9. Testing con perfiles

```java
@SpringBootTest
@ActiveProfiles("test")  // Activa perfil test
public class OrderServiceTest {
    
    @Test
    public void testOrderCreation() {
        // Test con configuración de perfil "test"
    }
}
```

---

## 10. Mejores prácticas

✅ **Usar perfiles para:**
- Bases de datos diferentes (H2 en dev, MySQL en prod)
- Niveles de logging distintos
- Configuraciones de seguridad
- Servicios mock vs reales
- Inicialización de datos

❌ **NO usar perfiles para:**
- Lógica de negocio
- Algoritmos diferentes
- Features flags (usar feature toggles)

---

## Comandos útiles

### Ver perfil activo
```bash
# En logs al iniciar
...
The following profiles are active: dev
```

### Cambiar perfil temporalmente
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

### Múltiples perfiles
```bash
java -jar app.jar --spring.profiles.active=prod,metrics,aws
```

---

## Resumen rápido

| Acción | Código |
|--------|--------|
| Activar perfil | `spring.profiles.active=dev` |
| Bean para perfil | `@Profile("dev")` |
| Múltiples perfiles | `@Profile({"dev", "test"})` |
| Negar perfil | `@Profile("!prod")` |
| Archivo específico | `application-dev.properties` |
| Test con perfil | `@ActiveProfiles("test")` |

---

**¡Reinicia la aplicación después de cambiar el perfil activo!**
