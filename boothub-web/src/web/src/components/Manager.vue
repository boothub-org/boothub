<template>
  <div class="manager">
    <h2>Skeleton Manager</h2>
    <el-alert v-if="!loggedInUserId"
              type="info"
              :closable="false"
              title="Sign in required"
              description="Please sign in with GitHub in order to manage your project templates."
              show-icon>
    </el-alert>
    <div v-if="loggedInUserId && skeletonRowsLoaded && skeletonRows.length === 0">
      <el-alert v-if="!skeletonRowsError"
                type="info"
                :closable="false"
                title="You currently don't own any templates."
                show-icon>
      </el-alert>
      <el-alert v-if="skeletonRowsError"
                type="error"
                :closable="false"
                title="Cannot load templates. Please try again later."
                show-icon>
      </el-alert>
    </div>
    <el-row :gutter="8">
      <el-col :span="14">
        <el-table
          v-if="loggedInUserId && (skeletonRows.length > 0 || !skeletonRowsLoaded)"
          ref="skeletonTable"
          :data="skeletonRows"
          v-loading="!skeletonRowsLoaded"
          element-loading-text="Loading project templates..."
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
          <el-table-column
            style="word-wrap: normal;"
            label="About"
            prop="caption">
          </el-table-column>
        </el-table>
        <el-row :gutter="8" v-if="loggedInUserId" style="margin-top:60px;">
          <el-col :span="18">
            <el-input placeholder="Enter the URL of a new template (accepted protocols: http and https)" v-model="newSkeletonUrl"></el-input>
          </el-col>
          <el-col :span="6">
            <el-button type="primary" :disabled="isAddSkeletonDisabled()" v-loading="busyAddSkeleton" @click="addSkeleton">Add template</el-button>
          </el-col>
        </el-row>
      </el-col>
      <el-col :span="10">
        <el-card v-if="selectedRow" class="infobox">
          <div slot="header" class="clearfix">
            <span class="info-name">{{selectedRow.name}}</span>
          </div>
          <div>
            <el-tabs type="border-card">
              <el-tab-pane label="Versions">
                <el-row :gutter="8">
                  <el-col :span="18">
                    <el-table
                      v-if="selectedRow.entries"
                      ref="versionTable"
                      :data="versionRows"
                      highlight-current-row
                      @current-change="setSelectedVersion"
                      empty-text="No versions found."
                      height="180"
                      style="width: 100%">
                      <el-table-column
                        min-width="160"
                        label="Version"
                        prop="version">
                      </el-table-column>
                      <el-table-column
                        label="Valid"
                        align="center"
                        width="80">
                        <template slot-scope="scope">
                          <el-popover trigger="hover" placement="top-end" width="600" v-if="scope.row.validationError">
                            <p class="danger-color">{{ scope.row.validationError }}</p>
                            <div slot="reference" class="el-icon-error danger-color"/>
                          </el-popover>
                          <i class="el-icon-success success-color" v-if="!scope.row.validationError"/>
                        </template>
                      </el-table-column>
                    </el-table>
                  </el-col>
                  <el-col :span="6">
                    <el-button type="danger" :disabled="selectedVersion === null" v-loading="busyDeleteSkeleton"
                               @click="deleteVersion" style="margin-top:60px;">Delete version</el-button>
                  </el-col>
                </el-row>
              </el-tab-pane>
              <el-tab-pane label="Tags">
                <el-row :gutter="8">
                  <el-col :span="18">
                    <el-alert v-if="selectedRow.entries && tagRowsLoaded && tagRowsError"
                              type="error"
                              :closable="false"
                              title="Cannot load tags. Please try again later."
                              show-icon>
                    </el-alert>
                    <el-table
                      ref="tagTable"
                      :data="tagRows"
                      v-loading="selectedRow.entries && !tagRowsLoaded"
                      highlight-current-row
                      @current-change="setSelectedTag"
                      empty-text="No tags found."
                      height="180"
                      style="width: 100%">
                      <el-table-column
                        label="Tags"
                        min-width="160"
                        prop="tag">
                      </el-table-column>
                    </el-table>
                  </el-col>
                  <el-col :span="6">
                    <el-button type="danger" :disabled="selectedTag=== null"
                               v-loading="busyDeleteTag" @click="deleteTag" style="margin-top:60px;">Delete tag</el-button>
                  </el-col>
                </el-row>
                <el-row :gutter="8" style="margin-top:60px;">
                  <el-col :span="18">
                    <el-input placeholder="Enter a new tag" v-model="newTag"></el-input>
                  </el-col>
                  <el-col :span="6">
                    <el-button type="primary" :disabled="isAddTagDisabled()" v-loading="busyAddTag" @click="addTag">Add tag</el-button>
                  </el-col>
                </el-row>
              </el-tab-pane>
              <el-tab-pane label="Owners">
                <el-row :gutter="8">
                  <el-col :span="18">
                    <el-alert v-if="selectedRow.entries && ownerRowsLoaded && ownerRowsError"
                              type="error"
                              :closable="false"
                              title="Cannot load owners. Please try again later."
                              show-icon>
                    </el-alert>
                    <el-table
                      v-if="selectedRow.entries && !ownerRowsError"
                      ref="ownerTable"
                      :data="ownerRows"
                      v-loading="!ownerRowsLoaded"
                      highlight-current-row
                      @current-change="setSelectedOwner"
                      empty-text="No owners found."
                      height="180"
                      style="width: 100%">
                      <el-table-column
                        min-width="160"
                        label="Owner"
                        prop="ownerText">
                      </el-table-column>
                    </el-table>
                  </el-col>
                  <el-col :span="6">
                    <el-button type="danger" :disabled="selectedOwner === null || selectedOwner === loggedInUserId"
                               v-loading="busyDeleteOwner" @click="deleteOwner" style="margin-top:60px;">Delete owner</el-button>
                  </el-col>
                </el-row>
                <el-row :gutter="8" style="margin-top:60px;">
                  <el-col :span="18">
                    <el-input placeholder="Enter the GitHub user of a new owner" v-model="newOwner"></el-input>
                  </el-col>
                  <el-col :span="6">
                    <el-button type="primary" :disabled="isAddOwnerDisabled()"
                               v-loading="busyAddOwner" @click="addOwner">Add owner</el-button>
                  </el-col>
                </el-row>
              </el-tab-pane>
            </el-tabs>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
  import {flatMap, includes, keys, mapKeys, merge, toArray, values} from 'lodash';
  import axios from 'axios';
  import { mapState } from 'vuex';
  import validUrl from 'valid-url';

  export default {
    name: 'manager',
    data () {
      return {
        skeletonRowsError: false,
        skeletonRowsLoaded: false,
        skeletonRows: [],
        selectedRow: null,
        newSkeletonUrl: null,
        busyAddSkeleton: false,
        busyDeleteSkeleton: false,

        versionRows: [],
        selectedVersion: null,

        tagRowsError: false,
        tagRowsLoaded: false,
        tagRows: [],
        selectedTag: null,
        newTag: null,
        busyAddTag: false,
        busyDeleteTag: false,

        ownerRowsError: false,
        ownerRowsLoaded: false,
        ownerRows: [],
        selectedOwner: null,
        newOwner: null,
        busyAddOwner: false,
        busyDeleteOwner: false,

        busyLogin: false,
      }
    },
    computed: {
      ...mapState(['loggedInUserId']),
    },
    methods: {
      setSelectedRow(row) {
        console.log('setSelectedRow(' + (row ? row.id : null) + ')');
        this.$refs.skeletonTable.setCurrentRow(row);
        this.selectedRow = row;
        this.versionRows = row ? _.values(row.entries) : [];
        this.selectedVersion = null;
        this.getTags();
        this.getOwners();
      },
      setSelectedVersion(versionRow) {
        this.$refs.versionTable.setCurrentRow(versionRow);
        this.selectedVersion = (versionRow ? versionRow.version : null);
        console.log('setSelectedVersion(' + this.selectedVersion + ')');
      },
      setSelectedTag(tagRow) {
        this.$refs.tagTable.setCurrentRow(tagRow);
        this.selectedTag = (tagRow ? tagRow.tag : null);
        console.log('setSelectedTag(' + this.selectedTag + ')');
      },
      setSelectedOwner(ownerRow) {
        this.$refs.ownerTable.setCurrentRow(ownerRow);
        this.selectedOwner = (ownerRow ? ownerRow.owner : null);
        console.log('setSelectedOwner(' + this.selectedOwner + ')');
      },
      getSkeletons() {
        console.log('Loading templates of user ' + this.loggedInUserId + '...');
        let prevSelectedSkeletonId = this.selectedRow ? this.selectedRow.id : null;
        console.log('prevSelectedSkeletonId: ' + prevSelectedSkeletonId);
        this.skeletonRowsLoaded = false;
        this.skeletonRowsError = false;
        this.skeletonRows = [];
        this.selectedRow = null;
        this.versionRows = [];
        this.selectedVersion = null;
        this.tagRows = [];
        this.selectedTag = null;
        this.newTag = null;
        this.ownerRows = [];
        this.selectedOwner = null;
        this.newOwner = null;
        axios.post('/api/querySkeletons', {ownerId: this.loggedInUserId, includeInvalidEntries: true})
          .then(response => {
            this.handleResult(response.data)
            this.skeletonRows = _.map(_.keys(response.data.value), key => _.merge({id: key}, response.data.value[key]));
            this.skeletonRowsLoaded = true;
            console.log('Found ' + this.skeletonRows.length + ' templates owned by user ' + this.loggedInUserId);
            if(prevSelectedSkeletonId) {
              this.setSelectedRow(this.getSkeletonRow(prevSelectedSkeletonId));
            }
          })
          .catch(error => {
            this.skeletonRows = [];
            this.skeletonRowsLoaded = true;
            this.skeletonRowsError = true;
            console.log('Error loading templates: ' + error);
          });
      },
      getSkeletonRow(skeletonId) {
        if(!this.skeletonRows) return null;
        return _.find(this.skeletonRows, function(row) { return row.id === skeletonId; });
      },
      getOwners() {
        if(this.selectedRow) {
          let skeletonId = this.selectedRow.id;
          console.log('Loading owners of template ' + skeletonId + '...');
          this.ownerRowsLoaded = false;
          this.ownerRowsError = false;
          this.ownerRows = [];
          this.selectedOwner = null;
          this.newOwner = null;
          axios.post('/api/queryOwners', {skeletonId: skeletonId})
            .then(response => {
              this.handleResult(response.data)
              let myself = this.loggedInUserId;
              this.ownerRows =  _.map(response.data.value, function (own) {
                return {owner: own, ownerText: own + ((own === myself) ? ' (you)' : '')}
              });
              this.ownerRowsLoaded = true;
              console.log('Found ' + this.ownerRows.length + ' owners of ' + skeletonId);
            })
            .catch(error => {
              this.ownerRows = [];
              this.ownerRowsLoaded = true;
              this.ownerRowsError = true;
              console.log('Error loading owners of ' + skeletonId + ': ' + error);
            });
        }
      },
      isAddOwnerDisabled() {
        if(!this.newOwner) return true;
        let currentOwners = _.map(this.ownerRows, function (ownerRow) {return ownerRow.owner});
        return _.includes(currentOwners, this.newOwner);
      },
      isAddTagDisabled() {
        if(!this.newTag) return true;
        let currentTags = _.map(this.tagRows, function (tagRow) {return tagRow.tag});
        return _.includes(currentTags, this.newTag);
      },
      isAddSkeletonDisabled() {
        if(!this.newSkeletonUrl) return true;
        return !validUrl.is_web_uri(this.newSkeletonUrl);
      },
      getTags() {
        if(this.selectedRow) {
          let skeletonId = this.selectedRow.id;
          console.log('Loading tags of template ' + skeletonId + '...');
          this.tagRowsLoaded = false;
          this.tagRowsError = false;
          this.tagRows = [];
          this.selectedTag= null;
          this.newTag = null;
          axios.post('/api/queryTags', {skeletonId: skeletonId})
            .then(response => {
              this.handleResult(response.data)
              this.tagRows = _.map(response.data.value, function (t) {return {tag: t}});
              this.tagRowsLoaded = true;
              console.log('Found ' + this.tagRows.length + ' tags of ' + skeletonId);
            })
            .catch(error => {
              this.tagRows = [];
              this.tagRowsLoaded = true;
              this.tagRowsError = true;
              console.log('Error loading tags of ' + skeletonId + ': ' + error);
            });
        }
      },
      deleteVersion() {
        console.log('DELETING ' + this.selectedRow.id +  ' version ' + this.selectedVersion);
        this.busyDeleteSkeleton = true;
        axios.post('/api/deleteSkeletonEntry', {skeletonId: this.selectedRow.id, version: this.selectedVersion})
          .then(response => {
            console.log('deleteSkeletonEntry returned: ' + JSON.stringify(response.data));
            this.handleResult(response.data)
          })
          .catch(error => {
            console.log('Error deleting ' + this.selectedRow.id +  ' version ' + this.selectedVersion + ': ' + error);
          })
          .then(() => {this.finalizeDeleteVersion()}, () => {this.finalizeDeleteVersion()});
      },
      finalizeDeleteVersion() {
        this.getSkeletons();
        this.busyDeleteSkeleton = false;
      },
      deleteTag() {
        console.log('DELETING tag ' + this.selectedTag + ' of ' + this.selectedRow.id);
        this.busyDeleteTag = true;
        axios.post('/api/deleteTag', {skeletonId: this.selectedRow.id, tag: this.selectedTag})
          .then(response => {
            console.log('deleteTag returned: ' + JSON.stringify(response.data));
            this.handleResult(response.data)
          })
          .catch(error => {
            console.log('Error deleting tag ' + this.selectedTag + ' of ' + this.selectedRow.id + ': ' + error);
          })
          .then(() => {this.finalizeDeleteTag()}, () => {this.finalizeDeleteTag()});
      },
      finalizeDeleteTag() {
        this.getTags();
        this.busyDeleteTag = false;
      },
      deleteOwner() {
        console.log('DELETING owner ' + this.selectedOwner + ' of ' + this.selectedRow.id +  ' version ' + this.selectedVersion);
        this.busyDeleteOwner = true;
        axios.post('/api/deleteOwner', {skeletonId: this.selectedRow.id, version: this.selectedVersion, ownerId: this.selectedOwner})
          .then(response => {
            console.log('deleteOwner returned: ' + JSON.stringify(response.data));
            this.handleResult(response.data)
          })
          .catch(error => {
            console.log('Error deleting owner ' + this.selectedOwner + ' of ' + this.selectedRow.id +  ' version ' + this.selectedVersion + ': ' + error);
          })
          .then(() => {this.finalizeDeleteOwner()}, () => {this.finalizeDeleteOwner()});
      },
      finalizeDeleteOwner() {
        this.getOwners();
        this.busyDeleteOwner = false;
      },
      addSkeleton() {
        this.busyAddSkeleton = true;
        axios.post('/api/addSkeleton', {url: this.newSkeletonUrl})
          .then(response => {
            console.log('addSkeleton returned: ' + JSON.stringify(response.data));
            this.handleResult(response.data)
          })
          .catch(error => {
            console.log('Error adding template' + this.newSkeletonUrl + ': ' + error);
          })
          .then(() => {this.finalizeAddSkeleton()}, () => {this.finalizeAddSkeleton()});
      },
      finalizeAddSkeleton() {
        this.getSkeletons();
        this.busyAddSkeleton = false;
      },
      addTag() {
        console.log('Adding tag ' + this.newTag + ' to ' + this.selectedRow.id);
        this.busyAddTag = true;
        return axios.post('/api/addTag', {skeletonId: this.selectedRow.id, tag: this.newTag})
          .then(response => {
            console.log('addTag returned: ' + JSON.stringify(response.data));
            this.handleResult(response.data);
            console.log('### CALLING getTags() !!!');
            this.getTags();
          })
          .catch(error => {
            console.log('Error adding tag ' + this.newTag + ' to ' + this.selectedRow.id + ': ' + error);
          })
          .then(() => {this.finalizeAddTag()}, () => {this.finalizeAddTag()});
      },
      finalizeAddTag() {
        this.getTags();
        this.busyAddTag = false;
      },
      addOwner() {
        console.log('Adding owner ' + this.newOwner + ' to ' + this.selectedRow.id);
        this.busyAddOwner = true;
        axios.post('/api/addOwner', {skeletonId: this.selectedRow.id, ownerId: this.newOwner})
          .then(response => {
            console.log('addOwner returned: ' + JSON.stringify(response.data));
            this.handleResult(response.data)
          })
          .catch(error => {
            console.log('Error adding owner ' + this.newOwner + ' to ' + this.selectedRow.id + ': ' + error);
          })
          .then(() => {this.finalizeAddOwner()}, () => {this.finalizeAddOwner()});
      },
      finalizeAddOwner() {
        this.getOwners();
        this.busyAddOwner = false;
      },
      login() {
        this.busyLogin = true;
        window.location.href = '/auth/login/home?exec=true&skeletonUrl=' + encodeURIComponent(this.skeletonUrl);
      },
      handleResult(result) {
        if(result.message) {
          let msgType = (result.type == 'ERROR') ? 'error' : (result.type == 'WARNING') ? 'warning' : 'info'
          this.$message({message: result.message, type: msgType});
          console.log(result.type + ': ' + result.message);
        }
        if(result.type != 'SUCCESS') throw result.type + ': ' + result.message;
      },
      handleResultWithSuccessMessage(result, successMessage) {
        this.handleResult(result);
        if(result.type == 'SUCCESS') {
          this.$message.info(successMessage);
        }
      },
    },
    created() {
      if(this.loggedInUserId && (this.skeletonRows.length === 0)) {
        this.getSkeletons();
      }
    },
    watch: {
      loggedInUserId: function (newLoggedInUserId) {
        console.log('User changed to: ' + this.loggedInUserId)
        this.getSkeletons();
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

  .danger-color {
    color: #FA5555;
  }
  .success-color {
    color: #67C23A;
  }

</style>
