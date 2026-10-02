<template>
  <div class="ephemeris-page">
    <div class="app-back-bar">
      <button type="button" class="app-back-button" @click="goBack">
        <i class="el-icon-arrow-left"></i>
        <span>返回上一页</span>
      </button>
      <div class="app-back-context header-clock">
        <span class="clock-dot"></span>
        UTC: <span class="clock-text">{{ utcClock }}</span>
      </div>
    </div>

    <div class="page-header">
      <h2><i class="el-icon-cpu"></i> 天然卫星星历与寻星图工具</h2>
      <p>数据来源: SAI MSU / STScI DSS</p>
    </div>

    <el-row :gutter="16" class="main-row">
      <!-- 左侧：参数设置 -->
      <el-col :xs="24" :lg="8">
        <el-card class="param-card" shadow="hover">
          <div slot="header" class="card-title">
            <i class="el-icon-setting"></i> 参数设置
          </div>

          <!-- 观测站 -->
          <div class="form-group">
            <label class="field-label">观测站代码 (Observatory)</label>
            <el-input v-model="observatory" placeholder="例如: 286, 500" size="small"></el-input>
            <p class="field-hint">默认: 286 (云南天文台 1m)。地心坐标: 500</p>
          </div>

          <!-- 目标天体 -->
          <div class="form-group">
            <label class="field-label">目标天体 (Object)</label>
            <div class="satellite-box">
              <el-select
                v-model="selectedSat"
                filterable
                class="target-select"
                popper-class="ephemeris-target-options"
                placeholder="搜索天体名称或编号"
                aria-label="目标天体"
                size="small"
                @change="onSatelliteChange">
                <el-option-group
                  v-for="group in satelliteGroups"
                  :key="group.id"
                  :label="`${group.name} (${group.targets.length})`">
                  <el-option
                    v-for="sat in group.targets"
                    :key="sat.code"
                    :value="sat.code"
                    :label="`${sat.name} · ${sat.label} (${sat.code})`">
                    <span class="target-option-name">{{ sat.name }} ({{ sat.code }})</span>
                    <span class="target-option-detail">{{ sat.label }} · {{ sat.type }}</span>
                  </el-option>
                </el-option-group>
              </el-select>
              <p v-if="selectedTarget && !customSatellite" class="selected-target">
                {{ selectedTarget.name }} · {{ selectedTarget.label }} · {{ selectedTarget.type }}
              </p>
              <div class="satellite-divider"></div>
              <el-input
                v-model="customSatellite"
                placeholder="或输入 MSU 接口编号"
                size="small"
                @input="onCustomChange">
              </el-input>
            </div>
          </div>

          <!-- 观测时间 -->
          <div class="form-group">
            <label class="field-label">观测时间 (北京时间 +8)</label>
            <el-input
              v-model="localTimeInput"
              placeholder="2025 02 12 12 00 00"
              size="small"
              class="mono-input">
            </el-input>
            <p class="field-hint"><i class="el-icon-info"></i> 留空则默认使用当前 UTC 时间</p>
          </div>

          <!-- 高级参数 -->
          <el-collapse v-model="advancedOpen" class="advanced-collapse">
            <el-collapse-item title="高级参数" name="adv">
              <el-row :gutter="8">
                <el-col :span="12">
                  <label class="field-label">历表类型</label>
                  <el-select v-model="nde" size="small" style="width: 100%">
                    <el-option v-for="n in ndeOptions" :key="n.value" :label="n.label" :value="n.value"></el-option>
                  </el-select>
                </el-col>
                <el-col :span="12">
                  <label class="field-label">步数</label>
                  <el-input v-model="ntimes" size="small"></el-input>
                </el-col>
                <el-col :span="12" style="margin-top: 10px">
                  <label class="field-label">步长 (Hours)</label>
                  <el-input v-model="timestep" size="small"></el-input>
                </el-col>
                <el-col :span="12" style="margin-top: 10px">
                  <label class="field-label fov-label">视场 (角秒)</label>
                  <el-input-number v-model="fov" :min="1" :max="60" :step="1" controls-position="right" size="small" style="width: 100%"></el-input-number>
                </el-col>
              </el-row>
            </el-collapse-item>
          </el-collapse>

          <el-button
            type="primary"
            class="submit-btn"
            :loading="calculating"
            @click="submitCalculation">
            {{ calculating ? '计算中...' : '开始计算' }}
          </el-button>
        </el-card>
      </el-col>

      <!-- 右侧：结果与预览 -->
      <el-col :xs="24" :lg="16">
        <el-alert
          v-if="errorMsg"
          :title="errorMsg"
          type="error"
          show-icon
          :closable="true"
          @close="errorMsg = ''"
          class="error-alert">
        </el-alert>

        <!-- 空状态 -->
        <div v-if="!hasResults && !calculating" class="empty-state">
          <i class="el-icon-document"></i>
          <h3>等待计算</h3>
          <p>请在左侧设置参数并点击"开始计算"</p>
        </div>

        <!-- 结果表 -->
        <el-card v-if="hasResults" class="result-card" shadow="hover">
          <div slot="header" class="card-title">
            <i class="el-icon-circle-check"></i> 计算结果 ({{ results.length }})
          </div>
          <el-table :data="results" border size="small" max-height="320" class="result-table">
            <el-table-column prop="time" label="时间 (UTC)" min-width="170"></el-table-column>
            <el-table-column prop="ra" label="赤经 (R.A.)" min-width="140" class-name="coord-cell"></el-table-column>
            <el-table-column prop="de" label="赤纬 (Dec.)" min-width="140" class-name="coord-cell"></el-table-column>
            <el-table-column label="操作" width="170" align="center" header-align="center">
              <template slot-scope="scope">
                <div class="row-actions">
                  <el-button
                    size="mini"
                    icon="el-icon-document-copy"
                    @click="copyCoords(scope.row, scope.$index)">
                    {{ copiedIndex === scope.$index ? '已复制' : '复制' }}
                  </el-button>
                  <el-button
                    size="mini"
                    type="primary"
                    icon="el-icon-picture"
                    @click="handlePreviewDSS(scope.$index)">
                    预览
                  </el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <!-- DSS 预览 -->
        <el-card v-if="previewVisible" class="preview-card" shadow="hover">
          <div slot="header" class="card-title preview-header">
            <span><i class="el-icon-picture-outline"></i> 图像预览</span>
            <div class="preview-actions">
              <el-checkbox
                v-if="previewUrl"
                v-model="showCenterCrosshair"
                title="在图像中心显示或隐藏目标十字准星">
                中心十字准星
              </el-checkbox>
              <el-button
                v-if="previewUrl"
                type="success"
                size="mini"
                icon="el-icon-download"
                :loading="savingPreview"
                @click="downloadCurrentPreview">
                保存图片
              </el-button>
            </div>
          </div>
          <div class="preview-content">
            <div v-if="previewLoading" class="preview-loading">
              <i class="el-icon-loading"></i>
              <p>正在从 STScI 获取图像 ({{ fov }}' x {{ fov }}')...</p>
            </div>
            <div v-else-if="previewError" class="preview-error">
              <i class="el-icon-warning-outline"></i>
              <p>{{ previewError }}</p>
            </div>
            <div v-else-if="previewUrl" class="preview-image-wrap">
              <img ref="previewImage" :src="previewImageSrc" alt="DSS Chart" @click="openFullscreen">
              <div v-if="showCenterCrosshair" class="center-crosshair" aria-hidden="true">
                <span class="crosshair-arm crosshair-top"></span>
                <span class="crosshair-arm crosshair-right"></span>
                <span class="crosshair-arm crosshair-bottom"></span>
                <span class="crosshair-arm crosshair-left"></span>
              </div>
              <div class="fov-badge">FOV: {{ fov }}'</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 全屏图片 -->
    <el-dialog
      :visible.sync="fullscreenVisible"
      :show-close="true"
      width="92%"
      top="3vh"
      custom-class="fullscreen-dialog"
      append-to-body>
      <div slot="title" class="fullscreen-title">
        <span>图像预览</span>
        <el-checkbox
          v-if="previewImageSrc"
          v-model="showCenterCrosshair"
          title="在图像中心显示或隐藏目标十字准星">
          中心十字准星
        </el-checkbox>
      </div>
      <div v-if="previewImageSrc" class="fullscreen-image-wrap">
        <img :src="previewImageSrc" alt="DSS Chart Fullscreen" class="fullscreen-img">
        <div v-if="showCenterCrosshair" class="center-crosshair" aria-hidden="true">
          <span class="crosshair-arm crosshair-top"></span>
          <span class="crosshair-arm crosshair-right"></span>
          <span class="crosshair-arm crosshair-bottom"></span>
          <span class="crosshair-arm crosshair-left"></span>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import axios from 'axios';
