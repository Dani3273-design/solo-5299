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

## 2. SQLite 与 MySQL 8.0 的详细对比

### 2.1 版本说明

本文档中的 MySQL 对比**特指 MySQL 8.0 版本**。MySQL 8.0 是 MySQL 的最新长期支持版本（LTS），引入了许多重要特性：
- 原生 UTF-8 支持（utf8mb4 作为默认字符集）
- 窗口函数（Window Functions）
- 通用表表达式（CTE）
- 不可见索引（Invisible Indexes）
- 降序索引（Descending Indexes）
- 原子 DDL 语句
- 增强的 JSON 功能
- 角色管理（Role Management）

### 2.2 架构与部署对比

| 特性       | SQLite                               | MySQL 8.0                               |
| -------- | ------------------------------------ | --------------------------------------- |
| **类型**   | 文件型嵌入式数据库                        | 客户端-服务器型关系数据库                     |
| **服务器**  | 无需独立服务器进程                        | 需要独立的 MySQL 服务器进程                  |
| **存储**   | 单个文件存储所有数据                       | 多个文件/表空间存储（InnoDB 引擎使用共享表空间或独立表空间） |
| **并发**   | 写操作锁定整个数据库                        | 支持行级锁定（InnoDB），高并发性能优秀              |
| **内存管理** | 轻量级，内存占用小                          | 需要配置缓冲池（Buffer Pool），内存占用较大           |
| **适用场景** | 嵌入式设备、移动应用、小型应用、测试环境、原型开发 | 大型企业应用、高并发系统、生产环境、数据密集型应用        |

### 2.3 数据库事务与锁机制对比

| 特性       | SQLite                                  | MySQL 8.0（InnoDB 引擎）                          |
| -------- | --------------------------------------- | ------------------------------------------------- |
| **隔离级别** | 默认 `SERIALIZABLE`（可串行化），仅支持读未提交和可串行化 | 默认 `REPEATABLE READ`（可重复读），支持所有 4 种隔离级别（读未提交、读已提交、可重复读、可串行化） |
| **事务支持** | 支持 ACID 事务，使用数据库级锁                  | 支持 ACID 事务，使用行级锁和 MVCC（多版本并发控制）         |
| **自动提交** | 默认自动提交                                | 默认自动提交，可通过 `SET autocommit = 0;` 关闭         |
| **锁机制**  | 数据库级锁（写操作阻塞所有其他读/写操作）              | 行级锁（Record Locks）+ 间隙锁（Gap Locks）+ 意向锁（Intention Locks） |
| **锁粒度**  | 粗粒度（整个数据库）                           | 细粒度（行级）                                    |
| **MVCC**   | 不支持（通过数据库级锁实现隔离）                     | 完全支持，通过 Undo Log 实现多版本并发控制                 |
| **死锁检测** | 不支持（由于数据库级锁，不会产生死锁）                 | 完全支持，自动检测并回滚持有最少行锁的事务                    |
| **保存点**  | 支持 SAVEPOINT                          | 支持 SAVEPOINT，支持部分回滚                           |

#### 事务隔离级别详细对比

| 隔离级别 | SQLite | MySQL 8.0 (InnoDB) | 说明 |
| ------ | ------ | ------------------- | ---- |
| 读未提交（Read Uncommitted） | ✅ 支持 | ✅ 支持 | 可能读到未提交的数据（脏读） |
| 读已提交（Read Committed） | ❌ 不支持 | ✅ 支持 | 只能读取已提交的数据，避免脏读 |
| 可重复读（Repeatable Read） | ❌ 不支持 | ✅ 支持（默认） | 同一事务内多次读取结果一致 |
| 可串行化（Serializable） | ✅ 支持（默认） | ✅ 支持 | 最高隔离级别，完全串行化执行 |

### 2.4 数据类型对比

#### 核心数据类型对比表

| 数据类别 | SQLite | MySQL 8.0 | 说明 |
| ------- | ------ | --------- | ---- |
| **整数类型** | `INTEGER` | `TINYINT`, `SMALLINT`, `MEDIUMINT`, `INT`, `BIGINT` | SQLite 的 INTEGER 是动态类型，可存储任意大小整数；MySQL 有严格的类型限制 |
| **浮点类型** | `REAL` | `FLOAT`, `DOUBLE`, `DECIMAL(M,D)` | SQLite 的 REAL 是 64 位浮点数；MySQL 支持精确小数类型 DECIMAL |
| **字符串类型** | `TEXT` | `CHAR(n)`, `VARCHAR(n)`, `TEXT`, `MEDIUMTEXT`, `LONGTEXT` | SQLite 的 TEXT 无长度限制；MySQL 的 VARCHAR 最大 65535 字节 |
| **二进制类型** | `BLOB` | `BINARY`, `VARBINARY`, `BLOB`, `MEDIUMBLOB`, `LONGBLOB` | 两者都支持二进制数据存储 |
| **布尔类型** | 无原生类型，用 `INTEGER` 0/1 表示 | `BOOLEAN`（映射为 TINYINT(1)） | MySQL 8.0 有 BOOLEAN 关键字，但实际存储为 TINYINT |
| **日期时间类型** | 无原生类型，用 `TEXT` 或 `INTEGER` 存储 | `DATE`, `TIME`, `DATETIME`, `TIMESTAMP`, `YEAR` | MySQL 8.0 有完整的日期时间类型支持 |
| **JSON 类型** | 无原生类型，用 `TEXT` 存储（需 JSON1 扩展） | `JSON`（原生类型） | MySQL 8.0 原生支持 JSON 类型，支持索引和部分更新 |
| **枚举类型** | 不支持 | `ENUM('value1', 'value2', ...)` | MySQL 8.0 支持枚举类型 |
| **集合类型** | 不支持 | `SET('value1', 'value2', ...)` | MySQL 8.0 支持集合类型 |
| **空间数据类型** | 不支持 | `GEOMETRY`, `POINT`, `LINESTRING`, `POLYGON` 等 | MySQL 8.0 支持 GIS 空间数据类型 |

#### 日期时间类型详细对比

**MySQL 8.0 日期时间类型：**

| 类型 | 格式 | 范围 | 说明 |
| ---- | ---- | ---- | ---- |
| `DATE` | 'YYYY-MM-DD' | '1000-01-01' 到 '9999-12-31' | 仅日期 |
| `TIME` | 'HH:MM:SS' | '-838:59:59' 到 '838:59:59' | 仅时间 |
| `DATETIME` | 'YYYY-MM-DD HH:MM:SS' | '1000-01-01 00:00:00' 到 '9999-12-31 23:59:59' | 日期+时间，无时区 |
| `TIMESTAMP` | 'YYYY-MM-DD HH:MM:SS' | '1970-01-01 00:00:01' UTC 到 '2038-01-19 03:14:07' UTC | 日期+时间，有时区，自动转换 |
| `YEAR` | YYYY | 1901 到 2155 | 仅年份 |

**SQLite 日期时间存储方式：**

```sql
-- 方式1：使用 TEXT 存储 ISO8601 格式
CREATE TABLE example (
    created_at TEXT  -- '2024-01-15 10:30:00'
);

-- 方式2：使用 INTEGER 存储 Unix 时间戳
CREATE TABLE example (
    created_at INTEGER  -- 1705311000
);

-- 方式3：使用 REAL 存储 Julian day 数字
CREATE TABLE example (
    created_at REAL  -- 2460325.5
);
```

### 2.5 SQL 语法详细对比

#### 2.5.1 自增主键

**SQLite：**
```sql
CREATE TABLE student (
    id INTEGER PRIMARY KEY AUTOINCREMENT,  -- 必须是 INTEGER PRIMARY KEY
    name TEXT NOT NULL
);
```

**MySQL 8.0：**
```sql
CREATE TABLE student (
    id INT PRIMARY KEY AUTO_INCREMENT,      -- INT/BIGINT 都可以
    name VARCHAR(100) NOT NULL
);

-- MySQL 8.0 新特性：可以在 CREATE TABLE 中指定索引
CREATE TABLE student (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    INDEX idx_name (name)
);
```

#### 2.5.2 字符串拼接

**SQLite：**
```sql
SELECT 'Hello' || ' ' || 'World';  -- 使用 || 操作符
```

**MySQL 8.0：**
```sql
-- 方式1：使用 CONCAT 函数（推荐）
SELECT CONCAT('Hello', ' ', 'World');

-- 方式2：使用空格分隔（仅在特定 SQL mode 下有效）
SELECT 'Hello' ' ' 'World';

-- MySQL 8.0 新增：CONCAT_WS（带分隔符拼接）
SELECT CONCAT_WS(', ', 'Apple', 'Banana', 'Cherry');
-- 输出: Apple, Banana, Cherry
```

#### 2.5.3 日期时间函数

**获取当前日期时间：**

| 操作 | SQLite | MySQL 8.0 |
| ---- | ------ | --------- |
| 当前日期 | `DATE('now')` | `CURDATE()` 或 `CURRENT_DATE` |
| 当前时间 | `TIME('now')` | `CURTIME()` 或 `CURRENT_TIME` |
| 当前日期时间 | `DATETIME('now')` | `NOW()` 或 `CURRENT_TIMESTAMP` |
| Unix 时间戳 | `STRFTIME('%s', 'now')` | `UNIX_TIMESTAMP()` |

**日期计算：**

**SQLite：**
```sql
-- 日期加减
SELECT DATE('2024-01-15', '+1 month');
SELECT DATE('2024-01-15', '-7 days');

-- 计算日期差（天数）
SELECT JULIANDAY('2024-02-15') - JULIANDAY('2024-01-15');
```

**MySQL 8.0：**
```sql
-- 日期加减
SELECT DATE_ADD('2024-01-15', INTERVAL 1 MONTH);
SELECT DATE_SUB('2024-01-15', INTERVAL 7 DAY);

-- 计算日期差
SELECT DATEDIFF('2024-02-15', '2024-01-15');  -- 天数
SELECT TIMESTAMPDIFF(MONTH, '2024-01-15', '2024-02-15');  -- 月数

-- MySQL 8.0 新增：日期格式转换
SELECT DATE_FORMAT('2024-01-15', '%Y年%m月%d日');
-- 输出: 2024年01月15日
```

#### 2.5.4 条件表达式

**IFNULL / COALESCE：**

| SQLite | MySQL 8.0 | 说明 |
| ------ | --------- | ---- |
| `IFNULL(col, 'default')` | `IFNULL(col, 'default')` | 两者都支持 |
| `COALESCE(col, 'default')` | `COALESCE(col, 'default')` | 两者都支持，支持多个参数 |
| 不支持 | `IF(col IS NULL, 'default', col)` | MySQL 特有的 IF 函数 |

**CASE 表达式（两者都支持）：**
```sql
-- 简单 CASE
CASE grade
    WHEN 'A' THEN '优秀'
    WHEN 'B' THEN '良好'
    ELSE '其他'
END

-- 搜索 CASE
CASE
    WHEN score >= 90 THEN '优秀'
    WHEN score >= 80 THEN '良好'
    ELSE '及格'
END
```

#### 2.5.5 分页查询

**两者基本相同：**
```sql
-- SQLite 和 MySQL 8.0 都支持
SELECT * FROM student LIMIT 10 OFFSET 20;

-- MySQL 8.0 还支持简写（不推荐，可读性差）
SELECT * FROM student LIMIT 20, 10;
```

#### 2.5.6 窗口函数（Window Functions）

**MySQL 8.0 新增特性，SQLite 部分支持：**

| 窗口函数 | MySQL 8.0 | SQLite 3.25+ | 说明 |
| ------- | --------- | ------------ | ---- |
| `ROW_NUMBER()` | ✅ 支持 | ✅ 支持 | 行号 |
| `RANK()` | ✅ 支持 | ✅ 支持 | 排名（有间隙） |
| `DENSE_RANK()` | ✅ 支持 | ✅ 支持 | 密集排名（无间隙） |
| `NTILE(n)` | ✅ 支持 | ✅ 支持 | 分桶 |
| `LAG()` | ✅ 支持 | ✅ 支持 | 前一行 |
| `LEAD()` | ✅ 支持 | ✅ 支持 | 后一行 |
| `FIRST_VALUE()` | ✅ 支持 | ✅ 支持 | 第一个值 |
| `LAST_VALUE()` | ✅ 支持 | ✅ 支持 | 最后一个值 |
| `NTH_VALUE()` | ✅ 支持 | ✅ 支持 | 第 N 个值 |
| 聚合函数 OVER() | ✅ 支持 | ✅ 支持 | 聚合窗口 |

**示例 - 查询每个学生的成绩排名：**

```sql
-- MySQL 8.0 和 SQLite 3.25+ 都支持
SELECT 
    student_id,
    subject,
    score,
    RANK() OVER (PARTITION BY subject ORDER BY score DESC) as rank_in_subject
FROM score;
```

#### 2.5.7 通用表表达式（CTE）

**MySQL 8.0 新增特性，SQLite 3.8.3+ 支持：**

**基本 CTE：**
```sql
-- MySQL 8.0 和 SQLite 都支持
WITH high_scores AS (
    SELECT student_id, subject, score 
    FROM score 
    WHERE score >= 90
)
SELECT s.name, hs.subject, hs.score
FROM student s
JOIN high_scores hs ON s.id = hs.student_id;
```

**递归 CTE（MySQL 8.0 和 SQLite 3.8.3+ 都支持）：**
```sql
-- 生成数字序列 1-10
WITH RECURSIVE numbers(n) AS (
    SELECT 1
    UNION ALL
    SELECT n + 1 FROM numbers WHERE n < 10
)
SELECT * FROM numbers;

-- 查询树形结构（如部门层级）
WITH RECURSIVE dept_path AS (
    SELECT id, name, parent_id, CAST(name AS CHAR(200)) AS path
    FROM department
    WHERE parent_id IS NULL
    UNION ALL
    SELECT d.id, d.name, d.parent_id, CONCAT(dp.path, ' -> ', d.name)
    FROM department d
    JOIN dept_path dp ON d.parent_id = dp.id
)
SELECT * FROM dept_path;
```

#### 2.5.8 JSON 功能对比

**MySQL 8.0 原生支持 JSON 类型，SQLite 需要 JSON1 扩展：**

| 功能 | MySQL 8.0 | SQLite (JSON1 扩展) |
| ---- | --------- | ------------------- |
| 创建 JSON 列 | ✅ `JSON` 类型 | ❌ 用 `TEXT` 存储 |
| JSON 路径查询 | `JSON_EXTRACT()` 或 `->` 操作符 | `json_extract()` |
| 路径简写 | `col->'$.key'` | 不支持 |
| 路径+解析 | `col->>'$.key'` | 不支持 |
| JSON 验证 | `JSON_VALID()` | `json_valid()` |
| JSON 数组 | `JSON_ARRAY()` | `json_array()` |
| JSON 对象 | `JSON_OBJECT()` | `json_object()` |
| JSON 合并 | `JSON_MERGE_PATCH()` | `json_patch()` |
| JSON 集合操作 | `JSON_CONTAINS()` | `json_contains()` |
| JSON 路径搜索 | `JSON_SEARCH()` | 不支持 |
| JSON 深度 | `JSON_DEPTH()` | 不支持 |
| JSON 长度 | `JSON_LENGTH()` | `json_array_length()` |
| JSON 键列表 | `JSON_KEYS()` | 不支持 |
| JSON 类型 | `JSON_TYPE()` | `json_type()` |
| JSON 格式化 | `JSON_PRETTY()` | 不支持 |
| JSON 移除 | `JSON_REMOVE()` | 不支持 |
| JSON 替换 | `JSON_REPLACE()` | 不支持 |
| JSON 设置 | `JSON_SET()` | 不支持 |
| JSON 追加 | `JSON_ARRAY_APPEND()` | 不支持 |
| JSON 插入 | `JSON_INSERT()` | 不支持 |

