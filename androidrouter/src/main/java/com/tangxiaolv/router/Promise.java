
package com.tangxiaolv.router;

import android.text.TextUtils;
import android.widget.Toast;

import com.tangxiaolv.router.exceptions.RouterException;
import com.tangxiaolv.router.utils.PromiseTimer;
import com.tangxiaolv.router.utils.ReflectTool;

/**
 * Manage router send and receive.
 */
class Promise {

    /**
     * Call on main thread.{@link Promise#call(Resolve, Reject)}
     */
    static final int FLAG_CALL_MAIN = 1 << 1;

    /**
     * Call on thread.{@link Promise#call(Resolve, Reject)}
     */
    static final int FLAG_CALL_THREAD = 1 << 2;

    /**
     * return on main thread.
     *
     * {@link Promise#resolve(String, Object)}
     * {@link Promise#reject(Exception)}}
     */
    static final int FLAG_RETURN_MIAN = 1 << 3;

    /**
     * return on thread.
     *
     * {@link Promise#resolve(String, Object)}
     * {@link Promise#reject(Exception)}}
     */
    static final int FLAG_RETURN_THREAD = 1 << 4;

    private final Asker asker;
    private final VPromise mVPromise;
    private Resolve resolve;
    private Reject reject;
    private String tag;
    private PromiseTimer timer;
    private int flagMark = 0;

    Promise(Asker asker) {
        this.asker = asker;
        this.mVPromise = new VPromise(this);
        if (asker != null) asker.setPromise(this);
    }

    /**
     * No want to receive.
     */
    void call() {
        call(null, null);
    }

    /**
     * Send router.Only receive success.
     *
     * @param resolve {@link Promise}
     */
    void call(Resolve resolve) {
        call(resolve, null);
    }

    /**
     * Send router.Only receive fail.
     *
     * @param reject {@link Promise}
     */
    void call(Reject reject) {
        call(null, reject);
    }

    /**
     * Send router. Receive success and fail.
     *
     * @param resolve {@link Promise}
     */
    void call(Resolve resolve, Reject reject) {
        this.resolve = resolve;
        this.reject = reject;

        //call on main thread
        if ((flagMark & FLAG_CALL_MAIN) != 0) {
            RouterHelper.HANDLER.post(new Runnable() {
                @Override
                public void run() {
                    asker.request();
                }
            });
        }

        //call on thread
        else if ((flagMark & FLAG_CALL_THREAD) != 0) {
            RouterHelper.EXECUTOR.execute(new Runnable() {
                @Override
                public void run() {
                    asker.request();
                }
            });
        }

        //call on current thread
        else {
            asker.request();
        }
    }

    void resolve(final String type, final Object result) {
        showToast();
        if (resolve == null)
            return;

        //call on main thread
        if ((flagMark & FLAG_RETURN_MIAN) != 0) {
            RouterHelper.HANDLER.post(new Runnable() {
                @Override
                public void run() {
                    resolve.call(type, result);
                }
            });
        }

        //call on thread
        else if ((flagMark & FLAG_RETURN_THREAD) != 0) {
            RouterHelper.EXECUTOR.execute(new Runnable() {
                @Override
                public void run() {
                    resolve.call(type, result);
                }
            });
        }

        //call on current thread
        else {
            resolve.call(type, result);
        }
    }

    void reject(Exception e) {
        showToast();
        if (e == null)
            e = new RouterException("unkownException");
        e.printStackTrace();
        if (reject == null)
            return;

        final Exception _e = e;
        //call on main thread
        if ((flagMark & FLAG_RETURN_MIAN) != 0) {
            RouterHelper.HANDLER.post(new Runnable() {
                @Override
                public void run() {
                    reject.call(_e);
                }
            });
        }

        //call on thread
        else if ((flagMark & FLAG_RETURN_THREAD) != 0) {
            RouterHelper.EXECUTOR.execute(new Runnable() {
                @Override
                public void run() {
                    reject.call(_e);
                }
            });
        }

        //call on current thread
        else {
            reject.call(e);
        }
    }

    void showTime() {
        timer = new PromiseTimer();
    }

    private void showToast() {
        if (timer == null) return;
        RouterHelper.HANDLER.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(ReflectTool.getApplication(), timer.getTime(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    VPromise getVPromise() {
        return mVPromise;
    }

    void setThreadFlag(int flag) {
        this.flagMark |= flag;
    }

    /**
     * Used by {@link AndroidRouter#findPromiseByTag(String)}
     *
     * @return Tag of {@link Promise}
     */
    String getTag() {
        if (TextUtils.isEmpty(tag)) {
            tag = RouterHelper.getInstance().genPromiseTag();
            RouterHelper.getInstance().addToPromisePool(tag, mVPromise);
        }
        return tag;
    }
}
