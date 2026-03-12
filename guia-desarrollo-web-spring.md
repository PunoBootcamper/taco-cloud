# Guía de Desarrollo de Aplicaciones Web con Spring

## 2. Desarrollando aplicaciones web

Esta guía cubre los conceptos fundamentales para desarrollar aplicaciones web usando Spring MVC, incluyendo la presentación de información, procesamiento de formularios, validación y configuración de vistas.

---

## 2.1 Mostrando información

### 2.1.1 Estableciendo el dominio

El primer paso en cualquier aplicación es definir las **clases del dominio** que representan los datos de negocio. Estas clases son POJOs (Plain Old Java Objects) que encapsulan los datos que la aplicación manejará.

**Características clave:**
- Son clases simples con propiedades y métodos getter/setter
- Pueden usar Lombok para reducir código boilerplate (`@Data`)
- Representan las entidades de negocio

**Ejemplo 1: Clase de dominio simple**
```java
package com.example.domain;

import lombok.Data;
import lombok.AllArgsConstructor;

@Data
@AllArgsConstructor
public class Product {
    private String id;
    private String name;
    private String category;
    private double price;
}
```

**Ejemplo 2: Clase de dominio con enumeración**
```java
package com.example.domain;

import lombok.Data;
import lombok.AllArgsConstructor;

@Data
@AllArgsConstructor
public class Book {
    private String isbn;
    private String title;
    private String author;
    private Genre genre;
    
    public enum Genre {
        FICTION, NON_FICTION, SCIENCE, HISTORY, BIOGRAPHY
    }
}
```

**Ejemplo 3: Clase de dominio con relaciones**
```java
package com.example.domain;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class ShoppingCart {
    private String customerId;
    private String customerName;
    private List<Product> items = new ArrayList<>();
    
    public void addItem(Product product) {
        items.add(product);
    }
    
    public double getTotalPrice() {
        return items.stream()
                    .mapToDouble(Product::getPrice)
                    .sum();
    }
}
```

---

### 2.1.2 Creando una clase controladora

Los **controladores** son el punto de entrada para las peticiones HTTP. Spring MVC usa anotaciones para mapear rutas a métodos.

#### Anotaciones principales:

| Anotación | Propósito |
|-----------|-----------|
| `@Controller` | Marca la clase como controlador MVC |
| `@RequestMapping` | Define la ruta base del controlador |
| `@GetMapping` | Maneja peticiones HTTP GET |
| `@PostMapping` | Maneja peticiones HTTP POST |
| `@ModelAttribute` | Añade datos al modelo antes de cada petición |

#### @Slf4j - Logging simplificado

La anotación `@Slf4j` de Lombok genera automáticamente un logger:

```java
@Slf4j
@Controller
public class ProductController {
    
    @GetMapping("/products")
    public String listProducts() {
        log.info("Listing all products");  // log está disponible automáticamente
        log.debug("Debug information");
        log.error("Error occurred");
        return "products";
    }
}
```

**Equivalente sin @Slf4j:**
```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class ProductController {
    private static final Logger log = LoggerFactory.getLogger(ProductController.class);
    
    @GetMapping("/products")
    public String listProducts() {
        log.info("Listing all products");
        return "products";
    }
}
```

#### @ModelAttribute - Poblando el modelo

`@ModelAttribute` en un método se ejecuta **antes de cada petición** del controlador y añade atributos al modelo.

**Ejemplo 1: Añadiendo datos de referencia**
```java
@Slf4j
@Controller
@RequestMapping("/books")
public class BookController {
    
    @ModelAttribute
    public void addCommonAttributes(Model model) {
        // Este método se ejecuta antes de showBookForm() o cualquier otro método
        List<String> genres = Arrays.asList("Fiction", "Non-Fiction", "Science", "History");
        model.addAttribute("availableGenres", genres);
        
        log.debug("Added {} genres to model", genres.size());
    }
    
    @GetMapping("/new")
    public String showBookForm(Model model) {
        // availableGenres ya está disponible en el modelo
        model.addAttribute("book", new Book());
        return "bookForm";
    }
}
```

**Ejemplo 2: Inicializando objetos en el modelo**
```java
@Controller
@RequestMapping("/shopping")
@SessionAttributes("cart")  // Mantiene cart en la sesión
public class ShoppingController {
    
    @ModelAttribute(name = "cart")
    public ShoppingCart createCart() {
        // Crea un nuevo carrito o recupera uno existente de la sesión
        return new ShoppingCart();
    }
    
    @ModelAttribute
    public void addCategories(Model model) {
        List<String> categories = Arrays.asList("Electronics", "Books", "Clothing");
        model.addAttribute("categories", categories);
    }
    
    @GetMapping("/browse")
    public String browse() {
        return "browse";
    }
}
```

**Ejemplo 3: Controlador completo con inyección de dependencias**
```java
@Slf4j
@Controller
@RequestMapping("/products")
public class ProductController {
    
    private final ProductService productService;
    
    // Inyección por constructor (recomendado)
    public ProductController(ProductService productService) {
        this.productService = productService;
    }
    
    @ModelAttribute
    public void addCategoriesAndStats(Model model) {
        // Datos que se necesitan en todas las vistas
        model.addAttribute("categories", productService.getAllCategories());
        model.addAttribute("totalProducts", productService.getTotalCount());
    }
    
    @GetMapping
    public String showAllProducts(Model model) {
        log.info("Fetching all products");
        model.addAttribute("products", productService.findAll());
        return "productList";
    }
    
    @GetMapping("/{category}")
    public String showByCategory(@PathVariable String category, Model model) {
        log.info("Fetching products for category: {}", category);
        model.addAttribute("products", productService.findByCategory(category));
        model.addAttribute("selectedCategory", category);
        return "productList";
    }
}
```

---

### 2.1.3 Diseñando la vista

Spring Boot soporta varias tecnologías de vistas, siendo **Thymeleaf** la más popular para aplicaciones Spring modernas.

#### Conceptos básicos de Thymeleaf

**1. Namespace y configuración**
```html
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml"
      xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Mi Aplicación</title>
    <link rel="stylesheet" th:href="@{/css/styles.css}" />
</head>
<body>
    <!-- Contenido -->
</body>
</html>
```

**2. Expresiones Thymeleaf**

| Expresión | Descripción | Ejemplo |
|-----------|-------------|---------|
| `${...}` | Variables del modelo | `${product.name}` |
| `*{...}` | Variables del objeto seleccionado | `*{name}` (con th:object) |
| `@{...}` | URLs | `@{/products}` |
| `#{...}` | Mensajes i18n | `#{label.welcome}` |
| `~{...}` | Fragmentos | `~{fragments :: header}` |

**3. Referenciando recursos estáticos**

Los recursos estáticos se colocan en `src/main/resources/static/`:

