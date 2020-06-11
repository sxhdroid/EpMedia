package xsh.ijkplaycmd.api;

import android.support.annotation.Keep;

import xsh.ijkplaycmd.core.EpVideo;

/**
 * @author xiaosh
 * @date 2020-06-11 15:09
 */
@Keep
public final class VideoWrapper extends EpVideo {

    public VideoWrapper(String videoPath) {
        super(videoPath);
    }
}
