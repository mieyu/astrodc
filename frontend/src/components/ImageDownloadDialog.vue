<template>
  <el-dialog title="批量下载原始 FITS" :visible="visible" :width="isMobile ? '96%' : '900px'"
             append-to-body :close-on-click-modal="false" @open="loadGroups" @close="$emit('close')" @closed="reset">
    <p class="download-intro">按 DATE-OBS 中记录的日期、目标和望远镜分组，选择需要的观测日。ZIP 内保留原目录和完整 FITS 文件。</p>
    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
    <div v-loading="loading" element-loading-text="正在整理观测日…" class="group-picker">
      <div class="download-filters">
        <el-date-picker v-model="dates" type="daterange" value-format="yyyy-MM-dd" range-separator="至"
                        :disabled="loading || preparing || starting" start-placeholder="起始日期" end-placeholder="结束日期" size="small" />
        <el-select v-model="target" :disabled="loading || preparing || starting" clearable filterable placeholder="全部目标" size="small">
          <el-option v-for="value in targets" :key="value" :value="value" :label="value" />
        </el-select>
        <el-select v-model="telescope" :disabled="loading || preparing || starting" clearable filterable placeholder="全部望远镜" size="small">
          <el-option v-for="value in telescopes" :key="value" :value="value" :label="value" />
        </el-select>
      </div>
      <div class="group-actions">
        <span>共 {{ groups.length }} 组，当前显示 {{ filteredGroups.length }} 组；已选 {{ selectedGroups.length }} 组（含隐藏项）</span>
        <el-button type="text" :disabled="loading || preparing || starting" @click="selectFiltered">选中当前筛选</el-button>
        <el-button type="text" :disabled="preparing || starting" @click="clearSelection">清空选择</el-button>
      </div>
      <el-table ref="groups" :data="filteredGroups" row-key="id" height="300" size="small" border
                empty-text="没有符合条件的观测日" @selection-change="selectionChanged">
        <el-table-column type="selection" :reserve-selection="true" width="45" :selectable="() => !preparing && !starting" />
        <el-table-column prop="date" label="观测日期" width="112" />
        <el-table-column prop="object" label="目标" min-width="80" />
        <el-table-column prop="tele" label="望远镜" min-width="105">
          <template slot-scope="{ row }">{{ row.tele || '未记录' }}</template>
        </el-table-column>
        <el-table-column prop="fileCount" label="目标当日文件" min-width="110" />
        <el-table-column prop="matchedCount" label="本次查询匹配" min-width="110" />
        <el-table-column prop="calibrationCount" label="同日校准文件" min-width="110" />
      </el-table>
      <p v-if="ungroupedCount" class="download-note">{{ ungroupedCount }} 条记录缺少日期或目标，无法按日分组，可返回列表逐个下载。</p>
    </div>

    <div class="download-options">
      <el-radio-group v-model="matchedOnly" :disabled="preparing || starting" @change="invalidatePlan">
        <el-radio :label="false">目标当日全部 FITS（推荐）</el-radio>
        <el-radio :label="true">仅本次查询匹配的 FITS</el-radio>
      </el-radio-group>
      <p class="download-note">{{ matchedOnly ? '保留本次查询的全部筛选条件。' : '按已选日期、目标和望远镜取全当天文件，不再受曝光时间、滤光片等细筛条件限制。' }}</p>
      <el-checkbox v-model="includeCalibration" :disabled="preparing || starting" @change="invalidatePlan">
        加入同日同望远镜的 BIAS / FLAT / DARK
      </el-checkbox>
      <p class="download-note">日期使用原始 DATE-OBS，不换算时区；未记录望远镜的组无法自动配对校准文件。同一文件只打包一次。</p>
    </div>

    <div v-if="prepared" class="download-summary">
      <strong>{{ prepared.fileCount }} 个可下载文件 · 原始大小 {{ formatBytes(prepared.totalBytes) }}</strong>
      <p>{{ prepared.filename }}</p>
      <template v-if="prepared.missingCount">
        <el-alert :title="'有 ' + prepared.missingCount + ' 个文件缺失、不可读或不是 FITS，将不放入 ZIP。'"
                  type="warning" :closable="false" show-icon />
        <details><summary>查看部分缺失文件</summary><ul><li v-for="name in prepared.missingExamples" :key="name">{{ name }}</li></ul></details>
        <el-checkbox v-model="acceptMissing">我已了解缺失情况，下载其余 {{ prepared.fileCount }} 个文件</el-checkbox>
      </template>
      <p class="download-note">大批量数据直接交给浏览器下载，不在页面中缓存。下载准备有效期为 30 分钟，ZIP 大小会略大于原文件总大小。</p>
      <el-alert v-if="started" title="已交给浏览器，请在浏览器下载列表中查看进度。下载完成前请保持服务运行。"
                type="success" :closable="false" show-icon />
    </div>
    <iframe ref="downloadFrame" :name="frameName" title="FITS 文件下载" class="download-frame"></iframe>
    <span slot="footer">
      <el-button @click="$emit('close')">关闭</el-button>
      <el-button type="primary" plain :disabled="loading || preparing || starting || !selectedGroups.length" :loading="preparing" @click="prepare">
        {{ prepared ? '重新核对文件' : '核对文件与大小' }}
      </el-button>
      <el-button v-if="prepared" type="primary" icon="el-icon-download"
                 :loading="starting" :disabled="preparing || starting || (prepared.missingCount > 0 && !acceptMissing)" @click="startDownload">
        {{ started ? '再次下载 ZIP' : '开始下载 ZIP' }}
      </el-button>
    </span>
  </el-dialog>
