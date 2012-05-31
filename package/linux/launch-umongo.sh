#!/bin/sh
dir=`dirname $0`
cd $dir

if [ -z $minHeap ]; then minHeap=64; fi
if [ -z $maxHeap ]; then maxHeap=512; fi

# sometimes runs better with opengl forced, but also has visual bugs: -Dsun.java2d.opengl=true
java -classpath './lib/*.jar' -Xms${minHeap}M -Xmx${maxHeap}M  -Djava.util.logging.config.file=./logging.properties -jar umongo.jar

