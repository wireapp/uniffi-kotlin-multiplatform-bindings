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

    val generatedDir = buildDir.resolve("generated").resolve("uniffi")
    val crateDir = projectDir.resolve("uniffi")
    val crateTargetDir = crateDir.resolve("target")
    val crateTargetBindingsDir = crateTargetDir.resolve("bindings")
    val crateTargetLibDir = crateTargetDir.resolve("debug")
//    fun resolveDynamicLibFile(crate: String, lib: String = crate) =
//        resolveTargetLibDir(crate).resolve("lib$lib.so")

//    val crates = listOf("coverall", "callbacks", "external_types")
//    val cratesDynamicLibs = crates.map { resolveDynamicLibFile(it) } + listOf(
////    resolveDynamicLibFile("external_types", "crate_one"),
////    resolveDynamicLibFile("external_types", "crate_two")
//    )

    val buildCrate = tasks.register("buildCrate", Exec::class) {
        group = "uniffi"
        workingDir(crateDir)
        commandLine("cargo", "build")
    }

    // Creating the bindings requires analysing the compiled libary in order to get the metadata from
    // uniffi's proc macro output
    // TODO implement that for coverall and external_types
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
        from(crateTargetLibDir)
        include("*.so")
        into(
            buildDir.resolve("processedResources").resolve("jvm").resolve("main").resolve("linux-x86-64")
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
//    js(BOTH) {
//        browser {
//            commonWebpackConfig {
//                cssSupport.enabled = true
//            }
//        }
//    }
        val hostOs = System.getProperty("os.name")
        val isMingwX64 = hostOs.startsWith("Windows")
        val nativeTarget = when {
            hostOs == "Mac OS X" -> macosX64("native")
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
