package info.brandonharris.speedsync;

/**
 * Created by brandon on 5/22/17.
 */

public abstract class SyncCallback {
    abstract void updateSpeed(double megabitsPerSecond);

    abstract void updateProgress(long bytes);

    abstract void updateTotalSize(long bytes);
}
