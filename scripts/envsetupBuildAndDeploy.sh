#!/bin/bash

# setup env
envSetup/setupEnv.sh

# build modules
build/dockerImageBuildScript.sh

# deploy
deploy/dockerImageDeployScript.sh