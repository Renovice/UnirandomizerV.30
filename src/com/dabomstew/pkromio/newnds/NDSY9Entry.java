package com.dabomstew.pkromio.newnds;

import com.dabomstew.pkromio.FileFunctions;
import cuecompressors.BLZCoder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

/*----------------------------------------------------------------------------*/
/*--  NDSY9Entry.java - an entry in the arm9 overlay system                 --*/
/*--  Code based on "Nintendo DS rom tool", copyright (C) DevkitPro         --*/
/*--  Original Code by Rafael Vuijk, Dave Murphy, Alexei Karpenko           --*/
/*--                                                                        --*/
/*--  Ported to Java by Dabomstew under the terms of the GPL:               --*/
/*--                                                                        --*/
/*--  This program is free software: you can redistribute it and/or modify  --*/
/*--  it under the terms of the GNU General Public License as published by  --*/
/*--  the Free Software Foundation, either version 3 of the License, or     --*/
/*--  (at your option) any later version.                                   --*/
/*--                                                                        --*/
/*--  This program is distributed in the hope that it will be useful,       --*/
/*--  but WITHOUT ANY WARRANTY; without even the implied warranty of        --*/
/*--  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the          --*/
/*--  GNU General Public License for more details.                          --*/
/*--                                                                        --*/
/*--  You should have received a copy of the GNU General Public License     --*/
/*--  along with this program. If not, see <http://www.gnu.org/licenses/>.  --*/
/*----------------------------------------------------------------------------*/

public class NDSY9Entry {

    private NDSRom parent;
    public int offset, size, original_size;
    public int fileID;
    public int overlay_id;
    public int ram_address, ram_size;
    public int bss_size;
    public int static_start, static_end;
    public int compressed_size;
    public int compress_flag;
    private Extracted status = Extracted.NOT;
    private String extFilename;
    public byte[] data;
    public long originalCRC;
    private boolean decompressed_data = false;

    public NDSY9Entry(NDSRom parent) {
        this.parent = parent;
    }

    public byte[] getContents() throws IOException {
        if (this.status == Extracted.NOT) {
            // extract file
            parent.reopenROM();
            RandomAccessFile rom = parent.getBaseRom();
            byte[] buf = new byte[this.original_size];
            rom.seek(this.offset);
            rom.readFully(buf);
            originalCRC = FileFunctions.getCRC32(buf);
            
            // Diagnostic output for overlay load
            System.out.println("DEBUG: Loaded overlay " + overlay_id +
                " - compress_flag=" + compress_flag +
                " original_size=" + original_size +
                " compressed_size=" + compressed_size +
                " file_size=" + buf.length);

            // Compression detection:
            // An overlay is compressed if compress_flag != 0 (usually 1 or 3).
            // In vanilla ROMs: original_size == compressed_size when compressed.
            // In modified ROMs (like fairy patches): compressed_size may differ from original_size
            // because the patched overlay compresses to a different size.
            // Check if compress_flag indicates compression (value 1, 2, or 3 where 3=compressed, 1=compressed old format)
            boolean isCompressed = (compress_flag == 1 || compress_flag == 3);

            // HACK: Some fairy ROMs have incorrect compress_flag values
            // Try to detect if data looks compressed even when flag says uncompressed
            if (!isCompressed && compress_flag == 2 && buf.length > 100) {
                // Check if first few bytes look like ARM code (indicating compressed data that wasn't decompressed)
                // ARM instructions typically have specific patterns - if we see unusual byte sequences, try decompressing
                int suspiciousBytes = 0;
                for (int i = 0; i < Math.min(20, buf.length); i++) {
                    int b = buf[i] & 0xFF;
                    // Type table values should be 0, 2, 4, or 8
                    // If we see lots of other values, this might be compressed
                    if (b != 0 && b != 2 && b != 4 && b != 8) {
                        suspiciousBytes++;
                    }
                }

                if (suspiciousBytes > 10) {
                    System.out.println("DEBUG: Overlay " + overlay_id + " has compress_flag=2 but data looks compressed (suspicious bytes: " + suspiciousBytes + "). Attempting decompression...");
                    try {
                        byte[] decompressed = new BLZCoder(null).BLZ_DecodePub(buf, "overlay " + overlay_id);
                        if (decompressed != null && decompressed.length > buf.length) {
                            System.out.println("DEBUG: Force-decompressed overlay " + overlay_id + " from " + buf.length + " to " + decompressed.length + " bytes");
                            buf = decompressed;
                            decompressed_data = true;
                        }
                    } catch (Exception e) {
                        System.out.println("DEBUG: Decompression attempt failed, using data as-is");
                    }
                }
            }

            if (isCompressed && this.compressed_size != 0) {
                System.out.println("DEBUG: Overlay " + overlay_id + " is compressed (flag=" + compress_flag + "), decompressing from " + buf.length + " bytes...");
                buf = new BLZCoder(null).BLZ_DecodePub(buf, "overlay " + overlay_id);
                System.out.println("DEBUG: Overlay " + overlay_id + " decompressed to " + buf.length + " bytes");
                decompressed_data = true;
            } else if (compress_flag != 0 && compress_flag != 2) {
                System.out.println("DEBUG: Overlay " + overlay_id + " has unknown compress_flag=" + compress_flag + ", treating as uncompressed");
            }
            if (parent.isWritingEnabled()) {
                // make a file
                String tmpDir = parent.getTmpFolder();
                String fullPath = String.format("overlay_%04d", overlay_id);
                this.extFilename = fullPath.replaceAll("[^A-Za-z0-9_]+", "");
                File tmpFile = new File(tmpDir + extFilename);
                FileOutputStream fos = new FileOutputStream(tmpFile);
                fos.write(buf);
                fos.close();
                tmpFile.deleteOnExit();
                this.status = Extracted.TO_FILE;
                this.data = null;
                return buf;
            } else {
                this.status = Extracted.TO_RAM;
                this.data = buf;
                byte[] newcopy = new byte[buf.length];
                System.arraycopy(buf, 0, newcopy, 0, buf.length);
                return newcopy;
            }
        } else if (this.status == Extracted.TO_RAM) {
            byte[] newcopy = new byte[this.data.length];
            System.arraycopy(this.data, 0, newcopy, 0, this.data.length);
            return newcopy;
        } else {
            String tmpDir = parent.getTmpFolder();
            return FileFunctions.readFileFullyIntoBuffer(tmpDir + this.extFilename);
        }
    }