```
static/
├── css/
│   └── styles.css
├── js/
│   └── app.js
└── images/
    └── logo.png
```

**En Thymeleaf:**
```html
<!-- CSS -->
<link rel="stylesheet" th:href="@{/css/styles.css}" />

<!-- JavaScript -->
<script th:src="@{/js/app.js}"></script>

<!-- Imágenes -->
<img th:src="@{/images/logo.png}" alt="Logo"/>

<!-- Con subdirectorios -->
<img th:src="@{/images/products/laptop.jpg}" alt="Laptop"/>
```

**Nota importante:** La expresión `@{...}` es específica de Thymeleaf y maneja automáticamente:
- El context path de la aplicación
- URLs absolutas y relativas
- Parámetros de URL

**Ejemplo completo: Lista de productos**
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Productos</title>
    <link rel="stylesheet" th:href="@{/css/products.css}" />
</head>
<body>
    <header>
        <img th:src="@{/images/store-logo.png}" alt="Store Logo" class="logo"/>
        <h1>Catálogo de Productos</h1>
    </header>
    
    <!-- Mostrar categorías desde @ModelAttribute -->
    <nav>
        <h3>Categorías</h3>
        <ul>
            <li th:each="category : ${categories}">
                <a th:href="@{/products/{cat}(cat=${category})}" 
                   th:text="${category}">Category</a>
            </li>
        </ul>
    </nav>
    
    <!-- Lista de productos -->
    <div class="product-grid">
        <div class="product-card" th:each="product : ${products}">
            <img th:src="@{'/images/products/' + ${product.id} + '.jpg'}" 
                 th:alt="${product.name}"/>
            <h3 th:text="${product.name}">Product Name</h3>
            <p class="category" th:text="${product.category}">Category</p>
            <p class="price" th:text="'$' + ${product.price}">$0.00</p>
            <a th:href="@{/products/{id}(id=${product.id})}" 
               class="btn">Ver detalles</a>
        </div>
    </div>
    
    <!-- Mensaje condicional -->
    <div th:if="${#lists.isEmpty(products)}" class="empty-message">
        <p>No hay productos disponibles en esta categoría.</p>
    </div>
</body>
</html>
```

**Ejemplo: Formulario con th:object**
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Nuevo Producto</title>
    <link rel="stylesheet" th:href="@{/css/forms.css}" />
</head>
<body>
    <h1>Agregar Producto</h1>
    
    <form method="POST" th:action="@{/products}" th:object="${product}">
        <div class="form-group">
            <label for="name">Nombre:</label>
            <input type="text" id="name" th:field="*{name}" />
        </div>
        
        <div class="form-group">
            <label for="category">Categoría:</label>
            <select id="category" th:field="*{category}">
                <option value="">Seleccione...</option>
                <option th:each="cat : ${categories}" 
                        th:value="${cat}" 
                        th:text="${cat}">Category</option>
            </select>
        </div>
        
        <div class="form-group">
            <label for="price">Precio:</label>
            <input type="number" id="price" th:field="*{price}" step="0.01" />
        </div>
        
        <button type="submit">Guardar</button>
    </form>
</body>
</html>
```

---

## 2.2 Procesamiento de envío de formularios

Cuando un formulario se envía, Spring MVC vincula automáticamente los datos del formulario a objetos Java.

### Diferencia crucial: Parámetros con y sin @ModelAttribute

| Parámetro | Anotación | Comportamiento |
|-----------|-----------|----------------|
| `Product product` | Ninguna | Spring crea un **nuevo objeto** vacío y lo puebla con los datos del formulario |
| `ShoppingCart cart` | `@ModelAttribute` | Spring recupera un **objeto existente** del modelo o sesión, no crea uno nuevo |

**Ejemplo 1: Objeto nuevo sin @ModelAttribute**
```java
@Controller
@RequestMapping("/products")
public class ProductController {
    
    @GetMapping("/new")
    public String showNewProductForm(Model model) {
        model.addAttribute("product", new Product());
        return "productForm";
    }
    
    @PostMapping
    public String saveProduct(Product product) {
        // product es un NUEVO objeto creado desde los datos del formulario
        // Spring crea new Product() y setea sus propiedades
        log.info("New product: {}", product);
        
        // Guardar en base de datos, etc.
        productService.save(product);
        
        return "redirect:/products";
    }
}
```

**Ejemplo 2: Objeto existente con @ModelAttribute**
```java
@Controller
@RequestMapping("/shopping")
@SessionAttributes("cart")  // Importante: mantiene cart en la sesión
public class ShoppingController {
    
    @ModelAttribute(name = "cart")
    public ShoppingCart initCart() {
        // Se ejecuta al inicio, crea el carrito
        return new ShoppingCart();
    }
    
    @GetMapping("/add-item")
    public String showAddItemForm(Model model) {
        model.addAttribute("product", new Product());
        return "addItemForm";
    }
    
    @PostMapping("/add-item")
    public String addItemToCart(Product product, 
                               @ModelAttribute ShoppingCart cart) {
        // product: NUEVO objeto creado desde el formulario
        // cart: objeto EXISTENTE recuperado de la sesión
        
        cart.addItem(product);  // Modifica el carrito existente
        log.info("Added {} to cart. Total items: {}", 
                 product.getName(), cart.getItems().size());
        
        return "redirect:/shopping/cart";
    }
    
    @GetMapping("/cart")
    public String viewCart(@ModelAttribute ShoppingCart cart, Model model) {
        // cart: el mismo objeto que hemos ido modificando
        model.addAttribute("totalPrice", cart.getTotalPrice());
        return "cartView";
    }
    
    @PostMapping("/checkout")
    public String checkout(@ModelAttribute ShoppingCart cart,
                          SessionStatus sessionStatus) {
        // Procesar el pedido
        orderService.processOrder(cart);
        
        // Limpiar la sesión
        sessionStatus.setComplete();
        
        return "redirect:/shopping/confirmation";
    }
}
```

