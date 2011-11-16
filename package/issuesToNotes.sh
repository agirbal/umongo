#!/bin/sh

grep "agirbal/JMongoBrowser/issues/[0-9]\+" $1 | sed -e s/'.*<a href="\/agirbal\/JMongoBrowser\/issues\/\(.*\)">\(.*\)<\/a>.*'/'* [issue\1](https:\/\/github.com\/agirbal\/JMongoBrowser\/issues\/\1): \2'/
