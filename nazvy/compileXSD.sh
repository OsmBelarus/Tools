#!/bin/sh

/opt/java8/bin/xjc -no-header -d src -p gen.alex73.osm.xmldatatypes src/osm.xsd
/opt/java8/bin/xjc -no-header -d src -p gen.alex73.osm.monitor src/monitor.xsd
/opt/java8/bin/xjc -no-header -d src -p gen.alex73.osm.validators.objects src/object_types.xsd
