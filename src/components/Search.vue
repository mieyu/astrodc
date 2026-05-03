<template>
  <el-form :model="form" ref="queryForm" label-width="80px" class="condition-form">
    <div class="back-link-container">
      <a href="#" class="el-icon-back" @click.prevent="goBack"></a>
    </div>

    <!-- 快速查询区域 -->
    <div class="quick-actions-section">
      <div class="section-header">
        <h2><i class="el-icon-view"></i> 快速查询</h2>
        <p>快速浏览所有图像数据或进行数据可视化分析</p>
      </div>
      <div class="quick-actions">
        <el-button type="primary" size="large" icon="el-icon-search" @click="QueryAll">
          查询所有图像
        </el-button>
        <el-button type="success" size="large" icon="el-icon-data-analysis" @click="handleDataVisualization">
          数据可视化
        </el-button>
      </div>
    </div>

    <!-- 条件查询区域 -->
    <div class="condition-section">
      <div class="section-header">
        <h2><i class="el-icon-filter"></i> 条件查询</h2>
        <p>根据特定条件筛选和检索图像数据</p>
      </div>
      <div class="table-container">
        <el-table :data="tableData" border class="condition-table" :header-cell-style="tableHeaderStyle">
      <el-table-column label="查询字段" width="150px">
        <template slot-scope="scope">
          <span>{{ scope.row.name === 'naxis' ? 'naxis1/naxis2' : scope.row.name }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="addCondition" label="添加条件" width="100px">
        <template slot-scope="scope">
          <el-checkbox :label="scope.row.name" v-model="selectedConditions">&nbsp;</el-checkbox>
        </template>
      </el-table-column>

      <el-table-column prop="value" label="Value">
        <template slot-scope="scope">
          <template v-if="scope.row.name === 'pwd' || scope.row.name === 'fit-name'">
            <el-input v-model="form[scope.row.name]" placeholder="请输入文本" :disabled="!selectedConditions.includes(scope.row.name)"></el-input>
          </template>
          <template v-else-if="scope.row.name ==='simple'">
            <el-radio-group v-model="form[scope.row.name]" :disabled="!selectedConditions.includes(scope.row.name)">
              <el-radio :label="1">T</el-radio>
              <el-radio :label="0">F</el-radio>
            </el-radio-group>
          </template>
          <template v-else-if="isRangeField(scope.row.name)">
            <div class="input-pair-container">
              <el-input v-model="form[scope.row.name + 'Min']" type="text" placeholder="最小值" :disabled="!selectedConditions.includes(scope.row.name)"></el-input>
              <el-input v-model="form[scope.row.name + 'Max']" type="text" placeholder="最大值" :disabled="!selectedConditions.includes(scope.row.name)"></el-input>
            </div>
          </template>
          <template v-else-if="scope.row.name === 'otcd' || scope.row.name === 'rot-code'">
            <el-radio-group v-model="form[scope.row.name]" :disabled="!selectedConditions.includes(scope.row.name)">
              <el-radio :label="1">正常</el-radio>
              <el-radio :label="0">错误</el-radio>
            </el-radio-group>
          </template>
          <template v-else-if="isSelectField(scope.row.name)">
            <el-select v-model="form[scope.row.name]" placeholder="请选择" filterable clearable :disabled="!selectedConditions.includes(scope.row.name)">
              <el-option v-for="item in selectOptions[scope.row.name]" :key="item" :label="item" :value="item"></el-option>
            </el-select>
          </template>
          <template v-else-if="scope.row.name === 'date-obs'">
            <div class="input-pair-container">
              <el-date-picker v-model="form[scope.row.name + 'Min']" type="datetime" placeholder="选择开始日期时间" :disabled="!selectedConditions.includes(scope.row.name)"></el-date-picker>
              <el-date-picker v-model="form[scope.row.name + 'Max']" type="datetime" placeholder="选择结束日期时间" :disabled="!selectedConditions.includes(scope.row.name)"></el-date-picker>
            </div>
          </template>
          <template v-else-if="scope.row.name === 'extime'">
            <el-input v-model="form[scope.row.name]" type="text" placeholder="请输入数值" :disabled="!selectedConditions.includes(scope.row.name)"></el-input>
          </template>
          <template v-else-if="scope.row.name === 'flipx' || scope.row.name === 'flipy'">
            <el-radio-group v-model="form[scope.row.name]" :disabled="!selectedConditions.includes(scope.row.name)">
              <el-radio :label="1">翻转</el-radio>
              <el-radio :label="0">不翻转</el-radio>
            </el-radio-group>
          </template>
        </template>
      </el-table-column>
      <el-table-column prop="simple" label="示例 (simple)"></el-table-column>
      <el-table-column prop="type" label="类型 (type)" width="120px"></el-table-column>
        </el-table>
      </div>
      
      <div class="action-buttons">
        <el-button type="primary" size="medium" icon="el-icon-search" @click="onQuery">
          执行查询
        </el-button>
        <el-button size="medium" icon="el-icon-refresh" @click="onReset">
          重置条件
        </el-button>
      </div>
    </div>
  </el-form>
</template>

<script>
import axios from 'axios';

const createInitialForm = () => ({
  pwd: '', 'fit-name': '', simple: null, extime: '', object: '', imagetyp: '', tele: '', teleap: '', telefl: '', filter: '', otcd: null, flipx: null, flipy: null, 'rot-code': null,
  bitpixMin: '', bitpixMax: '', naxisMin: '', naxisMax: '', naxis1Min: '', naxis1Max: '', naxis2Min: '', naxis2Max: '', bscaleMin: '', bscaleMax: '', bzeroMin: '', bzeroMax: '', 'date-obsMin': null, 'date-obsMax': null, 'r-centerMin': '', 'r-centerMax': '', 'd-centerMin': '', 'd-centerMax': '', xpixszMin: '', xpixszMax: '', ypixszMin: '', ypixszMax: '', xbinningMin: '', xbinningMax: '', ybinningMin: '', ybinningMax: '', 'rt-angleMin': '', 'rt-angleMax:': ''
});

export default {
  name: 'ConditionForm',
  data() {
    return {
      form: { // 更新表单的初始状态
        pwd: '', 'fit-name': '', simple: null, extime: '', object: '', imagetyp: '', tele: '', teleap: '', telefl: '', filter: '', otcd: null, flipx: null, flipy: null, 'rot-code': null, naxis: null, // 新增 naxis
        bitpixMin: '', bitpixMax: '', bscaleMin: '', bscaleMax: '', bzeroMin: '', bzeroMax: '', 'date-obsMin': null, 'date-obsMax': null, 'r-centerMin': '', 'r-centerMax': '', 'd-centerMin': '', 'd-centerMax': '', xpixszMin: '', xpixszMax: '', ypixszMin: '', ypixszMax: '', xbinningMin: '', xbinningMax: '', ybinningMin: '', ybinningMax: '', 'rt-angleMin': '', 'rt-angleMax': ''
      },
      selectedConditions: [],
      selectOptions: { // 更新下拉选项的结构
        object: [], imagetyp: [], tele: [], teleap: [], telefl: [], filter: [], bitpix: [], naxis: []
      },
      tableData: [ // 更新表格的定义，将 naxis1 和 naxis2 合并
        { name: 'pwd',       simple: '/path/to/file.fits', type: 'varchar(255)' },
        { name: 'fit-name', simple: 'file_name',          type: 'text' },
        { name: 'simple',    simple: 'T',                  type: 'int' },
        { name: 'bitpix',    simple: '16',                 type: 'int' },
        { name: 'naxis',     simple: '2048x2048',          type: 'varchar(255)' },
        { name: 'bscale',    simple: '1.0',                type: 'double' },
        { name: 'bzero',     simple: '32768.0',            type: 'double' },
        { name: 'object',    simple: 'S0',                 type: 'text' },
        { name: 'imagetyp',  simple: 'LIGHT',              type: 'text' },
        { name: 'date-obs',  simple: '2012-04-18T18:11:39',type: 'datetime' },
        { name: 'extime',    simple: '28.0',               type: 'double' },
        { name: 'otcd',      simple: '0',                  type: 'int' },
        { name: 'tele',      simple: 'YAO100',             type: 'text' },
        { name: 'teleap',    simple: '100',                type: 'int' },
        { name: 'telefl',    simple: '13300',              type: 'int' },
        { name: 'r-center', simple: '180.5',              type: 'double' },
        { name: 'd-center', simple: '30.2',               type: 'double' },
        { name: 'xpixsz',    simple: '13.5',               type: 'double' },
        { name: 'ypixsz',    simple: '13.5',               type: 'double' },
        { name: 'xbinning',  simple: '1',                  type: 'int' },
        { name: 'ybinning',  simple: '1',                  type: 'int' },
        { name: 'filter',    simple: 'Luminance',          type: 'text' },
        { name: 'rt-angle', simple: '0.0',                type: 'double' },
        { name: 'flipx',     simple: '0',                  type: 'int' },
        { name: 'flipy',     simple: '0',                  type: 'int' },
        { name: 'rot-code',  simple: '0',                  type: 'int' }
      ]
    };
  },
  methods: {
    goBack() {
      this.$router.go(-1);
    },
    isRangeField(fieldName) {
      const fields = ['bzero', 'r-center', 'd-center', 'xpixsz', 'ypixsz', 'xbinning', 'ybinning', 'bscale', 'rt-angle'];
      return fields.includes(fieldName);
    },
    isSelectField(fieldName) {
      const fields = ['object', 'imagetyp', 'tele', 'teleap', 'telefl', 'filter', 'bitpix', 'naxis'];
      return fields.includes(fieldName);
    },
    QueryAll() {
      const finalParams = {};
      Object.keys(finalParams).length == 0
      this.$router.push({ path: '/table', query: finalParams });
    },
    formatDateTime(date) {
      if (!date) return null;
      const year = date.getFullYear();
      const month = (date.getMonth() + 1).toString().padStart(2, '0');
      const day = date.getDate().toString().padStart(2, '0');
      const hours = date.getHours().toString().padStart(2, '0');
      const minutes = date.getMinutes().toString().padStart(2, '0');
      const seconds = date.getSeconds().toString().padStart(2, '0');
      return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
    },
    onQuery() {
      const finalParams = {};
      if (this.selectedConditions.length === 0) {
        this.$message.warning('请至少选择一个查询条件');
        return;
      }

      this.selectedConditions.forEach(field => {
        if (field === 'date-obs') {
          // ++ 关键修正点: 调用格式化方法 ++
          const minVal = this.formatDateTime(this.form[field + 'Min']);
          const maxVal = this.formatDateTime(this.form[field + 'Max']);

          if (minVal) finalParams['dateObsMin'] = minVal;
          if (maxVal) finalParams['dateObsMax'] = maxVal;

        } else if (this.isRangeField(field)) {
          const minVal = this.form[field + 'Min'];
          const maxVal = this.form[field + 'Max'];
          const minKey = (field + 'Min').replace(/-(\w)/g, (m, g) => g.toUpperCase());
          const maxKey = (field + 'Max').replace(/-(\w)/g, (m, g) => g.toUpperCase());
          if (minVal !== undefined && minVal !== '' && minVal !== null) finalParams[minKey] = minVal;
          if (maxVal !== undefined && maxVal !== '' && maxVal !== null) finalParams[maxKey] = maxVal;

        } else {
          const val = this.form[field];
          if (val !== undefined && val !== '' && val !== null) {
            const key = field.replace(/-(\w)/g, (m, g) => g.toUpperCase());
            finalParams[key] = val;
          }
        }
      });

      if (Object.keys(finalParams).length === 0) {
        this.$message.warning('请为选中的条件输入查询值');
        return;
      }
      this.$router.push({ path: '/table', query: finalParams });
    },
    onReset() {
      this.form = {
        pwd: '', 'fit-name': '', simple: null, extime: '', object: '', imagetyp: '', tele: '', teleap: '', telefl: '', filter: '', otcd: null, flipx: null, flipy: null, 'rot-code': null, naxis: null,
        bitpixMin: '', bitpixMax: '', bscaleMin: '', bscaleMax: '', bzeroMin: '', bzeroMax: '', 'date-obsMin': null, 'date-obsMax': null, 'r-centerMin': '', 'r-centerMax': '', 'd-centerMin': '', 'd-centerMax': '', xpixszMin: '', xpixszMax: '', ypixszMin: '', ypixszMax: '', xbinningMin: '', xbinningMax: '', ybinningMin: '', ybinningMax: '', 'rt-angleMin': '', 'rt-angleMax': ''
      };
      this.selectedConditions = [];
    },
    handleDataVisualization() {
      // 跳转到数据可视化页面
      this.$router.push({ path: '/data-visualization' });
    },
    tableHeaderStyle() {
      return {
        backgroundColor: '#f5f7fa',
        color: '#606266',
        fontWeight: 'bold'
      };
    },

    async fetchSelectOptions() {
      try {
        const response = await axios.get('/api/image/own/options');
        if (response.data.code === 1) {
          this.selectOptions = response.data.data;
        } else {
          console.error('获取下拉选项失败:', response.data.message);
        }
      } catch (error) {
        console.error('请求下拉选项时发生错误:', error);
      }
    }
  },
  created() {
    this.fetchSelectOptions();
  }
};
</script>

<style scoped>
/* 基础样式 */
.condition-form {
  position: relative;
  padding: 20px;
  padding-top: 50px;
}

.back-link-container {
  position: absolute;
  top: 15px;
  left: 20px;
}

.back-link-container a {
  text-decoration: none;
  color: #409EFF;
  font-size: 14px;
}

.back-link-container a:hover {
  text-decoration: underline;
}

.input-pair-container {
  display: flex;
  gap: 10px;
  align-items: center;
}

/* 保留的快速查询和条件查询区域样式 */
.quick-actions-section,
.condition-section {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  padding: 30px;
  margin-bottom: 30px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

.section-header {
  text-align: center;
  margin-bottom: 30px;
}

.section-header h2 {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  font-size: 22px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 10px 0;
}

.section-header h2 i {
  color: #409EFF;
  font-size: 24px;
}

.section-header p {
  color: #909399;
  font-size: 14px;
  margin: 0;
}

.quick-actions {
  display: flex;
  justify-content: center;
  gap: 20px;
  flex-wrap: wrap;
}

.quick-actions .el-button {
  min-width: 160px;
  height: 48px;
  font-size: 16px;
  font-weight: 500;
  border-radius: 12px;
  transition: all 0.3s ease;
}

.quick-actions .el-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.15);
}

