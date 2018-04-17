<template>
  <div id="app">
    <table border="0" width="100%">
      <tr>
      <td>
        <img height="60" src="./assets/logo-text.png">
      </td>
      <td><div style="width: 20px;">&nbsp;</div></td>
      <td style="width: 60%; white-space: nowrap;">
        <el-menu theme="light" :default-active="this.$router.history.current.name" class="el-menu-demo" mode="horizontal" :router="true">
          <el-menu-item index="home" :route="{path:'/home'}">Home</el-menu-item>
          <el-menu-item index="cli" :route="{path:'/cli'}">CLI</el-menu-item>
          <el-menu-item index="docs" :route="{path:'/docs'}">Guides</el-menu-item>
          <el-menu-item index="manager" :route="{path:'/manager'}">Template Manager</el-menu-item>
        </el-menu>
      </td>
      <td><div style="width: 10px;">&nbsp;</div></td>
        <td style="vertical-align: middle; white-space: nowrap;"><div style="white-space: nowrap; width: 130px;"><gh-btns-star slug="boothub-org/boothub" show-count></gh-btns-star></div></td>
      <td><div style="width: 10px;">&nbsp;</div></td>
      <td align="right" style="vertical-align: middle; alignment: right; white-space: nowrap; width: 30%;">
        <el-button type="primary" v-if="!loggedInUserId" @click="loginRepo" v-loading="busyLogin">Sign In with GitHub</el-button>
        <img v-if="loggedInPictureUrl" :src="loggedInPictureUrl" style="height: 32px;" :class="usernameStyle"/>
        <el-dropdown v-if="loggedInUserId" @command="handleLoggedInCommand">
          <span class="el-dropdown-link" style="font-weight: bold;vertical-align: 10px;">
            <span :class="usernameStyle">{{loggedInDisplayName || loggedInUserId}}</span>
            <i class="el-icon-caret-bottom el-icon--right"></i>
          </span>
          <el-dropdown-menu  class="el-dropdown-link" slot="dropdown">
            <el-dropdown-item command="logout" v-loading="busyLogout">Sign Out</el-dropdown-item>
            <el-dropdown-item v-if="loggedInInfoOnly" command="loginRepo" v-loading="busyLogout">Repo Sign In</el-dropdown-item>
          </el-dropdown-menu>
        </el-dropdown>
      </td>
      </tr>
    </table>

    <transition>
      <keep-alive>
        <router-view></router-view>
      </keep-alive>
    </transition>
  </div>
</template>

<script>
import { mapState } from 'vuex'
import axios from 'axios';
import 'vue-github-buttons/dist/vue-github-buttons.css';
require('font-awesome/css/font-awesome.css');

export default {
  name: 'app',
  data () {
    return {
      busyLogin: false,
      busyLogout: false,
    }
  },
  computed: {
    ...mapState(['loggedInUserId', 'loggedInDisplayName', 'loggedInPictureUrl', 'loggedInProfileUrl', 'loggedInInfoOnly', 'skeletonUrl', 'exec']),
    usernameStyle: function() {
      return this.loggedInInfoOnly ? 'logged-info' : 'logged-repo';
    },

  },
  methods: {
    loginRepo() {
      this.busyLogin = true;
      axios.get('/app/auth/logout')
          .catch(error => {this.busyLogout = false; this.$message.error('Failed to sign out');})
          .then(response => {
            var route = this.$router.history.current.name || 'home';
            var loc = '/app/auth/login/' + route;
            if(route === 'home') {
              if(this.exec || this.skeletonUrl) {
                var delim = '?';
                if(this.exec) {
                  loc += delim + 'exec=' + this.exec;
                  delim = '&';
                }
                if(this.skeletonUrl) {
                  loc += delim + 'skeletonUrl=' + encodeURIComponent(this.skeletonUrl);
                }
              }
            }
            window.location.href = loc;
            this.busyLogout = false;
          });
    },
    handleLoggedInCommand(command) {
      switch(command) {
        case 'logout':
          this.busyLogout = true;
          axios.get('/app/auth/logout')
              //.then(response => this.$message(response.data)})
              .catch(error => {this.busyLogout = false; this.$message.error('Failed to sign out');})
              .then(response => {
                this.updateState();
                this.busyLogout = false;
              });
          break;
        case 'loginRepo': this.loginRepo(); break;
        default: this.$message.error('Unknown command: ' + command);
      }
    },
    updateState() {
      return axios.get('/state')
          .then(response => {
            console.log('Retrieved state: ' + JSON.stringify(response.data, null, 2))
            this.$store.commit('updateState', response.data)
          })
          .catch(error => this.$message.error('Failed to retrieve application state'));
    }
  },
  created() {
    console.log('fallback: ' + this.$router.fallback);
    console.log('mode: ' + this.$router.mode);
    //console.log('options: ' + JSON.stringify(this.$router.options, null, 2));
    console.log('history.current: ' + JSON.stringify(this.$router.history.current));
    console.log('history.pending: ' + JSON.stringify(this.$router.history.pending));

    this.updateState().then(response => {
      var routeName = this.$router.history.current.name || 'home';
      console.log('routeName: ' + routeName);
      this.$router.push({name: routeName, params: this.$router.history.current.params});
    });

  }
}
</script>

<style>
#app {
/*
  font-family: 'Avenir', Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-align: center;
  color: #2c3e50;
  margin-top: 60px;
*/
  margin-top: 0px;
}

/* ElementUI sets word-break to 'break-all', which is the usual configuration for Chinese text. We override it with 'break-word' */
/* See also: https://github.com/ElemeFE/element/issues/3821 (not fixed, as of this writing) */
.el-table .cell {
    word-break: break-word;
}

</style>
