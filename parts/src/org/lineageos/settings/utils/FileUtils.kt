/*
 * Copyright (C) 2016 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lineageos.settings.utils

import android.util.Log
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.FileNotFoundException
import java.io.IOException

object FileUtils {
    private const val TAG = "FileUtils"

    /**
     * Reads the first line of text from the given file.
     * @return the read line contents, or null on failure
     */
    @JvmStatic
    fun readOneLine(fileName: String): String? {
        return try {
            BufferedReader(FileReader(fileName), 512).use { reader ->
                reader.readLine()
            }
        } catch (e: FileNotFoundException) {
            Log.w(TAG, "No such file $fileName for reading", e)
            null
        } catch (e: IOException) {
            Log.e(TAG, "Could not read from file $fileName", e)
            null
        }
    }

    /**
     * Writes the given value into the given file
     * @return true on success, false on failure
     */
    @JvmStatic
    fun writeLine(fileName: String, value: String): Boolean {
        return try {
            BufferedWriter(FileWriter(fileName)).use { writer ->
                writer.write(value)
            }
            true
        } catch (e: FileNotFoundException) {
            Log.w(TAG, "No such file $fileName for writing", e)
            false
        } catch (e: IOException) {
            Log.e(TAG, "Could not write to file $fileName", e)
            false
        }
    }

    /**
     * Checks whether the given file exists
     * @return true if exists, false if not
     */
    @JvmStatic
    fun fileExists(fileName: String): Boolean = File(fileName).exists()

    /**
     * Checks whether the given file is readable
     * @return true if readable, false if not
     */
    @JvmStatic
    fun isFileReadable(fileName: String): Boolean {
        val file = File(fileName)
        return file.exists() && file.canRead()
    }

    /**
     * Checks whether the given file is writable
     * @return true if writable, false if not
     */
    @JvmStatic
    fun isFileWritable(fileName: String): Boolean {
        val file = File(fileName)
        return file.exists() && file.canWrite()
    }

    /**
     * Deletes an existing file
     * @return true if the delete was successful, false if not
     */
    @JvmStatic
    fun delete(fileName: String): Boolean {
        return try {
            File(fileName).delete()
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException trying to delete $fileName", e)
            false
        }
    }

    /**
     * Renames an existing file
     * @return true if the rename was successful, false if not
     */
    @JvmStatic
    fun rename(srcPath: String, dstPath: String): Boolean {
        return try {
            File(srcPath).renameTo(File(dstPath))
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException trying to rename $srcPath to $dstPath", e)
            false
        } catch (e: NullPointerException) {
            Log.e(TAG, "NullPointerException trying to rename $srcPath to $dstPath", e)
            false
        }
    }
}
