<template>
  <div class="pdf-preview" v-loading="loading" element-loading-text="正在渲染 PDF…">
    <div class="pdf-toolbar">
      <el-button size="mini" icon="el-icon-arrow-left" :disabled="page <= 1 || loading" @click="changePage(-1)">上一页</el-button>
      <span>{{ page }} / {{ total || '—' }}</span>
      <el-button size="mini" :disabled="page >= total || loading" @click="changePage(1)">下一页<i class="el-icon-arrow-right"></i></el-button>
      <el-select v-model="zoom" size="mini" aria-label="PDF 缩放" @change="renderPage">
        <el-option label="适合宽度" :value="0" /><el-option label="100%" :value="1" /><el-option label="150%" :value="1.5" /><el-option label="200%" :value="2" />
      </el-select>
    </div>
    <div v-if="error" class="pdf-error">{{ error }}<el-button type="text" @click="load">重试</el-button></div>
    <div ref="viewport" class="pdf-viewport"><canvas ref="canvas" aria-label="PDF 页面"></canvas></div>
  </div>
</template>

<script>
export default {
  name: 'ReportPdfPreview', props: { blob: { type: Blob, required: true } },
  data: () => ({ page: 1, total: 0, zoom: 0, loading: true, error: '' }),
  mounted() {
    this.load()
    this._resize = new ResizeObserver(() => { clearTimeout(this._timer); this._timer = setTimeout(() => { if (this._pdf && !this.zoom) this.renderPage() }, 150) })
    this._resize.observe(this.$refs.viewport)
  },
  beforeDestroy() {
    this._destroyed = true; this._renderId = (this._renderId || 0) + 1
    clearTimeout(this._timer); this._resize.disconnect()
    if (this._renderTask) this._renderTask.cancel()
    if (this._loadTask) this._loadTask.destroy()
  },
  methods: {
    async load() {
      this.loading = true; this.error = ''
      try {
        if (this._loadTask) await this._loadTask.destroy()
        const pdfjs = await import('pdfjs-dist/legacy/build/pdf.mjs')
        pdfjs.GlobalWorkerOptions.workerSrc = new URL('pdfjs-dist/legacy/build/pdf.worker.min.mjs', import.meta.url).toString()
        const data = new Uint8Array(await this.blob.arrayBuffer())
        if (this._destroyed) return
        const base = `${process.env.BASE_URL}pdf-assets/`
        this._loadTask = pdfjs.getDocument({ data, cMapUrl: `${base}cmaps/`, cMapPacked: true,
          standardFontDataUrl: `${base}standard_fonts/`, wasmUrl: `${base}wasm/`, isEvalSupported: false })
        this._pdf = await this._loadTask.promise
        if (this._destroyed) return
        this.total = this._pdf.numPages; this.page = 1; await this.renderPage()
      } catch (_) { if (!this._destroyed) { this.error = 'PDF 预览失败，可重试或下载原文件查看。'; this.loading = false } }
    },
    changePage(delta) { this.page += delta; this.$refs.viewport.scrollTop = 0; this.renderPage() },
    async renderPage() {
      if (!this._pdf || this._destroyed) return
      const id = this._renderId = (this._renderId || 0) + 1
      this.loading = true; this.error = ''
      try {
        if (this._renderTask) { this._renderTask.cancel(); await this._renderTask.promise.catch(() => {}) }
        const page = await this._pdf.getPage(this.page)
        if (id !== this._renderId || this._destroyed) return
        const width = Math.max(160, this.$refs.viewport.clientWidth - 32)
        const scale = this.zoom || width / page.getViewport({ scale: 1 }).width
        const viewport = page.getViewport({ scale })
        const canvas = this.$refs.canvas
        const ratio = Math.min(window.devicePixelRatio || 1, 2)
        canvas.width = Math.ceil(viewport.width * ratio); canvas.height = Math.ceil(viewport.height * ratio)
        canvas.style.width = `${viewport.width}px`; canvas.style.height = `${viewport.height}px`
        this._renderTask = page.render({ canvasContext: canvas.getContext('2d'), viewport, transform: [ratio, 0, 0, ratio, 0, 0] })
        await this._renderTask.promise
      } catch (error) { if (id === this._renderId && !this._destroyed && error.name !== 'RenderingCancelledException') this.error = '此页渲染失败，请重试。' }
      finally { if (id === this._renderId && !this._destroyed) this.loading = false }
    }
  }
}
</script>

<style scoped>
.pdf-toolbar { display: flex; gap: 10px; align-items: center; justify-content: center; flex-wrap: wrap; padding: 10px; background: white; }
.pdf-toolbar .el-button { margin-left: 0; }.pdf-toolbar .el-select { width: 110px; }.pdf-toolbar span { font-size: 14px; }
.pdf-viewport { overflow: auto; height: 65vh; padding: 16px; box-sizing: border-box; }canvas { display: block; margin: 0 auto; box-shadow: 0 2px 8px #0002; }
.pdf-error { text-align: center; padding: 20px; }
@media(max-width:768px) { .pdf-viewport { height: calc(100dvh - 260px); min-height: 240px; }.pdf-toolbar { gap: 8px; } }
</style>
