# 历史观测报告

页面入口：网站服务 → 观测 → 历史观测报告。

## 文件管理

默认存储目录独立于观测图像目录：

```text
E:/Observation_reports/
├── 云南天文台/
└── 兴隆观测基地/
    └── 2026年9月12日至14日BFOSC天然卫星观测纲要及记.docx
```

将文档复制到对应台站文件夹，网页刷新列表后即可看到，无需重启服务或重新部署。
允许按年份等再建子文件夹（最多 9 层），列表会合并展示，并标注子文件夹路径。
同名文档可放在不同子文件夹中。文件列表按最后修改时间倒序排列；此时间不是观测日期。
Word 临时文件（`~$` 开头）以及非文档文件不会出现在列表中。
建议复制完成后再将临时扩展名改为正式扩展名，避免用户读取尚未传完的文件。

支持 `.docx`、`.pdf`、UTF-8 编码的 `.md` / `.markdown` 在线预览。
旧版 `.doc` 可下载；需要先用 Word 另存为 `.docx` 才能在线预览。
大于 50 MB 的文档只提供下载，以免浏览器内存占用过高。
Word 为快速阅读预览，复杂排版可能与 Word 软件略有不同。Markdown 预览不加载外部资源。
所有下载返回原文件字节，不转换格式、不压缩内容。

可以通过环境变量 `ASTRONOMY_REPORT_ROOT` 或 Spring 配置
`observation-reports.root-path` 更改根目录，重启后生效。
报告文件应随服务器数据定期备份，不应放入前端 dist 或 Git 仓库。

## 只读接口

- `GET /api/observation-reports/stations`：台站名称与文档数量。
- `GET /api/observation-reports/{station}`：台站文档列表，station 为 `yunnan` 或 `xinglong`。
- `GET /api/observation-reports/{station}/file?path=...`：原文件，用于浏览器预览和下载；path 为编码后的站内相对路径。

接口沿用网站现有访问密钥和 Cookie 校验，无网页上传、删除接口。
文件路径检查禁止跨台站、根目录外访问和外部文件链接。

## 验证

```powershell
.\mvnw.cmd '-DskipTests=false' '-Dtest=ObservationReportServiceTest,ObservationReportControllerTest' test
```

覆盖台站分类、目录实时更新、中文名称、子文件夹、临时文件过滤、原始下载字节以及非法路径拒绝。
