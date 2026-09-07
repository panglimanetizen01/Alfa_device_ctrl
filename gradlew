#!/usr/bin/env sh

##############################################################################
##
##  Gradle start up script for UN*X
##
##############################################################################

# Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

APP_NAME="Gradle"
APP_BASE_NAME=${0##*/}

# Use the maximum available, or set MAX_FD to override the maximum file descriptor limit.
MAX_FD="maximum"

warn () {
    echo "$*"
} >&2

# OS specific support (must be 'true' or 'false').
cygwin=false
msys=false
darwin=false
nonstop=false
case "$( uname )" in
  CYGWIN* )
    cygwin=true
    ;;
  Darwin* )
    darwin=true
    ;;
  MSYS* | MINGW* )
    msys=true
    ;;
  NONSTOP* )
    nonstop=true
    ;;
esac

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

# Determine the Java command to use to start the JVM.
if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
        JAVACMD=$JAVA_HOME/jre/sh/java
    else
        JAVACMD=$JAVA_HOME/bin/java
    fi
    if [ ! -x "$JAVACMD" ] ; then
        die "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
    fi
else
    JAVACMD=java
    if ! command -v java >/dev/null 2>&1 ; then
        die "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
    fi
fi

# Increase the maximum file descriptors if we can.
if ! $cygwin && ! $darwin && ! $nonstop ; then
    case $MAX_FD in
      max*)
        MAX_FD=$( ulimit -H -n )
        ;;
    esac
    if ! [ "$MAX_FD" = 'unlimited' ] && ! [ "$MAX_FD" = 'maximum' ] ; then
        ulimit -n "$MAX_FD" || warn "Could not set maximum file descriptor limit: $MAX_FD"
    fi
fi

# For Darwin, add options to specify how the application appears in the dock
if $darwin; then
    GRADLE_OPTS="$GRADLE_OPTS \"-Xdock:name=$APP_NAME\" \"-Xdock:icon=$APP_HOME/media/gradle.icns\""
fi

# For Cygwin or MSYS, switch paths to Windows format before running Java
if $cygwin || $msys ; then
    APP_HOME=$( cygpath --path --mixed "$APP_HOME" )
    CLASSPATH=$( cygpath --path --mixed "$CLASSPATH" )
    JAVACMD=$( cygpath --unix "$JAVACMD" )
    # Now convert the arguments - kludge
    for arg do
        if
            case $arg in
              -D*) false ;;
              *)   true ;;
            esac
        then
            arg=$( cygpath --path --ignore --mixed "$arg" )
        fi
        # Roll the args list around exactly as many times as the number of args
        # specified by the user.
        shift
        set -- "$@" "$arg"
    done
fi

# Escape application args
save () {
    for arg do
        printf '%s\n' "$arg" | sed "s/'/'\\''/g;1s/^/'/;\$s/\$/' \\\\&/"
    done
    printf 'X\n'
}
APP_ARGS=$(save "$@")

# Collect all arguments for the java command.
set -- \
    "-Dorg.gradle.appname=$APP_BASE_NAME" \
    -classpath "$CLASSPATH" \
    org.gradle.wrapper.GradleWrapperMain \
    "$@"

# Use the maximum available, or set MAX_FD to override the maximum file descriptor limit.
exec "$JAVACMD" "$@"
