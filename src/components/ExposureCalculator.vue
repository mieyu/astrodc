<template>
  <div class="exposure-page">
    <div class="app-back-bar">
      <button type="button" class="app-back-button" @click="goBack">
        <i class="el-icon-arrow-left"></i>
        <span>返回上一页</span>
      </button>
      <div class="app-back-context">
        天文曝光时间计算器 <strong>v1.0</strong>
      </div>
    </div>

    <div class="page-header">
      <h2><i class="el-icon-camera"></i> 天文曝光时间计算器</h2>
      <p>读取 FITS 图像 · 估计天光背景 · 物理噪声模型求解最佳曝光时间（计算在后端完成）</p>
    </div>

    <el-row :gutter="16" class="main-row">
      <!-- 左侧：参数设置 -->
      <el-col :xs="24" :lg="9">
        <el-card class="param-card" shadow="hover">
          <div slot="header" class="card-title"><i class="el-icon-setting"></i> 参数设置</div>

          <!-- 观测模式 -->
          <div class="form-group">
            <label class="field-label">观测模式</label>
            <el-radio-group v-model="mode" size="small" @change="onModeChange">
              <el-radio-button label="planet">🪐 行星模式</el-radio-button>
              <el-radio-button label="star">⭐ 恒星模式</el-radio-button>
            </el-radio-group>
            <p class="field-hint">面源每像素SNR / 点源总通量SNR</p>
          </div>

          <!-- FITS 文件 -->
          <div class="form-group">
            <label class="field-label">FITS 文件</label>
            <el-upload
              class="fits-upload"
              drag
              action="#"
              :auto-upload="false"
              :show-file-list="false"
              :before-upload="beforeUpload"
              accept=".fits,.fit,.FITS,.FIT">
              <div class="upload-inner">
                <i class="el-icon-upload2"></i>
                <span v-if="!fileName">将 FITS 拖到此处，或<em>点击选择</em></span>
                <span v-else class="file-name">{{ fileName }}</span>
              </div>
            </el-upload>
          </div>

          <!-- SNR + 最大曝光 -->
          <el-row :gutter="10">
            <el-col :span="12">
              <div class="form-group">
                <label class="field-label">目标 SNR</label>
                <el-input-number v-model="targetSnr" :min="3" :max="500" :step="1" :precision="1"
                                 controls-position="right" size="small" style="width: 100%"></el-input-number>
                <p class="field-hint">行星 10~30 / 恒星 50~200</p>
              </div>
            </el-col>
            <el-col :span="12">
              <div class="form-group">
                <label class="field-label">最大曝光 (秒)</label>
                <el-input-number v-model="maxExposure" :min="1" :max="7200" :step="10" :precision="0"
                                 controls-position="right" size="small" style="width: 100%"></el-input-number>
                <p class="field-hint">超过时给出可达SNR</p>
              </div>
            </el-col>
          </el-row>

          <!-- 相机参数 -->
          <el-collapse v-model="camOpen" class="cam-collapse">
            <el-collapse-item name="cam">
              <template slot="title"><i class="el-icon-camera-solid"></i>&nbsp;相机参数（Andor Tech 默认值）</template>
              <el-row :gutter="10">
                <el-col :span="8">
                  <label class="field-label">增益 (e⁻/ADU)</label>
                  <el-input-number v-model="gain" :min="0.1" :max="20" :step="0.1" :precision="2"
                                   controls-position="right" size="small" style="width: 100%"></el-input-number>
                </el-col>
                <el-col :span="8">
                  <label class="field-label">读出噪声 (e⁻)</label>
                  <el-input-number v-model="readNoise" :min="0.1" :max="50" :step="0.5" :precision="2"
                                   controls-position="right" size="small" style="width: 100%"></el-input-number>
                </el-col>
                <el-col :span="8">
                  <label class="field-label">暗电流 (e⁻/s)</label>
                  <el-input-number v-model="darkCurrent" :min="0" :max="10" :step="0.001" :precision="4"
                                   controls-position="right" size="small" style="width: 100%"></el-input-number>
                </el-col>
              </el-row>
              <p class="field-hint cam-note">💡 冷却至 -55°C 的 Andor CCD 暗电流极低，若知精确参数请修改，否则用默认值。</p>
            </el-collapse-item>
          </el-collapse>

          <el-button type="primary" class="submit-btn" icon="el-icon-cpu"
                     :loading="calculating" :disabled="!selectedFile" @click="doCalculate">
            {{ calculating ? '计算中...' : '🔬 计算最佳曝光时间' }}
          </el-button>
        </el-card>

        <!-- AI 智能分析 -->
        <el-card class="ai-card" shadow="hover">
          <div slot="header" class="card-title">
            <span><i class="el-icon-magic-stick"></i> AI 智能分析</span>
          </div>
          <el-button type="success" class="ai-btn" icon="el-icon-chat-dot-round"
                     :loading="aiLoading" :disabled="!result || !result.valid" @click="doAiAnalyze">
            {{ aiLoading ? 'AI 分析中...' : '🤖 AI 分析建议' }}
          </el-button>
          <div class="ai-output" v-if="aiText || aiError">
            <el-alert v-if="aiError" :title="aiError" type="error" :closable="false" show-icon></el-alert>
            <div v-else class="ai-markdown" v-html="renderedAi"></div>
          </div>
          <p v-else class="ai-placeholder">完成计算后点击「AI 分析建议」，由后端 LLM 解读结果并给出观测建议。</p>
        </el-card>
      </el-col>

      <!-- 右侧：预览 + 结果 -->
      <el-col :xs="24" :lg="15">
        <!-- 图像预览 -->
        <el-card class="preview-card" shadow="hover">
          <div slot="header" class="card-title preview-head">
            <span><i class="el-icon-picture-outline"></i> 图像预览与检测结果</span>
            <span class="zoom-info" v-if="result && result.width">缩放: {{ Math.round(zoom * 100) }}% · {{ result.width }}×{{ result.height }}</span>
          </div>

          <div v-if="!result || !result.imagePngBase64" class="preview-empty">
            <i class="el-icon-picture"></i>
            <p>选择 FITS 文件并点击「计算」后，将在此显示图像与检测标记</p>
            <p class="sub">滚轮缩放 · 拖拽平移</p>
          </div>

          <div v-else>
            <div class="canvas-wrap" ref="canvasWrap"
                 @wheel.prevent="onWheel"
                 @mousedown="onPanStart" @mousemove="onPanMove"
                 @mouseup="onPanEnd" @mouseleave="onPanEnd">
              <canvas ref="canvas" class="fits-canvas"
                      :style="{ transform: 'scale(' + zoom + ')' }"></canvas>
            </div>
            <p v-if="result.previewSummary" class="preview-summary">{{ result.previewSummary }}</p>
          </div>
        </el-card>

        <!-- 分析结果 -->
        <el-card class="result-card" shadow="hover">
          <div slot="header" class="card-title result-head">
            <span><i class="el-icon-data-analysis"></i> 分析结果</span>
            <span v-if="result && result.valid" class="result-badge">
              推荐曝光 {{ result.exptimeRecommended.toFixed(1) }} s
            </span>
          </div>
          <div v-if="!result || !result.report" class="result-empty">
            <i class="el-icon-document"></i>
            <p>设置参数后点击「计算最佳曝光时间」查看分析报告</p>
          </div>
          <pre v-else class="report">{{ result.report }}</pre>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import axios from 'axios';
