package xsh.ijkplaycmd.api;

/**
 * Created by YangJie on 2017/5/18.
 */

import android.support.annotation.Keep;

/**
 * 执行完成/错误 时的回调接口
 */
@Keep
public interface OnEditorListener {

	void onSuccess();

	void onFailure();

	void onProgress(float progress);
}
