// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.
import Vue from 'vue'
import Vuex from 'vuex'
import App from './App'
import router from './router'
import './favicon/favicon'
import VueGitHubButtons from 'vue-github-buttons';

Vue.config.productionTip = false

Vue.use(Vuex)
Vue.use(VueGitHubButtons)

const store = new Vuex.Store({
  strict: true,
  state: {
    selectedSkeleton: null,
    skeletonUrl: null,
    exec: false,

    loggedInUserId: null,
    loggedInDisplayName: null,
    loggedInPictureUrl: null,
    loggedInProfileUrl: null,
    loggedInInfoOnly: true,
  },
  mutations: {
    setSelectedSkeleton (state, skeleton) {
      console.log('skeleton mutated to: ' + skeleton);
      state.selectedSkeleton = skeleton
    },
    setSkeletonUrl (state, skeletonUrl) {
      console.log('skeletonUrl mutated to: ' + skeletonUrl);
      state.skeletonUrl = skeletonUrl;
    },
    setExec (state, exec) {
      console.log('exec mutated to: ' + exec + ' (' + typeof (exec) + ')');
      state.exec = exec;
    },
    loginUser (state, userId) {
      state.loggedInUserId = userId;
    },
    updateState (state, stateObj) {
      state.loggedInUserId = stateObj.loggedInUserId
      state.loggedInDisplayName = stateObj.loggedInDisplayName
      state.loggedInPictureUrl = stateObj.loggedInPictureUrl
      state.loggedInProfileUrl = stateObj.loggedInProfileUrl
      state.loggedInInfoOnly = stateObj.loggedInInfoOnly
    },
  }
})

/* eslint-disable no-new */
const app = new Vue({
  store,
  data: {
    activeIndex: 'home'
  },
  template: '<App/>',
  components: { App },
  router: router
})

app.$mount('#app')
