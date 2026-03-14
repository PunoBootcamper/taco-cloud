# Guía Completa de Spring Security

## 5. Asegurando aplicaciones Spring

Esta guía exhaustiva cubre todos los aspectos de Spring Security, desde la configuración básica hasta técnicas avanzadas de autenticación, autorización y protección de aplicaciones web.

---

## Índice

1. [Habilitando Spring Security](#51-habilitando-spring-security)
2. [Configurando autenticación](#52-configurando-autenticación)
3. [Asegurando peticiones web](#53-asegurando-peticiones-web)
4. [Seguridad a nivel de método](#54-aplicando-seguridad-a-nivel-de-método)
5. [Conociendo al usuario](#55-conociendo-a-tu-usuario)
6. [Conceptos avanzados](#conceptos-avanzados)

---

## 5.1 Habilitando Spring Security

Spring Security es un framework potente y personalizable para autenticación y control de acceso en aplicaciones Spring.

### Características principales:

- **Autenticación**: Verificar la identidad del usuario
- **Autorización**: Determinar qué puede hacer un usuario
- **Protección contra ataques**: CSRF, Session Fixation, Clickjacking
- **Integración**: LDAP, OAuth2, SAML, JWT
- **Personalizable**: Adaptable a cualquier requisito de seguridad

### Dependencia Maven

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

### ⚡ Auto-configuración por defecto

Al añadir Spring Security, automáticamente:

1. **Todas las rutas requieren autenticación**
2. **Se genera un usuario por defecto**: `user`
3. **Contraseña aleatoria en logs**: `Using generated security password: 8e557245-73e2-4286-969a-ff57fe326336`
4. **Formulario de login** en `/login`
5. **Protección CSRF** habilitada
6. **Sesiones** manejadas automáticamente

**Ejemplo de log al iniciar:**
```
Using generated security password: 8e557245-73e2-4286-969a-ff57fe326336

This generated password is for development use only. Your security configuration must be updated before running your application in production.
```

### Configuración básica en application.properties

```properties
# Usuario y contraseña por defecto (SOLO DESARROLLO)
spring.security.user.name=admin
spring.security.user.password=secret123
spring.security.user.roles=ADMIN

# Desactivar seguridad (NO RECOMENDADO, solo para pruebas)
# spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
```

**⚠️ IMPORTANTE:** La configuración en properties es solo para desarrollo. En producción, usa configuración programática.

---

## 5.2 Configurando autenticación

La autenticación verifica **quién eres**. Spring Security soporta múltiples mecanismos de almacenamiento de credenciales.

### Arquitectura de autenticación

```
Usuario -> Formulario -> AuthenticationManager -> AuthenticationProvider
                                                          ↓
                                                   UserDetailsService
                                                          ↓
                                                      UserDetails
```

### Componentes clave:

| Componente | Responsabilidad |
|------------|-----------------|
| `UserDetails` | Representa la información del usuario |
| `UserDetailsService` | Carga datos del usuario desde un almacén |
| `PasswordEncoder` | Codifica y verifica contraseñas |
| `AuthenticationManager` | Coordina el proceso de autenticación |
| `SecurityContext` | Almacena detalles del usuario autenticado |

---

### 5.2.1 In-memory user details service

Almacena usuarios en memoria. **Solo para desarrollo/testing.**

**Ejemplo 1: Configuración básica en memoria**

```java
package com.example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder encoder) {
        UserDetails user1 = User.builder()
            .username("john")
            .password(encoder.encode("password123"))
            .roles("USER")
            .build();
        
        UserDetails user2 = User.builder()
            .username("admin")
            .password(encoder.encode("admin123"))
            .roles("USER", "ADMIN")
            .build();
        
        return new InMemoryUserDetailsManager(user1, user2);
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .authorizeRequests()
                .anyRequest().authenticated()
            .and()
                .formLogin()
            .and()
                .httpBasic()
            .and()
            .build();
    }
}
```

**Ejemplo 2: Múltiples usuarios con diferentes autoridades**

```java
@Bean
public UserDetailsService userDetailsService(PasswordEncoder encoder) {
    List<UserDetails> users = new ArrayList<>();
    
    // Usuario regular
    users.add(User.builder()
        .username("user")
        .password(encoder.encode("user123"))
        .roles("USER")
        .build());
    
    // Moderador
    users.add(User.builder()
        .username("moderator")
        .password(encoder.encode("mod123"))
        .roles("USER", "MODERATOR")
        .authorities("READ", "WRITE", "DELETE_OWN")
        .build());
    
    // Administrador
    users.add(User.builder()
        .username("admin")
        .password(encoder.encode("admin123"))
        .roles("USER", "ADMIN")
        .authorities("READ", "WRITE", "DELETE", "MANAGE_USERS")
        .build());
    
    // Usuario deshabilitado
    users.add(User.builder()
        .username("inactive")
        .password(encoder.encode("inactive123"))
        .roles("USER")
        .disabled(true)
        .build());
    
    // Usuario con cuenta expirada
    users.add(User.builder()
        .username("expired")
        .password(encoder.encode("expired123"))
        .roles("USER")
        .accountExpired(true)
        .build());
    
    return new InMemoryUserDetailsManager(users);
}
```

**Ejemplo 3: Configuración legacy (antes de Spring Security 5.7)**

```java
// ⚠️ FORMA ANTIGUA - WebSecurityConfigurerAdapter está deprecated
@Configuration
@EnableWebSecurity
public class OldSecurityConfig extends WebSecurityConfigurerAdapter {
    
    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.inMemoryAuthentication()
            .withUser("user")
                .password("{noop}password")  // {noop} = sin encriptar
                .roles("USER")
            .and()
            .withUser("admin")
                .password("{bcrypt}$2a$10$GRLdNijSQMUvl/au9ofL.eDwmoohzzS7.rmNSJZ.0FxO/BTk76klW")
                .roles("USER", "ADMIN");
    }
    
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .authorizeRequests()
                .anyRequest().authenticated()
            .and()
            .formLogin();
    }
}
```

**Ejemplo 4: Forma moderna (Spring Security 5.7+)**

```java
// ✅ FORMA MODERNA - Component-based configuration
@Configuration
@EnableWebSecurity
public class ModernSecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .anyRequest().authenticated()
            )
            .formLogin(Customizer.withDefaults())
            .httpBasic(Customizer.withDefaults());
        
        return http.build();
    }
    
    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails user = User.withDefaultPasswordEncoder()  // Solo para desarrollo
            .username("user")
            .password("password")
            .roles("USER")
            .build();
        
        return new InMemoryUserDetailsManager(user);
    }
}
```

---

### 5.2.2 Customizing user authentication

Autenticación personalizada cargando usuarios desde una base de datos.

#### Paso 1: Definir el dominio de usuario y persistencia

**Interfaz UserDetails:**

Spring Security requiere que los usuarios implementen `UserDetails`:

```java
public interface UserDetails extends Serializable {
    Collection<? extends GrantedAuthority> getAuthorities();  // Roles/permisos
    String getPassword();                                      // Contraseña
    String getUsername();                                      // Nombre de usuario
    boolean isAccountNonExpired();                            // Cuenta no expirada
    boolean isAccountNonLocked();                             // Cuenta no bloqueada
    boolean isCredentialsNonExpired();                        // Credenciales no expiradas
    boolean isEnabled();                                       // Usuario habilitado
}
```

**Ejemplo 1: Entidad User básica**

```java
package com.example.domain;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import javax.persistence.*;
import java.util.Collection;
import java.util.Collections;

@Data
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User implements UserDetails {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String username;
    
    @Column(nullable = false)
    private String password;
    
    @Column(nullable = false)
    private String fullName;
    
    private String email;
    private String phoneNumber;
    
    @Column(nullable = false)
    private boolean enabled = true;
    
    @Column(nullable = false)
    private boolean accountNonExpired = true;
    
    @Column(nullable = false)
    private boolean accountNonLocked = true;
    
    @Column(nullable = false)
    private boolean credentialsNonExpired = true;
    
    // Constructor para registro
    public User(String username, String password, String fullName, String email) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.email = email;
    }
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Por defecto, todos son USER
        return Collections.singletonList(
            new SimpleGrantedAuthority("ROLE_USER")
        );
    }
    
    @Override
    public String getPassword() {
        return password;
    }
    
    @Override
    public String getUsername() {
        return username;
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }
    
    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
```

**Ejemplo 2: User con roles múltiples (relación OneToMany)**

```java
package com.example.domain;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User implements UserDetails {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false, length = 50)
    private String username;
    
    @Column(nullable = false)
    private String password;
    
    @Column(nullable = false)
    private String fullName;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    private String phoneNumber;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;
    
    private boolean enabled = true;
    private boolean accountNonExpired = true;
    private boolean accountNonLocked = true;
    private boolean credentialsNonExpired = true;
    
    // Relación con roles
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @ToString.Exclude
    private Set<Role> roles = new HashSet<>();
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
            .map(role -> new SimpleGrantedAuthority(role.getName()))
            .collect(Collectors.toList());
    }
    
    public void addRole(Role role) {
        roles.add(role);
    }
    
    public void removeRole(Role role) {
        roles.remove(role);
    }
    
    public boolean hasRole(String roleName) {
        return roles.stream()
            .anyMatch(role -> role.getName().equals(roleName));
    }
    
    // Métodos UserDetails
    @Override
    public String getPassword() {
        return password;
    }
    
    @Override
    public String getUsername() {
        return username;
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }
    
    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
```

**Entidad Role:**

```java
package com.example.domain;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "roles")
public class Role {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String name;  // ROLE_USER, ROLE_ADMIN, ROLE_MODERATOR
    
    private String description;
    
    @ManyToMany(mappedBy = "roles")
    private Set<User> users = new HashSet<>();
    
    public Role(String name) {
        this.name = name;
    }
    
    public Role(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
```

**Ejemplo 3: User con autoridades granulares (permisos)**

```java
package com.example.domain;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import javax.persistence.*;
import java.util.*;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User implements UserDetails {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String username;
    
    @Column(nullable = false)
    private String password;
    
    private String fullName;
    private String email;
    
    // Roles
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();
    
    // Permisos adicionales a nivel de usuario
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_permissions", 
                     joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "permission")
    private Set<String> permissions = new HashSet<>();
    
    private boolean enabled = true;
    private boolean accountNonExpired = true;
    private boolean accountNonLocked = true;
    private boolean credentialsNonExpired = true;
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> authorities = new HashSet<>();
        
        // Añadir roles
        authorities.addAll(
            roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .collect(Collectors.toSet())
        );
        
        // Añadir permisos de los roles
        authorities.addAll(
            roles.stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet())
        );
        
        // Añadir permisos específicos del usuario
        authorities.addAll(
            permissions.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet())
        );
        
        return authorities;
    }
    
    // Métodos restantes de UserDetails...
    @Override public String getPassword() { return password; }
    @Override public String getUsername() { return username; }
    @Override public boolean isAccountNonExpired() { return accountNonExpired; }
    @Override public boolean isAccountNonLocked() { return accountNonLocked; }
    @Override public boolean isCredentialsNonExpired() { return credentialsNonExpired; }
    @Override public boolean isEnabled() { return enabled; }
}
```

**Role con permisos:**

```java
@Data
@Entity
@Table(name = "roles")
public class Role {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String name;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "role_permissions", 
                     joinColumns = @JoinColumn(name = "role_id"))
    @Column(name = "permission")
    private Set<String> permissions = new HashSet<>();
    
    @ManyToMany(mappedBy = "roles")
    private Set<User> users = new HashSet<>();
}
```

**Schema SQL para roles y permisos:**

```sql
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100),
    email VARCHAR(100) UNIQUE NOT NULL,
    enabled BOOLEAN DEFAULT TRUE,
    account_non_expired BOOLEAN DEFAULT TRUE,
    account_non_locked BOOLEAN DEFAULT TRUE,
    credentials_non_expired BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP NULL
);

CREATE TABLE roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,
    description VARCHAR(255)
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

CREATE TABLE role_permissions (
    role_id BIGINT NOT NULL,
    permission VARCHAR(100) NOT NULL,
    PRIMARY KEY (role_id, permission),
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

CREATE TABLE user_permissions (
    user_id BIGINT NOT NULL,
    permission VARCHAR(100) NOT NULL,
    PRIMARY KEY (user_id, permission),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Datos iniciales
INSERT INTO roles (name, description) VALUES
('ROLE_USER', 'Usuario regular'),
('ROLE_MODERATOR', 'Moderador con permisos adicionales'),
('ROLE_ADMIN', 'Administrador con acceso completo');

INSERT INTO role_permissions (role_id, permission) VALUES
(1, 'READ'),                    -- USER puede leer
(2, 'READ'),                    -- MODERATOR puede leer
(2, 'WRITE'),                   -- MODERATOR puede escribir
(2, 'DELETE_OWN'),              -- MODERATOR puede borrar sus propias publicaciones
(3, 'READ'),                    -- ADMIN puede leer
(3, 'WRITE'),                   -- ADMIN puede escribir
(3, 'DELETE'),                  -- ADMIN puede borrar cualquier cosa
(3, 'MANAGE_USERS');            -- ADMIN puede gestionar usuarios
```

---

#### Paso 2: Crear un servicio de detalles de usuario

**Repositorio:**

```java
package com.example.repository;

import com.example.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
```

**UserDetailsService - Ejemplo 1: Básico**

```java
package com.example.config;

import com.example.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class UserDetailsServiceConfig {
    
    private final UserRepository userRepository;
    
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            log.debug("Attempting to load user: {}", username);
            
            return userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("User not found: {}", username);
                    return new UsernameNotFoundException(
                        "User '" + username + "' not found"
                    );
                });
        };
    }
}
```

**UserDetailsService - Ejemplo 2: Con servicio dedicado**

```java
package com.example.service;

import com.example.domain.User;
import com.example.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    
    private final UserRepository userRepository;
    
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user by username: {}", username);
        
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException(
                "User not found with username: " + username
            ));
        
        // Verificar si la cuenta está habilitada
        if (!user.isEnabled()) {
            log.warn("User account is disabled: {}", username);
            throw new DisabledException("User account is disabled");
        }
        
        // Verificar si la cuenta está bloqueada
        if (!user.isAccountNonLocked()) {
            log.warn("User account is locked: {}", username);
            throw new LockedException("User account is locked");
        }
        
        // Verificar si la cuenta ha expirado
        if (!user.isAccountNonExpired()) {
            log.warn("User account has expired: {}", username);
            throw new AccountExpiredException("User account has expired");
        }
        
        // Verificar si las credenciales han expirado
        if (!user.isCredentialsNonExpired()) {
            log.warn("User credentials have expired: {}", username);
            throw new CredentialsExpiredException("User credentials have expired");
        }
        
        log.info("User loaded successfully: {} with {} authorities", 
                 username, user.getAuthorities().size());
        
        return user;
    }
    
    // Método adicional para actualizar último login
    @Transactional
    public void updateLastLogin(String username) {
        userRepository.findByUsername(username)
            .ifPresent(user -> {
                user.setLastLogin(LocalDateTime.now());
                userRepository.save(user);
                log.debug("Updated last login for user: {}", username);
            });
    }
}
```

**UserDetailsService - Ejemplo 3: Con caché**

```java
package com.example.service;

import com.example.domain.User;
import com.example.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CachedUserDetailsService implements UserDetailsService {
    
    private final UserRepository userRepository;
    
    @Override
    @Cacheable(value = "userCache", key = "#username")
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user from database: {}", username);
        
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException(
                "User not found: " + username
            ));
    }
}
```

**Configuración de caché:**

```java
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(Arrays.asList(
            new ConcurrentMapCache("userCache")
        ));
        return cacheManager;
    }
}
```

---

#### Paso 3: Registrando usuarios

**DTO para registro:**

```java
package com.example.dto;

import lombok.Data;
import javax.validation.constraints.*;

@Data
public class RegistrationForm {
    
    @NotBlank(message = "Username is required")
    @Size(min = 4, max = 50, message = "Username must be between 4 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can only contain letters, numbers and underscores")
    private String username;
    
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
    
    @NotBlank(message = "Password confirmation is required")
    private String confirmPassword;
    
    @NotBlank(message = "Full name is required")
    private String fullName;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;
    
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Phone number must be valid")
    private String phoneNumber;
    
    // Validación personalizada
    public boolean passwordsMatch() {
        return password != null && password.equals(confirmPassword);
    }
}
```

**Servicio de registro:**

```java
package com.example.service;

import com.example.domain.Role;
import com.example.domain.User;
import com.example.dto.RegistrationForm;
import com.example.repository.RoleRepository;
import com.example.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserRegistrationService {
    
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Transactional
    public User registerUser(RegistrationForm form) {
        // Validar que las contraseñas coincidan
        if (!form.passwordsMatch()) {
            throw new IllegalArgumentException("Passwords do not match");
        }
        
        // Verificar que el username no exista
        if (userRepository.existsByUsername(form.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }
        
        // Verificar que el email no exista
        if (userRepository.existsByEmail(form.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }
        
        // Crear nuevo usuario
        User user = new User();
        user.setUsername(form.getUsername());
        user.setPassword(passwordEncoder.encode(form.getPassword()));
        user.setFullName(form.getFullName());
        user.setEmail(form.getEmail());
        user.setPhoneNumber(form.getPhoneNumber());
        user.setCreatedAt(LocalDateTime.now());
        user.setEnabled(true);
        
        // Asignar rol USER por defecto
        Role userRole = roleRepository.findByName("ROLE_USER")
            .orElseThrow(() -> new RuntimeException("Default role not found"));
        user.addRole(userRole);
        
        // Guardar usuario
        User savedUser = userRepository.save(user);
        
        log.info("New user registered: {}", savedUser.getUsername());
        
        return savedUser;
    }
    
    @Transactional(readOnly = true)
    public boolean usernameExists(String username) {
        return userRepository.existsByUsername(username);
    }
    
    @Transactional(readOnly = true)
    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }
}
```

**Controlador de registro:**

```java
package com.example.controller;

import com.example.dto.RegistrationForm;
import com.example.service.UserRegistrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Slf4j
@Controller
@RequestMapping("/register")
@RequiredArgsConstructor
public class RegistrationController {
    
    private final UserRegistrationService registrationService;
    
    @GetMapping
    public String showRegistrationForm(Model model) {
        model.addAttribute("registrationForm", new RegistrationForm());
        return "registration";
    }
    
    @PostMapping
    public String processRegistration(
            @Valid @ModelAttribute("registrationForm") RegistrationForm form,
            Errors errors,
            Model model) {
        
        // Validar errores de campo
        if (errors.hasErrors()) {
            return "registration";
        }
        
        // Validar que las contraseñas coincidan
        if (!form.passwordsMatch()) {
            model.addAttribute("error", "Passwords do not match");
            return "registration";
        }
        
        try {
            // Registrar usuario
            registrationService.registerUser(form);
            
            log.info("User registered successfully: {}", form.getUsername());
            
            // Redirigir a login con mensaje de éxito
            return "redirect:/login?registered";
            
        } catch (IllegalArgumentException e) {
            log.warn("Registration failed: {}", e.getMessage());
            model.addAttribute("error", e.getMessage());
            return "registration";
        }
    }
    
    // API endpoint para verificar disponibilidad de username (AJAX)
    @GetMapping("/check-username")
    @ResponseBody
    public boolean checkUsername(@RequestParam String username) {
        return !registrationService.usernameExists(username);
    }
    
    // API endpoint para verificar disponibilidad de email (AJAX)
    @GetMapping("/check-email")
    @ResponseBody
    public boolean checkEmail(@RequestParam String email) {
        return !registrationService.emailExists(email);
    }
}
```

**Vista de registro (registration.html):**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Register - Taco Cloud</title>
    <link rel="stylesheet" th:href="@{/css/styles.css}" />
</head>
<body>
    <div class="container">
        <h1>Create Account</h1>
        
        <!-- Mensaje de error general -->
        <div th:if="${error}" class="alert alert-danger" th:text="${error}"></div>
        
        <form method="POST" th:action="@{/register}" th:object="${registrationForm}">
            
            <!-- Username -->
            <div class="form-group">
                <label for="username">Username:</label>
                <input type="text" 
                       id="username" 
                       th:field="*{username}"
                       th:errorclass="field-error"
                       required />
                <span class="error" 
                      th:if="${#fields.hasErrors('username')}" 
                      th:errors="*{username}">Username Error</span>
            </div>
            
            <!-- Password -->
            <div class="form-group">
                <label for="password">Password:</label>
                <input type="password" 
                       id="password" 
                       th:field="*{password}"
                       th:errorclass="field-error"
                       required />
                <span class="error" 
                      th:if="${#fields.hasErrors('password')}" 
                      th:errors="*{password}">Password Error</span>
            </div>
            
            <!-- Confirm Password -->
            <div class="form-group">
                <label for="confirmPassword">Confirm Password:</label>
                <input type="password" 
                       id="confirmPassword" 
                       th:field="*{confirmPassword}"
                       th:errorclass="field-error"
                       required />
                <span class="error" 
                      th:if="${#fields.hasErrors('confirmPassword')}" 
                      th:errors="*{confirmPassword}">Confirm Password Error</span>
            </div>
            
            <!-- Full Name -->
            <div class="form-group">
                <label for="fullName">Full Name:</label>
                <input type="text" 
                       id="fullName" 
                       th:field="*{fullName}"
                       th:errorclass="field-error"
                       required />
                <span class="error" 
                      th:if="${#fields.hasErrors('fullName')}" 
                      th:errors="*{fullName}">Name Error</span>
            </div>
            
            <!-- Email -->
            <div class="form-group">
                <label for="email">Email:</label>
                <input type="email" 
                       id="email" 
                       th:field="*{email}"
                       th:errorclass="field-error"
                       required />
                <span class="error" 
                      th:if="${#fields.hasErrors('email')}" 
                      th:errors="*{email}">Email Error</span>
            </div>
            
            <!-- Phone Number -->
            <div class="form-group">
                <label for="phoneNumber">Phone Number:</label>
                <input type="tel" 
                       id="phoneNumber" 
                       th:field="*{phoneNumber}"
                       th:errorclass="field-error" />
                <span class="error" 
                      th:if="${#fields.hasErrors('phoneNumber')}" 
                      th:errors="*{phoneNumber}">Phone Error</span>
            </div>
            
            <button type="submit" class="btn btn-primary">Register</button>
        </form>
        
        <p>Already have an account? <a th:href="@{/login}">Login here</a></p>
    </div>
    
    <script>
        // Validación de username en tiempo real (AJAX)
        document.getElementById('username').addEventListener('blur', function() {
            const username = this.value;
            if (username.length >= 4) {
                fetch(`/register/check-username?username=${username}`)
                    .then(response => response.json())
                    .then(available => {
                        if (!available) {
                            alert('Username is already taken');
                        }
                    });
            }
        });
    </script>
</body>
</html>
```

---

### Password Encoders

Spring Security soporta múltiples algoritmos de codificación de contraseñas.

**Tipos de PasswordEncoder:**

| Encoder | Descripción | Seguridad | Uso |
|---------|-------------|-----------|-----|
| `BCryptPasswordEncoder` | Bcrypt con salt | ⭐⭐⭐⭐⭐ | **Recomendado** |
| `Argon2PasswordEncoder` | Argon2 (ganador competencia) | ⭐⭐⭐⭐⭐ | Muy seguro |
| `Pbkdf2PasswordEncoder` | PBKDF2 | ⭐⭐⭐⭐ | Bueno |
| `SCryptPasswordEncoder` | SCrypt | ⭐⭐⭐⭐ | Bueno |
| `NoOpPasswordEncoder` | Sin codificación | ❌ | **NUNCA en producción** |

**Ejemplo 1: BCryptPasswordEncoder (recomendado)**

```java
@Configuration
public class PasswordEncoderConfig {
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        // Strength: 10 (por defecto), rango 4-31
        // Mayor = más seguro pero más lento
        return new BCryptPasswordEncoder(12);
    }
}
```

**Ejemplo 2: Argon2PasswordEncoder**

```java
@Bean
public PasswordEncoder passwordEncoder() {
    // Parámetros: saltLength, hashLength, parallelism, memory, iterations
    return new Argon2PasswordEncoder(16, 32, 1, 65536, 3);
}
```

**Ejemplo 3: DelegatingPasswordEncoder (múltiples encoders)**

```java
@Bean
public PasswordEncoder passwordEncoder() {
    String encodingId = "bcrypt";
    
    Map<String, PasswordEncoder> encoders = new HashMap<>();
    encoders.put(encodingId, new BCryptPasswordEncoder());
    encoders.put("pbkdf2", new Pbkdf2PasswordEncoder());
    encoders.put("scrypt", new SCryptPasswordEncoder());
    encoders.put("argon2", new Argon2PasswordEncoder());
    encoders.put("sha256", new StandardPasswordEncoder()); // Legacy
    
    return new DelegatingPasswordEncoder(encodingId, encoders);
}
```

**Formato de contraseña con DelegatingPasswordEncoder:**
```
{bcrypt}$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG
{pbkdf2}5d923b44a6d129f3ddf3e3c8d29412723dcbde72445e8ef6bf3b508fbf17fa4ed4d6b99ca763d8dc
{sha256}97cde38028ad898ebc02e690819fa220e88c62e0699403e94fff291cfffaf8410849f27605abcbc0
```

**Uso del PasswordEncoder:**

```java
@Service
@RequiredArgsConstructor
public class PasswordService {
    
    private final PasswordEncoder passwordEncoder;
    
    public String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }
    
    public boolean matches(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
    
    public boolean needsUpgrade(String encodedPassword) {
        return passwordEncoder.upgradeEncoding(encodedPassword);
    }
}
```

---

## 5.3 Asegurando peticiones web

La autorización determina **qué puedes hacer**. Spring Security permite configurar reglas de acceso a nivel de URL.

### 5.3.1 Asegurando peticiones

**Arquitectura de autorización:**

```
Petición HTTP -> FilterChain -> SecurityFilterChain -> HttpSecurity
                                                            ↓
                                                    authorizeRequests()
                                                            ↓
                                                    AccessDecisionManager
                                                            ↓
                                                    Permitir/Denegar
```

**Ejemplo 1: Configuración básica**

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeRequests(authorize -> authorize
                // Rutas públicas
                .antMatchers("/", "/home", "/about").permitAll()
                
                // Recursos estáticos
                .antMatchers("/css/**", "/js/**", "/images/**").permitAll()
                
                // Requiere autenticación
                .antMatchers("/profile/**").authenticated()
                
                // Requiere rol específico
                .antMatchers("/admin/**").hasRole("ADMIN")
                
                // Cualquier otra petición
                .anyRequest().authenticated()
            )
            .formLogin(Customizer.withDefaults());
        
        return http.build();
    }
}
```

**Ejemplo 2: Configuración detallada con múltiples roles**

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    return http
        .authorizeRequests()
            // ===== RUTAS PÚBLICAS =====
            .antMatchers("/", "/home", "/about", "/contact").permitAll()
            .antMatchers("/api/public/**").permitAll()
            
            // ===== RECURSOS ESTÁTICOS =====
            .antMatchers("/css/**", "/js/**", "/images/**", "/fonts/**").permitAll()
            .antMatchers("/favicon.ico", "/robots.txt").permitAll()
            
            // ===== AUTENTICACIÓN =====
            .antMatchers("/login", "/register", "/forgot-password").permitAll()
            
            // ===== H2 CONSOLE (solo desarrollo) =====
            .antMatchers("/h2-console/**").permitAll()
            
            // ===== APIs REST =====
            .antMatchers(HttpMethod.GET, "/api/products/**").permitAll()
            .antMatchers(HttpMethod.POST, "/api/products/**").hasRole("ADMIN")
            .antMatchers(HttpMethod.PUT, "/api/products/**").hasRole("ADMIN")
            .antMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("ADMIN")
            
            // ===== ÁREA DE USUARIO =====
            .antMatchers("/profile/**").hasRole("USER")
            .antMatchers("/orders/**").hasRole("USER")
            .antMatchers("/cart/**").hasRole("USER")
            
            // ===== ÁREA DE MODERADOR =====
            .antMatchers("/moderate/**").hasAnyRole("MODERATOR", "ADMIN")
            .antMatchers("/reports/**").hasAnyRole("MODERATOR", "ADMIN")
            
            // ===== ÁREA DE ADMINISTRADOR =====
            .antMatchers("/admin/**").hasRole("ADMIN")
            .antMatchers("/users/manage/**").hasRole("ADMIN")
            
            // ===== PERMISOS ESPECÍFICOS =====
            .antMatchers("/api/delete/**").hasAuthority("DELETE")
            .antMatchers("/api/write/**").hasAnyAuthority("WRITE", "ADMIN")
            
            // ===== POR DEFECTO =====
            .anyRequest().authenticated()
        .and()
            .formLogin()
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard")
                .permitAll()
        .and()
            .logout()
                .logoutSuccessUrl("/")
                .permitAll()
        .and()
            // CSRF
            .csrf()
                .ignoringAntMatchers("/h2-console/**", "/api/**")
        .and()
            // Headers para H2 Console
            .headers()
                .frameOptions().sameOrigin()
        .and()
        .build();
}
```

**Ejemplo 3: Configuración con SpEL (Spring Expression Language)**

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    return http
        .authorizeRequests()
            // Acceso solo si NO está autenticado
            .antMatchers("/login", "/register")
                .access("!isAuthenticated()")
            
            // Acceso si está autenticado
            .antMatchers("/profile/**")
                .access("isAuthenticated()")
            
            // Acceso por rol
            .antMatchers("/admin/**")
                .access("hasRole('ADMIN')")
            
            // Múltiples condiciones
            .antMatchers("/moderate/**")
                .access("hasRole('MODERATOR') or hasRole('ADMIN')")
            
            // Condiciones complejas
            .antMatchers("/edit/**")
                .access("hasRole('USER') and !hasRole('GUEST')")
            
            // Usando expresiones personalizadas
            .antMatchers("/premium/**")
                .access("@securityService.isPremiumUser(authentication)")
            
            .anyRequest().authenticated()
        .and()
            .formLogin()
        .and()
        .build();
}
```

**Servicio personalizado para SpEL:**

```java
@Service("securityService")
public class SecurityService {
    
    public boolean isPremiumUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        
        User user = (User) authentication.getPrincipal();
        // Lógica personalizada
        return user.getAuthorities().stream()
            .anyMatch(auth -> auth.getAuthority().equals("ROLE_PREMIUM"));
    }
    
    public boolean canAccessResource(Authentication authentication, Long resourceId) {
        // Verificar si el usuario es dueño del recurso o es admin
        User user = (User) authentication.getPrincipal();
        return user.hasRole("ROLE_ADMIN") || 
               resourceBelongsToUser(resourceId, user.getId());
    }
    
    private boolean resourceBelongsToUser(Long resourceId, Long userId) {
        // Implementar lógica de verificación
        return true;
    }
}
```

**Ejemplo 4: Configuración por métodos HTTP**

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    return http
        .authorizeRequests()
            // GET - público
            .antMatchers(HttpMethod.GET, "/api/products").permitAll()
            
            // POST - solo ADMIN
            .antMatchers(HttpMethod.POST, "/api/products").hasRole("ADMIN")
            
            // PUT - solo ADMIN
            .antMatchers(HttpMethod.PUT, "/api/products/**").hasRole("ADMIN")
            
            // PATCH - ADMIN o USER (dueño)
            .antMatchers(HttpMethod.PATCH, "/api/products/**")
                .hasAnyRole("ADMIN", "USER")
            
            // DELETE - solo ADMIN
            .antMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("ADMIN")
            
            // HEAD y OPTIONS - público
            .antMatchers(HttpMethod.HEAD, "/**").permitAll()
            .antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
            
            .anyRequest().authenticated()
        .and()
            .httpBasic()  // Para APIs REST
        .and()
        .build();
}
```

**Métodos de configuración disponibles:**

| Método | Descripción | Ejemplo |
|--------|-------------|---------|
| `permitAll()` | Permite acceso sin autenticación | `.antMatchers("/public/**").permitAll()` |
| `denyAll()` | Niega todo acceso | `.antMatchers("/forbidden/**").denyAll()` |
| `authenticated()` | Requiere autenticación | `.antMatchers("/profile/**").authenticated()` |
| `anonymous()` | Solo usuarios anónimos | `.antMatchers("/guest/**").anonymous()` |
| `hasRole(role)` | Requiere rol específico | `.antMatchers("/admin/**").hasRole("ADMIN")` |
| `hasAnyRole(roles...)` | Requiere alguno de los roles | `.hasAnyRole("USER", "ADMIN")` |
| `hasAuthority(auth)` | Requiere autoridad específica | `.hasAuthority("WRITE")` |
| `hasAnyAuthority(auths...)` | Requiere alguna autoridad | `.hasAnyAuthority("READ", "WRITE")` |
| `access(spel)` | Expresión SpEL personalizada | `.access("hasRole('ADMIN')")` |
| `hasIpAddress(ip)` | Requiere IP específica | `.hasIpAddress("192.168.1.1")` |

**Ejemplo 5: Configuración con IP y tiempo**

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    return http
        .authorizeRequests()
            // Solo desde red local
            .antMatchers("/internal/**")
                .access("hasIpAddress('192.168.1.0/24') or hasIpAddress('127.0.0.1')")
            
            // Expresión compleja con IP y rol
            .antMatchers("/admin/emergency/**")
                .access("hasRole('ADMIN') and hasIpAddress('10.0.0.0/8')")
            
            .anyRequest().authenticated()
        .and()
        .build();
}
```

---

### 5.3.2 Creando una página de login personalizada

Por defecto, Spring Security proporciona un formulario de login básico. Puedes personalizarlo completamente.

**Configuración del login personalizado:**

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    return http
        .authorizeRequests()
            .antMatchers("/login", "/css/**", "/js/**").permitAll()
            .anyRequest().authenticated()
        .and()
            .formLogin()
                // Página de login personalizada
                .loginPage("/login")
                
                // URL donde se procesa el login (POST)
                .loginProcessingUrl("/authenticate")
                
                // Parámetros del formulario
                .usernameParameter("username")
                .passwordParameter("password")
                
                // Redirección en caso de éxito
                .defaultSuccessUrl("/dashboard", true)
                
                // Handler de éxito personalizado
                // .successHandler(authenticationSuccessHandler())
                
                // Redirección en caso de fallo
                .failureUrl("/login?error=true")
                
                // Handler de fallo personalizado
                // .failureHandler(authenticationFailureHandler())
                
                .permitAll()
        .and()
            .logout()
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
        .and()
        .build();
}
```

**Controlador de login:**

```java
@Controller
public class LoginController {
    
    @GetMapping("/login")
    public String login(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            @RequestParam(value = "registered", required = false) String registered,
            Model model) {
        
        if (error != null) {
            model.addAttribute("error", "Invalid username or password");
        }
        
        if (logout != null) {
            model.addAttribute("message", "You have been logged out successfully");
        }
        
        if (registered != null) {
            model.addAttribute("message", "Registration successful! Please login");
        }
        
        return "login";
    }
}
```

**Vista de login (login.html):**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Login - Taco Cloud</title>
    <link rel="stylesheet" th:href="@{/css/styles.css}" />
    <style>
        .login-container {
            max-width: 400px;
            margin: 100px auto;
            padding: 30px;
            border: 1px solid #ddd;
            border-radius: 8px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
        }
        .form-group {
            margin-bottom: 20px;
        }
        .form-group label {
            display: block;
            margin-bottom: 5px;
            font-weight: bold;
        }
        .form-group input {
            width: 100%;
            padding: 10px;
            border: 1px solid #ddd;
            border-radius: 4px;
        }
        .btn {
            width: 100%;
            padding: 12px;
            background-color: #007bff;
            color: white;
            border: none;
            border-radius: 4px;
            cursor: pointer;
            font-size: 16px;
        }
        .btn:hover {
            background-color: #0056b3;
        }
        .alert {
            padding: 12px;
            margin-bottom: 20px;
            border-radius: 4px;
        }
        .alert-danger {
            background-color: #f8d7da;
            color: #721c24;
            border: 1px solid #f5c6cb;
        }
        .alert-success {
            background-color: #d4edda;
            color: #155724;
            border: 1px solid #c3e6cb;
        }
        .links {
            margin-top: 20px;
            text-align: center;
        }
        .links a {
            color: #007bff;
            text-decoration: none;
        }
    </style>
</head>
<body>
    <div class="login-container">
        <h2>Login to Taco Cloud</h2>
        
        <!-- Mensaje de error -->
        <div th:if="${error}" class="alert alert-danger" th:text="${error}"></div>
        
        <!-- Mensaje de éxito -->
        <div th:if="${message}" class="alert alert-success" th:text="${message}"></div>
        
        <!-- Formulario de login -->
        <form method="POST" th:action="@{/authenticate}">
            <div class="form-group">
                <label for="username">Username:</label>
                <input type="text" 
                       id="username" 
                       name="username" 
                       required 
                       autofocus 
                       placeholder="Enter your username" />
            </div>
            
            <div class="form-group">
                <label for="password">Password:</label>
                <input type="password" 
                       id="password" 
                       name="password" 
                       required 
                       placeholder="Enter your password" />
            </div>
            
            <div class="form-group">
                <label>
                    <input type="checkbox" name="remember-me" /> Remember me
                </label>
            </div>
            
            <button type="submit" class="btn">Login</button>
        </form>
        
        <div class="links">
            <p>Don't have an account? <a th:href="@{/register}">Register here</a></p>
            <p><a th:href="@{/forgot-password}">Forgot password?</a></p>
        </div>
    </div>
</body>
</html>
```

**Success Handler personalizado:**

```java
@Component
public class CustomAuthenticationSuccessHandler 
        implements AuthenticationSuccessHandler {
    
    @Autowired
    private CustomUserDetailsService userDetailsService;
    
    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {
        
        // Registrar último login
        String username = authentication.getName();
        userDetailsService.updateLastLogin(username);
        
        // Obtener usuario
        User user = (User) authentication.getPrincipal();
        
        // Log de auditoría
        log.info("User {} logged in successfully from IP: {}", 
                 username, request.getRemoteAddr());
        
        // Redirección basada en rol
        if (user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            response.sendRedirect(request.getContextPath() + "/admin/dashboard");
        } else {
            response.sendRedirect(request.getContextPath() + "/dashboard");
        }
    }
}
```

**Failure Handler personalizado:**

```java
@Component
public class CustomAuthenticationFailureHandler 
        implements AuthenticationFailureHandler {
    
    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) throws IOException {
        
        String errorMessage;
        
        if (exception instanceof BadCredentialsException) {
            errorMessage = "Invalid username or password";
        } else if (exception instanceof DisabledException) {
            errorMessage = "Account is disabled";
        } else if (exception instanceof LockedException) {
            errorMessage = "Account is locked";
        } else if (exception instanceof AccountExpiredException) {
            errorMessage = "Account has expired";
        } else if (exception instanceof CredentialsExpiredException) {
            errorMessage = "Password has expired";
        } else {
            errorMessage = "Authentication failed";
        }
        
        log.warn("Authentication failed for user {} from IP {}: {}", 
                 request.getParameter("username"),
                 request.getRemoteAddr(),
                 errorMessage);
        
        response.sendRedirect(request.getContextPath() + 
                             "/login?error=" + URLEncoder.encode(errorMessage, "UTF-8"));
    }
}
```

**Configuración con handlers:**

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http, 
                                       CustomAuthenticationSuccessHandler successHandler,
                                       CustomAuthenticationFailureHandler failureHandler) 
        throws Exception {
    return http
        .authorizeRequests()
            .anyRequest().authenticated()
        .and()
            .formLogin()
                .loginPage("/login")
                .successHandler(successHandler)
                .failureHandler(failureHandler)
                .permitAll()
        .and()
        .build();
}
```

**Remember Me:**

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    return http
        .authorizeRequests()
            .anyRequest().authenticated()
        .and()
            .formLogin()
                .loginPage("/login")
                .permitAll()
        .and()
            .rememberMe()
                .key("uniqueAndSecretKey")  // Clave para generar token
                .tokenValiditySeconds(86400)  // 1 día
                .rememberMeParameter("remember-me")  // Nombre del parámetro
                .rememberMeCookieName("remember-me-cookie")
                .userDetailsService(userDetailsService())
        .and()
        .build();
}
```

---

### 5.3.3 Habilitando autenticación de terceros (OAuth2)

Spring Security soporta OAuth2 para autenticación con proveedores externos como Google, Facebook, GitHub, etc.

**Dependencia:**

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-client</artifactId>
</dependency>
```

**Configuración en application.yml:**

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          # Google
          google:
            client-id: your-google-client-id
            client-secret: your-google-client-secret
            scope:
              - email
              - profile
          
          # Facebook
          facebook:
            client-id: your-facebook-client-id
            client-secret: your-facebook-client-secret
            scope:
              - email
              - public_profile
          
          # GitHub
          github:
            client-id: your-github-client-id
            client-secret: your-github-client-secret
            scope:
              - user:email
              - read:user
          
          # Custom OAuth2 Provider
          custom:
            client-id: your-custom-client-id
            client-secret: your-custom-client-secret
            client-name: Custom OAuth2
            authorization-grant-type: authorization_code
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
            scope:
              - openid
              - profile
              - email
        
        provider:
          custom:
            authorization-uri: https://provider.com/oauth/authorize
            token-uri: https://provider.com/oauth/token
            user-info-uri: https://provider.com/oauth/userinfo
            user-name-attribute: sub
```

**Configuración de seguridad con OAuth2:**

```java
@Configuration
@EnableWebSecurity
public class OAuth2SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .authorizeRequests()
                .antMatchers("/", "/login", "/oauth2/**").permitAll()
                .anyRequest().authenticated()
            .and()
                .oauth2Login()
                    .loginPage("/login")
                    .defaultSuccessUrl("/dashboard")
                    // Handler personalizado para OAuth2
                    .successHandler(oAuth2AuthenticationSuccessHandler())
                    // Servicio personalizado para cargar/crear usuario
                    .userInfoEndpoint()
                        .userService(customOAuth2UserService())
            .and()
            .and()
            .build();
    }
    
    @Bean
    public OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler() {
        return new OAuth2AuthenticationSuccessHandler();
    }
    
    @Bean
    public CustomOAuth2UserService customOAuth2UserService() {
        return new CustomOAuth2UserService();
    }
}
```

**Servicio personalizado de OAuth2:**

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);
        
        try {
            return processOAuth2User(userRequest, oauth2User);
        } catch (Exception ex) {
            throw new OAuth2AuthenticationException("Error processing OAuth2 user");
        }
    }
    
    private OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oauth2User) {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");
        
        log.info("OAuth2 login from {}: email={}, name={}", registrationId, email, name);
        
        // Buscar usuario existente o crear nuevo
        User user = userRepository.findByEmail(email)
            .orElseGet(() -> {
                User newUser = new User();
                newUser.setUsername(email);
                newUser.setEmail(email);
                newUser.setFullName(name);
                newUser.setPassword(""); // No password for OAuth2 users
                newUser.setEnabled(true);
                
                // Asignar rol USER por defecto
                Role userRole = roleRepository.findByName("ROLE_USER")
                    .orElseThrow(() -> new RuntimeException("Default role not found"));
                newUser.addRole(userRole);
                
                return userRepository.save(newUser);
            });
        
        // Actualizar último login
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);
        
        return new CustomOAuth2User(user, oauth2User.getAttributes());
    }
}
```

**Clase CustomOAuth2User:**

```java
public class CustomOAuth2User implements OAuth2User, UserDetails {
    
