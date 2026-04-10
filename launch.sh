#!/bin/bash

./pull.sh
./gradlew bootWar

projectDir="$PWD"

(cd '/srv/herobrine' && java -Xmx2G -jar "$projectDir/build/libs/Herobrine-0.war") || (echo 'could not CD and EXEC')
