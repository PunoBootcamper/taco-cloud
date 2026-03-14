# Guía de Persistencia de Datos con Spring

## 3. Trabajando con datos

Esta guía cubre las diferentes formas de persistir datos en aplicaciones Spring, desde JDBC básico hasta Spring Data JPA, proporcionando ejemplos prácticos y comparaciones entre cada enfoque.

---

## 3.1 Lectura y escritura de datos con JDBC

JDBC (Java Database Connectivity) es la API estándar de Java para interactuar con bases de datos relacionales. Spring simplifica el uso de JDBC a través de `JdbcTemplate`.

### 3.1.1 Adaptando el dominio para persistencia

Las clases de dominio necesitan adaptarse para trabajar con bases de datos relacionales.

**Consideraciones clave:**
- Añadir identificadores únicos (IDs)
- Agregar campos de auditoría (creación, modificación)
- Manejar relaciones entre entidades
- Considerar el mapeo objeto-relacional

**Ejemplo 1: Clase de dominio simple**

**Antes (sin persistencia):**
```java
package com.example.domain;

import lombok.Data;

@Data
public class Product {
    private String name;
    private String category;
    private double price;
}
```

**Después (preparada para JDBC):**
```java
package com.example.domain;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    private Long id;                        // ID generado por la BD
    private String name;
    private String category;
    private double price;
    private LocalDateTime createdAt;        // Auditoría
    private LocalDateTime updatedAt;
    
    // Constructor sin ID (para nuevos productos)
    public Product(String name, String category, double price) {
        this.name = name;
        this.category = category;
        this.price = price;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
```

**Ejemplo 2: Entidad con relaciones**

```java
package com.example.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class Order {
    private Long id;
    private String customerName;
    private String shippingAddress;
    private LocalDateTime orderDate;
    private OrderStatus status;
    
    // Lista de items (relación)
    private List<OrderItem> items = new ArrayList<>();
    
    public Order(String customerName, String shippingAddress) {
        this.customerName = customerName;
        this.shippingAddress = shippingAddress;
        this.orderDate = LocalDateTime.now();
        this.status = OrderStatus.PENDING;
    }
    
    public void addItem(OrderItem item) {
        items.add(item);
    }
    
    public double getTotalAmount() {
        return items.stream()
                    .mapToDouble(OrderItem::getSubtotal)
                    .sum();
    }
    
    public enum OrderStatus {
        PENDING, PROCESSING, SHIPPED, DELIVERED, CANCELLED
    }
}
```

```java
package com.example.domain;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderItem {
    private Long id;
    private Long orderId;           // Foreign key
    private Long productId;         // Foreign key
    private String productName;
    private int quantity;
    private double unitPrice;
    
    public double getSubtotal() {
        return quantity * unitPrice;
    }
}
```

**Ejemplo 3: Clase con validación y persistencia**

```java
package com.example.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class User {
    private Long id;
    
    @NotBlank(message = "Username is required")
    @Size(min = 4, max = 50)
    private String username;
    
    @NotBlank(message = "Email is required")
    @Email
    private String email;
    
    @NotBlank(message = "Password is required")
    @Size(min = 8)
    private String password;
    
    @NotNull
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;
    
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;
    
    public User(String username, String email, String password, LocalDate dateOfBirth) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.dateOfBirth = dateOfBirth;
        this.active = true;
        this.createdAt = LocalDateTime.now();
    }
}
```

---

### 3.1.2 Trabajando con JdbcTemplate

`JdbcTemplate` es la clase central de Spring JDBC que simplifica el acceso a bases de datos eliminando código boilerplate.

**Dependencia Maven:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jdbc</artifactId>
</dependency>

<!-- Driver de base de datos - ejemplo con H2 -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>
```

**Configuración en application.properties:**
```properties
# H2 en memoria (desarrollo)
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# MySQL (producción)
# spring.datasource.url=jdbc:mysql://localhost:3306/mydb
# spring.datasource.username=root
# spring.datasource.password=secret
# spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# PostgreSQL
# spring.datasource.url=jdbc:postgresql://localhost:5432/mydb
# spring.datasource.username=postgres
# spring.datasource.password=secret

# Mostrar SQL en logs
spring.jpa.show-sql=true
logging.level.org.springframework.jdbc.core=DEBUG
```

#### Ventajas de JdbcTemplate

- Manejo automático de conexiones y recursos
- Manejo de excepciones mejorado (traduce SQLException)
- Reduce código boilerplate significativamente
- Ejecuta consultas, actualizaciones y procedimientos almacenados

**Comparación: JDBC puro vs JdbcTemplate**

**JDBC Puro (NO usar):**
```java
public Product findById(Long id) {
    Connection conn = null;
    PreparedStatement ps = null;
    ResultSet rs = null;
    
    try {
        conn = dataSource.getConnection();
        ps = conn.prepareStatement("SELECT * FROM products WHERE id = ?");
        ps.setLong(1, id);
        rs = ps.executeQuery();
        
        if (rs.next()) {
            Product product = new Product();
            product.setId(rs.getLong("id"));
            product.setName(rs.getString("name"));
            product.setCategory(rs.getString("category"));
            product.setPrice(rs.getDouble("price"));
            return product;
        }
        return null;
        
    } catch (SQLException e) {
        throw new RuntimeException("Error querying product", e);
    } finally {
        // Cerrar recursos manualmente
        if (rs != null) try { rs.close(); } catch (SQLException e) {}
        if (ps != null) try { ps.close(); } catch (SQLException e) {}
        if (conn != null) try { conn.close(); } catch (SQLException e) {}
    }
}
```

**Con JdbcTemplate (USAR):**
```java
public Product findById(Long id) {
    String sql = "SELECT * FROM products WHERE id = ?";
    
    return jdbcTemplate.queryForObject(sql, 
        (rs, rowNum) -> new Product(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getString("category"),
            rs.getDouble("price"),
            rs.getTimestamp("created_at").toLocalDateTime(),
            rs.getTimestamp("updated_at").toLocalDateTime()
        ),
        id
    );
}
```

---

#### Definiendo repositorios JDBC

Un repositorio encapsula la lógica de acceso a datos. Se recomienda usar interfaces y clases de implementación.

**Patrón de diseño:**
1. Definir interfaz del repositorio
2. Implementar con JdbcTemplate
3. Inyectar en servicios

**Ejemplo 1: Repositorio básico de productos**

**Interfaz:**
```java
package com.example.repository;

import com.example.domain.Product;
import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    Product save(Product product);
    Optional<Product> findById(Long id);
    List<Product> findAll();
    List<Product> findByCategory(String category);
    void update(Product product);
    void deleteById(Long id);
    long count();
}
```

**Implementación:**
```java
package com.example.repository;

import com.example.domain.Product;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Repository
public class JdbcProductRepository implements ProductRepository {
    
    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert jdbcInsert;
    