**Ejemplo 3: Caso práctico completo - Wizard multi-paso**
```java
@Slf4j
@Controller
@RequestMapping("/registration")
@SessionAttributes("registration")
public class RegistrationController {
    
    // Paso 1: Inicializar objeto de registro en sesión
    @ModelAttribute(name = "registration")
    public UserRegistration createRegistration() {
        return new UserRegistration();
    }
    
    // Paso 1: Información personal
    @GetMapping("/step1")
    public String showStep1() {
        return "registration/step1";
    }
    
    @PostMapping("/step1")
    public String processStep1(
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam String email,
            @ModelAttribute UserRegistration registration) {
        
        // registration: objeto existente de la sesión
        registration.setFirstName(firstName);
        registration.setLastName(lastName);
        registration.setEmail(email);
        
        log.info("Step 1 completed for {}", email);
        return "redirect:/registration/step2";
    }
    
    // Paso 2: Dirección
    @GetMapping("/step2")
    public String showStep2(@ModelAttribute UserRegistration registration) {
        // Verificar que step1 esté completo
        if (registration.getEmail() == null) {
            return "redirect:/registration/step1";
        }
        return "registration/step2";
    }
    
    @PostMapping("/step2")
    public String processStep2(
            @RequestParam String street,
            @RequestParam String city,
            @RequestParam String zipCode,
            @ModelAttribute UserRegistration registration) {
        
        // Continúa modificando el mismo objeto
        registration.setStreet(street);
        registration.setCity(city);
        registration.setZipCode(zipCode);
        
        log.info("Step 2 completed for {}", registration.getEmail());
        return "redirect:/registration/step3";
    }
    
    // Paso 3: Confirmación y guardado
    @GetMapping("/step3")
    public String showStep3(@ModelAttribute UserRegistration registration) {
        if (registration.getCity() == null) {
            return "redirect:/registration/step2";
        }
        return "registration/step3";
    }
    
    @PostMapping("/complete")
    public String completeRegistration(
            @ModelAttribute UserRegistration registration,
            SessionStatus sessionStatus) {
        
        // Guardar el registro completo
        userService.register(registration);
        
        log.info("Registration completed for {}", registration.getEmail());
        
        // Limpiar sesión
        sessionStatus.setComplete();
        
        return "redirect:/registration/success";
    }
}
```

**Flujo de datos en el wizard:**
1. `@ModelAttribute` en método crea `UserRegistration` en sesión
2. Cada POST modifica el **mismo objeto** en sesión
3. El objeto acumula datos a través de múltiples pasos
4. `sessionStatus.setComplete()` limpia la sesión al final

---

## 2.3 Validación de entrada de formularios

La validación asegura que los datos cumplan con las reglas de negocio antes de procesarlos.

### 2.3.1 Declarando reglas de validación

Spring usa **Bean Validation (JSR-380)** con anotaciones en las clases de dominio.

**Dependencia Maven:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

#### Anotaciones de validación comunes

| Anotación | Descripción | Ejemplo |
|-----------|-------------|---------|
| `@NotNull` | No puede ser null | `@NotNull private String name;` |
| `@NotBlank` | No null, no vacío, no solo espacios | `@NotBlank private String email;` |
| `@NotEmpty` | No null, no vacío (para colecciones) | `@NotEmpty private List<Item> items;` |
| `@Size` | Tamaño de String o colección | `@Size(min=2, max=50)` |
| `@Min` / `@Max` | Valor numérico mínimo/máximo | `@Min(0) private int quantity;` |
| `@Email` | Formato de email válido | `@Email private String email;` |
| `@Pattern` | Expresión regular | `@Pattern(regexp="[0-9]{3}-[0-9]{4}")` |
| `@CreditCardNumber` | Número de tarjeta válido (Hibernate Validator) | `@CreditCardNumber private String card;` |
| `@Digits` | Dígitos enteros y fraccionarios | `@Digits(integer=3, fraction=0)` |

**Ejemplo 1: Validación básica de producto**
```java
package com.example.domain;

import lombok.Data;
import javax.validation.constraints.*;

@Data
public class Product {
    
    @NotBlank(message = "El ID es obligatorio")
    @Size(min = 3, max = 20, message = "El ID debe tener entre 3 y 20 caracteres")
    private String id;
    
    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 5, max = 100, message = "El nombre debe tener entre 5 y 100 caracteres")
    private String name;
    
    @NotBlank(message = "La categoría es obligatoria")
    private String category;
    
    @NotNull(message = "El precio es obligatorio")
    @Min(value = 0, message = "El precio debe ser mayor o igual a 0")
    @Max(value = 10000, message = "El precio no puede exceder 10000")
    private Double price;
    
    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "Debe haber al menos 1 unidad")
    private Integer quantity;
    
    @Size(max = 500, message = "La descripción no puede exceder 500 caracteres")
    private String description;
}
```

**Ejemplo 2: Validación de usuario**
```java
package com.example.domain;

import lombok.Data;
import javax.validation.constraints.*;

@Data
public class User {
    
    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Size(min = 4, max = 20, message = "El nombre debe tener entre 4 y 20 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", 
             message = "Solo letras, números y guión bajo permitidos")
    private String username;
    
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Email no válido")
    private String email;
    
    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    private String password;
    
    @NotNull(message = "La edad es obligatoria")
    @Min(value = 18, message = "Debe ser mayor de 18 años")
    @Max(value = 120, message = "Edad no válida")
    private Integer age;
    
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", 
             message = "Número de teléfono no válido")
    private String phone;
}
```

**Ejemplo 3: Validación de orden con tarjeta de crédito**
```java
package com.example.domain;

import lombok.Data;
import org.hibernate.validator.constraints.CreditCardNumber;
import javax.validation.constraints.*;
import java.util.List;

@Data
public class Order {
    
    @NotBlank(message = "El nombre es obligatorio")
    private String customerName;
    
    @NotBlank(message = "La dirección es obligatoria")
    @Size(min = 5, max = 100, message = "Dirección inválida")
    private String address;
    
    @NotBlank(message = "La ciudad es obligatoria")
    private String city;
    
    @NotBlank(message = "El código postal es obligatorio")
    @Pattern(regexp = "^[0-9]{5}$", message = "Código postal debe tener 5 dígitos")
    private String zipCode;
    
    @CreditCardNumber(message = "Número de tarjeta no válido")
    private String creditCardNumber;
    
    @Pattern(regexp = "^(0[1-9]|1[0-2])(\\/|-)([2-9][0-9])$", 
             message = "Formato debe ser MM/YY")
    private String ccExpiration;
    
    @Digits(integer = 3, fraction = 0, message = "CVV debe tener 3 dígitos")
    private String ccCVV;
    
    @NotEmpty(message = "Debe incluir al menos un producto")
    @Size(min = 1, max = 50, message = "Máximo 50 productos por orden")
    private List<Product> items;
}
```

**Ejemplo 4: Validación personalizada**
```java
package com.example.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = StrongPasswordValidator.class)
@Documented
public @interface StrongPassword {
    String message() default "La contraseña debe contener mayúsculas, minúsculas, números y caracteres especiales";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
```

```java
package com.example.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class StrongPasswordValidator 
        implements ConstraintValidator<StrongPassword, String> {
    
    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null) {
            return false;
        }
        
        boolean hasUpper = password.matches(".*[A-Z].*");
        boolean hasLower = password.matches(".*[a-z].*");
        boolean hasDigit = password.matches(".*[0-9].*");
        boolean hasSpecial = password.matches(".*[!@#$%^&*()].*");
        
        return hasUpper && hasLower && hasDigit && hasSpecial;
    }
}
```

