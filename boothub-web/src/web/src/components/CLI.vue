<template>
  <div class="cli">
    <h2>CLI</h2>
    <div class="space-before">
      Download the CLI.
      <el-button type="success" @click="downloadCLI" style="margin-left:20px;">Download CLI</el-button>
    </div>
  </div>
</template>

<script>
require('../boothub.css');

export default {
  name: 'cli',
  methods: {
    downloadCLI() {
      // TODO - retrieve the version from gradle.properties
      window.location.href = 'https://github.com/boothub-org/boothub/releases/download/v0.1.0/boothub-0.1.0.zip';
    },
  },
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>

.space-before {
  margin-top: 40px;
}

</style>
