#!/bin/bash

# Першы парамэтар - email на які дасылаць памылкі
# Астатнія - скрыпты што трэба выконваць
# Ліст дасылаецца толькі калі нейкі скрыпт не спрацаваў


cd `dirname $0`

JAVA_HOME=/opt/java
PATH=$JAVA_HOME/bin:$PATH

EMAIL=$1
shift
LOG=/tmp/osm-`date +%Y%m%d_%H%M%S`.log

while [ "$1" != "" ]; do
  $1 1>>$LOG 2>&1
  if [ $? -ne 0 ]; then
   cat $LOG | tr '~' ' ' | mail $EMAIL -s RunRobot
   rm $LOG
   exit
  fi
shift
done

rm $LOG
