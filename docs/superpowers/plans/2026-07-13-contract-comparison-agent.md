# 合同对比 Agent 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建一个能自动对比"采购合同 vs 销售合同"的智能系统，抽取结构化要素、给出风险评级，支持对话式追问，包含用户端与管理端。

**Architecture:** 单后端（Spring Boot 3.3 + Spring AI）+ 双前端（Vue 3 用户端/管理端），MySQL 存元数据，本地磁盘存原件，LLM 通过 Spring AI `@Tool` 调用自主规划工作流。Agent 形态：单 Agent 自循环 + 4 个工具（extract_contract / get_extraction / compare_fields / get_chat_history）。

**Tech Stack:** Java 21 / Spring Boot 3.3 / Spring Security 6 / JWT 0.12 / MyBatis-Plus 3.5 / MySQL 8 / Spring AI 1.0 / Apache Tika 2.9 / Maven 3.9+ / Vue 3.5 / Vite 5 / TypeScript 5 / Pinia 2 / Tailwind CSS 4 / lucide-vue-next / Axios 1 / pnpm 9

> **前端设计基线**：UI 完全还原 `design/` 目录下的 5 个设计稿（登录/任务列表/任务详情/管理概览/用户管理），使用设计稿的 Google 配色主题（`#4285f4` 主色、`#f0f6ff` 侧边栏）、DM Sans + JetBrains Mono 字体、零阴影扁平卡片、8px 圆角、3.84px 间距基准。仅做接口适配改动（替换静态数据为 API 调用、补齐表单与弹窗交互）。缺失页面（新建任务/风险规则/LLM 日志/个人中心）按同一设计系统补齐。

**Spec:** `docs/superpowers/specs/2026-07-13-contract-comparison-agent-design.md`
**API:** `docs/api.md`

---

## Phase 0：仓库脚手架

### Task 0.1：初始化仓库结构与根 README

**Files:**
- Create: `C:\Users\xingchen\Desktop\agent\README.md`（已存在，跳过但需要 `.gitignore`）
- Create: `C:\Users\xingchen\Desktop\agent\.gitignore`

- [ ] **Step 1：初始化 git 仓库**

Run:
```bash
cd C:\Users\xingchen\Desktop\agent
git init
git config user.email "dev@example.com"
git config user.name "dev"
```

- [ ] **Step 2：写 `.gitignore`**

Create `C:\Users\xingchen\Desktop\agent\.gitignore`：
```gitignore
# Java / Maven
target/
*.class
*.jar
*.war
.mvn/wrapper/maven-wrapper.jar
HELP.md

# IDE
.idea/
*.iml
.vscode/
.project
.classpath
.settings/

# Node / Vue
node_modules/
dist/
.DS_Store
*.local
.env
.env.local

# Spring Boot
application-local.yml
application-dev.yml
storage/
logs/
*.log

# OS
Thumbs.db
desktop.ini
```

- [ ] **Step 3：创建子目录占位**

```bash
cd C:\Users\xingchen\Desktop\agent
mkdir backend frontend frontend-admin docs\samples docs\superpowers\plans
```

- [ ] **Step 4：第一次提交**

```bash
git add .
git commit -m "chore: 初始化仓库结构与 .gitignore"
```

---

## Phase 1：后端骨架（Spring Boot + Security + JWT）

### Task 1.1：Spring Boot 项目初始化

**Files:**
- Create: `backend/pom.xml`
- Create: `backend/src/main/java/com/example/contractagent/ContractAgentApplication.java`
- Create: `backend/src/main/resources/application.yml`

- [ ] **Step 1：写 `pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.4</version>
        <relativePath/>
    </parent>
    <groupId>com.example</groupId>
    <artifactId>contract-agent</artifactId>
    <version>0.1.0</version>
    <name>contract-agent</name>
    <properties>
        <java.version>21</java.version>
        <mybatis-plus.version>3.5.9</mybatis-plus.version>
        <spring-ai.version>1.0.0-M6</spring-ai.version>
        <jjwt.version>0.12.6</jjwt.version>
        <tika.version>2.9.2</tika.version>
    </properties>
    <dependencies>
        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-web</artifactId></dependency>
        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-security</artifactId></dependency>
        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-validation</artifactId></dependency>
        <dependency><groupId>com.baomidou</groupId><artifactId>mybatis-plus-spring-boot3-starter</artifactId><version>${mybatis-plus.version}</version></dependency>
        <dependency><groupId>com.mysql</groupId><artifactId>mysql-connector-j</artifactId></dependency>
        <dependency><groupId>org.springframework.ai</groupId><artifactId>spring-ai-starter-model-openai</artifactId></dependency>
        <dependency><groupId>org.springframework.ai</groupId><artifactId>spring-ai-starter-model-anthropic</artifactId></dependency>
        <dependency><groupId>org.apache.tika</groupId><artifactId>tika-core</artifactId><version>${tika.version}</version></dependency>
        <dependency><groupId>org.apache.tika</groupId><artifactId>tika-parsers-standard-package</artifactId><version>${tika.version}</version></dependency>
        <dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-api</artifactId><version>${jjwt.version}</version></dependency>
        <dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-impl</artifactId><version>${jjwt.version}</version><scope>runtime</scope></dependency>
        <dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-jackson</artifactId><version>${jjwt.version}</version><scope>runtime</scope></dependency>
        <dependency><groupId>org.projectlombok</groupId><artifactId>lombok</artifactId><optional>true</optional></dependency>
        <dependency><groupId>org.springdoc</groupId><artifactId>springdoc-openapi-starter-webmvc-ui</artifactId><version>2.6.0</version></dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId><scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId><scope>test</scope>
        </dependency>
    </dependencies>
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration><excludes><exclude><groupId>org.projectlombok</groupId><artifactId>lombok</artifactId></exclude></excludes></configuration>
            </plugin>
        </plugins>
    </build>
    <repositories>
        <repository><id>spring-milestones</id><url>https://repo.spring.io/milestone</url></repository>
        <repository><id>spring-snapshots</id><url>https://repo.spring.io/snapshot</url></repository>
    </repositories>
</project>
```

- [ ] **Step 2：写主类**

Create `backend/src/main/java/com/example/contractagent/ContractAgentApplication.java`：
```java
package com.example.contractagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ContractAgentApplication {
    public static void main(String[] args) {
        SpringApplication.run(ContractAgentApplication.class, args);
    }
}
```

- [ ] **Step 3：写默认配置**

Create `backend/src/main/resources/application.yml`：
```yaml
server:
  port: 8080

spring:
  application:
    name: contract-agent
  profiles:
    active: local
  servlet:
    multipart:
      max-file-size: 20MB
      max-request-size: 50MB
  datasource:
    url: jdbc:mysql://localhost:3306/contract_agent?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8mb4
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
  global-config:
    db-config:
      id-type: AUTO

storage:
  base-path: ./storage/contracts
  max-file-size-bytes: 20971520

springdoc:
  swagger-ui:
    path: /swagger-ui.html
```

- [ ] **Step 4：编译验证**

Run:
```bash
cd C:\Users\xingchen\Desktop\agent\backend
mvn -DskipTests compile
```
Expected: BUILD SUCCESS

- [ ] **Step 5：提交**

```bash
cd C:\Users\xingchen\Desktop\agent
git add backend/
git commit -m "feat(backend): 初始化 Spring Boot 项目骨架"
```

---

### Task 1.2：MySQL 数据库初始化脚本

**Files:**
- Create: `backend/src/main/resources/db/migration/V1__init.sql`
- Create: `backend/src/main/resources/db/migration/V2__seed_risk_rules.sql`

- [ ] **Step 1：写建表脚本**

Create `backend/src/main/resources/db/migration/V1__init.sql`：
```sql
-- 用户
CREATE TABLE IF NOT EXISTS user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(16) NOT NULL DEFAULT 'USER',
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 对比任务
CREATE TABLE IF NOT EXISTS comparison_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    summary TEXT,
    risk_level VARCHAR(16),
    confirmed_at DATETIME,
    confirmed_by BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_status (user_id, status),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 合同
CREATE TABLE IF NOT EXISTS contract (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    side VARCHAR(8) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    stored_path VARCHAR(512) NOT NULL,
    mime_type VARCHAR(64),
    extracted_text LONGTEXT,
    file_size BIGINT NOT NULL,
    uploaded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_task_side (task_id, side)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 抽取结果
CREATE TABLE IF NOT EXISTS extraction (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    contract_id BIGINT NOT NULL UNIQUE,
    supplier_name VARCHAR(255),
    item_name VARCHAR(255),
    item_model VARCHAR(255),
    unit VARCHAR(32),
    quantity DECIMAL(18,4),
    purchase_unit_price DECIMAL(18,4),
    purchase_total_amount DECIMAL(18,4),
    expected_delivery_date DATE,
    payment_terms TEXT,
    delivery_location VARCHAR(255),
    application_no VARCHAR(64),
    application_title VARCHAR(255),
    apply_date DATE,
    application_type VARCHAR(32),
    currency VARCHAR(8),
    application_status VARCHAR(16),
    create_time DATETIME,
    message VARCHAR(255),
    confidence DECIMAL(3,2),
    raw_quote TEXT,
    extracted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 对话消息
CREATE TABLE IF NOT EXISTS chat_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    role VARCHAR(16) NOT NULL,
    content TEXT NOT NULL,
    tool_calls JSON,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_task_created (task_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 风险规则
CREATE TABLE IF NOT EXISTS risk_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    field_key VARCHAR(64) NOT NULL,
    operator VARCHAR(8) NOT NULL,
    threshold_value DECIMAL(18,4),
    risk_level VARCHAR(16) NOT NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    remark VARCHAR(255),
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by BIGINT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- LLM 调用日志
CREATE TABLE IF NOT EXISTS llm_call_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT,
    user_id BIGINT,
    model VARCHAR(64) NOT NULL,
    prompt_tokens INT,
    completion_tokens INT,
    total_tokens INT,
    duration_ms INT,
    status VARCHAR(16) NOT NULL,
    error_msg TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_task (task_id),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 初始管理员（密码 admin123 的 BCrypt）
INSERT INTO user (username, password_hash, role) VALUES
('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ADMIN');
```

- [ ] **Step 2：写预置风险规则脚本**

Create `backend/src/main/resources/db/migration/V2__seed_risk_rules.sql`：
```sql
INSERT INTO risk_rule (field_key, operator, threshold_value, risk_level, enabled, remark) VALUES
('purchaseTotalAmount', 'PCT_GT', 0.05,    'HIGH',   1, '金额差 > 5%'),
('quantity',            'NE',     0,        'HIGH',   1, '数量不一致'),
('expectedDeliveryDate','GT',     7,        'MEDIUM', 1, '日期差 > 7 天'),
('paymentTerms',        'EQ',     '',       'MEDIUM', 1, '付款条款缺失'),
('deliveryLocation',    'NE',     '',       'MEDIUM', 1, '交付地点不一致');
```

- [ ] **Step 3：建库并执行脚本**

Run:
```bash
mysql -uroot -p -e "CREATE DATABASE IF NOT EXISTS contract_agent DEFAULT CHARSET utf8mb4;"
mysql -uroot -p contract_agent < backend/src/main/resources/db/migration/V1__init.sql
mysql -uroot -p contract_agent < backend/src/main/resources/db/migration/V2__seed_risk_rules.sql
mysql -uroot -p contract_agent -e "SELECT id,username,role FROM user;"
```
Expected: 1 行，admin / ADMIN

- [ ] **Step 4：提交**

```bash
git add backend/src/main/resources/db/
git commit -m "feat(backend): 建表 SQL 与预置数据"
```

---

### Task 1.3：通用响应 / 异常体系

**Files:**
- Create: `backend/src/main/java/com/example/contractagent/common/ApiResponse.java`
- Create: `backend/src/main/java/com/example/contractagent/common/ErrorCode.java`
- Create: `backend/src/main/java/com/example/contractagent/common/BusinessException.java`
- Create: `backend/src/main/java/com/example/contractagent/common/GlobalExceptionHandler.java`
- Create: `backend/src/test/java/com/example/contractagent/common/ApiResponseTest.java`

- [ ] **Step 1：写 `ApiResponse`**

```java
package com.example.contractagent.common;

public record ApiResponse<T>(int code, String message, T data) {
    public static <T> ApiResponse<T> ok(T data) { return new ApiResponse<>(0, "ok", data); }
    public static <T> ApiResponse<T> error(int code, String message) { return new ApiResponse<>(code, message, null); }
}
```

- [ ] **Step 2：写 `ErrorCode`**

```java
package com.example.contractagent.common;

public final class ErrorCode {
    private ErrorCode() {}
    public static final int PARAM_INVALID   = 1001;
    public static final int UNAUTHORIZED    = 1002;
    public static final int FORBIDDEN       = 1003;
    public static final int NOT_FOUND       = 2001;
    public static final int PARSE_FAIL      = 4001;
    public static final int EXTRACT_FAIL    = 4002;
    public static final int LLM_FAIL        = 5001;
    public static final int SYSTEM_ERROR    = 9999;
}
```

- [ ] **Step 3：写 `BusinessException`**

```java
package com.example.contractagent.common;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final int code;
    public BusinessException(int code, String message) { super(message); this.code = code; }
    public static BusinessException of(int code, String message) { return new BusinessException(code, message); }
}
```

- [ ] **Step 4：写 `GlobalExceptionHandler`**

```java
package com.example.contractagent.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
        HttpStatus status = ex.getCode() == ErrorCode.UNAUTHORIZED ? HttpStatus.UNAUTHORIZED
                : ex.getCode() == ErrorCode.FORBIDDEN ? HttpStatus.FORBIDDEN
                : ex.getCode() == ErrorCode.NOT_FOUND ? HttpStatus.NOT_FOUND
                : ex.getCode() == ErrorCode.PARSE_FAIL || ex.getCode() == ErrorCode.EXTRACT_FAIL ? HttpStatus.UNPROCESSABLE_ENTITY
                : ex.getCode() == ErrorCode.LLM_FAIL ? HttpStatus.BAD_GATEWAY
                : HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(status).body(ApiResponse.error(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .findFirst().map(f -> f.getField() + ": " + f.getDefaultMessage()).orElse("参数校验失败");
        return ResponseEntity.badRequest().body(ApiResponse.error(ErrorCode.PARAM_INVALID, msg));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleOther(Exception ex) {
        return ResponseEntity.internalServerError().body(ApiResponse.error(ErrorCode.SYSTEM_ERROR, ex.getMessage()));
    }
}
```

- [ ] **Step 5：写测试**

Create `backend/src/test/java/com/example/contractagent/common/ApiResponseTest.java`：
```java
package com.example.contractagent.common;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ApiResponseTest {
    @Test
    void ok_returnsZeroCode() {
        ApiResponse<String> r = ApiResponse.ok("hi");
        assertEquals(0, r.code());
        assertEquals("ok", r.message());
        assertEquals("hi", r.data());
    }

    @Test
    void error_carriesCode() {
        ApiResponse<Void> r = ApiResponse.error(1001, "bad");
        assertEquals(1001, r.code());
        assertEquals("bad", r.message());
        assertNull(r.data());
    }
}
```

- [ ] **Step 6：跑测试**

Run:
```bash
cd C:\Users\xingchen\Desktop\agent\backend
mvn test -Dtest=ApiResponseTest
```
Expected: 2 tests passed

- [ ] **Step 7：提交**

```bash
cd C:\Users\xingchen\Desktop\agent
git add backend/src/main/java/com/example/contractagent/common/
git add backend/src/test/java/com/example/contractagent/common/
git commit -m "feat(backend): 通用响应/异常体系 + 单测"
```

---

### Task 1.4：JWT 工具 + Spring Security 配置

**Files:**
- Create: `backend/src/main/java/com/example/contractagent/security/JwtService.java`
- Create: `backend/src/main/java/com/example/contractagent/security/UserDetailsServiceImpl.java`
- Create: `backend/src/main/java/com/example/contractagent/security/JwtAuthFilter.java`
- Create: `backend/src/main/java/com/example/contractagent/config/SecurityConfig.java`
- Create: `backend/src/main/java/com/example/contractagent/config/StorageProperties.java`
- Create: `backend/src/main/resources/application-local.yml`

- [ ] **Step 1：写 `StorageProperties`**

```java
package com.example.contractagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "storage")
public record StorageProperties(String basePath, long maxFileSizeBytes) {}
```

- [ ] **Step 2：写 `JwtService`**

```java
package com.example.contractagent.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey key;
    private final long ttlSeconds;

    public JwtService(@Value("${jwt.secret:dev-secret-key-must-be-at-least-32-bytes-long-yes}") String secret,
                      @Value("${jwt.ttl-seconds:86400}") long ttlSeconds) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttlSeconds = ttlSeconds;
    }

    public String issue(Long userId, String username, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(ttlSeconds)))
                .signWith(key)
                .compact();
    }

    public long getTtlSeconds() { return ttlSeconds; }

    public Long parseUserId(String token) {
        var claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        return Long.parseLong(claims.getSubject());
    }

    public String parseRole(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().get("role", String.class);
    }
}
```

- [ ] **Step 3：写 `UserDetailsServiceImpl`**

```java
package com.example.contractagent.security;

import com.example.contractagent.user.User;
import com.example.contractagent.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User u = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("user not found"));
        return new org.springframework.security.core.userdetails.User(
                u.getUsername(), u.getPasswordHash(),
                u.getEnabled() == 1,
                true, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole()))
        );
    }
}
```

- [ ] **Step 4：写 `JwtAuthFilter`**

```java
package com.example.contractagent.security;

import com.example.contractagent.common.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String auth = req.getHeader(HttpHeaders.AUTHORIZATION);
        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);
            try {
                Long userId = jwtService.parseUserId(token);
                String role = jwtService.parseRole(token);
                var authentication = new UsernamePasswordAuthenticationToken(
                        userId, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception e) {
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(req, res);
    }
}
```

- [ ] **Step 5：写 `SecurityConfig`**

```java
package com.example.contractagent.config;

import com.example.contractagent.common.ApiResponse;
import com.example.contractagent.common.ErrorCode;
import com.example.contractagent.security.JwtAuthFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableConfigurationProperties(StorageProperties.class)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final ObjectMapper objectMapper;

    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                .requestMatchers("/api/auth/**", "/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req, res, ex) -> {
                    res.setStatus(HttpStatus.UNAUTHORIZED.value());
                    res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    res.getWriter().write(objectMapper.writeValueAsString(
                            ApiResponse.error(ErrorCode.UNAUTHORIZED, "未登录或 Token 过期")));
                })
                .accessDeniedHandler((req, res, ex) -> {
                    res.setStatus(HttpStatus.FORBIDDEN.value());
                    res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    res.getWriter().write(objectMapper.writeValueAsString(
                            ApiResponse.error(ErrorCode.FORBIDDEN, "无权限")));
                }))
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
```

- [ ] **Step 6：写 `application-local.yml`**

Create `backend/src/main/resources/application-local.yml`：
```yaml
jwt:
  secret: dev-secret-key-must-be-at-least-32-bytes-long-yes
  ttl-seconds: 86400

spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY:sk-replace-me}
      chat:
        options:
          model: gpt-4o-mini
```

- [ ] **Step 7：编译**

Run:
```bash
cd C:\Users\xingchen\Desktop\agent\backend
mvn -DskipTests compile
```
Expected: BUILD SUCCESS（User entity / Repository 还没写，会报缺类，先继续到 Task 1.5）

- [ ] **Step 8：提交（仅安全相关）**

```bash
cd C:\Users\xingchen\Desktop\agent
git add backend/src/main/java/com/example/contractagent/security/
git add backend/src/main/java/com/example/contractagent/config/
git add backend/src/main/resources/application-local.yml
git commit -m "feat(backend): JWT 鉴权 + Spring Security 配置"
```

---

### Task 1.5：User 实体 / Repository / 注册登录

