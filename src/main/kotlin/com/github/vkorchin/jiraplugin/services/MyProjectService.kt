package com.github.vkorchin.jiraplugin.services

import com.intellij.openapi.project.Project
import com.github.vkorchin.jiraplugin.MyBundle

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