.table-container {
  margin: 25px 0;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.condition-table {
  width: 100%;
}

.condition-table .el-table__header {
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
}

.condition-table .el-table__row {
  transition: all 0.3s ease;
}

.condition-table .el-table__row:hover {
  background-color: #f8f9fa;
  transform: scale(1.01);
}

.condition-table .el-checkbox {
  margin-right: 0;
}

.condition-table .el-input,
.condition-table .el-select,
.condition-table .el-date-picker {
  width: 100%;
}

.condition-table .el-radio-group {
  display: flex;
  gap: 15px;
}

.action-buttons {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
}

.action-buttons .el-button {
  min-width: 140px;
  height: 44px;
  font-size: 15px;
  font-weight: 500;
  border-radius: 10px;
  transition: all 0.3s ease;
}

.action-buttons .el-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.15);
}

/* 按钮图标样式 */
.el-button i {
  margin-right: 6px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .quick-actions {
    flex-direction: column;
    align-items: center;
  }
  
  .quick-actions .el-button {
    width: 100%;
    max-width: 300px;
  }
  
  .input-pair-container {
    flex-direction: column;
    gap: 8px;
  }
  
  .action-buttons {
    flex-direction: column;
    align-items: center;
  }
  
  .action-buttons .el-button {
    width: 100%;
    max-width: 250px;
  }
}
</style>