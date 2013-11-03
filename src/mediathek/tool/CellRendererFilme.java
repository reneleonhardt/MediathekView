/*    
 *    MediathekView
 *    Copyright (C) 2008   W. Xaver
 *    W.Xaver[at]googlemail.com
 *    http://zdfmediathk.sourceforge.net/
 *    
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package mediathek.tool;

import java.awt.Component;
import java.awt.Font;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import mediathek.controller.History;
import mediathek.controller.starter.Start;
import mediathek.daten.Daten;
import msearch.daten.DatenFilm;
import msearch.daten.ListeFilme;

public class CellRendererFilme extends DefaultTableCellRenderer {

    private Daten ddaten;
    private History history = null;

    public CellRendererFilme(Daten d) {
        ddaten = d;
        history = ddaten.history;
    }

    @Override
    public Component getTableCellRendererComponent(
            JTable table,
            Object value,
            boolean isSelected,
            boolean hasFocus,
            int row,
            int column) {
        try {
            setBackground(null);
            setForeground(null);
            setFont(null);
            setHorizontalAlignment(SwingConstants.LEADING);
            super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);
            int r = table.convertRowIndexToModel(row);
            int c = table.convertColumnIndexToModel(column);
            String url = table.getModel().getValueAt(r, DatenFilm.FILM_URL_NR).toString();
            boolean live = table.getModel().getValueAt(r, DatenFilm.FILM_THEMA_NR).equals(ListeFilme.THEMA_LIVE);
            boolean start = false;
            Start s = Daten.listeDownloads.getStartOrgUrl(url);
            if (c == DatenFilm.FILM_GROESSE_NR || c == DatenFilm.FILM_DATUM_NR || c == DatenFilm.FILM_ZEIT_NR || c == DatenFilm.FILM_DAUER_NR) {
                setHorizontalAlignment(SwingConstants.CENTER);
            }
            if (c == DatenFilm.FILM_GROESSE_NR) {
                setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 10));
                setHorizontalAlignment(SwingConstants.RIGHT);
            }
            if (s != null) {
                if (s.datenDownload.getQuelle() == Start.QUELLE_BUTTON) {
                    start = true;
                    switch (s.status) {
                        case Start.STATUS_INIT:
                            if (isSelected) {
                                setBackground(GuiKonstanten.DOWNLOAD_FARBE_WAIT_SEL);
                            } else {
                                setBackground(GuiKonstanten.DOWNLOAD_FARBE_WAIT);
                            }
                            break;
                        case Start.STATUS_RUN:
                            if (isSelected) {
                                setBackground(GuiKonstanten.DOWNLOAD_FARBE_RUN_SEL);
                            } else {
                                setBackground(GuiKonstanten.DOWNLOAD_FARBE_RUN);
                            }
                            break;
                        case Start.STATUS_FERTIG:
                            if (isSelected) {
                                setBackground(GuiKonstanten.DOWNLOAD_FARBE_FERTIG_SEL);
                            } else {
                                setBackground(GuiKonstanten.DOWNLOAD_FARBE_FERTIG);
                            }
                            break;
                        case Start.STATUS_ERR:
                            if (isSelected) {
                                setBackground(GuiKonstanten.DOWNLOAD_FARBE_ERR_SEL);
                            } else {
                                setBackground(GuiKonstanten.DOWNLOAD_FARBE_ERR);
                            }
                            break;
                    }
                }
            }
            if (!start) {
                if (!live) {
                    // bei livestreams keine History anzeigen
                    if (history.contains(table.getModel().getValueAt(r, DatenFilm.FILM_URL_NR).toString())) {
                        if (isSelected) {
                            setBackground(GuiKonstanten.FARBE_GRAU_SEL);
                        } else {
                            setBackground(GuiKonstanten.FARBE_GRAU);
                        }
                    }
                } else {
                    setFont(new java.awt.Font("Dialog", Font.BOLD, 12));
                    setForeground(GuiKonstanten.DOWNLOAD_FARBE_LIVE);
                }
            }
        } catch (Exception ex) {
            Log.fehlerMeldung(630098552, Log.FEHLER_ART_PROG, this.getClass().getName(), ex);
        }
        return this;
    }
}
