package org.fastormbuilder.plugin.action;

import org.fastormbuilder.plugin.util.TextUtils;
import com.intellij.codeInsight.hint.HintManager;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.datatransfer.*;
import java.util.*;
import java.util.List;

public class CopyExecutableSqlAction extends DumbAwareAction {
    private static final String PREPARING = "Preparing: ";
    private static final String PARAMETERS = "Parameters: ";
    private static final Set<String> QUOTE_TYPES = new HashSet<>(Arrays.asList("(String)", "(Date)", "(Time)", "(Timestamp)", "(DateTime)", "(LocalDateTime)"));

    public @NotNull ActionUpdateThread getActionUpdateThread() { return ActionUpdateThread.BGT; }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Editor editor = event.getData(CommonDataKeys.EDITOR);
        if (editor == null) return;
        String text = editor.getSelectionModel().getSelectedText();
        if (text == null || !text.contains(PREPARING) || !text.contains(PARAMETERS)) {
            HintManager.getInstance().showErrorHint(editor, "Keywords \"Preparing: \" and \"Parameters: \" are required");
            return;
        }
        String sql = resolve(text);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(sql), null);
        Notifier.getInstance().info("Executable SQL copied to clipboard", event.getProject());
    }

    public static String resolve(String text) {
        int fromIndex = 0, nextIndex;
        List<String> results = new ArrayList<>();
        while ((nextIndex = text.indexOf(PREPARING, fromIndex)) >= 0) {
            String stmt = extractLine(text, PREPARING, fromIndex);
            if (text.indexOf(PARAMETERS, nextIndex) == -1) { results.add(stmt); break; }
            String paramsText = extractLine(text, PARAMETERS, nextIndex);
            List<String> params = parseParams(paramsText);
            StringTokenizer tok = new StringTokenizer(stmt, "?");
            StringBuilder sb = new StringBuilder();
            int i = 0;
            while (tok.hasMoreTokens()) {
                sb.append(tok.nextToken());
                if (i < params.size()) sb.append(params.get(i));
                i++;
            }
            results.add(sb.toString().trim());
            fromIndex = nextIndex + PREPARING.length();
        }
        if (results.isEmpty()) return "";
        if (results.size() == 1) return results.get(0);
        return String.join(";" + System.lineSeparator(), results) + ";";
    }

    private static List<String> parseParams(String text) {
        List<String> params = new ArrayList<>();
        StringTokenizer tok = new StringTokenizer(text, ",");
        while (tok.hasMoreTokens()) {
            String p = tok.nextToken();
            int idx = p.lastIndexOf("(");
            if (idx == -1) idx = p.length();
            String type = p.substring(idx);
            String val = p.substring(0, idx).trim();
            if (QUOTE_TYPES.contains(type)) val = "'" + val + "'";
            params.add(val);
        }
        return params;
    }

    private static String extractLine(String str, String keyword, int from) {
        int start = str.indexOf(keyword, from) + keyword.length();
        int end = str.indexOf('\n', start);
        return str.substring(start, end > -1 ? end : str.length());
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        Editor editor = event.getData(CommonDataKeys.EDITOR);
        if (editor != null) {
            SelectionModel sel = editor.getSelectionModel();
            if (TextUtils.hasValue(sel.getSelectedText())) return;
        }
        event.getPresentation().setVisible(false);
    }
}
