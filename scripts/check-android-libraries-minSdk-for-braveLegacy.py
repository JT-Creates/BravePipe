#!/usr/bin/python3

import requests
import zipfile
from io import BytesIO
import xml.etree.ElementTree as ET

# check android libraries (aar) for minSdk properties for braveLegacy flavor
# scroll down to see the syntax of inspect_min_sdk() and
# inspect_min_sdk_convert() to create substitute() calls

# License: GPL_v3
# Author: evermind
# Version: 1.0.0
#
# Changelog:
# - v1.0.0 (20260125):
#   * Initial version
#

REPOSITORIES = [
    "https://dl.google.com/dl/android/maven2",
    "https://repo1.maven.org/maven2",
    "https://repo.maven.apache.org/maven2",

]

AAR_METADATA_PATH = "META-INF/com/android/build/gradle/aar-metadata.properties"


def gav_to_path(group, artifact, version):
    return f"{group.replace('.', '/')}/{artifact}/{version}"


def download_from_repos(path, filename):
    for repo in REPOSITORIES:
        url = f"{repo}/{path}/{filename}"
        print(f"{url}")
        r = requests.get(url, timeout=15)
        if r.status_code == 200:
            print(f"✓ Downloaded from {repo}")
            return BytesIO(r.content)
    raise RuntimeError("Artifact not found in known repositories")


def download_aar(group, artifact, version):
    path = gav_to_path(group, artifact, version)
    return download_from_repos(path, f"{artifact}-{version}.aar")


def min_sdk_from_metadata(aar_bytes):
    with zipfile.ZipFile(aar_bytes) as z:
        if AAR_METADATA_PATH not in z.namelist():
            return None

        content = z.read(AAR_METADATA_PATH).decode("utf-8")
        for line in content.splitlines():
            if line.startswith("minSdkVersion="):
                return int(line.split("=", 1)[1])
    return None


def min_sdk_from_manifest(aar_bytes):
    with zipfile.ZipFile(aar_bytes) as z:
        if "AndroidManifest.xml" not in z.namelist():
            return None

        xml = z.read("AndroidManifest.xml").decode("utf-8")
        root = ET.fromstring(xml)

        android_ns = "{http://schemas.android.com/apk/res/android}"
        uses_sdk = root.find("uses-sdk")
        if uses_sdk is None:
            return None

        value = uses_sdk.get(android_ns + "minSdkVersion")
        return int(value) if value else None

def check_versions(lib, min_sdk, lessEqualMore, expectedSdk):
    prefix = "Bad"
    if "<=" == lessEqualMore and min_sdk <= expectedSdk or "=" == lessEqualMore and min_sdk == expectedSdk or "<" == lessEqualMore and min_sdk < expectedSdk or ">=" == lessEqualMore and min_sdk >= expectedSdk or ">" == lessEqualMore and min_sdk > expectedSdk:
        prefix = "Good"

    print(f"{prefix}: Found[{min_sdk}] expected[{lessEqualMore}{expectedSdk}] in lib[{lib}]")

def inspect_min_sdk(lib, lessEqualMore, expectedSdk):
    group, artifact, version = lib.split(":")
    print(f"\nInspecting {lib}\n")

    aar = download_aar(group, artifact, version)

    min_sdk = min_sdk_from_metadata(aar)
    if min_sdk is not None:
        #print(f"minSdk (aar-metadata): {min_sdk}")
        check_versions(lib, min_sdk, lessEqualMore, expectedSdk)
        return min_sdk

    min_sdk = min_sdk_from_manifest(aar)
    if min_sdk is not None:
        #print(f"minSdk (AndroidManifest.xml): {min_sdk}")
        check_versions(lib, min_sdk, lessEqualMore, expectedSdk)
        return min_sdk

    print("minSdk: not declared")
    return None

def inspect_min_sdk_convert(lib, lessEqualMore, expectedSdk):
    group, artifact, version = lib.split(":")

    versionName = f"{artifact.replace("-","_")}_last_android_sdk19_version"
    versionVar = "${" + versionName + "}"
    print(f"xx val {versionName} = \"{version}\"")
    print(f"        substitute(module(\"{group}:{artifact}\"))")
    print(f"            .using(module(\"{group}:{artifact}:{versionVar}\"))")
    print(f"            .because(\"we need Sdk 19 support\")\n")