import { marked } from 'marked';

export default {
  name: 'ExposureCalculator',
  data() {
    return {
      mode: 'planet',
      selectedFile: null,
      fileName: '',

      targetSnr: 15.0,
      maxExposure: 60,
      gain: 2.0,
      readNoise: 6.33,
      darkCurrent: 0.002,
      camOpen: ['cam'],

      calculating: false,
      result: null,

      // 预览缩放/平移
      zoom: 1,
      panning: false,
      panStart: null,

      // AI
      aiLoading: false,
      aiText: '',
      aiError: ''
    };
  },
  computed: {
    renderedAi() {
      return this.aiText ? marked.parse(this.aiText) : '';
    }
  },
  methods: {
    goBack() {
      this.$router.go(-1);
    },
    onModeChange(val) {
      // 与桌面版一致：切换模式时更新默认 SNR / 最大曝光
      if (val === 'planet') {
        this.targetSnr = 15.0;
        this.maxExposure = 60;
      } else {
        this.targetSnr = 100.0;
        this.maxExposure = 300;
      }
    },

    beforeUpload(file) {
      const name = file.name.toLowerCase();
      if (!(name.endsWith('.fits') || name.endsWith('.fit'))) {
        this.$message.error('只能上传 .fits 或 .fit 格式的文件！');
        return false;
      }
      this.selectedFile = file;
      this.fileName = file.name;
      this.result = null;
      this.aiText = '';
      this.aiError = '';
      return false; // 阻止 el-upload 自动上传，计算时统一提交
    },

    // ---------- 计算（调用后端）----------
    async doCalculate() {
      if (!this.selectedFile) {
        this.$message.warning('请先选择 FITS 文件');
        return;
      }
      this.calculating = true;
      this.aiText = '';
      this.aiError = '';
      try {
        const form = new FormData();
        form.append('file', this.selectedFile);
        form.append('mode', this.mode);
        form.append('targetSnr', this.targetSnr);
        form.append('maxExposure', this.maxExposure);
        form.append('gain', this.gain);
        form.append('readNoise', this.readNoise);
        form.append('darkCurrent', this.darkCurrent);

        const res = await axios.post('/api/exposure/analyze', form, {
          headers: { 'Content-Type': 'multipart/form-data' }
        });

        if (res.data && res.data.code === 1) {
          this.result = res.data.data;
          this.$nextTick(() => this.renderPreview());
          if (!this.result.valid) {
            this.$message.warning(this.result.message || '计算未通过');
          }
        } else {
          this.$message.error((res.data && res.data.message) || '计算失败');
        }
      } catch (err) {
        this.$message.error('请求失败: ' + err.message);
      } finally {
        this.calculating = false;
      }
    },

    // ---------- 预览渲染 ----------
    renderPreview() {
      const r = this.result;
      const canvas = this.$refs.canvas;
      if (!canvas || !r || !r.imagePngBase64) return;
      const ctx = canvas.getContext('2d');
      const imgEl = new Image();
      imgEl.onload = () => {
        canvas.width = r.width;
        canvas.height = r.height;
        ctx.drawImage(imgEl, 0, 0);
        this.drawMarkers(ctx);
        this.fitZoom();
      };
      imgEl.src = 'data:image/png;base64,' + r.imagePngBase64;
    },
    fitZoom() {
      const wrap = this.$refs.canvasWrap;
      if (!wrap || !this.result) return;
      const availW = wrap.clientWidth - 8;
      const availH = wrap.clientHeight - 8;
      const fit = Math.min(availW / this.result.width, availH / this.result.height);
      this.zoom = Math.max(0.05, Math.min(fit, 1));
    },
    drawMarkers(ctx) {
      const r = this.result;
      if (!r || !r.valid) return;
      if (r.mode === 'planet' && r.planet) {
        this.drawPlanetMarker(ctx, r.planet.cx, r.planet.cy, r.planet.radius, r.planet.snr);
      } else if (r.mode === 'star' && r.stars) {
        for (const s of r.stars) {
          this.drawStarMarker(ctx, s.cx, s.cy, s.radius, s.snrEstimate, s.selected);
        }
      }
    },
    drawPlanetMarker(ctx, cx, cy, radius, snr) {
      ctx.save();
      ctx.strokeStyle = 'rgba(0,255,0,0.95)';
      ctx.lineWidth = 2;
      ctx.beginPath();
      ctx.arc(cx, cy, radius, 0, Math.PI * 2);
      ctx.stroke();
      const cs = 10;
      ctx.beginPath();
      ctx.moveTo(cx - cs, cy); ctx.lineTo(cx + cs, cy);
      ctx.moveTo(cx, cy - cs); ctx.lineTo(cx, cy + cs);
      ctx.stroke();
      this.drawLabel(ctx, 'SNR: ' + snr.toFixed(2), cx + radius + 10, cy, 14, false);
      ctx.restore();
    },
    drawStarMarker(ctx, cx, cy, radius, snr, selected) {
      ctx.save();
      ctx.strokeStyle = selected ? 'rgba(255,200,0,0.95)' : 'rgba(0,255,0,0.9)';
      ctx.lineWidth = selected ? 2 : 1;
      ctx.beginPath();
      ctx.arc(cx, cy, radius, 0, Math.PI * 2);
      ctx.stroke();
      const cs = 6;
      ctx.beginPath();
      ctx.moveTo(cx - cs, cy); ctx.lineTo(cx + cs, cy);
      ctx.moveTo(cx, cy - cs); ctx.lineTo(cx, cy + cs);
      ctx.stroke();
      this.drawLabel(ctx, snr.toFixed(1), cx + radius + 4, cy, 9, selected);
      ctx.restore();
    },
    drawLabel(ctx, text, x, y, fontSize, highlight) {
      ctx.font = fontSize + 'px Arial';
      const w = ctx.measureText(text).width;
      const h = fontSize;
      ctx.fillStyle = 'rgba(0,0,0,0.7)';
      ctx.fillRect(x - 2, y - h / 2 - 2, w + 6, h + 4);
      ctx.fillStyle = highlight ? 'rgb(255,200,0)' : '#fff';
      ctx.textBaseline = 'middle';
      ctx.fillText(text, x + 1, y);
    },

    // ---------- 缩放/平移 ----------
    onWheel(e) {
      const step = 1.15;
      const z = e.deltaY < 0 ? this.zoom * step : this.zoom / step;
      this.zoom = Math.max(0.1, Math.min(20, z));
    },
    onPanStart(e) {
      const wrap = this.$refs.canvasWrap;
      this.panning = true;
      this.panStart = { x: e.clientX, y: e.clientY, sl: wrap.scrollLeft, st: wrap.scrollTop };
    },
    onPanMove(e) {
      if (!this.panning) return;
      const wrap = this.$refs.canvasWrap;
      wrap.scrollLeft = this.panStart.sl - (e.clientX - this.panStart.x);
      wrap.scrollTop = this.panStart.st - (e.clientY - this.panStart.y);
    },
    onPanEnd() {
      this.panning = false;
    },

    // ---------- AI（调用后端 LLM）----------
    async doAiAnalyze() {
      if (!this.result || !this.result.valid) {
        this.$message.warning('请先完成有效的曝光计算');
        return;
      }
      this.aiLoading = true;
      this.aiError = '';
      this.aiText = '';
      try {
        // 去掉体积较大的预览图，仅发送数值结果
        const payload = Object.assign({}, this.result, { imagePngBase64: null });
        const res = await axios.post('/api/exposure/ai-analyze', payload);
        if (res.data && res.data.code === 1) {
          this.aiText = res.data.data || '（AI 未返回内容）';
        } else {
          this.aiError = (res.data && res.data.message) || 'AI 分析失败';
        }
      } catch (err) {
        this.aiError = '请求失败: ' + err.message;
      } finally {
        this.aiLoading = false;
      }
    }
  }
};
</script>