    // Inyección por constructor
    public JdbcProductRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        
        // SimpleJdbcInsert para inserciones con auto-generación de ID
        this.jdbcInsert = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("products")
                .usingGeneratedKeyColumns("id");
    }
    
    @Override
    public Product save(Product product) {
        Map<String, Object> values = new HashMap<>();
        values.put("name", product.getName());
        values.put("category", product.getCategory());
        values.put("price", product.getPrice());
        values.put("created_at", product.getCreatedAt());
        values.put("updated_at", product.getUpdatedAt());
        
        Long id = jdbcInsert.executeAndReturnKey(values).longValue();
        product.setId(id);
        
        log.info("Saved product with ID: {}", id);
        return product;
    }
    
    @Override
    public Optional<Product> findById(Long id) {
        String sql = "SELECT * FROM products WHERE id = ?";
        
        try {
            Product product = jdbcTemplate.queryForObject(sql, productRowMapper(), id);
            return Optional.ofNullable(product);
        } catch (Exception e) {
            log.warn("Product not found with ID: {}", id);
            return Optional.empty();
        }
    }
    
    @Override
    public List<Product> findAll() {
        String sql = "SELECT * FROM products ORDER BY name";
        return jdbcTemplate.query(sql, productRowMapper());
    }
    
    @Override
    public List<Product> findByCategory(String category) {
        String sql = "SELECT * FROM products WHERE category = ? ORDER BY name";
        return jdbcTemplate.query(sql, productRowMapper(), category);
    }
    
    @Override
    public void update(Product product) {
        String sql = "UPDATE products SET name = ?, category = ?, " +
                    "price = ?, updated_at = ? WHERE id = ?";
        
        int rows = jdbcTemplate.update(sql, 
            product.getName(),
            product.getCategory(),
            product.getPrice(),
            product.getUpdatedAt(),
            product.getId()
        );
        
        log.info("Updated {} rows for product ID: {}", rows, product.getId());
    }
    
    @Override
    public void deleteById(Long id) {
        String sql = "DELETE FROM products WHERE id = ?";
        int rows = jdbcTemplate.update(sql, id);
        log.info("Deleted {} rows for product ID: {}", rows, id);
    }
    
    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM products";
        return jdbcTemplate.queryForObject(sql, Long.class);
    }
    
    // RowMapper reutilizable
    private RowMapper<Product> productRowMapper() {
        return (ResultSet rs, int rowNum) -> new Product(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getString("category"),
            rs.getDouble("price"),
            rs.getTimestamp("created_at").toLocalDateTime(),
            rs.getTimestamp("updated_at").toLocalDateTime()
        );
    }
}
```

**Ejemplo 2: Repositorio con relaciones (Order y OrderItem)**

```java
package com.example.repository;

import com.example.domain.Order;
import com.example.domain.OrderItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Repository
public class JdbcOrderRepository {
    
    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert orderInsert;
    private final SimpleJdbcInsert orderItemInsert;
    
    public JdbcOrderRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        
        this.orderInsert = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("orders")
                .usingGeneratedKeyColumns("id");
                
        this.orderItemInsert = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("order_items")
                .usingGeneratedKeyColumns("id");
    }
    
    @Transactional
    public Order save(Order order) {
        // 1. Guardar la orden
        Map<String, Object> orderValues = new HashMap<>();
        orderValues.put("customer_name", order.getCustomerName());
        orderValues.put("shipping_address", order.getShippingAddress());
        orderValues.put("order_date", order.getOrderDate());
        orderValues.put("status", order.getStatus().toString());
        
        Long orderId = orderInsert.executeAndReturnKey(orderValues).longValue();
        order.setId(orderId);
        
        // 2. Guardar los items
        for (OrderItem item : order.getItems()) {
            Map<String, Object> itemValues = new HashMap<>();
            itemValues.put("order_id", orderId);
            itemValues.put("product_id", item.getProductId());
            itemValues.put("product_name", item.getProductName());
            itemValues.put("quantity", item.getQuantity());
            itemValues.put("unit_price", item.getUnitPrice());
            
            Long itemId = orderItemInsert.executeAndReturnKey(itemValues).longValue();
            item.setId(itemId);
            item.setOrderId(orderId);
        }
        
        log.info("Saved order with ID: {} and {} items", orderId, order.getItems().size());
        return order;
    }
    
    public Optional<Order> findById(Long id) {
        String orderSql = "SELECT * FROM orders WHERE id = ?";
        
        try {
            Order order = jdbcTemplate.queryForObject(orderSql, (rs, rowNum) -> {
                Order o = new Order();
                o.setId(rs.getLong("id"));
                o.setCustomerName(rs.getString("customer_name"));
                o.setShippingAddress(rs.getString("shipping_address"));
                o.setOrderDate(rs.getTimestamp("order_date").toLocalDateTime());
                o.setStatus(Order.OrderStatus.valueOf(rs.getString("status")));
                return o;
            }, id);
            
            // Cargar items
            String itemsSql = "SELECT * FROM order_items WHERE order_id = ?";
            List<OrderItem> items = jdbcTemplate.query(itemsSql, (rs, rowNum) -> 
                new OrderItem(
                    rs.getLong("id"),
                    rs.getLong("order_id"),
                    rs.getLong("product_id"),
                    rs.getString("product_name"),
                    rs.getInt("quantity"),
                    rs.getDouble("unit_price")
                ), id
            );
            
            order.getItems().addAll(items);
            return Optional.of(order);
            
        } catch (Exception e) {
            log.warn("Order not found with ID: {}", id);
            return Optional.empty();
        }
    }
    
    public List<Order> findByCustomerName(String customerName) {
        String sql = "SELECT * FROM orders WHERE customer_name = ? ORDER BY order_date DESC";
        
        List<Order> orders = jdbcTemplate.query(sql, (rs, rowNum) -> {
            Order order = new Order();
            order.setId(rs.getLong("id"));
            order.setCustomerName(rs.getString("customer_name"));
            order.setShippingAddress(rs.getString("shipping_address"));
            order.setOrderDate(rs.getTimestamp("order_date").toLocalDateTime());
            order.setStatus(Order.OrderStatus.valueOf(rs.getString("status")));
            return order;
        }, customerName);
        
        // Cargar items para cada orden
        for (Order order : orders) {
            String itemsSql = "SELECT * FROM order_items WHERE order_id = ?";
            List<OrderItem> items = jdbcTemplate.query(itemsSql, (rs, rowNum) ->
                new OrderItem(
                    rs.getLong("id"),
                    rs.getLong("order_id"),
                    rs.getLong("product_id"),
                    rs.getString("product_name"),
                    rs.getInt("quantity"),
                    rs.getDouble("unit_price")
                ), order.getId()
            );
            order.getItems().addAll(items);
        }
        
        return orders;
    }
}
```

---

#### Insertando una fila

Existen varias formas de insertar datos con JdbcTemplate.

**Método 1: update() simple**
```java
public void insertProduct(Product product) {
    String sql = "INSERT INTO products (name, category, price, created_at, updated_at) " +
                 "VALUES (?, ?, ?, ?, ?)";
    
    jdbcTemplate.update(sql,
        product.getName(),
        product.getCategory(),
        product.getPrice(),
        product.getCreatedAt(),
        product.getUpdatedAt()
    );
}
```

**Método 2: SimpleJdbcInsert (recomendado)**
```java
public Product insertProduct(Product product) {
    SimpleJdbcInsert insert = new SimpleJdbcInsert(jdbcTemplate)
            .withTableName("products")
            .usingGeneratedKeyColumns("id");
    
    Map<String, Object> values = new HashMap<>();
    values.put("name", product.getName());
    values.put("category", product.getCategory());
    values.put("price", product.getPrice());
    values.put("created_at", product.getCreatedAt());
    values.put("updated_at", product.getUpdatedAt());
    
    Long id = insert.executeAndReturnKey(values).longValue();
    product.setId(id);
    
    return product;
}
```

**Método 3: Batch insert (múltiples registros)**
```java
public void insertProducts(List<Product> products) {
    String sql = "INSERT INTO products (name, category, price, created_at, updated_at) " +
                 "VALUES (?, ?, ?, ?, ?)";
    
    jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
        @Override
        public void setValues(PreparedStatement ps, int i) throws SQLException {
            Product product = products.get(i);
            ps.setString(1, product.getName());
            ps.setString(2, product.getCategory());
            ps.setDouble(3, product.getPrice());
            ps.setTimestamp(4, Timestamp.valueOf(product.getCreatedAt()));
            ps.setTimestamp(5, Timestamp.valueOf(product.getUpdatedAt()));
        }
        
        @Override
        public int getBatchSize() {
            return products.size();
        }
    });
    
    log.info("Inserted {} products in batch", products.size());
}
```

---

### 3.1.3 Definiendo un esquema y precargando datos

Spring Boot puede ejecutar scripts SQL automáticamente al iniciar la aplicación.

**Archivos de configuración:**
- `schema.sql`: Define la estructura de la base de datos (DDL)
- `data.sql`: Inserta datos iniciales (DML)

**Ubicación:** `src/main/resources/`

**Ejemplo: schema.sql**
```sql
-- Eliminar tablas si existen
DROP TABLE IF EXISTS order_items;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS users;

