#!/bin/bash

DIRNAME=`dirname "$0"`

if [ "$1" == "-h" -o "$1" == "--help" ]
then
	echo "Usage: $0 [host] [thrift port] [jmx port]"
	echo "Default: $0 localhost 9160 8080"
	exit 1
fi

java -jar $DIRNAME/cassandra-gui.one-jar.jar $@