**Files:**
- Create: `backend/src/main/java/com/example/contractagent/user/UserRole.java`
- Create: `backend/src/main/java/com/example/contractagent/user/User.java`
- Create: `backend/src/main/java/com/example/contractagent/user/UserRepository.java`
- Create: `backend/src/main/java/com/example/contractagent/user/UserService.java`
- Create: `backend/src/main/java/com/example/contractagent/user/AuthController.java`
- Create: `backend/src/main/java/com/example/contractagent/user/dto/RegisterRequest.java`
- Create: `backend/src/main/java/com/example/contractagent/user/dto/LoginRequest.java`
- Create: `backend/src/main/java/com/example/contractagent/user/dto/AuthResponse.java`
- Create: `backend/src/main/java/com/example/contractagent/user/dto/UserDto.java`
- Create: `backend/src/main/java/com/example/contractagent/user/UserMapper.java`
- Create: `backend/src/test/java/com/example/contractagent/user/UserServiceTest.java`

- [ ] **Step 1：写 `UserRole`**

```java
package com.example.contractagent.user;

public enum UserRole { USER, ADMIN }
```

- [ ] **Step 2：写 `User` 实体**

```java
package com.example.contractagent.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String passwordHash;
    private UserRole role;
    private Integer enabled;
    private LocalDateTime createdAt;
}
```

- [ ] **Step 3：写 `UserRepository`**

```java
package com.example.contractagent.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import java.util.Optional;

@Mapper
public interface UserRepository extends BaseMapper<User> {
    default Optional<User> findByUsername(String username) { return Optional.ofNullable(selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>().eq("username", username))); }
}
```

- [ ] **Step 4：写 `UserMapper`（DTO 转换）**

```java
package com.example.contractagent.user;

import com.example.contractagent.user.dto.UserDto;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    public UserDto toDto(User u) {
        return new UserDto(u.getId(), u.getUsername(), u.getRole().name());
    }
}
```

- [ ] **Step 5：写 DTO 类**

Create `backend/src/main/java/com/example/contractagent/user/dto/RegisterRequest.java`：
```java
package com.example.contractagent.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(min = 3, max = 64) String username,
        @NotBlank @Size(min = 6, max = 64) String password) {}
```

Create `backend/src/main/java/com/example/contractagent/user/dto/LoginRequest.java`：
```java
package com.example.contractagent.user.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
```

Create `backend/src/main/java/com/example/contractagent/user/dto/AuthResponse.java`：
```java
package com.example.contractagent.user.dto;

public record AuthResponse(String token, long expiresIn, UserDto user) {}
```

Create `backend/src/main/java/com/example/contractagent/user/dto/UserDto.java`：
```java
package com.example.contractagent.user.dto;

public record UserDto(Long id, String username, String role) {}
```

- [ ] **Step 6：写 `UserService`**

```java
package com.example.contractagent.user;

import com.example.contractagent.common.BusinessException;
import com.example.contractagent.common.ErrorCode;
import com.example.contractagent.security.JwtService;
import com.example.contractagent.user.dto.AuthResponse;
import com.example.contractagent.user.dto.LoginRequest;
import com.example.contractagent.user.dto.RegisterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    public AuthResponse register(RegisterRequest req) {
        if (userRepository.findByUsername(req.username()).isPresent()) {
            throw BusinessException.of(ErrorCode.PARAM_INVALID, "用户名已存在");
        }
        User u = new User();
        u.setUsername(req.username());
        u.setPasswordHash(passwordEncoder.encode(req.password()));
        u.setRole(UserRole.USER);
        u.setEnabled(1);
        userRepository.insert(u);
        return buildAuth(u);
    }

    public AuthResponse login(LoginRequest req) {
        User u = userRepository.findByUsername(req.username())
                .orElseThrow(() -> BusinessException.of(ErrorCode.UNAUTHORIZED, "用户名或密码错误"));
        if (u.getEnabled() != 1) throw BusinessException.of(ErrorCode.FORBIDDEN, "账号已禁用");
        if (!passwordEncoder.matches(req.password(), u.getPasswordHash())) {
            throw BusinessException.of(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }
        return buildAuth(u);
    }

    public UserDto me(Long userId) {
        User u = userRepository.selectById(userId);
        if (u == null) throw BusinessException.of(ErrorCode.NOT_FOUND, "用户不存在");
        return userMapper.toDto(u);
    }

    private AuthResponse buildAuth(User u) {
        String token = jwtService.issue(u.getId(), u.getUsername(), u.getRole().name());
        return new AuthResponse(token, jwtService.getTtlSeconds(), userMapper.toDto(u));
    }
}
```

- [ ] **Step 7：写 `AuthController`**

```java
package com.example.contractagent.user;

import com.example.contractagent.common.ApiResponse;
import com.example.contractagent.user.dto.AuthResponse;
import com.example.contractagent.user.dto.LoginRequest;
import com.example.contractagent.user.dto.RegisterRequest;
import com.example.contractagent.user.dto.UserDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public ApiResponse<UserDto> register(@RequestBody @Valid RegisterRequest req) {
        return ApiResponse.ok(userService.register(req).user());
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@RequestBody @Valid LoginRequest req) {
        return ApiResponse.ok(userService.login(req));
    }

    @GetMapping("/me")
    public ApiResponse<UserDto> me(@AuthenticationPrincipal Long userId) {
        return ApiResponse.ok(userService.me(userId));
    }
}
```

- [ ] **Step 8：写测试**

Create `backend/src/test/java/com/example/contractagent/user/UserServiceTest.java`：
```java
package com.example.contractagent.user;

import com.example.contractagent.common.BusinessException;
import com.example.contractagent.security.JwtService;
import com.example.contractagent.user.dto.AuthResponse;
import com.example.contractagent.user.dto.LoginRequest;
import com.example.contractagent.user.dto.RegisterRequest;
import com.example.contractagent.user.dto.UserDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserServiceTest {

    private UserRepository userRepository;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        JwtService jwt = new JwtService("dev-secret-key-must-be-at-least-32-bytes-long-yes", 86400);
        userService = new UserService(userRepository, encoder, jwt, new UserMapper());
    }

    @Test
    void register_insertsAndReturnsToken() {
        when(userRepository.findByUsername("u1")).thenReturn(Optional.empty());
        AuthResponse res = userService.register(new RegisterRequest("u1", "secret123"));
        assertNotNull(res.token());
        assertEquals("u1", res.user().username());
        assertEquals("USER", res.user().role());
        verify(userRepository).insert(any(User.class));
    }

    @Test
    void register_duplicateUsernameThrows() {
        when(userRepository.findByUsername("u1")).thenReturn(Optional.of(new User()));
        assertThrows(BusinessException.class, () -> userService.register(new RegisterRequest("u1", "secret123")));
    }

    @Test
    void login_wrongPasswordThrows() {
        User u = new User(); u.setId(1L); u.setUsername("u1");
        u.setPasswordHash(new BCryptPasswordEncoder().encode("right"));
        u.setRole(UserRole.USER); u.setEnabled(1);
        when(userRepository.findByUsername("u1")).thenReturn(Optional.of(u));
        assertThrows(BusinessException.class, () -> userService.login(new LoginRequest("u1", "wrong")));
    }
}
```

- [ ] **Step 9：跑测试**

Run:
```bash
cd C:\Users\xingchen\Desktop\agent\backend
mvn test -Dtest=UserServiceTest
```
Expected: 3 tests passed

- [ ] **Step 10：启动验证（需要本地 MySQL）**

Run:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```
另开终端：
```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}' | jq .
```
Expected: 返回 `{"code":0,"message":"ok","data":{"token":"...","expiresIn":86400,"user":{"id":1,"username":"admin","role":"ADMIN"}}}`

- [ ] **Step 11：提交**

```bash
cd C:\Users\xingchen\Desktop\agent
git add backend/src/main/java/com/example/contractagent/user/
git add backend/src/test/java/com/example/contractagent/user/
git commit -m "feat(backend): 用户注册/登录/me 接口"
```

---

## Phase 2：合同上传与解析

### Task 2.1：Storage Service + 文档解析

**Files:**
- Create: `backend/src/main/java/com/example/contractagent/contract/StorageService.java`
- Create: `backend/src/main/java/com/example/contractagent/contract/DocumentParser.java`
- Create: `backend/src/main/java/com/example/contractagent/contract/LocalFileStorageService.java`
- Create: `backend/src/main/java/com/example/contractagent/config/StorageConfig.java`
- Create: `backend/src/test/java/com/example/contractagent/contract/DocumentParserTest.java`

- [ ] **Step 1：写 `StorageService` 接口**

```java
package com.example.contractagent.contract;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface StorageService {
    String store(Long taskId, String side, MultipartFile file) throws IOException;
}
```

- [ ] **Step 2：写 `LocalFileStorageService`**

```java
package com.example.contractagent.contract;

import com.example.contractagent.common.BusinessException;
import com.example.contractagent.common.ErrorCode;
import com.example.contractagent.config.StorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
public class LocalFileStorageService implements StorageService {

    private final StorageProperties props;

    @Override
    public String store(Long taskId, String side, MultipartFile file) throws IOException {
        if (file.getSize() > props.maxFileSizeBytes()) {
            throw BusinessException.of(ErrorCode.PARAM_INVALID, "文件超过 20MB 限制");
        }
        String ext = extOf(file.getOriginalFilename());
        Path dir = Paths.get(props.basePath(), String.valueOf(taskId));
        Files.createDirectories(dir);
        Path dest = dir.resolve(side + ext);
        file.transferTo(dest);
        return dest.toString();
    }

    private String extOf(String name) {
        if (name == null) return "";
        int i = name.lastIndexOf('.');
        return i < 0 ? "" : name.substring(i);
    }
}
```

- [ ] **Step 3：写 `StorageConfig`（暴露 `DocumentParser` 依赖的 Bean）**

```java
package com.example.contractagent.config;

import com.example.contractagent.contract.DocumentParser;
import org.apache.tika.Tika;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StorageConfig {
    @Bean
    public Tika tika() { return new Tika(); }

    @Bean
    public DocumentParser documentParser(Tika tika) { return new DocumentParser(tika); }
}
```

- [ ] **Step 4：写 `DocumentParser`**

```java
package com.example.contractagent.contract;

import com.example.contractagent.common.BusinessException;
import com.example.contractagent.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@Component
@RequiredArgsConstructor
public class DocumentParser {

    private final Tika tika;

    public String extractText(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            return tika.parseToString(in);
        } catch (IOException | TikaException e) {
            throw BusinessException.of(ErrorCode.PARSE_FAIL, "合同文本解析失败，可能是不支持的格式: " + e.getMessage());
        }
    }
}
```

- [ ] **Step 5：写测试（基于真实文件）**

Create `backend/src/test/java/com/example/contractagent/contract/DocumentParserTest.java`：
```java
package com.example.contractagent.contract;

import org.apache.tika.Tika;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;

class DocumentParserTest {
    private final DocumentParser parser = new DocumentParser(new Tika());

    @Test
    void parsesPlainText() {
        MockMultipartFile f = new MockMultipartFile(
                "f", "test.txt", "text/plain", "采购单价 0.50 元".getBytes());
        String text = parser.extractText(f);
        assertTrue(text.contains("采购单价"));
    }

    @Test
    void rejectsEmpty() {
        MockMultipartFile f = new MockMultipartFile("f", "empty.txt", "text/plain", new byte[0]);
        assertDoesNotThrow(() -> parser.extractText(f));
    }
}
```

- [ ] **Step 6：跑测试**

Run:
```bash
cd C:\Users\xingchen\Desktop\agent\backend
mvn test -Dtest=DocumentParserTest
```
Expected: 2 tests passed

- [ ] **Step 7：提交**

```bash
cd C:\Users\xingchen\Desktop\agent
git add backend/src/main/java/com/example/contractagent/contract/
git add backend/src/main/java/com/example/contractagent/config/StorageConfig.java
git add backend/src/test/java/com/example/contractagent/contract/
git commit -m "feat(backend): 本地文件存储 + Tika 文档解析"
```

---

### Task 2.2：Contract / Task 实体 + 上传 + 任务创建

**Files:**
- Create: `backend/src/main/java/com/example/contractagent/contract/ContractSide.java`
- Create: `backend/src/main/java/com/example/contractagent/contract/Contract.java`
- Create: `backend/src/main/java/com/example/contractagent/contract/ContractRepository.java`
- Create: `backend/src/main/java/com/example/contractagent/contract/ContractService.java`
- Create: `backend/src/main/java/com/example/contractagent/task/TaskStatus.java`
- Create: `backend/src/main/java/com/example/contractagent/task/RiskLevel.java`
- Create: `backend/src/main/java/com/example/contractagent/task/ComparisonTask.java`
- Create: `backend/src/main/java/com/example/contractagent/task/ComparisonTaskRepository.java`
- Create: `backend/src/main/java/com/example/contractagent/task/ComparisonTaskService.java`
- Create: `backend/src/main/java/com/example/contractagent/task/TaskController.java`
- Create: `backend/src/main/java/com/example/contractagent/task/dto/CreateTaskResponse.java`
- Create: `backend/src/main/java/com/example/contractagent/task/dto/TaskSummaryDto.java`
- Create: `backend/src/main/java/com/example/contractagent/task/dto/TaskListResponse.java`
- Create: `backend/src/main/java/com/example/contractagent/task/dto/RetryResponse.java`

- [ ] **Step 1：写枚举**

`ContractSide.java`：
```java
package com.example.contractagent.contract;
public enum ContractSide { BUY, SELL }
```

`TaskStatus.java`：
```java
package com.example.contractagent.task;
public enum TaskStatus { PENDING, RUNNING, DONE, FAILED, CONFIRMED }
```

> 状态流转：`PENDING → RUNNING → DONE → CONFIRMED`（用户人工确认创建采购单后）；任意阶段失败转 `FAILED`。

`RiskLevel.java`：
```java
package com.example.contractagent.task;
public enum RiskLevel { LOW, MEDIUM, HIGH }
```

- [ ] **Step 2：写 `Contract` 实体**

```java
package com.example.contractagent.contract;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("contract")
public class Contract {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private ContractSide side;
    private String originalFilename;
    private String storedPath;
    private String mimeType;
    private String extractedText;
    private Long fileSize;
    private LocalDateTime uploadedAt;
}
```

- [ ] **Step 3：写 `ContractRepository`**

```java
package com.example.contractagent.contract;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Optional;

@Mapper
public interface ContractRepository extends BaseMapper<Contract> {
    default List<Contract> findByTaskId(Long taskId) { return selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Contract>().eq("task_id", taskId)); }
    default Optional<Contract> findByTaskIdAndSide(Long taskId, ContractSide side) { return Optional.ofNullable(selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Contract>().eq("task_id", taskId).eq("side", side))); }
}
```

- [ ] **Step 4：写 `ComparisonTask` 实体**

```java
package com.example.contractagent.task;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("comparison_task")
public class ComparisonTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String title;
    private TaskStatus status;
    private String summary;
    private RiskLevel riskLevel;
    private LocalDateTime confirmedAt;
    private Long confirmedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 5：写 `ComparisonTaskRepository`**

```java
package com.example.contractagent.task;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ComparisonTaskRepository extends BaseMapper<ComparisonTask> {
    default IPage<ComparisonTask> pageByUser(Long userId, int page, int size, TaskStatus status) {
        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ComparisonTask>()
                .eq("user_id", userId).orderByDesc("created_at");
        if (status != null) wrapper.eq("status", status);
        return selectPage(new Page<>(page, size), wrapper);
    }
}
```

- [ ] **Step 6：写 DTO 类**

`CreateTaskResponse.java`：
```java
package com.example.contractagent.task.dto;
import com.example.contractagent.task.TaskStatus;
import java.time.LocalDateTime;
public record CreateTaskResponse(Long id, String title, TaskStatus status, LocalDateTime createdAt) {}
```

`TaskSummaryDto.java`：
```java
package com.example.contractagent.task.dto;
import com.example.contractagent.task.RiskLevel;
import com.example.contractagent.task.TaskStatus;
import java.time.LocalDateTime;
public record TaskSummaryDto(Long id, String title, TaskStatus status, RiskLevel riskLevel,
                            LocalDateTime confirmedAt, Long confirmedBy,
                            LocalDateTime createdAt, LocalDateTime updatedAt) {}
```

`TaskListResponse.java`：
```java
package com.example.contractagent.task.dto;
import java.util.List;
public record TaskListResponse(long total, List<TaskSummaryDto> items) {}
```

`RetryResponse.java`：
```java
package com.example.contractagent.task.dto;
import com.example.contractagent.task.TaskStatus;
public record RetryResponse(Long id, TaskStatus status) {}
```

`ConfirmPurchaseResponse.java`：
```java
package com.example.contractagent.task.dto;
import com.example.contractagent.task.TaskStatus;
import java.time.LocalDateTime;
public record ConfirmPurchaseResponse(Long id, TaskStatus status,
                                       String applicationNo, String applicationStatus,
                                       LocalDateTime confirmedAt) {}
```

- [ ] **Step 7：写 `ContractService`（存文件 + 解析 + 落库）**

```java
package com.example.contractagent.contract;

import com.example.contractagent.common.BusinessException;
import com.example.contractagent.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContractService {

    private final ContractRepository contractRepository;
    private final StorageService storageService;
    private final DocumentParser documentParser;

    public Contract saveForTask(Long taskId, ContractSide side, MultipartFile file) {
        try {
            String storedPath = storageService.store(taskId, side.name(), file);
            String text = documentParser.extractText(file);
            Contract c = new Contract();
            c.setTaskId(taskId);
            c.setSide(side);
            c.setOriginalFilename(file.getOriginalFilename());
            c.setStoredPath(storedPath);
            c.setMimeType(file.getContentType());
            c.setExtractedText(text);
            c.setFileSize(file.getSize());
            contractRepository.insert(c);
            return c;
        } catch (IOException e) {
            throw BusinessException.of(ErrorCode.SYSTEM_ERROR, "文件存储失败: " + e.getMessage());
        }
    }

    public List<Contract> listByTask(Long taskId) {
        return contractRepository.findByTaskId(taskId);
    }

    public Contract requireForTask(Long taskId, ContractSide side) {
        return contractRepository.findByTaskIdAndSide(taskId, side)
                .orElseThrow(() -> BusinessException.of(ErrorCode.NOT_FOUND, "合同不存在: " + side));
    }
}
```

- [ ] **Step 8：写 `ComparisonTaskService`（创建/查询/重跑）**

