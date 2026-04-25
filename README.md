# Spring Boot + SQLite 项目理解

## 项目概述

这是一个基于 Spring Boot 3.2.0 和 Java 21 的 SQLite 演示项目，展示了如何在 Spring Boot 中集成 SQLite 数据库，并提供了学生和成绩的 RESTful API。

***

## 1. SQLite 如何创建、设计数据库表

### 1.1 数据库创建方式

SQLite 是一个文件型数据库，数据库文件会在首次连接时自动创建。在本项目中：

- **配置文件**：`src/main/resources/application.properties`
  ```properties
  spring.datasource.url=jdbc:sqlite:demo.db
  spring.datasource.driver-class-name=org.sqlite.JDBC
  ```
  当应用启动时，SQLite JDBC 驱动会检查 `demo.db` 文件是否存在，如果不存在则自动创建。

### 1.2 表设计方式

项目通过 `schema.sql` 文件定义数据库表结构：

**文件位置**：`src/main/resources/schema.sql`

```sql
-- 创建学生表
CREATE TABLE IF NOT EXISTS student (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    age INTEGER,
    gender TEXT,
    major TEXT
);

-- 创建成绩表
CREATE TABLE IF NOT EXISTS score (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    student_id INTEGER NOT NULL,
    subject TEXT NOT NULL,
    score REAL,
    exam_date TEXT,
    FOREIGN KEY (student_id) REFERENCES student(id)
);
```

### 1.3 SQLite 表设计要点

| 特性                                  | 说明                                                                                  |
| ----------------------------------- | ----------------------------------------------------------------------------------- |
| `CREATE TABLE IF NOT EXISTS`        | 确保表不存在时才创建，避免重复创建错误                                                                 |
| `INTEGER PRIMARY KEY AUTOINCREMENT` | 定义自增主键，SQLite 会自动生成唯一的整数值                                                           |
| 数据类型                                | SQLite 使用动态类型系统，主要类型：`INTEGER`、`TEXT`、`REAL`、`BLOB`、`NULL`                          |
| 外键约束                                | 使用 `FOREIGN KEY` 定义外键，但需要注意：SQLite 默认**不强制**外键约束，需要显式启用 `PRAGMA foreign_keys = ON;` |

***

## 2. SQLite 和 MySQL 的区别

### 2.1 架构与部署对比

| 特性       | SQLite               | MySQL           |
| -------- | -------------------- | --------------- |
| **类型**   | 文件型嵌入式数据库            | 客户端-服务器型关系数据库   |
| **服务器**  | 无需独立服务器进程            | 需要独立的 MySQL 服务器 |
| **存储**   | 单个文件存储所有数据           | 多个文件/表空间存储      |
| **并发**   | 写操作锁定整个数据库           | 支持行级锁定，高并发性能好   |
| **适用场景** | 嵌入式设备、移动应用、小型应用、测试环境 | 大型企业应用、高并发系统    |

### 2.2 数据库事务对比

| 特性       | SQLite                  | MySQL                      |
| -------- | ----------------------- | -------------------------- |
| **隔离级别** | 默认 `SERIALIZABLE`（可串行化） | 默认 `REPEATABLE READ`（可重复读） |
| **事务支持** | 支持 ACID 事务              | 支持 ACID 事务（InnoDB 引擎）      |
| **自动提交** | 默认自动提交                  | 默认自动提交                     |
| **锁机制**  | 数据库级锁（写操作阻塞所有其他操作）      | 行级锁（InnoDB），并发性能更好         |
| **保存点**  | 支持 SAVEPOINT            | 支持 SAVEPOINT               |

### 2.3 SQL 语法对比

#### 数据类型差异

| SQLite               | MySQL                           | 说明                               |
| -------------------- | ------------------------------- | -------------------------------- |
| `INTEGER`            | `INT`, `INTEGER`, `BIGINT`      | SQLite 的 INTEGER 是动态类型           |
| `TEXT`               | `VARCHAR(n)`, `CHAR(n)`, `TEXT` | SQLite 的 TEXT 无长度限制              |
| `REAL`               | `FLOAT`, `DOUBLE`, `DECIMAL`    | SQLite 的 REAL 是 64 位浮点数          |
| `BLOB`               | `BLOB`, `BINARY`                | 二进制数据                            |
| 无 `BOOLEAN` 类型       | `BOOLEAN`, `TINYINT(1)`         | SQLite 用 `INTEGER` 0/1 表示布尔值     |
| 无 `DATE/DATETIME` 类型 | `DATE`, `DATETIME`, `TIMESTAMP` | SQLite 用 `TEXT` 或 `INTEGER` 存储日期 |

#### 自增主键差异

**SQLite:**

```sql
id INTEGER PRIMARY KEY AUTOINCREMENT
```

