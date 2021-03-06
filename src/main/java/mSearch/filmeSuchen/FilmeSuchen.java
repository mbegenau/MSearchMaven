/*
 * MediathekView
 * Copyright (C) 2008 W. Xaver
 * W.Xaver[at]googlemail.com
 * http://zdfmediathk.sourceforge.net/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package mSearch.filmeSuchen;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import javax.swing.event.EventListenerList;
import mSearch.daten.ListeFilme;
import mSearch.filmeSuchen.sender.Mediathek3Sat;
import mSearch.filmeSuchen.sender.MediathekArd;
import mSearch.filmeSuchen.sender.MediathekArte_de;
import mSearch.filmeSuchen.sender.MediathekArte_fr;
import mSearch.filmeSuchen.sender.MediathekBr;
import mSearch.filmeSuchen.sender.MediathekDw;
import mSearch.filmeSuchen.sender.MediathekHr;
import mSearch.filmeSuchen.sender.MediathekKika;
import mSearch.filmeSuchen.sender.MediathekMdr;
import mSearch.filmeSuchen.sender.MediathekNdr;
import mSearch.filmeSuchen.sender.MediathekOrf;
import mSearch.filmeSuchen.sender.MediathekPhoenix;
import mSearch.filmeSuchen.sender.MediathekRbb;
import mSearch.filmeSuchen.sender.MediathekReader;
import mSearch.filmeSuchen.sender.MediathekSr;
import mSearch.filmeSuchen.sender.MediathekSrf;
import mSearch.filmeSuchen.sender.MediathekSrfPod;
import mSearch.filmeSuchen.sender.MediathekSwr;
import mSearch.filmeSuchen.sender.MediathekWdr;
import mSearch.filmeSuchen.sender.MediathekZdf;
import mSearch.filmeSuchen.sender.MediathekZdfTivi;
import mSearch.tool.GermanStringSorter;
import mSearch.Config;
import mSearch.tool.Log;

/**
 * ###########################################################################################################
 * Ablauf:
 * die gefundenen Filme kommen in die "listeFilme"
 * -> bei einem vollen Suchlauf: passiert nichts weiter
 * -> bei einem Update: "listeFilme" mit alter Filmliste auffüllen, URLs die es schon gibt werden verworfen
 * "listeFilme" ist dann die neue komplette Liste mit Filmen
 * ##########################################################################################################
 */
public class FilmeSuchen {

    public ListeFilme listeFilmeNeu; // neu angelegte Liste und da kommen die neu gesuchten Filme rein
    public ListeFilme listeFilmeAlt; // ist die "alte" Liste, wird beim Aufruf übergeben und enthält am Ende das Ergebnis
    // private
    private final LinkedList<MediathekReader> mediathekListe = new LinkedList<>();
    private final EventListenerList listeners = new EventListenerList();
    public static final ListeRunSender listeSenderLaufen = new ListeRunSender();
    private Date startZeit = new Date();
    private Date stopZeit = new Date();
    private final static String TRENNER = " | ";
    private boolean allStarted = false;
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    public FilmeSuchen() {
        //Reader laden Spaltenweises Laden
        mediathekListe.add(new MediathekArd(this, 0));
        mediathekListe.add(new MediathekZdf(this, 0));
        mediathekListe.add(new MediathekZdfTivi(this, 0));
        mediathekListe.add(new MediathekArte_de(this, 0));
        mediathekListe.add(new MediathekArte_fr(this, 0));
        mediathekListe.add(new Mediathek3Sat(this, 1));
        mediathekListe.add(new MediathekSwr(this, 0));
        mediathekListe.add(new MediathekNdr(this, 1));
        mediathekListe.add(new MediathekKika(this, 0));
        mediathekListe.add(new MediathekDw(this, 0));
        // Spalte 2
        mediathekListe.add(new MediathekMdr(this, 0));
        mediathekListe.add(new MediathekWdr(this, 1));
        mediathekListe.add(new MediathekHr(this, 0));
        mediathekListe.add(new MediathekRbb(this, 1));
        mediathekListe.add(new MediathekSr(this, 1));
        mediathekListe.add(new MediathekBr(this, 0));
        mediathekListe.add(new MediathekSrf(this, 1));
        mediathekListe.add(new MediathekSrfPod(this, 0));
        mediathekListe.add(new MediathekOrf(this, 1));
        mediathekListe.add(new MediathekPhoenix(this, 1));
    }