**MySQL 8.0 JSON 示例：**
```sql
-- 创建带 JSON 列的表
CREATE TABLE user_profile (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT,
    preferences JSON,  -- 原生 JSON 类型
    INDEX idx_prefs ((preferences->'$.theme'))  -- JSON 函数索引
);

-- 插入 JSON 数据
INSERT INTO user_profile (user_id, preferences)
VALUES (1, '{"theme": "dark", "notifications": {"email": true, "push": false}}');

-- 查询 JSON 字段
SELECT 
    preferences->'$.theme' as theme,
    preferences->>'$.notifications.email' as email_notif
FROM user_profile;

-- 更新 JSON 字段（部分更新）
UPDATE user_profile 
SET preferences = JSON_SET(preferences, '$.theme', 'light')
WHERE user_id = 1;

-- 检查 JSON 中是否包含某个值
SELECT * FROM user_profile 
WHERE JSON_CONTAINS(preferences, 'true', '$.notifications.email');
```

**SQLite JSON1 扩展示例：**
```sql
-- SQLite 用 TEXT 存储 JSON
CREATE TABLE user_profile (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER,
    preferences TEXT  -- 用 TEXT 存储 JSON
);

-- 插入 JSON 数据
INSERT INTO user_profile (user_id, preferences)
VALUES (1, '{"theme": "dark", "notifications": {"email": true}}');

-- 查询 JSON 字段（需要 JSON1 扩展）
SELECT 
    json_extract(preferences, '$.theme') as theme
FROM user_profile;

-- 验证 JSON 有效性
SELECT json_valid(preferences) FROM user_profile;
```

### 2.6 其他重要区别

| 特性 | SQLite | MySQL 8.0 |
| ---- | ------ | --------- |
| **用户权限系统** | 无内置权限系统，依赖文件系统权限 | 完整的 RBAC 权限模型，支持角色管理 |
| **存储过程** | 不支持 | 支持存储过程、函数、触发器、事件 |
| **视图** | 仅支持只读视图 | 支持可更新视图、物化视图（8.0+） |
| **全文搜索** | 支持 FTS3/FTS4/FTS5 虚拟表 | 内置全文搜索（InnoDB 和 MyISAM） |
| **分区表** | 不支持 | 支持 RANGE、LIST、HASH、KEY 分区 |
| **外键约束** | 默认不强制，需显式启用 | 默认强制（InnoDB） |
| **触发器** | 支持，但功能有限 | 支持 BEFORE/AFTER 触发器，功能强大 |
| **事件调度器** | 不支持 | 支持 Event Scheduler，定时执行任务 |
| **备份恢复** | 复制数据库文件即可 | 支持 mysqldump、XtraBackup、binlog 等 |
| **性能监控** | 有限 | 丰富的 Performance Schema、sys schema |
| **审计日志** | 不支持 | 支持（企业版或插件） |
| **SSL/TLS 连接** | 不支持 | 支持 |
| **连接池** | 无（嵌入式） | 支持，配合应用层连接池 |

### 2.7 集群与高可用对比

#### 2.7.1 MySQL 8.0 集群方案

MySQL 8.0 提供多种集群和高可用方案：

##### 方案一：MySQL InnoDB Cluster（官方推荐）

**架构组成：**
- **MySQL Group Replication**：提供组内数据同步和故障转移
- **MySQL Router**：提供连接路由和负载均衡
- **MySQL Shell**：提供管理接口

**特性：**
- ✅ 自动故障检测和转移
- ✅ 强一致性（可配置最终一致性）
- ✅ 自动成员管理
- ✅ 内置冲突检测和解决
- ✅ 单主模式（默认）或多主模式
- ✅ 与 MySQL Router 集成，应用透明

**架构图：**
```
                    ┌──────────────┐
                    │   应用程序     │
                    └──────┬───────┘
                           │
                    ┌──────▼───────┐
                    │ MySQL Router │ ← 连接路由、负载均衡
                    └──────┬───────┘
                           │
            ┌──────────────┼──────────────┐
            │              │              │
    ┌───────▼──────┐ ┌────▼─────┐ ┌─────▼──────┐
    │   Primary     │ │ Secondary │ │  Secondary │
    │  (读写)       │ │  (只读)   │ │  (只读)    │
    │ Group Replication │ │ Group Replication │ │ Group Replication │
    └───────┬──────┘ └────┬─────┘ └─────┬──────┘
            │              │              │
            └──────────────┼──────────────┘
                           │
                    ┌──────▼───────┐
                    │  内部组通信    │ ← Paxos 协议
                    └──────────────┘
```

**配置示例：**
```sql
-- 使用 MySQL Shell 配置 InnoDB Cluster
-- 1. 连接到实例
\connect root@mysql1:3306

-- 2. 检查实例配置
dba.checkInstanceConfiguration('root@mysql1:3306')

-- 3. 配置实例（如果需要）
dba.configureInstance('root@mysql1:3306')

-- 4. 创建集群
var cluster = dba.createCluster('myCluster')

-- 5. 添加实例
cluster.addInstance('root@mysql2:3306')
cluster.addInstance('root@mysql3:3306')

-- 6. 查看集群状态
cluster.status()
```

##### 方案二：MySQL Group Replication（底层技术）

InnoDB Cluster 的核心组件，提供：

| 特性 | 说明 |
| ---- | ---- |
| 复制协议 | 基于 Paxos 的分布式一致性协议 |
| 成员管理 | 自动检测和加入/移除成员 |
| 故障检测 | 自动检测失败节点并排除 |
| 故障转移 | 单主模式下自动选举新主节点 |
| 冲突检测 | 多主模式下自动检测和解决冲突 |

##### 方案三：MySQL NDB Cluster（内存集群）

**适用场景**：极高可用性、极高吞吐量、实时数据访问

**架构组成：**
- **Management Nodes (ndb_mgmd)**：管理集群配置
- **Data Nodes (ndbd)**：存储数据，可选磁盘持久化
- **SQL Nodes (mysqld)**：提供 SQL 接口

**特性：**
- ✅ 99.999% 可用性
- ✅ 自动分片（Sharding）
- ✅ 同步复制
- ✅ 内存存储为主，可选磁盘持久化
- ✅ 在线 Schema 变更

##### 方案四：主从复制（Master-Slave Replication）

**传统方案，适合读多写少场景：**

```
                    ┌──────────────┐
                    │   应用程序     │
                    └──────┬───────┘
                           │
              ┌────────────┼────────────┐
              │            │            │
         写操作         读操作        读操作
              │            │            │
    ┌─────────▼────────┐  │            │
    │     Master        │  │            │
    │   (主库-读写)     │  │            │
    └─────────┬────────┘  │            │
              │            │            │
         Binlog 复制       │            │
              │            │            │
    ┌─────────▼────────┐ ┌▼────────┐ ┌▼────────┐
    │     Slave 1       │ │ Slave 2  │ │ Slave 3  │
    │   (从库-只读)     │ │ (从库-只读)│ │ (从库-只读)│
    └──────────────────┘ └─────────┘ └─────────┘
```

**MySQL 8.0 主从复制新特性：**
- ✅ GTID 复制（全局事务标识）
- ✅ 并行复制（基于逻辑时钟）
- ✅ 半同步复制增强
- ✅ 自动故障转移（配合 MHA 或 Orchestrator）

#### 2.7.2 SQLite 无集群能力

| 特性 | SQLite | 说明 |
| ---- | ------ | ---- |
| **集群支持** | ❌ 完全不支持 | 单文件数据库，设计目标是嵌入式 |
| **数据复制** | ❌ 无内置机制 | 需要应用层实现文件复制 |
| **故障转移** | ❌ 不支持 | 单点故障 |
| **读写分离** | ❌ 不支持 | 只能单进程写入 |
| **水平扩展** | ❌ 不支持 | 无法通过添加节点扩展 |

**SQLite 可能的"替代方案"（非真正集群）：**

1. **文件复制**：定期复制数据库文件到其他服务器
   - 问题：无法保证数据一致性，可能丢失数据

2. **备份恢复**：使用 SQLite 的 `.backup` 命令
   - 问题：同样是定期操作，不是实时同步

3. **应用层分片**：在应用层将数据分散到多个 SQLite 文件
   - 问题：跨库查询复杂，事务无法跨库

### 2.8 适用场景对比总结

| 场景 | 推荐选择 | 原因 |
| ---- | -------- | ---- |
| **嵌入式设备** | SQLite | 零配置、轻量级、单文件 |
| **移动应用** | SQLite | Android/iOS 原生支持 |
| **桌面应用** | SQLite | 无需服务器，易于打包 |
| **原型开发** | SQLite | 快速启动、零配置 |
| **单元测试** | SQLite | 内存模式、快速启动 |
| **小型网站（低并发）** | SQLite 或 MySQL | 都可以，SQLite 运维简单 |
| **中大型网站** | MySQL 8.0 | 需要高并发、连接池 |
| **高可用要求** | MySQL 8.0 + InnoDB Cluster | 自动故障转移 |
| **读多写少** | MySQL 8.0 + 主从复制 | 读写分离扩展读能力 |
| **极高可用性** | MySQL 8.0 + NDB Cluster | 99.999% 可用性 |
| **地理分布式** | MySQL 8.0 + 多种方案 | 支持跨区域部署 |

---

## 2.9 重要讨论：MySQL 有嵌入式版本吗？

### 2.9.1 直接回答

**MySQL 没有像 SQLite 那样真正的嵌入式版本。**

| 特性 | SQLite | MySQL |
|------|--------|-------|
| **架构** | 库文件，直接链接到应用进程 | 独立服务器进程，客户端-服务器架构 |
| **启动方式** | 应用启动时自动加载 | 需要独立启动服务器进程 |
| **配置** | 零配置 | 需要配置用户名、密码、端口等 |
| **内存占用** | 极小（几MB） | 较大（几十到几百MB） |

### 2.9.2 为什么这是个重要问题

您提出了一个非常关键的观察：

> **"因为 MySQL 和 SQLite 的 SQL 语法有区别，所以无法做到开发环境用 SQLite，生产/测试环境用 MySQL"**

这是完全正确的！让我详细说明其中的陷阱：

#### 陷阱一：SQL 语法差异（实际案例）

```sql
-- ============================================
-- 场景1：自增主键
-- ============================================

-- SQLite 写法
CREATE TABLE student (
    id INTEGER PRIMARY KEY AUTOINCREMENT,  -- 必须是 INTEGER PRIMARY KEY
    name TEXT NOT NULL
);

-- MySQL 写法  
CREATE TABLE student (
    id INT PRIMARY KEY AUTO_INCREMENT,      -- INT/BIGINT 都可以
    name VARCHAR(100) NOT NULL
);

-- 问题：如果您用 SQLite 测试，然后切换到 MySQL
-- 结果：CREATE TABLE 语句会报错！


-- ============================================
-- 场景2：字符串拼接
-- ============================================

-- SQLite 写法
SELECT 'Hello' || ' ' || 'World';  -- 使用 || 操作符

-- MySQL 写法
SELECT CONCAT('Hello', ' ', 'World');  -- 使用 CONCAT 函数
-- 或
SELECT 'Hello' ' ' 'World';  -- 空格分隔

-- 问题：同样的代码，在不同数据库行为不同
-- SQLite: 返回 'Hello World'
-- MySQL: 返回 'Hello'（|| 是逻辑或操作符！）


-- ============================================
-- 场景3：日期函数
-- ============================================

-- SQLite 写法
SELECT DATE('now');           -- 当前日期
SELECT DATETIME('now');       -- 当前日期时间
SELECT STRFTIME('%s', 'now'); -- Unix 时间戳

-- MySQL 写法
SELECT CURDATE();              -- 当前日期
SELECT NOW();                  -- 当前日期时间
SELECT UNIX_TIMESTAMP();       -- Unix 时间戳

-- 问题：日期函数完全不同


-- ============================================
-- 场景4：分页查询
-- ============================================

-- 两者都支持
SELECT * FROM table LIMIT 10 OFFSET 20;

-- 但 MySQL 还有这种简写（不推荐，但很多人用）
SELECT * FROM table LIMIT 20, 10;  -- offset 20, size 10

-- 问题：SQLite 不支持 LIMIT offset, size 这种语法
```

#### 陷阱二：事务和锁行为差异

| 场景 | SQLite | MySQL (InnoDB) | 实际影响 |
|------|--------|----------------|----------|
| **写操作** | 锁定整个数据库 | 仅锁定相关行 | 开发环境并发测试通过，生产环境死锁 |
| **隔离级别** | 仅支持 READ UNCOMMITTED 和 SERIALIZABLE | 支持全部 4 种 | 事务行为不一致 |
| **死锁** | 不会发生 | 可能发生 | 开发环境无法测试死锁场景 |
| **MVCC** | 不支持 | 完全支持 | 读写并发行为不同 |

**真实案例**：
> 某团队在开发环境用 SQLite 测试了一个批量更新功能，测试完全通过。部署到生产环境（MySQL）后，高并发下频繁出现死锁，导致服务不可用。
> 
> 原因：SQLite 的写操作锁定整个数据库，不会产生死锁；MySQL 的行级锁在特定顺序下会产生死锁。

#### 陷阱三：数据类型差异

```sql
-- ============================================
-- 场景：字符串长度限制
-- ============================================

CREATE TABLE test (
    name VARCHAR(10)  -- 看似限制 10 个字符
);

-- SQLite 行为：VARCHAR 只是建议，实际可以存储任意长度
INSERT INTO test VALUES ('This is a very very long name that exceeds 10 characters');
-- 结果：成功插入，完整存储

-- MySQL 行为：严格限制，超长会报错或截断
INSERT INTO test VALUES ('This is a very very long name that exceeds 10 characters');
-- 结果：ERROR 1406 (22001): Data too long for column 'name'

-- 问题：开发环境测试通过，生产环境报错！
```

#### 陷阱四：外键约束

```sql
-- ============================================
-- 场景：外键约束
-- ============================================

-- SQLite 默认行为：不强制外键约束
INSERT INTO score (student_id, subject, score) 
VALUES (9999, '数学', 90);  -- student_id 9999 不存在
-- 结果：成功插入！（因为外键约束默认不启用）

-- 需要显式启用
PRAGMA foreign_keys = ON;

-- MySQL (InnoDB) 默认行为：强制外键约束
INSERT INTO score (student_id, subject, score) 
VALUES (9999, '数学', 90);
-- 结果：ERROR 1452 (23000): Cannot add or update a child row

-- 问题：开发环境插入了"脏数据"，生产环境直接报错！
```

#### 陷阱五：隐式类型转换