**MySQL:**

```sql
id INT PRIMARY KEY AUTO_INCREMENT
```

#### 字符串拼接

**SQLite:**

```sql
SELECT 'Hello' || ' ' || 'World';
```

**MySQL:**

```sql
SELECT CONCAT('Hello', ' ', 'World');
-- 或
SELECT 'Hello' ' ' 'World';
```

#### LIMIT 子句

两者语法相同：

```sql
SELECT * FROM table LIMIT 10 OFFSET 20;
```

#### IFNULL / COALESCE

**SQLite:**

```sql
SELECT IFNULL(column, 'default') FROM table;
SELECT COALESCE(column, 'default') FROM table;
```

**MySQL:**

```sql
SELECT IFNULL(column, 'default') FROM table;
SELECT COALESCE(column, 'default') FROM table;
SELECT IF(column IS NULL, 'default', column) FROM table;
```

### 2.4 其他重要区别

| 特性          | SQLite               | MySQL         |
| ----------- | -------------------- | ------------- |
| **用户权限**    | 无内置用户权限系统，依赖文件系统权限   | 完整的用户权限管理系统   |
| **存储过程**    | 不支持存储过程              | 支持存储过程、函数、触发器 |
| **视图**      | 支持只读视图               | 支持可更新视图       |
| **全文搜索**    | 支持 FTS3/FTS4/FTS5 扩展 | 内置全文搜索支持      |
| **JSON 支持** | 支持 JSON1 扩展          | 原生 JSON 数据类型  |

***

## 3. JdbcTemplate 使用 SQLite 的关键代码，与 MySQL 的区别

### 3.1 依赖配置

**SQLite 依赖** (`pom.xml`):

```xml
<dependency>
    <groupId>org.xerial</groupId>
    <artifactId>sqlite-jdbc</artifactId>
    <version>3.42.0.0</version>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jdbc</artifactId>
</dependency>
```

**MySQL 依赖** (对比):

```xml
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jdbc</artifactId>
</dependency>
```

### 3.2 数据源配置

**SQLite 配置** (`application.properties`):

```properties
spring.datasource.url=jdbc:sqlite:demo.db
spring.datasource.driver-class-name=org.sqlite.JDBC
# SQLite 不需要用户名和密码
```

**MySQL 配置** (对比):

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/mydb?useSSL=false&serverTimezone=UTC
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.username=root
spring.datasource.password=password
```

### 3.3 JdbcTemplate 关键代码

本项目中 JdbcTemplate 的使用方式在 `ScoreRepository` 和 `StudentRepository` 中：

#### 查询操作

**代码位置**：`src/main/java/com/example/repository/ScoreRepository.java:25-28`

```java
public List<Score> findAll() {
    String sql = "SELECT * FROM score";
    return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(Score.class));
}
```

**与 MySQL 的区别**：**完全相同**。标准的 SELECT 语句和 JdbcTemplate API 在两种数据库中使用方式一致。

#### 根据 ID 查询

**代码位置**：`src/main/java/com/example/repository/ScoreRepository.java:31-39`

```java
public Optional<Score> findById(Long id) {
    String sql = "SELECT * FROM score WHERE id = ?";
    try {
        Score score = jdbcTemplate.queryForObject(sql, 
            new BeanPropertyRowMapper<>(Score.class), id);
        return Optional.ofNullable(score);
    } catch (Exception e) {
        return Optional.empty();
    }
}
```

**与 MySQL 的区别**：**完全相同**。

#### 插入数据（获取自增主键）

**代码位置**：`src/main/java/com/example/repository/ScoreRepository.java:48-65`

```java
public Score save(Score score) {
    String sql = "INSERT INTO score (student_id, subject, score, exam_date) VALUES (?, ?, ?, ?)";
    KeyHolder keyHolder = new GeneratedKeyHolder();

    jdbcTemplate.update(connection -> {
        PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setLong(1, score.getStudentId());
        ps.setString(2, score.getSubject());
        ps.setObject(3, score.getScore());
        ps.setString(4, score.getExamDate());
        return ps;
    }, keyHolder);

    // 获取生成的ID
    Long generatedId = keyHolder.getKey().longValue();
    score.setId(generatedId);
    return score;
}
```

**与 MySQL 的区别**：

- **API 使用方式完全相同**：`KeyHolder` 和 `Statement.RETURN_GENERATED_KEYS` 的使用方式一致
- **SQL 语法注意点**：
  - SQLite：`AUTOINCREMENT` 关键字
  - MySQL：`AUTO_INCREMENT` 关键字

#### 更新操作

**代码位置**：`src/main/java/com/example/repository/ScoreRepository.java:68-77`

```java
public Score update(Score score) {
    String sql = "UPDATE score SET student_id = ?, subject = ?, score = ?, exam_date = ? WHERE id = ?";
    jdbcTemplate.update(sql,
            score.getStudentId(),
            score.getSubject(),
            score.getScore(),
            score.getExamDate(),
            score.getId());
    return score;
}
```

**与 MySQL 的区别**：**完全相同**。

#### 删除操作

**代码位置**：`src/main/java/com/example/repository/ScoreRepository.java:80-83`

```java
public void deleteById(Long id) {
    String sql = "DELETE FROM score WHERE id = ?";
    jdbcTemplate.update(sql, id);
}
```

**与 MySQL 的区别**：**完全相同**。

### 3.4 JdbcTemplate 使用 SQLite 与 MySQL 的主要区别总结

| 方面                   | SQLite            | MySQL                      | 影响                     |
| -------------------- | ----------------- | -------------------------- | ---------------------- |
| **数据源配置**            | 无需用户名密码           | 需要用户名密码                    | 配置文件不同                 |
| **URL 格式**           | `jdbc:sqlite:文件名` | `jdbc:mysql://主机:端口/数据库`   | 连接串格式不同                |
| **驱动类**              | `org.sqlite.JDBC` | `com.mysql.cj.jdbc.Driver` | 驱动类不同                  |
| **JdbcTemplate API** | 完全相同              | 完全相同                       | 代码层面无差异                |
| **SQL 语法**           | 部分差异（如自增关键字）      | 标准 SQL                     | 编写 SQL 时需注意            |
| **事务管理**             | Spring 管理相同       | Spring 管理相同                | 配置 `@Transactional` 即可 |