-- Tabla de productos
CREATE TABLE products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(50) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Tabla de usuarios
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    date_of_birth DATE NOT NULL,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP NULL
);

-- Tabla de órdenes
CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_name VARCHAR(100) NOT NULL,
    shipping_address VARCHAR(255) NOT NULL,
    order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL,
    CONSTRAINT chk_status CHECK (status IN ('PENDING', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED'))
);

-- Tabla de items de orden
CREATE TABLE order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(100) NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL,
    CONSTRAINT fk_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT chk_quantity CHECK (quantity > 0)
);

-- Índices para mejorar rendimiento
CREATE INDEX idx_products_category ON products(category);
CREATE INDEX idx_orders_customer ON orders(customer_name);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_order_items_order ON order_items(order_id);
```

**Ejemplo: data.sql**
```sql
-- Insertar productos iniciales
INSERT INTO products (name, category, price) VALUES
('Laptop Dell XPS 13', 'Electronics', 1299.99),
('iPhone 14 Pro', 'Electronics', 999.99),
('Samsung Galaxy S23', 'Electronics', 899.99),
('Spring in Action 6th', 'Books', 49.99),
('Clean Code', 'Books', 39.99),
('Design Patterns', 'Books', 54.99),
('Nike Air Max', 'Clothing', 129.99),
('Adidas Ultraboost', 'Clothing', 179.99),
('Levi''s 501 Jeans', 'Clothing', 89.99),
('Sony WH-1000XM5', 'Electronics', 399.99);

-- Insertar usuarios de prueba
INSERT INTO users (username, email, password, date_of_birth) VALUES
('john_doe', 'john@example.com', '$2a$10$abcdefghijklmnop', '1990-05-15'),
('jane_smith', 'jane@example.com', '$2a$10$qrstuvwxyz123456', '1985-08-22'),
('bob_jones', 'bob@example.com', '$2a$10$7890abcdefghijkl', '1992-12-03');

-- Insertar órdenes de prueba
INSERT INTO orders (customer_name, shipping_address, status) VALUES
('John Doe', '123 Main St, New York, NY 10001', 'DELIVERED'),
('Jane Smith', '456 Oak Ave, Los Angeles, CA 90001', 'SHIPPED'),
('Bob Jones', '789 Pine Rd, Chicago, IL 60601', 'PROCESSING');

-- Insertar items de orden
INSERT INTO order_items (order_id, product_id, product_name, quantity, unit_price) VALUES
(1, 1, 'Laptop Dell XPS 13', 1, 1299.99),
(1, 4, 'Spring in Action 6th', 2, 49.99),
(2, 2, 'iPhone 14 Pro', 1, 999.99),
(2, 10, 'Sony WH-1000XM5', 1, 399.99),
(3, 7, 'Nike Air Max', 2, 129.99),
(3, 9, 'Levi''s 501 Jeans', 1, 89.99);
```

**Configuración en application.properties:**
```properties
# Ejecutar scripts SQL en cada inicio (desarrollo)
spring.sql.init.mode=always

# Ejecutar solo si la BD está embebida (H2, HSQLDB)
# spring.sql.init.mode=embedded

# Nunca ejecutar (producción)
# spring.sql.init.mode=never

# Plataforma específica (para scripts con sufijos)
spring.sql.init.platform=h2

# Continuar si hay errores en scripts
spring.sql.init.continue-on-error=false

# Encoding de los scripts
spring.sql.init.encoding=UTF-8
```

**Scripts específicos por plataforma:**
```
resources/
├── schema.sql              # Genérico
├── schema-h2.sql          # Específico para H2
├── schema-mysql.sql       # Específico para MySQL
├── schema-postgresql.sql  # Específico para PostgreSQL
├── data.sql               # Genérico
├── data-h2.sql           # Datos para H2
└── data-mysql.sql        # Datos para MySQL
```

---

### 3.1.4 Insertando datos

#### GeneratedKeyHolder

`KeyHolder` y `GeneratedKeyHolder` se usan para recuperar claves generadas automáticamente después de un insert.

**Ejemplo 1: Usando GeneratedKeyHolder**
```java
public Product save(Product product) {
    String sql = "INSERT INTO products (name, category, price, created_at, updated_at) " +
                 "VALUES (?, ?, ?, ?, ?)";
    
    KeyHolder keyHolder = new GeneratedKeyHolder();
    
    jdbcTemplate.update(connection -> {
        PreparedStatement ps = connection.prepareStatement(sql, 
                Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, product.getName());
        ps.setString(2, product.getCategory());
        ps.setDouble(3, product.getPrice());
        ps.setTimestamp(4, Timestamp.valueOf(product.getCreatedAt()));
        ps.setTimestamp(5, Timestamp.valueOf(product.getUpdatedAt()));
        return ps;
    }, keyHolder);
    
    Long id = keyHolder.getKey().longValue();
    product.setId(id);
    
    log.info("Inserted product with generated ID: {}", id);
    return product;
}
```

**Ejemplo 2: Con múltiples columnas generadas**
```java
public Order saveWithGeneratedValues(Order order) {
    String sql = "INSERT INTO orders (customer_name, shipping_address, status) " +
                 "VALUES (?, ?, ?)";
    
    KeyHolder keyHolder = new GeneratedKeyHolder();
    
    jdbcTemplate.update(connection -> {
        PreparedStatement ps = connection.prepareStatement(sql, 
                Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, order.getCustomerName());
        ps.setString(2, order.getShippingAddress());
        ps.setString(3, order.getStatus().toString());
        return ps;
    }, keyHolder);
    
    // Recuperar múltiples valores generados
    Map<String, Object> keys = keyHolder.getKeys();
    order.setId(((Number) keys.get("id")).longValue());
    
    // Si la BD también genera order_date
    if (keys.containsKey("order_date")) {
        Timestamp orderDate = (Timestamp) keys.get("order_date");
        order.setOrderDate(orderDate.toLocalDateTime());
    }
    
    return order;
}
```

**Ejemplo 3: Inserción transaccional completa**
```java
@Service
public class OrderService {
    
    private final JdbcTemplate jdbcTemplate;
    
    public OrderService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    @Transactional
    public Order createOrder(Order order) {
        // 1. Insertar la orden principal
        String orderSql = "INSERT INTO orders (customer_name, shipping_address, status) " +
                         "VALUES (?, ?, ?)";
        
        KeyHolder orderKeyHolder = new GeneratedKeyHolder();
        
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(orderSql, 
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, order.getCustomerName());
            ps.setString(2, order.getShippingAddress());
            ps.setString(3, order.getStatus().toString());
            return ps;
        }, orderKeyHolder);
        
        Long orderId = orderKeyHolder.getKey().longValue();
        order.setId(orderId);
        
        // 2. Insertar los items
        String itemSql = "INSERT INTO order_items " +
                        "(order_id, product_id, product_name, quantity, unit_price) " +
                        "VALUES (?, ?, ?, ?, ?)";
        
        for (OrderItem item : order.getItems()) {
            KeyHolder itemKeyHolder = new GeneratedKeyHolder();
            
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(itemSql, 
                        Statement.RETURN_GENERATED_KEYS);
                ps.setLong(1, orderId);
                ps.setLong(2, item.getProductId());
                ps.setString(3, item.getProductName());
                ps.setInt(4, item.getQuantity());
                ps.setDouble(5, item.getUnitPrice());
                return ps;
            }, itemKeyHolder);
            
            Long itemId = itemKeyHolder.getKey().longValue();
            item.setId(itemId);
            item.setOrderId(orderId);
        }
        
        log.info("Created order {} with {} items", orderId, order.getItems().size());
        return order;
    }
}
```

---

## 3.2 Trabajando con Spring Data JDBC

Spring Data JDBC es una implementación más simple que JPA, manteniendo el control sobre SQL pero eliminando código boilerplate.

**Características clave:**
- Menos "magia" que JPA
- Sin lazy loading
- Sin caché de primer nivel
- Control explícito de SQL
- Más predecible y simple

### 3.2.1 Añadiendo Spring Data JDBC al build

**Dependencia Maven:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jdbc</artifactId>
</dependency>

<!-- Driver de base de datos -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>
```

