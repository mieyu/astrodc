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
      <!-- <div class="sql-box">
        <div class="box-header">
          <h3>📝 生成的SQL查询</h3>
          <button @click="copySql" class="copy-btn">
            {{ sqlCopied ? '✓ 已复制' : '📋 复制' }}
          </button>
        </div>
        <pre><code>{{ result.sql }}</code></pre>
      </div> -->

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
        const response = await fetch('http://127.0.0.1:8000/analyze', {
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
        this.error = `请求失败: ${err.message}。请确保后端服务运行在 http://127.0.0.1:8000`
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
  background: linear-gradient(135deg, #ffffff 0%, #38395a 100%);
  padding: 40px 20px;
}

.header {
  text-align: center;
  color: white;
  margin-bottom: 40px;
  animation: fadeIn 0.6s ease-out;
}

.header h1 {
  font-size: 2.5em;
  margin-bottom: 10px;
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.2);
}

.header p {
  font-size: 1.1em;
  opacity: 0.9;
}

.query-section {
  max-width: 900px;
  margin: 0 auto 30px;
  animation: slideUp 0.6s ease-out;
}

.input-group {
  display: flex;
  gap: 15px;
  margin-bottom: 10px;
}

textarea {
  flex: 1;
  padding: 15px;
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-family: inherit;
  resize: vertical;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  transition: all 0.3s;
}

textarea:focus {
  outline: none;
  box-shadow: 0 6px 12px rgba(0, 0, 0, 0.15);
  transform: translateY(-2px);
}

.analyze-btn {
  padding: 15px 30px;
  background: linear-gradient(135deg, #cecece 0%, #818396 100%);
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: bold;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  min-width: 120px;
}

.analyze-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 12px rgba(0, 0, 0, 0.2);
}

.analyze-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.error-box {
  max-width: 900px;
  margin: 0 auto 30px;
  background: #ff6b6b;
  color: white;
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}

.error-box h3 {
  margin-bottom: 10px;
}

.result-section {
  max-width: 1200px;
  margin: 0 auto;
  animation: fadeIn 0.6s ease-out;
}

.sql-box, .summary-box, .data-box {
  background: white;
  border-radius: 12px;
  padding: 25px;
  margin-bottom: 25px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}

.box-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 2px solid #f0f0f0;
}

.box-header h3 {
  color: #333;
  font-size: 1.3em;
}

.copy-btn, .export-btn {
  padding: 8px 16px;
  background: #717379;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s;
}

.copy-btn:hover, .export-btn:hover {
  background: #87888d;
  transform: translateY(-2px);
}

.sql-box pre {
  background: #f8f9fa;
  padding: 15px;
  border-radius: 8px;
  overflow-x: auto;
  border-left: 4px solid #777b8b;
}

.sql-box code {
  color: #e83e8c;
  font-family: 'Courier New', monospace;
  font-size: 14px;
}

.summary-content {
  color: #444;
  line-height: 1.8;
}

.summary-content h3, .summary-content h4 {
  color: #636671;
  margin: 20px 0 10px;
}

.summary-content strong {
  color: #8d8496;
}

.summary-content code {
  background: #f8f9fa;
  padding: 2px 6px;
  border-radius: 4px;
  color: #8f7581;
  font-family: 'Courier New', monospace;
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
  background: linear-gradient(135deg, #797b86 0%, #cfced0 100%);
  color: white;
}

th {
  padding: 12px;
  text-align: left;
  font-weight: 600;
  white-space: nowrap;
}

td {
  padding: 10px 12px;
  border-bottom: 1px solid #e9ecef;
}

tbody tr:hover {
  background: #f8f9fa;
}

.empty-state {
  max-width: 600px;
  margin: 60px auto;
  text-align: center;
  color: white;
  animation: fadeIn 0.6s ease-out;
}

.empty-icon {
  font-size: 4em;
  margin-bottom: 20px;
}

.empty-state h3 {
  font-size: 1.8em;
  margin-bottom: 10px;
}

.empty-state p {
  font-size: 1.1em;
  opacity: 0.9;
  margin-bottom: 30px;
}

.example-queries {
  background: rgba(255, 255, 255, 0.1);
  padding: 25px;
  border-radius: 12px;
  backdrop-filter: blur(10px);
}

.example-queries h4 {
  margin-bottom: 15px;
  font-size: 1.1em;
}

.examples {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.example-btn {
  padding: 12px 20px;
  background: rgba(255, 255, 255, 0.2);
  color: white;
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  font-size: 14px;
}

.example-btn:hover {
  background: rgba(255, 255, 255, 0.3);
  transform: translateX(5px);
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
    transform: translateY(20px);
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