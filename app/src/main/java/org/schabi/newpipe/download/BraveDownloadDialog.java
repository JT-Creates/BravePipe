package org.schabi.newpipe.download;

import android.content.Context;
import android.view.View;

import org.schabi.newpipe.databinding.DownloadDialogBinding;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.stream.Stream;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.VideoStream;
import org.schabi.newpipe.streams.io.StoredFileHelper;
import org.schabi.newpipe.util.SponsorBlockUtils;
import org.schabi.newpipe.util.VideoSegment;

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.view.menu.ActionMenuItemView;
import androidx.fragment.app.DialogFragment;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import us.shandian.giga.get.MissionRecoveryInfo;
import us.shandian.giga.service.DownloadManagerService;

import static org.schabi.newpipe.extractor.stream.DeliveryMethod.HLS;
import static org.schabi.newpipe.ktx.ViewUtils.animate;
import static org.schabi.newpipe.util.ListHelper.getStreamsOfSpecifiedDelivery;

public abstract class BraveDownloadDialog extends DialogFragment {

    private VideoSegment[] segments;
    private Disposable youtubeVideoSegmentsDisposable;

    protected List<VideoStream> braveAddHlsStreams(
            final StreamInfo info,
            final List<VideoStream> videoStreams) {
        videoStreams.addAll(getStreamsOfSpecifiedDelivery(info.getVideoStreams(), HLS));

        return videoStreams;
    }

    protected boolean braveIsHlsStream(
            final Stream selectedStream) {
        return selectedStream.getDeliveryMethod() == HLS;
    }

    @Override
    public void onDestroyView() {
        if (youtubeVideoSegmentsDisposable != null) {
            youtubeVideoSegmentsDisposable.dispose();
        }
        super.onDestroyView();
    }

    // SponsorBlock related methods
    protected void braveSponsorBlockCheckForYoutubeVideoSegments(
            final StreamInfo currentInfo,
            final ActionMenuItemView okButton,
            final DownloadDialogBinding dialogBinding) {
        // only lookup SponsorBlock for youtube
        if (currentInfo.getServiceId() != ServiceList.YouTube.getServiceId()) {
            return;
        }

        showLoading(dialogBinding);
        okButton.setEnabled(false); // disable until segments fetched
        youtubeVideoSegmentsDisposable = Single.fromCallable(() -> {
                    VideoSegment[] videoSegments = null;
                    try {
                        videoSegments = SponsorBlockUtils
                                .getYouTubeVideoSegments(getContext(), currentInfo);
                    } catch (final Exception e) {
                        // TODO: handle?
                    }

                    return videoSegments == null
                            ? new VideoSegment[0]
                            : videoSegments;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(videoSegments -> {
                    setVideoSegments(videoSegments);
                    okButton.setEnabled(true);
                    hideLoading(dialogBinding);
                });
    }

    private void setVideoSegments(final VideoSegment[] seg) {
        this.segments = seg;
    }

    private void showLoading(final DownloadDialogBinding dialogBinding) {
        dialogBinding.fileName.setVisibility(View.GONE);
        animate(dialogBinding.loadingProgressBar, true, 400);
    }

    private void hideLoading(final DownloadDialogBinding dialogBinding) {
        animate(dialogBinding.loadingProgressBar, false, 0);
        dialogBinding.fileName.setVisibility(View.VISIBLE);
    }

    protected void braveDownloadStartMissionWrapper(
            final Context context,
            final String[] urls,
            final StoredFileHelper storage,
            final char kind,
            final int threads,
            final StreamInfo streamInfo,
            final String psName,
            final String[] psArgs,
            final long nearLength,
            final ArrayList<MissionRecoveryInfo> recoveryInfo) {
        DownloadManagerService.startMission(context, urls, storage, kind, threads,
                streamInfo, psName, psArgs, nearLength, new ArrayList<>(recoveryInfo),
                this.segments);
    }
}