if __name__ == "__main__":
    #inspect_min_sdk("", "<=", 19) # is working fine

    androidxLifecycleVersion = '2.8.7'
    androidxRoomVersion = '2.6.1'
    androidxWorkVersion = '2.9.1'
    stateSaverVersion = '1.4.1'
    legacyOkHttpVersion = "3.12.13" # it is just a jar file but the last minSdk working version
    exoPlayerVersion = '2.19.1'
    groupieVersion = '2.10.1'
    markwonVersion = '4.6.2'
    googleAutoServiceVersion = '1.1.1'

    #############################################################################################
    ## below are all libraries that NewPipe lists in app/build.gradle.kts. timestamp: 20260125
    ## -> we check what the minSdk version -- if not commented out.
    #############################################################################################
    #inspect_min_sdk("androidx.constraintlayout:constraintlayout:2.1.4", "<=", 19) # last for minSdk19 working
    #inspect_min_sdk("androidx.appcompat:appcompat:1.6.1", "<=", 19) # last for minSdk19 working
    #inspect_min_sdk("androidx.cardview:cardview:1.0.0", "<=", 19) # is working fine
    #inspect_min_sdk("androidx.core:core-ktx:1.13.0", "<=", 19) # last for minSdk19 working
    #inspect_min_sdk("androidx.documentfile:documentfile:1.0.1", "<=", 19) # last for minSdk19 working
    #inspect_min_sdk("androidx.fragment:fragment-ktx:1.7.1", "<=", 19) # last for minSdk19 working, previously we had 1.6.2
    #inspect_min_sdk(f"androidx.lifecycle:lifecycle-livedata-ktx:{androidxLifecycleVersion}", "<=", 19) # last for minSdk19 working we had 2.6.2
    #inspect_min_sdk(f"androidx.lifecycle:lifecycle-viewmodel-ktx:{androidxLifecycleVersion}", "<=", 19) # last for minSdk19 working we had 2.6.2
    #inspect_min_sdk("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0", "<=", 19) # last for minSdk19 working
    #inspect_min_sdk("androidx.media:media:1.7.1", "<=", 19) # is working fine
    #inspect_min_sdk("androidx.preference:preference:1.2.1", "<=", 19) # is working fine
    #inspect_min_sdk("androidx.recyclerview:recyclerview:1.3.2", "<=", 19) # last for minSdk19 working
    #inspect_min_sdk(f"androidx.room:room-runtime:{androidxRoomVersion}", "<=", 19) # last for minSdk19 working
    #inspect_min_sdk(f"androidx.room:room-rxjava3:{androidxRoomVersion}", "<=", 19) # last for minSdk19 working
    #inspect_min_sdk(f"androidx.room:room-compiler:{androidxRoomVersion}", "<=", 19) # last for minSdk19 working
    #inspect_min_sdk(f"androidx.room:room-testing:{androidxRoomVersion}", "<=", 19) # last for minSdk19 working
    #inspect_min_sdk("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0", "<=", 19) # last for minSdk19 working
    #inspect_min_sdk("androidx.viewpager2:viewpager2:1.1.0", "<=", 19) # is working fine
    #inspect_min_sdk(f"androidx.work:work-runtime:{androidxWorkVersion}", "<=", 19) # last for minSdk19 working we had 2.8.1
    #inspect_min_sdk(f"androidx.work:work-rxjava3:{androidxWorkVersion}", "<=", 19) # last for minSdk19 working we had 2.8.1
    #inspect_min_sdk("", "<=", 19) # is working fine
    #inspect_min_sdk("com.google.android.material:material:1.11.0", "<=", 19) # is working fine
    #inspect_min_sdk("androidx.webkit:webkit:1.12.0", "<=", 19) # last for minSdk19 working we had 1.9.0
    #inspect_min_sdk("com.github.livefront:bridge:v2.0.2", "<=", 19) # is working fine
    #inspect_min_sdk(f"com.evernote:android-state:{stateSaverVersion}", "<=", 19) # is working fine
    #inspect_min_sdk(f"com.evernote:android-state-processor:${stateSaverVersion}", "<=", 19) # is working fine
    #inspect_min_sdk("org.jsoup:jsoup:1.21.2", "<=", 19) # might work pure java library
    #inspect_min_sdk(f"com.squareup.okhttp3:okhttp:{legacyokhttpversion}", "<=", 19) # last for minsdk19 working
    #inspect_min_sdk(f"com.squareup.okhttp3:okhttp-urlconnection:{legacyokhttpversion}", "<=", 19) # last for minsdk19 working
    #inspect_min_sdk(f"com.google.android.exoplayer:exoplayer-core:{exoPlayerVersion}", "<=", 19) # is working fine
    #inspect_min_sdk(f"com.google.android.exoplayer:exoplayer-dash:{exoPlayerVersion}", "<=", 19) # is working fine
    #inspect_min_sdk(f"com.google.android.exoplayer:exoplayer-database:{exoPlayerVersion}", "<=", 19) # is working fine
    #inspect_min_sdk(f"com.google.android.exoplayer:exoplayer-datasource:{exoPlayerVersion}", "<=", 19) # is working fine
    #inspect_min_sdk(f"com.google.android.exoplayer:exoplayer-hls:{exoPlayerVersion}", "<=", 19) # is working fine
    #inspect_min_sdk(f"com.google.android.exoplayer:exoplayer-smoothstreaming:{exoPlayerVersion}", "<=", 19) # is working fine
    #inspect_min_sdk(f"com.google.android.exoplayer:exoplayer-ui:{exoPlayerVersion}", "<=", 19) # is working fine
    #inspect_min_sdk(f"com.google.android.exoplayer:extension-mediasession:{exoPlayerVersion}", "<=", 19) # is working fine
    #inspect_min_sdk(f"com.github.lisawray.groupie:groupie:{groupieVersion}", "<=", 19) # is working fine
    #inspect_min_sdk(f"com.github.lisawray.groupie:groupie-viewbinding:{groupieVersion}", "<=", 19) # is working fine
    #inspect_min_sdk("com.squareup.picasso:picasso:2.8", "<=", 19) # is working fine
    #inspect_min_sdk(f"io.noties.markwon:core:{markwonVersion}", "<=", 19) # is working fine
    #inspect_min_sdk(f"io.noties.markwon:linkify:{markwonVersion}", "<=", 19) # is working fine
    #inspect_min_sdk("ch.acra:acra-core:5.13.1", "<=", 19) # is working fine we had 5.11.3
    #inspect_min_sdk(f"com.google.auto.service:auto-service-annotations:{googleAutoServiceVersion}", "<=", 19) # is working fine
    #inspect_min_sdk(f"com.google.auto.service:auto-service:{googleAutoServiceVersion}", "<=", 19) # is working fine
    #inspect_min_sdk("dev.zacsweers.autoservice:auto-service-ksp:1.2.0", "<=", 19) # might work pure java library
    #inspect_min_sdk("com.jakewharton:process-phoenix:3.0.0", "<=", 19) # is working fine we had 2.1.2
    #inspect_min_sdk("io.reactivex.rxjava3:rxjava:3.1.12", "<=", 19) # might work pure java library we had 3.1.8
    #inspect_min_sdk("io.reactivex.rxjava3:rxandroid:3.0.2", "<=", 19) # is working fine
    #inspect_min_sdk("com.jakewharton.rxbinding4:rxbinding:4.0.0", "<=", 19) # is working fine
    #inspect_min_sdk("org.ocpsoft.prettytime:prettytime:5.0.8.Final", "<=", 19) # is working fine

    ### the missing libraries are only for debugImplemenation/testImplementation in app/build.gradle.kts


    #############################################################################################
    ## below is a copy of all libraries definition from above that have the comment: 'last for minSdk19' aso of 20260125
    ## -> we generate a substitute() kotlin calls if not commented out
    ## -> the code is placed in app/build.gradle.kts in the method: braveLegacyFlavorLastWorkingMinSdk19Libraries()
    #############################################################################################
    #inspect_min_sdk_convert("androidx.constraintlayout:constraintlayout:2.1.4", "<=", 19) # last for minSdk19 working
    #inspect_min_sdk_convert("androidx.appcompat:appcompat:1.6.1", "<=", 19) # last for minSdk19 working
    #inspect_min_sdk_convert("androidx.core:core-ktx:1.13.0", "<=", 19) # last for minSdk19 working
    #inspect_min_sdk_convert("androidx.documentfile:documentfile:1.0.1", "<=", 19) # last for minSdk19 working
    #inspect_min_sdk_convert("androidx.fragment:fragment-ktx:1.7.1", "<=", 19) # last for minSdk19 working, previously we had 1.6.2
    #inspect_min_sdk_convert(f"androidx.lifecycle:lifecycle-livedata-ktx:{androidxLifecycleVersion}", "<=", 19) # last for minSdk19 working we had 2.6.2
    #inspect_min_sdk_convert(f"androidx.lifecycle:lifecycle-viewmodel-ktx:{androidxLifecycleVersion}", "<=", 19) # last for minSdk19 working we had 2.6.2
    #inspect_min_sdk_convert("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0", "<=", 19) # last for minSdk19 working
    #inspect_min_sdk_convert("androidx.recyclerview:recyclerview:1.3.2", "<=", 19) # last for minSdk19 working
    #inspect_min_sdk_convert(f"androidx.room:room-runtime:{androidxRoomVersion}", "<=", 19) # last for minSdk19 working
    #inspect_min_sdk_convert(f"androidx.room:room-rxjava3:{androidxRoomVersion}", "<=", 19) # last for minSdk19 working
    #inspect_min_sdk_convert(f"androidx.room:room-compiler:{androidxRoomVersion}", "<=", 19) # last for minSdk19 working
    #inspect_min_sdk_convert(f"androidx.room:room-testing:{androidxRoomVersion}", "<=", 19) # last for minSdk19 working
    #inspect_min_sdk_convert("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0", "<=", 19) # last for minSdk19 working
    #inspect_min_sdk_convert(f"androidx.work:work-runtime:{androidxWorkVersion}", "<=", 19) # last for minSdk19 working we had 2.8.1
    #inspect_min_sdk_convert(f"androidx.work:work-rxjava3:{androidxWorkVersion}", "<=", 19) # last for minSdk19 working we had 2.8.1
    #inspect_min_sdk_convert(f"com.squareup.okhttp3:okhttp:{legacyOkHttpVersion}", "<=", 19) # last for minSdk19 working
    #inspect_min_sdk_convert(f"com.squareup.okhttp3:okhttp-urlconnection:{legacyOkHttpVersion}", "<=", 19) # last for minSdk19 working
    #inspect_min_sdk_convert("androidx.webkit:webkit:1.12.0", "<=", 19) # last for minSdk19 working we had 1.9.0
