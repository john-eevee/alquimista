package dev.yaee.expert.project

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MixProjectTemplateTest {
    @Test
    fun `generates a standard Mix project with an ExUnit test`() {
        val files = MixProjectTemplate.files("hello-world")

        assertTrue(files.containsKey("mix.exs"))
        assertTrue(files.containsKey("lib/hello_world.ex"))
        assertTrue(files.containsKey("test/test_helper.exs"))
        assertTrue(files.containsKey("test/hello_world_test.exs"))
        assertTrue(files.getValue("mix.exs").contains("app: :hello_world"))
        assertTrue(files.getValue("lib/hello_world.ex").contains("defmodule HelloWorld do"))
    }

    @Test
    fun `normalizes project names to valid Mix app and module names`() {
        val project = MixProjectTemplate.files("123 Cool App")

        assertTrue(project.getValue("mix.exs").contains("app: :app_123_cool_app"))
        assertTrue(project.containsKey("lib/app_123_cool_app.ex"))
        assertEquals("/deps\n/_build\n/.expert\n/cover\n", project.getValue(".gitignore"))
    }
}
