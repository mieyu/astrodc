<template>
  <div class="report-preview" :aria-busy="loading">
    <div v-if="loading" class="preview-loading" role="status" aria-live="polite"><i class="el-icon-loading" aria-hidden="true"></i><p>{{ loadingText }}</p><el-progress v-if="downloadPercent !== null" :percentage="downloadPercent" :stroke-width="6" /><span>首次打开需要读取原文档，请稍候。</span></div>
    <div v-if="frameVisible && report.type === 'docx'" class="word-toolbar"><span>Word 预览</span><el-select v-model="wordZoom" size="mini" aria-label="Word 缩放" @change="fitWord"><el-option label="适合宽度" :value="0" /><el-option label="100%" :value="1" /><el-option label="150%" :value="1.5" /></el-select></div>
    <div v-if="error" class="preview-error"><i class="el-icon-document"></i><p>{{ error }}</p><el-button v-if="retryable" size="small" @click="load">重新加载</el-button></div>
    <ReportPdfPreview v-if="pdfBlob" :blob="pdfBlob" />
    <iframe :style="{ visibility: frameVisible ? 'visible' : 'hidden', height: frameVisible ? '' : '0' }" ref="frame" class="document-frame" :title="report.name" sandbox="allow-same-origin"></iframe>
  </div>
</template>

<script>
import axios from 'axios'
const frameShell = `<!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><meta http-equiv="Content-Security-Policy" content="default-src 'none'; img-src data: blob:; style-src 'unsafe-inline'; font-src data: blob:;"><style>html,body{margin:0;padding:0;color:#303133;background:#f1f3f6}body{overflow-x:auto}.docx-wrapper{padding:16px!important;background:#f1f3f6!important;align-items:center!important}.docx-wrapper>section.docx{margin-bottom:16px!important}#markdown{box-sizing:border-box;max-width:960px;margin:auto;padding:24px;background:white;min-height:100vh;font:16px/1.8 system-ui,sans-serif;overflow-wrap:anywhere}#markdown img{max-width:100%;height:auto}#markdown table{display:block;max-width:100%;overflow:auto;border-collapse:collapse}#markdown td,#markdown th{border:1px solid #ddd;padding:6px 10px}#markdown pre{overflow:auto;background:#f4f6f8;padding:16px}#markdown blockquote{border-left:3px solid #ccd8e3;margin-left:0;padding-left:16px;color:#606266}@media(max-width:600px){#markdown{padding:16px;font-size:15px}}</style></head><body><div id="content"></div></body></html>`