**Configuración application.properties:**
```properties
# Datasource
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.username=sa
spring.datasource.password=

# Spring Data JDBC
spring.data.jdbc.repositories.enabled=true

# Logging
logging.level.org.springframework.jdbc.core=DEBUG
```

---

### 3.2.2 Definiendo interfaces de repositorio

Con Spring Data, solo defines interfaces. Spring genera automáticamente las implementaciones.

**Jerarquía de interfaces:**
- `Repository<T, ID>`: Interfaz marcadora base
- `CrudRepository<T, ID>`: CRUD básico
- `PagingAndSortingRepository<T, ID>`: Con paginación
- `JdbcAggregateOperations`: Específico de Spring Data JDBC

**Ejemplo 1: Repositorio básico**
```java
package com.example.repository;

import com.example.domain.Product;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends CrudRepository<Product, Long> {
    // Métodos heredados de CrudRepository:
    // - save(S entity)
    // - saveAll(Iterable<S> entities)
    // - findById(ID id)
    // - existsById(ID id)
    // - findAll()
    // - findAllById(Iterable<ID> ids)
    // - count()
    // - deleteById(ID id)
    // - delete(T entity)
    // - deleteAll()
    
    // Métodos personalizados (Spring Data genera la implementación)
    List<Product> findByCategory(String category);
    List<Product> findByPriceLessThan(double price);
    List<Product> findByPriceGreaterThan(double price);
    List<Product> findByNameContaining(String name);
}
```

**Ejemplo 2: Repositorio con paginación**
```java
package com.example.repository;

import com.example.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductPageRepository extends PagingAndSortingRepository<Product, Long> {
    
    // Con paginación
    Page<Product> findByCategory(String category, Pageable pageable);
    
    // Con ordenamiento
    List<Product> findByPriceBetween(double minPrice, double maxPrice, Sort sort);
    
    // Top N resultados
    List<Product> findTop10ByOrderByPriceDesc();
    List<Product> findFirst5ByCategory(String category);
}
```

**Ejemplo 3: Repositorio con consultas personalizadas**
```java
package com.example.repository;

import com.example.domain.Product;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductQueryRepository extends CrudRepository<Product, Long> {
    
    // Consulta SQL personalizada
    @Query("SELECT * FROM products WHERE category = :category AND price < :maxPrice")
    List<Product> findByCategoryAndMaxPrice(@Param("category") String category, 
                                           @Param("maxPrice") double maxPrice);
    
    // Consulta de agregación
    @Query("SELECT category, COUNT(*) as count FROM products GROUP BY category")
    List<CategoryCount> countByCategory();
    
    // Actualización personalizada
    @Query("UPDATE products SET price = price * :factor WHERE category = :category")
    int updatePricesByCategory(@Param("category") String category, 
                               @Param("factor") double factor);
    
    // Consulta nativa compleja
    @Query("""
        SELECT p.* FROM products p 
        JOIN order_items oi ON p.id = oi.product_id 
        WHERE oi.order_id = :orderId
        """)
    List<Product> findProductsByOrderId(@Param("orderId") Long orderId);
}

// DTO para consultas de agregación
interface CategoryCount {
    String getCategory();
    Long getCount();
}
```

---

### 3.2.3 Anotando el dominio para persistencia

Spring Data JDBC usa anotaciones para mapear clases a tablas.

**Anotaciones principales:**
- `@Table`: Especifica el nombre de la tabla
- `@Id`: Marca el campo como clave primaria
- `@Column`: Personaliza el mapeo de la columna
- `@Transient`: Excluye un campo de la persistencia
- `@MappedCollection`: Define relaciones

**Ejemplo 1: Entidad simple**
```java
package com.example.domain;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("products")
public class Product {
    
    @Id
    private Long id;
    
    @Column("name")
    private String name;
    
    @Column("category")
    private String category;
    
    @Column("price")
    private Double price;
    
    @Column("created_at")
    private LocalDateTime createdAt;
    
    @Column("updated_at")
    private LocalDateTime updatedAt;
    
    // Constructor sin ID para nuevos productos
    public Product(String name, String category, Double price) {
        this.name = name;
        this.category = category;
        this.price = price;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
```

**Ejemplo 2: Entidad con relaciones (Aggregate Root)**
```java
package com.example.domain;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@Table("orders")
public class Order {
    
    @Id
    private Long id;
    
    @Column("customer_name")
    private String customerName;
    
    @Column("shipping_address")
    private String shippingAddress;
    
    @Column("order_date")
    private LocalDateTime orderDate;
    
    @Column("status")
    private OrderStatus status;
    
    // Relación uno a muchos
    // idColumn especifica la columna FK en order_items
    @MappedCollection(idColumn = "order_id")
    private Set<OrderItem> items = new HashSet<>();
    
    public Order(String customerName, String shippingAddress) {
        this.customerName = customerName;
        this.shippingAddress = shippingAddress;
        this.orderDate = LocalDateTime.now();
        this.status = OrderStatus.PENDING;
    }
    
    public void addItem(OrderItem item) {
        items.add(item);
    }
    
    public enum OrderStatus {
        PENDING, PROCESSING, SHIPPED, DELIVERED, CANCELLED
    }
}
```

```java
package com.example.domain;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("order_items")
public class OrderItem {
    
    @Id
    private Long id;
    
    // No necesita @Column si el nombre coincide con la BD
    // order_id se maneja automáticamente por @MappedCollection
    
    @Column("product_id")
    private Long productId;
    
    @Column("product_name")
    private String productName;
    
    @Column("quantity")
    private Integer quantity;
    
    @Column("unit_price")
    private Double unitPrice;
    
    public OrderItem(Long productId, String productName, Integer quantity, Double unitPrice) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }
    
    public double getSubtotal() {
        return quantity * unitPrice;
    }
}
```

**Ejemplo 3: Entidad con campos transitorios y calculados**
```java
package com.example.domain;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Data
@Table("users")
public class User {
    
    @Id
    private Long id;
    
    @Column("username")
    private String username;
    
    @Column("email")
    private String email;
    
    @Column("password")
    private String password;
    
    @Column("created_at")
    private LocalDateTime createdAt;
    
    @Column("last_login")
    private LocalDateTime lastLogin;
    
    @Column("active")
    private Boolean active;
    
    // Campo transitorio - no se persiste en BD
    @Transient
    private String confirmPassword;
    
    // Campo calculado - no se persiste
    @Transient
    public long getDaysSinceCreation() {
        return ChronoUnit.DAYS.between(createdAt, LocalDateTime.now());
    }
    
    @Transient
    public boolean hasLoggedInRecently() {
        if (lastLogin == null) return false;
        return ChronoUnit.DAYS.between(lastLogin, LocalDateTime.now()) < 30;
    }
}
```

---

