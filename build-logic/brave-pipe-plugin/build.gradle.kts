plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

gradlePlugin {
    plugins {
        register("brave.pipe.plugin") {
            id = "brave.pipe.plugin"
            implementationClass = "BravePipePlugin"
        }
    }
}

dependencies {
    implementation(gradleApi())
    compileOnly("com.android.tools.build:gradle:${libs.versions.agp.get()}")
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin.get()}")
}

repositories {
    google()
    mavenCentral()
}