<style scoped>
.exposure-page {
  padding: 16px 20px;
  max-width: 1500px;
  margin: 0 auto;
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
  color: var(--text-primary);
  margin: 0 0 6px;
}
.page-header h2 i { color: var(--brand); font-size: 24px; }
.page-header p { color: var(--text-muted); font-size: 12px; margin: 0; }

.main-row { margin-top: 8px; }

.card-title { font-size: 15px; font-weight: 600; color: var(--text-primary); }
.card-title i { color: var(--brand); margin-right: 6px; }

.param-card, .ai-card, .preview-card, .result-card { border-radius: 10px; margin-bottom: 14px; }

.form-group { margin-bottom: 14px; }
.field-label {
  display: block;
  font-size: 12px;
  font-weight: 500;
  color: var(--text-regular);
  margin-bottom: 6px;
}
.field-hint {
  font-size: 11px;
  color: var(--text-placeholder);
  margin: 4px 0 0;
  line-height: 1.4;
}
.cam-note { color: var(--text-muted); margin-top: 8px; }

/* 上传区 */
.fits-upload >>> .el-upload { width: 100%; display: block; }
.fits-upload >>> .el-upload-dragger {
  width: 100%;
  height: 84px;
  border-radius: var(--radius-sm);
  background: var(--bg-subtle);
  display: flex;
  align-items: center;
  justify-content: center;
}
.upload-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  color: var(--text-muted);
  font-size: 13px;
  padding: 0 12px;
}
.upload-inner i { font-size: 26px; color: var(--brand); }
.upload-inner em { color: var(--brand); font-style: normal; }
.file-name {
  color: var(--text-primary);
  font-weight: 500;
  word-break: break-all;
}

