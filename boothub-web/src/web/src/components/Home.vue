<template>
  <div class="home" style="margin-left:10px;margin-right:10px;">
    <div v-show="page == 'init'">
      <p>&nbsp;</p>
      <p><b>Choose a skeleton type from the table below.</b></p>
      <el-row :gutter="8">
        <el-col :span="14">
          <el-table
              ref="skeletonTable"
              :data="skeletons"
              v-loading="skeletons.length === 0"
              element-loading-text="Loading project skeletons..."
              highlight-current-row
              @current-change="setSelectedRow"
              empty-text=" "
              height="320"
              style="width: 100%">
              <el-table-column
                label="Name"
                prop="name"
                width="210">
              </el-table-column>
              <el-table-column
                label="ID"
                prop="id"
                width="210">
              </el-table-column>
              <el-table-column style="word-wrap: normal;"
                label="About"
                prop="caption"
                min-width="210">
              </el-table-column>
            </el-table>
            <br/><el-button type="primary" :disabled="!selectedSkeleton" @click="generate(true)">Generate project</el-button>

            <el-dialog title="You are currently not signed in to GitHub." :visible.sync="signInDialogVisible">
              <el-row :gutter="8">
                <table>
                  <tr>
                    <td class="dialog-card">
                      <table class="dialog-table">
                        <tr><td class="dialog-top">
                          If you sign in, you can choose whether the generated project should be created on GitHub or offered as a zip file for download.
                          <p/>Signing in is helpful even if you choose that your project should not be created on GitHub,
                          because the program will no longer ask you to enter information about your account.
                        </td></tr>
                        <tr><td class="dialog-bottom">
                          <el-button class="button" type="primary" @click="login" v-loading="busyLogin">Sign in and proceed</el-button>
                        </td></tr>
                      </table>
                    </td>
                    <td class="dialog-fill">&nbsp;</td>
                    <td class="dialog-card">
                      <table class="dialog-table">
                        <tr><td class="dialog-top">
                          You can proceed without signing in.
                          In this case, you are able to download a zip file containing the generated project,
                          but you cannot opt to have your project created on GitHub.
                          <p/>You will still be asked to provide a GitHub username, but no attempt will be made to connect to this account.
                        </td></tr>
                        <tr><td class="dialog-bottom">
                          <el-button class="button" type="primary" @click="signInDialogVisible = false; generate(false);">Proceed without signing in</el-button>
                        </td></tr>
                      </table>
                    </td>
                  </tr>
                </table>
              </el-row>
            </el-dialog>

        </el-col>
        <el-col :span="10">
          <el-card v-if="selectedSkeleton" class="infobox">
            <div slot="header" class="clearfix">
              <span class="info-name">{{selectedSkeleton.name}}</span>
            </div>
            <div>
              <span class="info-version">Version: {{selectedSkeleton.version}}</span>
              <span class="info-homepage" v-if="selectedSkeleton.homepage"><a :href="selectedSkeleton.homepage" target="_blank">Homepage</a></span>
            </div>
            <p>&nbsp;</p>
            <div v-html="md2html(selectedSkeleton.description, 'No description')"></div>
          </el-card>
        </el-col>
      </el-row>
    </div>
    <div v-show="page == 'textTerm'">
      <div style="margin-top:10px;margin-bottom: 10px;"><b>Skeleton: {{skeletonName}}</b></div>
      <div id="textterm" ref="textterm" class="textterm-pane">
        <span class="textterm-pair">
            <span class="textterm-prompt"></span>
            <span contenteditable="true" class="textterm-input"></span>
        </span>
      </div>
      <el-button type="danger" v-loading="waitingForAbort" @click="abortGeneration" style="margin-top:10px;">Abort</el-button>
    </div>
    <div v-show="page == 'result'">
      <div v-show="generationErrorMessage" class="msg-danger space-top">&nbsp;<p style="margin-left:20px;margin-right:20px;">{{generationErrorMessage}}</p>&nbsp;</div>
      <el-container class="space-top">
        <el-header>
          <div>
            <span v-show="gitHubRepoLink" class="space-top">
              Your project is now available on GitHub:
              <el-button type="success" @click="openGitHubProject" style="margin-left:20px;">Open project page <i class="fa fa-external-link" aria-hidden="true"></i></el-button>
            </span>
            <span v-show="zipDownloadLink" class="space-top">
              Your project has been successfully generated.
              <el-button type="success" @click="downloadZippedProject" class="el-icon-download" style="margin-left:20px;"><span style="font-family: Arial;">&nbsp;Download project</span></el-button>
            </span>
            <el-button type="primary" @click="goToStart" style="margin-left: 80px;">Back to start page</el-button>
          </div>
        </el-header>
        <el-main>
          <div v-show="instructions" v-html="md2html(instructions, '')"></div>
        </el-main>
      </el-container>
    </div>
  </div>
