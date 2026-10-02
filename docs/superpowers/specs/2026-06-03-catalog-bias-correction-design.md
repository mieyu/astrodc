# Catalog Bias Correction Design

## Goal

新增“星表偏差修正表”模块，将 `C:\Users\Lenovo\Downloads\master_nside64.csv` 一次性导入 MySQL，并在前端“数据存储 > 星表偏差修正表 > 数据展示”页面展示、筛选和导出。

## Data Source

CSV 文件包含 49152 行数据和 69 列：

- `ipix`：HEALPix 像素编号。
- 17 个星表：`ac`、`acrs`、`act`、`agk1`、`agk3`、`fk4catalogue`、`gaiadr1`、`gaiadr2`、`gsc1_2`、`hipparcos`、`ppm`、`sao`、`tycho2`、`ucac2`、`ucac4`、`usno_a2`、`yale`。
- 每个星表 4 个偏差字段：`dra_mas`、`ddec_mas`、`pmra_masyr`、`pmdec_masyr`。

空单元格导入为 `NULL`。

## Database

新表名：`catalog_bias_correction`。

表结构采用宽表，字段与 CSV 表头一一对应：

- `ipix bigint NOT NULL`，作为主键。
- 其他 68 个偏差字段使用 `double NULL`。
- 表字符集使用项目现有 MySQL 默认风格：`utf8mb4`。

选择宽表的原因：

- 与 CSV 原始结构一致，导入和导出最直接。
- 查询一行即可得到某个 `ipix` 的全部星表偏差。
- 前端可复用现有“规范后定位结果”的宽表展示模式。
- 当前数据量 49152 行，宽表分页查询压力可控。

不采用长表的原因：长表会膨胀为 835584 行，虽然便于按星表筛选，但不适合原始 CSV 级别的全字段展示。

## Backend

新增模块命名：

- Entity：`CatalogBiasCorrection`
- DTO：`CatalogBiasCorrectionQuery`
- Mapper：`CatalogBiasCorrectionMapper`
- Service：`CatalogBiasCorrectionService`
- Controller：`CatalogBiasCorrectionController`
- XML：`CatalogBiasCorrectionMapper.xml`

接口路径：`/api/catalog-bias-correction`

接口：

- `POST /search`：分页查询，参数包括 `page`、`pageSize`、`sortField`、`sortOrder`，请求体支持 `ipixMin`、`ipixMax` 和可选导出列。
- `POST /export`：按当前筛选条件导出 CSV，列顺序保持 CSV 原始列顺序。
- `GET /columns`：返回列定义和星表分组，供前端生成表格和详情视图。

查询规则：

- `page` 最小为 1。
- `pageSize` 默认 50，最大 1000。
- 排序字段必须在白名单内，不合法时回退到 `ipix`。
- 导出列必须在白名单内，不合法字段会被忽略；若最终为空，则导出全部列。

## Frontend

新增组件：`CatalogBiasCorrection.vue`。

更新路由：

- `/data/catalog-bias/display` 使用新组件，不再使用 `Placeholder`。

页面能力：

- 标题为“星表偏差修正表”。
- 筛选栏支持 `ipix` 起止范围。
- 支持分页、排序、重置、导出 CSV。
- “概览视图”默认展示 `ipix` 和少量常用星表字段。
- “字段分组”按 17 个星表切换，每组显示该星表的 4 个偏差字段。
- 详情抽屉展示全行数据，按星表分组。
- 交互、布局和 Element UI 风格参考 `NormalizedPositioning.vue`。

## Import

导入为一次性操作，不在前端或后端暴露上传入口。

步骤：

1. 在 MySQL 数据库 `satellite_data_center` 中创建 `catalog_bias_correction` 表。
2. 将 `master_nside64.csv` 导入该表。
3. 验证行数为 49152。
4. 抽样验证 `ipix=0` 等记录中的空值和数值字段。

## Testing

后端：

- 为查询条件构建、排序白名单、导出列过滤、CSV 值格式化添加单元测试。
- 运行 Maven 测试或至少运行新测试类。

前端：

- 运行 `npm run build`。
- 启动前端后用浏览器检查页面能打开、表格非空、分页和分组切换可用。

## Error Handling

- 后端接口统一返回项目现有 `Result` 格式。
- 导出接口直接写 CSV 响应，异常时尽量返回 500 JSON。
- 前端接口失败时使用 Element UI message 提示。

## Scope Boundaries

本次不做：

- 前端 CSV 上传或重复导入功能。
- 对偏差数据做业务计算或二次转换。
- 新增权限控制。
- 改动现有 `positioning_normalized`、`image_own`、`site_paper` 表结构。
