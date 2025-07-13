package com.kintmin.buildSrc

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

abstract class RenameApkTask : DefaultTask() {

    @get:Input
    abstract val variantName: Property<String>

    @get:Input
    abstract val buildType: Property<String>

    @get:InputArtifact
    abstract val inputApk: RegularFileProperty

    @TaskAction
    fun rename() {
        val apkFile = inputApk.get().asFile
        val timestamp = SimpleDateFormat("yyyyMMddHHmmss").format(Date())
        val newFileName = "jellytube_${buildType.get()}_${timestamp}.apk"
        val newFile = File(apkFile.parentFile, newFileName)

        apkFile.copyTo(newFile, overwrite = true)
    }
}