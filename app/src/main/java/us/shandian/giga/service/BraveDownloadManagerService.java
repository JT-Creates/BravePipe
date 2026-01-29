package us.shandian.giga.service;

import android.app.Service;

import us.shandian.giga.preprocessing.BraveHlsPreProcessor;
import us.shandian.giga.preprocessing.BravePreprocessing;
import us.shandian.giga.get.DownloadMission;

public abstract class BraveDownloadManagerService extends Service {

    protected static final String EXTRA_SEGMENTS = "DownloadManagerService.extra.segments";

    protected void braveLaunchHlsPreProcessor(
            final DownloadMission mission,
            final DownloadManager mManager) {
        final Thread prepareHlsDownload = new Thread(() -> { // own thread as we have network I/O
            final BravePreprocessing preProcessor = new BraveHlsPreProcessor();
            preProcessor.modifyMission(mission);
            mManager.startMission(mission);
        });
        prepareHlsDownload.setName("PrepareHls");
        prepareHlsDownload.start();
    }

}
