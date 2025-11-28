# Readiness 部分可用状态实现文档

## 概述

本文档描述了 Readiness 健康检查的回退策略实现，支持"部分可用"（DEGRADED）状态。

## 设计原理

### 服务分类

系统将依赖服务分为两类：

1. **关键服务（Critical Services）**
    - 这些服务对系统运行至关重要
    - 如果关键服务故障，整个系统状态为 DOWN
    - 示例：数据库（Postgres）、缓存（Redis）

2. **非关键服务（Non-Critical Services）**
    - 这些服务故障时，系统可以降级运行
    - 如果仅非关键服务故障，系统状态为 DEGRADED
    - 示例：消息队列（RabbitMQ）、MongoDB、外部 API

### 健康状态逻辑

| 关键服务状态   | 非关键服务状态 | 最终状态 | HTTP 状态码 |
| -------------- | -------------- | -------- | ----------- |
| 全部 UP        | 全部 UP        | UP       | 200         |
| 全部 UP        | 部分/全部 DOWN | DEGRADED | 200         |
| 部分/全部 DOWN | 任意           | DOWN     | 503         |

## 配置说明

### application.yaml 配置

```yaml
health-check:
  critical-services:
    - postgres
    - redis
  non-critical-services:
    - rabbitmq
    - mongodb
    - mockWebServer

management:
  endpoint:
    health:
      status:
        http-mapping:
          DEGRADED: 200
      group:
        readiness:
          include:
            - degradedReadiness
          status:
            # 定义状态顺序：DOWN < DEGRADED < UP
            # 这样 Spring Boot 才能正确识别和聚合 DEGRADED 状态
            order: DOWN, DEGRADED, UP
```

## 使用场景

### 场景 1：全部正常

**条件**：所有服务都正常

**响应示例**：

```json
{
  "status": "UP",
  "components": {
    "readiness": {
      "status": "UP",
      "details": {
        "reason": "All services are up",
        "criticalServicesUp": "2/2",
        "nonCriticalServicesUp": "3/3",
        "services": {
          "postgres": "UP",
          "redis": "UP",
          "rabbitmq": "UP",
          "mongodb": "UP",
          "mockWebServer": "UP"
        }
      }
    }
  }
}
```

**HTTP 状态码**：200 OK

### 场景 2：部分可用（非关键服务故障）

**条件**：Postgres 和 Redis 正常，但 RabbitMQ 故障

**响应示例**：

```json
{
  "status": "DEGRADED",
  "components": {
    "readiness": {
      "status": "DEGRADED",
      "details": {
        "reason": "Non-critical services are down, but system is partially available",
        "criticalServicesUp": "2/2",
        "nonCriticalServicesUp": "2/3",
        "services": {
          "postgres": "UP",
          "redis": "UP",
          "rabbitmq": "DOWN",
          "mongodb": "UP",
          "mockWebServer": "UP"
        }
      }
    }
  }
}
```

**HTTP 状态码**：200 OK（系统仍可接收流量）

### 场景 3：不可用（关键服务故障）

**条件**：Postgres 故障

**响应示例**：

```json
{
  "status": "DOWN",
  "components": {
    "readiness": {
      "status": "DOWN",
      "details": {
        "reason": "Critical services are down",
        "criticalServicesUp": "1/2",
        "nonCriticalServicesUp": "3/3",
        "services": {
          "postgres": "DOWN",
          "redis": "UP",
          "rabbitmq": "UP",
          "mongodb": "UP",
          "mockWebServer": "UP"
        }
      }
    }
  }
}
```

**HTTP 状态码**：503 Service Unavailable

## Kubernetes 集成

### 配置 Readiness Probe

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: health-check-app
spec:
  containers:
    - name: app
      image: health-check:latest
      readinessProbe:
        httpGet:
          path: /actuator/health/readiness
          port: 8080
        initialDelaySeconds: 10
        periodSeconds: 5
        failureThreshold: 3
```

### 行为说明

- **UP 状态**：Pod 接收流量
- **DEGRADED 状态**：Pod 继续接收流量（降级服务）
- **DOWN 状态**：Pod 从 Service 中移除，不再接收流量

## 优势

1. **渐进式降级**：系统可以在部分依赖故障时继续提供服务
2. **更好的可用性**：避免因非关键服务故障导致整个系统不可用
3. **灵活配置**：可以根据业务需求调整关键和非关键服务
4. **透明度**：健康检查响应包含详细的服务状态信息

## 测试

### 测试全部正常

```bash
docker-compose up -d

curl http://localhost:8080/actuator/health/readiness
```

### 测试部分可用

```bash
# 停止非关键服务（如 RabbitMQ）
docker-compose stop rabbitmq

# 检查健康状态（应该返回 DEGRADED）
curl http://localhost:8080/actuator/health/readiness
```

### 测试不可用

```bash
# 停止关键服务（如 Postgres）
docker-compose stop postgres

# 检查健康状态（应该返回 DOWN）
curl http://localhost:8080/actuator/health/readiness
```

## 扩展

### 添加新服务

1. 创建新的 `HealthIndicator`
2. 在 `ReadinessHealthIndicator` 构造函数中注入
3. 在 `application.yaml` 中配置为关键或非关键服务

### 自定义状态映射

如果需要将 DEGRADED 状态映射到不同的 HTTP 状态码：

```yaml
management:
  endpoint:
    health:
      status:
        http-mapping:
          DEGRADED: 503  # 或其他状态码
```

### 添加监控告警

建议在监控系统中为 DEGRADED 状态设置告警，以便及时发现和修复非关键服务的问题。
