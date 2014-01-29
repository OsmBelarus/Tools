#!/bin/sh

/opt/java/bin/xjc -no-header -d src -p gen.alex73.osm.xmldatatypes src/osm.xsd
/opt/java/bin/xjc -no-header -d src -p gen.renames src/renames.xsd
