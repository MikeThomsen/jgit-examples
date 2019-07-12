package com.example.demo

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.transport.URIish
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.junit.Test

class JGitTest {
    def temp = File.createTempDir()
    Repository repo
    Git createEmptyRepo() {
        repo = FileRepositoryBuilder.create(new File(temp, ".git"))
        repo.create()
        new Git(repo)
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
        def commit = { message ->
            new File(temp, "README.md").write("""
                # Hello World!
            """)
            git.add().addFilepattern("*.md").call()
            git.commit().setMessage(message).call()
        }
        commit("First commit")
        git.branchCreate().setName("dev").call()
        git.checkout().setName("dev").call()
        def list = git.branchList().call()
        assert list.find { it.name.endsWith("master") }
        assert list.find { it.name.endsWith("dev") }
        commit("Second commit")
        def dev = git.log().all().call()
        assert dev.size() == 2
        git.checkout().setName("master").call()
        def master = git.log().call()
        assert repo.branch == "master"
        def masterCommits = []
        master.each { masterCommits << it }
        assert masterCommits.size() == 1
        assert masterCommits[0].fullMessage == 'First commit'
    }

    @Test
    void testPushPull() {
        def git = createEmptyRepo()
        def commit = { message ->
            new File(temp, "README.md").write("# Hello World!")
            git.add().addFilepattern("README.md").call()
            git.commit().setMessage(message).call()
        }
        commit("New commit ${Calendar.instance.time}")
        def remote = git.remoteAdd()
        remote.setName("GitHub")
        remote.setUri(new URIish("https://github.com/MikeThomsen/jgit-target"))
        remote.call()
        def provider = new UsernamePasswordCredentialsProvider(System.getProperty("github.username"), System.getProperty("github.password"))
        git.pull().setCredentialsProvider(provider).setRemote("GitHub").call()
        git.push().setRemote("GitHub")
                .setCredentialsProvider(provider)
                .call()
    }
}
