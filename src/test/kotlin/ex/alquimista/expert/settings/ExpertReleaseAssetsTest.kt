package ex.alquimista.expert.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExpertReleaseAssetsTest {
    @Test
    fun `selects the matching official Expert asset for supported platforms`() {
        assertEquals("expert_linux_amd64", ExpertReleaseAssets.assetName("linux", "x86_64"))
        assertEquals("expert_linux_arm64", ExpertReleaseAssets.assetName("linux", "aarch64"))
        assertEquals("expert_darwin_arm64", ExpertReleaseAssets.assetName("mac os x", "arm64"))
        assertEquals("expert_windows_amd64.exe", ExpertReleaseAssets.assetName("windows 11", "amd64"))
    }

    @Test
    fun `rejects platform combinations without a published Expert binary`() {
        assertNull(ExpertReleaseAssets.assetName("freebsd", "x86_64"))
        assertNull(ExpertReleaseAssets.assetName("windows", "aarch64"))
    }

    @Test
    fun `reads checksums from the release manifest by asset name`() {
        val manifest = """${"a".repeat(64)}  ./expert_linux_amd64
            |${"b".repeat(64)}  ./expert_windows_amd64.exe
        """.trimMargin()

        assertEquals("a".repeat(64), ExpertReleaseAssets.checksumFor("expert_linux_amd64", manifest))
        assertNull(ExpertReleaseAssets.checksumFor("expert_linux_arm64", manifest))
    }
}
