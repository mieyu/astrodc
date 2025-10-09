<template>
  <el-form :model="form" ref="queryForm" label-width="80px" class="condition-form">
    <div class="back-link-container">
      <a href="#" class="el-icon-back" @click.prevent="goBack"></a>
    </div>
    <h2>查看所有图像</h2>
    <el-button type="primary" @click="QueryAll">查询所有图像</el-button>


    <hr>
    <h2>图像信息检索</h2>
    <el-table :data="tableData" border style="width: 100%">
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
    <div class="button-group">
      <el-button type="primary" @click="onQuery">条件查询</el-button>
      <el-button @click="onReset">重置</el-button>
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

    async fetchSelectOptions() {
      try {
        const response = await axios.get('/observation/options');
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
.condition-form {
  position: relative; /* 为绝对定位的返回链接提供基准 */
  padding: 20px;
  padding-top: 50px; /* 为返回链接留出空间 */
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
.button-group {
  margin-top: 20px;
}
</style>