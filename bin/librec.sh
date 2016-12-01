#!/bin/bash

LIBREC_MAIN=net.librec.tool.driver.DataDriver
temp=`pwd`
LIBREC_HOME=${temp%/*}
LIB=${LIBREC_HOME}/lib

for f in ${LIB}/*.jar
do
	LIB_CLASSPATH=${LIB_CLASSPATH}:$f
done

LIBREC_ARG=""

while [ $# -gt 0 ]
do
    case $1 in
    data )
    	LIBREC_MAIN=net.librec.tool.driver.DataDriver
    	shift
    	;;
    rec )
    	LIBREC_MAIN=net.librec.tool.driver.RecDriver
    	shift
    	;;
    -exec )
		LIBREC_ARG=${LIBREC_ARG}" "$1
		shift
    	;;
    -load | -save)
    	shift
        LIBREC_ARG=${LIBREC_ARG}" "$1
        shift
        ;;
    -D | -jobconf)
    	LIBREC_ARG=${LIBREC_ARG}" "$1
        shift
        LIBREC_ARG=${LIBREC_ARG}" "$1
        shift
        ;;
    -conf)
    	CONF_PATH=$1
    	shift
    	CONF_PATH=${CONF_PATH}" "$1
    	shift
    	;;
    -libjars)
    	shift
    	CLASSPATH=${CLASSPATH}":"${1//,/:}
    	shift
    	;;
    esac
done
export CLASSPATH=${CLASSPATH}:${LIB_CLASSPATH}:${LIBREC_HOME}/conf:${LIBREC_HOME}/bin:${LIBREC_HOME}/lib
echo Classpath" "$CLASSPATH

JDK_VERSION=`$JAVA_HOME/bin/java -version 2>&1 | awk 'NR==1{gsub(/"/,""); print $3}'`
if [ "$JDK_VERSION" \> 1.7 ] ; then
	java -cp $CLASSPATH $LIBREC_MAIN ${LIBREC_ARG} ${CONF_PATH}
else
	echo "Please update your JDK version to 1.7 or higher"
fi
