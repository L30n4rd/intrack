plugins {
    id("com.android.application")
}

android {
    namespace = "com.l30n4rd.genlib"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        externalNativeBuild {
            cmake {
                // explicitly build libs
                targets("apriltag")
            }

        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
}

// dependencies {
//     implementation fileTree(dir: 'libs', include: ['*.jar'])
//     implementation 'com.android.support:appcompat-v7:28.0.0'
// }