**Uso:**
```java
@Data
public class UserRegistration {
    @NotBlank
    @StrongPassword
    private String password;
}
```

---

### 2.3.2 Realizando validación en el binding del formulario

Para activar la validación, usa `@Valid` o `@Validated` antes del parámetro del método y añade `Errors` como siguiente parámetro.

**Ejemplo 1: Validación básica en POST**
```java
@Controller
@RequestMapping("/products")
public class ProductController {
    
    @GetMapping("/new")
    public String showForm(Model model) {
        model.addAttribute("product", new Product());
        return "productForm";
    }
    
    @PostMapping
    public String saveProduct(@Valid Product product, Errors errors) {
        // @Valid activa la validación
        // Errors captura los errores de validación
        
        if (errors.hasErrors()) {
            // Si hay errores, volver al formulario
            return "productForm";
        }
        
        // Si todo está bien, procesar
        productService.save(product);
        return "redirect:/products";
    }
}
```

**Ejemplo 2: Validación con logging**
```java
@Slf4j
@Controller
@RequestMapping("/users")
public class UserController {
    
    @PostMapping("/register")
    public String register(@Valid User user, Errors errors, Model model) {
        
        if (errors.hasErrors()) {
            log.warn("Validation errors for user registration: {}", 
                     errors.getErrorCount());
            
            // Log de cada error
            errors.getAllErrors().forEach(error -> 
                log.debug("Validation error: {}", error.getDefaultMessage())
            );
            
            // Añadir información adicional al modelo si es necesario
            model.addAttribute("errorCount", errors.getErrorCount());
            
            return "userRegistrationForm";
        }
        
        log.info("User registered successfully: {}", user.getUsername());
        userService.register(user);
        
        return "redirect:/users/welcome";
    }
}
```

**Ejemplo 3: Validación con errores personalizados adicionales**
```java
@Slf4j
@Controller
@RequestMapping("/orders")
public class OrderController {
    
    private final OrderService orderService;
    
    @PostMapping
    public String submitOrder(@Valid Order order, Errors errors) {
        
        // Primero verificar errores de validación de anotaciones
        if (errors.hasErrors()) {
            log.warn("Validation errors in order submission");
            return "orderForm";
        }
        
        // Validación de negocio adicional
        if (!orderService.isProductAvailable(order.getItems())) {
            errors.rejectValue("items", "items.unavailable", 
                             "Algunos productos no están disponibles");
            return "orderForm";
        }
        
        if (order.getTotalPrice() > 5000 && order.getCreditCardNumber() == null) {
            errors.rejectValue("creditCardNumber", "payment.required", 
                             "Órdenes mayores a $5000 requieren tarjeta de crédito");
            return "orderForm";
        }
        
        // Todo válido, procesar orden
        orderService.processOrder(order);
        log.info("Order processed successfully for {}", order.getCustomerName());
        
        return "redirect:/orders/confirmation";
    }
}
```

**Ejemplo 4: Validación en wizard multi-paso**
```java
@Slf4j
@Controller
@RequestMapping("/checkout")
@SessionAttributes("cart")
public class CheckoutController {
    
    @ModelAttribute(name = "cart")
    public ShoppingCart cart() {
        return new ShoppingCart();
    }
    
    @GetMapping("/step1")
    public String showShippingForm(Model model) {
        model.addAttribute("shippingInfo", new ShippingInfo());
        return "checkout/shipping";
    }
    
    @PostMapping("/step1")
    public String processShipping(@Valid ShippingInfo shippingInfo, 
                                 Errors errors,
                                 @ModelAttribute ShoppingCart cart) {
        if (errors.hasErrors()) {
            log.warn("Shipping info validation failed");
            return "checkout/shipping";
        }
        
        cart.setShippingInfo(shippingInfo);
        return "redirect:/checkout/step2";
    }
    
    @GetMapping("/step2")
    public String showPaymentForm(Model model, @ModelAttribute ShoppingCart cart) {
        if (cart.getShippingInfo() == null) {
            return "redirect:/checkout/step1";
        }
        
        model.addAttribute("paymentInfo", new PaymentInfo());
        return "checkout/payment";
    }
    
    @PostMapping("/step2")
    public String processPayment(@Valid PaymentInfo paymentInfo, 
                                Errors errors,
                                @ModelAttribute ShoppingCart cart) {
        if (errors.hasErrors()) {
            log.warn("Payment info validation failed");
            return "checkout/payment";
        }
        
        cart.setPaymentInfo(paymentInfo);
        return "redirect:/checkout/review";
    }
    
    @PostMapping("/confirm")
    public String confirmOrder(@ModelAttribute ShoppingCart cart, 
                              SessionStatus sessionStatus,
                              Model model) {
        // Validación final
        if (cart.getShippingInfo() == null || cart.getPaymentInfo() == null) {
            return "redirect:/checkout/step1";
        }
        
        // Procesar orden
        Order order = checkoutService.createOrder(cart);
        model.addAttribute("orderNumber", order.getId());
        
        // Limpiar sesión
        sessionStatus.setComplete();
        
        return "checkout/confirmation";
    }
}
```

---

### 2.3.3 Mostrando errores de validación

Thymeleaf proporciona utilidades para mostrar errores de validación en la vista.

**Atributos útiles de Thymeleaf:**
- `th:errors`: Muestra errores de un campo específico
- `${#fields.hasErrors('field')}`: Verifica si un campo tiene errores
- `${#fields.errors('field')}`: Lista de errores de un campo
- `th:errorclass`: Añade una clase CSS si hay error

**Ejemplo 1: Mostrar errores básicos**
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Nuevo Producto</title>
    <link rel="stylesheet" th:href="@{/css/forms.css}" />
    <style>
        .error { color: red; font-size: 0.9em; }
        .field-error { border: 2px solid red; }
    </style>
</head>
<body>
    <h1>Agregar Producto</h1>
    
    <form method="POST" th:action="@{/products}" th:object="${product}">
        
        <div class="form-group">
            <label for="name">Nombre del producto:</label>
            <input type="text" 
                   id="name" 
                   th:field="*{name}"
                   th:errorclass="field-error" />
            
            <!-- Mostrar error si existe -->
            <span class="error" 
                  th:if="${#fields.hasErrors('name')}" 
                  th:errors="*{name}">Error de nombre</span>
        </div>
        
        <div class="form-group">
            <label for="category">Categoría:</label>
            <input type="text" 
                   id="category" 
                   th:field="*{category}"
                   th:errorclass="field-error" />
            <span class="error" 
                  th:if="${#fields.hasErrors('category')}" 
                  th:errors="*{category}">Error de categoría</span>
        </div>
        
        <div class="form-group">
            <label for="price">Precio:</label>
            <input type="number" 
                   id="price" 
                   th:field="*{price}" 
                   step="0.01"
                   th:errorclass="field-error" />
            <span class="error" 
                  th:if="${#fields.hasErrors('price')}" 
                  th:errors="*{price}">Error de precio</span>
        </div>
        
        <div class="form-group">
            <label for="quantity">Cantidad:</label>
            <input type="number" 
                   id="quantity" 
                   th:field="*{quantity}"
                   th:errorclass="field-error" />
            <span class="error" 
                  th:if="${#fields.hasErrors('quantity')}" 
                  th:errors="*{quantity}">Error de cantidad</span>
        </div>
        
        <button type="submit">Guardar</button>
    </form>
