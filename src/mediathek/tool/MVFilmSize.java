/*
 *   MediathekView
 *   Copyright (C) 2013 W. Xaver
 *   W.Xaver[at]googlemail.com
 *   http://zdfmediathk.sourceforge.net/
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package mediathek.tool;

import mediathek.controller.Log;

public class MVFilmSize implements Comparable<MVFilmSize> {

    private long aktSizeL = -1L;
    public Long sizeL = 0L;
    private String sizeStr = "";

    public MVFilmSize() {
    }

    @Override
    public int compareTo(MVFilmSize ll) {
        return (sizeL.compareTo(ll.sizeL));
    }

    @Override
    public String toString() {
        return sizeStr;
    }

    public void setSize(String size) {
        // im Film ist die Größe in "MB" !!
        if (size.isEmpty()) {
            aktSizeL = -1L;
            sizeL = 0L;
            sizeStr = "";
        } else {
            try {
                sizeL = Long.valueOf(size);
                sizeL = sizeL * 1000 * 1000;
                sizeStr = size;
            } catch (Exception ex) {
                Log.fehlerMeldung(978745320, Log.FEHLER_ART_MREADER, MVFilmSize.class.getName(), ex, "String: " + size);
                sizeL = 0L;
                sizeStr = "";
            }
        }
    }

    public void reset() {
        aktSizeL = -1L;
        setString();
    }

    public void setSize(long l) {
        sizeL = l;
        setString();
    }

    public long getSize() {
        return sizeL;
    }

    public void setAktSize(long l) {
        aktSizeL = l;
        setString();
    }

    public void addAktSize(long l) {
        aktSizeL += l;
        setString();
    }

    public long getAktSize() {
        return aktSizeL;
    }

    public void setString() {
        if (aktSizeL <= 0) {
            if (sizeL != 0) {
                sizeStr = getGroesse(sizeL);
            } else {
                sizeStr = "";
            }
        } else {
            sizeStr = getGroesse(aktSizeL) + " von " + getGroesse(sizeL);
        }
    }

    private String getGroesse(long l) {
        String ret = "";
        if (l > 1000 * 1000) {
            // größer als 1MB sonst kann ich mirs sparen
            ret = String.valueOf(l / (1000 * 1000));
        } else if (l > 0) {
            ret = "1";
        }
        return ret;
    }
}
