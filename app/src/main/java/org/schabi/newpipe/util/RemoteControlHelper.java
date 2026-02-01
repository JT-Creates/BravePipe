package org.schabi.newpipe.util;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Enables Android TV remote control and keyboard drag operations on RecyclerView items.
 *
 * Usage: Press OK/Enter/Space button and hold an arrow key to move items:
 * - UP or LEFT arrow: Move item up/left in the list/grid
 * - DOWN or RIGHT arrow: Move item down/right in the list/grid
 */
public class RemoteControlHelper implements View.OnKeyListener {
    private static final String TAG = "RemoteControlHelper";

    private static final int INITIAL_REPEAT_DELAY_MS = 300;
    private static final int REPEAT_DELAY_MS = 150;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final RecyclerView recyclerView;
    private final ItemTouchHelper.Callback dragCallback;
    private final RecyclerView.OnChildAttachStateChangeListener childAttachStateChangeListener;

    private boolean okPressed = false;
    private boolean arrowPressed = false;
    private int lastArrowKey = KeyEvent.KEYCODE_UNKNOWN;
    private final Runnable repeatMoveRunnable = this::repeatMove;

    /**
     * Creates a helper that adds remote and keyboard drag support to a RecyclerView.
     *
     * @param recyclerView the RecyclerView to add remote drag to
     * @param dragCallback the ItemTouchHelper.Callback that handles moves
     */
    public RemoteControlHelper(@NonNull final RecyclerView recyclerView,
                               @NonNull final ItemTouchHelper.Callback dragCallback) {
        this.recyclerView = recyclerView;
        this.dragCallback = dragCallback;

        this.childAttachStateChangeListener = new RecyclerView.OnChildAttachStateChangeListener() {
            @Override
            public void onChildViewAttachedToWindow(@NonNull final View view) {
                view.setOnKeyListener(RemoteControlHelper.this);
            }

            @Override
            public void onChildViewDetachedFromWindow(@NonNull final View view) {
                view.setOnKeyListener(null);
            }
        };
        this.recyclerView.addOnChildAttachStateChangeListener(childAttachStateChangeListener);
    }