```sql
-- ============================================
-- 场景：隐式类型转换
-- ============================================

-- SQLite：动态类型，转换宽松
SELECT '123' + 456;  -- 结果：579（字符串自动转数字）
SELECT 123 + 'abc';   -- 结果：123（无法转换的视为 0）

-- MySQL：转换规则不同
SELECT '123' + 456;   -- 结果：579（同上）
SELECT 123 + 'abc';    -- 结果：123，但有警告
-- Warning (Code 1292): Truncated incorrect DOUBLE value: 'abc'

-- 问题：看似相同的行为，实际有差异
-- 在复杂查询中，可能导致不同的执行计划和性能
```

### 2.9.3 MySQL 的"嵌入式"替代方案

虽然没有真正的嵌入式 MySQL，但有以下替代方案：

#### 方案一：MariaDB4j（推荐用于开发环境）

**MariaDB** 是 MySQL 的一个分支，由 MySQL 创始人创建，完全兼容 MySQL。**MariaDB4j** 是一个可以在 JVM 进程中启动 MariaDB 服务器的库。

**优点**：
- ✅ 真正的 MySQL 兼容语法
- ✅ 数据可以持久化或内存模式
- ✅ 启动速度较快（约 2 秒）
- ✅ 支持多种平台

**缺点**：
- ❌ 仍然是独立的服务器进程（虽然在 JVM 内启动）
- ❌ 需要下载 MariaDB 二进制文件（约 100MB）
- ❌ 生产环境不推荐使用

**使用示例**：

```xml
<!-- pom.xml -->
<dependency>
    <groupId>ch.vorburger.mariaDB4j</groupId>
    <artifactId>mariaDB4j-springboot</artifactId>
    <version>2.6.0</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.mariadb.jdbc</groupId>
    <artifactId>mariadb-java-client</artifactId>
    <version>3.3.1</version>
</dependency>
```

```java
// 配置类 - 仅在开发环境启用
package com.example.config;

import ch.vorburger.mariadb4j.DB;
import ch.vorburger.mariadb4j.DBConfigurationBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "app.database.embedded", havingValue = "true")
public class EmbeddedMariaDBConfig {
    
    @Bean(initMethod = "start", destroyMethod = "stop")
    public DB mariaDB() throws Exception {
        DBConfigurationBuilder config = DBConfigurationBuilder.newBuilder()
            .setPort(3307)  // 使用非标准端口，避免与本地 MySQL 冲突
            .setDataDir("target/mariadb_data")  // 数据目录，构建时清理
            .setSocketTimeout(30000);
        
        return DB.newEmbeddedDB(config.build());
    }
}
```

```properties
# application-dev.properties
app.database.embedded=true

# MariaDB4j 连接配置
spring.datasource.url=jdbc:mariadb://localhost:3307/test
spring.datasource.username=root
spring.datasource.password=
spring.datasource.driver-class-name=org.mariadb.jdbc.Driver

# JPA 配置
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MariaDBDialect
```

#### 方案二：Testcontainers（推荐用于测试环境）

**Testcontainers** 是一个 Java 库，可以在 Docker 容器中运行数据库。

**优点**：
- ✅ 真正的 MySQL 服务器（可以指定任意版本）
- ✅ 完全与生产环境一致
- ✅ 支持多数据库（MySQL、PostgreSQL、MongoDB 等）
- ✅ 测试完成后自动清理

**缺点**：
- ❌ 需要 Docker 环境
- ❌ 启动较慢（约 10-30 秒，取决于网络）
- ❌ 开发环境每次都要启动容器

**使用示例**：

```xml
<!-- pom.xml -->
<properties>
    <testcontainers.version>1.19.3</testcontainers.version>
</properties>

<dependencies>
    <!-- Testcontainers JUnit Jupiter 集成 -->
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>${testcontainers.version}</version>
        <scope>test</scope>
    </dependency>
    
    <!-- MySQL 模块 -->
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>mysql</artifactId>
        <version>${testcontainers.version}</version>
        <scope>test</scope>
    </dependency>
    
    <!-- MySQL 驱动 -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

```java
// 测试类
package com.example.repository;

import com.example.entity.Student;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers  // 启用 Testcontainers
class StudentRepositoryTest {
    
    // 定义 MySQL 容器 - 使用 MySQL 8.0
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>(
        DockerImageName.parse("mysql:8.0.33")
    )
        .withDatabaseName("testdb")           // 数据库名
        .withUsername("testuser")             // 用户名
        .withPassword("testpass")             // 密码
        .withInitScript("schema-mysql.sql")   // 初始化脚本
        .withCommand(                          // MySQL 启动参数
            "--default-authentication-plugin=mysql_native_password",
            "--character-set-server=utf8mb4",
            "--collation-server=utf8mb4_unicode_ci"
        );
    
    // 动态注入数据源配置
    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", mysql::getDriverClassName);
    }
    
    @Autowired
    private StudentRepository studentRepository;
    
    @Test
    void testSaveAndFindById() {
        // 准备数据
        Student student = new Student();
        student.setName("张三");
        student.setAge(20);
        student.setGender("男");
        student.setMajor("计算机科学");
        
        // 执行保存
        Student saved = studentRepository.save(student);
        
        // 验证
        assertThat(saved.getId()).isNotNull();
        
        Optional<Student> found = studentRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("张三");
    }
    
    @Test
    void testFindByMajor() {
        // 准备数据
        Student s1 = new Student();
        s1.setName("张三");
        s1.setMajor("计算机科学");
        studentRepository.save(s1);
        
        Student s2 = new Student();
        s2.setName("李四");
        s2.setMajor("计算机科学");
        studentRepository.save(s2);
        
        Student s3 = new Student();
        s3.setName("王五");
        s3.setMajor("数学");
        studentRepository.save(s3);
        
        // 执行查询
        var csStudents = studentRepository.findByMajor("计算机科学");
        
        // 验证
        assertThat(csStudents).hasSize(2);
    }
}
```

**高级用法：使用 ApplicationContextInitializer**

如果需要在所有测试中共享同一个 MySQL 容器：

```java
package com.example;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.lifecycle.Startables;

public class MySQLContainerInitializer implements 
    ApplicationContextInitializer<ConfigurableApplicationContext> {
    
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0.33")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");
    
    static {
        // 启动容器（只启动一次）
        Startables.deepStart(mysql).join();
    }
    
    @Override
    public void initialize(ConfigurableApplicationContext context) {
        TestPropertyValues.of(
            "spring.datasource.url=" + mysql.getJdbcUrl(),
            "spring.datasource.username=" + mysql.getUsername(),
            "spring.datasource.password=" + mysql.getPassword()
        ).applyTo(context.getEnvironment());
    }
}
```

```java
// 在测试类中使用
@SpringBootTest
@ContextConfiguration(initializers = MySQLContainerInitializer.class)
class MyTest {
    // 测试代码...
}
```

#### 方案三：H2 数据库（MySQL 兼容模式）

**H2** 是一个纯 Java 嵌入式数据库，可以配置为 MySQL 兼容模式。

**优点**：
- ✅ 真正的嵌入式，零配置
- ✅ 启动极快（毫秒级）
- ✅ 支持内存模式和文件模式
- ✅ 提供 Web 控制台

**缺点**：
- ❌ 不是真正的 MySQL，仍有语法差异
- ❌ 某些 MySQL 特性不支持（如窗口函数的某些用法）
- ❌ 生产环境绝对不推荐

**使用示例**：

```xml
<!-- pom.xml -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>
```

```properties
# application-dev.properties

# H2 内存模式，MySQL 兼容
spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# H2 控制台（访问 http://localhost:8080/h2-console）
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
spring.h2.console.settings.web-allow-others=false

# JPA 配置
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect
```

**注意**：H2 的 MySQL 兼容模式并不完美。以下是一些已知差异：

| 功能 | MySQL | H2 (MySQL 模式) |
|------|-------|-----------------|
| `AUTO_INCREMENT` | ✅ 支持 | ✅ 支持（但内部实现不同） |
| `LIMIT offset, size` | ✅ 支持 | ❌ 不支持 |
| `ON DUPLICATE KEY UPDATE` | ✅ 支持 | ❌ 不支持 |
| `INSERT IGNORE` | ✅ 支持 | ❌ 不支持 |
| 窗口函数 | ✅ 完整支持 | ⚠️ 部分支持 |
| `DATE_ADD()` | ✅ 支持 | ⚠️ 需要使用 `DATEADD()` |
| `UNIX_TIMESTAMP()` | ✅ 支持 | ❌ 不支持 |

#### 方案四：本地 MySQL 服务器（最推荐用于开发）

在每个开发者的机器上直接安装 MySQL。

**优点**：
- ✅ 完全与生产环境一致
- ✅ 没有任何兼容性问题
- ✅ 可以使用真实的 MySQL 工具（MySQL Workbench、命令行等）
- ✅ 性能最好

**缺点**：
- ❌ 需要安装和配置
- ❌ 占用一定资源
- ❌ 每个开发者都要维护自己的实例

**安装方式**：

```bash
# macOS (使用 Homebrew)
brew install mysql
brew services start mysql

# 或使用 Docker（推荐）
docker run -d \
  --name mysql-dev \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=mydb_dev \
  -e MYSQL_USER=dev \
  -e MYSQL_PASSWORD=devpass \
  -p 3306:3306 \
  mysql:8.0.33 \
  --character-set-server=utf8mb4 \
  --collation-server=utf8mb4_unicode_ci
```

```properties
# application-dev.properties
spring.datasource.url=jdbc:mysql://localhost:3306/mydb_dev?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&characterEncoding=utf8mb4
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.username=dev
spring.datasource.password=devpass

# JPA 配置
spring.jpa.hibernate.ddl-auto=validate  # 不自动创建，由 Flyway 管理
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect

# Flyway 数据库迁移
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.baseline-on-migrate=true
```

### 2.9.4 为什么"开发环境用 SQLite、生产环境用 MySQL"是个坏主意

让我总结一下这种做法的风险：

#### 风险评估表

| 风险类别 | 严重程度 | 说明 |
|----------|----------|------|
| **SQL 语法错误** | 🔴 高 | CREATE TABLE、日期函数、字符串拼接等语法不同 |
| **事务行为差异** | 🔴 高 | 锁机制、隔离级别、死锁场景无法在开发环境测试 |
| **数据类型问题** | 🟡 中 | 类型约束、隐式转换行为不同 |
| **外键约束** | 🔴 高 | SQLite 默认不强制外键，可能产生脏数据 |
| **性能差异** | 🟡 中 | 执行计划、索引使用方式不同 |
| **功能缺失** | 🟡 中 | MySQL 特有功能（窗口函数、CTE 高级用法）无法在 SQLite 测试 |

#### 真实案例：生产事故

> **案例一**：某电商平台
> 
> **场景**：开发环境用 SQLite，测试和生产用 MySQL。
> 
> **问题**：订单系统中有一个批量更新库存的功能。开发环境测试完全通过，并发测试也正常。部署到生产环境后，高并发下频繁出现死锁，导致无法下单。
> 
> **原因**：SQLite 的写操作锁定整个数据库，不会产生死锁；MySQL 的行级锁在更新顺序不对时会产生死锁。
> 
> **损失**：故障持续 2 小时，影响订单数千笔，紧急回滚后才恢复。

> **案例二**：某社交应用
> 
> **场景**：开发环境用 SQLite，生产用 MySQL。
> 
> **问题**：用户注册功能中，用户名限制为 20 个字符。开发环境测试时，输入 100 个字符也能注册成功。部署到生产环境后，用户输入长用户名时直接报错。
> 
> **原因**：SQLite 的 VARCHAR(n) 只是建议，实际不限制长度；MySQL 严格限制，超长会报错。
> 
> **损失**：用户体验差，需要紧急修复并回滚异常数据。

### 2.9.5 最佳实践建议

#### 核心原则：环境一致性

> **"开发、测试、生产环境应该尽可能一致。"**

这是软件工程的基本原则之一，数据库层面尤其重要。

#### 推荐方案一：所有环境都用 MySQL（强烈推荐）

```
┌─────────────────────────────────────────────────────────────┐
│                        环境架构                               │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  开发环境：本地 MySQL 8.0                                    │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  Docker 容器 或 直接安装                              │   │
│  │  - 版本：mysql:8.0.33                                │   │
│  │  - 字符集：utf8mb4                                   │   │
│  │  - 与生产环境完全一致                                 │   │
│  └─────────────────────────────────────────────────────┘   │
│                           ↓                                 │
│  测试环境：Testcontainers + MySQL 8.0                      │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  自动化测试时启动 Docker 容器                          │   │
│  │  - 版本与生产环境完全一致                             │   │
│  │  - 测试完成后自动销毁                                 │   │
│  └─────────────────────────────────────────────────────┘   │
│                           ↓                                 │
│  生产环境：MySQL 8.0 + InnoDB Cluster                      │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  高可用集群                                            │   │
│  │  - 自动故障转移                                        │   │
│  │  - 读写分离                                            │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

**配置示例**：

```
src/main/resources/
├── application.properties           # 公共配置
├── application-dev.properties       # 开发环境：本地 MySQL
├── application-test.properties      # 测试环境：Testcontainers
└── application-prod.properties      # 生产环境：InnoDB Cluster
```

**application-dev.properties**：
```properties
# 开发环境：本地 MySQL 8.0
spring.datasource.url=jdbc:mysql://localhost:3306/mydb_dev?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&characterEncoding=utf8mb4
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.username=dev_user
spring.datasource.password=${DEV_DB_PASSWORD:devpass}

# JPA 配置
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect
spring.jpa.properties.hibernate.format_sql=true

# 连接池
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5

# Flyway 数据库迁移
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.baseline-on-migrate=true
```

**application-test.properties**：
```properties
# 测试环境：Testcontainers 会动态覆盖这些配置
# 详见测试类中的 @DynamicPropertySource
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=true
```

**application-prod.properties**：
```properties
# 生产环境：MySQL 8.0 + InnoDB Cluster
spring.datasource.url=jdbc:mysql://mysql-router:6446/mydb_prod?useSSL=true&serverTimezone=Asia/Shanghai&characterEncoding=utf8mb4
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.username=app_user
spring.datasource.password=${DB_PASSWORD}  # 从环境变量读取

# JPA 配置
spring.jpa.hibernate.ddl-auto=none
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect

# 连接池优化
spring.datasource.hikari.maximum-pool-size=50
spring.datasource.hikari.minimum-idle=10
spring.datasource.hikari.idle-timeout=300000
spring.datasource.hikari.max-lifetime=1800000
spring.datasource.hikari.connection-timeout=30000

# MySQL 特定优化
spring.datasource.hikari.data-source-properties.cachePrepStmts=true
spring.datasource.hikari.data-source-properties.prepStmtCacheSize=250
spring.datasource.hikari.data-source-properties.prepStmtCacheSqlLimit=2048
spring.datasource.hikari.data-source-properties.useServerPrepStmts=true
spring.datasource.hikari.data-source-properties.rewriteBatchedStatements=true

# Flyway
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
```

#### 推荐方案二：使用 ORM 抽象层（如果必须支持多数据库）

如果您确实需要支持多种数据库（比如产品需要支持多种数据库部署），使用成熟的 ORM 框架可以减少差异。

**Spring Data JPA 示例**：

