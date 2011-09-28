#!/bin/sh

MAC_BUNDLE_TEMPLATE="build-resources/Mac OS Bundle"
DEST_DIR="build"
APPLICATION_NAME="GenomeMuseum.app"
APPLICATION_DEST="$DEST_DIR/$APPLICATION_NAME"
GENOMEMUSEUM_JAR_SOURCE="target/scala-2.9.1/"
GENOMEMUSEUM_JAR_DEST="$APPLICATION_DEST/Contents/Resources/Java/genomemuseum.jar"

if [ ! -n "$PROJECT_DIR" ]; then
  PROJECT_BIN_DIR=`dirname $0`
  PROJECT_DIR=`dirname $PROJECT_BIN_DIR`
fi

cd $PROJECT_DIR

if [ ! -d "$DEST_DIR" ]; then
  mkdir $DEST_DIR
fi

if [ -e "$APPLICATION_DEST" ]; then
  rm -rf $APPLICATION_DEST
fi

cp -R "$MAC_BUNDLE_TEMPLATE" "$APPLICATION_DEST"

find target -name genomemuseum*.min.jar |
  while read jarfile
  do
    cp "$jarfile" "$GENOMEMUSEUM_JAR_DEST"
    break
  done
