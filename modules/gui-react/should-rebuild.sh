#!/usr/bin/env bash
cd $1
rebuild=false
if [ -f ./docker/binary/sepal-gui.jar ]; then
    found=$(find . \
        -not \( -path ./target -prune \) \
        -not \( -path ./frontend/dist -prune \) \
        -newer ./docker/binary/sepal-gui.jar \
        -print -quit)
    if [ ! -z "$found" ]; then
        rebuild=true
     fi
else
    rebuild=true
fi
echo ${rebuild}