    /**
     * Handles arrow keys when OK/Enter/Space is pressed.
     * @param v view
     * @param event being triggered
     * @param keyCode is the code from the remote/keyboard
     */
    @Override
    public boolean onKey(@NonNull final View v, final int keyCode, @NonNull final KeyEvent event) {
        Log.d(TAG, "onKey: keyCode=" + keyCode + ", event=" + event);
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_SPACE:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
                return handleOkButton(event);

            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                return handleArrowKey(keyCode, event);

            default:
                return false;
        }
    }

    /**
     * Handles the OK/Enter/Space button press and release.
     * @param event being triggered
     * @return whether button was pressed or released
     */
    private boolean handleOkButton(@NonNull final KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
            okPressed = true;
            return true;
        } else if (event.getAction() == KeyEvent.ACTION_UP) {
            okPressed = false;
            if (arrowPressed) {
                stopRepeatingMove();
                arrowPressed = false;
            } else {
                // If OK was pressed and released without any arrow keys, perform a click
                final View focusedChild = recyclerView.getFocusedChild();
                if (focusedChild != null) {
                    focusedChild.performClick();
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Handles arrow keys when OK/Enter/Space is pressed.
     * @param event being triggered
     * @param keyCode is the code from the remote/keyboard
     * @return whether ok and directions are being pressed.
     */
    private boolean handleArrowKey(final int keyCode, @NonNull final KeyEvent event) {
        if (!okPressed) {
            return false;
        }

        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
            arrowPressed = true;
            lastArrowKey = keyCode;
            // Start moving immediately and then repeat
            performMove(keyCode);
            handler.postDelayed(repeatMoveRunnable, INITIAL_REPEAT_DELAY_MS);
            return true;
        } else if (event.getAction() == KeyEvent.ACTION_UP) {
            stopRepeatingMove();
            arrowPressed = false;
            lastArrowKey = KeyEvent.KEYCODE_UNKNOWN;
            return true;
        }
        return false;
    }

    /**
     * Performs a single move operation.
     * @param keyCode is the code from the remote/keyboard
     */
    private void performMove(final int keyCode) {
        final View focusedChild = recyclerView.getFocusedChild();
        if (focusedChild == null) {
            return;
        }

        final RecyclerView.ViewHolder currentHolder = recyclerView.getChildViewHolder(focusedChild);
        if (currentHolder == null) {
            return;
        }

        final int currentPos = currentHolder.getBindingAdapterPosition();
        final int targetPos = getTargetPos(currentPos, keyCode);

        if (targetPos == -1) {
            return;
        }

        // Check bounds
        final RecyclerView.Adapter<?> adapter = recyclerView.getAdapter();
        if (adapter == null || targetPos < 0 || targetPos >= adapter.getItemCount()) {
            return;
        }

        final RecyclerView.ViewHolder targetHolder =
                recyclerView.findViewHolderForAdapterPosition(targetPos);
        if (targetHolder == null) {
            // The view holder is not laid out, so we can't move to it
            return;
        }

        // Perform the move through the callback
        dragCallback.onMove(recyclerView, currentHolder, targetHolder);

        // Move focus to the target
        targetHolder.itemView.requestFocus();
    }

    private int getTargetPos(final int currentPos, final int keyCode) {
        final RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        if (layoutManager instanceof GridLayoutManager) {
            final GridLayoutManager gridLayoutManager = (GridLayoutManager) layoutManager;
            final int spanCount = gridLayoutManager.getSpanCount();
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_UP:
                    return currentPos - spanCount;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    return currentPos + spanCount;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    return currentPos - 1;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    return currentPos + 1;
            }
        } else if (layoutManager instanceof LinearLayoutManager) {
            final LinearLayoutManager linearLayoutManager = (LinearLayoutManager) layoutManager;
            final int orientation = linearLayoutManager.getOrientation();
            if (orientation == RecyclerView.VERTICAL) {
                switch (keyCode) {
                    case KeyEvent.KEYCODE_DPAD_UP:
                    case KeyEvent.KEYCODE_DPAD_LEFT:
                        return currentPos - 1;
                    case KeyEvent.KEYCODE_DPAD_DOWN:
                    case KeyEvent.KEYCODE_DPAD_RIGHT:
                        return currentPos + 1;
                }
            } else if (orientation == RecyclerView.HORIZONTAL) {
                switch (keyCode) {
                    case KeyEvent.KEYCODE_DPAD_LEFT:
                    case KeyEvent.KEYCODE_DPAD_UP:
                        return currentPos - 1;
                    case KeyEvent.KEYCODE_DPAD_RIGHT:
                    case KeyEvent.KEYCODE_DPAD_DOWN:
                        return currentPos + 1;
                }
            }
        }
        // Default for unknown or unsupported layout managers
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_LEFT:
                return currentPos - 1;
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                return currentPos + 1;
        }
        return -1;
    }

    /**
     * Schedules repeated moves while the arrow key is held.
     */
    private void repeatMove() {
        if (okPressed && arrowPressed && lastArrowKey != KeyEvent.KEYCODE_UNKNOWN) {
            performMove(lastArrowKey);
            handler.postDelayed(repeatMoveRunnable, REPEAT_DELAY_MS);
        }
    }

    /**
     * Stops any scheduled repeated moves.
     */
    private void stopRepeatingMove() {
        handler.removeCallbacks(repeatMoveRunnable);
    }

    /**
     * Cleans up the helper when the RecyclerView is no longer needed.
     */
    public void cleanup() {
        stopRepeatingMove();
        recyclerView.removeOnChildAttachStateChangeListener(childAttachStateChangeListener);
        for (int i = 0; i < recyclerView.getChildCount(); i++) {
            recyclerView.getChildAt(i).setOnKeyListener(null);
        }
    }
}
