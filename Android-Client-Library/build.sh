#!/bin/bash

# build amp project with gradle
#gradle build

PACKAGE=/com/localytics/android/

OS=$(uname -s)
DIR=${PWD}
SRC_PATH=/src/main/java/com/localytics/android
AMP_SRC_DIR=$DIR/android-client-library-amp$SRC_PATH
ANALYTICS_SRC_DIR=$DIR/android-client-library$SRC_PATH

AMP_LIB=$DIR/android-client-library-amp/build/bundles/release/classes.jar
ANALYTICS_LIB=$DIR/android-client-library/build/bundles/release/classes.jar

ANALYTICS_SRC=(
	LICENSE \
	Constants.java \
	DatapointHelper.java \
	JsonObjects.java \
	LocalyticsProvider.java \
	LocalyticsSession.java \
	PushReceiver.java \
	ReferralReceiver.java \
	ReflectionUtils.java \
	SessionHandler.java \
	UploadHandler.java )

# ANALYTICS_SRC=(${ANALYTICS_SRC[@]/#/"$PACKAGE"})

# copy analytics code from amp src directory to analytics src directory
echo "Copy analytics code to analytics directory."
rm $ANALYTICS_SRC_DIR/*
cd $AMP_SRC_DIR
cp ${ANALYTICS_SRC[*]} $ANALYTICS_SRC_DIR

# create the new directory for the generating jar/zip
mkdir -p $DIR/build

# modify the library version by modify 'androida' to 'android'
echo "Correct library version for analytics ..."
cd $ANALYTICS_SRC_DIR
if [ "$OS" != 'Darwin' ]; then
	find . -name "Constants.java" -type f -exec sed -i "s/androida/android/g" '{}' \;
else
	find . -name "Constants.java" -type f -exec sed -i "" "s/androida/android/g" '{}' \;
fi

# compress the analytics src code
echo "Compress analytics code into the zip file ..."
cd $DIR/android-client-library/src/main/java/
tar -zcvf $DIR/build/Localytics-Android-latest.src.zip com

# build project with gradle
echo "Building Localytics project ..."
cd $DIR
gradle build --stacktrace

# rename the generated jar
echo "Moves jar to targeting directory: "$DIR/build/
mv $AMP_LIB $DIR/build/android-client-library-amp.jar
mv $ANALYTICS_LIB $DIR/build/android-client-library.jar

# package the zip for localytics IAM library
cd $DIR/build/
mkdir localytics-in-app
mv android-client-library-amp.jar localytics-in-app
mv $ANALYTICS_SRC_DIR/LICENSE localytics-in-app
tar -zcvf $DIR/build/localytics-in-app.zip localytics-in-app
rm -rf localytics-in-app

#cp $DIR/build/android-client-library-amp.jar ../rocketrush/libs