    public static String[] getNamenSender() {
        // liefert eine Array mit allen Sendernamen
        LinkedList<String> liste = new LinkedList<>();
        liste.add(MediathekArd.SENDERNAME);
        liste.add(MediathekZdf.SENDERNAME);
        liste.add(MediathekZdfTivi.SENDERNAME);
        liste.add(MediathekArte_de.SENDERNAME);
        liste.add(MediathekArte_fr.SENDERNAME);
        liste.add(Mediathek3Sat.SENDERNAME);
        liste.add(MediathekSwr.SENDERNAME);
        liste.add(MediathekNdr.SENDERNAME);
        liste.add(MediathekKika.SENDERNAME);
        liste.add(MediathekDw.SENDERNAME);
        // Spalte 2
        liste.add(MediathekMdr.SENDERNAME);
        liste.add(MediathekWdr.SENDERNAME);
        liste.add(MediathekHr.SENDERNAME);
        liste.add(MediathekRbb.SENDERNAME);
        liste.add(MediathekSr.SENDERNAME);
        liste.add(MediathekBr.SENDERNAME);
        liste.add(MediathekSrf.SENDERNAME);
        liste.add(MediathekSrfPod.SENDERNAME);
        liste.add(MediathekOrf.SENDERNAME);
        liste.add(MediathekPhoenix.SENDERNAME);

        GermanStringSorter sorter = GermanStringSorter.getInstance();
        Collections.sort(liste, sorter);
        return liste.toArray(new String[liste.size()]);
    }

    public void addAdListener(ListenerFilmeLaden listener) {
        listeners.add(ListenerFilmeLaden.class, listener);
    }

    /**
     * es werden alle Filme gesucht
     *
     * @param listeFilme
     */
    public synchronized void filmeBeimSenderLaden(ListeFilme listeFilme) {
        initStart(listeFilme);
        // die mReader nach Prio starten
        mrStarten(0);
        if (!Config.getStop()) {
            mrWarten();
            mrStarten(1);
            allStarted = true;
        }
    }

    /**
     * es werden nur einige Sender aktualisiert
     *
     * @param nameSender
     * @param listeFilme
     */
    public void updateSender(String[] nameSender, ListeFilme listeFilme) {
        // nur für den Mauskontext "Sender aktualisieren"
        boolean starten = false;
        initStart(listeFilme);
        for (MediathekReader reader : mediathekListe) {
            for (String s : nameSender) {
                if (reader.checkNameSenderFilmliste(s)) {
                    starten = true;
                    new Thread(reader).start();
                }
            }
        }
        allStarted = true;
        if (!starten) {
            // dann fertig
            meldenFertig("");
        }
    }

    public synchronized RunSender melden(String sender, int max, int progress, String text) {
        RunSender runSender = listeSenderLaufen.getSender(sender);
        if (runSender != null) {
            runSender.max = max;
            runSender.progress = progress;
        } else {
            // Sender startet
            runSender = new RunSender(sender, max, progress);
            listeSenderLaufen.add(runSender);
            //wird beim Start des Senders aufgerufen, 1x
            if (listeSenderLaufen.size() <= 1 /* erster Aufruf */) {
                notifyStart(new ListenerFilmeLadenEvent(sender, text, listeSenderLaufen.getMax(), listeSenderLaufen.getProgress(), listeFilmeNeu.size(), false));
            }
        }
        notifyProgress(new ListenerFilmeLadenEvent(sender, text, listeSenderLaufen.getMax(), listeSenderLaufen.getProgress(), listeFilmeNeu.size(), false));
        progressBar();
        return runSender;
    }