.cam-collapse { margin-bottom: 14px; border-top: none; }
.cam-collapse >>> .el-collapse-item__header {
  font-size: 13px;
  font-weight: 500;
  background: var(--bg-subtle);
  padding: 0 10px;
  border-radius: var(--radius-sm);
  border-bottom: none;
}
.cam-collapse >>> .el-collapse-item__wrap { border-bottom: none; }
.cam-collapse >>> .el-collapse-item__content { padding: 12px 4px 6px; }

.submit-btn {
  width: 100%;
  height: 40px;
  font-size: 14px;
  font-weight: 600;
  border-radius: 8px;
}

/* AI */
.preview-head, .result-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.ai-btn { width: 100%; height: 38px; font-weight: 600; border-radius: 8px; }
.ai-output { margin-top: 12px; }
.ai-placeholder {
  margin: 12px 0 0;
  font-size: 12px;
  color: var(--text-placeholder);
  line-height: 1.6;
}
.ai-markdown {
  font-size: 13px;
  line-height: 1.7;
  color: var(--text-regular);
  background: var(--bg-subtle);
  padding: 12px 14px;
  border-radius: var(--radius-sm);
  border-left: 3px solid #67c23a;
}
.ai-markdown >>> p { margin: 0 0 8px; }
.ai-markdown >>> ul { margin: 6px 0; padding-left: 20px; }
.ai-markdown >>> strong { color: var(--text-strong); }

