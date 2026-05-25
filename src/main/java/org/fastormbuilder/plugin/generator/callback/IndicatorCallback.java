package org.fastormbuilder.plugin.generator.callback;

import com.intellij.openapi.progress.ProgressIndicator;
import org.mybatis.generator.api.ProgressCallback;

public class IndicatorCallback implements ProgressCallback {
    private final ProgressIndicator indicator;

    public IndicatorCallback(ProgressIndicator indicator) {
        this.indicator = indicator;
    }

    @Override public void introspectionStarted(int totalTasks) { indicator.setIndeterminate(false); }
    @Override public void generationStarted(int totalTasks) {}
    @Override public void saveStarted(int totalTasks) {}
    @Override public void startTask(String taskName) { indicator.setText(taskName); }
    @Override public void done() { indicator.setFraction(1.0); }
    @Override public void checkCancel() throws InterruptedException {
        if (indicator.isCanceled()) throw new InterruptedException("Cancelled");
    }
}
