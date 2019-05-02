#!/bin/sh

. setenv.sh

# kotlin -cp .:$CLASSPATH ClientKt
java -cp .:$CLASSPATH ClientKt
