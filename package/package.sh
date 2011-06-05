#!/bin/sh

pkgdir=`dirname $0`
echo Moving to package folder $pkgdir
cd $pkgdir
version=`cat version`

cp ../dist/lib/SwingFast.jar ../lib/
cp ../dist/lib/{bson,mongo}.jar ../lib/

function package_build {
	os=$1
	arch=$2
	appdir=JMongoBrowser-${os}-${arch}_${version}
	echo Creating folder $appdir
	rm -rf $appdir ${appdir}.zip
	mkdir $appdir

	cp ${os}-files/* $appdir/
	cp ../dist/JMongoBrowser.jar $appdir/
	mkdir $appdir/lib
	cp ../lib/*.jar $appdir/lib/

	zip -r ${appdir}.zip $appdir
}

package_build windows all
package_build linux all
package_build mac all

