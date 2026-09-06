#!/bin/sh
# Gradle Wrapper launcher for Alfa_device_ctrl.
# Generated wrapper scripts delegate to gradle/wrapper/gradle-wrapper.jar.
APP_HOME="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)" || exit 1
JAVACMD="${JAVA_HOME:+$JAVA_HOME/bin/java}"
[ -x "$JAVACMD" ] || JAVACMD=java
exec "$JAVACMD" -Dorg.gradle.appname=gradlew -jar "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" "$@"
