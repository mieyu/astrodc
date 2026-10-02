# 后端配置

数据库密码、DeepSeek API 密钥和网站访问密钥通过环境变量配置：

| 环境变量 | 用途 |
| --- | --- |
| `ASTRONOMY_DB_PASSWORD` | MySQL 数据库密码 |
| `DEEPSEEK_API_KEY` | 学术 AI 助手的 API 密钥 |
| `ASTRONOMY_ACCESS_KEY` | 网站解锁密钥及访问 Cookie 签名 |
| `ASTRONOMY_FILE_ROOT` | 观测图像根目录，默认 `E:/Observated_images` |
| `ASTRONOMY_REPORT_ROOT` | 观测报告根目录，默认 `E:/Observation_reports` |
| `ASTRONOMY_ANALYSIS_TABLE` | AI 查询数据表，默认 `observation_image.total` |

本机也可在进程工作目录下创建 `config/application-private.yml`；应用通过
`spring.config.import` 自动加载它。该文件被 Git 忽略，不随源码提交或打包，
部署到其他机器时需要单独配置。示例：

```yaml
spring:
  datasource:
    password: ${ASTRONOMY_DB_PASSWORD:replace-with-local-password}
deepseek:
  api-key: ${DEEPSEEK_API_KEY:replace-with-local-api-key}
access:
  key: ${ASTRONOMY_ACCESS_KEY:replace-with-local-access-key}
```

不要提交实际凭据。访问密钥未配置时，解锁请求会被拒绝。
