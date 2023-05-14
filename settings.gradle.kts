pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net")
        maven("https://maven.architectury.dev/")
        maven("https://maven.minecraftforge.net/")
        maven("https://maven.quiltmc.org/repository/release")
    }
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs")
    }
}

rootProject.name = "AdaptiveTooltips"
include("common")
include("fabric")
include("forge")
