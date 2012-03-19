#!/bin/sh
dir=`dirname $0`
cd $dir

if [ -z $minHeap ]; then minHeap=64; fi
if [ -z $maxHeap ]; then maxHeap=512; fi

java -classpath './lib/*.jar' -Xms${minHeap}M -Xmx${maxHeap}M $maxHeapOpt -Dapple.laf.useScreenMenuBar=true -Xdock:name="JMongoBrowser" -Xdock:icon="mongo_leaf.png" -Djava.util.logging.config.file=./logging.properties -jar JMongoBrowser.jar