    private User user;
    private Map<String, Object> attributes;
    
    public CustomOAuth2User(User user, Map<String, Object> attributes) {
        this.user = user;
        this.attributes = attributes;
    }
    
    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return user.getAuthorities();
    }
    
    @Override
    public String getName() {
        return user.getUsername();
    }
    
    // UserDetails methods
    @Override public String getPassword() { return user.getPassword(); }
    @Override public String getUsername() { return user.getUsername(); }
    @Override public boolean isAccountNonExpired() { return user.isAccountNonExpired(); }
    @Override public boolean isAccountNonLocked() { return user.isAccountNonLocked(); }
    @Override public boolean isCredentialsNonExpired() { return user.isCredentialsNonExpired(); }
    @Override public boolean isEnabled() { return user.isEnabled(); }
    
    public User getUser() {
        return user;
    }
}
```

**Vista de login con OAuth2:**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Login</title>
    <link rel="stylesheet" th:href="@{/css/styles.css}" />
</head>
<body>
    <div class="login-container">
        <h2>Login</h2>
        
        <!-- Login tradicional -->
        <form method="POST" th:action="@{/authenticate}">
            <div class="form-group">
                <input type="text" name="username" placeholder="Username" required />
            </div>
            <div class="form-group">
                <input type="password" name="password" placeholder="Password" required />
            </div>
            <button type="submit" class="btn">Login</button>
        </form>
        
        <div class="separator">
            <span>OR</span>
        </div>
        
        <!-- Login con OAuth2 -->
        <div class="oauth2-buttons">
            <a th:href="@{/oauth2/authorization/google}" class="btn btn-google">
                <img src="/images/google-icon.png" alt="Google" />
                Login with Google
            </a>
            
            <a th:href="@{/oauth2/authorization/facebook}" class="btn btn-facebook">
                <img src="/images/facebook-icon.png" alt="Facebook" />
                Login with Facebook
            </a>
            
            <a th:href="@{/oauth2/authorization/github}" class="btn btn-github">
                <img src="/images/github-icon.png" alt="GitHub" />
                Login with GitHub
            </a>
        </div>
        
        <div class="links">
            <p>Don't have an account? <a th:href="@{/register}">Register</a></p>
        </div>
    </div>
</body>
</html>
```