export default {
  name: 'ReportPreview',
  components: { ReportPdfPreview: () => import('./ReportPdfPreview.vue') },
  props: { report: { type: Object, required: true }, url: { type: String, required: true } },
  data: () => ({ loading: false, loadingText: '正在读取文档…', downloadPercent: null, error: '', retryable: true, frameVisible: false, pdfBlob: null, wordZoom: 0 }),
  mounted() { this.load(); if (window.ResizeObserver) { this._resize = new ResizeObserver(() => this.fitWord()); this._resize.observe(this.$el) } },
  beforeDestroy() { this._request = (this._request || 0) + 1; if (this._abort) this._abort.abort(); if (this._resize) this._resize.disconnect() },
  methods: {
    initializeFrame() {
      // Initialize the existing same-origin document directly. Waiting for a hidden
      // iframe's load event can hang, particularly on retries with identical srcdoc.
      const doc = this.$refs.frame.contentDocument
      if (!doc) throw new Error('当前浏览器无法初始化文档预览，请重试。')
      doc.open(); doc.write(frameShell); doc.close()
      return doc
    },
    async bounded(promise, milliseconds, message) {
      let timer
      try {
        return await Promise.race([promise, new Promise((_, reject) => { timer = setTimeout(() => reject(new Error(message)), milliseconds) })])
      } finally { clearTimeout(timer) }
    },
    fitWord() {
      if (!this.frameVisible || this.report.type !== 'docx') return
      const frame = this.$refs.frame
      if (!frame || !frame.contentDocument) return
      const pages = [...frame.contentDocument.querySelectorAll('section.docx')]
      if (!pages.length) return
      const width = Math.max(...pages.map(p => p.offsetWidth)) + 32
      frame.contentDocument.body.style.zoom = this.wordZoom || Math.min(1, frame.clientWidth / width)
    },
    async load() {
      const request = this._request = (this._request || 0) + 1
      if (this._abort) this._abort.abort()
      this._abort = new AbortController()
      this.loading = false; this.loadingText = '正在读取文档…'; this.downloadPercent = null
      this.error = ''; this.frameVisible = false; this.pdfBlob = null; this.retryable = true
      if (this.report.type === 'doc') { this.error = '此文件为旧版 Word（.doc），请下载查看。另存为 .docx 后即可在线预览。'; this.retryable = false; return }
      if (this.report.size > 50 * 1024 * 1024) { this.error = '此文档超过 50 MB，请下载原文件查看。'; this.retryable = false; return }
      this.loading = true
      try {
        const { data } = await axios.get(this.url, {
          responseType: 'blob', signal: this._abort.signal, timeout: 60000,
          onDownloadProgress: event => {
            if (request !== this._request) return
            const total = event.total || this.report.size
            this.downloadPercent = total ? Math.min(100, Math.round(event.loaded / total * 100)) : null
            this.loadingText = this.downloadPercent === 100 ? '文档已读取，正在准备预览…' : '正在读取文档…'
          }
        })
        if (request !== this._request) return
        if (this.report.type === 'pdf') { this.pdfBlob = data; return }
        this.downloadPercent = null
        this.loadingText = '正在排版文档…'
        const doc = this.initializeFrame()
        if (this.report.type === 'docx') {
          const { renderAsync } = await this.bounded(import('docx-preview'), 20000, '预览组件加载超时，请重试。')
          if (request !== this._request) return
          await this.bounded(renderAsync(await data.arrayBuffer(), doc.getElementById('content'), undefined, {
            useBase64URL: true, renderAltChunks: false, ignoreLastRenderedPageBreak: false
          }), 30000, 'Word 排版超时，请重试或下载原文件查看。')
          if (!doc.querySelector('section.docx')) throw new Error('Word 文档未能生成预览，请下载原文件查看。')
          const sizing = doc.createElement('style')
          sizing.textContent = '.docx-wrapper{width:max-content;min-width:100%;box-sizing:border-box}'
          doc.head.appendChild(sizing)
          doc.querySelectorAll('a').forEach(a => a.removeAttribute('href'))
        } else {
          const [{ marked }, { default: DOMPurify }] = await this.bounded(Promise.all([import('marked'), import('dompurify')]), 20000, '预览组件加载超时，请重试。')
          const html = DOMPurify.sanitize(marked.parse(await data.text()), { USE_PROFILES: { html: true }, FORBID_TAGS: ['style', 'form', 'input', 'button'], FORBID_ATTR: ['style'] })
          if (request !== this._request) return
          const content = doc.getElementById('content'); content.id = 'markdown'; content.innerHTML = html
          doc.querySelectorAll('a').forEach(a => { a.removeAttribute('href'); a.removeAttribute('target') })
        }
        if (request !== this._request) return
        this.frameVisible = true; await this.$nextTick(); this.fitWord()
      } catch (error) {
        if (request === this._request && !axios.isCancel(error)) {
          this._request += 1
          this.loading = false
          this.error = error.code === 'ECONNABORTED' ? '读取文档超时，请检查网络后重试。' :
            (error.response && error.response.status === 401 ? '访问已过期，请重新解锁网站后预览。' :
              error.response && error.response.status === 404 ? '文档已移除，请刷新列表。' :
                error.message && /超时|未能生成|无法初始化/.test(error.message) ? error.message : '文档预览失败，请重试或下载原文件查看。')
        }
      } finally { if (request === this._request) this.loading = false }
    }
  }
}
</script>

<style scoped>
.preview-loading { padding: 48px 24px; text-align: center; color: #606266; }.preview-loading i { font-size: 28px; color: #1f4e79; }.preview-loading span { font-size: 13px; color: #909399; }.preview-loading .el-progress { max-width: 320px; margin: 16px auto; }
.report-preview { min-height: 250px; background: #f1f3f6; border-radius: 6px; overflow: hidden; }
.document-frame { display: block; width: 100%; height: 72vh; border: 0; }
.word-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 8px 12px; background: white; font-size: 13px; color: #909399; }.word-toolbar .el-select { width: 120px; }.word-toolbar + .document-frame { height: 67vh; }
.preview-error { padding: 60px 20px; text-align: center; line-height: 1.8; }.preview-error i { font-size: 36px; color: #909399; }
@media(max-width:768px) { .document-frame { height: calc(100dvh - 180px); min-height: 280px; }.word-toolbar + .document-frame { height: calc(100dvh - 230px); } }
</style>
