#!/bin/sh
if [[ -L $0 ]]; then
	dir="$(dirname "$(readlink -f "$0")")"
else
	dir=`dirname $0`
fi
cd $dir

if [ -z $minHeap ]; then minHeap=64; fi
if [ -z $maxHeap ]; then maxHeap=512; fi

java -classpath './lib/*.jar' -Xms${minHeap}M -Xmx${maxHeap}M -jar umongo.jar