---

### 5.3.4 Previniendo ataques de falsificación de peticiones entre sitios (CSRF)

CSRF (Cross-Site Request Forgery) es un ataque donde un sitio malicioso envía peticiones no autorizadas a tu aplicación en nombre de un usuario autenticado.

**¿Cómo funciona Spring Security contra CSRF?**

1. Genera un token CSRF único por sesión
2. Incluye el token en formularios y peticiones AJAX
3. Verifica el token en peticiones POST/PUT/DELETE/PATCH

**Por defecto, CSRF está HABILITADO en Spring Security.**

**Ejemplo 1: Configuración por defecto**

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    return http
        .authorizeRequests()
            .anyRequest().authenticated()
        .and()
            .formLogin()
        .and()
            // CSRF habilitado por defecto
            .csrf()  // Puedes omitir esta línea
        .and()
        .build();
}
```

**Ejemplo 2: Deshabilitar CSRF (NO RECOMENDADO para producción)**

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    return http
        .authorizeRequests()
            .anyRequest().authenticated()
        .and()
            .csrf().disable()  // ⚠️ Solo para testing
        .and()
        .build();
}
```

**Ejemplo 3: Ignorar CSRF en rutas específicas**

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    return http
        .authorizeRequests()
            .anyRequest().authenticated()
        .and()
            .csrf()
                // Ignorar CSRF en APIs REST
                .ignoringAntMatchers("/api/**")
                
                // Ignorar H2 Console (desarrollo)
                .ignoringAntMatchers("/h2-console/**")
                
                // Ignorar webhooks
                .ignoringAntMatchers("/webhooks/**")
        .and()
        .build();
}
```

**Uso en formularios Thymeleaf:**

Thymeleaf incluye automáticamente el token CSRF:

```html
<!-- Token incluido automáticamente -->
<form method="POST" th:action="@{/products}">
    <input type="text" name="name" />
    <button type="submit">Save</button>