### 3.2.4 Precargando datos con CommandLineRunner

`CommandLineRunner` ejecuta código después de que la aplicación Spring Boot se inicia.

**Ejemplo 1: Precarga básica**
```java
package com.example.config;

import com.example.domain.Product;
import com.example.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class DataLoader {
    
    @Bean
    public CommandLineRunner loadData(ProductRepository productRepository) {
        return args -> {
            log.info("Loading initial data...");
            
            productRepository.save(new Product("Laptop Dell XPS", "Electronics", 1299.99));
            productRepository.save(new Product("iPhone 14 Pro", "Electronics", 999.99));
            productRepository.save(new Product("Spring in Action", "Books", 49.99));
            productRepository.save(new Product("Clean Code", "Books", 39.99));
            productRepository.save(new Product("Nike Air Max", "Clothing", 129.99));
            
            long count = productRepository.count();
            log.info("Loaded {} products", count);
        };
    }
}
```

**Ejemplo 2: Precarga con múltiples repositorios**
```java
package com.example.config;

import com.example.domain.*;
import com.example.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.Arrays;

@Slf4j
@Configuration
@Profile("dev") // Solo en perfil de desarrollo
public class DevDataLoader {
    
    @Bean
    public CommandLineRunner loadDevData(
            ProductRepository productRepository,
            OrderRepository orderRepository) {
        
        return args -> {
            log.info("=== Loading DEV data ===");
            
            // 1. Cargar productos
            Product laptop = productRepository.save(
                new Product("Laptop Dell XPS", "Electronics", 1299.99)
            );
            Product phone = productRepository.save(
                new Product("iPhone 14 Pro", "Electronics", 999.99)
            );
            Product book = productRepository.save(
                new Product("Spring in Action", "Books", 49.99)
            );
            
            log.info("Loaded {} products", productRepository.count());
            
            // 2. Crear órdenes con items
            Order order1 = new Order("John Doe", "123 Main St, NY");
            order1.addItem(new OrderItem(laptop.getId(), laptop.getName(), 1, laptop.getPrice()));
            order1.addItem(new OrderItem(book.getId(), book.getName(), 2, book.getPrice()));
            orderRepository.save(order1);
            
            Order order2 = new Order("Jane Smith", "456 Oak Ave, LA");
            order2.addItem(new OrderItem(phone.getId(), phone.getName(), 1, phone.getPrice()));
            orderRepository.save(order2);
            
            log.info("Loaded {} orders", orderRepository.count());
            log.info("=== DEV data loading complete ===");
        };
    }
}
```

**Ejemplo 3: Precarga condicional con verificación**
```java
package com.example.config;

import com.example.domain.Product;
import com.example.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class SmartDataLoader {
    
    @Bean
    public CommandLineRunner smartLoadData(ProductRepository productRepository) {
        return args -> {
            // Solo cargar si la BD está vacía
            long count = productRepository.count();
            
            if (count > 0) {
                log.info("Database already contains {} products. Skipping data load.", count);
                return;
            }
            
            log.info("Database is empty. Loading initial data...");
            
            // Cargar datos por categorías
            loadElectronics(productRepository);
            loadBooks(productRepository);
            loadClothing(productRepository);
            
            log.info("Data loading complete. Total products: {}", productRepository.count());
        };
    }
    
    private void loadElectronics(ProductRepository repo) {
        repo.saveAll(Arrays.asList(
            new Product("Laptop Dell XPS 13", "Electronics", 1299.99),
            new Product("MacBook Pro 16", "Electronics", 2499.99),
            new Product("iPhone 14 Pro", "Electronics", 999.99),
            new Product("Samsung Galaxy S23", "Electronics", 899.99),
            new Product("Sony WH-1000XM5", "Electronics", 399.99)
        ));
        log.info("Loaded Electronics");
    }
    
    private void loadBooks(ProductRepository repo) {
        repo.saveAll(Arrays.asList(
            new Product("Spring in Action 6th", "Books", 49.99),
            new Product("Clean Code", "Books", 39.99),
            new Product("Design Patterns", "Books", 54.99),
            new Product("Effective Java", "Books", 45.99)
        ));
        log.info("Loaded Books");
    }
    
    private void loadClothing(ProductRepository repo) {
        repo.saveAll(Arrays.asList(
            new Product("Nike Air Max", "Clothing", 129.99),
            new Product("Adidas Ultraboost", "Clothing", 179.99),
            new Product("Levi's 501 Jeans", "Clothing", 89.99)
        ));
        log.info("Loaded Clothing");
    }
}
```

**Ejemplo 4: Múltiples CommandLineRunners con orden**
```java
package com.example.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Slf4j
@Configuration
public class OrderedDataLoader {
    
    @Bean
    @Order(1) // Se ejecuta primero
    public CommandLineRunner initSchema() {
        return args -> {
            log.info("Step 1: Initializing schema...");
            // Lógica de inicialización de esquema
        };
    }
    
    @Bean
    @Order(2) // Se ejecuta segundo
    public CommandLineRunner loadMasterData(ProductRepository productRepo) {
        return args -> {
            log.info("Step 2: Loading master data...");
            // Cargar datos maestros (categorías, tipos, etc.)
        };
    }
    
    @Bean
    @Order(3) // Se ejecuta tercero
    public CommandLineRunner loadTransactionalData(OrderRepository orderRepo) {
        return args -> {
            log.info("Step 3: Loading transactional data...");
            // Cargar datos transaccionales (órdenes, etc.)
        };
    }
    
    @Bean
    @Order(4) // Se ejecuta último
    public CommandLineRunner verifyData(ProductRepository productRepo, 
                                       OrderRepository orderRepo) {
        return args -> {
            log.info("Step 4: Verifying data integrity...");
            log.info("Products: {}", productRepo.count());
            log.info("Orders: {}", orderRepo.count());
            log.info("Data loading sequence complete!");
        };
    }
}
```

---

## 3.3 Persistiendo datos con Spring Data JPA

JPA (Java Persistence API) es el estándar para ORM en Java. Spring Data JPA simplifica su uso.

### Características de JPA:
- ORM completo (Object-Relational Mapping)
- Lazy loading
- Caché de primer y segundo nivel
- JPQL (Java Persistence Query Language)
- Criteria API para consultas dinámicas
- Manejo automático de relaciones

### 3.3.1 Añadiendo Spring Data JPA al proyecto

**Dependencia Maven:**
```xml
<dependencies>
    <!-- Spring Data JPA -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    
    <!-- Driver de base de datos -->
    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
        <scope>runtime</scope>
    </dependency>
    
    <!-- Para MySQL -->
    <!--
    <dependency>
        <groupId>mysql</groupId>
        <artifactId>mysql-connector-java</artifactId>
        <scope>runtime</scope>
    </dependency>
    -->
    
    <!-- Para PostgreSQL -->
    <!--
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>
    -->
</dependencies>
```

**Configuración application.properties:**
```properties
# ===== DATASOURCE =====
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# ===== JPA / HIBERNATE =====
# DDL auto (create, create-drop, update, validate, none)
spring.jpa.hibernate.ddl-auto=create-drop

# Dialecto de Hibernate
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect

# Mostrar SQL en consola
spring.jpa.show-sql=true

# Formatear SQL
spring.jpa.properties.hibernate.format_sql=true

# Logging de SQL y bindings
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE

# Estrategia de nombres (físicos)
spring.jpa.hibernate.naming.physical-strategy=org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl

# ===== H2 CONSOLE =====
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

**Configuración para MySQL:**
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/mydb?useSSL=false&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=secret
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

spring.jpa.database-platform=org.hibernate.dialect.MySQL8Dialect
spring.jpa.hibernate.ddl-auto=update
```

**Configuración para PostgreSQL:**
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/mydb
spring.datasource.username=postgres
spring.datasource.password=secret

spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=update
```

---

### 3.3.2 Anotando el dominio como entidades

JPA usa anotaciones extensivas para mapear clases a tablas de base de datos.

**Anotaciones JPA principales:**

| Anotación | Descripción |
|-----------|-------------|
| `@Entity` | Marca la clase como entidad JPA |
| `@Table` | Especifica el nombre de la tabla |
| `@Id` | Marca el campo como clave primaria |
| `@GeneratedValue` | Estrategia de generación de ID |
| `@Column` | Personaliza mapeo de columna |
| `@Temporal` | Tipo de campo temporal (Date/Time) |
| `@Enumerated` | Cómo persistir enums |
| `@Transient` | Excluye campo de persistencia |
| `@OneToOne` | Relación uno a uno |
| `@OneToMany` | Relación uno a muchos |
| `@ManyToOne` | Relación muchos a uno |
| `@ManyToMany` | Relación muchos a muchos |
| `@JoinColumn` | Especifica columna FK |
| `@JoinTable` | Tabla intermedia para ManyToMany |

**Ejemplo 1: Entidad básica**
```java
package com.example.domain;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "products")
public class Product {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "name", nullable = false, length = 100)
    private String name;
    
    @Column(name = "category", nullable = false)
    private String category;
    
    @Column(name = "price", nullable = false)
    private Double price;
    
    @Column(name = "description", length = 500)
    private String description;
    
    @Column(name = "stock_quantity")
    private Integer stockQuantity;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

**Ejemplo 2: Entidad con Enum**
```java
package com.example.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String username;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String password;
    
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;
    
    // ORDINAL: guarda 0, 1, 2... (no recomendado)
    // STRING: guarda "ADMIN", "USER", "MODERATOR" (recomendado)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "last_login")
    private LocalDateTime lastLogin;
    
    public enum Role {
        USER, MODERATOR, ADMIN
    }
    
    public enum AccountStatus {
        ACTIVE, INACTIVE, SUSPENDED, BANNED
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (role == null) role = Role.USER;
        if (status == null) status = AccountStatus.ACTIVE;
    }
}
```

**Ejemplo 3: Relación OneToMany y ManyToOne**
```java
package com.example.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@Entity
@Table(name = "orders")
public class Order {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "customer_name", nullable = false)
    private String customerName;
    
    @Column(name = "shipping_address")
    private String shippingAddress;
    
    @Column(name = "order_date")
    private LocalDateTime orderDate;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;
    
    // OneToMany: una orden tiene muchos items
    // cascade: operaciones en cascada (persist, merge, remove, etc.)
    // orphanRemoval: elimina items huérfanos
    // mappedBy: indica el campo dueño de la relación
    @OneToMany(
        mappedBy = "order",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    private List<OrderItem> items = new ArrayList<>();
    
    // Métodos helper para mantener sincronizadas ambas partes
    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }
    
    public void removeItem(OrderItem item) {
        items.remove(item);
        item.setOrder(null);
    }
    
    @PrePersist
    protected void onCreate() {
        orderDate = LocalDateTime.now();
        if (status == null) status = OrderStatus.PENDING;
    }
    
    public enum OrderStatus {
        PENDING, PROCESSING, SHIPPED, DELIVERED, CANCELLED
    }
}
```

```java
package com.example.domain;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.*;

@Data
@NoArgsConstructor
@Entity
@Table(name = "order_items")
@ToString(exclude = "order") // Evitar recursión infinita
public class OrderItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // ManyToOne: muchos items pertenecen a una orden
    // JoinColumn: especifica la columna FK
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;
    
    @Column(name = "product_name")
    private String productName;
    
    @Column(nullable = false)
    private Integer quantity;
    
    @Column(name = "unit_price", nullable = false)
    private Double unitPrice;
    
    public OrderItem(Product product, Integer quantity) {
        this.product = product;
        this.productName = product.getName();
        this.quantity = quantity;
        this.unitPrice = product.getPrice();
    }
    
    public double getSubtotal() {
        return quantity * unitPrice;
    }
}
```

**Ejemplo 4: Relación ManyToMany**
```java
package com.example.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@Entity
@Table(name = "students")
public class Student {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(unique = true)
    private String studentId;
    
    // ManyToMany: muchos estudiantes tienen muchos cursos
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "student_courses",              // Tabla intermedia
        joinColumns = @JoinColumn(name = "student_id"),      // FK a students
        inverseJoinColumns = @JoinColumn(name = "course_id") // FK a courses
    )
    private Set<Course> courses = new HashSet<>();
    
    public void addCourse(Course course) {
        courses.add(course);
        course.getStudents().add(this);
    }
    
    public void removeCourse(Course course) {
        courses.remove(course);
        course.getStudents().remove(this);
    }
}
```

```java
package com.example.domain;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@Entity
@Table(name = "courses")
@ToString(exclude = "students") // Evitar recursión
public class Course {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(unique = true)
    private String courseCode;
    
    private Integer credits;
    
    // Lado inverso de la relación
    @ManyToMany(mappedBy = "courses", fetch = FetchType.LAZY)
    private Set<Student> students = new HashSet<>();
}
```

**Ejemplo 5: Herencia con estrategias**

**Single Table (una tabla para todas las clases):**
```java
@Entity
@Table(name = "vehicles")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "vehicle_type", discriminatorType = DiscriminatorType.STRING)
public abstract class Vehicle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String brand;
    private String model;
}

@Entity
@DiscriminatorValue("CAR")
public class Car extends Vehicle {
    private Integer doors;
}

@Entity
@DiscriminatorValue("MOTORCYCLE")
public class Motorcycle extends Vehicle {
    private Boolean hasSidecar;
}
```

**Joined (tabla para cada clase):**
```java
@Entity
@Table(name = "vehicles")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Vehicle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String brand;
}

@Entity
@Table(name = "cars")
public class Car extends Vehicle {
    private Integer doors;
}

@Entity
@Table(name = "motorcycles")
public class Motorcycle extends Vehicle {
    private Boolean hasSidecar;
}
```

---

### 3.3.3 Declarando repositorios JPA

Similar a Spring Data JDBC, pero con más capacidades.

**Ejemplo 1: Repositorio básico**
```java
package com.example.repository;

import com.example.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    // JpaRepository hereda de:
    // - CrudRepository: operaciones CRUD básicas
    // - PagingAndSortingRepository: paginación y ordenamiento
    // Y añade:
    // - flush()
    // - saveAndFlush()
    // - deleteInBatch()
    // - findAll() con Sort
    // - getOne() (lazy loading)
    
    // Métodos de consulta derivados del nombre
    List<Product> findByCategory(String category);
    List<Product> findByNameContaining(String name);
    List<Product> findByPriceBetween(Double minPrice, Double maxPrice);
    List<Product> findByCategoryOrderByPriceAsc(String category);
}
```

**Ejemplo 2: Repositorio con consultas JPQL**
```java
package com.example.repository;

import com.example.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    // JPQL básica
    @Query("SELECT o FROM Order o WHERE o.customerName = :name")
    List<Order> findByCustomerName(@Param("name") String name);
    
    // JPQL con JOIN FETCH (evita N+1 queries)
    @Query("SELECT DISTINCT o FROM Order o JOIN FETCH o.items WHERE o.id = :id")
    Order findByIdWithItems(@Param("id") Long id);
    
    // JPQL con múltiples condiciones
    @Query("SELECT o FROM Order o WHERE o.orderDate BETWEEN :start AND :end " +
           "AND o.status = :status")
    List<Order> findByDateRangeAndStatus(
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end,
        @Param("status") Order.OrderStatus status
    );
    
    // SQL nativo
    @Query(value = "SELECT * FROM orders WHERE customer_name LIKE %:keyword%", 
           nativeQuery = true)
    List<Order> searchByCustomerNative(@Param("keyword") String keyword);
    
    // Consulta de agregación
    @Query("SELECT o.status, COUNT(o) FROM Order o GROUP BY o.status")
    List<Object[]> countByStatus();
    
    // DTO projection
    @Query("SELECT new com.example.dto.OrderSummary(o.id, o.customerName, o.orderDate) " +
           "FROM Order o WHERE o.status = :status")
    List<OrderSummary> findSummariesByStatus(@Param("status") Order.OrderStatus status);
}
```