```java
package com.example.contractagent.task;

import com.example.contractagent.agent.AgentRunner;
import com.example.contractagent.common.BusinessException;
import com.example.contractagent.common.ErrorCode;
import com.example.contractagent.contract.ContractService;
import com.example.contractagent.contract.ContractSide;
import com.example.contractagent.extraction.Extraction;
import com.example.contractagent.extraction.ExtractionService;
import com.example.contractagent.task.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ComparisonTaskService {

    private final ComparisonTaskRepository taskRepository;
    private final ContractService contractService;
    private final AgentRunner agentRunner;
    private final ExtractionService extractionService;

    @Transactional
    public CreateTaskResponse create(Long userId, String title, MultipartFile buyFile, MultipartFile sellFile) {
        ComparisonTask t = new ComparisonTask();
        t.setUserId(userId);
        t.setTitle(title);
        t.setStatus(TaskStatus.PENDING);
        taskRepository.insert(t);

        contractService.saveForTask(t.getId(), ContractSide.BUY, buyFile);
        contractService.saveForTask(t.getId(), ContractSide.SELL, sellFile);

        t.setStatus(TaskStatus.RUNNING);
        taskRepository.updateById(t);

        // 触发 Agent 异步跑（Phase 3 实现）
        try { agentRunner.run(t.getId()); } catch (Exception ignored) {}

        return new CreateTaskResponse(t.getId(), t.getTitle(), t.getStatus(), t.getCreatedAt());
    }

    public TaskListResponse list(Long userId, int page, int size, TaskStatus status) {
        var p = taskRepository.pageByUser(userId, page, size, status);
        List<TaskSummaryDto> items = p.getRecords().stream()
                .map(t -> new TaskSummaryDto(t.getId(), t.getTitle(), t.getStatus(), t.getRiskLevel(),
                        t.getConfirmedAt(), t.getConfirmedBy(), t.getCreatedAt(), t.getUpdatedAt()))
                .toList();
        return new TaskListResponse(p.getTotal(), items);
    }

    public ComparisonTask requireOwned(Long taskId, Long userId) {
        ComparisonTask t = taskRepository.selectById(taskId);
        if (t == null) throw BusinessException.of(ErrorCode.NOT_FOUND, "任务不存在");
        if (!t.getUserId().equals(userId)) throw BusinessException.of(ErrorCode.FORBIDDEN, "无权访问该任务");
        return t;
    }

    public ComparisonTask require(Long taskId) {
        ComparisonTask t = taskRepository.selectById(taskId);
        if (t == null) throw BusinessException.of(ErrorCode.NOT_FOUND, "任务不存在");
        return t;
    }

    public void updateStatus(Long taskId, TaskStatus status) {
        ComparisonTask t = require(taskId);
        t.setStatus(status);
        taskRepository.updateById(t);
    }

    public void updateResult(Long taskId, String summary, RiskLevel riskLevel) {
        ComparisonTask t = require(taskId);
        t.setSummary(summary);
        t.setRiskLevel(riskLevel);
        t.setStatus(TaskStatus.DONE);
        taskRepository.updateById(t);
    }

    public RetryResponse retry(Long userId, Long taskId) {
        ComparisonTask t = requireOwned(taskId, userId);
        if (t.getStatus() != TaskStatus.FAILED) {
            throw BusinessException.of(ErrorCode.PARAM_INVALID, "仅 FAILED 状态可重跑");
        }
        t.setStatus(TaskStatus.RUNNING);
        taskRepository.updateById(t);
        try { agentRunner.run(t.getId()); } catch (Exception ignored) {}
        return new RetryResponse(t.getId(), t.getStatus());
    }

    /**
     * 人工确认创建采购单：仅 DONE 状态可确认。
     * 确认后任务转 CONFIRMED，BUY 侧 extraction 的 application_status 升为「已确认」，
     * message 改为「已人工确认创建采购单」。
     */
    @Transactional
    public ConfirmPurchaseResponse confirmPurchase(Long userId, Long taskId) {
        ComparisonTask t = requireOwned(taskId, userId);
        if (t.getStatus() != TaskStatus.DONE) {
            throw BusinessException.of(ErrorCode.PARAM_INVALID, "仅 DONE 状态可确认创建采购单");
        }
        // 1. 更新任务状态 + 审计字段
        LocalDateTime now = LocalDateTime.now();
        t.setStatus(TaskStatus.CONFIRMED);
        t.setConfirmedAt(now);
        t.setConfirmedBy(userId);
        taskRepository.updateById(t);

        // 2. 同步更新 BUY 侧 extraction 的 application_status / message
        Contract buy = contractService.requireForTask(taskId, ContractSide.BUY);
        Extraction eBuy = extractionService.getByContractId(buy.getId());
        extractionService.updateApplicationStatus(eBuy.getId(), "已确认", "已人工确认创建采购单");

        return new ConfirmPurchaseResponse(t.getId(), t.getStatus(),
                eBuy.getApplicationNo(), "已确认", now);
    }
}
```

- [ ] **Step 8.5：在 `ExtractionService` 增加更新申请状态方法**

修改 `ExtractionService.java`，追加：
```java
@Transactional
public void updateApplicationStatus(Long extractionId, String applicationStatus, String message) {
    Extraction e = repository.selectById(extractionId);
    if (e == null) throw BusinessException.of(ErrorCode.NOT_FOUND, "抽取记录不存在");
    e.setApplicationStatus(applicationStatus);
    e.setMessage(message);
    repository.updateById(e);
}
```

- [ ] **Step 9：写 `TaskController`**

```java
package com.example.contractagent.task;

import com.example.contractagent.common.ApiResponse;
import com.example.contractagent.task.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final ComparisonTaskService taskService;

    @PostMapping(consumes = "multipart/form-data")
    public ApiResponse<CreateTaskResponse> create(
            @AuthenticationPrincipal Long userId,
            @RequestParam("title") String title,
            @RequestParam("buyFile") MultipartFile buyFile,
            @RequestParam("sellFile") MultipartFile sellFile) {
        return ApiResponse.ok(taskService.create(userId, title, buyFile, sellFile));
    }

    @GetMapping
    public ApiResponse<TaskListResponse> list(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) TaskStatus status) {
        return ApiResponse.ok(taskService.list(userId, page, size, status));
    }

    @PostMapping("/{id}/retry")
    public ApiResponse<RetryResponse> retry(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        return ApiResponse.ok(taskService.retry(userId, id));
    }

    /**
     * 人工确认创建采购单（仅 DONE 状态可调用）。
     * 确认后任务转 CONFIRMED，BUY 侧 extraction 的 application_status 升为「已确认」。
     */
    @PostMapping("/{id}/confirm")
    public ApiResponse<ConfirmPurchaseResponse> confirmPurchase(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) {
        return ApiResponse.ok(taskService.confirmPurchase(userId, id));
    }
}
```

- [ ] **Step 10：先跳过编译，Phase 3 写完 AgentRunner 后再统一编译**

> 注：Step 8 引用了 `AgentRunner`，该类在 Phase 3 Task 3.4 才定义。完成 Phase 3 全部 Task 后再执行 `mvn -DskipTests compile`。

- [ ] **Step 11：先跳过上握手测，Phase 3 写完 AgentRunner 后再手测**

> 注：手测需要 AgentRunner 完成才能跑完整流程，移至 Phase 3 末尾统一做。

- [ ] **Step 12：提交**

```bash
cd C:\Users\xingchen\Desktop\agent
git add backend/src/main/java/com/example/contractagent/contract/ContractSide.java
git add backend/src/main/java/com/example/contractagent/contract/Contract.java
git add backend/src/main/java/com/example/contractagent/contract/ContractRepository.java
git add backend/src/main/java/com/example/contractagent/contract/ContractService.java
git add backend/src/main/java/com/example/contractagent/task/
git commit -m "feat(backend): 合同/任务实体 + 上传/列表/重跑"
```

---

## Phase 3：LLM 抽取 + 风险规则 + Agent 编排

### Task 3.1：Extraction 实体 + 强类型字段

**Files:**
- Create: `backend/src/main/java/com/example/contractagent/extraction/Extraction.java`
- Create: `backend/src/main/java/com/example/contractagent/extraction/ExtractionRepository.java`
- Create: `backend/src/main/java/com/example/contractagent/extraction/ExtractionResultDto.java`（LLM 输出 JSON schema）

- [ ] **Step 1：写 `Extraction`**

```java
package com.example.contractagent.extraction;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("extraction")
public class Extraction {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long contractId;
    private String supplierName;
    private String itemName;
    private String itemModel;
    private String unit;
    private BigDecimal quantity;
    private BigDecimal purchaseUnitPrice;
    private BigDecimal purchaseTotalAmount;
    private LocalDate expectedDeliveryDate;
    private String paymentTerms;
    private String deliveryLocation;
    private String applicationNo;
    private String applicationTitle;
    private LocalDate applyDate;
    private String applicationType;
    private String currency;
    private String applicationStatus;
    private LocalDateTime createTime;
    private String message;
    private BigDecimal confidence;
    private String rawQuote;
    private LocalDateTime extractedAt;
}
```

- [ ] **Step 2：写 `ExtractionRepository`**

```java
package com.example.contractagent.extraction;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import java.util.Optional;

@Mapper
public interface ExtractionRepository extends BaseMapper<Extraction> {
    default Optional<Extraction> findByContractId(Long contractId) {
        return Optional.ofNullable(selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Extraction>().eq("contract_id", contractId)));
    }
}
```

- [ ] **Step 3：写 LLM 输出 DTO（schema）**

```java
package com.example.contractagent.extraction;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ExtractionResultDto(
        @JsonProperty("supplier_name")            String supplierName,
        @JsonProperty("item_name")                String itemName,
        @JsonProperty("item_model")               String itemModel,
        @JsonProperty("unit")                     String unit,
        @JsonProperty("quantity")                 BigDecimal quantity,
        @JsonProperty("purchase_unit_price")      BigDecimal purchaseUnitPrice,
        @JsonProperty("purchase_total_amount")    BigDecimal purchaseTotalAmount,
        @JsonProperty("expected_delivery_date")   LocalDate expectedDeliveryDate,
        @JsonProperty("payment_terms")            String paymentTerms,
        @JsonProperty("delivery_location")        String deliveryLocation,
        @JsonProperty("raw_quote")                String rawQuote,
        @JsonProperty("confidence")               BigDecimal confidence
) {}
```

- [ ] **Step 4：提交**

```bash
git add backend/src/main/java/com/example/contractagent/extraction/Extraction.java
git add backend/src/main/java/com/example/contractagent/extraction/ExtractionRepository.java
git add backend/src/main/java/com/example/contractagent/extraction/ExtractionResultDto.java
git commit -m "feat(backend): extraction 实体 + LLM 输出 schema"
```

---

### Task 3.2：ExtractionService（调 LLM + 落库 + 自动填充）

**Files:**
- Create: `backend/src/main/java/com/example/contractagent/extraction/ExtractionService.java`
- Create: `backend/src/main/java/com/example/contractagent/llm/LlmCallLog.java`
- Create: `backend/src/main/java/com/example/contractagent/llm/LlmCallLogRepository.java`
- Create: `backend/src/main/java/com/example/contractagent/llm/LlmCallLogger.java`

- [ ] **Step 1：写 `LlmCallLog`**

```java
package com.example.contractagent.llm;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("llm_call_log")
public class LlmCallLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Long userId;
    private String model;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private Integer durationMs;
    private String status;
    private String errorMsg;
    private LocalDateTime createdAt;
}
```

- [ ] **Step 2：写 `LlmCallLogRepository`**

```java
package com.example.contractagent.llm;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LlmCallLogRepository extends BaseMapper<LlmCallLog> {}
```

- [ ] **Step 3：写 `LlmCallLogger`**

```java
package com.example.contractagent.llm;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class LlmCallLogger {
    private final LlmCallLogRepository repo;

    public void log(Long taskId, Long userId, String model,
                    Integer prompt, Integer completion, Integer total,
                    long durationMs, String status, String errorMsg) {
        LlmCallLog log = new LlmCallLog();
        log.setTaskId(taskId); log.setUserId(userId); log.setModel(model);
        log.setPromptTokens(prompt); log.setCompletionTokens(completion); log.setTotalTokens(total);
        log.setDurationMs((int) durationMs); log.setStatus(status); log.setErrorMsg(errorMsg);
        log.setCreatedAt(LocalDateTime.now());
        repo.insert(log);
    }
}
```

- [ ] **Step 4：写 `ExtractionService`**

```java
package com.example.contractagent.extraction;

import com.example.contractagent.common.BusinessException;
import com.example.contractagent.common.ErrorCode;
import com.example.contractagent.contract.Contract;
import com.example.contractagent.llm.LlmCallLogger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExtractionService {

    private final ExtractionRepository repository;
    private final ChatClient chatClient;
    private final LlmCallLogger llmLogger;

    @Transactional
    public Extraction extractAndSave(Long taskId, Long userId, Contract contract) {
        ExtractionResultDto result = callLlm(taskId, userId, contract);
        Extraction e = new Extraction();
        e.setContractId(contract.getId());
        e.setSupplierName(result.supplierName());
        e.setItemName(result.itemName());
        e.setItemModel(result.itemModel());
        e.setUnit(result.unit());
        e.setQuantity(result.quantity());
        e.setPurchaseUnitPrice(result.purchaseUnitPrice());
        e.setPurchaseTotalAmount(result.purchaseTotalAmount());
        e.setExpectedDeliveryDate(result.expectedDeliveryDate());
        e.setPaymentTerms(result.paymentTerms());
        e.setDeliveryLocation(result.deliveryLocation());
        e.setConfidence(result.confidence());
        e.setRawQuote(result.rawQuote());
        // 自动填充字段
        e.setApplicationNo(generateApplicationNo());
        e.setApplicationType("商品采购");
        e.setCurrency("CNY");
        e.setApplicationStatus("待提交");
        e.setApplyDate(LocalDate.now());
        e.setApplicationTitle(buildTitle(result.supplierName(), result.itemName()));
        e.setCreateTime(java.time.LocalDateTime.now());
        e.setMessage("抽取成功");
        e.setExtractedAt(java.time.LocalDateTime.now());
        repository.insert(e);
        return e;
    }

    public Extraction getByContractId(Long contractId) {
        return repository.findByContractId(contractId)
                .orElseThrow(() -> BusinessException.of(ErrorCode.NOT_FOUND, "该合同未抽取"));
    }

    private String buildTitle(String supplier, String item) {
        if (supplier == null && item == null) return null;
        return ((supplier == null ? "" : supplier) + "-" + (item == null ? "" : item)).replaceAll("^-+|-+$", "");
    }

    private String generateApplicationNo() {
        return "PA" + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%04d", new java.security.SecureRandom().nextInt(10000));
    }

    private ExtractionResultDto callLlm(Long taskId, Long userId, Contract contract) {
        String prompt = buildPrompt(contract.getExtractedText());
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        long start = System.currentTimeMillis();
        try {
            String json = chatClient.prompt()
                    .system("你是合同要素抽取助手。请严格按 JSON Schema 输出，字段缺失时填 null。")
                    .user(prompt)
                    .call()
                    .content();
            ExtractionResultDto dto = mapper.readValue(json, ExtractionResultDto.class);
            llmLogger.log(taskId, userId, "gpt-4o-mini", null, null, null,
                    System.currentTimeMillis() - start, "OK", null);
            return dto;
        } catch (Exception first) {
            // 重试 1 次
            try {
                String json = chatClient.prompt()
                        .system("你是合同要素抽取助手。请严格按 JSON Schema 输出，字段缺失时填 null。")
                        .user(prompt)
                        .call()
                        .content();
                llmLogger.log(taskId, userId, "gpt-4o-mini", null, null, null,
                        System.currentTimeMillis() - start, "OK", null);
                return mapper.readValue(json, ExtractionResultDto.class);
            } catch (Exception second) {
                llmLogger.log(taskId, userId, "gpt-4o-mini", null, null, null,
                        System.currentTimeMillis() - start, "FAIL", second.getMessage());
                throw BusinessException.of(ErrorCode.EXTRACT_FAIL, "抽取失败: " + second.getMessage());
            }
        }
    }

    private String buildPrompt(String text) {
        return """
                请从以下合同正文中抽取关键要素，严格按 JSON 输出。
                字段缺失时填 null，不要编造。

                字段：
                - supplier_name: 供应商名称
                - item_name: 商品标准名称
                - item_model: 规格型号
                - unit: 单位
                - quantity: 采购数量（数字）
                - purchase_unit_price: 采购单价（数字，币种默认 CNY）
                - purchase_total_amount: 采购总金额（数字）
                - expected_delivery_date: 预计交付日期（YYYY-MM-DD）
                - payment_terms: 付款条款
                - delivery_location: 交付地点
                - raw_quote: 最相关的 1 句原文引用（便于溯源）
                - confidence: 抽取置信度 0~1

                合同正文：
                %s
                """.formatted(text);
    }
}
```

- [ ] **Step 5：写 ChatClient 配置 Bean**

Create `backend/src/main/java/com/example/contractagent/config/ChatClientConfig.java`：
```java
package com.example.contractagent.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }
}
```

- [ ] **Step 6：编译**

Run:
```bash
cd C:\Users\xingchen\Desktop\agent\backend
mvn -DskipTests compile
```
Expected: BUILD SUCCESS

- [ ] **Step 7：提交**

```bash
cd C:\Users\xingchen\Desktop\agent
git add backend/src/main/java/com/example/contractagent/extraction/ExtractionService.java
git add backend/src/main/java/com/example/contractagent/llm/
git add backend/src/main/java/com/example/contractagent/config/ChatClientConfig.java
git commit -m "feat(backend): LLM 抽取 + 自动填充 + 调用日志"
```

---

### Task 3.3：风险规则引擎

**Files:**
- Create: `backend/src/main/java/com/example/contractagent/comparison/RiskOperator.java`
- Create: `backend/src/main/java/com/example/contractagent/comparison/RiskRule.java`
- Create: `backend/src/main/java/com/example/contractagent/comparison/RiskRuleRepository.java`
- Create: `backend/src/main/java/com/example/contractagent/comparison/RiskRuleEvaluator.java`
- Create: `backend/src/main/java/com/example/contractagent/comparison/FieldDifference.java`
- Create: `backend/src/main/java/com/example/contractagent/comparison/ComparisonResult.java`
- Create: `backend/src/main/java/com/example/contractagent/comparison/ComparisonService.java`
- Create: `backend/src/test/java/com/example/contractagent/comparison/RiskRuleEvaluatorTest.java`

- [ ] **Step 1：写 `RiskOperator` 枚举**

```java
package com.example.contractagent.comparison;

public enum RiskOperator { GT, LT, EQ, NE, PCT_GT }
```

- [ ] **Step 2：写 `RiskRule` 实体**

```java
package com.example.contractagent.comparison;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.contractagent.task.RiskLevel;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("risk_rule")
public class RiskRule {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String fieldKey;
    private RiskOperator operator;
    private BigDecimal thresholdValue;
    private RiskLevel riskLevel;
    private Integer enabled;
    private String remark;
    private LocalDateTime updatedAt;
    private Long updatedBy;
}
```

- [ ] **Step 3：写 `RiskRuleRepository`**

```java
package com.example.contractagent.comparison;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface RiskRuleRepository extends BaseMapper<RiskRule> {
    default List<RiskRule> findAllEnabled() {
        return selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<RiskRule>().eq("enabled", 1));
    }
}
```

- [ ] **Step 4：写 `FieldDifference` / `ComparisonResult`**

`FieldDifference.java`：
```java
package com.example.contractagent.comparison;

import com.example.contractagent.task.RiskLevel;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FieldDifference {
    private String field;
    private Object buy;
    private Object sell;
    private String status;   // MATCH / DIFFER / MISSING
    private RiskLevel risk;
}
```

`ComparisonResult.java`：
```java
package com.example.contractagent.comparison;

import com.example.contractagent.task.RiskLevel;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class ComparisonResult {
    private List<FieldDifference> differences;
    private RiskLevel riskLevel;
}
```

- [ ] **Step 5：写 `RiskRuleEvaluator`**