import SATELLITE_GROUPS from '../data/ephemeris-targets.json';

const SATELLITES = SATELLITE_GROUPS.flatMap(group => group.targets);

const NDE_OPTIONS = [
  { value: '6', label: 'DE441 (默认)' },
  { value: '8', label: 'INPOP19a' },
  { value: '7', label: 'INPOP17a' },
  { value: '1', label: 'INPOP13C' },
  { value: '9', label: 'EPM2021' },
  { value: '5', label: 'DE431' },
  { value: '2', label: 'DE405' },
  { value: '3', label: 'DE406' },
  { value: '0', label: 'DE200' },
  { value: '4', label: 'VSOP87' },
  { value: '10', label: 'LeVerrier' }
];

function pad(n) {
  return String(n).padStart(2, '0');
}

function formatUtcSpace(date) {
  return [
    date.getUTCFullYear(),
    pad(date.getUTCMonth() + 1),
    pad(date.getUTCDate()),
    pad(date.getUTCHours()),
    pad(date.getUTCMinutes()),
    pad(date.getUTCSeconds())
  ].join(' ');
}

// The upstream sign may appear on minutes/seconds when degrees are zero.
// Move it to degrees using strings to preserve -0, leading zeros and precision.
function normalizeDeclination(value) {
  if (typeof value !== 'string') return value;
  const parts = value.match(/^(\s*)([+-]?\d+(?:\.\d+)?)(\s*°\s*|\s+)([+-]?\d+(?:\.\d+)?)(\s*['′]\s*|\s+)([+-]?\d+(?:\.\d+)?)(\s*["″]?\s*)$/);
  if (!parts || ![parts[2], parts[4], parts[6]].some(part => part.startsWith('-'))) return value;
  const magnitude = part => part.replace(/^[+-]/, '');
  return parts[1] + '-' + magnitude(parts[2]) + parts[3]
    + magnitude(parts[4]) + parts[5] + magnitude(parts[6]) + parts[7];
}

export default {
  name: 'EphemerisTool',
  data() {
    return {
      satellites: SATELLITES,
      satelliteGroups: SATELLITE_GROUPS,
      ndeOptions: NDE_OPTIONS,

      observatory: '286',
      selectedSat: '6000',
      customSatellite: '',
      localTimeInput: '',
      nde: '6',
      ntimes: '1',
      timestep: '1',
      fov: 10,
      advancedOpen: ['adv'],

      utcClock: '--:--:--',
      clockTimer: null,

      calculating: false,
      results: [],
      errorMsg: '',
      copiedIndex: -1,
      copyResetTimer: null,

      previewVisible: false,
      previewLoading: false,
      previewError: '',
      previewUrl: '',
      previewFilename: '',
      savingPreview: false,
      currentSatelliteName: '',
      showCenterCrosshair: true,

      fullscreenVisible: false
    };
  },
  computed: {
    selectedTarget() {
      return this.satellites.find(s => s.code === this.selectedSat);
    },
    hasResults() {
      return this.results.length > 0;
    },
    previewImageSrc() {
      if (!this.previewUrl) return '';
      if (/^https?:\/\//i.test(this.previewUrl)) return this.previewUrl;
      return (axios.defaults.baseURL || '') + this.previewUrl;
    }
  },
  mounted() {
    this.tickClock();
    this.clockTimer = setInterval(this.tickClock, 1000);
  },
  beforeDestroy() {
    if (this.clockTimer) clearInterval(this.clockTimer);
    if (this.copyResetTimer) clearTimeout(this.copyResetTimer);
  },
  methods: {
    goBack() {
      this.$router.go(-1);
    },
    tickClock() {
      const now = new Date();
      this.utcClock =
        `${now.getUTCFullYear()}-${pad(now.getUTCMonth() + 1)}-${pad(now.getUTCDate())} ` +
        `${pad(now.getUTCHours())}:${pad(now.getUTCMinutes())}:${pad(now.getUTCSeconds())}`;
    },
    onSatelliteChange() {
      this.customSatellite = '';
    },
    onCustomChange(val) {
      if (val && val.trim()) {
        this.selectedSat = '';
      }
    },
    resolveSatellite() {
      const custom = (this.customSatellite || '').trim();
      if (custom) return custom;
      return this.selectedSat || '';
    },
    resolveSatelliteName() {
      const custom = (this.customSatellite || '').trim();
      if (custom) return custom;
      const found = this.satellites.find(s => s.code === this.selectedSat);
      return found ? found.name : 'Unknown';
    },
    parseLocalTimeToUtc(localStr) {
      const cleaned = localStr.replace(/[-:]/g, ' ').trim();
      const parts = cleaned.split(/\s+/);
      if (parts.length < 6) return null;
      const [Y, M, D, h, m, s] = parts.map(p => parseInt(p, 10));
      if ([Y, M, D, h, m, s].some(n => Number.isNaN(n))) return null;
      // 北京时间 (+8) -> UTC
      const dateObj = new Date(Date.UTC(Y, M - 1, D, h - 8, m, s));
      return formatUtcSpace(dateObj);
    },
    async submitCalculation() {
      this.errorMsg = '';
      const satellite = this.resolveSatellite();
      if (!satellite) {
        this.$message.warning('请选择或输入一个目标天体');
        return;
      }
      let initmom;
      const localStr = (this.localTimeInput || '').trim();
      if (localStr) {
        initmom = this.parseLocalTimeToUtc(localStr);
        if (!initmom) {
          this.$message.error('时间格式错误,请使用: YYYY MM DD HH mm ss');
          return;
        }
      } else {
        initmom = formatUtcSpace(new Date());
      }
      if (!this.observatory) {
        this.$message.warning('请输入观测站代码');
        return;
      }

      this.calculating = true;
      this.previewVisible = false;
      this.previewUrl = '';
      this.currentSatelliteName = this.resolveSatelliteName();

      try {
        const payload = {
          observatory: this.observatory,
          satellite,
          initmom,
          plnvar: '0',
          nde: this.nde,
          ntimes: String(this.ntimes),
          timestep: String(this.timestep)
        };
        const res = await axios.post('/api/ephemeris/calculate', payload);
        if (res.data && res.data.success) {
          this.results = (res.data.data || []).map(row => ({
            ...row,
            de: normalizeDeclination(row.de),
            de_pure: normalizeDeclination(row.de_pure)
          }));
          if (this.results.length > 0) {
            // 自动预览第一条
            setTimeout(() => this.handlePreviewDSS(0), 300);
          } else {
            this.errorMsg = '未返回任何历表数据';
          }
        } else {
          this.errorMsg = (res.data && res.data.message) || '未知错误';
        }
      } catch (err) {
        this.errorMsg = '请求失败: ' + err.message;
      } finally {
        this.calculating = false;
      }
    },
    async copyCoords(row, index) {
      const text = `${row.ra_pure} ${row.de_pure}`;
      const ok = await this.copyText(text);
      if (ok) {
        this.copiedIndex = index;
        if (this.copyResetTimer) clearTimeout(this.copyResetTimer);
        this.copyResetTimer = setTimeout(() => { this.copiedIndex = -1; }, 1500);
      } else {
        this.$message.error('复制失败');
      }
    },
    async copyText(text) {
      // 优先用 navigator.clipboard (需要 HTTPS 或 localhost)
      if (navigator.clipboard && window.isSecureContext) {
        try {
          await navigator.clipboard.writeText(text);
          return true;
        } catch (e) { /* 失败则降级 */ }
      }
      // 降级:HTTP / 老浏览器用 execCommand
      const ta = document.createElement('textarea');
      ta.value = text;
      ta.setAttribute('readonly', '');
      ta.style.position = 'fixed';
      ta.style.top = '0';
      ta.style.left = '0';
      ta.style.opacity = '0';
      document.body.appendChild(ta);
      ta.select();
      ta.setSelectionRange(0, text.length);
      let ok = false;
      try { ok = document.execCommand('copy'); } catch (e) { ok = false; }
      document.body.removeChild(ta);
      return ok;
    },
    async handlePreviewDSS(index) {
      const item = this.results[index];
      if (!item) return;

      this.previewVisible = true;
      this.previewLoading = true;
      this.previewError = '';
      this.previewUrl = '';
      this.previewFilename = '';

      try {
        const res = await axios.post('/api/ephemeris/download_chart', {
          ra: item.ra_pure,
          dec: item.de_pure,
          satellite_name: this.currentSatelliteName || this.resolveSatelliteName(),
          time_str: item.time,
          fov: this.fov
        });
        if (res.data && res.data.success) {
          this.previewUrl = res.data.download_url;
          this.previewFilename = res.data.filename || 'dss_chart.gif';
        } else {
          this.previewError = '加载失败: ' + ((res.data && res.data.message) || '未知错误');
        }
      } catch (err) {
        this.previewError = '加载失败: ' + err.message;
      } finally {
        this.previewLoading = false;
      }
    },
    async downloadCurrentPreview() {
      if (!this.previewUrl || this.savingPreview) return;
      // Capture the image and checkbox state at the moment Save is pressed.
      const imageUrl = this.previewImageSrc;
      const withCrosshair = this.showCenterCrosshair;
      const originalFilename = this.previewFilename || 'dss_chart.gif';
      const previewImage = this.$refs.previewImage;
      const displayedWidth = previewImage ? previewImage.getBoundingClientRect().width : 0;
      this.savingPreview = true;

      try {
        // Fetch through the authenticated API, then decode a local blob URL.
        // This keeps cross-origin images from tainting the export canvas.
        const response = await axios.get(imageUrl, { responseType: 'blob' });
        let output = response.data;
        let filename = originalFilename;
        if (!output || !output.size) throw new Error('图片内容为空');

        if (withCrosshair) {
          const sourceUrl = URL.createObjectURL(output);
          try {
            const image = await new Promise((resolve, reject) => {
              const img = new Image();
              img.onload = () => resolve(img);
              img.onerror = () => reject(new Error('图片解码失败'));
              img.src = sourceUrl;
            });
            const canvas = document.createElement('canvas');
            canvas.width = image.naturalWidth;
            canvas.height = image.naturalHeight;
            const ctx = canvas.getContext('2d');
            if (!ctx) throw new Error('浏览器不支持图片合成');
            ctx.drawImage(image, 0, 0);

            // Match the preview's four 6px arms, 3px centre gap and 1px stroke,
            // scaled back to the original image resolution.
            const scale = displayedWidth > 0 ? image.naturalWidth / displayedWidth : 1;
            ctx.save();
            ctx.translate(canvas.width / 2, canvas.height / 2);
            ctx.scale(scale, scale);
            ctx.fillStyle = '#ff3b30';
            ctx.shadowColor = 'rgba(0, 0, 0, 0.6)';
            ctx.shadowBlur = scale;
            ctx.fillRect(-0.5, -9, 1, 6);
            ctx.fillRect(3, -0.5, 6, 1);
            ctx.fillRect(-0.5, 3, 1, 6);
            ctx.fillRect(-9, -0.5, 6, 1);
            ctx.restore();
            output = await new Promise((resolve, reject) => {
              canvas.toBlob(blob => blob ? resolve(blob) : reject(new Error('图片导出失败')), 'image/png');
            });
            filename = originalFilename.replace(/\.[^/.]+$/, '') + '_crosshair.png';
          } finally {
            URL.revokeObjectURL(sourceUrl);
          }
        }

        const downloadUrl = URL.createObjectURL(output);
        const link = document.createElement('a');
        link.href = downloadUrl;
        link.download = filename;
        document.body.appendChild(link);
        try {
          link.click();
        } finally {
          link.remove();
          // Let the browser start reading the download before releasing the blob.
          setTimeout(() => URL.revokeObjectURL(downloadUrl), 1000);
        }
      } catch (err) {
        this.$message.error('保存图片失败，请重试：' + (err.message || '未知错误'));
      } finally {
        this.savingPreview = false;
      }
    },
    openFullscreen() {
      if (!this.previewUrl) return;
      this.fullscreenVisible = true;
    }
  }
};
</script>

<style scoped>
.ephemeris-page {
  padding: 16px 20px;
  max-width: 1400px;
  margin: 0 auto;
}

.header-clock {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-family: Menlo, Consolas, monospace;
  font-size: 12px;
  color: #606266;
  background: #f5f7fa;
  padding: 4px 10px;
  border-radius: 6px;
}

.clock-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #67C23A;
  animation: pulse 1.5s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

.page-header {
  text-align: center;
  margin-bottom: 16px;
}

.page-header h2 {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  font-size: 22px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 6px 0;
}

.page-header h2 i { color: var(--brand); font-size: 24px; }
.page-header p { color: #909399; font-size: 12px; margin: 0; }

.main-row { margin-top: 8px; }

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}
.card-title i { color: var(--brand); margin-right: 6px; }

/* 参数面板 */
.param-card { border-radius: 10px; }

.form-group {
  margin-bottom: 14px;
}

.field-label {
  display: block;
  font-size: 12px;
  font-weight: 500;
  color: #606266;
  margin-bottom: 6px;
}

.fov-label { color: var(--brand); font-weight: 600; }

.field-hint {
  font-size: 11px;
  color: #c0c4cc;
  margin: 4px 0 0;
  line-height: 1.4;
}

.field-hint i { margin-right: 2px; }

.satellite-box {
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 8px;
  background: #fafafa;
}

.target-select {
  width: 100%;
}

.selected-target {
  margin: 8px 0 0;
  font-size: 12px;
  line-height: 1.5;
  color: #606266;
  overflow-wrap: anywhere;
}

.satellite-divider {
  height: 1px;
  background: #ebeef5;
  margin: 8px 0;
}

.mono-input >>> .el-input__inner {
  font-family: Menlo, Consolas, monospace;
}

.advanced-collapse {
  margin-bottom: 14px;
  border-top: none;
}

.advanced-collapse >>> .el-collapse-item__header {
  font-size: 13px;
  font-weight: 500;
  background: #fafafa;
  padding-left: 10px;
  border-radius: 4px;
}

.advanced-collapse >>> .el-collapse-item__content {
  padding-bottom: 10px;
}

.submit-btn {
  width: 100%;
  height: 40px;
  font-size: 14px;
  font-weight: 600;
  border-radius: 8px;
  margin-top: 6px;
}

/* 右侧 */
.error-alert { margin-bottom: 12px; }

.empty-state {
  background: #fff;
  border: 1px dashed #dcdfe6;
  border-radius: 10px;
  padding: 60px 20px;
  text-align: center;
  color: #909399;
}

.empty-state i {
  font-size: 48px;
  color: #dcdfe6;
  margin-bottom: 12px;
}

.empty-state h3 {
  font-size: 16px;
  font-weight: 500;
  margin: 0 0 6px;
  color: #606266;
}

.empty-state p { font-size: 12px; margin: 0; }

.result-card { border-radius: 10px; margin-bottom: 12px; }

.result-table >>> .coord-cell {
  font-family: Menlo, Consolas, monospace;
  color: #1d4ed8;
  font-weight: 600;
}

.row-actions {
  display: inline-flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 6px;
}

.row-actions .el-button + .el-button {
  margin-left: 0;
}

.preview-card { border-radius: 10px; }

.preview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.preview-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
}

.preview-actions >>> .el-checkbox {
  margin-right: 0;
}

.preview-content {
  background: #18181b;
  height: 480px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 0 0 6px 6px;
  overflow: hidden;
  position: relative;
}

.preview-loading,
.preview-error {
  color: #c0c4cc;
  text-align: center;
}

.preview-loading i { font-size: 32px; color: var(--brand); }
.preview-error i { font-size: 32px; color: #f87171; }
.preview-loading p,
.preview-error p { margin: 10px 0 0; font-size: 13px; }

.preview-image-wrap {
  position: relative;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.preview-image-wrap img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  border-radius: 4px;
  cursor: zoom-in;
}

.center-crosshair {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 0;
  height: 0;
  z-index: 3;
  pointer-events: none;
}

.crosshair-arm {
  position: absolute;
  display: block;
  background: #ff3b30;
  box-shadow: 0 0 1px rgba(0, 0, 0, 0.6);
}

.crosshair-top,
.crosshair-bottom {
  left: -0.5px;
  width: 1px;
  height: 6px;
}

.crosshair-left,
.crosshair-right {
  top: -0.5px;
  width: 6px;
  height: 1px;
}

.crosshair-top { bottom: 3px; }
.crosshair-right { left: 3px; }
.crosshair-bottom { top: 3px; }
.crosshair-left { right: 3px; }

.fov-badge {
  position: absolute;
  bottom: 8px;
  right: 8px;
  background: rgba(0, 0, 0, 0.7);
  color: #fff;
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 4px;
  pointer-events: none;
}

.fullscreen-dialog >>> .el-dialog__body {
  padding: 0;
  background: #000;
  text-align: center;
}

.fullscreen-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding-right: 34px;
  font-weight: 600;
}

.fullscreen-title >>> .el-checkbox {
  margin-right: 0;
}

.fullscreen-image-wrap {
  position: relative;
  display: inline-block;
  max-width: 100%;
  line-height: 0;
}

.fullscreen-img {
  max-width: 100%;
  max-height: 88vh;
  display: block;
}

@media (max-width: 991px) {
  .preview-content { height: 360px; }
}

@media (max-width: 560px) {
  .preview-header,
  .preview-actions {
    align-items: flex-start;
  }

  .preview-header {
    flex-direction: column;
  }
}
</style>

<style>
.ephemeris-target-options {
  max-width: calc(100vw - 32px);
}

.ephemeris-target-options .el-select-dropdown__item {
  height: auto;
  min-height: 54px;
  padding: 7px 20px;
  line-height: 20px;
  white-space: normal;
  overflow-wrap: anywhere;
}

.ephemeris-target-options .target-option-name,
.ephemeris-target-options .target-option-detail {
  display: block;
}

.ephemeris-target-options .target-option-detail {
  font-size: 12px;
  color: #606266;
  font-weight: normal;
}

@media (max-width: 760px) {
  .ephemeris-page .page-header h2 {
    font-size: 20px;
  }
}
</style>