    public synchronized void meldenFertig(String sender) {
        //wird ausgeführt wenn Sender beendet ist
        String zeile;
        RunSender run = listeSenderLaufen.senderFertig(sender);
        if (run != null) {
            zeile = "" + "\n";
            zeile += "-------------------------------------------------------------------------------------" + "\n";
            zeile += "Fertig " + sender + ": " + new SimpleDateFormat("HH:mm:ss").format(new Date()) + " Uhr, Filme: " + listeSenderLaufen.get(sender, RunSender.Count.FILME) + "\n";
            int sekunden = run.getLaufzeitSekunden();
            zeile += "     -> Dauer[Min]: " + (sekunden / 60 == 0 ? "<1" : sekunden / 60) + "\n";
            zeile += "     ->       Rest: " + listeSenderLaufen.getSenderRun() + "\n";
            zeile += "-------------------------------------------------------------------------------------" + "\n";
            Log.sysLog(zeile);
        }
        if (!allStarted || !listeSenderLaufen.listeFertig()) {
            //nur ein Sender fertig oder noch nicht alle gestartet
            notifyProgress(new ListenerFilmeLadenEvent(sender, "", listeSenderLaufen.getMax(), listeSenderLaufen.getProgress(), listeFilmeNeu.size(), false));
        } else {
            // alles fertig
            // wird einmal aufgerufen, wenn alle Sender fertig sind
            Log.progress(""); // zum löschen der Progressbar
            if (Config.getStop()) {
                // Abbruch melden
                Log.sysLog("                                                                                     ");
                Log.sysLog("                                                                                     ");
                Log.sysLog("*************************************************************************************");
                Log.sysLog("*************************************************************************************");
                Log.sysLog("*************************************************************************************");
                Log.sysLog("     ----- Abbruch -----                                                             ");
                Log.sysLog("*************************************************************************************");
                Log.sysLog("*************************************************************************************");
                Log.sysLog("*************************************************************************************");
                Log.sysLog("                                                                                     ");
                Log.sysLog("                                                                                     ");
            }
            mrClear();
            if (Config.updateFilmliste) {
                // alte Filme eintragen wenn angefordert oder nur ein update gesucht wurde
                //////toDo
                listeFilmeNeu.updateListe(listeFilmeAlt, true /* über den Index vergleichen */, false /*ersetzen*/);
            }
            listeFilmeNeu.sort();
            // FilmlisteMetaDaten
            stopZeit = new Date(System.currentTimeMillis());
            listeFilmeNeu.writeMetaData();

            endeMeldung().forEach(Log::sysLog);

            notifyFertig(new ListenerFilmeLadenEvent(sender, "", listeSenderLaufen.getMax(), listeSenderLaufen.getProgress(), (int) listeSenderLaufen.get(RunSender.Count.FILME), false));
        }
    }

    public ArrayList<String> endeMeldung() {
        // wird einmal aufgerufen, wenn alle Sender fertig sind
        ArrayList<String> retArray = new ArrayList<>();
        // Sender ===============================================
        // ======================================================
        retArray.add("");
        retArray.add("");
        retArray.add("=================================================================================");
        retArray.add("==  Sender  =====================================================================");
        retArray.add("");
        listeSenderLaufen.getTextSum(retArray);
        listeSenderLaufen.getTextCount(retArray);

        // Gesamt ===============================================
        // ======================================================
        int sekunden = getDauerSekunden();
        retArray.add("");
        retArray.add("=================================================================================");
        retArray.add("=================================================================================");
        retArray.add("");
        retArray.add("       Filme geladen: " + listeSenderLaufen.get(RunSender.Count.FILME));
        retArray.add("      Seiten geladen: " + listeSenderLaufen.get(RunSender.Count.ANZAHL));

        retArray.add("   Summe geladen[MB]: " + RunSender.getStringZaehler(listeSenderLaufen.get(RunSender.Count.SUM_DATA_BYTE)));
        retArray.add("        Traffic [MB]: " + RunSender.getStringZaehler(listeSenderLaufen.get(RunSender.Count.SUM_TRAFFIC_BYTE)));

        // Durchschnittswerte ausgeben
        double doub = (1.0 * listeSenderLaufen.get(RunSender.Count.SUM_TRAFFIC_BYTE)) / (sekunden == 0 ? 1 : sekunden) / 1000;
        String rate = doub < 1 ? "<1" : String.format("%.1f", (doub));
        retArray.add("    ->    Rate[kB/s]: " + rate);
        retArray.add("    ->    Dauer[Min]: " + (sekunden / 60 == 0 ? "<1" : sekunden / 60));
        retArray.add("           ->  Start: " + sdf.format(startZeit));
        retArray.add("           ->   Ende: " + sdf.format(stopZeit));
        retArray.add("");
        retArray.add("=================================================================================");
        retArray.add("=================================================================================");
        return retArray;
    }