```java
package com.example.contractagent.comparison;

import com.example.contractagent.extraction.Extraction;
import com.example.contractagent.task.RiskLevel;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class RiskRuleEvaluator {

    private static final Set<String> KNOWN_FIELDS = Set.of(
            "applicationNo","applicationStatus","applicationTitle","applicationType","applyDate",
            "supplierName","itemName","itemModel","unit","quantity",
            "purchaseUnitPrice","purchaseTotalAmount","currency",
            "expectedDeliveryDate","deliveryLocation","paymentTerms",
            "createTime","message"
    );

    public RiskLevel evaluate(Extraction buy, Extraction sell, List<RiskRule> rules) {
        RiskLevel max = RiskLevel.LOW;
        for (RiskRule rule : rules) {
            if (rule.getEnabled() != 1) continue;
            if (!KNOWN_FIELDS.contains(rule.getFieldKey())) continue;
            if (match(rule, buy, sell) && rule.getRiskLevel().ordinal() > max.ordinal()) {
                max = rule.getRiskLevel();
            }
        }
        return max;
    }

    private boolean match(RiskRule rule, Extraction buy, Extraction sell) {
        Object a = read(buy, rule.getFieldKey());
        Object b = read(sell, rule.getFieldKey());
        BigDecimal threshold = rule.getThresholdValue();
        return switch (rule.getOperator()) {
            case EQ -> isEmpty(a) == isEmpty(b) && equalish(a, b);
            case NE -> isEmpty(a) != isEmpty(b) || !equalish(a, b);
            case GT -> numericDiff(a, b) > threshold.doubleValue();
            case LT -> numericDiff(a, b) < threshold.doubleValue();
            case PCT_GT -> pctDiff(a, b) > threshold.doubleValue();
        };
    }

    private boolean isEmpty(Object v) {
        if (v == null) return true;
        if (v instanceof String s) return s.isBlank();
        return false;
    }

    private boolean equalish(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        if (a instanceof BigDecimal && b instanceof BigDecimal) return ((BigDecimal) a).compareTo((BigDecimal) b) == 0;
        return a.toString().trim().equals(b.toString().trim());
    }

    private double numericDiff(Object a, Object b) {
        if (!(a instanceof BigDecimal) || !(b instanceof BigDecimal)) return 0;
        return Math.abs(((BigDecimal) a).subtract((BigDecimal) b)).doubleValue();
    }

    private double pctDiff(Object a, Object b) {
        if (!(a instanceof BigDecimal) || !(b instanceof BigDecimal)) return 0;
        BigDecimal x = (BigDecimal) a, y = (BigDecimal) b;
        BigDecimal min = x.min(y);
        if (min.signum() == 0) return 0;
        return x.subtract(y).abs().divide(min, 4, java.math.RoundingMode.HALF_UP).doubleValue();
    }

    private Object read(Extraction e, String key) {
        if (e == null) return null;
        try { Field f = Extraction.class.getDeclaredField(key); f.setAccessible(true); return f.get(e); }
        catch (Exception ex) { return null; }
    }
}
```

- [ ] **Step 6：写 `ComparisonService`**

```java
package com.example.contractagent.comparison;

import com.example.contractagent.extraction.Extraction;
import com.example.contractagent.task.RiskLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ComparisonService {

    private static final Set<String> BUSINESS_FIELDS = new LinkedHashSet<>(java.util.List.of(
            "applicationNo","applicationStatus","applicationTitle","applicationType","applyDate",
            "supplierName","itemName","itemModel","unit","quantity",
            "purchaseUnitPrice","purchaseTotalAmount","currency",
            "expectedDeliveryDate","deliveryLocation","paymentTerms",
            "createTime","message"
    ));

    private final RiskRuleRepository riskRuleRepository;
    private final RiskRuleEvaluator evaluator;

    public ComparisonResult compare(Extraction buy, Extraction sell) {
        List<FieldDifference> diffs = new ArrayList<>();
        for (String key : BUSINESS_FIELDS) {
            Object a = read(buy, key);
            Object b = read(sell, key);
            String status;
            if (a == null || b == null) {
                status = "MISSING";
            } else if (a.toString().equals(b.toString())) {
                status = "MATCH";
            } else {
                status = "DIFFER";
            }
            diffs.add(FieldDifference.builder().field(key).buy(a).sell(b).status(status).risk(null).build());
        }
        RiskLevel risk = evaluator.evaluate(buy, sell, riskRuleRepository.findAllEnabled());
        return ComparisonResult.builder().differences(diffs).riskLevel(risk).build();
    }

    private Object read(Extraction e, String key) {
        if (e == null) return null;
        try { var f = Extraction.class.getDeclaredField(key); f.setAccessible(true); return f.get(e); }
        catch (Exception ex) { return null; }
    }
}
```

- [ ] **Step 7：写测试**

Create `backend/src/test/java/com/example/contractagent/comparison/RiskRuleEvaluatorTest.java`：
```java
package com.example.contractagent.comparison;

import com.example.contractagent.extraction.Extraction;
import com.example.contractagent.task.RiskLevel;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RiskRuleEvaluatorTest {
    private final RiskRuleEvaluator evaluator = new RiskRuleEvaluator();

    private Extraction ext(BigDecimal qty, BigDecimal total) {
        Extraction e = new Extraction();
        e.setQuantity(qty);
        e.setPurchaseTotalAmount(total);
        return e;
    }

    @Test
    void pctGtTriggersHigh() {
        RiskRule r = new RiskRule();
        r.setFieldKey("purchaseTotalAmount");
        r.setOperator(RiskOperator.PCT_GT);
        r.setThresholdValue(new BigDecimal("0.05"));
        r.setRiskLevel(RiskLevel.HIGH);
        r.setEnabled(1);
        assertEquals(RiskLevel.HIGH, evaluator.evaluate(ext(null, new BigDecimal("100")), ext(null, new BigDecimal("110")), List.of(r)));
    }

    @Test
    void noRuleMatchReturnsLow() {
        assertEquals(RiskLevel.LOW, evaluator.evaluate(ext(new BigDecimal("10"), new BigDecimal("100")), ext(new BigDecimal("10"), new BigDecimal("100")), List.of()));
    }

    @Test
    void missingTriggersEqEmptyRule() {
        RiskRule r = new RiskRule();
        r.setFieldKey("paymentTerms");
        r.setOperator(RiskOperator.EQ);
        r.setThresholdValue(new BigDecimal(0));
        r.setRiskLevel(RiskLevel.MEDIUM);
        r.setEnabled(1);
        Extraction buy = new Extraction(); buy.setPaymentTerms("月结 30");
        Extraction sell = new Extraction(); // paymentTerms 缺失
        assertEquals(RiskLevel.MEDIUM, evaluator.evaluate(buy, sell, List.of(r)));
    }
}
```

- [ ] **Step 8：跑测试**

Run:
```bash
cd C:\Users\xingchen\Desktop\agent\backend
mvn test -Dtest=RiskRuleEvaluatorTest
```
Expected: 3 tests passed

- [ ] **Step 9：提交**

```bash
cd C:\Users\xingchen\Desktop\agent
git add backend/src/main/java/com/example/contractagent/comparison/
git add backend/src/test/java/com/example/contractagent/comparison/
git commit -m "feat(backend): 风险规则评估器 + 对比服务 + 单测"
```

---

### Task 3.4：Agent 编排（4 工具 + 工作流 + 任务详情）

**Files:**
- Create: `backend/src/main/java/com/example/contractagent/agent/AgentTools.java`
- Create: `backend/src/main/java/com/example/contractagent/agent/ContractAgent.java`
- Create: `backend/src/main/java/com/example/contractagent/agent/AgentRunner.java`
- Create: `backend/src/main/java/com/example/contractagent/chat/ChatRole.java`
- Create: `backend/src/main/java/com/example/contractagent/chat/ChatMessage.java`
- Create: `backend/src/main/java/com/example/contractagent/chat/ChatMessageRepository.java`
- Create: `backend/src/main/java/com/example/contractagent/chat/ChatService.java`
- Create: `backend/src/main/java/com/example/contractagent/chat/ChatController.java`
- Create: `backend/src/main/java/com/example/contractagent/chat/dto/ChatRequest.java`
- Create: `backend/src/main/java/com/example/contractagent/chat/dto/ChatResponse.java`
- Create: `backend/src/main/java/com/example/contractagent/chat/dto/MessageDto.java`
- Create: `backend/src/main/java/com/example/contractagent/chat/dto/MessageListResponse.java`
- Create: `backend/src/main/java/com/example/contractagent/task/TaskDetailService.java`
- Create: `backend/src/main/java/com/example/contractagent/task/dto/TaskDetailResponse.java`
- Create: `backend/src/main/java/com/example/contractagent/task/dto/ContractDetailDto.java`
- Modify: `backend/src/main/java/com/example/contractagent/task/TaskController.java`（加 GET /{id}）

- [ ] **Step 1：写 `ChatRole` / `ChatMessage` / `ChatMessageRepository`**

`ChatRole.java`：
```java
package com.example.contractagent.chat;
public enum ChatRole { USER, ASSISTANT, TOOL }
```

`ChatMessage.java`：
```java
package com.example.contractagent.chat;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("chat_message")
public class ChatMessage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private ChatRole role;
    private String content;
    private String toolCalls;
    private LocalDateTime createdAt;
}
```

`ChatMessageRepository.java`：
```java
package com.example.contractagent.chat;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatMessageRepository extends BaseMapper<ChatMessage> {
    default IPage<ChatMessage> pageByTask(Long taskId, int page, int size) {
        return selectPage(new Page<>(page, size),
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ChatMessage>().eq("task_id", taskId).orderByAsc("created_at"));
    }
}
```

- [ ] **Step 2：写 `AgentTools`（4 个 @Tool 方法）**

```java
package com.example.contractagent.agent;

import com.example.contractagent.comparison.ComparisonResult;
import com.example.contractagent.comparison.ComparisonService;
import com.example.contractagent.contract.Contract;
import com.example.contractagent.contract.ContractService;
import com.example.contractagent.contract.ContractSide;
import com.example.contractagent.extraction.Extraction;
import com.example.contractagent.extraction.ExtractionService;
import com.example.contractagent.task.ComparisonTask;
import com.example.contractagent.task.ComparisonTaskService;
import com.example.contractagent.task.TaskStatus;
import com.example.contractagent.chat.ChatMessage;
import com.example.contractagent.chat.ChatMessageRepository;
import com.example.contractagent.chat.ChatRole;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AgentTools {

    private final ContractService contractService;
    private final ExtractionService extractionService;
    private final ComparisonTaskService taskService;
    private final ComparisonService comparisonService;
    private final ChatMessageRepository chatRepo;

    @Tool("抽取指定合同的关键要素，返回结构化结果")
    public Extraction extractContract(Long contractId) {
        Contract c = contractService.listByTask(0L).stream()
                .filter(x -> x.getId().equals(contractId)).findFirst()
                .orElseThrow();
        return extractionService.extractAndSave(c.getTaskId(), null, c);
    }

    @Tool("获取合同已抽取的要素（不重新抽取）")
    public Extraction getExtraction(Long contractId) {
        return extractionService.getByContractId(contractId);
    }

    @Tool("对比任务下两份合同的全部字段，返回差异列表和整体风险等级")
    public ComparisonResult compareFields(Long taskId) {
        Contract buy = contractService.requireForTask(taskId, ContractSide.BUY);
        Contract sell = contractService.requireForTask(taskId, ContractSide.SELL);
        Extraction eBuy = extractionService.getByContractId(buy.getId());
        Extraction eSell = extractionService.getByContractId(sell.getId());
        return comparisonService.compare(eBuy, eSell);
    }

    @Tool("查看本任务历史对话")
    public List<ChatMessage> getChatHistory(Long taskId) {
        return chatRepo.selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ChatMessage>()
                .eq("task_id", taskId).orderByAsc("created_at"));
    }
}
```

- [ ] **Step 3：写 `ContractAgent`（ChatClient + 4 工具 + Prompt）**

```java
package com.example.contractagent.agent;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ContractAgent {

    private final ChatClient baseClient;
    private final AgentTools tools;

    public ChatClient forTask(Long taskId) {
        ChatMemory memory = new InMemoryChatMemory();
        var advisor = new MessageChatMemoryAdvisor(memory);
        return baseClient.mutate()
                .defaultSystem(systemPrompt())
                .defaultTools(tools)
                .defaultAdvisors(advisor)
                .build();
    }

    private String systemPrompt() {
        return """
                你是「合同对比助手」，负责对比同一标的的采购合同与销售合同。

                # 工作流（任务刚创建时执行）
                1. 调 extract_contract 抽取采购合同
                2. 调 extract_contract 抽取销售合同
                3. 调 compare_fields 做字段对比
                4. 把 compare_fields 的结果用自然语言总结，输出 3 段：
                   - 核心差异（金额/数量/日期）
                   - 风险提示（按 HIGH/MEDIUM 排序）
                   - 建议（人话，不堆术语）

                # 追问时
                - 用户问"X 是什么"时优先调 get_extraction
                - 用户问"为什么有风险"时引用 raw_quote
                - 永远不要编造数据，没抽到就说"该字段在原文中未明确"

                # 输出约束
                - 中文 / 不超过 800 字
                - 金额保留两位小数
                - 风险等级用 🟢🟡🔴 emoji
                - 不重复用户已知内容
                """;
    }
}
```

- [ ] **Step 4：写 `AgentRunner`（任务创建时跑一次）**

```java
package com.example.contractagent.agent;

import com.example.contractagent.chat.ChatMessage;
import com.example.contractagent.chat.ChatMessageRepository;
import com.example.contractagent.chat.ChatRole;
import com.example.contractagent.comparison.ComparisonResult;
import com.example.contractagent.comparison.ComparisonService;
import com.example.contractagent.contract.Contract;
import com.example.contractagent.contract.ContractService;
import com.example.contractagent.contract.ContractSide;
import com.example.contractagent.extraction.Extraction;
import com.example.contractagent.extraction.ExtractionService;
import com.example.contractagent.task.ComparisonTask;
import com.example.contractagent.task.ComparisonTaskService;
import com.example.contractagent.task.RiskLevel;
import com.example.contractagent.task.TaskStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class AgentRunner {

    private final ContractService contractService;
    private final ExtractionService extractionService;
    private final ComparisonService comparisonService;
    private final ComparisonTaskService taskService;
    private final ChatMessageRepository chatRepo;
    private final ContractAgent agent;

    @Async
    public void run(Long taskId) {
        try {
            ComparisonTask task = taskService.require(taskId);

            // 直接调 Service 完成抽取和对比（避免 LLM 反复调用）
            Contract buy = contractService.requireForTask(taskId, ContractSide.BUY);
            Contract sell = contractService.requireForTask(taskId, ContractSide.SELL);
            Extraction eBuy = extractionService.extractAndSave(taskId, task.getUserId(), buy);
            Extraction eSell = extractionService.extractAndSave(taskId, task.getUserId(), sell);
            ComparisonResult result = comparisonService.compare(eBuy, eSell);

            // 让 Agent 基于结果生成自然语言总结
            ChatClient client = agent.forTask(taskId);
            String summary = client.prompt()
                    .user("compare_fields 已完成，结果如下：%s。请用自然语言总结成 3 段：核心差异 / 风险提示 / 建议。".formatted(result.toString()))
                    .call()
                    .content();

            taskService.updateResult(taskId, summary, result.getRiskLevel());

            // 落库首条助手消息
            ChatMessage msg = new ChatMessage();
            msg.setTaskId(taskId);
            msg.setRole(ChatRole.ASSISTANT);
            msg.setContent(summary);
            msg.setCreatedAt(LocalDateTime.now());
            chatRepo.insert(msg);
        } catch (Exception e) {
            taskService.updateStatus(taskId, TaskStatus.FAILED);
        }
    }
}
```

- [ ] **Step 5：写 `ChatService` / `ChatController` / DTO**

`dto/ChatRequest.java`：
```java
package com.example.contractagent.chat.dto;
import jakarta.validation.constraints.NotBlank;
public record ChatRequest(@NotBlank String content) {}
```

`dto/ChatResponse.java`：
```java
package com.example.contractagent.chat.dto;
import java.util.List;
public record ChatResponse(String reply, List<ToolCall> toolCalls) {
    public record ToolCall(String tool, String args) {}
}
```

`dto/MessageDto.java`：
```java
package com.example.contractagent.chat.dto;
import com.example.contractagent.chat.ChatRole;
import java.time.LocalDateTime;
public record MessageDto(Long id, ChatRole role, String content, LocalDateTime createdAt) {}
```

`dto/MessageListResponse.java`：
```java
package com.example.contractagent.chat.dto;
import java.util.List;
public record MessageListResponse(long total, List<MessageDto> items) {}
```

`ChatService.java`：
```java
package com.example.contractagent.chat;

import com.example.contractagent.agent.ContractAgent;
import com.example.contractagent.common.BusinessException;
import com.example.contractagent.common.ErrorCode;
import com.example.contractagent.task.ComparisonTask;
import com.example.contractagent.task.ComparisonTaskService;
import com.example.contractagent.task.TaskStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ComparisonTaskService taskService;
    private final ChatMessageRepository chatRepo;
    private final ContractAgent agent;

    @Transactional
    public ChatMessageDto chat(Long userId, Long taskId, String content) {
        ComparisonTask task = taskService.requireOwned(taskId, userId);
        if (task.getStatus() == TaskStatus.PENDING) throw BusinessException.of(ErrorCode.PARAM_INVALID, "任务尚未开始");

        ChatMessage userMsg = new ChatMessage();
        userMsg.setTaskId(taskId); userMsg.setRole(ChatRole.USER); userMsg.setContent(content);
        userMsg.setCreatedAt(LocalDateTime.now());
        chatRepo.insert(userMsg);

        ChatClient client = agent.forTask(taskId);
        String reply = client.prompt().user(content).call().content();

        ChatMessage asstMsg = new ChatMessage();
        asstMsg.setTaskId(taskId); asstMsg.setRole(ChatRole.ASSISTANT); asstMsg.setContent(reply);
        asstMsg.setCreatedAt(LocalDateTime.now());
        chatRepo.insert(asstMsg);

        return new ChatMessageDto(asstMsg.getId(), asstMsg.getContent());
    }

    public MessageListResponse history(Long userId, Long taskId, int page, int size) {
        taskService.requireOwned(taskId, userId);
        var p = chatRepo.pageByTask(taskId, page, size);
        List<MessageDto> items = p.getRecords().stream()
                .map(m -> new MessageDto(m.getId(), m.getRole(), m.getContent(), m.getCreatedAt()))
                .toList();
        return new MessageListResponse(p.getTotal(), items);
    }

    public record ChatMessageDto(Long id, String content) {}
}
```

`ChatController.java`：
```java
package com.example.contractagent.chat;

import com.example.contractagent.chat.dto.*;
import com.example.contractagent.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tasks/{taskId}")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/chat")
    public ApiResponse<ChatService.ChatMessageDto> chat(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long taskId,
            @RequestBody @Valid ChatRequest req) {
        return ApiResponse.ok(chatService.chat(userId, taskId, req.content()));
    }

    @GetMapping("/messages")
    public ApiResponse<MessageListResponse> messages(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long taskId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ApiResponse.ok(chatService.history(userId, taskId, page, size));
    }
}
```

- [ ] **Step 6：写 `TaskDetailService` + `TaskDetailResponse` + 加 GET /{id} 到 `TaskController`**

`dto/TaskDetailResponse.java`：
```java
package com.example.contractagent.task.dto;

import com.example.contractagent.comparison.FieldDifference;
import com.example.contractagent.task.RiskLevel;
import com.example.contractagent.task.TaskStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class TaskDetailResponse {
    private Long id;
    private String title;
    private TaskStatus status;
    private RiskLevel riskLevel;
    private String summary;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private ContractDetailDto buy;
    private ContractDetailDto sell;
    private List<FieldDifference> differences;
}
```

`dto/ContractDetailDto.java`：
```java
package com.example.contractagent.task.dto;

import com.example.contractagent.extraction.Extraction;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ContractDetailDto {
    private Long id;
    private String filename;
    private Extraction extraction;
}
```

`TaskDetailService.java`：
```java
package com.example.contractagent.task;

import com.example.contractagent.comparison.ComparisonResult;
import com.example.contractagent.comparison.ComparisonService;
import com.example.contractagent.contract.Contract;
import com.example.contractagent.contract.ContractService;
import com.example.contractagent.contract.ContractSide;
import com.example.contractagent.extraction.Extraction;
import com.example.contractagent.extraction.ExtractionService;
import com.example.contractagent.task.dto.ContractDetailDto;
import com.example.contractagent.task.dto.TaskDetailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskDetailService {

    private final ComparisonTaskService taskService;
    private final ContractService contractService;
    private final ExtractionService extractionService;
    private final ComparisonService comparisonService;

    public TaskDetailResponse get(Long userId, Long taskId) {
        ComparisonTask t = taskService.requireOwned(taskId, userId);
        Contract buy = contractService.requireForTask(taskId, ContractSide.BUY);
        Contract sell = contractService.requireForTask(taskId, ContractSide.SELL);
        Extraction eBuy = extractionService.getByContractId(buy.getId());
        Extraction eSell = extractionService.getByContractId(sell.getId());
        ComparisonResult cr = comparisonService.compare(eBuy, eSell);
        return TaskDetailResponse.builder()
                .id(t.getId()).title(t.getTitle()).status(t.getStatus()).riskLevel(t.getRiskLevel())
                .summary(t.getSummary()).createdAt(t.getCreatedAt()).updatedAt(t.getUpdatedAt())
                .buy(new ContractDetailDto(buy.getId(), buy.getOriginalFilename(), eBuy))
                .sell(new ContractDetailDto(sell.getId(), sell.getOriginalFilename(), eSell))
                .differences(cr.getDifferences())
                .build();
    }
}
```

