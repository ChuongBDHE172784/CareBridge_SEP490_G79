allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

val newBuildDir: Directory =
    rootProject.layout.buildDirectory
        .dir("../../build")
        .get()
rootProject.layout.buildDirectory.value(newBuildDir)

subprojects {
    val newSubprojectBuildDir: Directory = newBuildDir.dir(project.name)
    project.layout.buildDirectory.value(newSubprojectBuildDir)

    pluginManager.withPlugin("com.android.library") {
        extensions.configure<com.android.build.api.variant.LibraryAndroidComponentsExtension> {
            // Zego Express 3.24.1 still declares compileSdk 31, while its
            // current AndroidX dependencies require API 34 or newer.
            finalizeDsl { extension ->
                extension.compileSdk = 36
            }
        }
    }
}
subprojects {
    project.evaluationDependsOn(":app")
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