**Ejemplo 3: Repositorio con Specifications (Criteria API):**
```java
package com.example.repository;

import com.example.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductSpecRepository extends JpaRepository<Product, Long>,
                                               JpaSpecificationExecutor<Product> {
    // JpaSpecificationExecutor añade:
    // - findOne(Specification)
    // - findAll(Specification)
    // - findAll(Specification, Pageable)
    // - findAll(Specification, Sort)
    // - count(Specification)
}
```

```java
package com.example.specification;

import com.example.domain.Product;
import org.springframework.data.jpa.domain.Specification;

public class ProductSpecifications {
    
    public static Specification<Product> hasCategory(String category) {
        return (root, query, cb) -> 
            category == null ? null : cb.equal(root.get("category"), category);
    }
    
    public static Specification<Product> priceBetween(Double minPrice, Double maxPrice) {
        return (root, query, cb) -> 
            cb.between(root.get("price"), minPrice, maxPrice);
    }
    
    public static Specification<Product> nameContains(String keyword) {
        return (root, query, cb) -> 
            cb.like(cb.lower(root.get("name")), "%" + keyword.toLowerCase() + "%");
    }
}
```

**Uso en servicio:**
```java
@Service
public class ProductService {
    
    private final ProductSpecRepository repository;
    
    public List<Product> searchProducts(String category, Double minPrice, 
                                        Double maxPrice, String keyword) {
        Specification<Product> spec = Specification.where(null);
        
        if (category != null) {
            spec = spec.and(ProductSpecifications.hasCategory(category));
        }
        
        if (minPrice != null && maxPrice != null) {
            spec = spec.and(ProductSpecifications.priceBetween(minPrice, maxPrice));
        }
        
        if (keyword != null) {
            spec = spec.and(ProductSpecifications.nameContains(keyword));
        }
        
        return repository.findAll(spec);
    }
}
```

---

### 3.3.4 Personalizando repositorios

Spring Data JPA permite personalizar consultas usando la convención de nombres de métodos con operadores especiales.

#### Operadores disponibles en nombres de métodos

**Operadores de comparación:**

| Operador | Ejemplo | SQL Equivalente |
|----------|---------|-----------------|
| `Is`, `Equals` | `findByName(String name)` | `WHERE name = ?` |
| `IsNot`, `Not` | `findByActiveNot(boolean active)` | `WHERE active <> ?` |
| `IsNull`, `Null` | `findByDescriptionNull()` | `WHERE description IS NULL` |
| `IsNotNull`, `NotNull` | `findByDescriptionNotNull()` | `WHERE description IS NOT NULL` |

**Operadores de comparación numérica/fecha:**

| Operador | Ejemplo | SQL Equivalente |
|----------|---------|-----------------|
| `IsGreaterThan`, `GreaterThan` | `findByPriceGreaterThan(double price)` | `WHERE price > ?` |
| `IsGreaterThanEqual`, `GreaterThanEqual` | `findByPriceGreaterThanEqual(double price)` | `WHERE price >= ?` |
| `IsLessThan`, `LessThan` | `findByPriceLessThan(double price)` | `WHERE price < ?` |
| `IsLessThanEqual`, `LessThanEqual` | `findByPriceLessThanEqual(double price)` | `WHERE price <= ?` |
| `IsBetween`, `Between` | `findByPriceBetween(double min, double max)` | `WHERE price BETWEEN ? AND ?` |
| `IsAfter`, `After` | `findByCreatedAtAfter(LocalDateTime date)` | `WHERE created_at > ?` |
| `IsBefore`, `Before` | `findByCreatedAtBefore(LocalDateTime date)` | `WHERE created_at < ?` |

**Operadores de colecciones:**

| Operador | Ejemplo | SQL Equivalente |
|----------|---------|-----------------|
| `IsIn`, `In` | `findByCategoryIn(List<String> categories)` | `WHERE category IN (?)` |
| `IsNotIn`, `NotIn` | `findByCategoryNotIn(List<String> categories)` | `WHERE category NOT IN (?)` |

**Operadores de texto:**

| Operador | Ejemplo | SQL Equivalente |
|----------|---------|-----------------|
| `IsStartingWith`, `StartingWith`, `StartsWith` | `findByNameStartingWith(String prefix)` | `WHERE name LIKE '?%'` |
| `IsEndingWith`, `EndingWith`, `EndsWith` | `findByNameEndingWith(String suffix)` | `WHERE name LIKE '%?'` |
| `IsContaining`, `Containing`, `Contains` | `findByNameContaining(String str)` | `WHERE name LIKE '%?%'` |
| `IsLike`, `Like` | `findByNameLike(String pattern)` | `WHERE name LIKE ?` |
| `IsNotLike`, `NotLike` | `findByNameNotLike(String pattern)` | `WHERE name NOT LIKE ?` |

**Operadores booleanos:**

| Operador | Ejemplo | SQL Equivalente |
|----------|---------|-----------------|
| `IsTrue`, `True` | `findByActiveTrue()` | `WHERE active = TRUE` |
| `IsFalse`, `False` | `findByActiveFalse()` | `WHERE active = FALSE` |

**Case sensitivity:**

| Operador | Ejemplo | Descripción |
|----------|---------|-------------|
| `IgnoringCase`, `IgnoresCase` | `findByNameIgnoringCase(String name)` | Búsqueda case-insensitive |
| `AllIgnoringCase`, `AllIgnoresCase` | `findByNameAndEmailAllIgnoringCase(...)` | Todos los parámetros case-insensitive |