    private synchronized void mrStarten(int prio) {
        mediathekListe.stream().filter(mr -> mr.getStartPrio() == prio).forEach(mr -> new Thread(mr).start());
    }

    private synchronized void mrClear() {
        //die MediathekReader aufräumen
        mediathekListe.forEach(MediathekReader::clear);
    }

    private synchronized void mrWarten() {
        // 4 Minuten warten, alle 10 Sekunden auf STOP prüfen
        try {
            for (int i = 0; i < 4 * 60; ++i) {
                if (Config.getStop()) {
                    break;
                }
                this.wait(1000); // warten, Sender nach der Gesamtlaufzeit starten
            }
        } catch (Exception ex) {
            Log.errorLog(978754213, ex);
        }
    }

    private int getDauerSekunden() {
        int sekunden;
        try {
            sekunden = Math.round((stopZeit.getTime() - startZeit.getTime()) / (1000));
        } catch (Exception ex) {
            sekunden = 1;
        }
        if (sekunden <= 0) {
            sekunden = 1;
        }
        return sekunden;
    }

    private void initStart(ListeFilme listeFilme) {
        listeSenderLaufen.clear();
        allStarted = false;
        listeFilmeAlt = listeFilme;
        Config.setStop(false);
        startZeit = new Date(System.currentTimeMillis());
        listeFilmeNeu = new ListeFilme();
        listeFilmeNeu.liveStreamEintragen();
        Log.sysLog("");
        Log.sysLog("=======================================");
        Log.sysLog("Start Filme laden:");
        if (Config.loadMax()) {
            Log.sysLog("Filme laden: max");
        } else if (Config.loadLongMax()) {
            Log.sysLog("Filme laden: long");
        } else {
            Log.sysLog("Filme laden: short");
        }
        if (Config.updateFilmliste) {
            Log.sysLog("Filmliste: aktualisieren");
        } else {
            Log.sysLog("Filmliste: neue erstellen");
        }
        Log.sysLog("=======================================");
        Log.sysLog("");
    }

    private void progressBar() {
        int max = listeSenderLaufen.getMax();
        int progress = listeSenderLaufen.getProgress();
        int proz = 0;
        String text;
        int sekunden = 0;
        try {
            sekunden = Math.round((new Date(System.currentTimeMillis()).getTime() - startZeit.getTime()) / (1000));
        } catch (Exception ignored) {
        }

        if (max != 0) {
            if (progress != 0) {
                proz = progress * 100 / max;
            }
            if (max > 0 && proz == 100) {
                proz = 99;
            }
            text = "  [ ";
            int a = proz / 10;
            for (int i = 0; i < a; ++i) {
                text += "#";
            }
            for (int i = 0; i < (10 - a); ++i) {
                text += "-";
            }
            text += " ]  " + listeSenderLaufen.get(RunSender.Count.ANZAHL) + " Seiten / "
                    + proz + "% von " + max + " Themen / Filme: " + listeSenderLaufen.get(RunSender.Count.FILME)
                    + " / Dauer[Min]: " + (sekunden / 60 == 0 ? "<1" : sekunden / 60)
                    + " / R-Sender: " + listeSenderLaufen.getAnzSenderRun();
            Log.progress(text);
        }
    }

    private void notifyStart(ListenerFilmeLadenEvent event) {
        for (Object l : listeners.getListenerList()) {
            if (l instanceof ListenerFilmeLaden) {
                ((ListenerFilmeLaden) l).start(event);
            }
        }
    }

    private void notifyProgress(ListenerFilmeLadenEvent event) {
        for (Object l : listeners.getListenerList()) {
            if (l instanceof ListenerFilmeLaden) {
                ((ListenerFilmeLaden) l).progress(event);
            }
        }
    }

    private void notifyFertig(ListenerFilmeLadenEvent event) {
        for (Object l : listeners.getListenerList()) {
            if (l instanceof ListenerFilmeLaden) {
                ((ListenerFilmeLaden) l).fertig(event);
            }
        }
    }
}
