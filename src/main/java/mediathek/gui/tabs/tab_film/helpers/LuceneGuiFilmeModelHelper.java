package mediathek.gui.tabs.tab_film.helpers;

import com.google.common.base.Stopwatch;
import javafx.collections.ObservableList;
import mediathek.config.Daten;
import mediathek.controller.history.SeenHistoryController;
import mediathek.daten.DatenFilm;
import mediathek.daten.IndexedFilmList;
import mediathek.gui.tabs.tab_film.GuiFilme;
import mediathek.javafx.filterpanel.FilmActionPanel;
import mediathek.javafx.filterpanel.FilmLengthSlider;
import mediathek.mainwindow.MediathekGui;
import mediathek.tool.SwingErrorDialog;
import mediathek.tool.models.TModelFilm;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LuceneGuiFilmeModelHelper {
    private static final Logger logger = LogManager.getLogger();
    private final FilmActionPanel filmActionPanel;
    private final SeenHistoryController historyController;
    private final GuiFilme.SearchField searchField;
    private boolean showNewOnly;
    private boolean showBookmarkedOnly;
    private boolean showSubtitlesOnly;
    private boolean showHqOnly;
    private boolean dontShowSeen;
    private boolean dontShowAbos;
    private boolean showLivestreamsOnly;
    private boolean dontShowTrailers;
    private boolean dontShowGebaerdensprache;
    private boolean dontShowAudioVersions;
    private long maxLength;
    private SliderRange sliderRange;

    public LuceneGuiFilmeModelHelper(@NotNull FilmActionPanel filmActionPanel,
                                     @NotNull SeenHistoryController historyController,
                                     @NotNull GuiFilme.SearchField searchField) {
        this.filmActionPanel = filmActionPanel;
        this.historyController = historyController;
        this.searchField = searchField;
    }

    private String getFilterThema() {
        String filterThema = filmActionPanel.themaBox.getSelectionModel().getSelectedItem();
        if (filterThema == null) {
            filterThema = "";
        }

        return filterThema;
    }

    private boolean noFiltersAreSet() {
        return filmActionPanel.getViewSettingsPane().senderCheckList.getCheckModel().isEmpty()
                && getFilterThema().isEmpty()
                && searchField.getText().isEmpty()
                && ((int) filmActionPanel.filmLengthSlider.getLowValue() == 0)
                && ((int) filmActionPanel.filmLengthSlider.getHighValue() == FilmLengthSlider.UNLIMITED_VALUE)
                && !filmActionPanel.dontShowAbos.getValue()
                && !filmActionPanel.showUnseenOnly.getValue()
                && !filmActionPanel.showOnlyHd.getValue()
                && !filmActionPanel.showSubtitlesOnly.getValue()
                && !filmActionPanel.showLivestreamsOnly.getValue()
                && !filmActionPanel.showNewOnly.getValue()
                && !filmActionPanel.showBookMarkedOnly.getValue()
                && !filmActionPanel.dontShowTrailers.getValue()
                && !filmActionPanel.dontShowSignLanguage.getValue()
                && !filmActionPanel.dontShowAudioVersions.getValue();
    }

    private void updateFilterVars() {
        showNewOnly = filmActionPanel.showNewOnly.getValue();
        showBookmarkedOnly = filmActionPanel.showBookMarkedOnly.getValue();
        showSubtitlesOnly = filmActionPanel.showSubtitlesOnly.getValue();
        showHqOnly = filmActionPanel.showOnlyHd.getValue();
        dontShowSeen = filmActionPanel.showUnseenOnly.getValue();
        dontShowAbos = filmActionPanel.dontShowAbos.getValue();
        showLivestreamsOnly = filmActionPanel.showLivestreamsOnly.getValue();
        dontShowTrailers = filmActionPanel.dontShowTrailers.getValue();
        dontShowGebaerdensprache = filmActionPanel.dontShowSignLanguage.getValue();
        dontShowAudioVersions = filmActionPanel.dontShowAudioVersions.getValue();
    }

    private void calculateFilmLengthSliderValues() {
        final long minLength = (long) filmActionPanel.filmLengthSlider.getLowValue();
        maxLength = (long) filmActionPanel.filmLengthSlider.getHighValue();
        var minLengthInSeconds = TimeUnit.SECONDS.convert(minLength, TimeUnit.MINUTES);
        var maxLengthInSeconds = TimeUnit.SECONDS.convert(maxLength, TimeUnit.MINUTES);
        sliderRange = new SliderRange(minLengthInSeconds, maxLengthInSeconds);
    }

    private TModelFilm performTableFiltering() {
        var listeFilme = (IndexedFilmList) Daten.getInstance().getListeFilmeNachBlackList();
        try {
            updateFilterVars();
            calculateFilmLengthSliderValues();

            if (dontShowSeen)
                historyController.prepareMemoryCache();

            String searchText = searchField.getText();
            List<DatenFilm> resultList;
            Stream<DatenFilm> stream;

            if (searchText.isEmpty()) {
                resultList = new ArrayList<>(listeFilme);
                stream = resultList.parallelStream();
            } else {
                Stopwatch watch2 = Stopwatch.createStarted();
                try (var reader = DirectoryReader.open(listeFilme.getLuceneDirectory())) {
                    Query q = new QueryParser("titel", listeFilme.getAnalyzer())
                            .parse(searchText);
                    var searcher = new IndexSearcher(reader);
                    var docs = searcher.search(q, Integer.MAX_VALUE);
                    var hits = docs.scoreDocs;

                    watch2.stop();
                    logger.trace("Lucene index search took: {}", watch2);

                    Set<Integer> filmNrSet = new HashSet<>(hits.length);
                    for (var hit : hits) {
                        var d = searcher.doc(hit.doc);
                        filmNrSet.add(Integer.parseInt(d.get("id")));
                    }
                    logger.trace("Number of found Lucene index entries: {}", filmNrSet.size());
                    stream = listeFilme.parallelStream()
                            .filter(film -> filmNrSet.contains(film.getFilmNr()));
                }
            }

            final ObservableList<String> selectedSenders = filmActionPanel.getViewSettingsPane().senderCheckList.getCheckModel().getCheckedItems();
            if (!selectedSenders.isEmpty()) {
                //ObservableList.contains() is insanely slow...this speeds up to factor 250!
                Set<String> senderSet = new HashSet<>(selectedSenders.size());
                senderSet.addAll(selectedSenders);
                stream = stream.filter(f -> senderSet.contains(f.getSender()));
            }
            if (showNewOnly)
                stream = stream.filter(DatenFilm::isNew);
            if (showBookmarkedOnly)
                stream = stream.filter(DatenFilm::isBookmarked);
            if (showLivestreamsOnly)
                stream = stream.filter(DatenFilm::isLivestream);
            if (showHqOnly)
                stream = stream.filter(DatenFilm::isHighQuality);
            if (dontShowTrailers)
                stream = stream.filter(film -> !film.isTrailerTeaser());
            if (dontShowGebaerdensprache)
                stream = stream.filter(film -> !film.isSignLanguage());
            if (dontShowAudioVersions)
                stream = stream.filter(film -> !film.isAudioVersion());
            if (dontShowAbos)
                stream = stream.filter(film -> film.getAbo() == null);
            if (showSubtitlesOnly) {
                stream = stream.filter(this::subtitleCheck);
            }

            final String filterThema = getFilterThema();
            if (!filterThema.isEmpty()) {
                stream = stream.filter(film -> film.getThema().equalsIgnoreCase(filterThema));
            }
            if (maxLength < FilmLengthSlider.UNLIMITED_VALUE) {
                stream = stream.filter(this::maxLengthCheck);
            }
            if (dontShowSeen) {
                stream = stream.filter(this::seenCheck);
            }
            //perform min length filtering after all others may have reduced the available entries...
            stream = stream.filter(this::minLengthCheck);

            resultList = stream.collect(Collectors.toList());
            logger.trace("Resulting filmlist size after all filters applied: {}", resultList.size());
            stream.close();

            //adjust initial capacity
            var filmModel = new TModelFilm(resultList.size());
            filmModel.addAll(resultList);

            resultList.clear();

            if (dontShowSeen)
                historyController.emptyMemoryCache();

            return filmModel;
        } catch (Exception ex) {
            logger.error("Lucene filtering failed!", ex);
            SwingUtilities.invokeLater(() -> {
                SwingErrorDialog.showExceptionMessage(MediathekGui.ui(),
                        "Die Lucene Abfrage ist inkorrekt und führt zu keinen Ergebnissen.", ex);
            });
            return new TModelFilm();
        }
    }

    private boolean subtitleCheck(DatenFilm film) {
        return film.hasSubtitle() || film.hasBurnedInSubtitles();
    }

    private boolean maxLengthCheck(DatenFilm film) {
        return film.getFilmLength() < sliderRange.getMaxLengthInSeconds();
    }

    private boolean seenCheck(DatenFilm film) {
        return !historyController.hasBeenSeenFromCache(film);
    }

    private boolean minLengthCheck(DatenFilm film) {
        var filmLength = film.getFilmLength();
        if (filmLength == 0)
            return true; // always show entries with length 0, which are internally "no length"
        else
            return filmLength >= sliderRange.getMinLengthInSeconds();
    }

    /**
     * Filter the filmlist.
     *
     * @return the filtered table model.
     */
    public TableModel getFilteredTableModel() {
        var listeFilme = (IndexedFilmList) Daten.getInstance().getListeFilmeNachBlackList();
        TModelFilm filmModel;

        if (!listeFilme.isEmpty()) {
            if (noFiltersAreSet()) {
                //adjust initial capacity
                filmModel = new TModelFilm(listeFilme.size());
                filmModel.addAll(listeFilme);
            } else {
                filmModel = performTableFiltering();
            }
        } else
            return new TModelFilm();

        return filmModel;
    }
}
