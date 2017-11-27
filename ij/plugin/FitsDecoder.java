package ij.plugin;

import ij.IJ;
import ij.io.FileInfo;
import ij.util.Tools;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

class FitsDecoder {
    private String directory, fileName;
    private DataInputStream f;
    private StringBuffer info = new StringBuffer(512);
    double bscale = 1.0, bzero = 0.0;

    public FitsDecoder(String directory, String fileName) {
        this.directory = directory;
        this.fileName = fileName;
    }

    FileInfo getInfo() throws IOException
    {
        FileInfo fi = new FileInfo();
        fi.fileFormat = FileInfo.FITS;
        fi.fileName = fileName;
        fi.directory = directory;
        fi.width = 0;
        fi.height = 0;
        fi.offset = 0;

        InputStream is = new FileInputStream(directory + fileName);
        if (fileName.toLowerCase().endsWith(".gz")) is = new GZIPInputStream(is);
        f = new DataInputStream(is);
        String line = getString(80);
        info.append(line+"\n");
        if (!line.startsWith("SIMPLE"))
        {f.close(); return null;}
        int count = 1;
        while ( true ) {
            count++;
            line = getString(80);
            info.append(line+"\n");

            // Cut the key/value pair
            int index = line.indexOf ( "=" );

            // Strip out comments
            int commentIndex = line.indexOf ( "/", index );
            if ( commentIndex < 0 )
                commentIndex = line.length ();

            // Split that values
            String key;
            String value;
            if ( index >= 0 ) {
                key = line.substring ( 0, index ).trim ();
                value = line.substring ( index + 1, commentIndex ).trim ();
            } else {
                key = line.trim ();
                value = "";
            }

            // Time to stop ?
            if (key.equals ("END") ) break;

            // Look for interesting information
            if (key.equals("BITPIX")) {
                int bitsPerPixel = Integer.parseInt ( value );
                if (bitsPerPixel==8)
                    fi.fileType = FileInfo.GRAY8;
                else if (bitsPerPixel==16)
                    fi.fileType = FileInfo.GRAY16_SIGNED;
                else if (bitsPerPixel==32)
                    fi.fileType = FileInfo.GRAY32_INT;
                else if (bitsPerPixel==-32)
                    fi.fileType = FileInfo.GRAY32_FLOAT;
                else if (bitsPerPixel==-64)
                    fi.fileType = FileInfo.GRAY64_FLOAT;
                else {
                    IJ.error("BITPIX must be 8, 16, 32, -32 (float) or -64 (double).");
                    f.close();
                    return null;
                }
            } else if (key.equals("NAXIS1"))
                fi.width = Integer.parseInt ( value );
            else if (key.equals("NAXIS2"))
                fi.height = Integer.parseInt( value );
            else if (key.equals("NAXIS3")) //for multi-frame fits
                fi.nImages = Integer.parseInt ( value );
            else if (key.equals("BSCALE"))
                bscale = Tools.parseDouble(value, 1.0);
            else if (key.equals("BZERO"))
                bzero = Tools.parseDouble(value, 0.0);
            //            else if (key.equals("CDELT1"))                //LET WCS TAKE CARE OF ADDING THIS INFO
            //                fi.pixelWidth = Math.abs(Tools.parseDouble(value, 1.0));
            //            else if (key.equals("CDELT2"))
            //                fi.pixelHeight = Math.abs(Tools.parseDouble(value, 1.0));
            //            else if (key.equals("CDELT3"))
            //                    fi.pixelDepth = parseDouble ( value );
            //            else if (key.equals("CTYPE1"))
            //                fi.unit = value;

            if (count>360 && fi.width==0)
            {f.close(); return null;}
        }
        if (fi.fileType==FileInfo.GRAY32_INT && bzero == 2147483648.0 && bscale == 1.0) fi.fileType = FileInfo.GRAY32_UNSIGNED;
        f.close();
        fi.offset = 2880+2880*(((count*80)-1)/2880);
        return fi;
    }

    String getString(int length) throws IOException {
        byte[] b = new byte[length];
        f.readFully(b);
        if (IJ.debugMode)
            IJ.log(new String(b));
        return new String(b);
    }

    int getInteger(String s) {
        s = s.substring(10, 30);
        s = s.trim();
        return Integer.parseInt(s);
    }

    double parseDouble(String s) throws NumberFormatException {
        Double d = new Double(s);
        return d.doubleValue();
    }

    String getHeaderInfo() {
        return new String(info);
    }

}

