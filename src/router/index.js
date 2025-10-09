import Vue from 'vue'
import VueRouter from 'vue-router'
import Main from '../components/Main.vue'
import Search from '../components/Search.vue'
import TableList from '../components/TableList.vue'
import FileBrowser from '../components/FileBrowser.vue'
import FitsParser from '../components/FitsParser.vue'
import Paper from '../components/Paper.vue';

Vue.use(VueRouter)

const routes = [
    {
        path: '/',
        name: 'Main',
        component: Main
    },
    {
        path: '/search',
        name: 'Search',
        component: Search
    },
    {
        path: '/table',
        name: 'TableList',
        component: TableList
    },
    {
        path: '/files',
        name: 'FileBrowser',
        component: FileBrowser
    },
    {
        path: '/fits-parser',
        name: 'FitsParser',
        component: FitsParser
    },
    {
        path: '/paper',
        name: 'Paper',
        component: Paper  // 新添加的路由
    },
]

const router = new VueRouter({
    mode: 'history',
    base: process.env.BASE_URL,
    routes
})

export default router