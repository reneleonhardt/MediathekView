package mediathek.tool.table;

import mediathek.config.MVConfig;
import mediathek.daten.DatenFilm;
import mediathek.gui.tabs.tab_film.GuiFilme;
import mediathek.tool.FilmSize;
import mediathek.tool.models.TModelFilm;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class MVFilmTable extends MVTable {
    private static final Logger logger = LogManager.getLogger();
    /**
     * Stores the selected FILM NUMBERS that need to be restored after updates.
     */
    private final List<Integer> selectedFilmNumbers = new ArrayList<>();
    private MyRowSorter<TableModel> sorter;

    @Override public String getToolTipText(MouseEvent e) {
        var p = e.getPoint(); // MouseEvent
        final int viewColumn = columnAtPoint(p);
        final int modelColumnIndex = convertColumnIndexToModel(viewColumn);

        //only show title as tooltip for TITEL column...
        if (modelColumnIndex != DatenFilm.FILM_TITEL)
            return super.getToolTipText(e);

        String toolTipText = null;
        final int viewRow = rowAtPoint(p);
        var comp = prepareRenderer(getCellRenderer(viewRow, viewColumn), viewRow, viewColumn);
        var bounds = getCellRect(viewRow, viewColumn, false);


        try {
            //comment row, exclude heading
            if (comp.getPreferredSize().width > bounds.width) {
                final int modelRowIndex = convertRowIndexToModel(viewRow);
                final DatenFilm datenFilm = (DatenFilm) getModel().getValueAt(modelRowIndex, DatenFilm.FILM_REF);

                toolTipText = datenFilm.getTitle();
            }
        } catch (RuntimeException ignored) {
            //catch null pointer exception if mouse is over an empty line
        }

        return toolTipText;
    }


    public MVFilmTable() {
        super(DatenFilm.MAX_ELEM, GuiFilme.VISIBLE_COLUMNS,
                Optional.of(MVConfig.Configs.SYSTEM_TAB_FILME_ICON_ANZEIGEN),
                Optional.of(MVConfig.Configs.SYSTEM_TAB_FILME_ICON_KLEIN),
                Optional.of(MVConfig.Configs.SYSTEM_EIGENSCHAFTEN_TABELLE_FILME));

        setAutoCreateRowSorter(false);
        addPropertyChangeListener("model", evt -> {
            //we need to setup sorter later as the model is invalid at ctor point...
            var model = (TableModel) evt.getNewValue();
            if (sorter == null) {
                sorter = new MyRowSorter<>(model);
                sorter.setModel(model);
                setRowSorter(sorter);
            }
            else
                sorter.setModel(model);
        });
    }

    private void resetFilmeTab(int i) {
        //logger.debug("resetFilmeTab()");

        reihe[i] = i;
        breite[i] = 200;
        switch (i) {
            case DatenFilm.FILM_NR -> breite[i] = 75;
            case DatenFilm.FILM_TITEL -> breite[i] = 300;
            case DatenFilm.FILM_DATUM, DatenFilm.FILM_ZEIT, DatenFilm.FILM_SENDER, DatenFilm.FILM_GROESSE,
                    DatenFilm.FILM_DAUER, DatenFilm.FILM_GEO -> breite[i] = 100;
            case DatenFilm.FILM_URL -> breite[i] = 500;
            case DatenFilm.FILM_ABSPIELEN, DatenFilm.FILM_AUFZEICHNEN, DatenFilm.FILM_MERKEN -> breite[i] = 20;
            case DatenFilm.FILM_HD, DatenFilm.FILM_UT -> breite[i] = 50;
        }
    }

    @Override
    public void resetTabelle() {
        //logger.debug("resetTabelle()");

        for (int i = 0; i < maxSpalten; ++i) {
            resetFilmeTab(i);
        }

        getRowSorter().setSortKeys(null); // empty sort keys
        spaltenAusschalten();
        setSpaltenEinAus(breite);
        setSpalten();
        calculateRowHeight();
    }

    @Override
    protected void spaltenAusschalten() {
        // do nothing here
    }

    @Override
    public void getSpalten() {
        //logger.debug("getSpalten()");

        // Einstellungen der Tabelle merken
        saveSelectedTableRows();

        for (int i = 0; i < reihe.length && i < getModel().getColumnCount(); ++i) {
            reihe[i] = convertColumnIndexToModel(i);
        }

        for (int i = 0; i < breite.length && i < getModel().getColumnCount(); ++i) {
            breite[i] = getColumnModel().getColumn(convertColumnIndexToView(i)).getWidth();
        }

        // save sortKeys
        var rowSorter = getRowSorter();
        if (rowSorter != null) {
            listeSortKeys = rowSorter.getSortKeys();
        } else {
            listeSortKeys = null;
        }
    }

    private void reorderColumns()
    {
        final TableColumnModel model = getColumnModel();
        var numCols = getColumnCount();
        for (int i = 0; i < reihe.length && i < numCols; ++i) {
            //move only when there are changes...
            if (reihe[i] != i)
                model.moveColumn(convertColumnIndexToView(reihe[i]), i);
        }
    }

    private void restoreSortKeys()
    {
        if (listeSortKeys != null) {
            var rowSorter = getRowSorter();
            var tblSortKeys = rowSorter.getSortKeys();
            if (!(listeSortKeys == tblSortKeys)) {
                if (!listeSortKeys.isEmpty()) {
                    rowSorter.setSortKeys(listeSortKeys);
                }
            }
        }
    }

    /**
     * Setzt die gemerkte Position der Spalten in der Tabelle wieder.
     * Ziemlich ineffizient!
     */
    @Override
    public void setSpalten() {
        //logger.debug("setSpalten()");
        try {
            changeInternalColumnWidths();

            changeTableModelColumnWidths();

            reorderColumns();

            restoreSortKeys();

            restoreSelectedTableRows();

            validate();
        } catch (Exception ex) {
            logger.error("setSpalten", ex);
        }
    }

    /**
     * Scroll to a selected row, but center it in the middle of the visible rectangle.
     * @param rowIndex The row to be displayed centered.
     */
    private void scrollToSelectionCentered(int rowIndex) {
        Rectangle rect = getCellRect(rowIndex, 0, true);
        Rectangle viewRect = getVisibleRect();
        rect.setLocation(rect.x - viewRect.x, rect.y - viewRect.y);

        int centerX = (viewRect.width - rect.width) / 2;
        int centerY = (viewRect.height - rect.height) / 2;
        if (rect.x < centerX) {
            centerX = -centerX;
        }
        if (rect.y < centerY) {
            centerY = -centerY;
        }
        rect.translate(centerX, centerY);
        scrollRectToVisible(rect);
    }

    @Override
    protected void scrollToIndexDelegate(int index) {
        // film list moves selected entries into center of JTable view
        scrollToSelectionCentered(index);
    }

    @Override
    protected void restoreSelectedTableRows() {
        if (!selectedFilmNumbers.isEmpty()) {
            boolean found = false;

            selectionModel.setValueIsAdjusting(true);
            final var tModel = (TModelFilm) getModel();
            for (var i : selectedFilmNumbers) {
                int r = tModel.getModelRowForFilmNumber(i);
                if (r >= 0) {
                    // ansonsten gibts die Zeile nicht mehr
                    r = convertRowIndexToView(r);
                    addRowSelectionInterval(r, r);
                    found = true;
                }
            }
            if (!found && selRow >= 0 && getRowCount() > selRow) {
                setRowSelectionInterval(selRow,selRow);
            } else if (!found && selRow >= 0 && getRowCount() > 0) {
                final var rowCount = tModel.getRowCount() - 1;
                setRowSelectionInterval(rowCount, rowCount);
            }
            selectionModel.setValueIsAdjusting(false);
        }

        selectedFilmNumbers.clear();
    }

    @Override
    public void saveSelectedTableRows() {
        super.saveSelectedTableRows();

        int selIndex = -1;
        if (selRow >= 0) {
            selIndex = (int) getModel().getValueAt(convertRowIndexToModel(selRow), DatenFilm.FILM_NR);
        }

        selectedFilmNumbers.clear();
        if (selIndex >= 0) {
            for (int i : selRows) {
                selectedFilmNumbers.add((int) getModel().getValueAt(convertRowIndexToModel(i), DatenFilm.FILM_NR));
            }
        }
    }

    static class MyRowSorter<M extends TableModel> extends TableRowSorter<M> {
        public MyRowSorter(M model) {
            super(model);
        }

        @Override
        public void setModel(M model) {
            super.setModel(model);

            //must be set after each model change
            // do not sort buttons
            setSortable(DatenFilm.FILM_ABSPIELEN, false);
            setSortable(DatenFilm.FILM_AUFZEICHNEN, false);
            setSortable(DatenFilm.FILM_URL, false);
            //compare to FilmSize->int instead of String
            setComparator(DatenFilm.FILM_GROESSE, (Comparator<FilmSize>) FilmSize::compareTo);
            // deactivate german collator used in DatenFilm as it slows down sorting as hell...
            setComparator(DatenFilm.FILM_SENDER, (Comparator<String>) String::compareTo);
            setComparator(DatenFilm.FILM_ZEIT, (Comparator<String>) String::compareTo);
            //setComparator(DatenFilm.FILM_URL, (Comparator<String>) String::compareTo);
            setComparator(DatenFilm.FILM_DAUER, Comparator.naturalOrder());
        }

        @Override
        public void setSortKeys(List<? extends SortKey> sortKeys) {
            // MV config stores only ONE sort key
            // here we make sure that only one will be set on the table...
            if (sortKeys != null) {
                while (sortKeys.size() > 1)
                    sortKeys.remove(1);
            }
            super.setSortKeys(sortKeys);
        }
    }
}
