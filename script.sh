#!/bin/bash

MODULES_TRIGGER_ALL="common/ app-metadata/"
MODULES=$(find -name pom.xml | sed -e 's|pom.xml| |' | sed -e 's|./| |')
URL="https://api.github.com/repos/quarkus-qe/quarkus-openshift-test-suite/pulls/130/files"
FILES=$(curl -s -X GET -G $URL | jq -r '.[] | .filename'  | tr " " "\n")
CHANGED=""

for module in $MODULES
do
    if [[ $FILES =~ ([[:space:]]"$module") ]] ; then
        CHANGED=$(echo $CHANGED " " $module)
    fi
done

echo "Changes: $CHANGED"
TRIGGERED=false
for module in $CHANGED
do
    if [[ $MODULES_TRIGGER_ALL == *"$module"* ]] ; then
        TRIGGERED=true
        break;
    fi
done

echo "Triggered: $TRIGGERED"