#!/bin/bash

pushd ~/gits/OsmBelarus-Pieraklady/zychodniki/
DIRS="`echo *`"
popd

mkdir -p /var/www/osm/omegat/
for i in $DIRS; do
  sed s/TYPE/$i/g < omegat-project.template > /var/www/osm/omegat/$i.omegat.project
  sed s/TYPE/$i/g < omegat-jnlp.template > /var/www/osm/omegat/$i.jnlp
done