</body>
</html>
```

**Ejemplo 2: Mostrar todos los errores en un bloque**
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Registro de Usuario</title>
    <link rel="stylesheet" th:href="@{/css/forms.css}" />
    <style>
        .error-summary {
            background-color: #ffebee;
            border: 1px solid #f44336;
            border-radius: 4px;
            padding: 15px;
            margin-bottom: 20px;
        }
        .error-summary h3 {
            color: #d32f2f;
            margin-top: 0;
        }
        .error-summary ul {
            margin-bottom: 0;
            color: #c62828;
        }
        .field-error {
            border: 2px solid #f44336;
        }
    </style>
</head>
<body>
    <h1>Registro de Usuario</h1>
    
    <!-- Resumen de errores al inicio del formulario -->
    <div class="error-summary" th:if="${#fields.hasErrors('*')}">
        <h3>Por favor corrija los siguientes errores:</h3>
        <ul>
            <li th:each="err : ${#fields.errors('*')}" th:text="${err}">Error</li>
        </ul>
    </div>
    
    <form method="POST" th:action="@{/users/register}" th:object="${user}">
        
        <div class="form-group">
            <label for="username">Nombre de usuario:</label>
            <input type="text" 
                   id="username" 
                   th:field="*{username}"
                   th:errorclass="field-error" />
            <small class="error" 
                   th:if="${#fields.hasErrors('username')}" 
                   th:errors="*{username}">Error</small>
        </div>
        
        <div class="form-group">
            <label for="email">Email:</label>
            <input type="email" 
                   id="email" 
                   th:field="*{email}"
                   th:errorclass="field-error" />
            <small class="error" 
                   th:if="${#fields.hasErrors('email')}" 
                   th:errors="*{email}">Error</small>
        </div>
        
        <div class="form-group">
            <label for="password">Contraseña:</label>
            <input type="password" 
                   id="password" 
                   th:field="*{password}"
                   th:errorclass="field-error" />
            <small class="error" 
                   th:if="${#fields.hasErrors('password')}" 
                   th:errors="*{password}">Error</small>
        </div>
        
        <div class="form-group">
            <label for="age">Edad:</label>
            <input type="number" 
                   id="age" 
                   th:field="*{age}"
                   th:errorclass="field-error" />
            <small class="error" 
                   th:if="${#fields.hasErrors('age')}" 
                   th:errors="*{age}">Error</small>
        </div>
        
        <button type="submit">Registrarse</button>
    </form>
</body>
</html>
```

**Ejemplo 3: Errores con iconos e indicadores visuales**
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Orden de Compra</title>
    <link rel="stylesheet" th:href="@{/css/forms.css}" />
    <style>
        .form-group {
            margin-bottom: 20px;
            position: relative;
        }
        .input-wrapper {
            position: relative;
        }
        .field-error {
            border: 2px solid #e74c3c;
            padding-right: 40px;
        }
        .field-success {
            border: 2px solid #27ae60;
        }
        .error-icon {
            position: absolute;
            right: 10px;
            top: 50%;
            transform: translateY(-50%);
            color: #e74c3c;
        }
        .error-message {
            color: #e74c3c;
            font-size: 0.85em;
            margin-top: 5px;
            display: flex;
            align-items: center;
        }
        .error-message::before {
            content: "⚠ ";
            margin-right: 5px;
        }
    </style>
</head>
<body>
    <h1>Completar Orden</h1>
    
    <form method="POST" th:action="@{/orders}" th:object="${order}">
        
        <h2>Información del Cliente</h2>
        
        <div class="form-group">
            <label for="customerName">Nombre completo:</label>
            <div class="input-wrapper">
                <input type="text" 
                       id="customerName" 
                       th:field="*{customerName}"
                       th:classappend="${#fields.hasErrors('customerName')} ? 'field-error' : 'field-success'" />
                <span class="error-icon" th:if="${#fields.hasErrors('customerName')}">✕</span>
            </div>
            <div class="error-message" 
                 th:if="${#fields.hasErrors('customerName')}" 
                 th:errors="*{customerName}">Error</div>
        </div>
        
        <div class="form-group">
            <label for="address">Dirección:</label>
            <div class="input-wrapper">
                <input type="text" 
                       id="address" 
                       th:field="*{address}"
                       th:classappend="${#fields.hasErrors('address')} ? 'field-error' : 'field-success'" />
                <span class="error-icon" th:if="${#fields.hasErrors('address')}">✕</span>
            </div>
            <div class="error-message" 
                 th:if="${#fields.hasErrors('address')}" 
                 th:errors="*{address}">Error</div>
        </div>
        
        <div class="form-group">
            <label for="city">Ciudad:</label>
            <div class="input-wrapper">
                <input type="text" 
                       id="city" 
                       th:field="*{city}"
                       th:classappend="${#fields.hasErrors('city')} ? 'field-error' : 'field-success'" />
                <span class="error-icon" th:if="${#fields.hasErrors('city')}">✕</span>
            </div>
            <div class="error-message" 
                 th:if="${#fields.hasErrors('city')}" 
                 th:errors="*{city}">Error</div>
        </div>
        
        <div class="form-group">
            <label for="zipCode">Código Postal:</label>
            <div class="input-wrapper">
                <input type="text" 
                       id="zipCode" 
                       th:field="*{zipCode}"
                       placeholder="12345"
                       th:classappend="${#fields.hasErrors('zipCode')} ? 'field-error' : 'field-success'" />
                <span class="error-icon" th:if="${#fields.hasErrors('zipCode')}">✕</span>
            </div>
            <div class="error-message" 
                 th:if="${#fields.hasErrors('zipCode')}" 
                 th:errors="*{zipCode}">Error</div>
        </div>
        
        <h2>Información de Pago</h2>
        
        <div class="form-group">
            <label for="creditCardNumber">Número de Tarjeta:</label>
            <div class="input-wrapper">
                <input type="text" 
                       id="creditCardNumber" 
                       th:field="*{creditCardNumber}"
                       placeholder="1111222233334444"
                       th:classappend="${#fields.hasErrors('creditCardNumber')} ? 'field-error' : 'field-success'" />
                <span class="error-icon" th:if="${#fields.hasErrors('creditCardNumber')}">✕</span>
            </div>
            <div class="error-message" 
                 th:if="${#fields.hasErrors('creditCardNumber')}" 
                 th:errors="*{creditCardNumber}">Error</div>
        </div>
        
        <div class="form-row">
            <div class="form-group">
                <label for="ccExpiration">Vencimiento (MM/YY):</label>
                <div class="input-wrapper">
                    <input type="text" 
                           id="ccExpiration" 
                           th:field="*{ccExpiration}"
                           placeholder="12/25"
                           th:classappend="${#fields.hasErrors('ccExpiration')} ? 'field-error' : 'field-success'" />
                    <span class="error-icon" th:if="${#fields.hasErrors('ccExpiration')}">✕</span>
                </div>
                <div class="error-message" 
                     th:if="${#fields.hasErrors('ccExpiration')}" 
                     th:errors="*{ccExpiration}">Error</div>
            </div>
            
            <div class="form-group">
                <label for="ccCVV">CVV:</label>
                <div class="input-wrapper">
                    <input type="text" 
                           id="ccCVV" 
                           th:field="*{ccCVV}"
                           placeholder="123"
                           th:classappend="${#fields.hasErrors('ccCVV')} ? 'field-error' : 'field-success'" />
                    <span class="error-icon" th:if="${#fields.hasErrors('ccCVV')}">✕</span>
                </div>
                <div class="error-message" 
                     th:if="${#fields.hasErrors('ccCVV')}" 
                     th:errors="*{ccCVV}">Error</div>
            </div>
        </div>
        
        <button type="submit">Procesar Orden</button>
    </form>