/* 预览 */
.zoom-info {
  font-size: 12px;
  color: var(--text-muted);
  font-family: Menlo, Consolas, monospace;
  font-weight: 400;
}
.preview-empty, .result-empty {
  text-align: center;
  padding: 50px 20px;
  color: var(--text-muted);
}
.preview-empty i, .result-empty i {
  font-size: 46px;
  color: var(--border-strong);
  margin-bottom: 10px;
  display: block;
}
.preview-empty p, .result-empty p { margin: 4px 0; font-size: 13px; }
.preview-empty .sub { font-size: 11px; color: var(--text-placeholder); }

.canvas-wrap {
  width: 100%;
  height: 460px;
  overflow: auto;
  background: #14141f;
  border-radius: var(--radius-sm);
  cursor: grab;
  position: relative;
}
.canvas-wrap:active { cursor: grabbing; }
.fits-canvas {
  display: block;
  transform-origin: top left;
  image-rendering: pixelated;
}
.preview-summary {
  margin: 10px 0 0;
  font-size: 12px;
  line-height: 1.6;
  color: var(--text-regular);
  background: var(--bg-subtle);
  padding: 8px 12px;
  border-radius: var(--radius-sm);
  white-space: pre-line;
}

/* 结果 */
.result-badge {
  font-size: 13px;
  font-weight: 600;
  color: #fff;
  background: var(--brand);
  padding: 3px 12px;
  border-radius: 12px;
}
.report {
  margin: 0;
  font-family: Menlo, Consolas, 'Courier New', monospace;
  font-size: 12.5px;
  line-height: 1.55;
  color: var(--text-primary);
  white-space: pre;
  overflow-x: auto;
  max-height: 560px;
  overflow-y: auto;
}

@media (max-width: 991px) {
  .canvas-wrap { height: 360px; }
}
</style>
