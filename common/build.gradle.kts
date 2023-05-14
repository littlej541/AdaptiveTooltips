plugins {
    alias(libs.plugins.architectury.loom)
}

base {
    archivesName.set("adaptive-tooltips")
}

architectury {
    common("fabric", "forge")
}

loom {
    silentMojangMappingsLicense()

    accessWidenerPath.set(file("src/main/resources/adaptivetooltips.accesswidener"))
}

val minecraftVersion = libs.versions.minecraft.get()

dependencies {
    minecraft(libs.minecraft)
    mappings(loom.layered {
        mappings("org.quiltmc:quilt-mappings:$minecraftVersion+build.${libs.versions.quilt.mappings.get()}:intermediary-v2")
        officialMojangMappings()
    })
    modImplementation(libs.fabric.loader)

    modCompileOnly(libs.yacl.fabric)

    libs.mixin.extras.common.let {
        include(it)
        implementation(it)
        annotationProcessor(it)
    }

}

java {
    withSourcesJar()
}

tasks {
    remapJar {
        archiveClassifier.set(null as String?)

        from(rootProject.file("LICENSE"))
    }
}

publishing {
    publications {
        create<MavenPublication>("common") {
            groupId = "dev.isxander"
            artifactId = "adaptive-tooltips-common"

            from(components["java"])
        }
    }
}
tasks.findByPath("publishCommonPublicationToReleasesRepository")?.let {
    rootProject.tasks["releaseMod"].dependsOn(it)
}