</body>
</html>
```

**Ejemplo 4: Validación dinámica con JavaScript (bonus)**
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Formulario Avanzado</title>
    <link rel="stylesheet" th:href="@{/css/forms.css}" />
</head>
<body>
    <form method="POST" th:action="@{/users/register}" th:object="${user}" id="registrationForm">
        
        <div class="form-group">
            <label for="email">Email:</label>
            <input type="email" 
                   id="email" 
                   th:field="*{email}"
                   th:errorclass="field-error" />
            <!-- Error del servidor -->
            <span class="error server-error" 
                  th:if="${#fields.hasErrors('email')}" 
                  th:errors="*{email}">Error</span>
            <!-- Error del cliente (JavaScript) -->
            <span class="error client-error" id="emailError" style="display:none;"></span>
        </div>
        
        <div class="form-group">
            <label for="password">Contraseña:</label>
            <input type="password" 
                   id="password" 
                   th:field="*{password}"
                   th:errorclass="field-error" />
            <span class="error" 
                  th:if="${#fields.hasErrors('password')}" 
                  th:errors="*{password}">Error</span>
            <div id="passwordStrength"></div>
        </div>
        
        <button type="submit">Registrarse</button>
    </form>
    
    <script>
        // Validación del lado del cliente (complementaria)
        document.getElementById('email').addEventListener('blur', function() {
            const email = this.value;
            const emailError = document.getElementById('emailError');
            const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
            
            if (!emailRegex.test(email)) {
                emailError.textContent = 'Email no válido';
                emailError.style.display = 'block';
                this.classList.add('field-error');
            } else {
                emailError.style.display = 'none';
                this.classList.remove('field-error');
            }
        });
        
        document.getElementById('password').addEventListener('input', function() {
            const password = this.value;
            const strengthDiv = document.getElementById('passwordStrength');
            
            let strength = 0;
            if (password.length >= 8) strength++;
            if (/[A-Z]/.test(password)) strength++;
            if (/[a-z]/.test(password)) strength++;
            if (/[0-9]/.test(password)) strength++;
            if (/[^A-Za-z0-9]/.test(password)) strength++;
            
            const levels = ['Muy débil', 'Débil', 'Media', 'Fuerte', 'Muy fuerte'];
            const colors = ['#e74c3c', '#e67e22', '#f39c12', '#27ae60', '#2ecc71'];
            
            strengthDiv.textContent = 'Fortaleza: ' + levels[strength - 1] || '';
            strengthDiv.style.color = colors[strength - 1] || '';
        });
    </script>
</body>
</html>
```

---

## 2.4 Trabajando con controladores de vista

Los **View Controllers** son un atajo para mapear URLs directamente a vistas sin lógica intermedia.

### ¿Cuándo usar View Controllers?

**Úsalos cuando:**
- Solo necesitas renderizar una vista estática
- No hay procesamiento de datos
- No hay lógica de negocio
- Ejemplos: página de inicio, página "acerca de", términos y condiciones

**NO los uses cuando:**
- Necesitas añadir datos al modelo
- Hay lógica de negocio
- Necesitas procesamiento de datos

### Configuración

**Ejemplo 1: Configuración básica**
```java
package com.example.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Mapea "/" a la vista "home"
        registry.addViewController("/").setViewName("home");
        
        // Más mapeos simples
        registry.addViewController("/about").setViewName("about");
        registry.addViewController("/contact").setViewName("contact");
        registry.addViewController("/terms").setViewName("terms");
        registry.addViewController("/privacy").setViewName("privacy");
    }
}
```

**Equivalente sin View Controller:**
```java
@Controller
public class HomeController {
    
    @GetMapping("/")
    public String home() {
        return "home";
    }
    
    @GetMapping("/about")
    public String about() {
        return "about";
    }
    
    @GetMapping("/contact")
    public String contact() {
        return "contact";
    }
}
```

**Ejemplo 2: Con código de estado personalizado**
```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("home");
        
        // Redireccionamiento
        registry.addRedirectViewController("/old-path", "/new-path");
        
        // Página de error personalizada
        registry.addViewController("/error/404")
                .setViewName("errors/notFound")
                .setStatusCode(HttpStatus.NOT_FOUND);
        
        registry.addViewController("/error/403")
                .setViewName("errors/forbidden")
                .setStatusCode(HttpStatus.FORBIDDEN);
    }
}
```

**Ejemplo 3: Con seguridad (Spring Security)**
```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Páginas públicas
        registry.addViewController("/").setViewName("home");
        registry.addViewController("/login").setViewName("login");
        registry.addViewController("/register").setViewName("register");
        
        // Páginas protegidas (la seguridad se configura en SecurityConfig)
        registry.addViewController("/dashboard").setViewName("dashboard");
        registry.addViewController("/profile").setViewName("profile");
    }
}
```