</form>

<!-- Renderiza como: -->
<form method="POST" action="/products">
    <input type="text" name="name" />
    <input type="hidden" name="_csrf" value="4bfd1575-3ad1-4d21-96c7-4ef2d9f86721"/>
    <button type="submit">Save</button>
</form>
```

**Uso manual del token CSRF:**

```html
<form method="POST" action="/products">
    <input type="text" name="name" />
    
    <!-- Incluir token manualmente -->
    <input type="hidden" 
           th:name="${_csrf.parameterName}" 
           th:value="${_csrf.token}" />
    
    <button type="submit">Save</button>
</form>
```

**CSRF con AJAX (JavaScript):**

```html
<script th:inline="javascript">
    /*<![CDATA[*/
    
    // Obtener token del meta tag
    var token = /*[[${_csrf.token}]]*/ '';
    var header = /*[[${_csrf.headerName}]]*/ '';
    
    // Opción 1: Usar fetch con header
    fetch('/api/products', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            [header]: token
        },
        body: JSON.stringify({name: 'Product 1'})
    });
    
    // Opción 2: Usar jQuery
    $(function() {
        $(document).ajaxSend(function(e, xhr, options) {
            xhr.setRequestHeader(header, token);
        });
        
        $.post('/api/products', {name: 'Product 1'});
    });
    
    /*]]>*/
