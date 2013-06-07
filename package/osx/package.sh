#!/bin/sh

pkgdir=`dirname $0`
pkgdir=`dirname pkgdir`
echo Moving to package folder $pkgdir
cd $pkgdir
version=`cat version`
vdash=`echo $version | sed -e s/'\.'/-/g`

cp ../dist/lib/*.jar ../lib/

function package_build {
	os=$1
	arch=$2
	appdir=umongo-${os}-${arch}_${vdash}
	echo Creating folder $appdir
	rm -rf $appdir ${appdir}.zip
	mkdir $appdir

	cp -r ${os}/umongo.app $appdir
	app=${appdir}/umongo.app
        sed -i "" -e "N; s/\(.*CFBundleShortVersionString.*\n\).*/\1        <string>$version<\/string>/" $app/Contents/Info.plist

    mkdir -p $app/Contents/Resources/Java
	cp ../dist/umongo.jar $app/Contents/Resources/Java/
	cp ../lib/*.jar $app/Contents/Resources/Java/
        cp ./common-files/* $app/Contents/Resources/Java/
    cp ../README $appdir/

	zip -r ${appdir}.zip $appdir
}

package_build osx all