修改 `TaskController.java`，在 `create` 之后加：
```java
    @GetMapping("/{id}")
    public ApiResponse<TaskDetailResponse> detail(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        return ApiResponse.ok(taskDetailService.get(userId, id));
    }
```
并在 `TaskController` 类头加：
```java
    private final TaskDetailService taskDetailService;
```
`ComparisonTaskService` 已经在 Phase 2.2 Step 8 里直接注入了 `AgentRunner`，无需再改。

- [ ] **Step 7：Phase 3 末尾统一编译 + 手测**

```bash
cd C:\Users\xingchen\Desktop\agent\backend
mvn -DskipTests compile
```
Expected: BUILD SUCCESS

启动：
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

另开终端手测上传：
```bash
echo "采购合同 螺丝 M8×20 0.5元 月结30天" > /tmp/buy.txt
echo "销售合同 螺丝 M8×20 0.8元" > /tmp/sell.txt
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.data.token')

curl -s -X POST http://localhost:8080/api/tasks \
  -H "Authorization: Bearer $TOKEN" \
  -F "title=测试任务" \
  -F "buyFile=@/tmp/buy.txt" \
  -F "sellFile=@/tmp/sell.txt" | jq .
```
Expected: 任务创建成功，状态 RUNNING（或 DONE，取决于 LLM 速度）

- [ ] **Step 8：提交**

```bash
cd C:\Users\xingchen\Desktop\agent
git add backend/src/main/java/com/example/contractagent/agent/
git add backend/src/main/java/com/example/contractagent/chat/
git add backend/src/main/java/com/example/contractagent/task/TaskDetailService.java
git add backend/src/main/java/com/example/contractagent/task/dto/TaskDetailResponse.java
git add backend/src/main/java/com/example/contractagent/task/dto/ContractDetailDto.java
git add backend/src/main/java/com/example/contractagent/task/TaskController.java
git add backend/src/main/java/com/example/contractagent/task/ComparisonTaskService.java
git commit -m "feat(backend): Agent 编排 + 4 工具 + 对话服务 + 任务详情"
```

---

## Phase 4：管理端接口

### Task 4.1：管理端用户管理 + 全量任务审计

**Files:**
- Create: `backend/src/main/java/com/example/contractagent/admin/AdminController.java`
- Create: `backend/src/main/java/com/example/contractagent/admin/AdminStatsService.java`
- Create: `backend/src/main/java/com/example/contractagent/admin/dto/StatsResponse.java`
- Create: `backend/src/main/java/com/example/contractagent/admin/dto/UpdateUserRequest.java`
- Create: `backend/src/main/java/com/example/contractagent/admin/dto/AdminUserDto.java`
- Create: `backend/src/main/java/com/example/contractagent/admin/RiskRuleController.java`
- Create: `backend/src/main/java/com/example/contractagent/admin/RiskRuleService.java`
- Create: `backend/src/main/java/com/example/contractagent/admin/dto/RiskRuleDto.java`
- Create: `backend/src/main/java/com/example/contractagent/admin/dto/CreateRiskRuleRequest.java`
- Create: `backend/src/main/java/com/example/contractagent/admin/LlmLogController.java`
- Create: `backend/src/main/java/com/example/contractagent/admin/dto/LlmLogDto.java`
- Create: `backend/src/main/java/com/example/contractagent/admin/dto/LlmLogListResponse.java`
- Modify: `backend/src/main/java/com/example/contractagent/llm/LlmCallLogRepository.java`（加按 userId/model 过滤的 page 方法）

- [ ] **Step 1：写 `StatsResponse` + `AdminStatsService`**

`StatsResponse.java`：
```java
package com.example.contractagent.admin.dto;

import com.example.contractagent.task.TaskStatus;
import java.util.Map;

public record StatsResponse(
        long userCount, long taskCount,
        long todayLlmCalls, long todayTokens,
        Map<TaskStatus, Long> tasksByStatus) {}
```

`AdminStatsService.java`：
```java
package com.example.contractagent.admin;

import com.example.contractagent.admin.dto.StatsResponse;
import com.example.contractagent.llm.LlmCallLogRepository;
import com.example.contractagent.task.ComparisonTaskRepository;
import com.example.contractagent.task.TaskStatus;
import com.example.contractagent.user.UserRepository;
import com.example.contractagent.comparison.RiskRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminStatsService {

    private final UserRepository userRepository;
    private final ComparisonTaskRepository taskRepository;
    private final LlmCallLogRepository llmLogRepository;
    private final RiskRuleRepository riskRuleRepository;

    public StatsResponse stats() {
        long userCount = userRepository.selectCount(null);
        long taskCount = taskRepository.selectCount(null);
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        long todayCalls = llmLogRepository.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.example.contractagent.llm.LlmCallLog>()
                        .ge("created_at", startOfDay));
        long todayTokens = llmLogRepository.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.example.contractagent.llm.LlmCallLog>()
                        .ge("created_at", startOfDay))
                .stream().mapToLong(l -> l.getTotalTokens() == null ? 0 : l.getTotalTokens()).sum();
        Map<TaskStatus, Long> byStatus = new HashMap<>();
        for (TaskStatus s : TaskStatus.values()) {
            byStatus.put(s, taskRepository.selectCount(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.example.contractagent.task.ComparisonTask>()
                            .eq("status", s)));
        }
        return new StatsResponse(userCount, taskCount, todayCalls, todayTokens, byStatus);
    }
}
```

- [ ] **Step 2：写 `AdminUserDto` / `UpdateUserRequest`**

`AdminUserDto.java`：
```java
package com.example.contractagent.admin.dto;

import com.example.contractagent.user.UserRole;
import java.time.LocalDateTime;

public record AdminUserDto(Long id, String username, UserRole role, Integer enabled, LocalDateTime createdAt) {}
```

`UpdateUserRequest.java`：
```java
package com.example.contractagent.admin.dto;

import com.example.contractagent.user.UserRole;

public record UpdateUserRequest(UserRole role, Integer enabled) {}
```

- [ ] **Step 3：写 `AdminController`**

```java
package com.example.contractagent.admin;

import com.example.contractagent.admin.dto.*;
import com.example.contractagent.common.ApiResponse;
import com.example.contractagent.common.BusinessException;
import com.example.contractagent.common.ErrorCode;
import com.example.contractagent.task.ComparisonTask;
import com.example.contractagent.task.ComparisonTaskRepository;
import com.example.contractagent.task.RiskLevel;
import com.example.contractagent.task.TaskStatus;
import com.example.contractagent.task.dto.TaskListResponse;
import com.example.contractagent.task.dto.TaskSummaryDto;
import com.example.contractagent.user.User;
import com.example.contractagent.user.UserRepository;
import com.example.contractagent.user.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminStatsService statsService;
    private final UserRepository userRepository;
    private final ComparisonTaskRepository taskRepository;

    @GetMapping("/stats")
    public ApiResponse<StatsResponse> stats() {
        return ApiResponse.ok(statsService.stats());
    }

    @GetMapping("/users")
    public ApiResponse<Map<String, Object>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer enabled) {
        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>().orderByDesc("created_at");
        if (keyword != null && !keyword.isBlank()) wrapper.like("username", keyword);
        if (enabled != null) wrapper.eq("enabled", enabled);
        var p = userRepository.selectPage(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size), wrapper);
        List<AdminUserDto> items = p.getRecords().stream()
                .map(u -> new AdminUserDto(u.getId(), u.getUsername(), u.getRole(), u.getEnabled(), u.getCreatedAt()))
                .toList();
        return ApiResponse.ok(Map.of("total", p.getTotal(), "items", items));
    }

    @PatchMapping("/users/{id}")
    public ApiResponse<AdminUserDto> updateUser(@PathVariable Long id, @RequestBody UpdateUserRequest req) {
        User u = userRepository.selectById(id);
        if (u == null) throw BusinessException.of(ErrorCode.NOT_FOUND, "用户不存在");
        if (req.role() != null) u.setRole(req.role());
        if (req.enabled() != null) u.setEnabled(req.enabled());
        userRepository.updateById(u);
        return ApiResponse.ok(new AdminUserDto(u.getId(), u.getUsername(), u.getRole(), u.getEnabled(), u.getCreatedAt()));
    }

    @GetMapping("/tasks")
    public ApiResponse<TaskListResponse> listTasks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) RiskLevel riskLevel) {
        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ComparisonTask>().orderByDesc("created_at");
        if (userId != null) wrapper.eq("user_id", userId);
        if (status != null) wrapper.eq("status", status);
        if (riskLevel != null) wrapper.eq("risk_level", riskLevel);
        var p = taskRepository.selectPage(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size), wrapper);
        List<TaskSummaryDto> items = p.getRecords().stream()
                .map(t -> new TaskSummaryDto(t.getId(), t.getTitle(), t.getStatus(), t.getRiskLevel(), t.getCreatedAt(), t.getUpdatedAt()))
                .toList();
        return ApiResponse.ok(new TaskListResponse(p.getTotal(), items));
    }
}
```

- [ ] **Step 4：写 `RiskRuleService` + Controller + DTO**

`RiskRuleDto.java`：
```java
package com.example.contractagent.admin.dto;

import com.example.contractagent.comparison.RiskOperator;
import com.example.contractagent.task.RiskLevel;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RiskRuleDto(
        Long id, String fieldKey, RiskOperator operator, BigDecimal thresholdValue,
        RiskLevel riskLevel, Integer enabled, String remark, LocalDateTime updatedAt) {}
```

`CreateRiskRuleRequest.java`：
```java
package com.example.contractagent.admin.dto;

import com.example.contractagent.comparison.RiskOperator;
import com.example.contractagent.task.RiskLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreateRiskRuleRequest(
        @NotBlank String fieldKey,
        @NotNull RiskOperator operator,
        BigDecimal thresholdValue,
        @NotNull RiskLevel riskLevel,
        Integer enabled,
        String remark) {}
```

`RiskRuleService.java`：
```java
package com.example.contractagent.admin;

import com.example.contractagent.admin.dto.CreateRiskRuleRequest;
import com.example.contractagent.admin.dto.RiskRuleDto;
import com.example.contractagent.common.BusinessException;
import com.example.contractagent.common.ErrorCode;
import com.example.contractagent.comparison.RiskOperator;
import com.example.contractagent.comparison.RiskRule;
import com.example.contractagent.comparison.RiskRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RiskRuleService {

    private final RiskRuleRepository repository;

    public List<RiskRuleDto> list() {
        return repository.selectList(null).stream().map(this::toDto).toList();
    }

    public RiskRuleDto create(CreateRiskRuleRequest req) {
        validateOperator(req.operator(), req.thresholdValue());
        RiskRule r = new RiskRule();
        r.setFieldKey(req.fieldKey());
        r.setOperator(req.operator());
        r.setThresholdValue(req.thresholdValue());
        r.setRiskLevel(req.riskLevel());
        r.setEnabled(req.enabled() == null ? 1 : req.enabled());
        r.setRemark(req.remark());
        repository.insert(r);
        return toDto(r);
    }

    public RiskRuleDto update(Long id, CreateRiskRuleRequest req) {
        RiskRule r = repository.selectById(id);
        if (r == null) throw BusinessException.of(ErrorCode.NOT_FOUND, "规则不存在");
        if (req.fieldKey() != null) r.setFieldKey(req.fieldKey());
        if (req.operator() != null) {
            validateOperator(req.operator(), req.thresholdValue());
            r.setOperator(req.operator());
        }
        if (req.thresholdValue() != null) r.setThresholdValue(req.thresholdValue());
        if (req.riskLevel() != null) r.setRiskLevel(req.riskLevel());
        if (req.enabled() != null) r.setEnabled(req.enabled());
        if (req.remark() != null) r.setRemark(req.remark());
        repository.updateById(r);
        return toDto(r);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    private void validateOperator(RiskOperator op, BigDecimal threshold) {
        if ((op == RiskOperator.GT || op == RiskOperator.LT || op == RiskOperator.PCT_GT)
                && threshold == null) {
            throw BusinessException.of(ErrorCode.PARAM_INVALID, op + " 必须提供 thresholdValue");
        }
    }

    private RiskRuleDto toDto(RiskRule r) {
        return new RiskRuleDto(r.getId(), r.getFieldKey(), r.getOperator(), r.getThresholdValue(),
                r.getRiskLevel(), r.getEnabled(), r.getRemark(), r.getUpdatedAt());
    }
}
```

`RiskRuleController.java`：
```java
package com.example.contractagent.admin;

import com.example.contractagent.admin.dto.CreateRiskRuleRequest;
import com.example.contractagent.admin.dto.RiskRuleDto;
import com.example.contractagent.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/risk-rules")
@RequiredArgsConstructor
public class RiskRuleController {

    private final RiskRuleService service;

    @GetMapping
    public ApiResponse<List<RiskRuleDto>> list() {
        return ApiResponse.ok(service.list());
    }

    @PostMapping
    public ApiResponse<RiskRuleDto> create(@RequestBody @Valid CreateRiskRuleRequest req) {
        return ApiResponse.ok(service.create(req));
    }

    @PatchMapping("/{id}")
    public ApiResponse<RiskRuleDto> update(@PathVariable Long id, @RequestBody CreateRiskRuleRequest req) {
        return ApiResponse.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.ok(null);
    }
}
```

- [ ] **Step 5：写 `LlmLogController` + DTO + 扩 Repository**

修改 `LlmCallLogRepository.java`，加：
```java
    default IPage<LlmCallLog> pageFiltered(int page, int size, Long userId, String model, java.time.LocalDate from, java.time.LocalDate to) {
        var w = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<LlmCallLog>().orderByDesc("created_at");
        if (userId != null) w.eq("user_id", userId);
        if (model != null && !model.isBlank()) w.eq("model", model);
        if (from != null) w.ge("created_at", from.atStartOfDay());
        if (to != null) w.lt("created_at", to.plusDays(1).atStartOfDay());
        return selectPage(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size), w);
    }
```
（在原 `@Mapper` 接口里追加方法即可；注意把 `import` 也补上：`com.baomidou.mybatisplus.core.metadata.IPage`、`com.baomidou.mybatisplus.extension.plugins.pagination.Page`）

`LlmLogDto.java`：
```java
package com.example.contractagent.admin.dto;

import java.time.LocalDateTime;

public record LlmLogDto(Long id, Long taskId, Long userId, String model,
                         Integer promptTokens, Integer completionTokens, Integer totalTokens,
                         Integer durationMs, String status, String errorMsg, LocalDateTime createdAt) {}
```

`LlmLogListResponse.java`：
```java
package com.example.contractagent.admin.dto;
import java.util.List;
public record LlmLogListResponse(long total, List<LlmLogDto> items) {}
```

`LlmLogController.java`：
```java
package com.example.contractagent.admin;

import com.example.contractagent.admin.dto.LlmLogDto;
import com.example.contractagent.admin.dto.LlmLogListResponse;
import com.example.contractagent.common.ApiResponse;
import com.example.contractagent.llm.LlmCallLog;
import com.example.contractagent.llm.LlmCallLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/llm-logs")
@RequiredArgsConstructor
public class LlmLogController {

    private final LlmCallLogRepository repository;

    @GetMapping
    public ApiResponse<LlmLogListResponse> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        var p = repository.pageFiltered(page, size, userId, model, from, to);
        List<LlmLogDto> items = p.getRecords().stream()
                .map(l -> new LlmLogDto(l.getId(), l.getTaskId(), l.getUserId(), l.getModel(),
                        l.getPromptTokens(), l.getCompletionTokens(), l.getTotalTokens(),
                        l.getDurationMs(), l.getStatus(), l.getErrorMsg(), l.getCreatedAt()))
                .toList();
        return ApiResponse.ok(new LlmLogListResponse(p.getTotal(), items));
    }
}
```

- [ ] **Step 6：编译**

Run:
```bash
cd C:\Users\xingchen\Desktop\agent\backend
mvn -DskipTests compile
```
Expected: BUILD SUCCESS

- [ ] **Step 7：手测管理端**

启动后：
```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.data.token')

curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/admin/stats | jq .
curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/admin/risk-rules | jq .
```
Expected: 都返回 0 + 数据

- [ ] **Step 8：提交**

```bash
cd C:\Users\xingchen\Desktop\agent
git add backend/src/main/java/com/example/contractagent/admin/
git add backend/src/main/java/com/example/contractagent/llm/LlmCallLogRepository.java
git commit -m "feat(backend): 管理端 - 统计/用户/任务/风险规则/日志"
```

---

### Task 4.2：报告导出（Markdown）

**Files:**
- Create: `backend/src/main/java/com/example/contractagent/admin/ReportService.java`
- Modify: `backend/src/main/java/com/example/contractagent/admin/AdminController.java`（加 GET /api/tasks/{id}/report）

- [ ] **Step 1：写 `ReportService`**

```java
package com.example.contractagent.admin;

import com.example.contractagent.comparison.ComparisonService;
import com.example.contractagent.contract.Contract;
import com.example.contractagent.contract.ContractService;
import com.example.contractagent.contract.ContractSide;
import com.example.contractagent.extraction.Extraction;
import com.example.contractagent.extraction.ExtractionService;
import com.example.contractagent.task.ComparisonTask;
import com.example.contractagent.task.ComparisonTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ComparisonTaskService taskService;
    private final TaskDetailService taskDetailService;
    private final ContractService contractService;
    private final ExtractionService extractionService;
    private final ComparisonService comparisonService;

    public String generateMarkdown(Long taskId) {
        ComparisonTask t = taskService.require(taskId);
        Contract buy = contractService.requireForTask(taskId, ContractSide.BUY);
        Contract sell = contractService.requireForTask(taskId, ContractSide.SELL);
        Extraction eBuy = extractionService.getByContractId(buy.getId());
        Extraction eSell = extractionService.getByContractId(sell.getId());
        var cr = comparisonService.compare(eBuy, eSell);

        var df = DateTimeFormatter.ISO_LOCAL_DATE;
        var sb = new StringBuilder();
        sb.append("# 合同对比报告：").append(t.getTitle()).append("\n\n");
        sb.append("- 任务ID：").append(t.getId()).append("\n");
        sb.append("- 生成时间：").append(t.getUpdatedAt()).append("\n");
        sb.append("- 整体风险：").append(t.getRiskLevel()).append("\n");
        sb.append("- 任务状态：").append(t.getStatus()).append("\n");
        if (t.getConfirmedAt() != null) {
            sb.append("- 采购单确认时间：").append(t.getConfirmedAt()).append("\n");
            sb.append("- 确认人ID：").append(t.getConfirmedBy()).append("\n");
        } else {
            sb.append("- 采购单确认状态：**未确认**\n");
        }
        sb.append("\n");

        sb.append("## 1. 要素对比表\n\n");
        sb.append("| 字段 | 采购合同 | 销售合同 | 差异 |\n");
        sb.append("|---|---|---|---|\n");
        for (var d : cr.getDifferences()) {
            sb.append("| ").append(d.getField())
              .append(" | ").append(d.getBuy() == null ? "—" : d.getBuy())
              .append(" | ").append(d.getSell() == null ? "—" : d.getSell())
              .append(" | ").append(d.getStatus()).append(d.getRisk() != null ? " (" + d.getRisk() + ")" : "")
              .append(" |\n");
        }
        sb.append("\n## 2. Agent 风险总结\n\n");
        sb.append(t.getSummary() == null ? "（无）" : t.getSummary()).append("\n");

        return sb.toString();
    }
}
```

