<template>
  <div class="cli">
    <h3 class="space-top">The Command Line Interface <el-button type="success" @click="downloadCLI" style="margin-left:20px;">Download CLI</el-button></h3>
    <div class="space-top">
      <p/>With the BootHub CLI you can generate projects directly in the terminal.
      <br>Usually, the CLI runs faster than the web-based interface, which may incur delays when the server is under heavy load.

      <p/>Download the CLI archive and extract it to a location of your choice, then <code>cd</code> into the bin subdirectory.
      <br>Start the CLI by typing <code>./boothub</code> (on Linux or Mac) or <code>boothub</code> (on Windows).

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
      window.location.href = 'https://github.com/boothub-org/boothub/releases/download/v0.3.4/boothub-0.3.4.zip';
    },
  },
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>

.space-top {
  margin-top: 40px;
}

code {
  background-color:#EEEEEE;
  font-family:Consolas,Monaco,Lucida Console,Liberation Mono,DejaVu Sans Mono,Bitstream Vera Sans Mono,Courier New;
}

</style>