</script>
```

**Meta tags para CSRF (en layout base):**

```html
<head>
    <meta th:name="_csrf" th:content="${_csrf.token}"/>
    <meta th:name="_csrf_header" th:content="${_csrf.headerName}"/>
</head>
```

**JavaScript global para CSRF:**

```javascript
// csrf.js
const token = document.querySelector('meta[name="_csrf"]').getAttribute('content');
const header = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

// Configurar Axios
axios.defaults.headers.common[header] = token;

// Configurar Fetch
const originalFetch = window.fetch;
window.fetch = function(url, options = {}) {
    if (options.method && options.method.toUpperCase() !== 'GET') {
        options.headers = {
            ...options.headers,
            [header]: token
        };
    }
    return originalFetch(url, options);
};
```

**Repositorio de tokens CSRF personalizado:**

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    return http
        .csrf()
            .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
        .and()
        .build();
}
```

Esto almacena el token CSRF en una cookie llamada `XSRF-TOKEN` que JavaScript puede leer.

---

## 5.4 Aplicando seguridad a nivel de método

La seguridad a nivel de método permite proteger métodos individuales con anotaciones.

**Habilitar seguridad a nivel de método:**

```java
@Configuration
@EnableGlobalMethodSecurity(
    prePostEnabled = true,      // Habilita @PreAuthorize, @PostAuthorize
    securedEnabled = true,       // Habilita @Secured
    jsr250Enabled = true         // Habilita @RolesAllowed
)
public class MethodSecurityConfig {
}
```