### 3.5 注意事项

1. **SQLite 外键约束**：SQLite 默认不强制外键约束，如需启用，需要在连接后执行：
   ```java
   jdbcTemplate.execute("PRAGMA foreign_keys = ON;");
   ```
2. **日期时间处理**：SQLite 没有原生的日期时间类型，通常使用 `TEXT` 存储（如 "2024-01-15"）或 `INTEGER` 存储 Unix 时间戳。
3. **并发性能**：SQLite 的写操作会锁定整个数据库，不适合高并发写入场景。如果需要高并发，应该考虑切换到 MySQL 或 PostgreSQL。

***

## 4. 第一次启动 Spring Boot 时，是否可自动创建 SQLite 数据库

### 4.1 答案：是的，可以自动创建

本项目已经配置了自动创建机制，让我们详细分析：

### 4.2 自动创建的配置

**配置文件**：`src/main/resources/application.properties`

```properties
# SQLite 数据库配置
spring.datasource.url=jdbc:sqlite:demo.db
spring.datasource.driver-class-name=org.sqlite.JDBC

# 初始化数据库
spring.sql.init.mode=always
spring.sql.init.schema-locations=classpath:schema.sql
```

### 4.3 自动创建流程

```
Spring Boot 启动
       ↓
1. 加载数据源配置
       ↓
2. SQLite JDBC 驱动检查 demo.db 文件
       ├─ 不存在 → 自动创建新的数据库文件
       └─ 存在 → 使用现有文件
       ↓
3. 执行 spring.sql.init 配置
       ├─ mode=always → 每次启动都执行初始化
       └─ schema-locations → 执行 classpath:schema.sql
       ↓
4. schema.sql 中的 CREATE TABLE IF NOT EXISTS
       ├─ 表不存在 → 创建新表
       └─ 表已存在 → 不执行任何操作
       ↓
5. 数据库和表准备完成，应用启动成功
```

### 4.4 关键配置说明

| 配置项                                   | 值                      | 说明                                               |
| ------------------------------------- | ---------------------- | ------------------------------------------------ |
| `spring.datasource.url`               | `jdbc:sqlite:demo.db`  | 指定数据库文件路径，不存在时自动创建                               |
| `spring.datasource.driver-class-name` | `org.sqlite.JDBC`      | SQLite JDBC 驱动                                   |
| `spring.sql.init.mode`                | `always`               | 每次启动都执行 SQL 初始化（可选值：`always`、`never`、`embedded`） |
| `spring.sql.init.schema-locations`    | `classpath:schema.sql` | 指定要执行的 SQL 脚本位置                                  |

### 4.5 schema.sql 的设计

**文件位置**：`src/main/resources/schema.sql`

```sql
-- 创建学生表
CREATE TABLE IF NOT EXISTS student (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    age INTEGER,
    gender TEXT,
    major TEXT
);

-- 创建成绩表
CREATE TABLE IF NOT EXISTS score (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    student_id INTEGER NOT NULL,
    subject TEXT NOT NULL,
    score REAL,
    exam_date TEXT,
    FOREIGN KEY (student_id) REFERENCES student(id)
);
```

