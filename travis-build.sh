#!/bin/bash
set -ev
if [ "${DOC_ONLY}" == "true" ]; then
  ./gradlew --no-daemon -i -s groovydoc asciidoc -x processResources
else
  ./gradlew --no-daemon -i -s build groovydoc asciidoc
fi

echo TRAVIS_PULL_REQUEST = $TRAVIS_PULL_REQUEST
echo TRAVIS_BRANCH = $TRAVIS_BRANCH
if [ "${TRAVIS_PULL_REQUEST}" == "false" -a "${TRAVIS_BRANCH}" == "master" ]; then
  echo Start publishing docs
  if [ "`git ls-remote origin gh-pages`" == "" ]; then
    echo Start gitPublishPush with ghPageType=init
    ./gradlew --no-daemon -i -s gitPublishPush --rerun-tasks -PghPageType=init
    echo Finished gitPublishPush with ghPageType=init
  fi
  echo Start gitPublishPush with ghPageType=latest
  ./gradlew --no-daemon -i -s gitPublishPush --rerun-tasks -PghPageType=latest
  echo Finished gitPublishPush with ghPageType=version

  echo Start gitPublishPush with ghPageType=version
  ./gradlew --no-daemon -i -s gitPublishPush --rerun-tasks -PghPageType=version
  echo Finished gitPublishPush with ghPageType=version

  echo Start updating releases.md
  ./gradlew --no-daemon -i -s update-release-list gitPublishPush --rerun-tasks -PghPageType=list
  echo Finished updating releases.md
fi