### Anotaciones disponibles

| Anotación | Framework | Evaluación | Uso |
|-----------|-----------|------------|-----|
| `@PreAuthorize` | Spring Security | Antes del método | **Recomendada** - Soporta SpEL |
| `@PostAuthorize` | Spring Security | Después del método | Verifica resultado |
| `@Secured` | Spring Security | Antes del método | Roles simples |
| `@RolesAllowed` | JSR-250 | Antes del método | Estándar Java EE |
| `@PreFilter` | Spring Security | Antes del método | Filtra colecciones |
| `@PostFilter` | Spring Security | Después del método | Filtra resultados |

---

### @PreAuthorize

Verifica condiciones **antes** de ejecutar el método.

**Ejemplo 1: Básico con roles**

```java
@Service
public class ProductService {
    
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteProduct(Long id) {
        // Solo ADMIN puede ejecutar
    }
    
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public Product getProduct(Long id) {
        // USER o ADMIN pueden ejecutar
    }
    
    @PreAuthorize("hasAuthority('DELETE')")
    public void deleteAll() {
        // Solo con permiso DELETE
    }
}
```

**Ejemplo 2: Con variables de método**

```java
@Service
public class OrderService {
    
    @Autowired
    private OrderRepository orderRepository;
    
    // Verificar que el usuario sea dueño de la orden
    @PreAuthorize("@securityService.isOrderOwner(authentication, #orderId)")
    public void cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new NotFoundException("Order not found"));
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
    }
    
    // Verificar rol y propiedad
    @PreAuthorize("hasRole('ADMIN') or @securityService.isOrderOwner(authentication, #orderId)")
    public void updateOrder(Long orderId, OrderDTO orderDTO) {
        // ADMIN puede actualizar cualquier orden
        // USER solo puede actualizar sus propias órdenes
    }
}
```

**Servicio de seguridad:**

```java
@Service("securityService")
public class SecurityService {
    
    @Autowired
    private OrderRepository orderRepository;
    
    public boolean isOrderOwner(Authentication authentication, Long orderId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        
        User user = (User) authentication.getPrincipal();
        Order order = orderRepository.findById(orderId).orElse(null);
        
        if (order == null) {
            return false;
        }
        
        return order.getUserId().equals(user.getId());
    }
    
    public boolean canEditProduct(Authentication authentication, Long productId) {
        User user = (User) authentication.getPrincipal();
        
        // Admin puede editar cualquier producto
        if (user.hasRole("ROLE_ADMIN")) {
            return true;
        }
        
        // Usuario puede editar sus propios productos
        Product product = productRepository.findById(productId).orElse(null);
        return product != null && product.getCreatedBy().equals(user.getId());
    }
}
```

**Ejemplo 3: Expresiones complejas**

