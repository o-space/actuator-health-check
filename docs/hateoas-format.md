# HATEOAS 格式说明

## 为什么数据在 `_embedded` 里？

Spring HATEOAS 默认使用 **HAL (Hypertext Application Language)** 格式来序列化资源。这是 HAL 规范的标准格式。

### HAL 格式结构

#### 单个资源（EntityModel）
```json
{
  "id": 1,
  "serviceName": "postgres",
  "status": "UP",
  "_links": {
    "self": { "href": "/api/health-checks/1" },
    "service-health-checks": { "href": "/api/services/postgres/health-checks" }
  }
}
```

#### 集合资源（CollectionModel）
```json
{
  "_embedded": {
    "healthCheckRecordList": [
      {
        "id": 1,
        "serviceName": "postgres",
        "status": "UP",
        "_links": {
          "self": { "href": "/api/health-checks/1" }
        }
      }
    ]
  },
  "_links": {
    "self": { "href": "/api/health-checks" }
  }
}
```

### 为什么使用 `_embedded`？

1. **HAL 规范要求**：HAL 规范定义了 `_embedded` 用于包含嵌入的相关资源
2. **区分资源类型**：
   - `_links`：超媒体链接
   - `_embedded`：嵌入的相关资源
   - 其他字段：资源本身的数据
3. **标准化**：遵循 HAL 规范，客户端可以统一处理
4. **可扩展性**：可以在同一响应中嵌入多种类型的资源

### 自定义 `_embedded` 字段名

默认情况下，Spring HATEOAS 会根据类型名生成字段名（如 `healthCheckRecordList`）。

可以使用 `@Relation` 注解自定义：

```java
import org.springframework.hateoas.server.core.Relation;

@Relation(collectionRelation = "healthChecks", itemRelation = "healthCheck")
@Entity
@Table(name = "health_check_records")
public class HealthCheckRecord {
    // ...
}
```

这样 `_embedded` 中的字段名就会变成 `healthChecks` 而不是 `healthCheckRecordList`。

### 禁用 HAL 格式

如果不想使用 HAL 格式，可以在 `application.yaml` 中配置：

```yaml
spring:
  hateoas:
    use-hal-as-default-json-media-type: false
```

**注意**：禁用 HAL 后，响应将不包含 `_links` 和 `_embedded`，失去 HATEOAS 功能。

### 推荐做法

- **保持 HAL 格式**：这是 RESTful API 的最佳实践
- **使用 `@Relation`**：自定义更友好的字段名
- **客户端适配**：客户端应该理解 HAL 格式并正确处理 `_embedded` 和 `_links`
