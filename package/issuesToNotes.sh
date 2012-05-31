#!/bin/sh

grep "agirbal/umongo/issues/[0-9]\+" $1 | sed -e s/'.*<a href="\/agirbal\/umongo\/issues\/\(.*\)">\(.*\)<\/a>.*'/'* [issue\1](https:\/\/github.com\/agirbal\/umongo\/issues\/\1): \2'/