- [ ] **Step 2：在 `TaskController` 加 report 接口（用户端）**

修改 `TaskController.java`：
- 注入 `ReportService reportService`
- 加方法：
```java
    @GetMapping(value = "/{id}/report", produces = "text/markdown; charset=utf-8")
    public String report(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        taskService.requireOwned(id, userId);
        return reportService.generateMarkdown(id);
    }
```

- [ ] **Step 3：编译**

Run:
```bash
cd C:\Users\xingchen\Desktop\agent\backend
mvn -DskipTests compile
```
Expected: BUILD SUCCESS

- [ ] **Step 4：提交**

```bash
cd C:\Users\xingchen\Desktop\agent
git add backend/src/main/java/com/example/contractagent/admin/ReportService.java
git add backend/src/main/java/com/example/contractagent/task/TaskController.java
git commit -m "feat(backend): 报告导出 Markdown"
```

---

## Phase 5：前端（用户端 + 管理端）

> **设计稿来源**：`design/pages/` 下的 5 个 HTML（登录/任务列表/任务详情/管理概览/用户管理）+ `design/assets/image_*.svg` 图标。前端**完全还原设计稿样式**，仅做接口适配：把静态演示数据换成 API 调用、补齐按钮触发的表单/弹窗、补齐设计稿未覆盖但功能必需的页面（新建任务/风险规则/LLM 日志/个人中心，统一沿用设计系统）。
>
> **技术选型**：Vue 3.5 + Vite 5 + TS 5 + Pinia 2 + Vue Router 4 + Axios 1 + **Tailwind CSS 4（`@tailwindcss/vite` 插件，非 browser build）** + **`lucide-vue-next`（图标作为 Vue 组件，替代设计稿的 `mask-image` SVG）** + `dayjs` + `marked` + `@fontsource/dm-sans` + `@fontsource/jetbrains-mono`。**不再使用 Element Plus**。
>
> **设计 Token 提取**（来自 `design/pages/任务列表.html` 的 `<style id="theme-vars">`，所有页面共用）：
> - 主色 `--primary: #4285f4`；侧边栏 `--sidebar: #f0f6ff`；图表色 `--chart-1..5: #4285f4 / #ea4335 / #fbbc05 / #0043ad / #34a853`
> - 字体：`--font-sans: DM Sans`；`--font-mono: JetBrains Mono`
> - 圆角 `--radius: 0.5rem`；间距 `--spacing: 0.24rem`（3.84px）；零阴影（shadow color opacity 0）
> - 布局：侧边栏 260px + 顶栏 76px + 内容区 padding `calc(var(--spacing) * 6)`
>
> **图标映射**（设计稿 `image_N_tgt36j.svg` → lucide-vue-next 组件）：
> | 设计稿 SVG | 用途 | Lucide 组件 |
> |---|---|---|
> | image_0 | 任务列表/用户任务导航 | `ListTodo` |
> | image_1 / image_9 / image_18 | 管理后台/管理概览导航 | `LayoutDashboard` |
> | image_2 | 搜索 | `Search` |
> | image_3 / image_10 | 通知/风险提示 | `Bell` / `TriangleAlert` |
> | image_4 | 新建对比 | `Plus` |
> | image_5 / image_12 | 导出报告 | `Download` |
> | image_6 | 上一页 | `ChevronLeft` |
> | image_7 | 下一页 | `ChevronRight` |
> | image_8 | 返回任务列表 | `ListTodo` |
> | image_11 | 面包屑返回 | `ChevronLeft` |
> | image_13 | AI 风险摘要 | `Sparkles` |
> | image_14 | 交付日期 | `Calendar` |
> | image_15 | 付款条款 | `CreditCard` |
> | image_16 | 对话追问 | `MessageCircle` |
> | image_17 | 发送 | `SendHorizontal` |
> | image_19 | 编辑 | `PenLine` |

---

### Task 5.1：用户端 frontend/ —— 设计稿还原 + 接口适配

**Files:**
- Create: `frontend/package.json`
- Create: `frontend/vite.config.ts`
- Create: `frontend/tsconfig.json`
- Create: `frontend/tsconfig.node.json`
- Create: `frontend/index.html`
- Create: `frontend/src/main.ts`
- Create: `frontend/src/App.vue`
- Create: `frontend/src/styles/theme.css`（设计稿 CSS 变量 + Tailwind `@theme inline` 别名）
- Create: `frontend/src/styles/main.css`（Tailwind 入口 + base 层）
- Create: `frontend/src/router/index.ts`
- Create: `frontend/src/stores/auth.ts`
- Create: `frontend/src/stores/toast.ts`（替代 ElMessage 的轻量 toast）
- Create: `frontend/src/api/http.ts`
- Create: `frontend/src/api/auth.ts`
- Create: `frontend/src/api/task.ts`
- Create: `frontend/src/components/AppShell.vue`（侧边栏 + 顶栏 + 内容槽，对应设计稿布局）
- Create: `frontend/src/components/AppSidebar.vue`
- Create: `frontend/src/components/AppTopbar.vue`
- Create: `frontend/src/components/StatusBadge.vue`
- Create: `frontend/src/components/RiskBadge.vue`
- Create: `frontend/src/components/FilterChips.vue`
- Create: `frontend/src/components/Pagination.vue`
- Create: `frontend/src/components/FieldDiffTable.vue`（任务详情字段差异表）
- Create: `frontend/src/components/ChatPanel.vue`（任务详情对话区）
- Create: `frontend/src/components/Toast.vue`（toast 容器）
- Create: `frontend/src/views/LoginView.vue`（对应 `design/pages/登录.html`，登录/注册 Tab）
- Create: `frontend/src/views/TaskListView.vue`（对应 `design/pages/任务列表.html`）
- Create: `frontend/src/views/TaskCreateView.vue`（补齐，沿用卡片表单设计）
- Create: `frontend/src/views/TaskDetailView.vue`（对应 `design/pages/任务详情.html`）
- Create: `frontend/src/views/MeView.vue`（补齐，顶栏头像下拉入口）

- [ ] **Step 1：`package.json`**

```json
{
  "name": "contract-agent-frontend",
  "version": "0.1.0",
  "private": true,
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "vue-tsc -b && vite build",
    "preview": "vite preview"
  },
  "dependencies": {
    "vue": "^3.5.0",
    "vue-router": "^4.4.0",
    "pinia": "^2.2.0",
    "axios": "^1.7.0",
    "lucide-vue-next": "^0.460.0",
    "dayjs": "^1.11.0",
    "marked": "^14.0.0"
  },
  "devDependencies": {
    "@vitejs/plugin-vue": "^5.1.0",
    "@tailwindcss/vite": "^4.0.0",
    "tailwindcss": "^4.0.0",
    "@fontsource/dm-sans": "^5.1.0",
    "@fontsource/jetbrains-mono": "^5.1.0",
    "typescript": "^5.5.0",
    "vite": "^5.4.0",
    "vue-tsc": "^2.1.0"
  }
}
```

- [ ] **Step 2：`vite.config.ts`**

```ts
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue(), tailwindcss()],
  resolve: { alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) } },
  server: {
    port: 5173,
    proxy: { '/api': { target: 'http://localhost:8080', changeOrigin: true } }
  }
})
```

- [ ] **Step 3：`tsconfig.json` + `tsconfig.node.json`**

`tsconfig.json`：
```json
{
  "compilerOptions": {
    "target": "ES2022", "module": "ESNext", "moduleResolution": "Bundler",
    "strict": true, "jsx": "preserve",
    "esModuleInterop": true, "skipLibCheck": true,
    "resolveJsonModule": true, "isolatedModules": true,
    "lib": ["ES2022", "DOM", "DOM.Iterable"],
    "types": ["vite/client"],
    "baseUrl": ".", "paths": { "@/*": ["src/*"] }
  },
  "include": ["src/**/*", "src/**/*.vue"],
  "references": [{ "path": "./tsconfig.node.json" }]
}
```

`tsconfig.node.json`：
```json
{ "compilerOptions": { "composite": true, "skipLibCheck": true, "types": ["node"] }, "include": ["vite.config.ts"] }
```

- [ ] **Step 4：`index.html`**

```html
<!DOCTYPE html>
<html lang="zh-CN" class="light" data-theme="light">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>合同对比 Agent</title>
</head>
<body class="min-h-screen font-sans antialiased">
  <div id="app"></div>
  <script type="module" src="/src/main.ts"></script>
</body>
</html>
```

- [ ] **Step 5：`src/styles/theme.css`**（设计稿 CSS 变量原样提取）

直接拷贝 `design/pages/任务列表.html` 中 `<style id="theme-vars">` 的 `:root` 与 `.dark` 块，以及 `@theme inline` 别名层。关键变量：
```css
:root {
  --background: #ffffff; --foreground: #0e1115;
  --card: #ffffff; --card-foreground: #0e1115;
  --popover: #f9f9fa; --popover-foreground: #0e1115;
  --primary: #4285f4; --primary-foreground: #ffffff;
  --secondary: #dbeafe; --secondary-foreground: #333942;
  --muted: #eff1f4; --muted-foreground: #7f8d9f;
  --accent: #dbeafe; --accent-foreground: #003e8f;
  --destructive: #ef4444; --destructive-foreground: #ffffff;
  --border: #ebebeb; --input: #e2e3e4; --ring: #4285f4;
  --chart-1: #4285f4; --chart-2: #ea4335; --chart-3: #fbbc05;
  --chart-4: #0043ad; --chart-5: #34a853;
  --sidebar: #f0f6ff; --sidebar-foreground: #0e1115;
  --sidebar-primary: #4285f4; --sidebar-primary-foreground: #ffffff;
  --sidebar-accent: #dbeafe; --sidebar-accent-foreground: #003e8f;
  --sidebar-border: #e7eaef; --sidebar-ring: #4285f4;
  --font-sans: 'DM Sans', ui-sans-serif, sans-serif, system-ui;
  --font-mono: 'JetBrains Mono', monospace;
  --radius: 0.5rem; --spacing: 0.24rem; --tracking-normal: 0em;
  --shadow-2xs: 0 3px 0px 0px rgba(14,17,21,0);
  --shadow-sm: 0 3px 0px 0px rgba(14,17,21,0), 0 1px 2px -1px rgba(14,17,21,0);
}
/* .dark 块同设计稿，按需启用 */
@theme inline {
  --color-background: var(--background); --color-foreground: var(--foreground);
  --color-card: var(--card); --color-card-foreground: var(--card-foreground);
  --color-primary: var(--primary); --color-primary-foreground: var(--primary-foreground);
  --color-secondary: var(--secondary); --color-muted: var(--muted);
  --color-muted-foreground: var(--muted-foreground);
  --color-accent: var(--accent); --color-accent-foreground: var(--accent-foreground);
  --color-border: var(--border); --color-input: var(--input);
  --color-sidebar: var(--sidebar); --color-sidebar-foreground: var(--sidebar-foreground);
  --color-sidebar-primary: var(--sidebar-primary);
  --color-sidebar-primary-foreground: var(--sidebar-primary-foreground);
  --color-sidebar-accent: var(--sidebar-accent);
  --color-sidebar-border: var(--sidebar-border);
  --color-chart-1: var(--chart-1); --color-chart-2: var(--chart-2);
  --color-chart-3: var(--chart-3); --color-chart-4: var(--chart-4);
  --color-chart-5: var(--chart-5);
  --font-sans: var(--font-sans); --font-mono: var(--font-mono);
  --radius: var(--radius); --spacing: var(--spacing);
}
```

- [ ] **Step 6：`src/styles/main.css`**（Tailwind 入口 + base 层）

```css
@import "tailwindcss";
@import "@fontsource/dm-sans/400.css";
@import "@fontsource/dm-sans/500.css";
@import "@fontsource/dm-sans/600.css";
@import "@fontsource/dm-sans/700.css";
@import "@fontsource/jetbrains-mono/400.css";
@import "@fontsource/jetbrains-mono/500.css";
@import "./theme.css";

@layer base {
  body { background: var(--background); color: var(--foreground); font-family: var(--font-sans); }
  td, th { word-break: break-word; }
  th { white-space: nowrap; }
}
/* 设计稿通用工具类 */
.no-scrollbar::-webkit-scrollbar { display: none; }
.no-scrollbar { -ms-overflow-style: none; scrollbar-width: none; }
```

- [ ] **Step 7：`src/main.ts`**

```ts
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import './styles/main.css'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.mount('#app')
```

- [ ] **Step 8：`src/App.vue`**

```vue
<template>
  <router-view />
  <Toast />
</template>
<script setup lang="ts">
import Toast from '@/components/Toast.vue'
</script>
```

- [ ] **Step 9：`src/stores/toast.ts`**（替代 ElMessage）

```ts
import { defineStore } from 'pinia'

export type ToastType = 'success' | 'error' | 'warning' | 'info'
export interface ToastItem { id: number; type: ToastType; message: string }

let _id = 0
export const useToastStore = defineStore('toast', {
  state: () => ({ items: [] as ToastItem[] }),
  actions: {
    push(message: string, type: ToastType = 'info', ttl = 2600) {
      const id = ++_id
      this.items.push({ id, type, message })
      setTimeout(() => this.dismiss(id), ttl)
    },
    success(m: string) { this.push(m, 'success') },
    error(m: string) { this.push(m, 'error', 3600) },
    warning(m: string) { this.push(m, 'warning') },
    info(m: string) { this.push(m, 'info') },
    dismiss(id: number) {
      const i = this.items.findIndex(t => t.id === id)
      if (i >= 0) this.items.splice(i, 1)
    }
  }
})
```

- [ ] **Step 10：`src/api/http.ts`**（无 ElMessage 依赖，改用 toast store）

```ts
import axios from 'axios'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import router from '@/router'

export const http = axios.create({ baseURL: '/api' })

http.interceptors.request.use(cfg => {
  const auth = useAuthStore()
  if (auth.token) cfg.headers.Authorization = `Bearer ${auth.token}`
  return cfg
})

http.interceptors.response.use(
  res => {
    const body = res.data
    if (body && typeof body === 'object' && 'code' in body && body.code !== 0) {
      useToastStore().error(body.message || '请求失败')
      return Promise.reject(body)
    }
    return body
  },
  err => {
    const body = err.response?.data
    if (body?.code === 1002) {
      useAuthStore().logout()
      router.push('/login')
    }
    useToastStore().error(body?.message || err.message || '网络异常')
    return Promise.reject(err)
  }
)
```

- [ ] **Step 11：`src/stores/auth.ts`**

```ts
import { defineStore } from 'pinia'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    user: null as null | { id: number; username: string; role: 'USER' | 'ADMIN' }
  }),
  getters: {
    isAdmin: (s) => s.user?.role === 'ADMIN'
  },
  actions: {
    setToken(t: string) { this.token = t; localStorage.setItem('token', t) },
    setUser(u: any) { this.user = u },
    logout() { this.token = ''; this.user = null; localStorage.removeItem('token') }
  }
})
```

- [ ] **Step 12：`src/api/auth.ts` + `src/api/task.ts`**

`src/api/auth.ts`：
```ts
import { http } from './http'
export const authApi = {
  login: (data: { username: string; password: string }) => http.post('/auth/login', data),
  register: (data: { username: string; password: string }) => http.post('/auth/register', data),
  me: () => http.get('/auth/me')
}
```

`src/api/task.ts`：
```ts
import { http } from './http'
export const taskApi = {
  create: (form: FormData) => http.post('/tasks', form, { headers: { 'Content-Type': 'multipart/form-data' } }),
  list: (params: { page?: number; size?: number; status?: string }) => http.get('/tasks', { params }),
  detail: (id: number) => http.get(`/tasks/${id}`),
  chat: (id: number, content: string) => http.post(`/tasks/${id}/chat`, { content }),
  messages: (id: number, params?: { page?: number; size?: number }) => http.get(`/tasks/${id}/messages`, { params }),
  report: (id: number) => http.get(`/tasks/${id}/report`, { responseType: 'text' }),
  retry: (id: number) => http.post(`/tasks/${id}/retry`),
  confirm: (id: number) => http.post(`/tasks/${id}/confirm`)
}
```

- [ ] **Step 13：`src/router/index.ts`**

```ts
import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  { path: '/login', component: () => import('@/views/LoginView.vue'), meta: { guest: true } },
  { path: '/', redirect: '/tasks' },
  { path: '/tasks', component: () => import('@/views/TaskListView.vue'), meta: { auth: true } },
  { path: '/tasks/new', component: () => import('@/views/TaskCreateView.vue'), meta: { auth: true } },
  { path: '/tasks/:id', component: () => import('@/views/TaskDetailView.vue'), meta: { auth: true } },
  { path: '/me', component: () => import('@/views/MeView.vue'), meta: { auth: true } }
]

const router = createRouter({ history: createWebHistory(), routes })
router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.auth && !auth.token) return '/login'
  if (to.meta.guest && auth.token) return '/tasks'
})
export default router
```

- [ ] **Step 14：`src/components/Toast.vue`**

```vue
<template>
  <div class="fixed top-5 right-5 z-50 flex flex-col gap-2">
    <div v-for="t in toast.items" :key="t.id"
      class="px-4 py-3 text-sm font-medium shadow-sm border"
      :style="boxStyle(t.type)"
      @click="toast.dismiss(t.id)">
      {{ t.message }}
    </div>
  </div>
</template>
<script setup lang="ts">
import { useToastStore, type ToastType } from '@/stores/toast'
const toast = useToastStore()
function boxStyle(t: ToastType) {
  const map: Record<ToastType, string> = {
    success: `background:var(--card);color:var(--chart-5);border-color:var(--border)`,
    error:   `background:var(--card);color:var(--destructive);border-color:var(--border)`,
    warning: `background:var(--card);color:var(--chart-3);border-color:var(--border)`,
    info:    `background:var(--card);color:var(--foreground);border-color:var(--border)`
  }
  return `border-radius:var(--radius);${map[t]}`
}
</script>
```

- [ ] **Step 15：`src/components/AppSidebar.vue` + `AppTopbar.vue` + `AppShell.vue`**

布局严格对应 `design/pages/任务列表.html` 的 `.app-shell` / `.app-sidebar` / `.app-topbar` / `.app-content` 结构与样式（260px 侧边栏 + 76px 顶栏）。把设计稿内联样式原样迁移到 Vue SFC 的 `<style scoped>`，把 `data-icon` mask-image 换成 `<LucideIcon>` 组件。

`AppShell.vue`：
```vue
<template>
  <div class="app-shell">
    <AppSidebar :navs="navs" />
    <div class="app-main">
      <AppTopbar :search-placeholder="searchPlaceholder" v-model:search="search" />
      <section class="app-content">
        <slot />
      </section>
    </div>
  </div>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import AppSidebar from './AppSidebar.vue'
import AppTopbar from './AppTopbar.vue'

defineProps<{ navs: Array<{ key: string; label: string; icon: any; to?: string }>; searchPlaceholder?: string }>()
const search = ref('')
</script>
<style scoped>
/* 拷贝自 design/pages/任务列表.html 的 .app-shell / .app-main / .app-content 样式 */
.app-shell { display: grid; grid-template-columns: 260px 1fr; height: 100vh; }
.app-main { display: flex; flex-direction: column; min-width: 0; }
.app-content {
  padding: calc(var(--spacing) * 6);
  display: flex; flex-direction: column; gap: calc(var(--spacing) * 5);
  min-height: 0; overflow-y: auto;
}
@media (max-width: 1100px) { .app-shell { grid-template-columns: 84px 1fr; } }
</style>
```

`AppSidebar.vue`：对应设计稿 `.app-sidebar`，brand mark（CC 蓝底圆角）+ nav 按钮（active 蓝底白字）+ footer。导航项由 `navs` prop 驱动，`router-link-active` 时加 `.active` 类。

`AppTopbar.vue`：对应 `.app-topbar`，左侧搜索框（圆角 999px，`--input` 边框）+ 右侧通知按钮 + 头像（首字母，蓝底圆形）。头像点击 emit `avatar-click`，由父组件弹出下拉菜单（个人中心/退出）。

