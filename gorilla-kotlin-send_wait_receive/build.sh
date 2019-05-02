#!/bin/sh

. setenv.sh

kotlinc -cp $CLASSPATH *.kt