**Ejemplo 4: Organización completa**
```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Landing pages
        addLandingPages(registry);
        
        // Páginas de información
        addInfoPages(registry);
        
        // Páginas de error
        addErrorPages(registry);
    }
    
    private void addLandingPages(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("home");
        registry.addViewController("/home").setViewName("home");
        registry.addViewController("/welcome").setViewName("home");
    }
    
    private void addInfoPages(ViewControllerRegistry registry) {
        registry.addViewController("/about").setViewName("info/about");
        registry.addViewController("/contact").setViewName("info/contact");
        registry.addViewController("/faq").setViewName("info/faq");
        registry.addViewController("/terms").setViewName("legal/terms");
        registry.addViewController("/privacy").setViewName("legal/privacy");
    }
    
    private void addErrorPages(ViewControllerRegistry registry) {
        registry.addViewController("/error/404")
                .setViewName("errors/notFound")
                .setStatusCode(HttpStatus.NOT_FOUND);
        
        registry.addViewController("/error/500")
                .setViewName("errors/serverError")
                .setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
```

**Comparación: Cuándo usar cada enfoque**

```java
// ❌ NO NECESARIO - Usar View Controller
@Controller
public class StaticController {
    @GetMapping("/about")
    public String about() {
        return "about";  // Sin lógica, solo devuelve la vista
    }
}

// ✅ USAR View Controller en WebConfig
registry.addViewController("/about").setViewName("about");

// ✅ CONTROLADOR NECESARIO - Hay lógica
@Controller
public class AboutController {
    @GetMapping("/about")
    public String about(Model model) {
        model.addAttribute("companyInfo", companyService.getInfo());
        model.addAttribute("teamMembers", teamService.getAllMembers());
        return "about";  // Añade datos al modelo
    }
}
```

---

## 2.5 Eligiendo una biblioteca de plantillas de vista

Spring Boot soporta varias tecnologías de plantillas para renderizar vistas.

### Opciones principales

| Tecnología | Características | Casos de uso |
|------------|----------------|--------------|
| **Thymeleaf** | Natural templating, integración Spring, HTML5 válido | Aplicaciones modernas, prototipos rápidos |
| **JSP** | Tecnología Java clásica, ampliamente conocida | Legacy, integración con sistemas antiguos |
| **Freemarker** | Flexible, potente, no solo para web | Emails, reportes, plantillas complejas |
| **Mustache** | Simple, logic-less, multiplataforma | Microservicios, APIs con SSR mínima |
| **Groovy Templates** | Sintaxis Groovy, expresivo | Proyectos Groovy/Grails |

### Thymeleaf (Recomendado)

**Ventajas:**
- HTML válido que puede abrirse en navegador sin servidor
- Excelente integración con Spring
- Soporte completo para Spring Security, internacionalización, formularios
- Sintaxis natural y legible
- Activamente mantenido

**Dependencia Maven:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>
```

**Configuración automática:**
- Plantillas en: `src/main/resources/templates/`
- Extensión: `.html`
- No requiere configuración adicional

**Ejemplo de plantilla:**
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title th:text="${pageTitle}">Default Title</title>
</head>
<body>
    <h1 th:text="${message}">Default Message</h1>
    
    <ul>
        <li th:each="item : ${items}" th:text="${item.name}">Item</li>
    </ul>
    
    <div th:if="${user != null}">
        <p>Welcome, <span th:text="${user.name}">User</span>!</p>
    </div>
</body>
</html>
```

### JSP (Java Server Pages)

**Ventajas:**
- Ampliamente conocida
- Muchos ejemplos y recursos
- JSTL y etiquetas personalizadas

**Desventajas:**
- No funciona bien con JAR ejecutables
- Sintaxis más verbosa
- No es HTML válido
- Menos popular en proyectos nuevos

**Dependencia Maven:**
```xml
<dependency>
    <groupId>org.apache.tomcat.embed</groupId>
    <artifactId>tomcat-embed-jasper</artifactId>
</dependency>
<dependency>
    <groupId>javax.servlet</groupId>
    <artifactId>jstl</artifactId>
</dependency>
```

**Configuración en application.properties:**
```properties
spring.mvc.view.prefix=/WEB-INF/jsp/
spring.mvc.view.suffix=.jsp
```

**Ejemplo:**
```jsp
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
    <title>${pageTitle}</title>
</head>
<body>
    <h1>${message}</h1>
    
    <ul>
        <c:forEach items="${items}" var="item">
            <li>${item.name}</li>
        </c:forEach>
    </ul>
    
    <c:if test="${user != null}">
        <p>Welcome, ${user.name}!</p>
    </c:if>
</body>
</html>
```

### Freemarker

**Ventajas:**
- Muy flexible
- Bueno para plantillas de email
- Potente sistema de macros
- Rápido

**Dependencia:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-freemarker</artifactId>
</dependency>
```

**Ejemplo:**
```ftl
<!DOCTYPE html>
<html>
<head>
    <title>${pageTitle}</title>
</head>
<body>
    <h1>${message}</h1>
    
    <ul>
        <#list items as item>
            <li>${item.name}</li>
        </#list>
    </ul>
    
    <#if user??>
        <p>Welcome, ${user.name}!</p>
    </#if>
</body>
</html>
```

### Comparación práctica

**Caso 1: Aplicación web moderna**
```xml
<!-- RECOMENDADO: Thymeleaf -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>
```

**Caso 2: Sistema legacy con JSP existente**
```xml
<!-- Mantener JSP -->
<dependency>
    <groupId>org.apache.tomcat.embed</groupId>
    <artifactId>tomcat-embed-jasper</artifactId>
</dependency>
```

**Caso 3: Generación de emails y reportes**
```xml
<!-- Freemarker es excelente para esto -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-freemarker</artifactId>
</dependency>
```

---

## 2.5.1 Cacheando plantillas

El **caching de plantillas** mejora el rendimiento al evitar que las plantillas se re-procesen en cada petición.

### Comportamiento por defecto

**En desarrollo (profile por defecto):**
- Cache DESHABILITADO
- Cambios en plantillas se ven inmediatamente
- Facilita el desarrollo

**En producción:**
- Cache HABILITADO
- Mejor rendimiento
- Menor uso de CPU
- Plantillas cargadas una vez

### Configuración en application.properties

**Para desarrollo:**
```properties
# Deshabilitar caché para ver cambios inmediatamente
spring.thymeleaf.cache=false

# Alternativa: usar profiles
spring.profiles.active=dev
```

**Para producción:**
```properties
# Habilitar caché
spring.thymeleaf.cache=true

# Configuraciones adicionales de rendimiento
spring.thymeleaf.encoding=UTF-8
spring.thymeleaf.mode=HTML
spring.thymeleaf.servlet.content-type=text/html
```

### Configuración por profiles

**application.properties (base):**
```properties
# Configuración común
spring.thymeleaf.prefix=classpath:/templates/
spring.thymeleaf.suffix=.html
spring.thymeleaf.encoding=UTF-8
```

**application-dev.properties:**
```properties
# Desarrollo: sin caché
spring.thymeleaf.cache=false

