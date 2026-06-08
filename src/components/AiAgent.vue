<template>
  <div class="ai-agent">
    <div class="header">
      
      <h1>AI Agent 数据分析</h1>
    </div>

    <div class="query-section">
      <div class="input-group">
        <textarea
          v-model="task"
          placeholder="请输入您的查询需求"
          rows="3"
          @keydown.ctrl.enter="analyzeData"
        ></textarea>
        <button 
          @click="analyzeData" 
          :disabled="loading || !task.trim()"
          class="analyze-btn"
        >
          <span v-if="!loading">分析</span>
          <span v-else>分析中...</span>
        </button>
      </div>
    </div>

    <div v-if="error" class="error-box">
      <h3>❌ 错误</h3>
      <p>{{ error }}</p>
    </div>

    <div v-if="result" class="result-section">
      <!-- SQL查询 -->
      <div class="sql-box">
        <div class="box-header">
          <h3>📝 生成的SQL查询</h3>
          <button @click="copySql" class="copy-btn">
            {{ sqlCopied ? '✓ 已复制' : '📋 复制' }}
          </button>
        </div>
        <pre><code>{{ result.sql }}</code></pre>
      </div>

      <!-- AI摘要 -->
      <div class="summary-box">
        <div class="box-header">
          <h3>🧠 AI 分析摘要</h3>
        </div>
        <div class="summary-content" v-html="formatSummary(result.summary)"></div>
      </div>

      <!-- 数据表格 -->
      <div class="data-box" v-if="result.data && result.data.length > 0">
        <div class="box-header">
          <h3>📊 查询结果 (共 {{ result.data.length }} 条)</h3>
          <button @click="exportData" class="export-btn">
            📥 导出JSON
          </button>
        </div>
        <div class="table-wrapper">
          <table>
            <thead>
              <tr>
                <th v-for="(value, key) in result.data[0]" :key="key">
                  {{ key }}
                </th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(row, index) in result.data" :key="index">
                <td v-for="(value, key) in row" :key="key">
                  {{ formatValue(value) }}
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <div v-if="!result && !loading && !error" class="empty-state">
      <p>在上方输入框中输入您的查询需求</p>
      <div class="example-queries">
        <h4>示例查询:</h4>
        <div class="examples">
          <button @click="task = '给我前十条数据'" class="example-btn">
            给我前十条数据
          </button>
          <button @click="task = '查询NAO216望远镜的所有LIGHT类型图像'" class="example-btn">
            查询NAO216望远镜的所有LIGHT类型图像
          </button>
          <button @click="task = '统计不同图像类型的数量'" class="example-btn">
            统计不同图像类型的数量
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'AiAgent',
  data() {
    return {
      task: '',
      loading: false,
      result: null,
      error: null,
      sqlCopied: false
    }
  },
  methods: {
    async analyzeData() {
      if (!this.task.trim()) return
      
      this.loading = true
      this.error = null
      this.result = null
      
      try {
        const response = await fetch('http://10.126.126.2:8088/analyze', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'accept': 'application/json'
          },
          body: JSON.stringify({
            task: this.task
          })
        })
        
        if (!response.ok) {
          throw new Error(`HTTP错误! 状态: ${response.status}`)
        }
        
        this.result = await response.json()
      } catch (err) {
        this.error = `请求失败: ${err.message}。请确保后端服务运行在 http://10.126.126.2:8088`
      } finally {
        this.loading = false
      }
    },
    
    formatSummary(summary) {
      if (!summary) return ''
      
      return summary
        .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
        .replace(/###\s+(.*?)(\n|$)/g, '<h4>$1</h4>')
        .replace(/##\s+(.*?)(\n|$)/g, '<h3>$1</h3>')
        .replace(/`(.*?)`/g, '<code>$1</code>')
        .replace(/\n\n/g, '</p><p>')
        .replace(/\n/g, '<br>')
        .replace(/^(.*)$/, '<p>$1</p>')
    },
    
    formatValue(value) {
      if (value === null || value === undefined) {
        return 'null'
      }
      return value
    },
    
    copySql() {
      if (!this.result || !this.result.sql) return
      
      navigator.clipboard.writeText(this.result.sql).then(() => {
        this.sqlCopied = true
        setTimeout(() => {
          this.sqlCopied = false
        }, 2000)
      })
    },
    
    exportData() {
      if (!this.result || !this.result.data) return
      
      const dataStr = JSON.stringify(this.result.data, null, 2)
      const blob = new Blob([dataStr], { type: 'application/json' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `aiagent_result_${Date.now()}.json`
      a.click()
      URL.revokeObjectURL(url)
    }
  }
}
</script>

<style scoped>
.ai-agent {
  min-height: 100vh;
  background: var(--bg-page);
  padding: 40px 20px 60px;
}

.header {
  text-align: center;
  margin-bottom: 32px;
  animation: fadeIn 0.5s ease-out;
}

.header h1 {
  font-size: 26px;
  font-weight: 700;
  color: var(--text-strong);
  margin: 0;
}

.query-section {
  max-width: 900px;
  margin: 0 auto 24px;
  animation: slideUp 0.5s ease-out;
}

.input-group {
  display: flex;
  gap: 12px;
  margin-bottom: 10px;
}

textarea {
  flex: 1;
  padding: 14px 16px;
  border: 1px solid var(--border-strong);
  border-radius: var(--radius-sm);
  font-size: 15px;
  font-family: inherit;
  color: var(--text-primary);
  resize: vertical;
  background: var(--bg-elevated);
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

textarea:focus {
  outline: none;
  border-color: var(--brand);
  box-shadow: 0 0 0 3px var(--brand-soft);
}

.analyze-btn {
  padding: 0 28px;
  background: var(--brand);
  color: #fff;
  border: none;
  border-radius: var(--radius-sm);
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.2s ease, box-shadow 0.2s ease;
  min-width: 110px;
}

.analyze-btn:hover:not(:disabled) {
  background: var(--brand-dark);
  box-shadow: var(--shadow-brand);
}

.analyze-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.error-box {
  max-width: 900px;
  margin: 0 auto 24px;
  background: #fef0f0;
  border: 1px solid #fde2e2;
  color: #f56c6c;
  padding: 16px 20px;
  border-radius: var(--radius-sm);
}

.error-box h3 {
  margin: 0 0 8px;
  font-size: 15px;
}

.error-box p {
  margin: 0;
}

.result-section {
  max-width: 1200px;
  margin: 0 auto;
  animation: fadeIn 0.5s ease-out;
}

.sql-box, .summary-box, .data-box {
  background: var(--bg-elevated);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 24px;
  margin-bottom: 20px;
  box-shadow: var(--shadow);
}

.box-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border);
}

.box-header h3 {
  color: var(--text-strong);
  font-size: 16px;
  margin: 0;
}

.copy-btn, .export-btn {
  padding: 7px 14px;
  background: var(--brand-soft);
  color: var(--brand);
  border: 1px solid transparent;
  border-radius: var(--radius-sm);
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  transition: background-color 0.2s ease, color 0.2s ease;
}

.copy-btn:hover, .export-btn:hover {
  background: var(--brand);
  color: #fff;
}

.sql-box pre {
  background: var(--bg-subtle);
  padding: 14px 16px;
  border-radius: var(--radius-sm);
  overflow-x: auto;
  border-left: 3px solid var(--brand);
  margin: 0;
}

.sql-box code {
  color: var(--brand-dark);
  font-family: Menlo, Consolas, 'Courier New', monospace;
  font-size: 13px;
}

.summary-content {
  color: var(--text-regular);
  line-height: 1.8;
}

.summary-content h3, .summary-content h4 {
  color: var(--text-strong);
  margin: 18px 0 10px;
}

.summary-content strong {
  color: var(--text-strong);
}

.summary-content code {
  background: var(--bg-subtle);
  padding: 2px 6px;
  border-radius: 4px;
  color: var(--brand-dark);
  font-family: Menlo, Consolas, 'Courier New', monospace;
}

.table-wrapper {
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

thead {
  background: var(--bg-subtle);
  color: var(--text-primary);
}

th {
  padding: 12px;
  text-align: left;
  font-weight: 600;
  white-space: nowrap;
  border-bottom: 1px solid var(--border-strong);
}

td {
  padding: 10px 12px;
  border-bottom: 1px solid var(--border);
  color: var(--text-regular);
}

tbody tr:hover {
  background: var(--bg-subtle);
}

.empty-state {
  max-width: 640px;
  margin: 48px auto;
  text-align: center;
  color: var(--text-muted);
  animation: fadeIn 0.5s ease-out;
}

.empty-state h3 {
  font-size: 18px;
  margin-bottom: 10px;
  color: var(--text-regular);
}

.empty-state > p {
  font-size: 14px;
  margin-bottom: 24px;
}

.example-queries {
  background: var(--bg-elevated);
  border: 1px solid var(--border);
  padding: 24px;
  border-radius: var(--radius);
  box-shadow: var(--shadow);
  text-align: left;
}

.example-queries h4 {
  margin: 0 0 14px;
  font-size: 14px;
  color: var(--text-strong);
}

.examples {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.example-btn {
  padding: 11px 16px;
  background: var(--bg-subtle);
  color: var(--text-regular);
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: border-color 0.2s ease, color 0.2s ease, background-color 0.2s ease;
  font-size: 14px;
  text-align: left;
}

.example-btn:hover {
  background: var(--brand-soft);
  border-color: #c6e2ff;
  color: var(--brand);
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(16px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 768px) {
  .input-group {
    flex-direction: column;
  }

  .analyze-btn {
    width: 100%;
    height: 44px;
  }

  .box-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  .examples {
    gap: 8px;
  }
}
</style>