</template>

<script>
import axios from 'axios';
import mobileViewport from '../mixins/mobileViewport';

export default {
  name: 'ImageDownloadDialog',
  mixins: [mobileViewport],
  props: { visible: Boolean, query: { type: Object, default: () => ({}) } },
  data() {
    return {
      groups: [], selectedGroups: [], ungroupedCount: 0, dates: null, target: '', telescope: '',
      matchedOnly: false, includeCalibration: false, loading: false, preparing: false, starting: false,
      error: '', lastToken: null, prepared: null, acceptMissing: false, started: false, controller: null,
      frameName: 'fits-download-' + Math.random().toString(36).slice(2)
    };
  },
  computed: {
    targets() { return [...new Set(this.groups.map(group => group.object))].sort(); },
    telescopes() { return [...new Set(this.groups.map(group => group.tele).filter(Boolean))].sort(); },
    filteredGroups() {
      return this.groups.filter(group => (!this.target || group.object === this.target)
        && (!this.telescope || group.tele === this.telescope)
        && (!this.dates || (group.date >= this.dates[0] && group.date <= this.dates[1])));
    }
  },
  methods: {
    async loadGroups() {
      this.reset();
      this.loading = true;
      const controller = new AbortController();
      this.controller = controller;
      try {
        const response = await axios.post('/api/image/own/downloads/groups', this.query, { signal: controller.signal });
        if (this.controller !== controller) return;
        if (response.data.code !== 1) throw new Error(response.data.message || '整理观测日失败');
        this.groups = response.data.data.groups;
        this.ungroupedCount = response.data.data.ungroupedCount;
        this.target = this.query.object || '';
        this.telescope = this.query.tele || '';
        if (this.filteredGroups.length === 1) {
          await this.$nextTick();
          this.$refs.groups.toggleRowSelection(this.filteredGroups[0], true);
        }
      } catch (error) {
        if (this.controller === controller && !axios.isCancel(error)) this.error = this.errorMessage(error);
      } finally {
        if (this.controller === controller) { this.loading = false; this.controller = null; }
      }
    },
    selectionChanged(rows) { this.selectedGroups = rows; this.invalidatePlan(); },
    clearSelection() { this.$refs.groups.clearSelection(); },
    selectFiltered() { this.filteredGroups.forEach(group => this.$refs.groups.toggleRowSelection(group, true)); },
    invalidatePlan() { this.prepared = null; this.acceptMissing = false; this.started = false; },
    async prepare() {
      this.invalidatePlan();
      this.error = '';
      this.preparing = true;
      const controller = new AbortController();
      this.controller = controller;
      try {
        const response = await axios.post('/api/image/own/downloads/prepare', {
          query: this.query, groupIds: this.selectedGroups.map(group => group.id),
          includeCalibration: this.includeCalibration, matchedOnly: this.matchedOnly, previousToken: this.lastToken
        }, { signal: controller.signal });
        if (this.controller !== controller) return;
        if (response.data.code !== 1) throw new Error(response.data.message || '文件核对失败');
        this.prepared = response.data.data;
        this.lastToken = this.prepared.token;
      } catch (error) {
        if (this.controller === controller && !axios.isCancel(error)) this.error = this.errorMessage(error);
      } finally {
        if (this.controller === controller) { this.preparing = false; this.controller = null; }
      }
    },
    async startDownload() {
      if (!this.prepared || this.starting || (this.prepared.missingCount && !this.acceptMissing)) return;
      if (Date.now() >= Date.parse(this.prepared.expiresAt)) {
        this.invalidatePlan();
        this.error = '下载准备已过期，请重新核对文件。';
        return;
      }
      const prepared = this.prepared;
      this.starting = true;
      this.error = '';
      const controller = new AbortController();
      this.controller = controller;
      try {
        const url = axios.defaults.baseURL.replace(/\/$/, '') + '/api/image/own/downloads/' + encodeURIComponent(prepared.token);
        await axios.get(url + '/check', { signal: controller.signal });
        if (this.controller !== controller || this.prepared !== prepared) return;
        const link = document.createElement('a');
        link.href = url;
        link.target = this.frameName;
        document.body.appendChild(link);
        link.click();
        link.remove();
        this.started = true;
      } catch (error) {
        if (this.controller === controller && !axios.isCancel(error)) {
          this.invalidatePlan();
          this.error = this.errorMessage(error);
        }
      } finally {
        if (this.controller === controller) { this.starting = false; this.controller = null; }
      }
    },
    formatBytes(bytes) {
      if (!bytes) return '0 B';
      const units = ['B', 'KiB', 'MiB', 'GiB', 'TiB'];
      const index = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), units.length - 1);
      return (bytes / Math.pow(1024, index)).toFixed(index ? 2 : 0) + ' ' + units[index];
    },
    errorMessage(error) {
      const data = error.response && error.response.data;
      return (data && data.message) || error.message || '请求失败，请稍后重试';
    },
    reset() {
      if (this.controller) this.controller.abort();
      this.controller = null;
      this.loading = false;
      this.preparing = false;
      this.starting = false;
      this.groups = [];
      if (this.$refs.groups) this.$refs.groups.clearSelection();
      this.selectedGroups = [];
      this.ungroupedCount = 0;
      this.dates = null;
      this.target = '';
      this.telescope = '';
      this.matchedOnly = false;
      this.includeCalibration = false;
      this.error = '';
      this.invalidatePlan();
    }
  },
  beforeDestroy() { if (this.controller) this.controller.abort(); }
};
</script>

<style scoped>
.download-intro, .download-note { color: #606266; line-height: 1.6; }
.download-note { font-size: 12px; margin: 8px 0; }
.download-filters { display: flex; gap: 10px; margin: 16px 0 8px; }
.download-filters .el-select { width: 155px; }
.download-filters .el-date-editor { width: 310px; }
.group-actions { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; font-size: 12px; color: #606266; }
.group-actions span { margin-right: auto; }
.download-options { margin-top: 18px; padding-top: 16px; border-top: 1px solid #ebeef5; }
.download-summary { margin-top: 18px; padding: 15px; background: #f4f8fc; border: 1px solid #d8e5f1; border-radius: 6px; }
.download-summary p { overflow-wrap: anywhere; }
.download-summary details { margin: 12px 0; overflow-wrap: anywhere; }
.download-frame { display: none; }
@media (max-width: 768px) {
  .download-filters { flex-wrap: wrap; }
  .download-filters .el-select { width: calc(50% - 5px); }
  .download-filters .el-date-editor { width: 100%; }
  .download-options .el-radio { display: block; margin: 0 0 12px; }
  .download-options .el-checkbox { display: flex; white-space: normal; line-height: 1.6; }
}
</style>