# Logs detallados
logging.level.org.thymeleaf=DEBUG
logging.level.org.springframework.web=DEBUG
```

**application-prod.properties:**
```properties
# Producción: con caché
spring.thymeleaf.cache=true

# Logs mínimos
logging.level.org.thymeleaf=WARN
logging.level.org.springframework.web=WARN
```

**Activar profile en ejecución:**
```bash
# Development
java -jar app.jar --spring.profiles.active=dev

# Production
java -jar app.jar --spring.profiles.active=prod
```

### Configuración avanzada con código

**Ejemplo de configuración personalizada:**
```java
package com.example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.thymeleaf.spring5.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;

@Configuration
public class ThymeleafConfig {
    
    @Bean
    @Profile("dev")
    public SpringResourceTemplateResolver devTemplateResolver() {
        SpringResourceTemplateResolver templateResolver = 
            new SpringResourceTemplateResolver();
        
        templateResolver.setPrefix("classpath:/templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setCharacterEncoding("UTF-8");
        
        // SIN CACHÉ en desarrollo
        templateResolver.setCacheable(false);
        
        return templateResolver;
    }
    
    @Bean
    @Profile("prod")
    public SpringResourceTemplateResolver prodTemplateResolver() {
        SpringResourceTemplateResolver templateResolver = 
            new SpringResourceTemplateResolver();
        
        templateResolver.setPrefix("classpath:/templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setCharacterEncoding("UTF-8");
        
        // CON CACHÉ en producción
        templateResolver.setCacheable(true);
        templateResolver.setCacheTTLMs(3600000L); // 1 hora
        
        return templateResolver;
    }
}
```

### Invalidación manual del caché

**Cuando es necesario:**
- Actualización de plantillas en producción sin reinicio
- Testing de cambios
- Hot-reload en ambientes específicos

**Ejemplo de servicio para limpiar caché:**
```java
package com.example.service;

import org.springframework.stereotype.Service;
import org.thymeleaf.spring5.SpringTemplateEngine;

@Service
public class TemplateCacheService {
    
    private final SpringTemplateEngine templateEngine;
    
    public TemplateCacheService(SpringTemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }
    
    public void clearCache() {
        templateEngine.clearTemplateCache();
    }
    
    public void clearCacheForTemplate(String templateName) {
        templateEngine.getCacheManager()
                      .getTemplateCache()
                      .clearKey(templateName);
    }
}
```

**Endpoint administrativo:**
```java
package com.example.controller;

import com.example.service.TemplateCacheService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/cache")
public class CacheAdminController {
    
    private final TemplateCacheService cacheService;
    
    public CacheAdminController(TemplateCacheService cacheService) {
        this.cacheService = cacheService;
    }
    
    @PostMapping("/clear")
    public String clearAllCache() {
        cacheService.clearCache();
        return "Template cache cleared";
    }
    
    @PostMapping("/clear/{templateName}")
    public String clearTemplateCache(@PathVariable String templateName) {
        cacheService.clearCacheForTemplate(templateName);
        return "Cache cleared for template: " + templateName;
    }
}
```

### Monitoreo del caché

**Configuración de métricas con Actuator:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

**application.properties:**
```properties
management.endpoints.web.exposure.include=health,metrics,cache
management.endpoint.health.show-details=always
```

**Verificar estado del caché:**
```bash
# Via Actuator
curl http://localhost:8080/actuator/metrics/cache.size
curl http://localhost:8080/actuator/metrics/cache.gets
```

### Mejores prácticas

**1. Siempre deshabilitar caché en desarrollo:**
```properties
spring.thymeleaf.cache=false
```

**2. Usar profiles para ambientes:**
```bash
# Dev
--spring.profiles.active=dev

# Prod
--spring.profiles.active=prod
```

**3. Monitorear en producción:**
- Configurar métricas de caché
- Alertas en caso de problemas de memoria
- Logs de rendimiento

**4. Testing:**
```java
@SpringBootTest
@ActiveProfiles("test")
class TemplateTest {
    
    @Autowired
    private SpringTemplateEngine templateEngine;
    
    @Test
    void testTemplateRendering() {
        // Test con caché deshabilitado
        Context context = new Context();
        context.setVariable("message", "Test");
        
        String result = templateEngine.process("testTemplate", context);
        
        assertThat(result).contains("Test");
    }
}
```

---

## Resumen de conceptos clave

### Flujo completo de una petición web en Spring MVC:

1. **Cliente** envía petición HTTP GET/POST
2. **DispatcherServlet** recibe y enruta
3. **Controller** procesa la petición:
   - `@ModelAttribute` métodos se ejecutan primero
   - Método handler (`@GetMapping`, `@PostMapping`) se ejecuta
   - `@Valid` valida beans si aplica
   - Se añaden datos al `Model`
4. **ViewResolver** resuelve el nombre de vista
5. **Template Engine** (Thymeleaf) renderiza HTML
6. **Respuesta** se envía al cliente

### Anotaciones esenciales:

```java
@Controller              // Marca clase como controlador MVC
@RequestMapping("/path") // Mapeo de URL base
@GetMapping              // Petición GET
@PostMapping             // Petición POST
@ModelAttribute          // Poblar modelo o recuperar de sesión
@SessionAttributes       // Mantener objetos en sesión
@Valid                   // Activar validación
@PathVariable            // Variable de URL
@RequestParam            // Parámetro de query string
```

### Reglas de oro:

1. **Usa @ModelAttribute sin parámetro** → inicialización de modelo
2. **Usa @ModelAttribute en parámetro** → recupera objeto existente de sesión
3. **Sin @ModelAttribute en parámetro** → crea nuevo objeto desde formulario
4. **Siempre añade `Errors` después de `@Valid`** para capturar errores
5. **Usa View Controllers** solo para vistas estáticas sin lógica
6. **Deshabilita caché en desarrollo**, habilítalo en producción

---

## Referencias y recursos adicionales

**Documentación oficial:**
- [Spring MVC Documentation](https://docs.spring.io/spring-framework/reference/web/webmvc.html)
- [Thymeleaf Documentation](https://www.thymeleaf.org/documentation.html)
- [Bean Validation Specification](https://beanvalidation.org/)
- [Spring Boot Reference - Web](https://docs.spring.io/spring-boot/docs/current/reference/html/web.html)

**Conceptos relacionados:**
- REST APIs (Capítulo 6)
- Spring Security (Capítulo 4)
- Data Persistence (Capítulo 3)
- Testing (Capítulo 9)

---

*Esta guía está basada en Spring in Action 6th Edition y utiliza Spring Boot 2.5+ con Java 11+.*
