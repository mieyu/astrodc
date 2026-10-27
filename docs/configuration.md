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

## 自有图像批量下载

查询结果通过“批量下载原始 FITS”统一进入下载弹窗，在弹窗内按 DATE-OBS 原值的日期、目标、望远镜筛选和选择数据。
默认取目标当日全部文件，可改为仅下载本次查询匹配的文件；校准选项加入同日同望远镜的 BIAS、FLAT、DARK。
日期不进行时区换算；缺少望远镜的记录不自动匹配校准文件。缺少日期或目标的记录可使用单文件下载。
下载前核对实际文件数量、原始总大小和缺失项；部分缺失需要确认后才能下载，其余数据不会被自动替换。

ZIP 保留存储根目录内的相对目录，文件内容逐字节保持原样；同名不同目录的文件均保留，同一物理路径只加入一次。
下载直接从磁盘流式传输到浏览器，支持 ZIP64，不生成完整临时压缩包，也不将整包放入浏览器内存。
准备结果保存在当前 Java 进程内，30 分钟有效；服务重启、准备过期或文件变化后需重新核对。
“导出信息 CSV”只导出查询结果的元数据，不包含 FITS 像素。
