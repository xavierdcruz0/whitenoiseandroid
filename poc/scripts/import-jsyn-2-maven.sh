#!/bin/bash

JSYN=$1

if [[ ! -e "$JSYN" ]] ; then
	echo "Usage: import-jsyn-2-maven.sh <path to jsyn.jar>"
	exit 1
fi

mvn install:install-file -Dfile=$JSYN -DgroupId=com.jsyn -DartifactId=jsyn -Dversion=1.0 -Dpackaging=jar