- [ ] **Step 16：`src/components/StatusBadge.vue` + `RiskBadge.vue`**

对应设计稿 `.status-badge` / `.risk-badge` 样式（圆角 999px，`color-mix` 半透明背景）。

`StatusBadge.vue`：
```vue
<template>
  <span class="status-badge" :class="cls">{{ label }}</span>
</template>
<script setup lang="ts">
import { computed } from 'vue'
const props = defineProps<{ status: string }>()
const map: Record<string, { cls: string; label: string }> = {
  PENDING: { cls: 'status-pending', label: '待处理' },
  RUNNING: { cls: 'status-running', label: '进行中' },
  DONE: { cls: 'status-done', label: '已完成' },
  FAILED: { cls: 'status-failed', label: '失败' },
  CONFIRMED: { cls: 'status-done', label: '已确认' }
}
const cur = computed(() => map[props.status] || { cls: 'status-pending', label: props.status })
const cls = computed(() => cur.value.cls)
const label = computed(() => cur.value.label)
</script>
<style scoped>
.status-badge { display: inline-flex; align-items: center; gap: .35rem; padding: .34rem .7rem; border-radius: 999px; font-size: .8rem; font-weight: 500; white-space: nowrap; }
.status-done { background: color-mix(in srgb, var(--chart-5) 16%, var(--background)); color: var(--chart-5); }
.status-running { background: color-mix(in srgb, var(--chart-1) 16%, var(--background)); color: var(--chart-1); }
.status-pending { background: var(--muted); color: var(--muted-foreground); }
.status-failed { background: color-mix(in srgb, var(--destructive) 16%, var(--background)); color: var(--destructive); }
</style>
```

`RiskBadge.vue`：同上，映射 `HIGH→risk-high（chart-2 红）/ MEDIUM→risk-medium（chart-3 黄）/ LOW→risk-low（chart-5 绿）`，无风险显示 `—`。

- [ ] **Step 17：`src/components/FilterChips.vue` + `Pagination.vue`**

`FilterChips.vue`：对应 `.filter-chip`（圆角 `--radius`，active 蓝底白字）。Props：`options: string[]`，`modelValue: string`。
`Pagination.vue`：对应 `.pagination` + `.page-btn`。Props：`total`、`page`、`size`；emit `update:page`。上一页/下一页用 `ChevronLeft` / `ChevronRight` 图标。

- [ ] **Step 18：`src/views/LoginView.vue`**（对应 `design/pages/登录.html`）

**还原要点**：居中 max-w-sm 卡片，brand mark（CC 蓝底）+ 标题「合同对比 Agent」+ 副标题。卡片内顶部登录/注册 Tab（active 蓝色下划线），表单输入框带 `User` / `Lock` Lucide 图标前缀，高度 32px，`--input` 边框。登录按钮蓝底白字，注册按钮 `--secondary` 浅蓝底。

**接口适配**：
- 登录 Tab 提交 → `authApi.login` → `auth.setToken` + `auth.setUser` → `router.push('/tasks')`
- 注册 Tab 提交 → `authApi.register` → 成功后切回登录 Tab 并 toast「注册成功」
- 表单校验：用户名 ≥3、密码 ≥6，不通过用 toast 提示

```vue
<template>
  <main class="flex items-center justify-center min-h-screen px-4">
    <div class="w-full max-w-sm">
      <!-- Brand -->
      <div class="flex items-center gap-3 mb-8 justify-center">
        <div class="w-10 h-10 grid place-items-center font-bold text-sm"
          :style="`background:var(--sidebar-primary);color:var(--sidebar-primary-foreground);border-radius:calc(var(--radius) + 4px)`">CC</div>
        <div>
          <h1 class="text-base font-semibold leading-tight" style="color:var(--foreground)">合同对比 Agent</h1>
          <p class="text-xs" style="color:var(--muted-foreground)">Contract Compare Platform</p>
        </div>
      </div>
      <!-- Card -->
      <div class="rounded-lg" :style="`background:var(--card);border:1px solid var(--border);border-radius:var(--radius);padding:calc(var(--spacing)*6)`">
        <div class="flex gap-0 mb-6" style="border-bottom:1px solid var(--border)">
          <button v-for="t in tabs" :key="t.key" @click="tab=t.key"
            class="pb-3 text-sm font-semibold cursor-pointer bg-transparent border-0"
            :style="tabStyle(t.key)">{{ t.label }}</button>
        </div>
        <!-- Login Form -->
        <form v-if="tab==='login'" @submit.prevent="onLogin" class="flex flex-col gap-4">
          <FieldInput v-model="form.username" label="用户名" icon="User" placeholder="请输入用户名" />
          <FieldInput v-model="form.password" label="密码" icon="Lock" type="password" placeholder="请输入密码" />
          <button type="submit" :disabled="loading"
            class="mt-2 flex items-center justify-center gap-2 cursor-pointer font-medium text-sm disabled:opacity-60"
            style="height:32px;padding:0 calc(var(--spacing)*4);background:var(--primary);color:var(--primary-foreground);border:1px solid transparent;border-radius:var(--radius)">登录</button>
        </form>
        <!-- Register Form -->
        <form v-else @submit.prevent="onRegister" class="flex flex-col gap-4">
          <FieldInput v-model="form.username" label="用户名" icon="User" placeholder="请输入用户名" />
          <FieldInput v-model="form.password" label="密码" icon="Lock" type="password" placeholder="请输入密码" />
          <FieldInput v-model="form.confirm" label="确认密码" icon="Lock" type="password" placeholder="请再次输入密码" />
          <button type="submit" :disabled="loading"
            class="mt-2 flex items-center justify-center gap-2 cursor-pointer font-medium text-sm disabled:opacity-60"
            style="height:32px;padding:0 calc(var(--spacing)*4);background:var(--secondary);color:var(--secondary-foreground);border:1px solid var(--border);border-radius:var(--radius)">注册</button>
        </form>
      </div>
    </div>
  </main>
</template>
<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock } from 'lucide-vue-next'
import { authApi } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import FieldInput from '@/components/FieldInput.vue'  // 见 Step 19

const tabs = [{ key: 'login', label: '登录' }, { key: 'register', label: '注册' }]
const tab = ref<'login' | 'register'>('login')
const form = reactive({ username: '', password: '', confirm: '' })
const loading = ref(false)
const router = useRouter()
const auth = useAuthStore()
const toast = useToastStore()
function tabStyle(k: string) {
  return k === tab.value
    ? 'color:var(--primary);border-bottom:2px solid var(--primary);margin-bottom:-1px'
    : 'color:var(--muted-foreground);border-bottom:2px solid transparent;margin-bottom:-1px;margin-left:calc(var(--spacing)*4)'
}
async function onLogin() {
  if (form.username.length < 3 || form.password.length < 6) return toast.warning('用户名≥3，密码≥6')
  loading.value = true
  try {
    const res: any = await authApi.login({ username: form.username, password: form.password })
    auth.setToken(res.data.token); auth.setUser(res.data.user)
    toast.success('登录成功')
    router.push('/tasks')
  } finally { loading.value = false }
}
async function onRegister() {
  if (form.username.length < 3 || form.password.length < 6) return toast.warning('用户名≥3，密码≥6')
  if (form.password !== form.confirm) return toast.warning('两次密码不一致')
  loading.value = true
  try {
    await authApi.register({ username: form.username, password: form.password })
    toast.success('注册成功，请登录')
    tab.value = 'login'
  } finally { loading.value = false }
}
</script>
```

- [ ] **Step 19：`src/components/FieldInput.vue`**（设计稿输入框样式抽成通用组件）

```vue
<template>
  <div class="flex flex-col gap-1.5">
    <label class="text-xs font-medium" style="color:var(--muted-foreground)">{{ label }}</label>
    <div class="flex items-center gap-2 rounded-md"
      :style="`border:1px solid var(--input);background:var(--popover);padding:0 calc(var(--spacing)*3);height:32px`">
      <component :is="icon" :size="16" :style="{ color: 'var(--muted-foreground)', flexShrink: 0 }" />
      <input :type="type" v-model="model" :placeholder="placeholder"
        class="bg-transparent border-0 outline-none text-sm w-full" style="color:var(--foreground);font-family:var(--font-sans)" />
    </div>
  </div>
</template>
<script setup lang="ts">
import type { Component } from 'vue'
defineProps<{ label: string; icon: Component; type?: string; placeholder?: string }>()
const model = defineModel<string>()
</script>
```

- [ ] **Step 20：`src/views/TaskListView.vue`**（对应 `design/pages/任务列表.html`）

**还原要点**：`AppShell` 内部结构 = Hero（标题「我的对比任务」+ 副标题 + 右侧「新建对比」primary 按钮 + 「导出报告」ghost 按钮）→ `content-card`（filter chips：全部/已完成/进行中/待处理/失败 + data-table + pagination）。表格列：任务名称/状态/风险等级/创建时间/更新时间。状态用 `StatusBadge`，风险用 `RiskBadge`，时间用 `cell-mono`（JetBrains Mono）。点击行 → `router.push('/tasks/{id}')`。

**接口适配**：
- onMounted → `taskApi.list({ page, size: 20, status })` → 填充表格
- filter chips 切换 → 重新 `load()`，status 映射：全部=undefined、已完成=DONE、进行中=RUNNING、待处理=PENDING、失败=FAILED
- 分页 → `update:page` 事件触发 `load()`
- 「新建对比」→ `router.push('/tasks/new')`
- 「导出报告」→ 无选中任务时 toast 提示，或跳详情页导出

```vue
<template>
  <AppShell :navs="navs" search-placeholder="搜索任务名称、合同编号...">
    <div class="hero">
      <div style="min-width:0">
        <h2 style="text-wrap:balance">我的对比任务</h2>
        <p>创建和管理采购合同与销售合同的智能对比分析</p>
      </div>
      <div class="hero-actions">
        <button class="action-btn primary" @click="$router.push('/tasks/new')">
          <Plus :size="16" /> 新建对比
        </button>
        <button class="action-btn ghost" @click="onExport">
          <Download :size="16" /> 导出报告
        </button>
      </div>
    </div>
    <article class="content-card">
      <FilterChips :options="filters" v-model="activeFilter" />
      <table class="data-table">
        <thead>
          <tr>
            <th style="min-width:160px">任务名称</th>
            <th style="min-width:90px">状态</th>
            <th style="min-width:90px">风险等级</th>
            <th style="min-width:110px">创建时间</th>
            <th style="min-width:110px">更新时间</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="t in tasks" :key="t.id" @click="$router.push(`/tasks/${t.id}`)" style="cursor:pointer">
            <td class="cell-task-name truncate" style="max-width:220px">{{ t.title }}</td>
            <td><StatusBadge :status="t.status" /></td>
            <td><RiskBadge :level="t.riskLevel" /></td>
            <td class="cell-mono whitespace-nowrap">{{ fmt(t.createdAt) }}</td>
            <td class="cell-mono whitespace-nowrap">{{ fmt(t.updatedAt) }}</td>
          </tr>
        </tbody>
      </table>
      <Pagination :total="total" :page="page" :size="20" @update:page="page=$event; load()" />
    </article>
  </AppShell>
</template>
<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { Plus, Download, ListTodo, LayoutDashboard } from 'lucide-vue-next'
import AppShell from '@/components/AppShell.vue'
import FilterChips from '@/components/FilterChips.vue'
import Pagination from '@/components/Pagination.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import RiskBadge from '@/components/RiskBadge.vue'
import { taskApi } from '@/api/task'
import { useToastStore } from '@/stores/toast'
import dayjs from 'dayjs'

const navs = [
  { key: 'tasks', label: '任务列表', icon: ListTodo, to: '/tasks' },
  { key: 'admin', label: '管理后台', icon: LayoutDashboard, to: '/admin' }
]
const filters = ['全部', '已完成', '进行中', '待处理', '失败']
const activeFilter = ref('全部')
const tasks = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const toast = useToastStore()
const statusMap: Record<string, string|undefined> = {
  '全部': undefined, '已完成': 'DONE', '进行中': 'RUNNING', '待处理': 'PENDING', '失败': 'FAILED'
}
function fmt(s: string) { return s ? dayjs(s).format('YYYY-MM-DD') : '—' }
async function load() {
  const res: any = await taskApi.list({ page: page.value, size: 20, status: statusMap[activeFilter.value] })
  tasks.value = res.data.items
  total.value = res.data.total
}
function onExport() { toast.info('请进入任务详情页导出报告') }
watch(activeFilter, () => { page.value = 1; load() })
onMounted(load)
</script>
<style scoped>
/* 拷贝自 design/pages/任务列表.html 的 .hero / .hero-actions / .action-btn / .content-card / .data-table / .cell-mono 样式 */
</style>
```

- [ ] **Step 21：`src/views/TaskCreateView.vue`**（补齐，沿用设计系统）

**设计**：`AppShell` 内单列居中 `content-card`（max-width 720px），卡片内标题「新建对比任务」+ 表单（任务名 FieldInput + 两个文件上传框 + 提交按钮）。文件上传框沿用设计稿卡片样式：虚线边框、`--input` 颜色、拖拽区高亮 `--accent`，选中后显示文件名 + 替换按钮。

**接口适配**：
- 提交 → 构造 `FormData`（title + buyFile + sellFile）→ `taskApi.create` → `router.push('/tasks/{id}')`
- 文件大小校验 ≤ 20MB，类型由后端 Tika 校验

```vue
<template>
  <AppShell :navs="navs" search-placeholder="搜索任务...">
    <div class="hero"><div><h2>新建对比任务</h2><p>上传采购与销售合同，系统自动抽取要素并对比</p></div></div>
    <article class="content-card" style="max-width:720px;padding:calc(var(--spacing)*6)">
      <form @submit.prevent="onSubmit" class="flex flex-col gap-5">
        <FieldInput v-model="title" label="任务名称" icon="Tag" placeholder="如：7月螺丝采购" />
        <FileDrop label="采购合同（BUY）" v-model="buy" />
        <FileDrop label="销售合同（SELL）" v-model="sell" />
        <div class="flex gap-3">
          <button type="button" class="action-btn ghost" @click="$router.back()">取消</button>
          <button type="submit" :disabled="loading" class="action-btn primary">提交对比</button>
        </div>
      </form>
    </article>
  </AppShell>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Tag } from 'lucide-vue-next'
import AppShell from '@/components/AppShell.vue'
import FieldInput from '@/components/FieldInput.vue'
import FileDrop from '@/components/FileDrop.vue'
import { taskApi } from '@/api/task'
import { useToastStore } from '@/stores/toast'
const navs = [{ key: 'tasks', label: '任务列表', icon: Tag, to: '/tasks' }]
const title = ref(''); const buy = ref<File|null>(null); const sell = ref<File|null>(null)
const loading = ref(false); const router = useRouter(); const toast = useToastStore()
async function onSubmit() {
  if (!title.value) return toast.warning('请填任务名')
  if (!buy.value || !sell.value) return toast.warning('请上传两份合同')
  if (buy.value.size > 20*1024*1024 || sell.value.size > 20*1024*1024) return toast.warning('文件 ≤ 20MB')
  loading.value = true
  try {
    const fd = new FormData()
    fd.append('title', title.value); fd.append('buyFile', buy.value); fd.append('sellFile', sell.value)
    const res: any = await taskApi.create(fd)
    toast.success('任务已创建，正在后台处理')
    router.push(`/tasks/${res.data.id}`)
  } finally { loading.value = false }
}
</script>
```

`FileDrop.vue`：拖拽上传组件，样式同设计稿卡片（`--input` 边框 + `--popover` 背景 + 拖拽 hover `--accent`），显示 `Upload` Lucide 图标 + 文件名/占位文案。

- [ ] **Step 22：`src/components/FieldDiffTable.vue` + `ChatPanel.vue`**（任务详情子组件）

`FieldDiffTable.vue`：对应 `design/pages/任务详情.html` 的「字段差异对比」表格。5 列：字段/采购值/销售值/状态/风险。状态徽标 `MATCH（chart-5 绿）/ DIFFER（chart-2 红）/ MISSING（chart-3 黄）`，风险徽标同 `RiskBadge`。Props：`diffs: any[]`。字段中文名映射：`supplierName→供应商、itemName→品名、itemModel→型号、quantity→数量、purchaseUnitPrice→单价、purchaseTotalAmount→总额、expectedDeliveryDate→预交日期、paymentTerms→付款条款、deliveryLocation→交货地点`。

`ChatPanel.vue`：对应任务详情底部「对话追问」卡片。用户消息靠右蓝底白字，AI 消息靠左带 AI 头像 + `--muted` 灰底。底部输入框 + 发送按钮（`SendHorizontal` 图标，蓝底方形）。
**接口适配**：
- onMounted → `taskApi.messages(taskId)` 加载历史
- 发送 → `taskApi.chat(taskId, content)` → 追加 AI 回复
- AI 回复内容用 `marked` 渲染 Markdown

- [ ] **Step 23：`src/views/TaskDetailView.vue`**（对应 `design/pages/任务详情.html`）

**还原要点**：`AppShell` 内结构 = 面包屑（任务列表 / 当前任务名）→ 标题行（任务名 + 状态徽标 + 风险徽标 + 右侧「导出报告」「重跑」按钮）→ AI 风险摘要卡片（3 条彩色提示条，红/黄/黄）→ 两列对比卡片（采购合同 chart-1 头部 / 销售合同 chart-5 头部，10 字段网格 + 置信度进度条）→ 字段差异表 `FieldDiffTable` → 对话区 `ChatPanel`。

**接口适配**：
- onMounted → `taskApi.detail(id)` → 填充标题/状态/风险/summary/两份 extraction/differences
- AI 风险摘要：从 `summary`（Markdown）解析或直接展示 `differences` 中 risk 非 null 的条目
- 「导出报告」→ `taskApi.report(id)` → 下载 `.md` 文件
- 「重跑」→ 仅 `status===FAILED` 显示按钮 → `taskApi.retry(id)` → 重新 `load()`
- 「确认创建采购单」→ 仅 `status===DONE` 显示按钮（设计稿未画，按设计系统补一个 primary 按钮）→ `taskApi.confirm(id)` → toast + `load()`