**关键点**：

- 使用 `CREATE TABLE IF NOT EXISTS` 确保表不会重复创建
- 即使 `spring.sql.init.mode=always`，也不会因为表已存在而报错
- 这样可以实现：**首次启动创建数据库和表，后续启动仅验证表结构**

### 4.6 不同初始化模式对比

| `spring.sql.init.mode` 值 | 行为                             | 适用场景                              |
| ------------------------ | ------------------------------ | --------------------------------- |
| `always`                 | 每次启动都执行初始化脚本                   | 开发环境、测试环境，需要每次重置数据库               |
| `never`                  | 不执行任何初始化                       | 生产环境，数据库已预先配置好                    |
| `embedded`               | 仅对嵌入式数据库（如 H2、HSQL、Derby）执行初始化 | 生产环境使用 MySQL/PostgreSQL，开发环境使用 H2 |

### 4.7 生产环境建议

对于生产环境，建议修改配置：

```properties
# 生产环境：不自动执行初始化
spring.sql.init.mode=never

# 或使用条件配置（仅在开发环境自动初始化）
# spring.sql.init.mode=${SQL_INIT_MODE:never}
```

**原因**：

1. 生产数据库通常由 DBA 预先创建和管理
2. 自动执行 SQL 可能导致意外的数据丢失
3. 应该使用数据库迁移工具（如 Flyway 或 Liquibase）管理 schema 变更

### 4.8 验证自动创建

可以通过以下步骤验证：

1. **删除现有数据库文件**（如果存在）：
   ```bash
   rm demo.db
   ```
2. **启动应用**：
   ```bash
   mvn spring-boot:run
   ```
3. **观察日志**：应该看到类似以下输出：
   ```
   ========================================
     SQLite Demo Application Started!
   ========================================
   ```
4. **验证数据库文件已创建**：
   ```bash
   ls -la demo.db
   ```
5. **验证表已创建**（使用 SQLite 命令行工具）：
   ```bash
   sqlite3 demo.db ".tables"
   # 输出：score  student

   sqlite3 demo.db ".schema student"
   # 输出表结构
   ```

***

## 项目结构总结

```
main/
├── src/
│   ├── main/
│   │   ├── java/com/example/
│   │   │   ├── SqliteDemoApplication.java    # 主应用类
│   │   │   ├── controller/
│   │   │   │   ├── ScoreController.java       # 成绩 REST API
│   │   │   │   └── StudentController.java     # 学生 REST API
│   │   │   ├── entity/
│   │   │   │   ├── Score.java                 # 成绩实体
│   │   │   │   └── Student.java               # 学生实体
│   │   │   └── repository/
│   │   │       ├── ScoreRepository.java       # 成绩数据访问
│   │   │       └── StudentRepository.java     # 学生数据访问
│   │   └── resources/
│   │       ├── application.properties         # 应用配置
│   │       └── schema.sql                     # 数据库初始化脚本
│   └── test/
├── pom.xml                                      # Maven 依赖
├── demo.db                                      # SQLite 数据库文件（运行时创建）
└── README.md                                    # 本文档
```

***

## API 接口

启动应用后，可访问以下 API：

### 学生 API (`http://localhost:8080/api/students`)

| 方法     | 路径                   | 说明         |
| ------ | -------------------- | ---------- |
| GET    | `/api/students`      | 获取所有学生     |
| GET    | `/api/students/{id}` | 根据 ID 获取学生 |
| POST   | `/api/students`      | 创建学生       |
| PUT    | `/api/students/{id}` | 更新学生       |
| DELETE | `/api/students/{id}` | 删除学生       |

### 成绩 API (`http://localhost:8080/api/scores`)

| 方法     | 路径                                | 说明           |
| ------ | --------------------------------- | ------------ |
| GET    | `/api/scores`                     | 获取所有成绩       |
| GET    | `/api/scores/{id}`                | 根据 ID 获取成绩   |
| GET    | `/api/scores/student/{studentId}` | 根据学生 ID 获取成绩 |
| POST   | `/api/scores`                     | 创建成绩         |
| PUT    | `/api/scores/{id}`                | 更新成绩         |
| DELETE | `/api/scores/{id}`                | 删除成绩         |

***

## 快速开始

1. **编译项目**：
   ```bash
   mvn clean compile
   ```
2. **运行应用**：
   ```bash
   mvn spring-boot:run
   ```
3. **测试 API**：
   ```bash
   # 创建学生
   curl -X POST http://localhost:8080/api/students \
     -H "Content-Type: application/json" \
     -d '{"name":"张三","age":20,"gender":"男","major":"计算机科学"}'

   # 获取所有学生
   curl http://localhost:8080/api/students
   ```

