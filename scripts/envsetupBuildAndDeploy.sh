#!/bin/bash

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