```java
@Service
public class ArticleService {
    
    // Múltiples condiciones
    @PreAuthorize("hasRole('EDITOR') and #article.status == 'DRAFT'")
    public void publishArticle(Article article) {
        article.setStatus("PUBLISHED");
        articleRepository.save(article);
    }
    
    // Verificar propiedad del objeto
    @PreAuthorize("#article.author == authentication.name or hasRole('ADMIN')")
    public void updateArticle(Article article) {
        articleRepository.save(article);
    }
    
    // Con operadores lógicos
    @PreAuthorize("(hasRole('USER') and #comment.isPublic == true) or hasRole('MODERATOR')")
    public void viewComment(Comment comment) {
        // Lógica
    }
}
```

---

### @PostAuthorize

Verifica condiciones **después** de ejecutar el método, útil para verificar el resultado.

**Ejemplo:**

```java
@Service
public class DocumentService {
    
    // Verificar que el documento retornado pertenece al usuario
    @PostAuthorize("returnObject.owner == authentication.name")
    public Document getDocument(Long id) {
        return documentRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Document not found"));
    }
    
    // Verificar propiedades del objeto retornado
    @PostAuthorize("returnObject.isPublic == true or returnObject.owner == authentication.name")
    public Article getArticle(Long id) {
        return articleRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Article not found"));
    }
}
```

---

### @Secured

Anotación más simple, solo verifica roles.

```java
@Service
public class UserService {
    
    @Secured("ROLE_ADMIN")
    public void deleteUser(Long userId) {
        userRepository.deleteById(userId);
    }
    
    @Secured({"ROLE_ADMIN", "ROLE_MODERATOR"})
    public void banUser(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        user.setAccountNonLocked(false);
        userRepository.save(user);
    }
}
```

---

### @RolesAllowed

Estándar JSR-250, similar a @Secured.

```java
@Service
public class ReportService {
    
    @RolesAllowed("ADMIN")
    public Report generateFinancialReport() {
        // Solo ADMIN
    }
    
    @RolesAllowed({"ADMIN", "MANAGER"})
    public Report generateUserReport() {
        // ADMIN o MANAGER
    }
}
```

---

### @PreFilter y @PostFilter

Filtran elementos de colecciones basándose en condiciones.

**@PreFilter - Filtra parámetros de entrada:**

```java
@Service
public class BatchService {
    
    // Filtra la lista de entrada, solo procesa documentos públicos o del usuario
    @PreFilter("filterObject.isPublic == true or filterObject.owner == authentication.name")
    public void processDocuments(List<Document> documents) {
        documents.forEach(doc -> {
            // Procesar solo documentos permitidos
        });
    }
}
```

**@PostFilter - Filtra resultados:**

```java
@Service
public class ArticleService {
    
    // Retorna solo artículos públicos o del usuario autenticado
    @PostFilter("filterObject.isPublic == true or filterObject.author == authentication.name")
    public List<Article> getAllArticles() {
        return articleRepository.findAll();
    }
    
    // Filtrar por múltiples condiciones
    @PostFilter("hasRole('ADMIN') or filterObject.createdBy == authentication.name")
    public List<Product> getProducts() {
        return productRepository.findAll();
    }
}
```

---

### Ejemplo completo: Servicio con múltiples niveles de seguridad

```java
@Service
@Slf4j
@RequiredArgsConstructor
public class OrderAdminService {
    
    private final OrderRepository orderRepository;
    private final SecurityService securityService;
    
    // Solo ADMIN puede ver todas las órdenes
    @PreAuthorize("hasRole('ADMIN')")
    public List<Order> getAllOrders() {
        log.info("Admin viewing all orders");
        return orderRepository.findAll();
    }
    
    // USER puede ver solo sus órdenes, ADMIN puede ver todas
    @PreAuthorize("hasRole('USER')")
    @PostFilter("hasRole('ADMIN') or filterObject.user.username == authentication.name")
    public List<Order> getMyOrders() {
        return orderRepository.findAll();
    }
    
    // Verificar propiedad antes de cancelar
    @PreAuthorize("@securityService.isOrderOwner(authentication, #orderId) or hasRole('ADMIN')")
    public void cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new NotFoundException("Order not found"));
        
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        
        log.info("Order {} cancelled", orderId);
    }
    
    // Solo ADMIN puede eliminar
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteOrder(Long orderId) {
        orderRepository.deleteById(orderId);
        log.warn("Order {} deleted by admin", orderId);
    }
    
    // Verificar después de obtener
    @PostAuthorize("returnObject.user.username == authentication.name or hasRole('ADMIN')")
    public Order getOrder(Long orderId) {
        return orderRepository.findById(orderId)
            .orElseThrow(() -> new NotFoundException("Order not found"));
    }
    
    // Expresión compleja
    @PreAuthorize("(hasRole('ADMIN') and #order.totalAmount < 10000) or (hasRole('SUPER_ADMIN'))")
    public void approveOrder(Order order) {
        order.setStatus(OrderStatus.APPROVED);
        orderRepository.save(order);
    }
}
```

---

## 5.5 Conociendo a tu usuario

Spring Security proporciona varias formas de acceder al usuario autenticado.

### Métodos para obtener el usuario actual

**1. SecurityContextHolder (programático):**

```java
@Service
public class CurrentUserService {
    
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder
            .getContext()
            .getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user");
        }
        
        return (User) authentication.getPrincipal();
    }
    
    public String getCurrentUsername() {
        return SecurityContextHolder
            .getContext()
            .getAuthentication()
            .getName();
    }
    
    public boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder
            .getContext()
            .getAuthentication();
        
        return authentication != null && 
               authentication.isAuthenticated() && 
               !(authentication instanceof AnonymousAuthenticationToken);
    }
    
    public boolean hasRole(String role) {
        return SecurityContextHolder
            .getContext()
            .getAuthentication()
            .getAuthorities()
            .stream()
            .anyMatch(auth -> auth.getAuthority().equals("ROLE_" + role));
    }
}
```

**2. @AuthenticationPrincipal (en controladores):**

```java
@Controller
@RequestMapping("/profile")
public class ProfileController {
    
    // Inyectar User directamente
    @GetMapping
    public String viewProfile(
            @AuthenticationPrincipal User user,
            Model model) {
        
        model.addAttribute("user", user);
        return "profile";
    }
    
    // Con Optional
    @GetMapping("/edit")
    public String editProfile(
            @AuthenticationPrincipal(errorOnInvalidType = false) User user,
            Model model) {
        
        if (user == null) {
            return "redirect:/login";
        }
        
        model.addAttribute("user", user);
        return "profile-edit";
    }
}
```

**3. Principal (interfaz estándar):**

```java
@GetMapping("/info")
public String userInfo(Principal principal, Model model) {
    String username = principal.getName();
    model.addAttribute("username", username);
    return "user-info";
}
```

**4. Authentication (objeto completo):**

```java
@GetMapping("/details")
public String userDetails(Authentication authentication, Model model) {
    if (authentication != null) {
        model.addAttribute("username", authentication.getName());
        model.addAttribute("authorities", authentication.getAuthorities());
        model.addAttribute("details", authentication.getDetails());
        model.addAttribute("user", authentication.getPrincipal());
    }
    return "user-details";
}
```

**5. En servicios con método helper:**

```java
@Service
@RequiredArgsConstructor
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final CurrentUserService currentUserService;
    
    public Order createOrder(OrderDTO orderDTO) {
        User currentUser = currentUserService.getCurrentUser();
        
        Order order = new Order();
        order.setUser(currentUser);
        order.setItems(orderDTO.getItems());
        order.setTotalAmount(calculateTotal(orderDTO));
        
        return orderRepository.save(order);
    }
    
    public List<Order> getMyOrders() {
        User currentUser = currentUserService.getCurrentUser();
        return orderRepository.findByUserId(currentUser.getId());
    }
}
```

**6. En vistas Thymeleaf:**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
<head>
    <title>Dashboard</title>
</head>
<body>
    <!-- Mostrar username -->
    <p>Welcome, <span sec:authentication="name">User</span>!</p>
    
    <!-- Mostrar información completa del usuario -->
    <div sec:authentication="principal">
        <p>Full Name: <span sec:authentication="principal.fullName">Name</span></p>
        <p>Email: <span sec:authentication="principal.email">email@example.com</span></p>
    </div>
    
    <!-- Mostrar condicionalmente según rol -->
    <div sec:authorize="hasRole('ADMIN')">
        <p>Admin content here</p>
        <a th:href="@{/admin/dashboard}">Admin Panel</a>
    </div>
    
    <div sec:authorize="hasRole('USER')">
        <p>User content here</p>
    </div>
    
    <!-- Mostrar si está autenticado -->
    <div sec:authorize="isAuthenticated()">
        <a th:href="@{/logout}">Logout</a>
    </div>
    
    <!-- Mostrar si es anónimo -->
    <div sec:authorize="isAnonymous()">
        <a th:href="@{/login}">Login</a>
    </div>
    
    <!-- Mostrar autoridades -->
    <ul>
        <li th:each="authority : ${#authentication.authorities}" 
            th:text="${authority.authority}">ROLE</li>
    </ul>
