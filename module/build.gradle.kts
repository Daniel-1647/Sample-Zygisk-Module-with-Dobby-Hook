plugins {
    id("com.android.library")
}

android {
    compileSdk = 30
    ndkVersion = "25.1.8937393"

    defaultConfig {
        minSdk = 16
        compileSdk = 30

        packagingOptions{
            resources {
                excludes += "META-INF/**"
            }
            jniLibs {
                excludes += "**/liblog.so"
                excludes += "**/libdobby.so"
                excludes += "**/libdl.so"
            }
        }

        externalNativeBuild{
            cmake{
                arguments += "-DANDROID_STL=none"
                arguments += "-DCMAKE_BUILD_TYPE=MinSizeRel"
                arguments += "-DPlugin.Android.BionicLinkerUtil=ON"

                cppFlags += "-std=c++2b"
                cppFlags += "-fno-exceptions"
                cppFlags += "-fno-rtti"
                cppFlags += "-fvisibility=hidden"
                cppFlags += "-fvisibility-inlines-hidden"
            }
        }
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
    buildFeatures {
        prefab = true
    }
}
dependencies {
    implementation("dev.rikka.ndk.thirdparty:cxx:1.2.0")
}

tasks.register("updateModuleProp") {
    doLast {
        val versionName = "v1.0"
        val versionCode = "100"

        val modulePropFile = project.rootDir.resolve("module/module/module.prop")

        var content = modulePropFile.readText()

        content = content.replace(Regex("version=.*"), "version=$versionName")
        content = content.replace(Regex("versionCode=.*"), "versionCode=$versionCode")

        modulePropFile.writeText(content)
    }
}


tasks.register("copyFiles") {
    dependsOn("updateModuleProp")

    doLast {
        val moduleFolder = project.rootDir.resolve("module/module")
        val soDir =
            project.layout.buildDirectory.get().asFile.resolve("intermediates/stripped_native_libs/debug/out/lib")

        soDir.walk().filter { it.isFile && it.extension == "so" }.forEach { soFile ->
            val abiFolder = soFile.parentFile.name
            val destination = moduleFolder.resolve("zygisk/$abiFolder.so")
            soFile.copyTo(destination, overwrite = true)
        }
    }
}

tasks.register<Zip>("zip") {
    dependsOn("copyFiles")

    archiveFileName.set("Daniel.zip")
    destinationDirectory.set(project.rootDir.resolve("out"))

    from(project.rootDir.resolve("module/module"))
}

afterEvaluate {
    tasks["assembleDebug"].finalizedBy("updateModuleProp", "copyFiles", "zip")
}