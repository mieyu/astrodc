import Vue from 'vue'
import App from './App.vue'
import ElementUI from 'element-ui'
import 'element-ui/lib/theme-chalk/index.css'
import './assets/theme.css'
import './assets/back-navigation.css'
import axios from 'axios'
import router from './router' // 导入路由
import './assets/mobile.css'

Vue.config.productionTip = false
Vue.use(ElementUI)

const apiBaseUrl = process.env.VUE_APP_API_BASE_URL || 'http://localhost:8088'
axios.defaults.baseURL = apiBaseUrl
axios.defaults.withCredentials = true

axios.interceptors.response.use(
    response => response,
    error => {
        const status = error && error.response ? error.response.status : null
        const url = error && error.config && error.config.url ? error.config.url : ''
        if (status === 401 && !url.startsWith('/api/access/')) {
            window.dispatchEvent(new CustomEvent('astronomy-access-required'))
        }
        return Promise.reject(error)
    }
)

new Vue({
    router, // 在Vue实例中注册路由
    render: h => h(App),
}).$mount('#app')
