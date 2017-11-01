<template>
  <div class="generate">
    <h1>NOT IMPLEMENTED</h1>
    <p>&nbsp;</p>
  </div>
</template>

<script>

import { mapState, mapMutations } from 'vuex';

export default {
  name: 'generate',
  methods: {
    ...mapMutations (['setSelectedSkeleton', 'setSkeletonUrl', 'setExec', ]),
  },
  watch: {
    '$route' (to, from) {
      console.log('Route changed to: skeletonUrl = ' + this.$route.params.skeletonUrl + ', params = ' + JSON.stringify(this.$route.params));
      if(this.$route.params.skeletonUrl) {
        this.setSkeletonUrl(this.$route.params.skeletonUrl);
        this.setExec(true);
        console.log('BEFORE REPLACING GENERATE WITH HOME: skeletonUrl = ' + this.skeletonUrl + ', selectedSkeleton = ' + this.selectedSkeleton);
        this.$router.replace('/home');
      }
    }
  },
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>

.text {
  font-size: 14px;
}

</style>
