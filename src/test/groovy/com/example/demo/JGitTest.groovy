package com.example.demo

import org.eclipse.jgit.api.Git
import org.junit.Test

class JGitTest {
    @Test
    void testCreateRepo() {
        def git = Git.init().setDirectory(File.createTempDir()).call()
        def status = git.status().call()
        assert status.clean
    }
}