</body>
</html>
```

**Dependencia para Thymeleaf Security:**

```xml
<dependency>
    <groupId>org.thymeleaf.extras</groupId>
    <artifactId>thymeleaf-extras-springsecurity5</artifactId>
</dependency>
```

---

## Conceptos avanzados

### 1. Múltiples SecurityFilterChains

Puedes tener múltiples configuraciones de seguridad para diferentes patrones de URL.

```java
@Configuration
@EnableWebSecurity
public class MultipleSecurityConfig {
    
    @Bean
    @Order(1)  // Prioridad más alta
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
            .antMatcher("/api/**")
            .authorizeRequests()
                .anyRequest().hasRole("API_USER")
            .and()
                .httpBasic()
            .and()
                .csrf().disable();  // APIs REST sin CSRF
        
        return http.build();
    }
    
    @Bean
    @Order(2)
    public SecurityFilterChain adminFilterChain(HttpSecurity http) throws Exception {
        http
            .antMatcher("/admin/**")
            .authorizeRequests()
                .anyRequest().hasRole("ADMIN")
            .and()
                .formLogin()
                    .loginPage("/admin/login")
            .and()
                .logout()
                    .logoutUrl("/admin/logout");
        
        return http.build();
    }
    
    @Bean
    @Order(3)  // Configuración por defecto
    public SecurityFilterChain defaultFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeRequests()
                .antMatchers("/", "/public/**").permitAll()
                .anyRequest().authenticated()
            .and()
                .formLogin()
                    .loginPage("/login")
                    .permitAll();
        
        return http.build();
    }
}
```

### 2. Auditoría de acciones de seguridad

```java
@Component
@Slf4j
public class SecurityAuditListener {
    
    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        String username = event.getAuthentication().getName();
        log.info("User '{}' logged in successfully", username);
        
        // Guardar en BD
        auditService.logLogin(username, "SUCCESS");
    }
    
    @EventListener
    public void onAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        String username = event.getAuthentication().getName();
        Exception exception = event.getException();
        
        log.warn("Failed login attempt for user '{}': {}", 
                 username, exception.getMessage());
        
        // Guardar en BD
        auditService.logLogin(username, "FAILURE");
    }
    
    @EventListener
    public void onLogout(LogoutSuccessEvent event) {
        String username = event.getAuthentication().getName();
        log.info("User '{}' logged out", username);
        
        auditService.logLogout(username);
    }
    
    @EventListener
    public void onAuthorizationFailure(AuthorizationFailureEvent event) {
        Authentication authentication = (Authentication) event.getAuthentication();
        String username = authentication != null ? authentication.getName() : "anonymous";
        
        log.warn("Authorization failure for user '{}' accessing: {}", 
                 username, event.getAccessDeniedException().getMessage());
        
        auditService.logAccessDenied(username, event.getSource().toString());
    }
}
```

### 3. Bloqueo de cuenta por intentos fallidos

```java
@Service
@RequiredArgsConstructor
public class LoginAttemptService {
    
    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCK_TIME_DURATION = 24; // horas
    
    private final UserRepository userRepository;
    private final LoadingCache<String, Integer> attemptsCache;
    
    public LoginAttemptService() {
        this.attemptsCache = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.DAYS)
            .build(new CacheLoader<String, Integer>() {
                @Override
                public Integer load(String key) {
                    return 0;
                }
            });
    }
    
    public void loginSucceeded(String username) {
        attemptsCache.invalidate(username);
        
        // Desbloquear usuario si estaba bloqueado
        userRepository.findByUsername(username).ifPresent(user -> {
            if (!user.isAccountNonLocked()) {
                user.setAccountNonLocked(true);
                user.setLockTime(null);
                userRepository.save(user);
            }
        });
    }
    
    public void loginFailed(String username) {
        int attempts = attemptsCache.getUnchecked(username);
        attempts++;
        attemptsCache.put(username, attempts);
        
        if (attempts >= MAX_ATTEMPTS) {
            userRepository.findByUsername(username).ifPresent(user -> {
                user.setAccountNonLocked(false);
                user.setLockTime(LocalDateTime.now());
                userRepository.save(user);
            });
        }
    }
    
    public boolean isBlocked(String username) {
        return attemptsCache.getUnchecked(username) >= MAX_ATTEMPTS ||
               isUserLocked(username);
    }
    
    private boolean isUserLocked(String username) {
        return userRepository.findByUsername(username)
            .map(user -> {
                if (!user.isAccountNonLocked() && user.getLockTime() != null) {
                    LocalDateTime unlockTime = user.getLockTime()
                        .plusHours(LOCK_TIME_DURATION);
                    
                    if (LocalDateTime.now().isAfter(unlockTime)) {
                        // Desbloquear automáticamente
                        user.setAccountNonLocked(true);
                        user.setLockTime(null);
                        userRepository.save(user);
                        return false;
                    }
                    return true;
                }
                return false;
            })
            .orElse(false);
    }
}
```

### 4. Sesiones concurrentes

Limitar el número de sesiones simultáneas por usuario.

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    return http
        .authorizeRequests()
            .anyRequest().authenticated()
        .and()
            .formLogin()
        .and()
            .sessionManagement()
                .maximumSessions(1)  // Solo 1 sesión activa
                .maxSessionsPreventsLogin(true)  // Prevenir nuevo login
                .expiredUrl("/login?expired=true")
        .and()
        .and()
        .build();
}
```

### 5. Cambio de sesión en autenticación

Protección contra Session Fixation.

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    return http
        .sessionManagement()
            .sessionFixation().newSession()  // Crear nueva sesión en login
            .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            .invalidSessionUrl("/login?invalid=true")
        .and()
        .build();
}
```

---

## Mejores prácticas de seguridad

### 1. Nunca almacenar contraseñas en texto plano

```java
// ❌ MAL
user.setPassword("password123");

// ✅ BIEN
user.setPassword(passwordEncoder.encode("password123"));
```

### 2. Usar HTTPS en producción

```properties
# application-prod.properties
server.ssl.enabled=true
server.ssl.key-store=classpath:keystore.p12
server.ssl.key-store-password=changeit
server.ssl.key-store-type=PKCS12
server.ssl.key-alias=tomcat

# Forzar HTTPS
security.require-ssl=true
```

### 3. Configurar timeouts de sesión

```properties
# Timeout de sesión (30 minutos)
server.servlet.session.timeout=30m

# Remember-me timeout (7 días)
spring.security.remember-me.token-validity-seconds=604800
```

### 4. Validar entrada del usuario

```java
// Prevenir SQL injection, XSS, etc.
@NotBlank
@Pattern(regexp = "^[a-zA-Z0-9_]+$")
private String username;
```

### 5. Implementar rate limiting

```java
@Component
public class RateLimitingFilter extends OncePerRequestFilter {
    
    private final RateLimiter rateLimiter = RateLimiter.create(100.0); // 100 req/seg
    
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain) 
            throws ServletException, IOException {
        
        if (!rateLimiter.tryAcquire()) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("Too many requests");
            return;
        }
        
        filterChain.doFilter(request, response);
    }
}
```

### 6. Logging y monitoreo

```java
@Slf4j
@Component
public class SecurityEventListener {
    
    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        log.info("LOGIN_SUCCESS: user={}", event.getAuthentication().getName());
    }
    
    @EventListener
    public void onAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        log.warn("LOGIN_FAILURE: user={}, reason={}", 
                 event.getAuthentication().getName(),
                 event.getException().getMessage());
    }
}
```

### 7. Headers de seguridad

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    return http
        .headers()
            .xssProtection()
                .and()
            .contentSecurityPolicy("script-src 'self'")
                .and()
            .frameOptions()
                .deny()
        .and()
        .build();
}
```

---

## Resumen

**Flujo completo de autenticación:**
1. Usuario envía credenciales
2. `AuthenticationManager` las procesa
3. `UserDetailsService` carga el usuario
4. `PasswordEncoder` verifica la contraseña
5. Si es correcto, crea `Authentication`
6. Almacena en `SecurityContext`
7. Usuario autenticado puede acceder a recursos protegidos

**Flujo completo de autorización:**
1. Usuario autenticado intenta acceder a recurso
2. `SecurityFilterChain` intercepta la petición
3. Verifica reglas de `authorizeRequests()`
4. O verifica anotaciones `@PreAuthorize`
5. Si tiene permisos, permite acceso
6. Si no, lanza `AccessDeniedException`

**Puntos clave:**
- Siempre usar `PasswordEncoder` (BCrypt recomendado)
- CSRF habilitado por defecto para formularios
- Múltiples formas de obtener usuario actual
- Seguridad a nivel de método con `@PreAuthorize`
- OAuth2 para autenticación de terceros
- Auditoría y logging son esenciales
- HTTPS obligatorio en producción

---

*Esta guía está basada en Spring in Action 6th Edition y cubre Spring Security 5.7+ con Spring Boot 2.7+.*
