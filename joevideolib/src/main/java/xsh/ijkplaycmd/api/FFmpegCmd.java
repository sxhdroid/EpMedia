package xsh.ijkplaycmd.api;

import android.support.annotation.Keep;

/**
 * Created by 杨杰 on 2017/2/14.
 */
@Keep
public class FFmpegCmd {
	/**
	 * 加载所有相关链接库
	 */
	static {
		System.loadLibrary("ffmpegCmd");
	}

	private static OnEditorListener listener;
	/** 单位为秒 */
	private static int duration;
	private static boolean isFirstUpdateProgress;

	/**
	 * 调用底层执行
	 *
	 * @param argc
	 * @param argv
	 * @return
	 */
	@Keep
	public static native int exec(int argc, String[] argv);

	@Keep
	public static native void exit();

	@Keep
	public static void onExecuted(int ret) {
		if (listener != null) {
			if (ret == 0) {
				listener.onProgress(1);
				listener.onSuccess();
			} else {
				listener.onFailure();
			}
		}
	}

	@Keep
	public static void onProgress(float progress) {
		if (listener != null) {
			if (duration != 0) {
				if (isFirstUpdateProgress) {
					isFirstUpdateProgress = false;
					listener.onProgress(0);
				} else {
					listener.onProgress(progress / duration * 0.95f);
				}
			}
		}
	}


	/**
	 * 执行ffmoeg命令
	 *
	 * @param cmds
	 * @param duration 持续时间， 单位为微秒
	 * @param listener
	 */
	@Keep
	public static void exec(String[] cmds, long duration, OnEditorListener listener) {
		FFmpegCmd.listener = listener;
		FFmpegCmd.duration = (int) (duration / 1000000);
		exec(cmds.length, cmds);
		isFirstUpdateProgress = true;
	}
}
