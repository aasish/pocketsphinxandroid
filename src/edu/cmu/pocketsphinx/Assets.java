/* ====================================================================
 * Copyright (c) 2014 Alpha Cephei Inc.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY ALPHA CEPHEI INC. ``AS IS'' AND
 * ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL CARNEGIE MELLON UNIVERSITY
 * NOR ITS EMPLOYEES BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * ====================================================================
 */
package edu.cmu.pocketsphinx;

import static android.os.Environment.getExternalStorageState;
import static java.lang.String.format;

import java.io.*;
import java.util.*;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

/**
 * Provides utility methods to keep asset files to external storage to allow
 * further JNI code access assets from a filesystem.
 * 
 * There must be special file {@value #ASSET_LIST_NAME} among the application
 * assets containing relative paths of assets to synchronize. If the
 * corresponding path does not exist on the external storage it is copied. If
 * the path exists checksums are compared and the asset is copied only if there
 * is a mismatch. Checksum is stored in a separate asset with the name that
 * consists of the original name and a suffix that depends on the checksum
 * algorithm (e.g. MD5). Checksum files are copied along with the corresponding
 * asset files.
 * 
 * @author Alexander Solovets
 */
public class Assets {

    protected static final String TAG = Assets.class.getSimpleName();

    public static final String ASSET_LIST_NAME = "assets.lst";
    public static final String SYNC_DIR = "sync";
    public static final String HASH_EXT = ".md5";

    private final AssetManager assetManager;
    private final File externalDir;

    /**
     * Creates new instance.
     * 
     * @param context
     *            application context
     * 
     * @throws IOException
     *             if the directory does not exist
     * 
     * @see android.content.Context#getExternalFilesDir
     * @see android.os.Environment#getExternalStorageState
     */
    public Assets(Context context) throws IOException {
        File appDir = context.getExternalFilesDir(null);
        if (null == appDir)
            throw new IOException("cannot get external files dir, "
                    + "external storage state is " + getExternalStorageState());
        externalDir = new File(appDir, SYNC_DIR);
        assetManager = context.getAssets();
    }

    /**
     * Returns destination path on external storage where assets are copied.
     * 
     * @return path to application directory or null if it does not exists
     */
    public File getExternalDir() {
        return externalDir;
    }

    /**
     * Returns the map of asset paths to the files checksums.
     * 
     * @return path to the root of resources directory on external storage
     * @throws IOException
     *             if an I/O error occurs or "assets.lst" is missing
     */
    public Map<String, String> getItems() throws IOException {
        Map<String, String> items = new HashMap<String, String>();
        for (String path : readLines(openAsset(ASSET_LIST_NAME))) {
            Reader reader = new InputStreamReader(openAsset(path + HASH_EXT));
            items.put(path, new BufferedReader(reader).readLine());
        }
        return items;
    }

    /**
     * Returns path to hash mappings for the previously copied files. This
     * method can be used to find out assets which must be updated.
     */
    public Map<String, String> getExternalItems() {
        try {
            Map<String, String> items = new HashMap<String, String>();
            File assetFile = new File(externalDir, ASSET_LIST_NAME);
            for (String line : readLines(new FileInputStream(assetFile))) {
                String[] fields = line.split(" ");
                items.put(fields[0], fields[1]);
            }
            return items;
        } catch (IOException e) {
            return Collections.emptyMap();
        }
    }

    /**
     * In case you want to create more smart sync implementation, this method
     * returns the list of items which must be syncronized.
     */
    public Collection<String> getItemsToCopy(String path) throws IOException {
        Collection<String> items = new ArrayList<String>();
        Queue<String> queue = new ArrayDeque<String>();
        queue.offer(path);

        while (!queue.isEmpty()) {
            path = queue.poll();
            String[] list = assetManager.list(path);
            for (String nested : list)
                queue.offer(nested);

            if (list.length == 0)
                items.add(path);
        }

        return items;
    }

    private List<String> readLines(InputStream source) throws IOException {
        List<String> lines = new ArrayList<String>();
        BufferedReader br = new BufferedReader(new InputStreamReader(source));
        String line;
        while (null != (line = br.readLine()))
            lines.add(line);
        return lines;
    }

    private InputStream openAsset(String asset) throws IOException {
        return assetManager.open(new File(SYNC_DIR, asset).getPath());
    }

    /**
     * Saves the list of synchronized items. The list is stored as a two-column
     * space-separated list of items in a text file. The file is located at the
     * root of synchronization directory in the external storage.
     * 
     * @param items
     *            the items
     * @throws IOException
     *             if an I/O error occurs
     */
    public void updateItemList(Map<String, String> items) throws IOException {
        File assetListFile = new File(externalDir, ASSET_LIST_NAME);
        PrintWriter pw = new PrintWriter(new FileOutputStream(assetListFile));
        for (Map.Entry<String, String> entry : items.entrySet())
            pw.format("%s %s\n", entry.getKey(), entry.getValue());
        pw.close();
    }

    /**
     * Copies raw asset resource to external storage of the device.
     * 
     * @param path
     *            path of the asset to copy
     * @throws IOException
     *             if an I/O error occurs
     */
    public File copy(String asset) throws IOException {
        InputStream source = openAsset(asset);
        File destinationFile = new File(externalDir, asset);
        destinationFile.getParentFile().mkdirs();
        OutputStream destination = new FileOutputStream(destinationFile);
        byte[] buffer = new byte[1024];
        int nread;

        while ((nread = source.read(buffer)) != -1) {
            if (nread == 0) {
                nread = source.read();
                if (nread < 0)
                    break;
                destination.write(nread);
                continue;
            }
            destination.write(buffer, 0, nread);
        }
        destination.close();
        return destinationFile;
    }

    /**
     * Performs the sync of assets in the application and on the external
     * storage
     * 
     * @return The folder on external storage with data
     * @throws IOException
     */
    public File syncAssets() throws IOException {
        Collection<String> newItems = new ArrayList<String>();
        Collection<String> unusedItems = new ArrayList<String>();
        Map<String, String> items = getItems();
        Map<String, String> externalItems = getExternalItems();

        for (String path : items.keySet()) {
            if (!items.get(path).equals(externalItems.get(path))
                    || !(new File(externalDir, path).exists()))
                newItems.add(path);
            else
                Log.i(TAG,
                        format("Skipping asset %s: checksums are equal", path));

        }

        unusedItems.addAll(externalItems.keySet());
        unusedItems.removeAll(items.keySet());

        for (String path : newItems) {
            File file = copy(path);
            Log.i(TAG, format("Copying asset %s to %s", path, file));
        }

        for (String path : unusedItems) {
            File file = new File(externalDir, path);
            file.delete();
            Log.i(TAG, format("Removing asset %s", file));
        }

        updateItemList(items);
        return externalDir;
    }

}
