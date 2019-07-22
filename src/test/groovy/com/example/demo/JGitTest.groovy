package com.example.demo

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ListBranchCommand
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.transport.URIish
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.eclipse.jgit.treewalk.TreeWalk
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

    @Test
    void testReadCommitFromBranch() {
        def git = createEmptyRepo()
        def commit = { name, message ->
            new File(temp, name).write("# Hello World!")
            git.add().addFilepattern(name).call()
            git.commit().setMessage(message).call()
        }
        commit("README.md", "First commit")
        git.branchCreate().setName("dev").call()
        git.checkout().setName("dev").call()
        commit("LICENSE", "Do what thou wilt!")
        commit("NOTICE", "Nothing to see here, move along.")
        git.checkout().setName("master").call()
        def dev = git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call().find { it.name.endsWith("/dev") }
        assert dev
        println git.repository.directory
        List<RevCommit> found = []
        git.log().add(git.repository.resolve(dev.name)).call().each {
            println it.fullMessage
            found << it
        }
        assert found.size() == 3
        assert found[0].fullMessage == "Nothing to see here, move along."

        def treeWalk = new TreeWalk(git.repository)
        treeWalk.addTree(found[0].tree)
        treeWalk.setRecursive(true)
        assert treeWalk.next()
        def objectId = treeWalk.getObjectId(0)
        def loader = git.repository.open(objectId)
        def out = new ByteArrayOutputStream()
        loader.copyTo(out)
        out.close()
        def output = new String(out.toByteArray())
        assert output == "# Hello World!"
    }
}