```java
package com.example.repository;

import com.example.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    
    // 方法名派生查询：JPA 自动适配不同数据库
    List<Student> findByMajor(String major);
    List<Student> findByAgeGreaterThan(Integer age);
    List<Student> findByGenderAndMajor(String gender, String major);
    
    // 使用 JPQL 而不是原生 SQL
    @Query("SELECT s FROM Student s WHERE s.age BETWEEN :minAge AND :maxAge")
    List<Student> findByAgeRange(@Param("minAge") Integer minAge, 
                                   @Param("maxAge") Integer maxAge);
    
    // 排序
    @Query("SELECT s FROM Student s ORDER BY s.name ASC")
    List<Student> findAllSortedByName();
}
```

**MyBatis 多数据库支持示例**：

```xml
<!-- 使用 databaseIdProvider -->
<databaseIdProvider type="DB_VENDOR">
    <property name="SQL Server" value="sqlserver"/>
    <property name="DB2" value="db2"/>
    <property name="Oracle" value="oracle"/>
    <property name="MySQL" value="mysql"/>
    <property name="H2" value="h2"/>
    <property name="PostgreSQL" value="postgresql"/>
</databaseIdProvider>

<!-- MySQL 特定的 SQL -->
<select id="findByPage" resultMap="BaseResultMap" databaseId="mysql">
    SELECT * FROM student 
    ORDER BY id 
    LIMIT #{offset}, #{pageSize}
</select>

<!-- SQLite/H2 特定的 SQL -->
<select id="findByPage" resultMap="BaseResultMap" databaseId="h2">
    SELECT * FROM student 
    ORDER BY id 
    LIMIT #{pageSize} OFFSET #{offset}
</select>
```

#### 推荐方案三：使用数据库迁移工具

无论使用哪种数据库，都应该使用迁移工具管理 schema 变更。

**Flyway 示例**：

```sql
-- V1__Create_Student_Table.sql
-- 使用标准 SQL，尽量避免数据库特定语法

CREATE TABLE student (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    age INT,
    gender VARCHAR(10),
    major VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_student_major ON student(major);
```

```sql
-- V2__Create_Score_Table.sql
CREATE TABLE score (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    subject VARCHAR(50) NOT NULL,
    score DOUBLE,
    exam_date DATE,
    FOREIGN KEY (student_id) REFERENCES student(id)
);

CREATE INDEX idx_score_student ON score(student_id);
CREATE INDEX idx_score_subject ON score(subject);
```

```properties
# Flyway 配置
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.baseline-on-migrate=true
spring.flyway.validate-on-migrate=true
```

### 2.9.6 方案对比总结

| 方案 | 开发环境 | 测试环境 | 生产环境 | 一致性 | 推荐度 |
|------|----------|----------|----------|--------|--------|
| **方案一（推荐）** | 本地 MySQL | Testcontainers | InnoDB Cluster | ⭐⭐⭐⭐⭐ | 强烈推荐 |
| **方案二** | MariaDB4j | Testcontainers | InnoDB Cluster | ⭐⭐⭐ | 可接受 |
| **方案三** | H2 (MySQL 模式) | H2 | MySQL | ⭐⭐ | 不推荐 |
| **方案四（传统）** | SQLite | SQLite | MySQL | ⭐ | 强烈不推荐 |

### 2.9.7 最终建议

> **问题**：MySQL 有没有可以像 SQLite 一样运行在 Spring Boot 进程中的方式？
> 
> **答案**：**没有真正的嵌入式 MySQL**，但有替代方案：
> - MariaDB4j 可以在 JVM 内启动 MariaDB（MySQL 兼容）
> - Testcontainers 可以在 Docker 容器中运行真正的 MySQL
> - H2 可以配置为 MySQL 兼容模式

> **问题**：开发环境用 SQLite、生产环境用 MySQL 可行吗？
> 
> **答案**：**强烈不推荐**。
> - SQL 语法差异（自增、日期函数、字符串拼接等）
> - 事务和锁行为差异（可能导致生产环境死锁）
> - 数据类型和约束差异（可能导致数据截断或脏数据）
> - 外键约束行为差异（SQLite 默认不强制）

> **最佳实践**：
> 1. **所有环境使用相同的数据库**（开发、测试、生产都用 MySQL 8.0）
> 2. **本地开发**：使用 Docker 运行 MySQL 或直接安装
> 3. **自动化测试**：使用 Testcontainers 启动 MySQL 容器
> 4. **生产环境**：使用 InnoDB Cluster 或主从复制
> 5. **Schema 管理**：使用 Flyway 或 Liquibase 管理数据库迁移

> **核心原则**：
> **"环境一致性是避免生产事故的关键。不要为了开发方便而牺牲一致性。"**

现代开发工具（Docker、Testcontainers）已经让"在本地运行 MySQL"变得非常简单，完全没有必要再使用 SQLite 作为开发环境的"简化版"数据库。

---

## 3. JdbcTemplate 使用 SQLite 的关键代码，与 MySQL 8.0 的区别

### 3.1 依赖配置

#### SQLite 依赖 (`pom.xml`)

```xml
<dependencies>
    <!-- Spring Boot JDBC Starter -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-jdbc</artifactId>
    </dependency>
    
    <!-- SQLite JDBC 驱动 -->
    <dependency>
        <groupId>org.xerial</groupId>
        <artifactId>sqlite-jdbc</artifactId>
        <version>3.42.0.0</version>
    </dependency>
    
    <!-- Spring Boot Web Starter (可选，本项目使用) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
</dependencies>
```

#### MySQL 8.0 依赖 (`pom.xml`)

```xml
<dependencies>
    <!-- Spring Boot JDBC Starter -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-jdbc</artifactId>
    </dependency>
    
    <!-- MySQL 8.0 JDBC 驱动 (Connector/J) -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <version>8.0.33</version>
        <scope>runtime</scope>
    </dependency>
    
    <!-- HikariCP 连接池（Spring Boot 默认自动引入） -->
    <!-- <groupId>com.zaxxer</groupId> -->
    <!-- <artifactId>HikariCP</artifactId> -->
    
    <!-- Spring Boot Web Starter -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
</dependencies>
```

**驱动版本说明：**
- MySQL 8.0 推荐使用 `mysql-connector-j` 8.0.x 版本
- 旧版本 `mysql-connector-java` 已不再维护
- 驱动类从 `com.mysql.jdbc.Driver` 变为 `com.mysql.cj.jdbc.Driver`

### 3.2 数据源配置

#### SQLite 配置 (`application.properties`)

```properties
# 服务器端口
server.port=8080

# SQLite 数据库配置
spring.datasource.url=jdbc:sqlite:demo.db
spring.datasource.driver-class-name=org.sqlite.JDBC
# SQLite 不需要用户名和密码

# 初始化数据库
spring.sql.init.mode=always
spring.sql.init.schema-locations=classpath:schema.sql

# SQLite 不支持连接池，以下配置可选
# spring.datasource.hikari.maximum-pool-size=1
```

#### MySQL 8.0 配置 (`application.properties`)

```properties
# 服务器端口
server.port=8080

# MySQL 8.0 数据库配置
spring.datasource.url=jdbc:mysql://localhost:3306/mydb?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&characterEncoding=utf8mb4
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.username=root
spring.datasource.password=your_password

# 初始化数据库（可选）
spring.sql.init.mode=never  # 生产环境建议 never
spring.sql.init.schema-locations=classpath:schema-mysql.sql

# HikariCP 连接池配置（Spring Boot 默认使用 HikariCP）
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
spring.datasource.hikari.pool-name=MySQLPool
spring.datasource.hikari.auto-commit=true
spring.datasource.hikari.read-only=false

# 连接测试查询
spring.datasource.hikari.connection-test-query=SELECT 1

# MySQL 特定配置
spring.datasource.hikari.data-source-properties.useUnicode=true
spring.datasource.hikari.data-source-properties.characterEncoding=utf8mb4
spring.datasource.hikari.data-source-properties.cachePrepStmts=true
spring.datasource.hikari.data-source-properties.prepStmtCacheSize=250
spring.datasource.hikari.data-source-properties.prepStmtCacheSqlLimit=2048
spring.datasource.hikari.data-source-properties.useServerPrepStmts=true
spring.datasource.hikari.data-source-properties.useLocalSessionState=true
spring.datasource.hikari.data-source-properties.rewriteBatchedStatements=true
spring.datasource.hikari.data-source-properties.cacheResultSetMetadata=true
spring.datasource.hikari.data-source-properties.cacheServerConfiguration=true
spring.datasource.hikari.data-source-properties.elideSetAutoCommits=true
spring.datasource.hikari.data-source-properties.maintainTimeStats=false
```

**MySQL 8.0 URL 参数说明：**

| 参数 | 值 | 说明 |
| ---- | ---- | ---- |
| `useSSL` | `false` | 生产环境建议 `true`，开发环境可设为 `false` |
| `serverTimezone` | `Asia/Shanghai` | MySQL 8.0 必须指定时区 |
| `allowPublicKeyRetrieval` | `true` | 允许获取公钥（用于 caching_sha2_password 认证） |
| `characterEncoding` | `utf8mb4` | 使用完整的 UTF-8 字符集（支持 emoji） |
| `zeroDateTimeBehavior` | `convertToNull` | 如何处理 '0000-00-00' 日期 |

#### MySQL 8.0 YAML 配置示例 (`application.yml`)

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mydb?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&characterEncoding=utf8mb4
    driver-class-name: com.mysql.cj.jdbc.Driver
    username: root
    password: your_password
    hikari:
      connection-timeout: 30000
      maximum-pool-size: 10
      minimum-idle: 5
      idle-timeout: 600000
      max-lifetime: 1800000
      pool-name: MySQLPool
      connection-test-query: SELECT 1
      data-source-properties:
        useUnicode: true
        characterEncoding: utf8mb4
        cachePrepStmts: true
        prepStmtCacheSize: 250
        prepStmtCacheSqlLimit: 2048
        useServerPrepStmts: true
        rewriteBatchedStatements: true
  sql:
    init:
      mode: never
```

### 3.3 JdbcTemplate 关键代码对比

#### 3.3.1 查询操作

**SQLite 代码（本项目示例）：**

**文件位置**：`src/main/java/com/example/repository/ScoreRepository.java:25-28`

```java
public List<Score> findAll() {
    String sql = "SELECT * FROM score";
    return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(Score.class));
}
```

**MySQL 8.0 代码：**

```java
public List<Score> findAll() {
    String sql = "SELECT * FROM score";
    return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(Score.class));
}
```

**区别**：**完全相同**。标准的 SELECT 语句和 JdbcTemplate API 在两种数据库中使用方式一致。

#### 3.3.2 根据 ID 查询

**SQLite 代码：**

**文件位置**：`src/main/java/com/example/repository/ScoreRepository.java:31-39`

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

**MySQL 8.0 代码：**

```java
public Optional<Score> findById(Long id) {
    String sql = "SELECT * FROM score WHERE id = ?";
    try {
        Score score = jdbcTemplate.queryForObject(sql, 
            new BeanPropertyRowMapper<>(Score.class), id);
        return Optional.ofNullable(score);
    } catch (EmptyResultDataAccessException e) {
        return Optional.empty();
    }
}
```

**区别**：
- 两者基本相同
- MySQL 通常会捕获更具体的 `EmptyResultDataAccessException`

#### 3.3.3 插入数据（获取自增主键）

**SQLite 代码：**

**文件位置**：`src/main/java/com/example/repository/ScoreRepository.java:48-65`

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

**MySQL 8.0 代码：**

```java
public Score save(Score score) {
    String sql = "INSERT INTO score (student_id, subject, score, exam_date) VALUES (?, ?, ?, ?)";
    KeyHolder keyHolder = new GeneratedKeyHolder();

    jdbcTemplate.update(connection -> {
        PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setLong(1, score.getStudentId());
        ps.setString(2, score.getSubject());
        // MySQL 可以使用更具体的方法
        if (score.getScore() != null) {
            ps.setDouble(3, score.getScore());
        } else {
            ps.setNull(3, Types.DOUBLE);
        }
        // MySQL 8.0 支持 LocalDateTime
        if (score.getExamDate() != null) {
            ps.setString(4, score.getExamDate());
            // 或者：ps.setObject(4, LocalDateTime.parse(score.getExamDate()));
        } else {
            ps.setNull(4, Types.VARCHAR);
        }
        return ps;
    }, keyHolder);

    // MySQL 8.0 的 KeyHolder 可能返回 BigInteger
    Long generatedId = Objects.requireNonNull(keyHolder.getKey()).longValue();
    score.setId(generatedId);
    return score;
}
```

**区别**：
- **API 使用方式完全相同**：`KeyHolder` 和 `Statement.RETURN_GENERATED_KEYS` 的使用方式一致
- **SQL 语法注意点**：
  - SQLite：`AUTOINCREMENT` 关键字
  - MySQL：`AUTO_INCREMENT` 关键字
- **类型处理**：MySQL 对 null 值处理更严格

#### 3.3.4 批量操作

**SQLite 批量插入：**
```java
public int[] batchInsert(List<Score> scores) {
    String sql = "INSERT INTO score (student_id, subject, score, exam_date) VALUES (?, ?, ?, ?)";
    
    return jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
        @Override
        public void setValues(PreparedStatement ps, int i) throws SQLException {
            Score score = scores.get(i);
            ps.setLong(1, score.getStudentId());
            ps.setString(2, score.getSubject());
            ps.setObject(3, score.getScore());
            ps.setString(4, score.getExamDate());
        }
        
        @Override
        public int getBatchSize() {
            return scores.size();
        }
    });
}
```

**MySQL 8.0 批量插入（使用 rewriteBatchedStatements 优化）：**
```java
public int[] batchInsert(List<Score> scores) {
    String sql = "INSERT INTO score (student_id, subject, score, exam_date) VALUES (?, ?, ?, ?)";
    
    // MySQL 8.0 + rewriteBatchedStatements=true 时，批量插入会被优化为
    // INSERT INTO table VALUES (...), (...), (...)
    // 性能提升显著
    return jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
        @Override
        public void setValues(PreparedStatement ps, int i) throws SQLException {
            Score score = scores.get(i);
            ps.setLong(1, score.getStudentId());
            ps.setString(2, score.getSubject());
            if (score.getScore() != null) {
                ps.setDouble(3, score.getScore());
            } else {
                ps.setNull(3, Types.DOUBLE);
            }
            ps.setString(4, score.getExamDate());
        }
        
        @Override
        public int getBatchSize() {
            return scores.size();
        }
    });
}
```

**MySQL 8.0 批量操作注意点：**
- 需在 URL 中添加 `rewriteBatchedStatements=true` 才能获得最佳性能
- 可以使用 `NamedParameterJdbcTemplate` 进行更方便的命名参数操作

#### 3.3.5 使用 NamedParameterJdbcTemplate（MySQL 8.0 推荐）

```java
@Repository
public class ScoreRepository {
    
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    
    public ScoreRepository(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }
    
    public List<Score> findBySubjectAndScore(String subject, Double minScore) {
        String sql = "SELECT * FROM score WHERE subject = :subject AND score >= :minScore";
        
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("subject", subject)
            .addValue("minScore", minScore);
        
        return namedParameterJdbcTemplate.query(sql, params, 
            new BeanPropertyRowMapper<>(Score.class));
    }
    
