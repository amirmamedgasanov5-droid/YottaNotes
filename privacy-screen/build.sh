#!/usr/bin/env bash
# Сборка APK без Gradle: aapt2 + javac + d8 + apksigner.
set -euo pipefail
cd "$(dirname "$0")"
SDK=${ANDROID_HOME:-/opt/android}
BT=$SDK/build-tools/34.0.0
JAR=$SDK/platforms/android-34/android.jar
OUT=build; rm -rf $OUT; mkdir -p $OUT/classes

$BT/aapt2 link -o $OUT/base.apk -I $JAR --manifest AndroidManifest.xml \
  --min-sdk-version 26 --target-sdk-version 34 --version-code 1 --version-name 1.0
javac -nowarn --release 8 -g:none -cp $JAR -d $OUT/classes $(find src -name '*.java') 2>&1 | grep -v '^Note\|warning' || true
$BT/d8 --release --min-api 26 --lib $JAR --output $OUT $(find $OUT/classes -name '*.class')
(cd $OUT && zip -q -j base.apk classes.dex)
$BT/zipalign -f 4 $OUT/base.apk $OUT/aligned.apk

KS=debug.keystore
[ -f $KS ] || keytool -genkeypair -keystore $KS -storepass android -keypass android \
  -alias key -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=PrivacyScreen" 2>/dev/null
$BT/apksigner sign --ks $KS --ks-pass pass:android --key-pass pass:android \
  --out PrivacyScreen.apk $OUT/aligned.apk
$BT/apksigner verify PrivacyScreen.apk && echo OK && ls -l PrivacyScreen.apk
