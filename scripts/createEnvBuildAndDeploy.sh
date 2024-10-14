#!/bin/bash

if [ ! -f "$HOME/.aws/credentials" ]; then
    echo "run aws configure"
    exit
fi

# setup env
cd envSetup
./setupEnv.sh
cd ..

# build modules
cd build
./dockerImageBuildScript.sh
cd ..

# deploy
cd deploy
./dockerImageDeployScript.sh
cd ..