    public Score save(Score score) {
        String sql = "INSERT INTO score (student_id, subject, score, exam_date) " +
                    "VALUES (:studentId, :subject, :score, :examDate)";
        
        KeyHolder keyHolder = new GeneratedKeyHolder();
        
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("studentId", score.getStudentId())
            .addValue("subject", score.getSubject())
            .addValue("score", score.getScore())
            .addValue("examDate", score.getExamDate());
        
        namedParameterJdbcTemplate.update(sql, params, keyHolder);
        
        score.setId(Objects.requireNonNull(keyHolder.getKey()).longValue());
        return score;
    }
}
```

### 3.4 JdbcTemplate 使用 SQLite 与 MySQL 8.0 的主要区别总结

| 方面 | SQLite | MySQL 8.0 | 影响 |
| ---- | ------ | --------- | ---- |
| **数据源配置** | 无需用户名密码 | 需要用户名密码 | 配置文件不同 |
| **URL 格式** | `jdbc:sqlite:文件名` | `jdbc:mysql://主机:端口/数据库?参数` | 连接串格式不同 |
| **驱动类** | `org.sqlite.JDBC` | `com.mysql.cj.jdbc.Driver` | 驱动类不同 |
| **连接池** | 不适用（嵌入式） | 必须配置（HikariCP 等） | MySQL 需要连接池管理 |
| **JdbcTemplate API** | 完全相同 | 完全相同 | 代码层面无差异 |
| **SQL 语法** | 部分差异（如自增关键字） | 标准 SQL + 扩展 | 编写 SQL 时需注意 |
| **事务管理** | Spring 管理相同 | Spring 管理相同 | 配置 `@Transactional` 即可 |
| **批量操作性能** | 一般 | 优秀（需配置 rewriteBatchedStatements） | MySQL 批量操作性能更好 |
| **参数处理** | 宽松 | 严格 | MySQL 对 null 值处理更严格 |

### 3.5 事务管理对比

#### SQLite 事务管理

```java
@Service
public class ScoreService {
    
    private final ScoreRepository scoreRepository;
    private final StudentRepository studentRepository;
    private final TransactionTemplate transactionTemplate;
    
    public ScoreService(ScoreRepository scoreRepository, 
                        StudentRepository studentRepository,
                        TransactionTemplate transactionTemplate) {
        this.scoreRepository = scoreRepository;
        this.studentRepository = studentRepository;
        this.transactionTemplate = transactionTemplate;
    }
    
    // 使用 @Transactional 注解
    @Transactional
    public void transferScores(Long fromStudentId, Long toStudentId, List<Long> scoreIds) {
        // 检查学生是否存在
        if (!studentRepository.existsById(fromStudentId) || 
            !studentRepository.existsById(toStudentId)) {
            throw new IllegalArgumentException("学生不存在");
        }
        
        // 更新成绩的学生ID
        for (Long scoreId : scoreIds) {
            Optional<Score> scoreOpt = scoreRepository.findById(scoreId);
            if (scoreOpt.isPresent()) {
                Score score = scoreOpt.get();
                if (!score.getStudentId().equals(fromStudentId)) {
                    throw new IllegalStateException("成绩不属于指定学生");
                }
                score.setStudentId(toStudentId);
                scoreRepository.update(score);
            }
        }
        
        // SQLite 的事务是数据库级别的，写操作会锁定整个数据库
        // 如果出现异常，Spring 会自动回滚
    }
    
    // 使用 TransactionTemplate（编程式事务）
    public void transferScoresProgrammatic(Long fromStudentId, Long toStudentId, List<Long> scoreIds) {
        transactionTemplate.execute(status -> {
            try {
                transferScores(fromStudentId, toStudentId, scoreIds);
                return null;
            } catch (Exception e) {
                status.setRollbackOnly();
                throw e;
            }
        });
    }
}
```

#### MySQL 8.0 事务管理

```java
@Service
public class ScoreService {
    
    private final ScoreRepository scoreRepository;
    private final StudentRepository studentRepository;
    
    public ScoreService(ScoreRepository scoreRepository, 
                        StudentRepository studentRepository) {
        this.scoreRepository = scoreRepository;
        this.studentRepository = studentRepository;
    }
    
    // 使用 @Transactional 注解
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public void transferScores(Long fromStudentId, Long toStudentId, List<Long> scoreIds) {
        // MySQL 8.0 可以指定隔离级别
        // InnoDB 支持行级锁，并发性能好
        
        // 检查学生是否存在
        Student fromStudent = studentRepository.findById(fromStudentId)
            .orElseThrow(() -> new IllegalArgumentException("源学生不存在"));
        Student toStudent = studentRepository.findById(toStudentId)
            .orElseThrow(() -> new IllegalArgumentException("目标学生不存在"));
        
        // 更新成绩的学生ID
        for (Long scoreId : scoreIds) {
            Score score = scoreRepository.findById(scoreId)
                .orElseThrow(() -> new IllegalArgumentException("成绩不存在"));
            
            if (!score.getStudentId().equals(fromStudentId)) {
                throw new IllegalStateException("成绩不属于源学生");
            }
            
            score.setStudentId(toStudentId);
            scoreRepository.update(score);
        }
        
        // MySQL 8.0 + InnoDB 使用行级锁
        // 只有被修改的行会被锁定，其他行仍可读写
        // 支持 MVCC，读写不阻塞
    }
    
    // 带传播行为的事务
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createStudentWithScores(Student student, List<Score> scores) {
        // REQUIRES_NEW 会挂起当前事务，创建新事务
        Student savedStudent = studentRepository.save(student);
        
        for (Score score : scores) {
            score.setStudentId(savedStudent.getId());
            scoreRepository.save(score);
        }
    }
}
```

**事务管理对比表：**

| 特性 | SQLite | MySQL 8.0 (InnoDB) |
| ---- | ------ | ------------------- |
| **隔离级别** | 仅支持 READ UNCOMMITTED 和 SERIALIZABLE | 支持全部 4 种隔离级别 |
| **锁粒度** | 数据库级锁 | 行级锁 + 间隙锁 |
| **MVCC** | 不支持 | 完全支持 |
| **死锁** | 不会发生（数据库级锁） | 可能发生，但会自动检测 |
| **并发性能** | 写操作阻塞所有操作 | 读写不阻塞，并发性能好 |
| **Spring 集成** | 完全支持 | 完全支持 |

### 3.6 注意事项

#### SQLite 注意事项

1. **SQLite 外键约束**：SQLite 默认不强制外键约束，如需启用，需要在连接后执行：
   ```java
   @PostConstruct
   public void init() {
       jdbcTemplate.execute("PRAGMA foreign_keys = ON;");
   }
   ```

2. **日期时间处理**：SQLite 没有原生的日期时间类型，通常使用 `TEXT` 存储（如 "2024-01-15"）或 `INTEGER` 存储 Unix 时间戳。

3. **并发性能**：SQLite 的写操作会锁定整个数据库，不适合高并发写入场景。如果需要高并发，应该考虑切换到 MySQL 或 PostgreSQL。

4. **连接池**：SQLite 是嵌入式数据库，不需要连接池。实际上，多线程同时写入可能会导致数据库锁问题。

#### MySQL 8.0 注意事项

1. **时区配置**：MySQL 8.0 必须在 URL 中指定 `serverTimezone` 参数，否则连接会失败。

2. **字符集**：推荐使用 `utf8mb4` 字符集，支持完整的 Unicode（包括 emoji）。

3. **SSL 连接**：MySQL 8.0 默认要求 SSL 连接，开发环境可以设置 `useSSL=false`。

4. **连接池**：MySQL 必须使用连接池（如 HikariCP），直接创建连接性能很差。

5. **rewriteBatchedStatements**：批量操作时，务必在 URL 中添加 `rewriteBatchedStatements=true`，可以大幅提升批量插入性能。

6. **认证插件**：MySQL 8.0 默认使用 `caching_sha2_password` 认证插件，需要配置 `allowPublicKeyRetrieval=true` 或使用 SSL 连接。

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

| 配置项 | 值 | 说明 |
| ------ | ---- | ---- |
| `spring.datasource.url` | `jdbc:sqlite:demo.db` | 指定数据库文件路径，不存在时自动创建 |
| `spring.datasource.driver-class-name` | `org.sqlite.JDBC` | SQLite JDBC 驱动 |
| `spring.sql.init.mode` | `always` | 每次启动都执行 SQL 初始化（可选值：`always`、`never`、`embedded`） |
| `spring.sql.init.schema-locations` | `classpath:schema.sql` | 指定要执行的 SQL 脚本位置 |

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

| `spring.sql.init.mode` 值 | 行为 | 适用场景 |
| ------------------------ | ---- | -------- |
| `always` | 每次启动都执行初始化脚本 | 开发环境、测试环境，需要每次重置数据库 |
| `never` | 不执行任何初始化 | 生产环境，数据库已预先配置好 |
| `embedded` | 仅对嵌入式数据库（如 H2、HSQL、Derby）执行初始化 | 生产环境使用 MySQL/PostgreSQL，开发环境使用 H2 |

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

---

## 5. MySQL 8.0 与 Spring Boot 的完整集成方案

### 5.1 方案概述

MySQL 8.0 与 Spring Boot 的集成有多种方案，从简单到复杂依次为：

| 集成方案 | 复杂度 | 适用场景 |
| -------- | ------ | -------- |
| **Spring JDBC (JdbcTemplate)** | 低 | 简单 CRUD、原生 SQL 控制 |
| **Spring Data JPA** | 中 | 标准 ORM、快速开发 |
| **MyBatis** | 中 | SQL 优化、复杂查询 |
| **Spring Data JDBC** | 中 | 简单 ORM、无 Hibernate  overhead |
| **jOOQ** | 高 | 类型安全 SQL、复杂查询 |

### 5.2 方案一：Spring Data JPA（最常用）

#### 5.2.1 依赖配置

```xml
<dependencies>
    <!-- Spring Boot Starter Data JPA -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    
    <!-- MySQL 8.0 Connector -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>
    
    <!-- HikariCP 连接池（自动引入） -->
</dependencies>
```

#### 5.2.2 数据源配置

```properties
# MySQL 8.0 数据源
spring.datasource.url=jdbc:mysql://localhost:3306/mydb?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&characterEncoding=utf8mb4
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.username=root
spring.datasource.password=your_password

# JPA 配置
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.use_sql_comments=true

# HikariCP 连接池
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.idle-timeout=300000
spring.datasource.hikari.pool-name=SpringBootHikariCP
```

#### 5.2.3 实体类定义

```java
package com.example.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "student")
public class Student {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "name", nullable = false, length = 100)
    private String name;
    
    @Column(name = "age")
    private Integer age;
    
    @Column(name = "gender", length = 10)
    private String gender;
    
    @Column(name = "major", length = 100)
    private String major;
    
    @Column(name = "created_at")
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
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getMajor() { return major; }
    public void setMajor(String major) { this.major = major; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
```

```java
package com.example.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "score", 
       indexes = {
           @Index(name = "idx_student_id", columnList = "student_id"),
           @Index(name = "idx_subject", columnList = "subject")
       })
public class Score {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "student_id", nullable = false)
    private Long studentId;
    
    @Column(name = "subject", nullable = false, length = 50)
    private String subject;
    
    @Column(name = "score")
    private Double score;
    
    @Column(name = "exam_date")
    private LocalDate examDate;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", insertable = false, updatable = false)
    private Student student;
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }
    public LocalDate getExamDate() { return examDate; }
    public void setExamDate(LocalDate examDate) { this.examDate = examDate; }
    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
}
```

#### 5.2.4 Repository 接口

```java
package com.example.repository;

import com.example.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    
    // 派生查询（根据方法名自动生成 SQL）
    List<Student> findByMajor(String major);
    List<Student> findByAgeGreaterThan(Integer age);
    List<Student> findByGenderAndMajor(String gender, String major);
    Optional<Student> findByName(String name);
    boolean existsByName(String name);
    long countByMajor(String major);
    
    // @Query 注解（JPQL）
    @Query("SELECT s FROM Student s WHERE s.age BETWEEN :minAge AND :maxAge")
    List<Student> findByAgeRange(@Param("minAge") Integer minAge, @Param("maxAge") Integer maxAge);
    
    // 原生 SQL 查询
    @Query(value = "SELECT * FROM student WHERE major LIKE %:keyword%", nativeQuery = true)
    List<Student> searchByMajorKeyword(@Param("keyword") String keyword);
    
    // 排序和分页
    @Query("SELECT s FROM Student s ORDER BY s.name ASC")
    List<Student> findAllSortedByName();
}
```

```java
package com.example.repository;

import com.example.entity.Score;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScoreRepository extends JpaRepository<Score, Long> {
    
    List<Score> findByStudentId(Long studentId);
    List<Score> findBySubject(String subject);
    List<Score> findByStudentIdAndSubject(Long studentId, String subject);
    
    // 使用 JPQL 查询
    @Query("SELECT s FROM Score s WHERE s.score >= :minScore ORDER BY s.score DESC")
    List<Score> findHighScores(@Param("minScore") Double minScore);
    
    // 聚合查询
    @Query("SELECT AVG(s.score) FROM Score s WHERE s.subject = :subject")
    Double findAverageScoreBySubject(@Param("subject") String subject);
    
    @Query("SELECT COUNT(s) FROM Score s WHERE s.studentId = :studentId")
    long countByStudentId(@Param("studentId") Long studentId);
    
    // 关联查询（JOIN）
    @Query("SELECT s FROM Score s JOIN FETCH s.student WHERE s.studentId = :studentId")
    List<Score> findByStudentIdWithStudent(@Param("studentId") Long studentId);
}
```

#### 5.2.5 Service 层

```java
package com.example.service;

import com.example.entity.Student;
import com.example.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class StudentService {
    
    private final StudentRepository studentRepository;
    
    @Autowired
    public StudentService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }
    
    public List<Student> findAll() {
        return studentRepository.findAll();
    }
    
    public Optional<Student> findById(Long id) {
        return studentRepository.findById(id);
    }
    
    @Transactional
    public Student save(Student student) {
        return studentRepository.save(student);
    }
    
    @Transactional
    public Student update(Long id, Student studentDetails) {
        return studentRepository.findById(id)
            .map(student -> {
                student.setName(studentDetails.getName());
                student.setAge(studentDetails.getAge());
                student.setGender(studentDetails.getGender());
                student.setMajor(studentDetails.getMajor());
                return studentRepository.save(student);
            })
            .orElseThrow(() -> new RuntimeException("Student not found with id: " + id));
    }
    
    @Transactional
    public void deleteById(Long id) {
        studentRepository.deleteById(id);
    }
    
    public List<Student> findByMajor(String major) {
        return studentRepository.findByMajor(major);
    }
    
    public List<Student> findByAgeRange(Integer minAge, Integer maxAge) {
        return studentRepository.findByAgeRange(minAge, maxAge);
    }
}
```

### 5.3 方案二：MyBatis

#### 5.3.1 依赖配置

```xml
<dependencies>
    <!-- MyBatis Spring Boot Starter -->
    <dependency>
        <groupId>org.mybatis.spring.boot</groupId>
        <artifactId>mybatis-spring-boot-starter</artifactId>
        <version>3.0.3</version>
    </dependency>
    
    <!-- MySQL 8.0 Connector -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>
</dependencies>
```

#### 5.3.2 配置文件

