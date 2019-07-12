package com.example.demo

import org.eclipse.jgit.api.Git
import org.junit.Test

class JGitTest {
    Git createEmptyRepo() {
        Git.init().setDirectory(File.createTempDir()).call()
    }

    @Test
    void testCreateRepo() {
        def git = createEmptyRepo()
        def status = git.status().call()
        assert status.clean
    }

    @Test
    void testCreateBranchWithCommit() {
        def git = createEmptyRepo()
        def folder = git.repository.directory
        new File(folder, "README.md").write("""
            # Hello World!
        """)
        git.add().addFilepattern("*.md").call()
        git.commit().setMessage("First commit").call()
        git.branchCreate().setName("dev").call()
        def list = git.branchList().call()
        assert list.find { it.name.endsWith("master") }
        assert list.find { it.name.endsWith("dev") }
    }
}
