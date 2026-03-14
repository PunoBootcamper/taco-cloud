# Guía Completa de Configuration Properties en Spring Boot

## 6. Trabajando con propiedades de configuración

Esta guía exhaustiva cubre todos los aspectos de configuración en Spring Boot, desde ajustar la autoconfiguración hasta crear propiedades personalizadas y trabajar con perfiles.

---

## Índice

1. [Ajustando la autoconfiguración](#61-ajustando-la-autoconfiguración)
   - [Abstracción del entorno](#611-entendiendo-la-abstracción-del-entorno)
   - [Configurar datasource](#612-configurando-un-datasource)
   - [Servidor embebido y SSL](#613-configurando-el-servidor-embebido)
   - [Configurar logging](#614-configurando-logging)
   - [Valores especiales de propiedades](#615-usando-valores-especiales-de-propiedades)
2. [Creando propiedades de configuración propias](#62-creando-tus-propias-propiedades-de-configuración)
   - [Definir holders de propiedades](#621-definiendo-holders-de-propiedades-de-configuración)
   - [Declarar metadata](#622-declarando-metadata-de-propiedades)
3. [Configurando con perfiles](#63-configurando-con-perfiles)
   - [Propiedades específicas por perfil](#631-definiendo-propiedades-específicas-por-perfil)
   - [Activar perfiles](#632-activando-perfiles)
   - [Creación condicional de beans](#633-creando-beans-condicionalmente-con-perfiles)

---

## 6.1 Ajustando la autoconfiguración

Spring Boot autoconfigura muchas cosas por defecto. Puedes personalizarlas usando propiedades de configuración.

### Fuentes de configuración (en orden de precedencia)

Spring Boot lee configuración de múltiples fuentes, de **mayor a menor prioridad**:

| Precedencia | Fuente | Ejemplo |
|-------------|--------|---------|
| 1 | **Command line arguments** | `--server.port=9000` |
| 2 | **JVM system properties** | `-Dserver.port=9000` |
| 3 | **OS environment variables** | `SERVER_PORT=9000` |
| 4 | **application-{profile}.properties** fuera del JAR | `/config/application-prod.properties` |
| 5 | **application-{profile}.properties** dentro del JAR | `src/main/resources/application-prod.properties` |
| 6 | **application.properties** fuera del JAR | `/config/application.properties` |
| 7 | **application.properties** dentro del JAR | `src/main/resources/application.properties` |
| 8 | **@PropertySource** | `@PropertySource("classpath:custom.properties")` |
| 9 | **Default properties** | `SpringApplication.setDefaultProperties()` |

**Regla de oro:** Las fuentes con mayor precedencia **sobrescriben** a las de menor precedencia.

---

## 6.1.1 Entendiendo la abstracción del entorno

Spring Boot unifica todas las fuentes de configuración en una abstracción llamada **Environment**.

### Acceder al Environment

**Ejemplo 1: Inyectar Environment**

```java
package com.example.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class AppConfig {
    
    @Autowired
    private Environment env;
    
    public void printConfig() {
        // Obtener propiedad
        String appName = env.getProperty("spring.application.name");
        System.out.println("App Name: " + appName);
        
        // Obtener con valor por defecto
        int port = env.getProperty("server.port", Integer.class, 8080);
        System.out.println("Server Port: " + port);
        
        // Verificar si existe
        boolean hasDataSource = env.containsProperty("spring.datasource.url");
        System.out.println("Has DataSource: " + hasDataSource);
        
        // Obtener perfiles activos
        String[] activeProfiles = env.getActiveProfiles();
        System.out.println("Active Profiles: " + String.join(", ", activeProfiles));
        
        // Obtener perfiles por defecto
        String[] defaultProfiles = env.getDefaultProfiles();
        System.out.println("Default Profiles: " + String.join(", ", defaultProfiles));
    }
}
```

**Ejemplo 2: Usando @Value**

```java
package com.example.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ConfigService {
    
    // Inyectar valor simple
    @Value("${spring.application.name}")
    private String applicationName;
    
    // Con valor por defecto
    @Value("${app.max-connections:100}")
    private int maxConnections;
    
    // Expresión SpEL
    @Value("#{systemProperties['user.home']}")
    private String userHome;
    
    // Lista de valores
    @Value("${app.allowed-origins:http://localhost:3000,http://localhost:8080}")
    private String[] allowedOrigins;
    
    // Valor booleano
    @Value("${app.feature.enabled:true}")
    private boolean featureEnabled;
    
    // Valor numérico con conversión
    @Value("${app.timeout:30}")
    private long timeout;
    
    public void printValues() {
        System.out.println("Application Name: " + applicationName);
        System.out.println("Max Connections: " + maxConnections);
        System.out.println("Feature Enabled: " + featureEnabled);
        System.out.println("Timeout: " + timeout + " seconds");
    }
}
```

**Ejemplo 3: PropertySource personalizado**

```java
package com.example.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:custom.properties")
@PropertySource("classpath:external-${spring.profiles.active}.properties")
public class CustomPropertyConfig {
    // Las propiedades de custom.properties están disponibles ahora
}
```

**custom.properties:**
```properties
app.custom.setting=custom-value
app.custom.timeout=60
```

**Ejemplo 4: Múltiples PropertySources**

```java
@Configuration
@PropertySources({
    @PropertySource("classpath:app.properties"),
    @PropertySource("classpath:database.properties"),
    @PropertySource(value = "classpath:optional.properties", ignoreResourceNotFound = true)
})
public class MultiPropertyConfig {
}
```

### Conversión automática de tipos

Spring Boot convierte automáticamente valores de properties a los tipos correctos:

```java
@Component
public class TypeConversionExample {
    
    @Value("${app.enabled:true}")
    private boolean enabled;  // String "true" → boolean
    
    @Value("${app.port:8080}")
    private int port;  // String "8080" → int
    
    @Value("${app.timeout:3000}")
    private long timeout;  // String "3000" → long
    
    @Value("${app.ratio:0.75}")
    private double ratio;  // String "0.75" → double
    
    @Value("${app.tags:web,api,rest}")
    private List<String> tags;  // CSV → List<String>
    
    @Value("${app.expiry:PT30M}")
    private Duration expiry;  // ISO-8601 → Duration
    
    @Value("${app.size:10MB}")
    private DataSize size;  // "10MB" → DataSize
    
    @Value("${app.start-date:2024-01-01}")
    private LocalDate startDate;  // String → LocalDate
}
```

---

## 6.1.2 Configurando un datasource

Spring Boot autoconfigura un DataSource si encuentra librerías de base de datos en el classpath.

### Formato: application.properties vs application.yml

**application.properties:**
```properties
# === H2 Database (in-memory) ===
spring.datasource.url=jdbc:h2:mem:tacocloud
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.datasource.generate-unique-name=false

# H2 Console
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

# === MySQL ===
# spring.datasource.url=jdbc:mysql://localhost:3306/tacocloud
# spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
# spring.datasource.username=root
# spring.datasource.password=secret
```

**application.yml:**
```yaml
spring:
  datasource:
    # H2 Database
    url: jdbc:h2:mem:tacocloud
    driver-class-name: org.h2.Driver
    username: sa
    password: 
    generate-unique-name: false
  
  h2:
    console:
      enabled: true
      path: /h2-console

# MySQL configuration (commented)
# spring:
#   datasource:
#     url: jdbc:mysql://localhost:3306/tacocloud
#     driver-class-name: com.mysql.cj.jdbc.Driver
#     username: root
#     password: secret
```

### Configuraciones de DataSource por base de datos

**H2 (in-memory):**
```properties
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# Persistir en archivo
# spring.datasource.url=jdbc:h2:file:./data/tacocloud
```

**H2 (file-based con persistencia):**
```yaml
spring:
  datasource:
    url: jdbc:h2:file:./data/tacocloud
    driver-class-name: org.h2.Driver
    username: sa
    password: taco123
  jpa:
    hibernate:
      ddl-auto: update  # create, create-drop, update, validate, none
```

**MySQL:**
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/tacocloud?useSSL=false&serverTimezone=UTC
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.username=taco_user
spring.datasource.password=taco_password

# Connection pool settings
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
```

**PostgreSQL:**
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/tacocloud
    driver-class-name: org.postgresql.Driver
    username: postgres
    password: postgres
    hikari:
      maximum-pool-size: 10
      connection-timeout: 20000
```

**Oracle:**
```properties
spring.datasource.url=jdbc:oracle:thin:@localhost:1521:xe
spring.datasource.driver-class-name=oracle.jdbc.OracleDriver
spring.datasource.username=system
spring.datasource.password=oracle
```

**SQL Server:**
```yaml
spring:
  datasource:
    url: jdbc:sqlserver://localhost:1433;databaseName=tacocloud
    driver-class-name: com.microsoft.sqlserver.jdbc.SQLServerDriver
    username: sa
    password: YourStrongPassword123
```

### Configuración de JPA/Hibernate

```properties
# === Hibernate DDL ===
spring.jpa.hibernate.ddl-auto=create-drop
# create: crea el esquema al inicio, NO drop al final
# create-drop: crea al inicio, drop al final (ideal para testing)
# update: actualiza el esquema si cambia (cuidado en producción)
# validate: solo valida que el esquema coincide
# none: no hace nada

# === Mostrar SQL ===
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# === Dialecto (auto-detectado generalmente) ===
# spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
# spring.jpa.database-platform=org.hibernate.dialect.MySQL8Dialect
# spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect

# === Naming strategy ===
spring.jpa.hibernate.naming.physical-strategy=org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl
spring.jpa.hibernate.naming.implicit-strategy=org.hibernate.boot.model.naming.ImplicitNamingStrategyLegacyJpaImpl

# === Inicialización de datos ===
spring.jpa.defer-datasource-initialization=true
spring.sql.init.mode=always
# spring.sql.init.data-locations=classpath:data.sql
# spring.sql.init.schema-locations=classpath:schema.sql
```

**En YAML:**
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: create-drop
      naming:
        physical-strategy: org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl
        implicit-strategy: org.hibernate.boot.model.naming.ImplicitNamingStrategyLegacyJpaImpl
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        use_sql_comments: true
        jdbc:
          batch_size: 20
        order_inserts: true
        order_updates: true
    defer-datasource-initialization: true
  
  sql:
    init:
      mode: always
      data-locations: classpath:data.sql
      schema-locations: classpath:schema.sql
      encoding: UTF-8
      continue-on-error: false
```

### HikariCP (Connection Pool por defecto)

```properties
# === HikariCP Settings ===
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
spring.datasource.hikari.auto-commit=true
spring.datasource.hikari.pool-name=TacoHikariPool
spring.datasource.hikari.leak-detection-threshold=60000

# Propiedades específicas de la base de datos
spring.datasource.hikari.data-source-properties.cachePrepStmts=true
spring.datasource.hikari.data-source-properties.prepStmtCacheSize=250
spring.datasource.hikari.data-source-properties.prepStmtCacheSqlLimit=2048
spring.datasource.hikari.data-source-properties.useServerPrepStmts=true
```

### Múltiples DataSources

**Ejemplo de configuración con 2 bases de datos:**

```java
@Configuration
public class DataSourceConfig {
    
    // === Primary DataSource ===
    @Primary
    @Bean(name = "primaryDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.primary")
    public DataSource primaryDataSource() {
        return DataSourceBuilder.create().build();
    }
    
    @Primary
    @Bean(name = "primaryEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean primaryEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("primaryDataSource") DataSource dataSource) {
        return builder
            .dataSource(dataSource)
            .packages("com.example.primary.domain")
            .persistenceUnit("primary")
            .build();
    }
    
    @Primary
    @Bean(name = "primaryTransactionManager")
    public PlatformTransactionManager primaryTransactionManager(
            @Qualifier("primaryEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
    
    // === Secondary DataSource ===
    @Bean(name = "secondaryDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.secondary")
    public DataSource secondaryDataSource() {
        return DataSourceBuilder.create().build();
    }
    
    @Bean(name = "secondaryEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean secondaryEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("secondaryDataSource") DataSource dataSource) {
        return builder
            .dataSource(dataSource)
            .packages("com.example.secondary.domain")
            .persistenceUnit("secondary")
            .build();
    }
    
    @Bean(name = "secondaryTransactionManager")
    public PlatformTransactionManager secondaryTransactionManager(
            @Qualifier("secondaryEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
```

**application.properties para múltiples datasources:**
```properties
# Primary DataSource
spring.datasource.primary.url=jdbc:mysql://localhost:3306/tacocloud
spring.datasource.primary.username=root
spring.datasource.primary.password=secret
spring.datasource.primary.driver-class-name=com.mysql.cj.jdbc.Driver

# Secondary DataSource
spring.datasource.secondary.url=jdbc:postgresql://localhost:5432/analytics
spring.datasource.secondary.username=postgres
spring.datasource.secondary.password=postgres
spring.datasource.secondary.driver-class-name=org.postgresql.Driver
```

---

## 6.1.3 Configurando el servidor embebido

Spring Boot incluye servidores embebidos (Tomcat, Jetty, Undertow) que puedes configurar.

### Configuración básica del servidor

**application.properties:**
```properties
# === Puerto del servidor ===
server.port=9090
# server.port=0  # Puerto aleatorio
# server.port=-1 # Deshabilitar servidor HTTP (solo para apps no-web)

# === Context path ===
server.servlet.context-path=/api
# Acceso: http://localhost:9090/api/orders

# === Session timeout ===
server.servlet.session.timeout=30m
server.servlet.session.cookie.name=TACOSESSION
server.servlet.session.cookie.max-age=7200
server.servlet.session.cookie.secure=false
server.servlet.session.cookie.http-only=true

# === Compresión ===
server.compression.enabled=true
server.compression.mime-types=text/html,text/xml,text/plain,text/css,text/javascript,application/javascript,application/json
server.compression.min-response-size=1024

# === Encoding ===
server.servlet.encoding.charset=UTF-8
server.servlet.encoding.enabled=true
server.servlet.encoding.force=true
```

**application.yml:**
```yaml
server:
  port: 9090
  servlet:
    context-path: /api
    session:
      timeout: 30m
      cookie:
        name: TACOSESSION
        max-age: 7200
        secure: false
        http-only: true
  compression:
    enabled: true
    mime-types:
      - text/html
      - text/xml
      - application/json
      - application/javascript
    min-response-size: 1KB
  encoding:
    charset: UTF-8
    enabled: true
    force: true
```

### Configuración de Tomcat

```properties
# === Tomcat específico ===
server.tomcat.threads.max=200
server.tomcat.threads.min-spare=10
server.tomcat.max-connections=10000
server.tomcat.accept-count=100
server.tomcat.connection-timeout=20000

# URI encoding
server.tomcat.uri-encoding=UTF-8

# Access logs
server.tomcat.accesslog.enabled=true
server.tomcat.accesslog.directory=logs
server.tomcat.accesslog.prefix=access_log
server.tomcat.accesslog.suffix=.log
server.tomcat.accesslog.pattern=%h %l %u %t "%r" %s %b %D

# Remote IP
server.tomcat.remoteip.remote-ip-header=X-Forwarded-For
server.tomcat.remoteip.protocol-header=X-Forwarded-Proto
```

**En YAML:**
```yaml
server:
  tomcat:
    threads:
      max: 200
      min-spare: 10
    max-connections: 10000
    accept-count: 100
    connection-timeout: 20s
    uri-encoding: UTF-8
    accesslog:
      enabled: true
      directory: logs
      prefix: access_log
      suffix: .log
      pattern: '%h %l %u %t "%r" %s %b %D'
    remoteip:
      remote-ip-header: X-Forwarded-For
      protocol-header: X-Forwarded-Proto
```

### Cambiar a Jetty o Undertow

**Excluir Tomcat e incluir Jetty:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <exclusions>
        <exclusion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-tomcat</artifactId>
        </exclusion>
    </exclusions>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jetty</artifactId>
</dependency>
```

**Configuración de Jetty:**
```properties
server.jetty.threads.max=200
server.jetty.threads.min=8
server.jetty.threads.idle-timeout=60000
server.jetty.connection-idle-timeout=30000
```

**Incluir Undertow (más performante):**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-undertow</artifactId>
</dependency>
```

**Configuración de Undertow:**
```properties
server.undertow.threads.io=4
server.undertow.threads.worker=32
server.undertow.buffer-size=1024
server.undertow.direct-buffers=true
```

### Configuración SSL/HTTPS

**Generar keystore (self-signed para desarrollo):**

```bash
# Usando keytool (incluido en JDK)
keytool -genkeypair -alias tacokeystore -keyalg RSA -keysize 2048 \
  -storetype PKCS12 -keystore tacokeystore.p12 -validity 3650 \
  -storepass letmein

# Responder preguntas:
# What is your first and last name? [localhost]
# What is the name of your organizational unit? [Development]
# What is the name of your organization? [TacoCloud]
# etc.
```

**Colocar tacokeystore.p12 en:**
- Desarrollo: `src/main/resources/`
- Producción: Fuera del JAR, configurado por ruta absoluta

**Configuración SSL en properties:**
```properties
# === HTTPS/SSL Configuration ===
server.port=8443
server.ssl.enabled=true
server.ssl.key-store=classpath:tacokeystore.p12
server.ssl.key-store-type=PKCS12
server.ssl.key-store-password=letmein
server.ssl.key-alias=tacokeystore

# Protocolos y cifrados
server.ssl.protocol=TLS
server.ssl.enabled-protocols=TLSv1.2,TLSv1.3
server.ssl.ciphers=TLS_RSA_WITH_AES_128_CBC_SHA,TLS_RSA_WITH_AES_256_CBC_SHA

# Cliente requiere certificado
server.ssl.client-auth=need  # need, want, none
```

**En YAML:**
```yaml
server:
  port: 8443
  ssl:
    enabled: true
    key-store: classpath:tacokeystore.p12
    key-store-type: PKCS12
    key-store-password: letmein
    key-alias: tacokeystore
    protocol: TLS
    enabled-protocols:
      - TLSv1.2
      - TLSv1.3
    ciphers:
      - TLS_RSA_WITH_AES_128_CBC_SHA
      - TLS_RSA_WITH_AES_256_CBC_SHA
    client-auth: none
```

**Redireccionar HTTP a HTTPS (configuración adicional):**

```java
@Configuration
public class HttpsRedirectConfig {
    
    @Bean
    public ServletWebServerFactory servletContainer() {
        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory() {
            @Override
            protected void postProcessContext(Context context) {
                SecurityConstraint securityConstraint = new SecurityConstraint();
                securityConstraint.setUserConstraint("CONFIDENTIAL");
                SecurityCollection collection = new SecurityCollection();
                collection.addPattern("/*");
                securityConstraint.addCollection(collection);
                context.addConstraint(securityConstraint);
            }
        };
        
        tomcat.addAdditionalTomcatConnectors(redirectConnector());
        return tomcat;
    }
    
    private Connector redirectConnector() {
        Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
        connector.setScheme("http");
        connector.setPort(8080);
        connector.setSecure(false);
        connector.setRedirectPort(8443);
        return connector;
    }
}
```

### HTTP/2

```properties
# Requiere SSL habilitado primero
server.http2.enabled=true
```

---

## 6.1.4 Configurando logging

Spring Boot usa SLF4J con Logback por defecto.

### Configuración básica en properties

```properties
# === Nivel de logging ===
logging.level.root=INFO
logging.level.com.example.tacocloud=DEBUG
logging.level.org.springframework.web=DEBUG
logging.level.org.springframework.security=DEBUG
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE

# === Archivo de log ===
logging.file.name=logs/taco-cloud.log
logging.file.path=logs
logging.file.max-size=10MB
logging.file.max-history=30
logging.file.total-size-cap=1GB

# === Patrón de log ===
logging.pattern.console=%clr(%d{yyyy-MM-dd HH:mm:ss.SSS}){faint} %clr(${LOG_LEVEL_PATTERN:%5p}) %clr(${PID:- }){magenta} %clr(---){faint} %clr([%15.15t]){faint} %clr(%-40.40logger{39}){cyan} %clr(:){faint} %m%n${LOG_EXCEPTION_CONVERSION_WORD:%wEx}
logging.pattern.file=%d{yyyy-MM-dd HH:mm:ss.SSS} ${LOG_LEVEL_PATTERN:%5p} ${PID:- } --- [%t] %-40.40logger{39} : %m%n${LOG_EXCEPTION_CONVERSION_WORD:%wEx}

# === Caracteres de color deshabilitados en archivo ===
logging.pattern.dateformat=yyyy-MM-dd HH:mm:ss.SSS
```

**En YAML:**
```yaml
logging:
  level:
    root: INFO
    com.example.tacocloud: DEBUG
    org.springframework.web: DEBUG
    org.springframework.security: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
  
  file:
    name: logs/taco-cloud.log
    path: logs
    max-size: 10MB
    max-history: 30
    total-size-cap: 1GB
  
  pattern:
    console: "%clr(%d{yyyy-MM-dd HH:mm:ss.SSS}){faint} %clr(${LOG_LEVEL_PATTERN:%5p}) %clr([%15.15t]){faint} %clr(%-40.40logger{39}){cyan} : %m%n"
    file: "%d{yyyy-MM-dd HH:mm:ss.SSS} ${LOG_LEVEL_PATTERN:%5p} [%t] %-40.40logger{39} : %m%n"
    dateformat: yyyy-MM-dd HH:mm:ss.SSS
```

### Configuración avanzada con logback.xml

Para control total, crear `src/main/resources/logback-spring.xml`:

**Ejemplo 1: Configuración básica**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    
    <!-- Console Appender -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <!-- File Appender -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/taco-cloud.log</file>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
        
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/taco-cloud-%d{yyyy-MM-dd}.%i.log</fileNamePattern>
            <timeBasedFileNamingAndTriggeringPolicy 
                class="ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP">
                <maxFileSize>10MB</maxFileSize>
            </timeBasedFileNamingAndTriggeringPolicy>
            <maxHistory>30</maxHistory>
            <totalSizeCap>1GB</totalSizeCap>
        </rollingPolicy>
    </appender>
    
    <!-- Root Logger -->
    <root level="INFO">
        <appender-ref ref="CONSOLE" />
        <appender-ref ref="FILE" />
    </root>
    
    <!-- Specific Loggers -->
    <logger name="com.example.tacocloud" level="DEBUG" />
    <logger name="org.springframework.web" level="DEBUG" />
    <logger name="org.hibernate.SQL" level="DEBUG" />
    
</configuration>
```

**Ejemplo 2: Con colores y múltiples appenders**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    
    <!-- Propiedad para el patrón -->
    <property name="CONSOLE_LOG_PATTERN" 
              value="%clr(%d{yyyy-MM-dd HH:mm:ss.SSS}){faint} %clr(${LOG_LEVEL_PATTERN:-%5p}) %clr(${PID:- }){magenta} %clr(---){faint} %clr([%15.15t]){faint} %clr(%-40.40logger{39}){cyan} %clr(:){faint} %m%n${LOG_EXCEPTION_CONVERSION_WORD:-%wEx}"/>
    
    <property name="FILE_LOG_PATTERN" 
              value="%d{yyyy-MM-dd HH:mm:ss.SSS} ${LOG_LEVEL_PATTERN:-%5p} ${PID:- } --- [%t] %-40.40logger{39} : %m%n${LOG_EXCEPTION_CONVERSION_WORD:-%wEx}"/>
    
    <!-- Console Appender con colores -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>${CONSOLE_LOG_PATTERN}</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>
    
    <!-- Archivo general -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/taco-cloud.log</file>
        <encoder>
            <pattern>${FILE_LOG_PATTERN}</pattern>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/archived/taco-cloud-%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
            <timeBasedFileNamingAndTriggeringPolicy 
                class="ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP">
                <maxFileSize>10MB</maxFileSize>
            </timeBasedFileNamingAndTriggeringPolicy>
            <maxHistory>30</maxHistory>
            <totalSizeCap>1GB</totalSizeCap>
        </rollingPolicy>
    </appender>
    
    <!-- Archivo solo para errores -->
    <appender name="ERROR_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/error.log</file>
        <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
            <level>ERROR</level>
        </filter>
        <encoder>
            <pattern>${FILE_LOG_PATTERN}</pattern>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/archived/error-%d{yyyy-MM-dd}.log.gz</fileNamePattern>
            <maxHistory>90</maxHistory>
        </rollingPolicy>
    </appender>
    
    <!-- Archivo para auditoría (aplicación específica) -->
    <appender name="AUDIT_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/audit.log</file>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder" />
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/archived/audit-%d{yyyy-MM-dd}.log.gz</fileNamePattern>
            <maxHistory>365</maxHistory>
        </rollingPolicy>
    </appender>
    
    <!-- Root Logger -->
    <root level="INFO">
        <appender-ref ref="CONSOLE" />
        <appender-ref ref="FILE" />
        <appender-ref ref="ERROR_FILE" />
    </root>
    
    <!-- Loggers específicos -->
    <logger name="com.example.tacocloud" level="DEBUG" additivity="false">
        <appender-ref ref="CONSOLE" />
        <appender-ref ref="FILE" />
    </logger>
    
    <logger name="com.example.tacocloud.audit" level="INFO" additivity="false">
        <appender-ref ref="AUDIT_FILE" />
    </logger>
    
    <logger name="org.springframework.web" level="DEBUG" />
    <logger name="org.springframework.security" level="DEBUG" />
    <logger name="org.hibernate.SQL" level="DEBUG" />
    <logger name="org.hibernate.type.descriptor.sql.BasicBinder" level="TRACE" />
    
</configuration>
```

### Configuración por perfil en logback-spring.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    
    <springProfile name="dev">
        <!-- Configuración para desarrollo -->
        <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
            <encoder>
                <pattern>%clr(%d{HH:mm:ss.SSS}){faint} %clr(%-5level) %clr([%logger{20}]){cyan} : %m%n</pattern>
            </encoder>
        </appender>
        
        <root level="DEBUG">
            <appender-ref ref="CONSOLE" />
        </root>
        
        <logger name="com.example.tacocloud" level="TRACE" />
    </springProfile>
    
    <springProfile name="prod">
        <!-- Configuración para producción -->
        <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
            <file>/var/log/taco-cloud/application.log</file>
            <encoder class="net.logstash.logback.encoder.LogstashEncoder" />
            <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
                <fileNamePattern>/var/log/taco-cloud/application-%d{yyyy-MM-dd}.log.gz</fileNamePattern>
                <maxHistory>60</maxHistory>
                <totalSizeCap>10GB</totalSizeCap>
            </rollingPolicy>
        </appender>
        
        <root level="WARN">
            <appender-ref ref="FILE" />
        </root>
        
        <logger name="com.example.tacocloud" level="INFO" />
    </springProfile>
    
    <springProfile name="test">
        <!-- Configuración para testing -->
        <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
            <encoder>
                <pattern>%d{HH:mm:ss.SSS} %-5level %logger{36} - %msg%n</pattern>
            </encoder>
        </appender>
        
        <root level="INFO">
            <appender-ref ref="CONSOLE" />
        </root>
        
        <logger name="com.example.tacocloud" level="DEBUG" />
    </springProfile>
    
</configuration>
```

### Niveles de logging

| Nivel | Descripción | Cuándo usar |
|-------|-------------|-------------|
| TRACE | Más detallado | Debugging muy detallado |
| DEBUG | Información de debugging | Desarrollo, troubleshooting |
| INFO | Información general | Eventos importantes de la aplicación |
| WARN | Advertencias | Situaciones inesperadas pero manejables |
| ERROR | Errores | Errores que requieren atención |
| FATAL | Errores críticos | Fallos que pueden terminar la app |
| OFF | Sin logging | Deshabilitar logger específico |

### Uso en código

```java
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class TacoService {
    
    public void createTaco(Taco taco) {
        log.trace("Entering createTaco method with: {}", taco);
        log.debug("Creating taco: {}", taco.getName());
        
        try {
            // Business logic
            tacoRepository.save(taco);
            log.info("Taco created successfully: {}", taco.getId());
        } catch (Exception e) {
            log.error("Error creating taco: {}", taco.getName(), e);
            throw new TacoCreationException("Failed to create taco", e);
        }
        
        log.trace("Exiting createTaco method");
    }
    
    public void checkInventory() {
        int count = ingredientRepository.count();
        if (count < 10) {
            log.warn("Low ingredient inventory: {} items remaining", count);
        }
    }
}
```

---

## 6.1.5 Usando valores especiales de propiedades

Spring Boot proporciona valores especiales que puedes usar en tus propiedades.

### Propiedades del sistema y entorno

```properties
# === System properties ===
app.java-version=${java.version}
app.user-home=${user.home}
app.user-name=${user.name}
app.os-name=${os.name}

# === Environment variables ===
app.database-url=${DATABASE_URL:jdbc:h2:mem:testdb}
app.api-key=${API_KEY:default-key}

# === Spring properties ===
app.app-name=${spring.application.name}
app.active-profile=${spring.profiles.active}
```

### Valores aleatorios

```properties
# === Random values ===
app.secret=${random.value}
app.random-int=${random.int}
app.random-long=${random.long}
app.random-uuid=${random.uuid}

# Random int en rango
app.random-port=${random.int[1024,65535]}
app.random-small=${random.int(100)}

# Random string
app.random-string=${random.value}
```

### Placeholders y referencias

```properties
# === Reutilizar valores ===
app.name=TacoCloud
app.description=${app.name} is an application for ordering tacos
app.welcome-message=Welcome to ${app.name}!

# === Con valores por defecto ===
app.api-url=${API_URL:http://localhost:8080}
app.timeout=${APP_TIMEOUT:30}

# === Expresiones anidadas ===
app.base-url=http://localhost
app.server-port=9090
app.full-url=${app.base-url}:${app.server-port}
```

### Usando en código

```java
@Component
public class AppInfo {
    
    @Value("${app.name}")
    private String appName;
    
    @Value("${app.description}")
    private String description;
    
    @Value("${app.random-uuid}")
    private String instanceId;
    
    @Value("${java.version}")
    private String javaVersion;
    
    @Value("${user.home}")
    private String userHome;
    
    public void printInfo() {
        System.out.println("App: " + appName);
        System.out.println("Description: " + description);
        System.out.println("Instance ID: " + instanceId);
        System.out.println("Java Version: " + javaVersion);
        System.out.println("User Home: " + userHome);
    }
}
```

### SpEL (Spring Expression Language) en properties

```properties
# === Expresiones SpEL ===
app.max-memory=#{systemProperties['java.vm.max-memory']}
app.current-time=#{T(System).currentTimeMillis()}
app.random-boolean=#{T(java.lang.Math).random() > 0.5}
```

**Uso en @Value:**
```java
@Value("#{systemProperties['user.region']}")
private String region;

@Value("#{T(java.lang.Math).random() * 100.0}")
private double randomValue;

@Value("#{'${app.allowed-origins}'.split(',')}")
private List<String> allowedOrigins;
```

---

## 6.2 Creando tus propias propiedades de configuración

Puedes crear propiedades personalizadas para tu aplicación.

## 6.2.1 Definiendo holders de propiedades de configuración

### Enfoque 1: @Value (simple)

```java
@Component
public class AppConfig {
    
    @Value("${app.max-connections:100}")
    private int maxConnections;
    
    @Value("${app.timeout:30}")
    private long timeout;
    
    @Value("${app.feature-enabled:true}")
    private boolean featureEnabled;
}
```

**Desventaja:** No es type-safe, disperso en el código.

### Enfoque 2: @ConfigurationProperties (recomendado)

**Ejemplo 1: Clase de configuración básica**

```java
package com.example.tacocloud.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "taco")
public class TacoProperties {
    
    private int pageSize = 20;
    
    private Orders orders = new Orders();
    
    private Discount discount = new Discount();
    
    @Data
    public static class Orders {
        private int maxOrders = 100;
        private long timeout = 30;
    }
    
    @Data
    public static class Discount {
        private boolean enabled = false;
        private double percentage = 10.0;
        private List<String> codes = new ArrayList<>();
    }
}
```

**application.properties:**
```properties
taco.page-size=15
taco.orders.max-orders=50
taco.orders.timeout=60
taco.discount.enabled=true
taco.discount.percentage=15.5
taco.discount.codes=TACO10,TACO20,TACO30
```

**application.yml:**
```yaml
taco:
  page-size: 15
  orders:
    max-orders: 50
    timeout: 60
  discount:
    enabled: true
    percentage: 15.5
    codes:
      - TACO10
      - TACO20
      - TACO30
```

**Uso:**
```java
@Service
@RequiredArgsConstructor
public class OrderService {
    
    private final TacoProperties tacoProperties;
    
    public void processOrders() {
        int pageSize = tacoProperties.getPageSize();
        int maxOrders = tacoProperties.getOrders().getMaxOrders();
        
        if (tacoProperties.getDiscount().isEnabled()) {
            double discount = tacoProperties.getDiscount().getPercentage();
            // Apply discount
        }
    }
}
```

**Ejemplo 2: Con validación**

```java
package com.example.tacocloud.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Data
@Component
@Validated
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    
    /**
     * Application name
     */
    @NotBlank(message = "Application name is required")
    private String name;
    
    /**
     * Maximum number of connections
     */
    @Min(value = 1, message = "Must have at least 1 connection")
    @Max(value = 1000, message = "Cannot exceed 1000 connections")
    private int maxConnections = 100;
    
    /**
     * Timeout duration
     */
    @NotNull
    private Duration timeout = Duration.ofSeconds(30);
    
    /**
     * Admin email
     */
    @Email(message = "Must be a valid email")
    private String adminEmail;
    
    /**
     * Allowed origins for CORS
     */
    @NotEmpty(message = "At least one allowed origin is required")
    private List<String> allowedOrigins = new ArrayList<>();
    
    /**
     * API configuration
     */
    @NotNull
    @Valid
    private Api api = new Api();
    
    @Data
    public static class Api {
        
        @NotBlank
        private String baseUrl = "http://localhost:8080";
        
        @NotBlank
        @Pattern(regexp = "^[A-Za-z0-9-_]+$", message = "Invalid API key format")
        private String key;
        
        @Min(1)
        @Max(10)
        private int retries = 3;
        
        private boolean enabled = true;
    }
}
```

**application.properties con validación:**
```properties
app.name=TacoCloud
app.max-connections=200
app.timeout=60s
app.admin-email=admin@tacocloud.com
app.allowed-origins=http://localhost:3000,http://localhost:4200

app.api.base-url=https://api.tacocloud.com
app.api.key=my-secret-api-key-123
app.api.retries=5
app.api.enabled=true
```

**Ejemplo 3: Con Map y List complejos**

```java
@Data
@Component
@ConfigurationProperties(prefix = "services")
public class ServiceProperties {
    
    /**
     * Lista de URLs de servicios externos
     */
    private List<String> endpoints = new ArrayList<>();
    
    /**
     * Configuración de cada servicio por nombre
     */
    private Map<String, ServiceConfig> configs = new HashMap<>();
    
    /**
     * Lista de objetos complejos
     */
    private List<Server> servers = new ArrayList<>();
    
    @Data
    public static class ServiceConfig {
        private String url;
        private int timeout;
        private boolean enabled;
        private Map<String, String> headers = new HashMap<>();
    }
    
    @Data
    public static class Server {
        private String name;
        private String host;
        private int port;
        private List<String> protocols;
    }
}
```

**application.yml para estructuras complejas:**
```yaml
services:
  endpoints:
    - https://api.service1.com
    - https://api.service2.com
    - https://api.service3.com
  
  configs:
    payment:
      url: https://payment.api.com
      timeout: 5000
      enabled: true
      headers:
        Authorization: Bearer token123
        Content-Type: application/json
    
    shipping:
      url: https://shipping.api.com
      timeout: 3000
      enabled: true
      headers:
        API-Key: shipping-key-456
  
  servers:
    - name: prod-server-1
      host: 192.168.1.10
      port: 8080
      protocols:
        - http
        - https
    
    - name: prod-server-2
      host: 192.168.1.11
      port: 8080
      protocols:
        - https
```

**Ejemplo 4: Con Duration, DataSize, Period**

```java
@Data
@Component
@ConfigurationProperties(prefix = "app.limits")
public class LimitsProperties {
    
    /**
     * Session timeout
     */
    private Duration sessionTimeout = Duration.ofMinutes(30);
    
    /**
     * Connection timeout
     */
    private Duration connectionTimeout = Duration.ofSeconds(10);
    
    /**
     * Retry delay
     */
    private Duration retryDelay = Duration.ofMillis(500);
    
    /**
     * Maximum upload file size
     */
    private DataSize maxUpload = DataSize.ofMegabytes(10);
    
    /**
     * Cache size
     */
    private DataSize cacheSize = DataSize.ofGigabytes(1);
    
    /**
     * Cleanup period
     */
    private Period cleanupPeriod = Period.ofDays(7);
}
```

**application.properties con Duration/DataSize:**
```properties
# Durations (soporta: ns, us, ms, s, m, h, d)
app.limits.session-timeout=45m
app.limits.connection-timeout=30s
app.limits.retry-delay=1000ms

# DataSize (soporta: B, KB, MB, GB, TB)
app.limits.max-upload=50MB
app.limits.cache-size=2GB

# Period
app.limits.cleanup-period=P30D  # ISO-8601 format
```

### Habilitar @ConfigurationProperties

**Opción 1: @Component en la clase**
```java
@Data
@Component  // ← Hace que sea un bean
@ConfigurationProperties(prefix = "app")
public class AppProperties {
}
```

**Opción 2: @EnableConfigurationProperties**
```java
@Configuration
@EnableConfigurationProperties({
    TacoProperties.class,
    AppProperties.class,
    ServiceProperties.class
})
public class PropertiesConfig {
}
```

**Opción 3: @ConfigurationPropertiesScan (Spring Boot 2.2+)**
```java
@SpringBootApplication
@ConfigurationPropertiesScan("com.example.tacocloud.config")
public class TacoCloudApplication {
    public static void main(String[] args) {
        SpringApplication.run(TacoCloudApplication.class, args);
    }
}
```

### Constructor Binding (inmutabilidad)

```java
package com.example.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

import java.time.Duration;

@Getter
@ConstructorBinding
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {
    
    private final String secretKey;
    private final Duration tokenExpiration;
    private final int maxAttempts;
    
    public SecurityProperties(
            String secretKey,
            Duration tokenExpiration,
            int maxAttempts) {
        this.secretKey = secretKey;
        this.tokenExpiration = tokenExpiration;
        this.maxAttempts = maxAttempts;
    }
}
```

**Habilitar con @EnableConfigurationProperties:**
```java
@Configuration
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfig {
    // SecurityProperties se inyecta como bean inmutable
}
```

---

## 6.2.2 Declarando metadata de propiedades

La metadata proporciona autocompletado y documentación en el IDE.

### Añadir el procesador de configuración

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-configuration-processor</artifactId>
    <optional>true</optional>
</dependency>
```

**Gradle:**
```gradle
annotationProcessor "org.springframework.boot:spring-boot-configuration-processor"
```

### Metadata automática

Al compilar con el procesador, se genera automáticamente:
`target/classes/META-INF/spring-configuration-metadata.json`

**Ejemplo de metadata generada:**
```json
{
  "groups": [
    {
      "name": "taco",
      "type": "com.example.tacocloud.config.TacoProperties",
      "sourceType": "com.example.tacocloud.config.TacoProperties"
    },
    {
      "name": "taco.orders",
      "type": "com.example.tacocloud.config.TacoProperties$Orders",
      "sourceType": "com.example.tacocloud.config.TacoProperties"
    }
  ],
  "properties": [
    {
      "name": "taco.page-size",
      "type": "java.lang.Integer",
      "description": "Number of items per page",
      "sourceType": "com.example.tacocloud.config.TacoProperties",
      "defaultValue": 20
    },
    {
      "name": "taco.orders.max-orders",
      "type": "java.lang.Integer",
      "sourceType": "com.example.tacocloud.config.TacoProperties$Orders",
      "defaultValue": 100
    }
  ]
}
```

### Metadata manual (adicional)

Crear `src/main/resources/META-INF/additional-spring-configuration-metadata.json`:

```json
{
  "properties": [
    {
      "name": "taco.page-size",
      "type": "java.lang.Integer",
      "description": "Number of tacos to display per page. Must be between 5 and 25.",
      "defaultValue": 20,
      "deprecation": {
        "reason": "Use taco.pagination.size instead",
        "replacement": "taco.pagination.size"
      }
    },
    {
      "name": "taco.orders.timeout",
      "type": "java.time.Duration",
      "description": "Maximum time to wait for order processing",
      "defaultValue": "30s"
    },
    {
      "name": "app.api.key",
      "type": "java.lang.String",
      "description": "API key for external service authentication. Required in production.",
      "sourceType": "com.example.config.AppProperties$Api"
    }
  ],
  "hints": [
    {
      "name": "spring.profiles.active",
      "values": [
        {
          "value": "dev",
          "description": "Development environment with debug logging"
        },
        {
          "value": "prod",
          "description": "Production environment"
        },
        {
          "value": "test",
          "description": "Testing environment"
        }
      ]
    },
    {
      "name": "logging.level.root",
      "values": [
        {
          "value": "TRACE",
          "description": "Most detailed logging"
        },
        {
          "value": "DEBUG",
          "description": "Debug level logging"
        },
        {
          "value": "INFO",
          "description": "Informational messages"
        },
        {
          "value": "WARN",
          "description": "Warning messages"
        },
        {
          "value": "ERROR",
          "description": "Error messages only"
        }
      ]
    }
  ]
}
```

### Deprecación de propiedades

```java
@Data
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    
    /**
     * @deprecated Use {@link #maxConnections} instead
     */
    @Deprecated
    @DeprecatedConfigurationProperty(replacement = "app.max-connections")
    private int maxConn;
    
    private int maxConnections = 100;
}
```

### Documentación con Javadoc

```java
/**
 * Configuration properties for TacoCloud application.
 * 
 * <p>Controls various aspects of taco ordering and display.</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "taco")
public class TacoProperties {
    
    /**
     * Number of tacos to display per page.
     * 
     * <p>Valid range is 5-25. Default is 20.</p>
     * 
     * @see #getPageSize()
     */
    @Min(5)
    @Max(25)
    private int pageSize = 20;
    
    /**
     * Order-related configuration.
     */
    private Orders orders = new Orders();
    
    @Data
    public static class Orders {
        
        /**
         * Maximum number of concurrent orders allowed.
         * 
         * <p>Limits system load during peak times.</p>
         */
        private int maxOrders = 100;
        
        /**
         * Timeout for order processing in seconds.
         */
        private long timeout = 30;
    }
}
```

### Quick-fixing missing metadata en IntelliJ

Cuando IntelliJ muestra advertencia "Cannot resolve configuration property":

1. **Añadir dependencia:**
   - Click en advertencia → "Add spring-boot-configuration-processor"
   
2. **Rebuild proyecto:**
   - Build → Rebuild Project
   
3. **Verificar metadata:**
   - Archivo generado en `target/classes/META-INF/spring-configuration-metadata.json`

4. **Si no funciona:**
   - Settings → Build → Compiler → Annotation Processors → Enable annotation processing ✓
   - Invalidate Caches → Restart

---

## 6.3 Configurando con perfiles

Los perfiles permiten tener configuraciones diferentes según el entorno.

## 6.3.1 Definiendo propiedades específicas por perfil

### Archivos separados por perfil

**Estructura:**
```
src/main/resources/
├── application.properties              # Común a todos
├── application-dev.properties          # Solo desarrollo
├── application-prod.properties         # Solo producción
└── application-test.properties         # Solo testing
```

**application.properties (común):**
```properties
spring.application.name=taco-cloud

# Configuración común
app.name=TacoCloud
app.version=1.0.0
```

**application-dev.properties:**
```properties
# Servidor
server.port=8080

# Base de datos
spring.datasource.url=jdbc:h2:mem:tacocloud
spring.h2.console.enabled=true

# JPA
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.hibernate.ddl-auto=create-drop

# Logging
logging.level.root=INFO
logging.level.com.example.tacocloud=DEBUG
logging.level.org.springframework.web=DEBUG
logging.level.org.hibernate.SQL=DEBUG

# Thymeleaf
spring.thymeleaf.cache=false

# Configuración personalizada
taco.page-size=5
app.debug=true
app.feature.new-ui=true
```

**application-prod.properties:**
```properties
# Servidor
server.port=9090

# SSL
server.ssl.enabled=true
server.ssl.key-store=classpath:keystore.p12
server.ssl.key-store-password=${SSL_PASSWORD}
server.ssl.key-store-type=PKCS12

# Base de datos
spring.datasource.url=jdbc:mysql://prod-db.example.com:3306/tacocloud
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# Connection pool
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5

# JPA
spring.jpa.show-sql=false
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect

# Logging
logging.level.root=WARN
logging.level.com.example.tacocloud=INFO
logging.file.name=/var/log/taco-cloud/application.log
logging.file.max-size=10MB
logging.file.max-history=30

# Thymeleaf
spring.thymeleaf.cache=true

# Configuración personalizada
taco.page-size=20
app.debug=false
app.feature.new-ui=false
```

**application-test.properties:**
```properties
# Puerto aleatorio para tests
server.port=0

# Base de datos en memoria
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driver-class-name=org.h2.Driver

# JPA
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=false

# Logging mínimo
logging.level.root=ERROR
logging.level.com.example.tacocloud=INFO

# Configuración de test
taco.page-size=10
app.debug=true
```

### Formato YAML con múltiples perfiles en un archivo

**application.yml:**
```yaml
# === Configuración común ===
spring:
  application:
    name: taco-cloud

app:
  name: TacoCloud
  version: 1.0.0

---
# === Perfil DEV ===
spring:
  config:
    activate:
      on-profile: dev
  
  datasource:
    url: jdbc:h2:mem:tacocloud
    driver-class-name: org.h2.Driver
  
  h2:
    console:
      enabled: true
  
  jpa:
    show-sql: true
    hibernate:
      ddl-auto: create-drop
    properties:
      hibernate:
        format_sql: true
  
  thymeleaf:
    cache: false

server:
  port: 8080

logging:
  level:
    root: INFO
    com.example.tacocloud: DEBUG
    org.springframework.web: DEBUG

taco:
  page-size: 5

app:
  debug: true

---
# === Perfil PROD ===
spring:
  config:
    activate:
      on-profile: prod
  
  datasource:
    url: jdbc:mysql://prod-db.example.com:3306/tacocloud
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
  
  jpa:
    show-sql: false
    hibernate:
      ddl-auto: validate
  
  thymeleaf:
    cache: true

server:
  port: 9090
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${SSL_PASSWORD}
    key-store-type: PKCS12

logging:
  level:
    root: WARN
    com.example.tacocloud: INFO
  file:
    name: /var/log/taco-cloud/application.log
    max-size: 10MB
    max-history: 30

taco:
  page-size: 20

app:
  debug: false

---
# === Perfil TEST ===
spring:
  config:
    activate:
      on-profile: test
  
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
  
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: false

server:
  port: 0  # Puerto aleatorio

logging:
  level:
    root: ERROR
    com.example.tacocloud: INFO

taco:
  page-size: 10
```

---

## 6.3.2 Activando perfiles

### Múltiples formas de activar perfiles

**1. En application.properties:**
```properties
spring.profiles.active=dev
```

**2. En application.yml:**
```yaml
spring:
  profiles:
    active: dev
```

**3. Como argumento de línea de comandos:**
```bash
java -jar taco-cloud.jar --spring.profiles.active=prod
```

**4. Como propiedad del sistema:**
```bash
java -Dspring.profiles.active=prod -jar taco-cloud.jar
```

**5. Como variable de entorno:**
```bash
export SPRING_PROFILES_ACTIVE=prod
java -jar taco-cloud.jar
```

**Windows:**
```cmd
set SPRING_PROFILES_ACTIVE=prod
java -jar taco-cloud.jar
```

**6. En Maven:**
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

**7. En Gradle:**
```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

**8. En IntelliJ IDEA:**
```
Run → Edit Configurations → Active profiles: dev
```

**9. En Eclipse/STS:**
```
Run Configurations → Spring Boot App → Profile tab → Profiles: dev
```

**10. Programáticamente:**
```java
@SpringBootApplication
public class TacoCloudApplication {
    
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(TacoCloudApplication.class);
        app.setAdditionalProfiles("dev");
        app.run(args);
    }
}
```

### Múltiples perfiles activos

```properties
spring.profiles.active=dev,debug,h2
```

**Precedencia:** Los perfiles de la derecha tienen mayor prioridad.

```bash
java -jar app.jar --spring.profiles.active=prod,metrics,aws
```

### Incluir perfiles adicionales

```properties
spring.profiles.active=dev
spring.profiles.include=debug,local
```

Resultado: `dev`, `debug`, `local` están activos.

### Perfiles por defecto

```properties
spring.profiles.default=dev
```

Se activa **solo si** no hay ningún perfil explícitamente activo.

---

## 6.3.3 Creando beans condicionalmente con perfiles

### @Profile en métodos @Bean

```java
@Configuration
public class DataSourceConfig {
    
    @Bean
    @Profile("dev")
    public DataSource devDataSource() {
        return new EmbeddedDatabaseBuilder()
            .setType(EmbeddedDatabaseType.H2)
            .addScript("schema.sql")
            .addScript("test-data.sql")
            .build();
    }
    
    @Bean
    @Profile("prod")
    public DataSource prodDataSource() {
        DataSourceBuilder<?> builder = DataSourceBuilder.create();
        builder.url("jdbc:mysql://prod-db:3306/tacocloud");
        builder.username(System.getenv("DB_USER"));
        builder.password(System.getenv("DB_PASSWORD"));
        return builder.build();
    }
    
    @Bean
    @Profile("test")
    public DataSource testDataSource() {
        return new EmbeddedDatabaseBuilder()
            .setType(EmbeddedDatabaseType.H2)
            .setName("testdb")
            .build();
    }
}
```

### @Profile en clases completas

```java
@Configuration
@Profile("dev")
public class DevSecurityConfig {
    
    @Bean
    public SecurityFilterChain devFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .authorizeRequests()
                .anyRequest().permitAll();
        return http.build();
    }
    
    @Bean
    public CommandLineRunner loadDevData(UserRepository userRepo) {
        return args -> {
            userRepo.save(new User("dev", "password", "Dev User"));
            log.info("Dev data loaded");
        };
    }
}
```

```java
@Configuration
@Profile("prod")
public class ProdSecurityConfig {
    
    @Bean
    public SecurityFilterChain prodFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf().csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            .and()
            .authorizeRequests()
                .antMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            .and()
            .formLogin()
            .and()
            .httpBasic();
        return http.build();
    }
}
```

### @Profile en componentes

```java
@Service
@Profile("dev")
public class MockEmailService implements EmailService {
    
    @Override
    public void sendEmail(String to, String subject, String body) {
        log.info("MOCK EMAIL - To: {}, Subject: {}", to, subject);
        log.debug("Body: {}", body);
        // No envía email real
    }
}
```

```java
@Service
@Profile("prod")
public class SmtpEmailService implements EmailService {
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Override
    public void sendEmail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
        log.info("Email sent to {}", to);
    }
}
```

### Expresiones en @Profile

**Negación:**
```java
@Bean
@Profile("!prod")  // Activo en todos EXCEPTO prod
public String debugMode() {
    return "DEBUG";
}
```

**OR:**
```java
@Bean
@Profile({"dev", "test"})  // Activo en dev O test
public DataSource h2DataSource() {
    return new EmbeddedDatabaseBuilder()
        .setType(EmbeddedDatabaseType.H2)
        .build();
}
```

**Expresiones complejas:**
```java
@Bean
@Profile("dev & debug")  // Activo cuando AMBOS están activos
public String devDebug() {
    return "DEV_DEBUG";
}

@Bean
@Profile("prod | staging")  // Activo en prod O staging
public String highAvailability() {
    return "HA_ENABLED";
}

@Bean
@Profile("!(prod | staging)")  // NOT (prod OR staging)
public String localMode() {
    return "LOCAL";
}
```

### Verificar perfil activo en código

```java
@Component
public class ProfileChecker {
    
    @Autowired
    private Environment env;
    
    @PostConstruct
    public void checkProfiles() {
        String[] activeProfiles = env.getActiveProfiles();
        String[] defaultProfiles = env.getDefaultProfiles();
        
        log.info("Active profiles: {}", Arrays.toString(activeProfiles));
        log.info("Default profiles: {}", Arrays.toString(defaultProfiles));
        
        if (env.acceptsProfiles(Profiles.of("dev"))) {
            log.info("Running in DEVELOPMENT mode");
        }
        
        if (env.acceptsProfiles(Profiles.of("prod"))) {
            log.warn("Running in PRODUCTION mode");
        }
    }
    
    public boolean isDevelopment() {
        return env.acceptsProfiles(Profiles.of("dev"));
    }
    
    public boolean isProduction() {
        return env.acceptsProfiles(Profiles.of("prod"));
    }
}
```

### @Profile en tests

```java
@SpringBootTest
@ActiveProfiles("test")
public class OrderServiceTest {
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private DataSource dataSource;  // Usa testDataSource
    
    @Test
    public void testOrderCreation() {
        // Test con perfil "test" activo
    }
}
```

**Múltiples perfiles en test:**
```java
@SpringBootTest
@ActiveProfiles({"test", "integration"})
public class IntegrationTest {
    // Ambos perfiles activos
}
```

---

## Mejores prácticas

### 1. Organización de propiedades

✅ **Agrupar por funcionalidad:**
```properties
# Database
spring.datasource.url=...
spring.datasource.username=...

# Server
server.port=...
server.ssl.enabled=...

# Logging
logging.level.root=...
logging.file.name=...

# Custom
app.feature.enabled=...
app.max-connections=...
```

### 2. Usar prefijos consistentes

```properties
# ✅ BIEN - prefijo consistente
app.database.url=...
app.database.username=...
app.api.url=...
app.api.timeout=...

# ❌ MAL - sin organización
database-url=...
db.username=...
apiUrl=...
timeout=...
```

### 3. Valores sensibles en variables de entorno

```properties
# ❌ MAL - contraseña en properties
spring.datasource.password=secret123

# ✅ BIEN - desde variable de entorno
spring.datasource.password=${DB_PASSWORD}
```

### 4. Valores por defecto razonables

```java
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private int maxConnections = 100;  // ✅ Valor por defecto
    private Duration timeout = Duration.ofSeconds(30);  // ✅ Timeout razonable
}
```

### 5. Validación de propiedades

```java
@Validated
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    
    @NotBlank
    @Email
    private String adminEmail;
    
    @Min(1)
    @Max(1000)
    private int maxConnections;
}
```

### 6. Documentación

```java
/**
 * Application configuration properties.
 * 
 * Prefix: {@code app}
 */
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    
    /**
     * Maximum number of concurrent connections.
     * Default: 100
     */
    private int maxConnections = 100;
}
```

### 7. Perfiles para entornos

```
dev → Desarrollo local
test → Testing automatizado
staging → Pre-producción
prod → Producción
```

### 8. No mezclar configuración en código

```java
// ❌ MAL
public class MyService {
    private static final String API_URL = "http://api.example.com";
}

// ✅ BIEN
@Service
public class MyService {
    @Value("${app.api.url}")
    private String apiUrl;
}
```

---

## Resumen

### Jerarquía de configuración (mayor a menor prioridad):

1. Command line args
2. JVM system properties
3. OS environment variables
4. `application-{profile}.properties` (external)
5. `application-{profile}.properties` (packaged)
6. `application.properties` (external)
7. `application.properties` (packaged)
8. `@PropertySource`
9. Default properties

### Formato recomendado:

| Caso de uso | Formato |
|-------------|---------|
| Configuración simple | `application.properties` |
| Configuración jerárquica | `application.yml` |
| Múltiples perfiles en un archivo | `application.yml` |
| Configuración por perfil separada | `application-{profile}.properties` |

### @ConfigurationProperties vs @Value:

| Aspecto | @ConfigurationProperties | @Value |
|---------|--------------------------|--------|
| Type-safe | ✅ Sí | ❌ No |
| Validación | ✅ Con @Validated | ❌ No |
| Reutilización | ✅ Inyectar en múltiples lugares | ❌ Duplicado |
| IDE support | ✅ Con metadata | ⚠️ Limitado |
| Flexibilidad | ✅ Objetos complejos | ⚠️ Valores simples |
| **Recomendación** | ✅ **Usar para configuración** | ⚠️ Solo casos simples |

---

*Esta guía está basada en Spring in Action 6th Edition y cubre Spring Boot 2.7+ / 3.0+.*