```properties
# 数据源配置
spring.datasource.url=jdbc:mysql://localhost:3306/mydb?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&characterEncoding=utf8mb4
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.username=root
spring.datasource.password=your_password

# MyBatis 配置
mybatis.mapper-locations=classpath:mapper/*.xml
mybatis.type-aliases-package=com.example.entity
mybatis.configuration.map-underscore-to-camel-case=true
mybatis.configuration.default-fetch-size=100
mybatis.configuration.default-statement-timeout=30
```

#### 5.3.3 Mapper 接口

```java
package com.example.mapper;

import com.example.entity.Student;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Mapper
@Repository
public interface StudentMapper {
    
    // 使用注解方式
    @Select("SELECT * FROM student WHERE id = #{id}")
    @Results(id = "studentResultMap", value = {
        @Result(property = "id", column = "id"),
        @Result(property = "name", column = "name"),
        @Result(property = "age", column = "age"),
        @Result(property = "gender", column = "gender"),
        @Result(property = "major", column = "major")
    })
    Optional<Student> findById(Long id);
    
    @Select("SELECT * FROM student")
    @ResultMap("studentResultMap")
    List<Student> findAll();
    
    @Insert("INSERT INTO student (name, age, gender, major) VALUES (#{name}, #{age}, #{gender}, #{major})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Student student);
    
    @Update("UPDATE student SET name = #{name}, age = #{age}, gender = #{gender}, major = #{major} WHERE id = #{id}")
    int update(Student student);
    
    @Delete("DELETE FROM student WHERE id = #{id}")
    int deleteById(Long id);
    
    // 使用 XML 方式（对应 mapper/StudentMapper.xml）
    List<Student> findByMajor(String major);
    
    List<Student> searchByCondition(@Param("name") String name, 
                                      @Param("major") String major,
                                      @Param("minAge") Integer minAge);
}
```

#### 5.3.4 Mapper XML 文件

```xml
<!-- src/main/resources/mapper/StudentMapper.xml -->
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="com.example.mapper.StudentMapper">
    
    <resultMap id="BaseResultMap" type="com.example.entity.Student">
        <id column="id" property="id" jdbcType="BIGINT"/>
        <result column="name" property="name" jdbcType="VARCHAR"/>
        <result column="age" property="age" jdbcType="INTEGER"/>
        <result column="gender" property="gender" jdbcType="VARCHAR"/>
        <result column="major" property="major" jdbcType="VARCHAR"/>
    </resultMap>
    
    <sql id="Base_Column_List">
        id, name, age, gender, major
    </sql>
    
    <select id="findByMajor" resultMap="BaseResultMap">
        SELECT <include refid="Base_Column_List"/>
        FROM student
        WHERE major = #{major}
        ORDER BY name
    </select>
    
    <select id="searchByCondition" resultMap="BaseResultMap">
        SELECT <include refid="Base_Column_List"/>
        FROM student
        <where>
            <if test="name != null and name != ''">
                AND name LIKE CONCAT('%', #{name}, '%')
            </if>
            <if test="major != null and major != ''">
                AND major = #{major}
            </if>
            <if test="minAge != null">
                AND age >= #{minAge}
            </if>
        </where>
        ORDER BY id DESC
    </select>
    
    <!-- 分页查询示例 -->
    <select id="findByPage" resultMap="BaseResultMap">
        SELECT <include refid="Base_Column_List"/>
        FROM student
        ORDER BY id
        LIMIT #{offset}, #{pageSize}
    </select>
    
    <!-- 批量插入示例 -->
    <insert id="batchInsert" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO student (name, age, gender, major)
        VALUES
        <foreach collection="list" item="item" separator=",">
            (#{item.name}, #{item.age}, #{item.gender}, #{item.major})
        </foreach>
    </insert>
</mapper>
```

### 5.4 方案三：连接池配置详解

#### 5.4.1 HikariCP 配置（Spring Boot 默认）

```properties
# 核心配置
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
spring.datasource.hikari.connection-timeout=30000

# 连接测试
spring.datasource.hikari.connection-test-query=SELECT 1
spring.datasource.hikari.keepalive-time=300000

# 池名称
spring.datasource.hikari.pool-name=MySQLHikariPool

# 高级配置
spring.datasource.hikari.auto-commit=true
spring.datasource.hikari.read-only=false
spring.datasource.hikari.transaction-isolation=TRANSACTION_READ_COMMITTED

# MySQL 特定优化
spring.datasource.hikari.data-source-properties.cachePrepStmts=true
spring.datasource.hikari.data-source-properties.prepStmtCacheSize=250
spring.datasource.hikari.data-source-properties.prepStmtCacheSqlLimit=2048
spring.datasource.hikari.data-source-properties.useServerPrepStmts=true
spring.datasource.hikari.data-source-properties.useLocalSessionState=true
spring.datasource.hikari.data-source-properties.rewriteBatchedStatements=true
spring.datasource.hikari.data-source-properties.cacheResultSetMetadata=true
spring.datasource.hikari.data-source-properties.cacheServerConfiguration=true
spring.datasource.hikari.data-source-properties.elideSetAutoCommits=true
spring.datasource.hikari.data-source-properties.maintainTimeStats=false
```

#### 5.4.2 连接池大小计算

**计算公式**：
```
connections = ((core_count * 2) + effective_spindle_count)
```

**实际建议**：
| 应用规模 | 连接池大小 | 说明 |
| -------- | ---------- | ---- |
| 小型应用 | 5-10 | 并发量小，简单业务 |
| 中型应用 | 10-30 | 中等并发，较复杂业务 |
| 大型应用 | 30-50 | 高并发，复杂业务 |
| 超大型应用 | 50+ | 需要配合数据库代理 |

**注意**：连接池不是越大越好，过大的连接池会导致：
- 更多的内存消耗
- 更多的上下文切换
- 数据库端的资源竞争

### 5.5 方案四：读写分离配置

#### 5.5.1 使用 AbstractRoutingDataSource

```java
package com.example.config;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

public class RoutingDataSource extends AbstractRoutingDataSource {
    
    @Override
    protected Object determineCurrentLookupKey() {
        return DataSourceContextHolder.getDataSourceType();
    }
}
```

```java
package com.example.config;

public class DataSourceContextHolder {
    
    private static final ThreadLocal<String> contextHolder = new ThreadLocal<>();
    
    public static final String MASTER = "master";
    public static final String SLAVE = "slave";
    
    public static void setDataSourceType(String dataSourceType) {
        contextHolder.set(dataSourceType);
    }
    
    public static String getDataSourceType() {
        return contextHolder.get();
    }
    
    public static void clearDataSourceType() {
        contextHolder.remove();
    }
}
```

```java
package com.example.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class DataSourceConfig {
    
    @Bean
    @ConfigurationProperties("spring.datasource.master")
    public DataSource masterDataSource() {
        return new HikariDataSource();
    }
    
    @Bean
    @ConfigurationProperties("spring.datasource.slave")
    public DataSource slaveDataSource() {
        return new HikariDataSource();
    }
    
    @Bean
    @Primary
    public DataSource routingDataSource(
            @Qualifier("masterDataSource") DataSource master,
            @Qualifier("slaveDataSource") DataSource slave) {
        
        RoutingDataSource routingDataSource = new RoutingDataSource();
        
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put(DataSourceContextHolder.MASTER, master);
        targetDataSources.put(DataSourceContextHolder.SLAVE, slave);
        
        routingDataSource.setTargetDataSources(targetDataSources);
        routingDataSource.setDefaultTargetDataSource(master);
        
        return routingDataSource;
    }
    
    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
```

#### 5.5.2 使用注解切换数据源

```java
package com.example.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ReadOnly {
}
```

```java
package com.example.aspect;

import com.example.annotation.ReadOnly;
import com.example.config.DataSourceContextHolder;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
public class ReadOnlyAspect implements Ordered {
    
    @Around("@annotation(com.example.annotation.ReadOnly)")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        
        ReadOnly readOnly = method.getAnnotation(ReadOnly.class);
        
        if (readOnly != null) {
            DataSourceContextHolder.setDataSourceType(DataSourceContextHolder.SLAVE);
        }
        
        try {
            return point.proceed();
        } finally {
            DataSourceContextHolder.clearDataSourceType();
        }
    }
    
    @Override
    public int getOrder() {
        return 0;
    }
}
```

#### 5.5.3 配置文件

```properties
# 主库配置
spring.datasource.master.jdbc-url=jdbc:mysql://master-host:3306/mydb?useSSL=false&serverTimezone=Asia/Shanghai
spring.datasource.master.username=root
spring.datasource.master.password=password
spring.datasource.master.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.master.hikari.maximum-pool-size=10

# 从库配置
spring.datasource.slave.jdbc-url=jdbc:mysql://slave-host:3306/mydb?useSSL=false&serverTimezone=Asia/Shanghai
spring.datasource.slave.username=root
spring.datasource.slave.password=password
spring.datasource.slave.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.slave.hikari.maximum-pool-size=10
```

#### 5.5.4 Service 使用示例

```java
package com.example.service;

import com.example.annotation.ReadOnly;
import com.example.entity.Student;
import com.example.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StudentService {
    
    private final StudentRepository studentRepository;
    
    @Autowired
    public StudentService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }
    
    // 读操作走从库
    @ReadOnly
    @Transactional(readOnly = true)
    public List<Student> findAll() {
        return studentRepository.findAll();
    }
    
    @ReadOnly
    @Transactional(readOnly = true)
    public Student findById(Long id) {
        return studentRepository.findById(id).orElse(null);
    }
    
    // 写操作走主库（默认）
    @Transactional
    public Student save(Student student) {
        return studentRepository.save(student);
    }
    
    @Transactional
    public void deleteById(Long id) {
        studentRepository.deleteById(id);
    }
}
```

---

## 6. MySQL 8.0 特有语法特性详解

### 6.1 窗口函数（Window Functions）

窗口函数是 MySQL 8.0 最重要的新特性之一，允许在不使用自连接的情况下进行复杂的数据分析。

#### 6.1.1 常用窗口函数

| 函数 | 说明 |
| ---- | ---- |
| `ROW_NUMBER()` | 为每一行分配唯一的行号 |
| `RANK()` | 排名，相同值排名相同，有间隙 |
| `DENSE_RANK()` | 密集排名，相同值排名相同，无间隙 |
| `NTILE(n)` | 将结果集分成 n 个桶 |
| `LAG(expr, n, default)` | 获取当前行之前第 n 行的值 |
| `LEAD(expr, n, default)` | 获取当前行之后第 n 行的值 |
| `FIRST_VALUE(expr)` | 获取窗口第一行的值 |
| `LAST_VALUE(expr)` | 获取窗口最后一行的值 |
| `NTH_VALUE(expr, n)` | 获取窗口第 n 行的值 |

#### 6.1.2 窗口函数示例

```sql
-- 准备数据
CREATE TABLE exam_scores (
    id INT PRIMARY KEY AUTO_INCREMENT,
    student_name VARCHAR(50),
    subject VARCHAR(50),
    score INT,
    exam_date DATE
);

INSERT INTO exam_scores (student_name, subject, score, exam_date) VALUES
('张三', '数学', 95, '2024-01-15'),
('李四', '数学', 88, '2024-01-15'),
('王五', '数学', 95, '2024-01-15'),
('赵六', '数学', 76, '2024-01-15'),
('张三', '英语', 85, '2024-01-16'),
('李四', '英语', 92, '2024-01-16'),
('王五', '英语', 88, '2024-01-16'),
('赵六', '英语', 90, '2024-01-16');

-- 1. 基本排名查询
SELECT 
    student_name,
    subject,
    score,
    ROW_NUMBER() OVER (PARTITION BY subject ORDER BY score DESC) as row_num,
    RANK() OVER (PARTITION BY subject ORDER BY score DESC) as rank_num,
    DENSE_RANK() OVER (PARTITION BY subject ORDER BY score DESC) as dense_rank_num
FROM exam_scores;

/* 结果示例（数学科目）：
+------------+---------+-------+---------+----------+----------------+
| student_name| subject | score | row_num | rank_num | dense_rank_num |
+------------+---------+-------+---------+----------+----------------+
| 张三       | 数学    |    95 |       1 |        1 |              1 |
| 王五       | 数学    |    95 |       2 |        1 |              1 |
| 李四       | 数学    |    88 |       3 |        3 |              2 |
| 赵六       | 数学    |    76 |       4 |        4 |              3 |
+------------+---------+-------+---------+----------+----------------+
*/

-- 2. 前后行比较（LAG/LEAD）
SELECT 
    student_name,
    subject,
    score,
    LAG(score, 1, 0) OVER (PARTITION BY subject ORDER BY score DESC) as prev_score,
    LEAD(score, 1, 0) OVER (PARTITION BY subject ORDER BY score DESC) as next_score,
    score - LAG(score, 1, score) OVER (PARTITION BY subject ORDER BY score DESC) as score_diff
FROM exam_scores;

-- 3. 分桶查询
SELECT 
    student_name,
    subject,
    score,
    NTILE(4) OVER (PARTITION BY subject ORDER BY score DESC) as quartile
FROM exam_scores;

-- 4. 累计和与移动平均
SELECT 
    exam_date,
    score,
    SUM(score) OVER (ORDER BY exam_date ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) as cumulative_sum,
    AVG(score) OVER (ORDER BY exam_date ROWS BETWEEN 2 PRECEDING AND CURRENT ROW) as moving_avg_3
FROM exam_scores
WHERE subject = '数学'
ORDER BY exam_date;
```

### 6.2 通用表表达式（CTE）

#### 6.2.1 基本 CTE

```sql
-- 简单 CTE
WITH high_scores AS (
    SELECT * FROM exam_scores WHERE score >= 90
)
SELECT * FROM high_scores ORDER BY score DESC;

-- 多个 CTE
WITH 
    math_scores AS (
        SELECT * FROM exam_scores WHERE subject = '数学'
    ),
    high_math AS (
        SELECT * FROM math_scores WHERE score >= 90
    )
SELECT * FROM high_math;
```

#### 6.2.2 递归 CTE

```sql
-- 生成数字序列
WITH RECURSIVE numbers(n) AS (
    SELECT 1
    UNION ALL
    SELECT n + 1 FROM numbers WHERE n < 10
)
SELECT * FROM numbers;

-- 生成日期序列
WITH RECURSIVE dates AS (
    SELECT DATE('2024-01-01') as date
    UNION ALL
    SELECT DATE_ADD(date, INTERVAL 1 DAY) 
    FROM dates 
    WHERE date < '2024-01-31'
)
SELECT * FROM dates;

-- 树形结构查询（部门层级）
CREATE TABLE department (
    id INT PRIMARY KEY,
    name VARCHAR(50),
    parent_id INT,
    FOREIGN KEY (parent_id) REFERENCES department(id)
);

INSERT INTO department (id, name, parent_id) VALUES
(1, '总公司', NULL),
(2, '技术部', 1),
(3, '市场部', 1),
(4, '前端组', 2),
(5, '后端组', 2),
(6, '销售组', 3);

-- 查询部门层级
WITH RECURSIVE dept_path AS (
    SELECT 
        id, 
        name, 
        parent_id, 
        CAST(name AS CHAR(200)) as path,
        1 as level
    FROM department 
    WHERE parent_id IS NULL
    
    UNION ALL
    
    SELECT 
        d.id, 
        d.name, 
        d.parent_id, 
        CONCAT(dp.path, ' -> ', d.name),
        dp.level + 1
    FROM department d
    JOIN dept_path dp ON d.parent_id = dp.id
)
SELECT * FROM dept_path ORDER BY path;
```