    public void writeOverride(byte[] data) throws IOException {
        if (status == Extracted.NOT) {
            // temp extract
            getContents();
        }
        size = data.length;
        
        System.out.println("DEBUG: writeOverride for overlay " + overlay_id +
            " - new_size=" + data.length +
            " compress_flag=" + compress_flag +
            " decompressed_data=" + decompressed_data +
            " original_size=" + original_size);

        // Note: compressed_size will be updated in getOverrideContents() when we actually compress
        // For uncompressed overlays (flag 0 or 2), we update it here
        boolean willBeCompressed = (compress_flag == 1 || compress_flag == 3);
        if (!willBeCompressed) {
            compressed_size = size;
            System.out.println("DEBUG: Overlay " + overlay_id + " will be stored uncompressed, setting compressed_size to " + size);
        } else {
            System.out.println("DEBUG: Overlay " + overlay_id + " will be recompressed on save, compressed_size will be set during compression");
        }
        if (status == Extracted.TO_FILE) {
            String tmpDir = parent.getTmpFolder();
            FileOutputStream fos = new FileOutputStream(new File(tmpDir + this.extFilename));
            fos.write(data);
            fos.close();
        } else {
            if (this.data.length == data.length) {
                // copy new in
                System.arraycopy(data, 0, this.data, 0, data.length);
            } else {
                // make new array
                this.data = null;
                this.data = new byte[data.length];
                System.arraycopy(data, 0, this.data, 0, data.length);
            }
        }
        if (data.length > ram_size) {
            System.out.println("DEBUG: Overlay " + overlay_id + " growing beyond ram_size: " + data.length + " > " + ram_size);
            parent.setOverlayRamSize(overlay_id, data.length);
        }
    }

    // returns null if no override
    public byte[] getOverrideContents() throws IOException {
        if (status == Extracted.NOT) {
            return null;
        }
        byte[] buf = getContents();
        // If the overlay was originally compressed (flag 1 or 3), we MUST recompress it
        // regardless of whether the size changed. The compress_flag tells us
        // the game expects this overlay to be compressed.
        // Flag values: 0=uncompressed, 1=compressed(old), 2=uncompressed(new), 3=compressed(new)
        boolean shouldCompress = (this.compress_flag == 1 || this.compress_flag == 3);

        if (shouldCompress && this.decompressed_data) {
            int beforeSize = buf.length;
            buf = new BLZCoder(null).BLZ_EncodePub(buf, false, false, "overlay " + overlay_id);
            int afterSize = buf.length;
            this.compressed_size = buf.length;
            System.out.println("DEBUG [Overlay " + overlay_id + "]: Recompressed " + beforeSize + " -> " + afterSize + " (compress_flag=" + compress_flag + ", original was " + original_size + ")");
        } else if (shouldCompress && !this.decompressed_data) {
            System.out.println("WARNING [Overlay " + overlay_id + "]: compress_flag=" + compress_flag + " but decompressed_data=false, this may indicate a problem!");
        }
        return buf;
    }

    private enum Extracted {
        NOT, TO_FILE, TO_RAM
    }

}
