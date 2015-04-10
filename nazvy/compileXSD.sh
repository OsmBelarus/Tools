#!/bin/sh

/opt/java8/bin/xjc -no-header -d src -p gen.alex73.osm.validators.objects src/object_types.xsd
/opt/java8/bin/xjc -no-header -d src -p gen.alex73.osm.validators.rehijony src/Rehijony.xsd