### 6.3 不可见索引（Invisible Indexes）

```sql
-- 创建不可见索引
CREATE INDEX idx_name ON student(name) INVISIBLE;

-- 查看索引状态
SHOW INDEX FROM student;

-- 修改索引可见性
ALTER TABLE student ALTER INDEX idx_name INVISIBLE;
ALTER TABLE student ALTER INDEX idx_name VISIBLE;

-- 优化器可以忽略不可见索引（用于测试）
-- 验证删除索引的影响，而无需实际删除
```

### 6.4 降序索引（Descending Indexes）

```sql
-- MySQL 8.0 之前 DESC 被忽略，现在真正支持降序索引
CREATE INDEX idx_score_desc ON exam_scores(score DESC);

-- 复合索引的降序
CREATE INDEX idx_subject_score ON exam_scores(subject ASC, score DESC);

-- 查看索引
SHOW INDEX FROM exam_scores;
```

### 6.5 函数索引（Functional Key Parts）

```sql
-- 基于表达式的索引
CREATE INDEX idx_name_upper ON student((UPPER(name)));

-- 使用函数索引
SELECT * FROM student WHERE UPPER(name) = 'ZHANGSAN';

-- JSON 字段的函数索引
CREATE TABLE user_profile (
    id INT PRIMARY KEY,
    preferences JSON
);

CREATE INDEX idx_theme ON user_profile((preferences->>'$.theme'));
```

### 6.6 NOWAIT 和 SKIP LOCKED

```sql
-- 立即返回错误而不是等待锁
SELECT * FROM student WHERE id = 1 FOR UPDATE NOWAIT;

-- 跳过已锁定的行
SELECT * FROM student WHERE id <= 10 FOR UPDATE SKIP LOCKED;

-- 结合使用
SELECT * FROM student 
WHERE id = 1 
FOR UPDATE OF student SKIP LOCKED NOWAIT;
```

### 6.7 直方图（Histogram）统计信息

```sql
-- 创建直方图
ANALYZE TABLE student UPDATE HISTOGRAM ON age;

-- 查看直方图
SELECT * FROM information_schema.column_statistics 
WHERE table_name = 'student' AND column_name = 'age';

-- 删除直方图
ANALYZE TABLE student DROP HISTOGRAM ON age;

-- 为多列创建直方图
ANALYZE TABLE student UPDATE HISTOGRAM ON major, age;
```

### 6.8 角色管理（Role Management）

```sql
-- 创建角色
CREATE ROLE 'app_read', 'app_write';

-- 授予权限给角色
GRANT SELECT ON mydb.* TO 'app_read';
GRANT INSERT, UPDATE, DELETE ON mydb.* TO 'app_write';

-- 创建用户
CREATE USER 'app_user'@'localhost' IDENTIFIED BY 'password';

-- 授予角色给用户
GRANT 'app_read', 'app_write' TO 'app_user'@'localhost';

-- 设置默认角色
SET DEFAULT ROLE 'app_read' TO 'app_user'@'localhost';

-- 激活角色
SET ROLE 'app_write';

-- 查看当前角色
SELECT CURRENT_ROLE();

-- 撤销角色
REVOKE 'app_write' FROM 'app_user'@'localhost';

-- 删除角色
DROP ROLE 'app_read', 'app_write';
```

---

## 7. 集群方案详细对比

### 7.1 各集群方案特性对比

| 特性 | InnoDB Cluster | Group Replication | NDB Cluster | 主从复制 |
| ---- | -------------- | ----------------- | ----------- | -------- |
| **官方支持** | ✅ 是 | ✅ 是 | ✅ 是 | ✅ 是 |
| **自动故障转移** | ✅ 是 | ✅ 是 | ✅ 是 | ❌ 需手动/第三方 |
| **数据一致性** | 强一致 | 强一致/最终一致 | 强一致 | 最终一致 |
| **读写分离** | ✅ 是 | ✅ 是 | ✅ 是 | ✅ 是 |
| **自动分片** | ❌ 否 | ❌ 否 | ✅ 是 | ❌ 否 |
| **事务支持** | ✅ 完整 | ✅ 完整 | ✅ 完整 | ✅ 完整 |
| **配置复杂度** | 中 | 高 | 很高 | 低 |
| **运维成本** | 中 | 高 | 很高 | 低 |
| **适用规模** | 中大型 | 中大型 | 超大型 | 中小型 |

### 7.2 InnoDB Cluster 详细配置

#### 7.2.1 环境准备

```bash
# 服务器规划
# mysql1:3306 - Primary (主节点)
# mysql2:3306 - Secondary (从节点)
# mysql3:3306 - Secondary (从节点)
# mysql-router:6446 - 读写端口
# mysql-router:6447 - 只读端口
```

#### 7.2.2 MySQL 配置文件（my.cnf）

```ini
[mysqld]
# 基本配置
server_id=1                          # 每个节点唯一
port=3306
datadir=/var/lib/mysql
socket=/var/lib/mysql/mysql.sock

# Group Replication 配置
plugin_load_add='group_replication.so'
group_replication_group_name="aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
group_replication_start_on_boot=off
group_replication_local_address= "mysql1:33061"
group_replication_group_seeds= "mysql1:33061,mysql2:33061,mysql3:33061"
group_replication_bootstrap_group=off

# 二进制日志
log_bin=mysql-bin
binlog_format=ROW
binlog_checksum=NONE
log_slave_updates=ON
enforce_gtid_consistency=ON
gtid_mode=ON

# 表空间
innodb_file_per_table=ON
innodb_flush_log_at_trx_commit=1
sync_binlog=1

# 性能优化
max_connections=500
innodb_buffer_pool_size=1G
```

#### 7.2.3 使用 MySQL Shell 配置集群

```javascript
// 连接到第一个节点
\connect root@mysql1:3306

// 检查实例配置
dba.checkInstanceConfiguration('root@mysql1:3306')

// 配置实例（如果需要）
dba.configureInstance('root@mysql1:3306', {
    interactive: false,
    restart: true
})

// 创建集群
var cluster = dba.createCluster('myCluster', {
    memberSslMode: 'REQUIRED',
    ipWhitelist: '192.168.0.0/24'
})

// 添加实例
cluster.addInstance('root@mysql2:3306', {
    recoveryMethod: 'clone'
})

cluster.addInstance('root@mysql3:3306', {
    recoveryMethod: 'clone'
})

// 查看集群状态
cluster.status()

// 查看集群拓扑
cluster.describe()
```

#### 7.2.4 MySQL Router 配置

```bash
# 引导 Router
mysqlrouter --bootstrap root@mysql1:3306 --directory /opt/mysqlrouter

# 启动 Router
/opt/mysqlrouter/start.sh

# 查看配置
cat /opt/mysqlrouter/mysqlrouter.conf
```

#### 7.2.5 应用连接配置

```properties
# 通过 Router 连接（读写分离）
# 读写端口：6446
# 只读端口：6447

# 主库连接（读写）
spring.datasource.master.jdbc-url=jdbc:mysql://mysql-router:6446/mydb?useSSL=false&serverTimezone=Asia/Shanghai

# 从库连接（只读）
spring.datasource.slave.jdbc-url=jdbc:mysql://mysql-router:6447/mydb?useSSL=false&serverTimezone=Asia/Shanghai
```

### 7.3 故障转移演示

```javascript
// 模拟主节点故障
// 在 mysql1 上执行：
sudo systemctl stop mysql

// 查看集群状态（在其他节点）
\connect root@mysql2:3306
var cluster = dba.getCluster()
cluster.status()
// 应该看到新的主节点已自动选举

// 恢复故障节点
sudo systemctl start mysql

// 重新加入集群
cluster.rejoinInstance('root@mysql1:3306')

// 可选：切换回原主节点
cluster.setPrimaryInstance('root@mysql1:3306')
```

---

## 8. 方案选择建议

### 8.1 根据规模选择

| 项目规模 | 数据库选择 | 集成方案 | 集群方案 |
| -------- | ---------- | -------- | -------- |
| **个人项目/原型** | SQLite | JdbcTemplate | 无 |
| **小型应用** | SQLite 或 MySQL | Spring Data JPA | 无 |
| **中型应用** | MySQL 8.0 | Spring Data JPA 或 MyBatis | 主从复制 |
| **大型应用** | MySQL 8.0 | MyBatis 或 jOOQ | InnoDB Cluster |
| **超大型应用** | MySQL 8.0 | MyBatis + 分库分表 | NDB Cluster 或 第三方中间件 |

### 8.2 根据业务特性选择

| 业务特性 | 推荐方案 | 理由 |
| -------- | -------- | ---- |
| **简单 CRUD** | Spring Data JPA | 开发效率高 |
| **复杂 SQL** | MyBatis | SQL 可控性强 |
| **高并发读写** | MySQL + 主从复制 + 读写分离 | 读能力可扩展 |
| **高可用性要求** | MySQL + InnoDB Cluster | 自动故障转移 |
| **数据一致性要求高** | MySQL 8.0 + 强一致复制 | 确保数据不丢失 |
| **快速原型开发** | SQLite | 零配置、启动快 |
| **单元测试** | SQLite（内存模式） | 快速、隔离性好 |

### 8.3 迁移建议

如果需要从 SQLite 迁移到 MySQL，建议按以下步骤：

1. **Schema 迁移**：
   - 将 SQLite 的动态类型转换为 MySQL 的严格类型
   - `INTEGER` → `BIGINT`
   - `TEXT` → `VARCHAR(n)` 或 `TEXT`
   - `REAL` → `DOUBLE` 或 `DECIMAL`
   - 添加 `NOT NULL`、`DEFAULT` 等约束

2. **数据迁移**：
   - 使用 `mysqldump` 或第三方工具
   - 注意字符集转换（使用 utf8mb4）

3. **代码调整**：
   - 更新数据源配置
   - 调整 SQL 语法差异（如自增关键字、日期函数）
   - 添加连接池配置
   - 考虑添加事务管理

4. **测试验证**：
   - 功能测试
   - 性能测试
   - 并发测试

---

## 9. MySQL 是否有嵌入式版本？开发/生产环境数据库切换方案

### 9.1 核心问题解答

#### 问题一：MySQL 是否有像 SQLite 一样的嵌入式版本？

**答案：没有官方的嵌入式版本。**

| 数据库 | 嵌入式支持 | 进程内运行 | 说明 |
| ------ | ---------- | ---------- | ---- |
| **SQLite** | ✅ 原生支持 | ✅ 运行在应用进程内 | 单文件数据库，零配置 |
| **MySQL** | ❌ 无官方支持 | ❌ 需要独立服务器进程 | MySQL Embedded 已废弃 |
| **H2** | ✅ 原生支持 | ✅ 运行在应用进程内 | 纯 Java，支持 MySQL 兼容模式 |
| **MariaDB4j** | ⚠️ 通过封装实现 | ⚠️ 启动独立进程 | 由应用管理 MariaDB 进程 |

**历史说明**：MySQL 曾经有 `MySQL Embedded Server` 版本（`libmysqld`），允许将 MySQL 引擎链接到应用程序中运行。但该功能在 MySQL 5.7 中已被弃用，并在 MySQL 8.0 中完全移除。

#### 问题二：为什么无法做到"开发用 SQLite，生产用 MySQL"？

**核心原因：SQL 语法和行为存在差异**

| 特性 | SQLite | MySQL 8.0 | 差异影响 |
| ---- | ------ | --------- | -------- |
| 自增主键 | `AUTOINCREMENT` | `AUTO_INCREMENT` | SQL 语法不同 |
| 字符串拼接 | `\|\|` | `CONCAT()` | SQL 语法不同 |
| 日期函数 | `DATE('now')` | `CURDATE()` | SQL 语法不同 |
| 数据类型 | 动态类型系统 | 严格类型系统 | 行为差异大 |
| 日期时间 | 无原生类型（TEXT/INTEGER） | 原生 `DATE/DATETIME` | 行为差异大 |
| 窗口函数 | 部分支持 | 完全支持 | 复杂查询不兼容 |
| 外键约束 | 默认不强制 | 默认强制 | 数据一致性不同 |
| 事务隔离 | 仅 2 种级别 | 4 种级别 | 并发行为不同 |
| JSON 类型 | 无（TEXT 存储） | 原生 `JSON` | 查询语法不同 |

**实际风险示例**：

```sql
-- SQLite：字符串拼接
SELECT 'Hello' || ' ' || 'World';

-- MySQL：相同的 SQL 会报错或返回 0（逻辑或操作）
SELECT 'Hello' || ' ' || 'World';  -- MySQL 中 || 是逻辑或！

-- MySQL 正确写法
SELECT CONCAT('Hello', ' ', 'World');
```

```sql
-- SQLite：自增主键
CREATE TABLE t (id INTEGER PRIMARY KEY AUTOINCREMENT);

-- MySQL：相同的 SQL 会报错
CREATE TABLE t (id INTEGER PRIMARY KEY AUTOINCREMENT);  -- 错误！

-- MySQL 正确写法
CREATE TABLE t (id INT PRIMARY KEY AUTO_INCREMENT);
```

### 9.2 解决方案

#### 方案一：H2 Database（MySQL 兼容模式）⭐ 推荐用于快速开发

**H2** 是一个纯 Java 编写的嵌入式数据库，可以运行在内存模式或文件模式。它支持 **MySQL 兼容模式**，可以模拟大部分 MySQL 语法。

**优势**：
- 启动极快
- 支持内存模式（测试后自动清理）
- 支持 MySQL 兼容模式

**劣势**：
- 不是 100% 语法兼容
- 部分高级特性不支持（存储过程、触发器等）

##### 依赖配置

```xml
<dependencies>
    <!-- H2 Database -->
    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
        <scope>runtime</scope>
    </dependency>
    
    <!-- Spring Boot JDBC/JPA -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-jdbc</artifactId>
    </dependency>
</dependencies>
```

##### 多环境配置

**开发环境**（`application-dev.properties`）：

```properties
# H2 数据库 - MySQL 兼容模式
spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MySQL;DATABASE_TO_LOWER=TRUE
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# H2 控制台（开发环境）
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

# 初始化数据库
spring.sql.init.mode=always
spring.sql.init.schema-locations=classpath:schema-h2.sql

# JPA 配置（如果使用 JPA）
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect
```

**生产环境**（`application-prod.properties`）：

```properties
# MySQL 8.0 生产环境
spring.datasource.url=jdbc:mysql://localhost:3306/mydb?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&characterEncoding=utf8mb4
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.username=root
spring.datasource.password=your_password

# HikariCP 连接池
spring.datasource.hikari.maximum-pool-size=10

# 初始化模式
spring.sql.init.mode=never

# JPA 配置
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect
```

**主配置**（`application.properties`）：

```properties
# 环境切换
spring.profiles.active=dev
# 生产环境设置为：spring.profiles.active=prod
```

##### H2 MySQL 兼容模式限制