</template>

<script>
import {flatMap, toArray} from 'lodash';
import axios from 'axios';
import commonmark from 'commonmark';

require('text-io/textterm.css');
require('../boothub.css');

import createTextTerm from 'text-io';

import { mapState, mapMutations } from 'vuex';

var mdReader = new commonmark.Parser();
var mdWriter = new commonmark.HtmlRenderer({safe: true});

export default {
  name: 'home',
  data () {
    return {
      skeletons: [],
      textTerm: null,
      page: 'init',
      signInDialogVisible: false,
      busyLogin: false,
      generationErrorMessage: null,
      gitHubRepoLink: null,
      zipDownloadLink: null,
      instructions: null,
      waitingForAbort: false,
    }
  },
  computed: {
    ...mapState(['loggedInUserId', 'selectedSkeleton', 'skeletonUrl', 'exec']),
    skeletonName: function() {
      return this.selectedSkeleton ? this.selectedSkeleton.name : this.skeletonUrl ? this.skeletonUrl : 'Unknown';
    },
  },
  methods: {
    ...mapMutations (['setSelectedSkeleton', 'setSkeletonUrl', 'setExec', ]),
    setSelectedRow(row) {
      console.log('setSelectedRow(' + (row ? row.url : null) + ')');
      this.$refs.skeletonTable.setCurrentRow(row);
      this.setSelectedSkeleton(row);
      if(row) {
        this.setSkeletonUrl(row.url);
      }
    },
    selectSkeletonUrl(url) {
      console.log('Selecting url ' + url + '...');
      this.setSkeletonUrl(url);
      let selectedRow = null;
      if(url) {
        for(var row in this.skeletons) {
          console.log('Checking ' + JSON.stringify(this.skeletons[row]));
          if(url === this.skeletons[row].url) {
            console.log('Selecting row: ' + row);
            selectedRow = this.skeletons[row];
            break;
          }
        }
        this.setSelectedRow(selectedRow);
      }
    },
    md2html(md, defaultHtml) {
      console.log('Transforming to HTML. Markdown: ' + md);
      if(md) {
        var parsed = mdReader.parse(md);
        return mdWriter.render(parsed);
      } else {
        return defaultHtml;
      }
    },
    getSkeletons() {
      console.log('Loading skeletons...');
      axios.post('/api/querySkeletons', {lastVersionOnly: true})
          .then(response => {
              this.handleResult(response.data)
              this.skeletons = _.flatMap(_.toArray(response.data.value), item => _.toArray(item.entries));
              console.log('Successfully loaded ' + this.skeletons.length + ' skeletons.');
              // this.skeletons = _.shuffle(_.flatMap(this.skeletons, item => [item, item, item, item, item, item, item, item, item, item]));
            }
          )
          .then(response => this.selectSkeletonUrl(this.skeletonUrl))
          .catch(error => {console.log('Error loading skeletons: ' + error); this.skeletons = [];});
    },

    handleResult(result) {
      if(result.message) {
        let msgType = (result.type == 'ERROR') ? 'error' : (result.type == 'WARNING') ? 'warning' : 'info'
        this.$message({message: result.message, type: msgType});
        console.log(result.type + ': ' + result.message);
        if(result.type != 'SUCCESS') throw result.type + ': ' + result.message;
      }
    },

    login() {
      this.busyLogin = true;
      window.location.href = '/auth/login/home?exec=true&skeletonUrl=' + encodeURIComponent(this.skeletonUrl);
    },

    initTextTerm() {
      var textTermElem = this.$refs.textterm;
      var tt = createTextTerm(textTermElem);
      tt.setLogLevelTrace();
      tt.onDispose = this.onTextTermDispose;
      tt.onAbort = this.onTextTermAbort;
      return tt;
    },


    onTextTermDispose (resultData) {
      this.textTerm.terminate();
      this.page = 'result';
      console.log('RESULT_DATA: ' + resultData);
      var res = JSON.parse(resultData);
      this.generationErrorMessage = res.errorMessage;
      this.gitHubRepoLink = res.gitHubRepoLink
      this.instructions = res.instructions;
      if (res.outputPath) {
        var zipLoc = 'zip/' + res.outputPath;
        if (res.ghProjectId) {
          zipLoc += '?ghProjectId=' + res.ghProjectId
        }
        this.zipDownloadLink = zipLoc;
      }
    },

    onTextTermAbort () {
      this.textTerm.terminate();
      this.waitingForAbort = false;
      this.generationErrorMessage = 'Project generation aborted.';
      this.page = 'result';
    },


    getInitData(checkLoggedIn) {
      if(!this.skeletonUrl) return null;
      if(checkLoggedIn && !this.loggedInUserId) {
        this.signInDialogVisible = true;
        return null;
      }
      var initData = {
        url: this.skeletonUrl,
        expectedSize: -1,
        expectedSha: '',
        id: null,
        version: null,
        ghUserId: this.loggedInUserId
      }
      if(this.selectedSkeleton) {
        initData.id = this.selectedSkeleton.id;
        initData.version = this.selectedSkeleton.version;
        initData.expectedSize = this.selectedSkeleton.size;
        initData.expectedSha = this.selectedSkeleton.sha;
      }
      return initData;
    },

    discardTextTerm() {
      console.log('ENTER discardTextTerm()');
      if(this.textTerm) {
        console.log('resetting old textTerm');
        this.textTerm.resetTextTerm();
        this.textTerm.terminate();
      }
      this.textTerm = null;
      this.generationErrorMessage = null;
      this.gitHubRepoLink = null;
      this.zipDownloadLink = null;
      this.instructions = null;
      console.log('EXIT discardTextTerm()');
    },

    generate(checkLoggedIn) {
      console.log('ENTER generate(' + checkLoggedIn + ')');
      var initData = this.getInitData(checkLoggedIn);
      if(initData) {
        console.log('Trying to generate with initData: ' + JSON.stringify(initData));

        this.discardTextTerm();
        this.textTerm = this.initTextTerm();
        this.page = 'textTerm';
        console.log('textTerm = ' + (this.textTerm ? this.textTerm.uuid : 'null'));
        this.textTerm.displayMessage('Connecting to server. Please wait...');
        this.textTerm.execute(initData);
      }
      console.log('EXIT generate(' + checkLoggedIn + ')');
    },

    abortGeneration() {
      console.log('ENTER abortGeneration()');
      if(this.textTerm) {
        this.waitingForAbort = true;
        this.textTerm.sendUserInterrupt();
        setTimeout(() => {if(this.waitingForAbort) this.onTextTermAbort();}, 5000);
      } else {
        this.onTextTermAbort();
      }
      console.log('EXIT abortGeneration()');
    },

    openGitHubProject() {
      window.open(this.gitHubRepoLink,'_blank');
    },

    downloadZippedProject() {
      window.location.href = this.zipDownloadLink;
    },

    goToStart() {
      this.page = 'init';
      this.discardTextTerm();
    }
  },
  created() {
    if(this.skeletons.length === 0) {
      this.getSkeletons();
    }
  },
  watch: {
    '$route' (to, from) {
      console.log('Route changed to ' + this.$route.name + ': exec = ' + this.$route.params.exec + ', skeletonUrl = ' + this.$route.params.skeletonUrl + ', params = ' + JSON.stringify(this.$route.params));
      if(this.$route.name === 'home') {
        if(this.$route.params.exec) {
          this.setExec(this.$route.params.exec == 'true');
          if(this.$route.params.skeletonUrl) {
            this.selectSkeletonUrl(this.$route.params.skeletonUrl);
          }
          console.log('BEFORE REPLACE: exec = ' + this.exec + ', skeletonUrl = ' + this.skeletonUrl + ', selectedSkeleton = ' + this.selectedSkeleton);
          this.$router.replace('/home');
        } else {
          if(this.exec) {
            this.setExec(false);
            console.log('BEFORE GENERATE: exec = ' + this.exec + ', skeletonUrl = ' + this.skeletonUrl + ', selectedSkeleton = ' + this.selectedSkeleton);
            this.generate(true);
          } else {
            console.log('ROUTED TO HOME: exec = ' + this.exec + ', skeletonUrl = ' + this.skeletonUrl + ', selectedSkeleton = ' + this.selectedSkeleton);
          }
        }
      }
    },
  },
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>

.text {
  font-size: 14px;
}

.item {
  padding: 18px 0;
}

.clearfix:before,
.clearfix:after {
    display: table;
    content: "";
}
.clearfix:after {
    clear: both
}

.infobox {
	overflow: auto;
}

.info-name {
  float: left;
  font-size: 24px;
  font-weight: bold;
}

.info-homepage {
  float: right;
}

.info-version {
  float: left;
  font-weight: bold;
}

.dialog-table {
  border-spacing: 10px 10px;
}

.dialog-card {
  width: 320px;
  border: 1px solid lightgray;
  text-align: left;
}

.dialog-fill {
  width: 20px;
}

.dialog-top {
  height: 180px;
  text-align: left;
  vertical-align: top;
}

.dialog-bottom {
  height: 40px;
  text-align: center;
  vertical-align: bottom;
}

.space-top {
  margin-top: 40px;
}

.msg-danger {
  color: white;
  background: #FA5555;
}

>>> code {
  position: relative;
  left: 40px;
  background-color: #EEEEEE;
  font-family:Consolas,Monaco,Lucida Console,Liberation Mono,DejaVu Sans Mono,Bitstream Vera Sans Mono,Courier New;
}

</style>
