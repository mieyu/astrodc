<template>
  <main class="observation-reports">
    <section class="ui-card reports-header">
      <h1>历史观测报告</h1>
      <p>按台站查阅观测纲要、观测记录与历史报告，支持 Word、Markdown 和 PDF 快速预览。</p>
    </section>
    <el-alert v-if="stationsError" :title="stationsError" type="error" show-icon :closable="false" />
    <div v-if="!activeStation" class="station-grid" v-loading="stationsLoading">
      <button v-for="station in stations" :key="station.id" class="station-card" @click="selectStation(station.id)">
        <i class="el-icon-office-building station-icon" aria-hidden="true"></i>
        <span class="station-info"><strong>{{ station.name }}</strong><span>{{ station.count }} 份文档</span></span>
        <i class="el-icon-arrow-right" aria-hidden="true"></i>
      </button>
    </div>
    <section v-else class="ui-card station-documents">
      <div class="station-heading">
        <div><el-button type="text" icon="el-icon-back" @click="selectStation('')">全部台站</el-button><h2>{{ activeStation.name }}</h2></div>
        <el-button icon="el-icon-refresh" :loading="loading" @click="refresh">刷新列表</el-button>
      </div>
      <div class="report-filters">
        <el-input v-model="search" placeholder="搜索文档名称" prefix-icon="el-icon-search" clearable aria-label="搜索文档名称" />
        <el-select v-model="type" aria-label="文档类型">
          <el-option label="全部格式" value="" /><el-option label="Word" value="word" />
          <el-option label="PDF" value="pdf" /><el-option label="Markdown" value="markdown" />
        </el-select>
        <span class="report-count">{{ filteredReports.length }} 份文档</span>
      </div>
      <el-alert v-if="listError" :title="listError" type="error" show-icon :closable="false" />
      <div v-loading="loading" class="report-list">
        <article v-for="report in visibleReports" :key="report.path" class="report-row">
          <span class="report-format" :class="report.type">{{ formatLabel(report.type) }}</span>
          <div class="report-info">
            <button class="report-name" @click="preview(report)">{{ report.name }}</button>
            <p>{{ formatSize(report.size) }}<span> · 更新于 {{ formatDate(report.modifiedAt) }}</span></p>
            <p v-if="report.path.includes('/')" class="report-directory">{{ report.path.slice(0, report.path.lastIndexOf('/')) }}</p>
          </div>
          <div class="report-actions">
            <el-button size="small" icon="el-icon-view" @click="preview(report)">预览</el-button>
            <el-button size="small" type="primary" plain icon="el-icon-download" :loading="downloading === report.path" @click="download(report)">下载</el-button>
          </div>
        </article>
        <el-empty v-if="!loading && !listError && !filteredReports.length" :description="search || type ? '没有符合条件的文档' : '该台站暂未收录观测报告'" :image-size="80" />
      </div>
      <el-pagination v-if="filteredReports.length > 20" :current-page.sync="page" :page-size="20" :total="filteredReports.length" :pager-count="5" layout="prev, pager, next" />
    </section>
    <el-button v-if="stationsError && !activeStation" icon="el-icon-refresh" @click="loadStations">重新加载</el-button>
    <el-dialog :visible.sync="previewVisible" :fullscreen="isMobile" width="92%" top="4vh" append-to-body custom-class="report-preview-dialog" :close-on-click-modal="false" @closed="selected = null">
      <div slot="title" class="preview-heading"><strong>{{ selected && selected.name }}</strong></div>
      <template v-if="selected && previewVisible">
        <div class="preview-actions"><span>{{ formatLabel(selected.type) }} · {{ formatSize(selected.size) }}</span><el-button size="small" type="primary" icon="el-icon-download" :loading="downloading === selected.path" @click="download(selected)">下载原文件</el-button></div>
        <ReportPreview :key="selected.path" :report="selected" :url="fileUrl(selected)" />
      </template>
    </el-dialog>
  </main>
