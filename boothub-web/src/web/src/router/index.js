import Vue from 'vue'
import ElementUI from 'element-ui'
import 'element-ui/lib/theme-chalk/index.css'
import Router from 'vue-router'
import Home from '@/components/Home'
import CLI from '@/components/CLI'
import Docs from '@/components/Docs'
import Manager from '@/components/Manager'
import Generate from '@/components/Generate'

Vue.use(ElementUI)
Vue.use(Router)

export default new Router({
  routes: [
    {path: '/home/:exec?/:skeletonUrl?', name: 'home', component: Home},
    {path: '/cli', name: 'cli', component: CLI},
    {path: '/docs', name: 'docs', component: Docs},
    {path: '/manager', name: 'manager', component: Manager},

    { path: '/generate/:skeletonUrl', component: Generate },
  ]
})
