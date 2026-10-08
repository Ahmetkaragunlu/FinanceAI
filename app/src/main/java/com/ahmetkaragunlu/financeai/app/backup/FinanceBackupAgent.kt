package com.ahmetkaragunlu.financeai.app.backup

import android.app.backup.BackupAgentHelper
import android.app.backup.FullBackupDataOutput
import com.ahmetkaragunlu.financeai.photo.PhotoStorageUtil
import java.io.File

/** Full backup owns a sanitised financial snapshot, never Firebase/FCM/App Check credentials. */
class FinanceBackupAgent : BackupAgentHelper() {
    override fun onFullBackup(data: FullBackupDataOutput) {
        val staging = File(filesDir, BACKUP_DIRECTORY)
        val snapshot = File(staging, DATABASE_NAME)
        try {
            val database = getDatabasePath(DATABASE_NAME)
            if (database.exists()) {
                FinanceBackupSnapshot.create(database, snapshot)
                fullBackupFile(snapshot, data)
            }
            val photos = File(filesDir, PhotoStorageUtil.PHOTO_DIRECTORY)
            if (photos.isDirectory) {
                photos
                    .listFiles()
                    ?.filter { it.isDirectory }
                    ?.forEach { ownerDirectory ->
                        ownerDirectory
                            .listFiles()
                            ?.filter { file ->
                                file.isFile &&
                                    file.name.endsWith(".jpg") &&
                                    (file.name.startsWith("IMG_") ||
                                        file.name.startsWith("SYNC_")) &&
                                    file.canonicalFile.parentFile == ownerDirectory.canonicalFile &&
                                    ownerDirectory.canonicalFile.parentFile == photos.canonicalFile
                            }
                            ?.forEach { fullBackupFile(it, data) }
                    }
            }
        } finally {
            snapshot.delete()
            staging.delete()
        }
    }

    override fun onRestoreFinished() {
        val snapshot = File(filesDir, "$BACKUP_DIRECTORY/$DATABASE_NAME")
        if (snapshot.exists()) {
            val destination = getDatabasePath(DATABASE_NAME)
            check(
                destination.parentFile?.isDirectory == true ||
                    destination.parentFile?.mkdirs() == true
            )
            // Only known SQLite sidecars of the restored database are disposable.
            for (suffix in listOf("-wal", "-shm", "-journal")) File(destination.path + suffix)
                .delete()
            snapshot.copyTo(destination, overwrite = true)
            FinanceBackupSnapshot.rebasePhotos(destination, filesDir)
            snapshot.delete()
            snapshot.parentFile?.delete()
        }
        super.onRestoreFinished()
    }

    private companion object {
        const val BACKUP_DIRECTORY = "finance_backup"
        const val DATABASE_NAME = "finance_db"
    }
}