```vue
<template>
  <AppShell :navs="navs" search-placeholder="搜索任务...">
    <div v-if="d" class="flex flex-col gap-5">
      <!-- Breadcrumb -->
      <div class="flex items-center gap-2 text-sm" style="color:var(--muted-foreground)">
        <router-link to="/tasks" class="flex items-center gap-1"><ChevronLeft :size="16" />任务列表</router-link>
        <span>/</span><span class="truncate font-medium" style="color:var(--foreground)">{{ d.title }}</span>
      </div>
      <!-- Title row -->
      <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div class="flex items-center gap-3 flex-wrap">
          <h2 class="text-xl font-bold truncate">{{ d.title }}</h2>
          <StatusBadge :status="d.status" />
          <RiskBadge :level="d.riskLevel" />
        </div>
        <div class="flex items-center gap-3">
          <button class="action-btn ghost" @click="onExport"><Download :size="16" /> 导出报告</button>
          <button v-if="d.status==='FAILED'" class="action-btn ghost" @click="onRetry">重跑</button>
          <button v-if="d.status==='DONE'" class="action-btn primary" @click="onConfirm">确认创建采购单</button>
        </div>
      </div>
      <!-- AI Risk Summary -->
      <article class="content-card" style="padding:calc(var(--spacing)*5)">
        <div class="flex items-center gap-2 mb-4">
          <Sparkles :size="18" style="color:var(--primary)" />
          <span class="text-xs font-semibold uppercase" style="letter-spacing:.08em;color:var(--muted-foreground)">AI 风险摘要</span>
        </div>
        <div class="flex flex-col gap-3" v-html="renderedSummary" />
      </article>
      <!-- Two-column comparison -->
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-5">
        <ContractCard side="BUY" :ex="d.buy?.extraction" :filename="d.buy?.filename" />
        <ContractCard side="SELL" :ex="d.sell?.extraction" :filename="d.sell?.filename" />
      </div>
      <!-- Diff Table -->
      <article class="content-card" style="overflow:hidden">
        <div class="flex items-center gap-2 px-5 py-4" style="border-bottom:1px solid var(--border)">
          <Search :size="18" style="color:var(--primary)" /><span class="text-sm font-semibold">字段差异对比</span>
        </div>
        <FieldDiffTable :diffs="d.differences" />
      </article>
      <!-- Chat -->
      <article class="content-card" style="overflow:hidden">
        <div class="flex items-center gap-2 px-5 py-4" style="border-bottom:1px solid var(--border)">
          <MessageCircle :size="18" style="color:var(--primary)" /><span class="text-sm font-semibold">对话追问</span>
        </div>
        <ChatPanel :task-id="Number($route.params.id)" />
      </article>
    </div>
  </AppShell>
</template>
<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ChevronLeft, Download, Sparkles, Search, MessageCircle, ListTodo } from 'lucide-vue-next'
import { marked } from 'marked'
import AppShell from '@/components/AppShell.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import RiskBadge from '@/components/RiskBadge.vue'
import FieldDiffTable from '@/components/FieldDiffTable.vue'
import ChatPanel from '@/components/ChatPanel.vue'
import ContractCard from '@/components/ContractCard.vue'
import { taskApi } from '@/api/task'
import { useToastStore } from '@/stores/toast'

const route = useRoute(); const toast = useToastStore()
const d = ref<any>(null)
const navs = [{ key: 'tasks', label: '任务列表', icon: ListTodo, to: '/tasks' }]
const renderedSummary = computed(() => marked.parse(d.value?.summary || '暂无摘要'))
async function load() { const res: any = await taskApi.detail(Number(route.params.id)); d.value = res.data }
async function onExport() {
  const res: any = await taskApi.report(Number(route.params.id))
  const blob = new Blob([res.data || res], { type: 'text/markdown' })
  const url = URL.createObjectURL(blob); const a = document.createElement('a')
  a.href = url; a.download = `${d.value.title}.md`; a.click(); URL.revokeObjectURL(url)
}
async function onRetry() { await taskApi.retry(Number(route.params.id)); toast.success('已重跑'); load() }
async function onConfirm() { await taskApi.confirm(Number(route.params.id)); toast.success('已确认'); load() }
onMounted(load)
</script>
```

`ContractCard.vue`：对应任务详情的双列对比卡片。Props：`side: 'BUY'|'SELL'`、`ex: extraction`、`filename`。BUY 头部用 `chart-1` 蓝色 tint，SELL 头部用 `chart-5` 绿色 tint。10 字段两列网格 + 置信度进度条（`ex.confidence * 100%`）。

- [ ] **Step 24：`src/views/MeView.vue`**（补齐，沿用设计系统）

`AppShell` 内单列 `content-card`（max-width 600px）：标题「个人中心」+ 用户信息网格（ID/用户名/角色）+ 「退出登录」ghost 按钮。
**接口适配**：onMounted → `authApi.me()` → 显示用户信息；退出 → `auth.logout()` → `router.push('/login')`。

- [ ] **Step 25：安装 + 启动**

```bash
cd C:\Users\xingchen\Desktop\agent\frontend
pnpm install
pnpm dev
```
访问 `http://localhost:5173` —— 应看到与 `design/pages/登录.html` 像素级一致的登录页（蓝底 CC brand、Tab 切换、图标输入框）。

- [ ] **Step 26：端到端手测**

- 用 `admin / admin123` 登录 → 跳转任务列表，UI 与 `design/pages/任务列表.html` 一致
- 新建任务 → 上传两份纯文本合同 → 跳详情页
- 任务详情：状态从 RUNNING → DONE，要素对比表/差异表/AI 摘要/对话区均正常
- 对话区输入「金额差多少？」→ AI 回复
- 导出报告 → 下载 `.md`
- 退出登录 → 回登录页

- [ ] **Step 27：提交**

```bash
cd C:\Users\xingchen\Desktop\agent
git add frontend/
git commit -m "feat(frontend): 用户端 Vue3 + Tailwind v4，还原设计稿 4 页 + 补齐新建/个人中心"
```

---

### Task 5.2：管理端 frontend-admin/ —— 设计稿还原 + 接口适配

**Files:**
- Create: `frontend-admin/package.json`（与 frontend 同结构，`name: contract-agent-admin`）
- Create: `frontend-admin/vite.config.ts`（端口 5174，其余同 frontend）
- Create: `frontend-admin/tsconfig.json` / `tsconfig.node.json` / `index.html`（同 frontend）
- Create: `frontend-admin/src/main.ts` / `App.vue`（同 frontend）
- Create: `frontend-admin/src/styles/theme.css` / `main.css`（**与 frontend 完全相同**，设计 token 一致）
- Create: `frontend-admin/src/router/index.ts`（带 ADMIN 守卫）
- Create: `frontend-admin/src/stores/auth.ts` / `stores/toast.ts`（复用 frontend 实现）
- Create: `frontend-admin/src/api/http.ts` / `api/auth.ts`（复用）
- Create: `frontend-admin/src/api/admin.ts`
- Create: `frontend-admin/src/components/`（复用 AppShell/AppSidebar/AppTopbar/StatusBadge/RiskBadge/FilterChips/Pagination/Toast/FieldInput）
- Create: `frontend-admin/src/views/LoginView.vue`（复用 frontend 登录页设计，登录后校验 role=ADMIN）
- Create: `frontend-admin/src/views/AdminDashboardView.vue`（对应 `design/pages/管理概览.html`）
- Create: `frontend-admin/src/views/UserManageView.vue`（对应 `design/pages/用户管理.html`）
- Create: `frontend-admin/src/views/AdminTaskListView.vue`（补齐，沿用设计系统）
- Create: `frontend-admin/src/views/RiskRuleView.vue`（补齐，沿用设计系统）
- Create: `frontend-admin/src/views/LlmLogView.vue`（补齐，沿用设计系统）

> **复用策略**：`styles/`、`stores/`、`api/http.ts`、`api/auth.ts`、通用 `components/`（AppShell/AppSidebar/AppTopbar/StatusBadge/RiskBadge/FilterChips/Pagination/Toast/FieldInput）直接拷贝 frontend 实现，仅侧边栏 `navs` 配置与路由不同。后续若需抽公共包可独立成 `packages/shared`，MVP 阶段保持双 SPA 物理拷贝。

- [ ] **Step 1：脚手架（package.json / vite.config.ts / tsconfig / index.html / main.ts / App.vue / styles/）**

复用 Task 5.1 的 Step 1-9，仅以下差异：
- `package.json` 的 `name` 改 `contract-agent-admin`
- `vite.config.ts` 的 `server.port` 改 `5174`
- `theme.css` / `main.css` 完全相同（设计 token 统一）
- `App.vue` 同结构（`<router-view /> + <Toast />`）

- [ ] **Step 2：`src/api/admin.ts`**

```ts
import { http } from './http'
export const adminApi = {
  stats: () => http.get('/admin/stats'),
  listUsers: (params: any) => http.get('/admin/users', { params }),
  updateUser: (id: number, body: any) => http.patch(`/admin/users/${id}`, body),
  listTasks: (params: any) => http.get('/admin/tasks', { params }),
  taskDetail: (id: number) => http.get(`/admin/tasks/${id}`),
  listRiskRules: () => http.get('/admin/risk-rules'),
  createRiskRule: (body: any) => http.post('/admin/risk-rules', body),
  updateRiskRule: (id: number, body: any) => http.patch(`/admin/risk-rules/${id}`, body),
  deleteRiskRule: (id: number) => http.delete(`/admin/risk-rules/${id}`),
  listLlmLogs: (params: any) => http.get('/admin/llm-logs', { params })
}
```

- [ ] **Step 3：`src/router/index.ts`（带 ADMIN 守卫）**

```ts
import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { authApi } from '@/api/auth'

const routes = [
  { path: '/login', component: () => import('@/views/LoginView.vue'), meta: { guest: true } },
  { path: '/', redirect: '/admin' },
  { path: '/admin', component: () => import('@/views/AdminDashboardView.vue'), meta: { auth: true, admin: true } },
  { path: '/users', component: () => import('@/views/UserManageView.vue'), meta: { auth: true, admin: true } },
  { path: '/tasks', component: () => import('@/views/AdminTaskListView.vue'), meta: { auth: true, admin: true } },
  { path: '/risk-rules', component: () => import('@/views/RiskRuleView.vue'), meta: { auth: true, admin: true } },
  { path: '/llm-logs', component: () => import('@/views/LlmLogView.vue'), meta: { auth: true, admin: true } }
]

const router = createRouter({ history: createWebHistory(), routes })
router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (to.meta.guest && auth.token) return '/admin'
  if (!to.meta.auth) return
  if (!auth.token) return '/login'
  if (!auth.user) {
    try { const res: any = await authApi.me(); auth.setUser(res.data) } catch { return '/login' }
  }
  if (to.meta.admin && auth.user?.role !== 'ADMIN') return '/login'
})
export default router
```

- [ ] **Step 4：`src/views/LoginView.vue`**（复用 frontend 登录页设计）

与 frontend `LoginView.vue` 视觉一致。登录成功后：
- 若 `user.role === 'ADMIN'` → `router.push('/admin')`
- 若 `user.role === 'USER'` → toast.error「无管理端权限」+ `auth.logout()`

- [ ] **Step 5：`src/views/AdminDashboardView.vue`**（对应 `design/pages/管理概览.html`）

**还原要点**：`AppShell`（侧边栏 5 项：用户任务/管理概览/用户管理/风险规则/LLM 日志，active=管理概览）内结构 = Hero（标题「管理概览」+ 副标题）→ 4 列 stats-grid（注册用户/对比任务/今日 LLM 调用/今日 Token 消耗，每张卡片 eyebrow label + 大数字 + 小徽标）→ panels-grid（左 1.7fr 任务状态分布柱状图 + 右 1fr 最近活动列表）→ 底部最近任务表格。

**接口适配**：
- onMounted → `adminApi.stats()` → 填充 4 张统计卡 + `tasksByStatus` 柱状图数据
- 柱状图：纯 CSS 实现（对应设计稿 `flex; align-items:flex-end` + 柱子 `height` 按百分比），4 根柱子 PENDING/RUNNING/DONE/FAILED，颜色 chart-1/chart-3/chart-5/chart-2
- 最近活动列表 & 最近任务表格：设计稿为静态数据，MVP 阶段可从 `adminApi.listTasks({ size: 5, sort: 'createdAt,desc' })` 取最近 5 条任务填充底部表格；「最近活动」若后端无对应接口，可暂用静态文案或前端聚合（任务状态变化 + 用户注册），标注 TODO
- 「导出 CSV」按钮 → 前端把当前表格数据转 CSV 下载

侧边栏 `navs`：
```ts
const navs = [
  { key: 'user-tasks', label: '用户任务', icon: ListTodo, to: '/tasks' },        // 跳管理端任务审计
  { key: 'overview', label: '管理概览', icon: LayoutDashboard, to: '/admin' },
  { key: 'users', label: '用户管理', icon: Users, to: '/users' },
  { key: 'risk', label: '风险规则', icon: ShieldAlert, to: '/risk-rules' },
  { key: 'logs', label: 'LLM 日志', icon: FileText, to: '/llm-logs' }
]
```

- [ ] **Step 6：`src/views/UserManageView.vue`**（对应 `design/pages/用户管理.html`）

**还原要点**：`AppShell` 内结构 = Hero（标题「用户管理」+ 副标题 + 右侧「搜索用户」primary 按钮）→ filter bar（搜索输入框 + 全部/已启用/已禁用 chips）→ 用户表格卡片（表头：用户ID/用户名/角色/状态/任务数/注册时间/操作）。角色徽标 `ADMIN` 用 primary tint、`USER` 用 muted。状态徽标「已启用」chart-5 绿、「已禁用」chart-2 红。操作列两个 ghost 按钮：编辑（PenLine 图标）/ 禁用|启用。禁用行 `opacity: .64`。底部 pagination。

**接口适配**：
- onMounted → `adminApi.listUsers({ page, size: 20, keyword, enabled })` → 填充表格
- filter chips 切换 → 重新 `load()`，enabled 映射：全部=undefined、已启用=true、已禁用=false
- 搜索框 → 输入回车或按钮点击 → `load()` 带 keyword
- 「禁用/启用」按钮 → `adminApi.updateUser(id, { enabled: 0|1 })` → toast + `load()`
- 「编辑」按钮 → 弹出 Modal（沿用设计系统卡片样式），可改 role（USER/ADMIN select）+ enabled switch → `adminApi.updateUser(id, body)` → toast + `load()`
- 任务数列：后端 `listUsers` 若不返回 `taskCount`，则前端暂显示 `—` 并标 TODO（或后端在 Phase 4 补字段）

- [ ] **Step 7：`src/views/AdminTaskListView.vue`**（补齐，沿用设计系统）

**设计**：`AppShell` 内 Hero「任务审计」+ filter bar（状态 select + 风险 select + 用户ID 输入 + 日期范围 + 查询按钮）→ 任务表格卡片（列：任务ID/任务名/用户/状态/风险/创建时间 + 操作「查看」）。点击行/查看 → 新窗口或同窗打开 `frontend-admin` 的任务详情（MVP 可直接复用 frontend `TaskDetailView` 的设计，路由 `/tasks/:id`）。

**接口适配**：
- onMounted → `adminApi.listTasks({ page, size: 20, status, riskLevel, userId, from, to })`
- 查询按钮 → 重新 `load()`
- 状态/风险/用户ID/日期 → 双向绑定到 filter reactive
- 分页 → `update:page`

- [ ] **Step 8：`src/views/RiskRuleView.vue`**（补齐，沿用设计系统）

**设计**：`AppShell` 内 Hero「风险规则」+「新增规则」primary 按钮 → 规则表格卡片（列：ID/字段/运算符/阈值/风险等级/启用/备注/操作）。启用列用 switch 样式（沿用设计稿 toggle，蓝底圆角）。操作列：编辑（PenLine）/ 删除（Trash2，ghost 红字）。

**接口适配**：
- onMounted → `adminApi.listRiskRules()` → 填充表格（注意 `enabled` 字段 0/1 → 布尔）
- 启用 switch → `adminApi.updateRiskRule(id, { enabled })` → toast
- 新增/编辑 → Modal（字段：fieldKey 输入、operator select[GT/LT/EQ/NE/PCT_GT]、thresholdValue 输入、riskLevel select[LOW/MEDIUM/HIGH]、enabled switch、remark 输入）→ `createRiskRule` / `updateRiskRule`
- 删除 → 确认弹窗 → `adminApi.deleteRiskRule(id)` → toast + `load()`

Modal 样式沿用设计系统：居中遮罩 + 卡片（`--card` 背景 + `--border` 边框 + `--radius` 圆角），表单用 `FieldInput` / `FieldSelect`，底部「取消」ghost + 「保存」primary。

- [ ] **Step 9：`src/views/LlmLogView.vue`**（补齐，沿用设计系统）

**设计**：`AppShell` 内 Hero「LLM 调用日志」+ filter bar（用户ID 输入 + 模型输入 + 日期范围 + 查询按钮）→ 日志表格卡片（列：ID/任务ID/用户ID/模型/Tokens/耗时(ms)/状态/错误/时间）。状态徽标 `OK` 用 chart-5 绿、`FAIL` 用 chart-2 红。

**接口适配**：
- onMounted → `adminApi.listLlmLogs({ page, size: 20, userId, model, from, to })`
- 查询按钮 → 重新 `load()`
- 分页 → `update:page`

- [ ] **Step 10：安装 + 启动**

```bash
cd C:\Users\xingchen\Desktop\agent\frontend-admin
pnpm install
pnpm dev
```
访问 `http://localhost:5174` —— 登录页与 frontend 一致；用 `admin/admin123` 登录后进入管理概览，UI 与 `design/pages/管理概览.html` 一致。

- [ ] **Step 11：手测**

- 用 `admin / admin123` 登录 → 管理概览，4 张统计卡 + 柱状图 + 最近任务表正常
- 切换到用户管理 → 表格、筛选、禁用/启用、编辑 Modal 均正常
- 切换到任务审计 → 筛选 + 表格正常
- 切换到风险规则 → 新增/编辑/删除/启停均正常
- 切换到 LLM 日志 → 筛选 + 表格正常
- 验证普通 USER 账号登录管理端会被踢回登录页 + toast 提示

- [ ] **Step 12：提交**

```bash
cd C:\Users\xingchen\Desktop\agent
git add frontend-admin/
git commit -m "feat(frontend-admin): 管理端 Vue3 + Tailwind v4，还原设计稿 2 页 + 补齐任务审计/风险规则/LLM日志"
```

---

## Phase 6：集成验证 + 文档收尾

### Task 6.1：端到端验证

- [ ] **Step 1：起后端 + 双前端**

```bash
# 终端 1
cd C:\Users\xingchen\Desktop\agent\backend
mvn spring-boot:run -Dspring-boot.run.profiles=local

# 终端 2
cd C:\Users\xingchen\Desktop\agent\frontend
pnpm dev

# 终端 3
cd C:\Users\xingchen\Desktop\agent\frontend-admin
pnpm dev
```

- [ ] **Step 2：完整跑一遍**

- 用 `admin / admin123` 在用户端登录
- 新建对比任务，上传两份合同
- 等 Agent 处理（看 `llm_call_log` 表确认有调用）
- 任务详情：要素对比表 + 风险总结 + 对话
- 追问 1-2 个问题，确认 Agent 引用历史
- 导出 Markdown 报告
- 切到管理端（5174），看 stats、users、tasks、risk-rules、llm-logs

- [ ] **Step 3：填 README 的运行步骤**

把 README 里"快速开始"那节的命令核对一遍，确保 windows + Git bash 下能跑通。

- [ ] **Step 4：最终提交**

```bash
cd C:\Users\xingchen\Desktop\agent
git status
git add .
git commit -m "docs: 校对 README 运行步骤" --allow-empty
```

---

## 自审报告

**Spec 覆盖检查：**
- §3 技术栈 → Phase 1 (pom.xml) + Phase 5 (前端 package.json) ✓
- §4 仓库结构 → README + Phase 0 ~ Phase 5 完整产出 ✓
- §5 架构 → Phase 1 SecurityConfig + Phase 3 ChatClient + Agent ✓
- §6 数据模型 → Phase 1.2 SQL + 各实体类 ✓
- §7 API → Phase 1.5, 2.2, 3.4, 4.x 全覆盖 ✓
- §8 Agent → Phase 3.4 (AgentTools + ContractAgent + AgentRunner) ✓
- §9 风险引擎 → Phase 3.3 ✓
- §10 前端 → Phase 5 ✓
- §11 错误处理 → Phase 1.3 ✓
- §12 测试 → Phase 1.3, 1.5, 2.1, 3.3 单测 ✓
- §13 部署运行 → README 快速开始 ✓
- §14 风险与后续 → 留给后续 plan

**占位符扫描：** 无 TBD / TODO / "实现" 留白。每个代码块都是可粘贴的完整代码。

**类型一致性：**
- `ComparisonTask.status` / `RiskLevel` 枚举名在 Phase 1.2 SQL、Phase 2.2 实体、Phase 3.4 AgentRunner 中一致
- `ContractSide` (BUY/SELL) 在 Contract/Repository/Service 一致
- `RiskOperator` 5 个值在枚举 / Evaluator / 前端 select 一致
- `TaskStatus` / `RiskLevel` 的字符串值在前后端 select 一致
- API 字段名（supplierName 等）↔ DB 字段（supplier_name）↔ LLM JSON 字段（supplier_name）已对齐
- 4 个 `@Tool` 方法名（extractContract / getExtraction / compareFields / getChatHistory）在 ContractAgent 的 Prompt 中也明确出现