</template>

<script>
import axios from 'axios'
import mobileViewport from '../mixins/mobileViewport'
import ReportPreview from './ReportPreview.vue'

const stationDefaults = [{ id: 'yunnan', name: '云南天文台', count: 0 }, { id: 'xinglong', name: '兴隆观测基地', count: 0 }]
export default {
  name: 'ObservationReports', components: { ReportPreview }, mixins: [mobileViewport],
  data: () => ({ stations: stationDefaults, stationsLoading: false, stationsError: '', reports: [], loading: false,
    listError: '', search: '', type: '', page: 1, selected: null, previewVisible: false, downloading: '' }),
  computed: {
    activeStation() { return this.stations.find(s => s.id === this.$route.query.station) },
    filteredReports() {
      const search = this.search.trim().toLowerCase()
      return this.reports.filter(r => r.name.toLowerCase().includes(search) && (!this.type ||
        (this.type === 'word' ? ['doc', 'docx'].includes(r.type) : this.type === 'markdown' ? ['md', 'markdown'].includes(r.type) : r.type === this.type)))
    },
    visibleReports() { return this.filteredReports.slice((this.page - 1) * 20, this.page * 20) }
  },
  watch: {
    '$route.query.station': { immediate: true, handler() { this.search = ''; this.type = ''; this.previewVisible = false; this.loadReports() } },
    search() { this.page = 1 }, type() { this.page = 1 }
  },
  mounted() { this.loadStations() },
  beforeDestroy() { this._listRequest = (this._listRequest || 0) + 1 },
  methods: {
    selectStation(station) { this.$router.push({ path: this.$route.path, query: station ? { station } : {} }) },
    async loadStations() {
      this.stationsLoading = true; this.stationsError = ''
      try { const { data } = await axios.get('/api/observation-reports/stations'); if (data.code !== 1) throw new Error(); this.stations = data.data }
      catch (_) { this.stationsError = '台站信息加载失败，请重试。' }
      finally { this.stationsLoading = false }
    },
    async loadReports() {
      const request = this._listRequest = (this._listRequest || 0) + 1
      this.reports = []; this.page = 1; this.listError = ''; this.loading = false
      if (!this.activeStation) return
      this.loading = true
      try {
        const { data } = await axios.get(`/api/observation-reports/${this.activeStation.id}`)
        if (data.code !== 1) throw new Error()
        if (request === this._listRequest) this.reports = data.data
      } catch (_) { if (request === this._listRequest) this.listError = '文档列表加载失败，请点击刷新列表重试。' }
      finally { if (request === this._listRequest) this.loading = false }
    },
    refresh() { this.loadReports(); this.loadStations() },
    fileUrl(report) { return `/api/observation-reports/${this.activeStation.id}/file?path=${encodeURIComponent(report.path)}` },
    preview(report) { this.selected = report; this.previewVisible = true },
    async download(report) {
      if (this.downloading) return
      this.downloading = report.path
      try {
        const { data } = await axios.get(this.fileUrl(report), { responseType: 'blob' })
        const url = URL.createObjectURL(data)
        const link = document.createElement('a'); link.href = url; link.download = report.name
        document.body.appendChild(link); link.click(); link.remove(); setTimeout(() => URL.revokeObjectURL(url), 1000)
      } catch (_) { this.$message.error('下载失败，文档可能已移除，请刷新后重试。') }
      finally { this.downloading = '' }
    },
    formatLabel(type) { return ({ docx: 'Word', doc: 'Word', pdf: 'PDF', md: 'MD', markdown: 'MD' })[type] || type },
    formatSize(size) { return size < 1024 ? `${size} B` : size < 1048576 ? `${(size / 1024).toFixed(1)} KB` : `${(size / 1048576).toFixed(1)} MB` },
    formatDate(value) { return new Date(value).toLocaleDateString('zh-CN') }
  }
}
</script>

