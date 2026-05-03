import Vue from 'vue'
import VueRouter from 'vue-router'
import Main from '../components/Main.vue'
import Search from '../components/Search.vue'
import TableList from '../components/TableList.vue'
import FileBrowser from '../components/FileBrowser.vue'
import FitsParser from '../components/FitsParser.vue'
import Paper from '../components/Paper.vue'
import DataVisualization from '../components/DataVisualization.vue'
import AiAgent from '../components/AiAgent.vue'
import OwnImagesHub from '../components/OwnImagesHub.vue'
import Placeholder from '../components/Placeholder.vue'

Vue.use(VueRouter)

const routes = [
    { path: '/', name: 'Main', component: Main },

    // 数据存储 → 图像 → 自有图像
    {
        path: '/data/images/own',
        name: 'OwnImagesHub',
        component: OwnImagesHub,
        meta: { title: '自有图像' }
    },
    {
        path: '/data/images/own/search',
        name: 'Search',
        component: Search,
        meta: { title: '图像信息检索' }
    },
    {
        path: '/data/images/own/table',
        name: 'TableList',
        component: TableList,
        meta: { title: '查询结果' }
    },
    {
        path: '/data/images/own/files',
        name: 'FileBrowser',
        component: FileBrowser,
        meta: { title: '文件数据' }
    },
    {
        path: '/data/images/own/fits-parser',
        name: 'FitsParser',
        component: FitsParser,
        meta: { title: '解析FITS头文件' }
    },
    {
        path: '/data/images/own/visualization',
        name: 'DataVisualization',
        component: DataVisualization,
        meta: { title: '数据可视化' }
    },

    // 数据存储 - 占位
    { path: '/data/images/others', component: Placeholder, meta: { title: '其他图像' } },
    { path: '/data/positioning/raw', component: Placeholder, meta: { title: '定位结果(IMCCE原始定位数据)' } },
    { path: '/data/positioning/normalized', component: Placeholder, meta: { title: '规范后的定位结果(观测星表)' } },
    { path: '/data/catalog-bias', component: Placeholder, meta: { title: '星表偏差修正表' } },
    { path: '/data/catalog-bias/display', component: Placeholder, meta: { title: '星表偏差修正表 - 数据展示' } },
    { path: '/data/spectrum', component: Placeholder, meta: { title: '光谱数据' } },
    { path: '/data/spectrum/release', component: Placeholder, meta: { title: '光谱数据 - 空间释放数据' } },

    // 网站服务 - 占位
    { path: '/services/observation/ephemeris', component: Placeholder, meta: { title: '天然卫星星历与寻星图工具' } },
    { path: '/services/observation/exposure-calculator', component: Placeholder, meta: { title: '曝光时间计算器' } },
    { path: '/services/prediction/imcce-api', component: Placeholder, meta: { title: '历表位置 (IMCCE API 查询)' } },
    { path: '/services/prediction/jpl-de', component: Placeholder, meta: { title: 'JPL DE系列 + 卫星模型' } },

    // 全局功能
    { path: '/paper', name: 'Paper', component: Paper },
    { path: '/aiagent', name: 'AiAgent', component: AiAgent },

    // 旧路径兼容(redirect 保留 query)
    { path: '/search', redirect: '/data/images/own/search' },
    { path: '/table', redirect: '/data/images/own/table' },
    { path: '/files', redirect: '/data/images/own/files' },
    { path: '/fits-parser', redirect: '/data/images/own/fits-parser' },
    { path: '/data-visualization', redirect: '/data/images/own/visualization' }
]

const router = new VueRouter({
    mode: 'history',
    base: process.env.BASE_URL,
    routes
})

export default router
