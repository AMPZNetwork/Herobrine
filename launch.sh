#!/bin/bash

./pull.sh
./gradlew bootWar
java -Xmx2G -jar build/libs/Herobrine-0.war