**Ejemplo completo: Repositorio con todos los operadores**
```java
package com.example.repository;

import com.example.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AdvancedProductRepository extends JpaRepository<Product, Long> {
    
    // ===== COMPARACIÓN BÁSICA =====
    Optional<Product> findByName(String name);
    List<Product> findByNameNot(String name);
    List<Product> findByActiveTrue();
    List<Product> findByActiveFalse();
    
    // ===== NULL CHECKS =====
    List<Product> findByDescriptionNull();
    List<Product> findByDescriptionNotNull();
    
    // ===== COMPARACIÓN NUMÉRICA =====
    List<Product> findByPriceGreaterThan(Double price);
    List<Product> findByPriceGreaterThanEqual(Double price);
    List<Product> findByPriceLessThan(Double price);
    List<Product> findByPriceLessThanEqual(Double price);
    List<Product> findByPriceBetween(Double minPrice, Double maxPrice);
    
    // ===== FECHAS =====
    List<Product> findByCreatedAtAfter(LocalDateTime date);
    List<Product> findByCreatedAtBefore(LocalDateTime date);
    List<Product> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    
    // ===== COLECCIONES =====
    List<Product> findByCategoryIn(List<String> categories);
    List<Product> findByCategoryNotIn(List<String> categories);
    
    // ===== TEXTO =====
    List<Product> findByNameStartingWith(String prefix);
    List<Product> findByNameEndingWith(String suffix);
    List<Product> findByNameContaining(String infix);
    List<Product> findByNameLike(String pattern);
    List<Product> findByNameNotLike(String pattern);
    
    // ===== CASE INSENSITIVE =====
    Optional<Product> findByNameIgnoreCase(String name);
    List<Product> findByNameContainingIgnoreCase(String name);
    List<Product> findByNameAndCategoryAllIgnoringCase(String name, String category);
    
    // ===== COMBINACIONES CON AND/OR =====
    List<Product> findByNameAndCategory(String name, String category);
    List<Product> findByNameOrCategory(String name, String category);
    List<Product> findByCategoryAndPriceGreaterThan(String category, Double price);
    List<Product> findByCategoryAndPriceBetween(String category, Double min, Double max);
    
    // ===== ORDENAMIENTO =====
    List<Product> findByCategoryOrderByPriceAsc(String category);
    List<Product> findByCategoryOrderByPriceDesc(String category);
    List<Product> findByCategoryOrderByNameAscPriceDesc(String category);
    
    // ===== TOP/FIRST =====
    Optional<Product> findFirstByOrderByPriceDesc();
    List<Product> findTop5ByOrderByPriceDesc();
    List<Product> findTop10ByCategoryOrderByCreatedAtDesc(String category);
    Optional<Product> findFirstByCategoryOrderByPriceAsc(String category);
    
    // ===== DISTINCT =====
    List<Product> findDistinctByCategory(String category);
    
    // ===== PAGINACIÓN Y ORDENAMIENTO =====
    Page<Product> findByCategory(String category, Pageable pageable);
    List<Product> findByPriceGreaterThan(Double price, Sort sort);
    
    // ===== DELETE Y COUNT =====
    Long deleteByCategory(String category);
    Long countByCategory(String category);
    boolean existsByName(String name);
    
    // ===== CONSULTAS COMPLEJAS =====
    List<Product> findByCategoryAndPriceBetweenAndStockQuantityGreaterThan(
        String category, Double minPrice, Double maxPrice, Integer minStock
    );
    
    Page<Product> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
        String nameKeyword, String descKeyword, Pageable pageable
    );
    
    List<Product> findTop10ByCategoryInAndPriceLessThanOrderByCreatedAtDesc(
        List<String> categories, Double maxPrice
    );
}
```

**Ejemplo de uso en servicio:**
```java
package com.example.service;

import com.example.domain.Product;
import com.example.repository.AdvancedProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class ProductSearchService {
    
    private final AdvancedProductRepository repository;
    
    public ProductSearchService(AdvancedProductRepository repository) {
        this.repository = repository;
    }
    
    public List<Product> findAffordableProducts(Double maxPrice) {
        return repository.findByPriceLessThan(maxPrice);
    }
    
    public List<Product> findInPriceRange(Double min, Double max) {
        return repository.findByPriceBetween(min, max);
    }
    
    public Page<Product> searchProducts(String keyword, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, 
                                                 Sort.by("name").ascending());
        return repository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
            keyword, keyword, pageRequest
        );
    }
    
    public List<Product> findNewArrivals(int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return repository.findByCreatedAtAfter(since);
    }
    
    public List<Product> findBestSellersInCategories(List<String> categories, Double maxPrice) {
        return repository.findTop10ByCategoryInAndPriceLessThanOrderByCreatedAtDesc(
            categories, maxPrice
        );
    }
    
    public List<Product> findProductsByCategories(String... categories) {
        return repository.findByCategoryIn(Arrays.asList(categories));
    }
    
    public long deleteOldProducts(String category, LocalDateTime before) {
        List<Product> oldProducts = repository
            .findByCategoryAndCreatedAtBefore(category, before);
        
        repository.deleteAll(oldProducts);
        return oldProducts.size();
    }
}
```

---

## Comparación de enfoques

### JDBC Template vs Spring Data JDBC vs Spring Data JPA

| Característica | JDBC Template | Spring Data JDBC | Spring Data JPA |
|----------------|---------------|------------------|-----------------|
| **Control SQL** | Total | Alto | Medio |
| **Código boilerplate** | Medio | Bajo | Muy bajo |
| **Curva de aprendizaje** | Baja | Media | Alta |
| **Rendimiento** | Excelente | Excelente | Bueno |
| **Consultas complejas** | Fácil | Medio | Complejo (JPQL) |
| **Lazy loading** | No | No | Sí |
| **Caché** | No | No | Sí (1° y 2° nivel) |
| **Relaciones** | Manual | Agregados | Automático |
| **Testing** | Fácil | Fácil | Medio |
| **Uso recomendado** | Consultas SQL complejas | Aplicaciones simples-medianas | Aplicaciones empresariales complejas |

### ¿Cuándo usar cada uno?

**Usar JDBC Template cuando:**
- Necesitas control total del SQL
- Trabajas con procedimientos almacenados complejos
- Optimización de rendimiento es crítica
- Consultas muy específicas de la BD

**Usar Spring Data JDBC cuando:**
- Quieres simplicidad sin "magia"
- No necesitas lazy loading
- Prefieres agregados sobre relaciones complejas
- Proyectos medianos con requisitos claros

**Usar Spring Data JPA cuando:**
- Necesitas ORM completo
- Relaciones complejas entre entidades
- Portabilidad entre bases de datos
- Proyectos empresariales grandes
- Necesitas caché y lazy loading

---

## Mejores prácticas

### 1. Diseño de entidades

```java
// ✅ BUENO
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_email", columnList = "email"),
    @Index(name = "idx_username", columnList = "username")
})
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // Campos con restricciones claras
    @Column(nullable = false, unique = true, length = 50)
    private String username;
    
    // ToString excluye relaciones para evitar recursión
    @ToString.Exclude
    @OneToMany(mappedBy = "user")
    private List<Order> orders;
}

// ❌ MALO
@Entity
public class User {
    @Id
    private Long id; // Sin estrategia de generación
    
    private String username; // Sin restricciones
    
    @OneToMany
    private List<Order> orders; // Puede causar N+1 queries
}
```

### 2. Manejo de relaciones

```java
// ✅ BUENO: Métodos helper para mantener sincronización
public class Order {
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();
    
    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }
    
    public void removeItem(OrderItem item) {
        items.remove(item);
        item.setOrder(null);
    }
}

// ❌ MALO: Modificación directa sin sincronización
order.getItems().add(newItem); // No setea order en newItem
```

### 3. Evitar N+1 queries

```java
// ✅ BUENO: JOIN FETCH
@Query("SELECT DISTINCT o FROM Order o JOIN FETCH o.items WHERE o.id = :id")
Order findByIdWithItems(@Param("id") Long id);

// ✅ BUENO: EntityGraph
@EntityGraph(attributePaths = {"items", "customer"})
Order findById(Long id);

// ❌ MALO: Lazy loading en loop
List<Order> orders = orderRepository.findAll();
for (Order order : orders) {
    order.getItems().size(); // ¡Query por cada orden!
}
```

### 4. Transacciones

```java
// ✅ BUENO
@Service
@Transactional(readOnly = true) // Por defecto read-only
public class OrderService {
    
    @Transactional // Override para escritura
    public Order createOrder(Order order) {
        // Toda la lógica en una transacción
        return orderRepository.save(order);
    }
    
    public List<Order> findAll() {
        // Read-only es más eficiente
        return orderRepository.findAll();
    }
}
```

---

## Resumen

**Flujo completo de persistencia con Spring Data JPA:**

1. **Definir entidades** con anotaciones JPA
2. **Crear repositorios** extendiendo JpaRepository
3. **Inyectar repositorios** en servicios
4. **Usar métodos** (generados o personalizados)
5. **Gestionar transacciones** con @Transactional

**Recuerda:**
- Usar @Entity y @Id es obligatorio
- CrudRepository para CRUD básico
- JpaRepository para funcionalidad completa
- Métodos derivados del nombre son muy potentes
- JPQL para consultas complejas
- @Transactional para integridad de datos

---

*Esta guía está basada en Spring in Action 6th Edition y cubre Spring Boot 2.5+ con Java 11+.*
