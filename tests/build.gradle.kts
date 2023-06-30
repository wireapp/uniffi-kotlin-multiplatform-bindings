import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        mavenCentral()
    }

    dependencies {
        classpath("org.jetbrains.kotlinx:atomicfu-gradle-plugin:0.18.5")
    }
}

plugins {
    kotlin("multiplatform") version "1.8.20"
}

allprojects {
    group = "net.folivo"
    version = "0.0.1"

    repositories {
        mavenCentral()
    }
}

kotlin {
    jvm() // just a dummy
}

subprojects {
    apply(plugin = "kotlinx-atomicfu")
    apply(plugin = "org.jetbrains.kotlin.multiplatform")

    val hostOs = System.getProperty("os.name")
    val hostArch = System.getProperty("os.arch")
    val isMingwX64 = hostOs.startsWith("Windows")

    val cargoTarget = when {
        hostOs == "Mac OS X" && hostArch == "aarch64" -> "aarch64-apple-darwin"
        hostOs == "Mac OS X" && hostArch == "x86_64" -> "x86_64-apple-darwin"
        hostOs == "Linux" -> "x86_64-unknown-linux-gnu"
        else -> throw GradleException("Host OS $hostOs is not supported.")
    }

    val generatedDir = buildDir.resolve("generated").resolve("uniffi")
    val crateDir = projectDir.resolve("uniffi")
    val crateTargetDir = crateDir.resolve("target/$cargoTarget")
    val crateTargetBindingsDir = crateDir.resolve("target").resolve("bindings")
    val crateTargetLibDir = crateTargetDir.resolve("debug")

    val buildCrate = tasks.register("buildCrate", Exec::class) {
        group = "uniffi"
        workingDir(crateDir)
        commandLine("cargo", "build", "--target", cargoTarget)
    }

    // Creating the bindings requires analysing the compiled library in order to get the metadata from
    // uniffi's proc macro output
    val createBindings = tasks.register("createBindings", Exec::class) {
        group = "uniffi"
        workingDir(crateDir)
        commandLine("cargo", "run", "--bin", "create_bindings")
        dependsOn(buildCrate)
    }

    val copyBindings = tasks.register("copyBindings", Copy::class) {
        group = "uniffi"
        from(crateTargetBindingsDir)
        into(generatedDir)
        dependsOn(createBindings)
    }

    val copyBinariesToProcessedRessources = tasks.register("copyBinaries", Copy::class) {
        group = "uniffi"

        val sharedObjectPattern = when {
            hostOs == "Mac OS X" -> "*.dylib"
            hostOs == "Linux" -> "*.so"
            else -> throw IllegalStateException()
        }
        val destinationResourceDirectory = when {
            hostOs == "Mac OS X" && hostArch == "aarch64" -> "darwin-aarch64"
            hostOs == "Mac OS X" && hostArch == "x86_64" -> "darwin-x86-64"
            hostOs == "Linux" -> "linux-x86-64"
            else -> throw IllegalStateException()
        }

        from(crateTargetLibDir)

        include(sharedObjectPattern)
        into(
            buildDir.resolve("processedResources")
                .resolve("jvm")
                .resolve("main")
                .resolve(destinationResourceDirectory)
        )
        dependsOn(buildCrate)
    }

    tasks.withType<ProcessResources> {
        dependsOn(copyBinariesToProcessedRessources)
    }

    tasks.withType<KotlinCompile> {
        dependsOn(copyBindings)
    }

    kotlin {
        jvm {
            compilations.all {
                kotlinOptions.jvmTarget = "1.8"
            }
            withJava()
            testRuns["test"].executionTask.configure {
                useJUnitPlatform()
            }
        }

        val nativeTarget = when {
            hostOs == "Mac OS X" && hostArch == "aarch64" -> macosArm64("native")
            hostOs == "Mac OS X" && hostArch == "x86_64" -> macosX64("native")
            hostOs == "Linux" -> linuxX64("native")
            isMingwX64 -> mingwX64("native")
            else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
        }

        nativeTarget.apply {
            compilations.getByName("main") {
                cinterops {
                    val uniffi by creating {
                        val crate = this@subprojects.name
                        packageName("$crate.cinterop")
                        header(
                            generatedDir.resolve("nativeInterop").resolve("cinterop").resolve("headers")
                                .resolve(crate).resolve("$crate.h")
                        )
                        tasks.named(interopProcessingTaskName) {
                            dependsOn(copyBinariesToProcessedRessources, copyBindings)
                        }
                        extraOpts("-libraryPath", crateTargetLibDir.absolutePath)
                    }
                }
            }
            binaries {
                executable {
                    entryPoint = "main"
                }
            }
        }

        sourceSets {
            val commonMain by getting {
                kotlin.srcDir(generatedDir.resolve("commonMain").resolve("kotlin"))
                dependencies {
                    implementation("com.squareup.okio:okio:3.2.0")
                    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
                    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
                }
            }
            val commonTest by getting {
                dependencies {
                    implementation(kotlin("test"))
                    implementation("io.kotest:kotest-assertions-core:5.5.4")
                    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4")
                }
            }
            val jvmMain by getting {
                kotlin.srcDir(generatedDir.resolve("jvmMain").resolve("kotlin"))
                dependencies {
                    implementation("net.java.dev.jna:jna:5.12.1")
                }
            }
            val jvmTest by getting
//        val jsMain by getting
//        val jsTest by getting
            val nativeMain by getting {
                kotlin.srcDir(generatedDir.resolve("nativeMain").resolve("kotlin"))
            }
            val nativeTest by getting
        }
    }
}
