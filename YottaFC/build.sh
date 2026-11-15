#!/bin/bash
# Builds a debug-signed APK without Gradle (needs: aapt, dx, zipalign, apksigner, javac, android.jar API 23+)
set -e
cd "$(dirname "$0")"
ANDROID_JAR=${ANDROID_JAR:-/usr/lib/android-sdk/platforms/android-23/android.jar}
DX=${DX:-/usr/lib/android-sdk/build-tools/debian/dx}
OUT=build
rm -rf $OUT && mkdir -p $OUT/gen $OUT/classes
SRC=app/src/main
aapt package -f -m -J $OUT/gen -M $SRC/AndroidManifest.xml -S $SRC/res -I $ANDROID_JAR -F $OUT/unsigned.apk
javac -encoding UTF-8 -nowarn -source 8 -target 8 -bootclasspath $ANDROID_JAR -d $OUT/classes $(find $SRC/java $OUT/gen -name '*.java') 2>&1 | grep -v -E "warning|^Note|Picked up" || true
$DX --dex --output=$OUT/classes.dex $OUT/classes
(cd $OUT && aapt add unsigned.apk classes.dex >/dev/null)
zipalign -f 4 $OUT/unsigned.apk $OUT/aligned.apk
[ -f tools/debug.keystore ] || keytool -genkeypair -keystore tools/debug.keystore -storepass android -keypass android -alias yotta -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Yotta FC" 2>/dev/null
apksigner sign --ks tools/debug.keystore --ks-pass pass:android --key-pass pass:android --out $OUT/YottaFC26.apk $OUT/aligned.apk
apksigner verify $OUT/YottaFC26.apk && echo "APK: $OUT/YottaFC26.apk ($(du -h $OUT/YottaFC26.apk | cut -f1))"
