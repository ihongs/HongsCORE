#!/bin/bash

PACK_NAME=Hongs-CORE-0.3-SNAPSHOT
WORK_PATH=`dirname $0`
PACK_PATH="$WORK_PATH/$PACK_NAME"
BASE_PATH="$WORK_PATH/hongs-web/target/$PACK_NAME"
CORE_PATH="$BASE_PATH/WEB-INF"

rm -rf    "$PACK_PATH"
mkdir     "$PACK_PATH"

cp -rf    "$BASE_PATH" "$PACK_PATH/web"
mv        "$PACK_PATH/web/WEB-INF/"* "$PACK_PATH/"
rm -rf    "$PACK_PATH/web/WEB-INF/"
rm -rf    "$PACK_PATH/web/META-INF"
mv        "$PACK_PATH/web.xml" "$PACK_PATH/etc/"
mv        "$PACK_PATH/web.tld" "$PACK_PATH/etc/"
mv        "$PACK_PATH/classes" "$PACK_PATH/lib/"

rm -rf    "$PACK_PATH/var"
mkdir     "$PACK_PATH/var"
mkdir     "$PACK_PATH/var/log"
mkdir     "$PACK_PATH/var/tmp"

cp -f     "$WORK_PATH/hongs-serv-server/"*.bat "$PACK_PATH/"
cp -f     "$WORK_PATH/hongs-serv-system/"*.bat "$PACK_PATH/"
cp -f     "$PACK_PATH/lib/classes/log4j2.xml" "$PACK_PATH/etc/"

chmod +x  "$PACK_PATH/bin/"*

tar cfz   "$PACK_PATH.tar.gz" "$PACK_PATH"