<style scoped>
.observation-reports { padding: 24px 32px 40px; width: 100%; box-sizing: border-box; min-width: 0; }
.reports-header { margin-bottom: 20px; }
.reports-header h1 { margin: 0 0 12px; font-size: 26px; }
.reports-header p { color: var(--text-regular); margin: 0; line-height: 1.8; }
.station-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 20px; min-height: 120px; }
.station-card { display: flex; align-items: center; gap: 18px; background: white; border: 1px solid var(--border); border-radius: 10px; padding: 30px 24px; text-align: left; color: var(--text-strong); cursor: pointer; font: inherit; }
.station-card:hover, .station-card:focus-visible { border-color: var(--brand); background: var(--brand-soft); }
.station-icon { font-size: 30px; color: var(--brand); background: var(--brand-soft); padding: 16px; border-radius: 10px; }
.station-info { display: flex; flex-direction: column; flex: 1; gap: 10px; }
.station-info strong { font-size: 20px; }.station-info span { color: var(--text-muted); font-size: 14px; }
.station-heading, .report-filters, .preview-actions { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.station-heading h2 { margin: 0 0 20px; font-size: 22px; }
.report-filters { margin-bottom: 16px; justify-content: flex-start; }
.report-filters .el-input { max-width: 400px; }.report-filters .el-select { width: 150px; flex-shrink: 0; }
.report-count { margin-left: auto; color: var(--text-muted); white-space: nowrap; font-size: 14px; }
.report-list { min-height: 130px; }.report-row { display: flex; align-items: center; gap: 16px; padding: 20px 0; border-bottom: 1px solid var(--border); }
.report-format { background: #edf4fb; color: var(--brand); font-size: 12px; font-weight: 600; width: 50px; height: 56px; border-radius: 6px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.report-format.pdf { color: #ad4940; background: #fff1ef; }.report-format.md, .report-format.markdown { color: #327061; background: #edf7f3; }
.report-info { min-width: 0; flex: 1; }.report-name { border: 0; background: none; padding: 0; font: inherit; font-size: 16px; text-align: left; color: var(--brand); cursor: pointer; overflow-wrap: anywhere; line-height: 1.6; }
.report-name:hover { text-decoration: underline; }.report-info p { font-size: 13px; color: var(--text-muted); margin: 6px 0 0; }.report-directory { overflow-wrap: anywhere; }
.report-actions { display: flex; flex-shrink: 0; }.el-pagination { margin-top: 20px; text-align: center; }
.preview-heading { padding-right: 24px; overflow-wrap: anywhere; line-height: 1.5; }.preview-actions { margin-bottom: 12px; }.preview-actions span { color: var(--text-muted); font-size: 13px; }
@media (max-width: 768px) {
  .observation-reports { padding: 12px; }.reports-header h1 { font-size: 23px; }.station-grid { grid-template-columns: 1fr; gap: 12px; }
  .station-card { padding: 22px 16px; }.station-info strong { font-size: 18px; }.station-icon { padding: 12px; }
  .report-filters { flex-wrap: wrap; }.report-filters .el-input { max-width: none; }.report-filters .el-select { width: 140px; }
  .report-row { flex-wrap: wrap; gap: 12px; }.report-actions { width: 100%; justify-content: flex-end; }.report-info { flex-basis: calc(100% - 70px); }
  .station-heading { align-items: flex-start; }.station-heading > .el-button { margin-top: 5px; }.station-heading h2 { font-size: 20px; }
}
</style>
<style>
.report-preview-dialog .el-dialog__body { padding: 12px 24px 20px; }
.report-preview-dialog.is-fullscreen .el-dialog__body { padding: 8px 12px 12px; }
@media(max-width:768px) { .report-preview-dialog.is-fullscreen { width: 100% !important; margin: 0 !important; border-radius: 0; } }
</style>
