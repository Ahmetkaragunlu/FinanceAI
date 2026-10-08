package com.ahmetkaragunlu.financeai.core.media.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.util.UUID
import org.junit.After
import org.junit.Assert.*
import org.junit.Test

class CameraPhotoDraftsTest {
    private var draft: File? = null
    @After fun cleanup() { draft?.let { it.delete(); it.parentFile?.delete() } }

    @Test fun cameraDraftKeepsTheAccountPrivatePathAndFileProviderContract() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val owner = "camera-test-${UUID.randomUUID()}"
        val result = CameraPhotoDrafts.createTempPhotoFile(context, owner)
        assertNotNull(result)
        val (file, uri) = checkNotNull(result)
        draft = file
        assertTrue(file.isFile)
        assertTrue(file.name.startsWith("TEMP_"))
        assertEquals(File(context.filesDir, "${PhotoFiles.DIRECTORY}/$owner").canonicalFile, checkNotNull(file.parentFile).canonicalFile)
        assertEquals("content", uri.scheme)
        assertEquals("${context.packageName}.fileprovider", uri.authority)
    }
    @Test fun invalidOwnersCannotCreateFilesOutsideTheirPrivateDirectory() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        for (owner in listOf("", ".", "..", "../other"))
            assertNull(CameraPhotoDrafts.createTempPhotoFile(context, owner))
    }
}
