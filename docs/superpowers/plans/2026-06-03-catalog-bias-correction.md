# Catalog Bias Correction Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the catalog bias correction data module, import `master_nside64.csv` into MySQL, and expose it through the existing Vue data-storage UI.

**Architecture:** Use a MySQL wide table named `catalog_bias_correction` matching the CSV header. Add a Spring Boot/MyBatis-Plus module for paginated search, column metadata, and CSV export. Add one Vue 2 + Element UI page at the existing `/data/catalog-bias/display` route.

**Tech Stack:** Java 17, Spring Boot 3.5, MyBatis-Plus, MySQL 8, JUnit 5, Vue 2, Element UI, Axios.

---

### Task 1: Backend Column Metadata

**Files:**
- Create: `src/main/java/mie/astronomy/common/CatalogBiasCorrectionColumns.java`
- Test: `src/test/java/mie/astronomy/common/CatalogBiasCorrectionColumnsTest.java`

- [ ] **Step 1: Write the failing test**

Create tests for the column count, primary column, grouped catalog names, export column filtering, and invalid sort rejection.

- [ ] **Step 2: Run test to verify it fails**

Run: `.\mvnw.cmd -DskipTests=false -Dtest=CatalogBiasCorrectionColumnsTest test`
Expected: compilation failure because `CatalogBiasCorrectionColumns` does not exist.

- [ ] **Step 3: Implement metadata utility**

Create `CatalogBiasCorrectionColumns` with:
- `ALL_COLUMNS`
- `CATALOGS`
- `GROUPS`
- `COL_TO_FIELD`
- `sanitizeColumns(List<String>)`
- `isKnownColumn(String)`
- `isSortableColumn(String)`

- [ ] **Step 4: Run test to verify it passes**

Run: `.\mvnw.cmd -DskipTests=false -Dtest=CatalogBiasCorrectionColumnsTest test`
Expected: PASS.

### Task 2: Backend API

**Files:**
- Create: `src/main/java/mie/astronomy/entity/CatalogBiasCorrection.java`
- Create: `src/main/java/mie/astronomy/dto/CatalogBiasCorrectionQuery.java`
- Create: `src/main/java/mie/astronomy/mapper/CatalogBiasCorrectionMapper.java`
- Create: `src/main/resources/mapper/CatalogBiasCorrectionMapper.xml`
- Create: `src/main/java/mie/astronomy/service/CatalogBiasCorrectionService.java`
- Create: `src/main/java/mie/astronomy/service/Impl/CatalogBiasCorrectionServiceImpl.java`
- Create: `src/main/java/mie/astronomy/controller/CatalogBiasCorrectionController.java`

- [ ] **Step 1: Write service tests**

Add focused unit tests for sort fallback, page-size clamping, and column metadata payload shape where practical without a live database.

- [ ] **Step 2: Implement minimal API**

Implement search, columns, and export using existing `PositioningNormalized` patterns.

- [ ] **Step 3: Run backend tests**

Run: `.\mvnw.cmd -DskipTests=false -Dtest=CatalogBiasCorrectionColumnsTest test`
Expected: PASS.

### Task 3: Database Import

**Files:**
- Create table directly in MySQL database `satellite_data_center`.

- [ ] **Step 1: Create table**

Create `catalog_bias_correction` with `ipix BIGINT PRIMARY KEY` and 68 nullable `DOUBLE` columns matching the CSV header.

- [ ] **Step 2: Import CSV**

Use `LOAD DATA LOCAL INFILE` from `C:\Users\Lenovo\Downloads\master_nside64.csv`, mapping empty strings to `NULL`.

- [ ] **Step 3: Verify import**

Run SQL count and sample checks:
- `SELECT COUNT(*) FROM catalog_bias_correction;`
- `SELECT * FROM catalog_bias_correction WHERE ipix = 0;`

Expected: 49152 rows.

### Task 4: Frontend Page

**Files:**
- Create: `src/components/CatalogBiasCorrection.vue`
- Modify: `src/router/index.js`

- [ ] **Step 1: Add component**

Build an Element UI page with `ipix` range filters, paginated table, grouped tabs, details drawer, copy JSON, and CSV export.

- [ ] **Step 2: Wire route**

Import `CatalogBiasCorrection` and assign it to `/data/catalog-bias/display`.

- [ ] **Step 3: Build frontend**

Run: `npm run build`
Expected: compiled successfully.

### Task 5: End-to-End Verification

**Files:**
- No new source files.

- [ ] **Step 1: Build backend**

Run: `.\mvnw.cmd -DskipTests=false -Dtest=CatalogBiasCorrectionColumnsTest test`
Expected: PASS.

- [ ] **Step 2: Start or verify services**

Start backend on port `8088` if not already running. Start frontend dev server if needed.

- [ ] **Step 3: Browser check**

Open `/data/catalog-bias/display`, verify that the page renders, records load, grouped columns switch, and export starts.
