import Vue from 'vue'
import App from './App.vue'
import ElementUI from 'element-ui'
import 'element-ui/lib/theme-chalk/index.css'
import './assets/back-navigation.css'
import axios from 'axios'
import router from './router' // 导入路由

Vue.config.productionTip = false
Vue.use(ElementUI)

// 本地访问接口
// axios.defaults.baseURL = 'http://127.0.0.2:8088'


// 远程访问接口
axios.defaults.baseURL = 'http://10.126.126.2:8088'

new Vue({
    router, // 在Vue实例中注册路由
    render: h => h(App),
}).$mount('#app')