| MySQL 特性 | H2 兼容情况 | 说明 |
| ---------- | ----------- | ---- |
| `AUTO_INCREMENT` | ✅ 支持 | 兼容 |
| `LIMIT` | ✅ 支持 | 兼容 |
| `IFNULL()` | ✅ 支持 | 兼容 |
| `CONCAT()` | ✅ 支持 | 兼容 |
| `DATE()`/`NOW()` | ✅ 支持 | 兼容 |
| 窗口函数 | ✅ 支持（H2 2.x） | 新版本完整支持 |
| `JSON` 类型 | ⚠️ 部分支持 | 可以存储但索引有限 |
| `GROUP BY` 扩展 | ⚠️ 差异 | MySQL 的 `ONLY_FULL_GROUP_BY` 行为不同 |
| 存储过程 | ❌ 不支持 | H2 不支持存储过程 |
| 触发器 | ⚠️ 语法不同 | 需要单独编写 |

#### 方案二：MariaDB4j（嵌入式 MariaDB）

**MariaDB4j** 是一个可以在 JVM 进程中启动 MariaDB（MySQL 的兼容分支）的库。它会：
1. 下载/解压 MariaDB 二进制文件
2. 启动一个独立的 MariaDB 进程
3. 应用结束时自动关闭

**优势**：
- 使用**真实的 MySQL 兼容数据库**
- SQL 语法 100% 兼容

**劣势**：
- 启动较慢
- 需要下载二进制文件
- 不是真正的"嵌入式"（启动独立进程）

##### 依赖配置

```xml
<dependencies>
    <!-- MariaDB4j -->
    <dependency>
        <groupId>ch.vorburger.mariaDB4j</groupId>
        <artifactId>mariaDB4j</artifactId>
        <version>3.0.1</version>
        <scope>test</scope>
    </dependency>
    
    <!-- MariaDB JDBC 驱动 -->
    <dependency>
        <groupId>org.mariadb.jdbc</groupId>
        <artifactId>mariadb-java-client</artifactId>
        <scope>runtime</scope>
    </dependency>
</dependencies>
```

##### 测试配置示例

```java
@SpringBootTest
public class StudentRepositoryTest {
    
    private static MariaDB4jService mariaDB;
    
    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        try {
            // 启动嵌入式 MariaDB
            DB db = DB.newEmbeddedDB(3307);
            db.start();
            
            // 创建数据库
            db.createDB("testdb");
            
            registry.add("spring.datasource.url", () -> 
                "jdbc:mariadb://localhost:3307/testdb?useSSL=false");
            registry.add("spring.datasource.username", () -> "root");
            registry.add("spring.datasource.password", () -> "");
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to start MariaDB4j", e);
        }
    }
    
    @Autowired
    private StudentRepository studentRepository;
    
    @Test
    void testSaveAndFind() {
        // 使用真实的 MariaDB 进行测试
        Student student = new Student();
        student.setName("张三");
        student.setMajor("计算机科学");
        
        Student saved = studentRepository.save(student);
        
        assertThat(saved.getId()).isNotNull();
        assertThat(studentRepository.findById(saved.getId())).isPresent();
    }
}
```

#### 方案三：Testcontainers（容器化数据库）⭐ 推荐用于集成测试

**Testcontainers** 是一个 Java 库，可以在测试时自动启动 Docker 容器中的真实数据库。

**优势**：
- 使用**真实的 MySQL 8.0**，SQL 语法 100% 兼容
- 测试环境与生产环境完全一致
- 测试结束后自动清理

**劣势**：
- 需要安装 Docker
- 测试启动较慢

##### 依赖配置

```xml
<dependencies>
    <!-- Testcontainers Core -->
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>testcontainers</artifactId>
        <version>1.19.3</version>
        <scope>test</scope>
    </dependency>
    
    <!-- Testcontainers MySQL -->
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>mysql</artifactId>
        <version>1.19.3</version>
        <scope>test</scope>
    </dependency>
    
    <!-- MySQL 驱动 -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>
    
    <!-- JUnit 5 -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

##### 完整测试示例

```java
package com.example.repository;

import com.example.entity.Student;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class StudentRepositoryTest {
    
    // 使用 MySQL 8.0 容器
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>(
        DockerImageName.parse("mysql:8.0.33")
    )
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test")
        .withInitScript("schema-mysql.sql")  // 可选：初始化脚本
        .withCommand("--character-set-server=utf8mb4", 
                    "--collation-server=utf8mb4_unicode_ci");
    
    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", mysql::getDriverClassName);
    }
    
    @Autowired
    private StudentRepository studentRepository;
    
    @Test
    void testSaveStudent() {
        // Given
        Student student = new Student();
        student.setName("张三");
        student.setAge(20);
        student.setGender("男");
        student.setMajor("计算机科学");
        
        // When
        Student saved = studentRepository.save(student);
        
        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("张三");
    }
    
    @Test
    void testFindById() {
        // Given
        Student student = new Student();
        student.setName("李四");
        student.setMajor("软件工程");
        Student saved = studentRepository.save(student);
        
        // When
        Optional<Student> found = studentRepository.findById(saved.getId());
        
        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("李四");
    }
    
    @Test
    void testFindByMajor() {
        // Given
        studentRepository.save(createStudent("学生1", "计算机科学"));
        studentRepository.save(createStudent("学生2", "计算机科学"));
        studentRepository.save(createStudent("学生3", "软件工程"));
        
        // When
        List<Student> csStudents = studentRepository.findByMajor("计算机科学");
        
        // Then
        assertThat(csStudents).hasSize(2);
    }
    
    private Student createStudent(String name, String major) {
        Student student = new Student();
        student.setName(name);
        student.setMajor(major);
        return student;
    }
}
```

#### 方案四：Docker Compose（开发环境）

对于本地开发，可以使用 Docker Compose 启动一个真实的 MySQL 容器。

**优势**：
- 使用真实的 MySQL 8.0
- 开发环境与生产环境一致
- 可以预加载测试数据

**`docker-compose.yml`**：

```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0.33
    container_name: mysql-dev
    ports:
      - "3306:3306"
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: mydb
      MYSQL_USER: dev
      MYSQL_PASSWORD: dev
    volumes:
      - mysql_data:/var/lib/mysql
      - ./init.sql:/docker-entrypoint-initdb.d/init.sql:ro
    command:
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_unicode_ci
      - --default-authentication-plugin=mysql_native_password
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5

volumes:
  mysql_data:
```

**`init.sql`**（初始化数据）：

```sql
CREATE TABLE IF NOT EXISTS student (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    age INT,
    gender VARCHAR(10),
    major VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS score (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    subject VARCHAR(50) NOT NULL,
    score DOUBLE,
    exam_date DATE,
    FOREIGN KEY (student_id) REFERENCES student(id)
);

-- 插入测试数据
INSERT INTO student (name, age, gender, major) VALUES
('张三', 20, '男', '计算机科学'),
('李四', 21, '女', '软件工程'),
('王五', 19, '男', '信息安全');
```

**开发配置**（`application-dev.properties`）：

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/mydb?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&characterEncoding=utf8mb4
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.username=dev
spring.datasource.password=dev

spring.datasource.hikari.maximum-pool-size=10
```

**使用方式**：

```bash
# 启动 MySQL 容器
docker-compose up -d

# 停止并删除容器（保留数据）
docker-compose down

# 完全清理（删除数据卷）
docker-compose down -v
```

### 9.3 方案对比与选择建议

| 方案 | 真实 MySQL | 启动速度 | 内存占用 | 适用场景 |
| ---- | ---------- | -------- | -------- | -------- |
| **H2 (MySQL 模式)** | ❌ 模拟 | ⚡ 极快 | 极小 | 单元测试、快速原型 |
| **MariaDB4j** | ✅ 真实（MariaDB） | 🐢 较慢 | 较大 | 集成测试 |
| **Testcontainers** | ✅ 真实 | 🐢 较慢 | 较大 | 集成测试、CI/CD |
| **Docker Compose** | ✅ 真实 | 🐢 较慢 | 较大 | 本地开发、手动测试 |

### 9.4 推荐策略

#### 策略一：H2 + Testcontainers 组合（推荐）

| 环境 | 数据库 | 理由 |
| ---- | ------ | ---- |
| **单元测试** | H2（内存模式） | 快速执行，隔离性好 |
| **集成测试** | Testcontainers (MySQL 8.0) | 使用真实数据库，确保兼容性 |
| **本地开发** | Docker Compose (MySQL 8.0) | 使用真实数据库，方便调试 |
| **生产环境** | MySQL 8.0 | 稳定可靠 |

#### 策略二：纯 Testcontainers（追求一致性）

| 环境 | 数据库 | 理由 |
| ---- | ------ | ---- |
| **所有测试** | Testcontainers (MySQL 8.0) | 100% 语法兼容 |
| **本地开发** | Docker Compose (MySQL 8.0) | 与测试环境一致 |
| **生产环境** | MySQL 8.0 | 稳定可靠 |

### 9.5 使用 ORM 层减少语法差异

**核心建议**：如果使用 JPA（Hibernate）或 MyBatis 这样的 ORM 框架，可以大幅减少 SQL 语法差异的影响。

#### JPA 示例

```java
@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    
    // JPA 自动生成 SQL，无需关心数据库差异
    List<Student> findByMajorAndAgeGreaterThan(String major, Integer age);
    
    Optional<Student> findFirstByNameOrderByIdDesc(String name);
    
    long countByGender(String gender);
    
    // 对于复杂查询，使用 JPQL（数据库无关）
    @Query("SELECT s FROM Student s WHERE s.age BETWEEN :min AND :max")
    List<Student> findByAgeRange(@Param("min") Integer minAge, @Param("max") Integer maxAge);
}
```

#### MyBatis 多数据库支持

**`mybatis-config.xml`**：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE configuration PUBLIC "-//mybatis.org//DTD Config 3.0//EN" 
    "http://mybatis.org/dtd/mybatis-3-config.dtd">
<configuration>
    <databaseIdProvider type="DB_VENDOR">
        <property name="MySQL" value="mysql"/>
        <property name="H2" value="h2"/>
        <property name="SQLite" value="sqlite"/>
    </databaseIdProvider>
</configuration>
```

**Mapper XML**：

```xml
<mapper namespace="com.example.mapper.StudentMapper">
    
    <!-- 通用 SQL -->
    <select id="findAll" resultType="Student">
        SELECT id, name, age, gender, major FROM student
    </select>
    
    <!-- MySQL 特定 SQL -->
    <select id="findByNameLike" resultType="Student" databaseId="mysql">
        SELECT * FROM student WHERE name LIKE CONCAT('%', #{name}, '%')
    </select>
    
    <!-- H2/SQLite 特定 SQL -->
    <select id="findByNameLike" resultType="Student" databaseId="h2">
        SELECT * FROM student WHERE name LIKE '%' || #{name} || '%'
    </select>
    
    <!-- SQLite 特定 SQL -->
    <select id="findByNameLike" resultType="Student" databaseId="sqlite">
        SELECT * FROM student WHERE name LIKE '%' || #{name} || '%'
    </select>
</mapper>
```

### 9.6 总结

#### 核心结论

1. **MySQL 没有官方嵌入式版本**：无法像 SQLite 一样运行在同一进程中
2. **语法差异确实存在**：SQLite 和 MySQL 的 SQL 语法有差异，无法无缝切换
3. **有替代方案**：
   - **H2 (MySQL 模式)**：适合快速测试，但语法不完全兼容
   - **MariaDB4j**：启动真实的 MariaDB 进程
   - **Testcontainers**：在 Docker 容器中启动真实的 MySQL 8.0 ⭐
   - **Docker Compose**：本地开发环境使用真实 MySQL

#### 最佳实践建议

**不要试图"开发用 SQLite，生产用 MySQL"**，因为：
1. SQL 语法差异会导致测试不充分
2. 数据类型行为不同（如日期、浮点数）
3. 事务和锁行为不同

**推荐的开发流程**：
```
开发环境：Docker Compose + MySQL 8.0
         ↓
单元测试：H2（内存模式）+ JPA
         ↓
集成测试：Testcontainers + MySQL 8.0
         ↓
生产环境：MySQL 8.0（主从/集群）
```

这样可以确保：
- 开发时使用真实的 MySQL 语法
- 单元测试快速执行
- 集成测试验证真实兼容性
- 生产环境稳定可靠

---

## 附录：MySQL 8.0 版本历史

| 版本 | 发布日期 | 主要特性 |
| ---- | -------- | -------- |
| **8.0.0** | 2016-09-12 | 首个开发者里程碑版本 |
| **8.0.11** | 2018-04-19 | 首个 GA 版本 |
| **8.0.13** | 2018-10-22 | 函数索引、降序索引增强 |
| **8.0.14** | 2019-01-21 | CTE 递归增强 |
| **8.0.16** | 2019-04-25 | 直方图统计信息 |
| **8.0.18** | 2019-10-14 | EXPLAIN ANALYZE |
| **8.0.20** | 2020-04-27 | 二进制日志压缩 |
| **8.0.22** | 2020-10-19 | InnoDB 表空间加密增强 |
| **8.0.23** | 2021-01-18 | 异步连接故障转移 |
| **8.0.27** | 2021-10-19 | 认证机制增强 |
| **8.0.28** | 2022-01-18 | 性能优化 |
| **8.0.30** | 2022-07-26 | MySQL 8.0 长期支持版本 |
| **8.0.33** | 2023-04-18 | 最新稳定版本（LTS） |

**MySQL 8.0 支持生命周期**：
- 常规支持：2018-04-19 到 2025-04-01
- 扩展支持：2025-04-01 到 2028-04-01

---

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

---

## API 接口

启动应用后，可访问以下 API：

### 学生 API (`http://localhost:8080/api/students`)

| 方法 | 路径 | 说明 |
| ---- | ---- | ---- |
| GET | `/api/students` | 获取所有学生 |
| GET | `/api/students/{id}` | 根据 ID 获取学生 |
| POST | `/api/students` | 创建学生 |
| PUT | `/api/students/{id}` | 更新学生 |
| DELETE | `/api/students/{id}` | 删除学生 |

### 成绩 API (`http://localhost:8080/api/scores`)

| 方法 | 路径 | 说明 |
| ---- | ---- | ---- |
| GET | `/api/scores` | 获取所有成绩 |
| GET | `/api/scores/{id}` | 根据 ID 获取成绩 |
| GET | `/api/scores/student/{studentId}` | 根据学生 ID 获取成绩 |
| POST | `/api/scores` | 创建成绩 |
| PUT | `/api/scores/{id}` | 更新成绩 |
| DELETE | `/api/scores/{id}` | 删除成绩 |

---

## 快速开始

### 使用 SQLite（本项目默认）

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

### 切换到 MySQL 8.0

1. **修改 pom.xml，添加 MySQL 依赖**：
   ```xml
   <dependency>
       <groupId>com.mysql</groupId>
       <artifactId>mysql-connector-j</artifactId>
       <scope>runtime</scope>
   </dependency>
   ```

2. **修改 application.properties**：
   ```properties
   # MySQL 8.0 配置
   spring.datasource.url=jdbc:mysql://localhost:3306/mydb?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&characterEncoding=utf8mb4
   spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
   spring.datasource.username=root
   spring.datasource.password=your_password
   
   # 连接池配置
   spring.datasource.hikari.maximum-pool-size=10
   
   # 初始化模式
   spring.sql.init.mode=never
   ```

3. **创建 MySQL 数据库**：
   ```sql
   CREATE DATABASE mydb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

4. **运行应用**：
   ```bash
   mvn spring-boot:run
   ```