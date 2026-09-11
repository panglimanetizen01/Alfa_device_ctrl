package com.alfa.device_ctrl;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.documentfile.provider.DocumentFile;

import java.util.Arrays;
import java.util.Comparator;

/** Binds the native project explorer to the real user-selected SAF tree without replacing the runtime engine. */
public final class StorageUiBridge {
    private StorageUiBridge() { }

    public static void bind(Activity activity) {
        View root = activity.findViewById(android.R.id.content);
        if (root == null) return;
        TextView header = findText(root, "PROJECT EXPLORER");
        if (header != null) {
            header.setContentDescription("Project Explorer. Tap to choose a storage directory using Android Storage Access Framework.");
            header.setOnClickListener(v -> activity.startActivity(new Intent(activity, StoragePickerActivity.class)));
        }
        refresh(activity, root);
    }

    public static void refresh(Activity activity) {
        View root = activity.findViewById(android.R.id.content);
        if (root == null) return;
        refresh(activity, root);
    }

    private static void refresh(Activity activity, View root) {
        TextView target = findRootText(root);
        if (target == null) return;
        String uriText = activity.getSharedPreferences("alfa_storage", Activity.MODE_PRIVATE).getString("tree_uri", null);
        if (uriText == null || uriText.isEmpty()) return;
        try {
            DocumentFile tree = DocumentFile.fromTreeUri(activity, Uri.parse(uriText));
            if (tree == null || !tree.canRead()) { target.setText("SAF ROOT\nREAD_BLOCKED"); return; }
            DocumentFile[] files = tree.listFiles();
            Arrays.sort(files, Comparator.comparing(DocumentFile::getName, Comparator.nullsFirst(String::compareToIgnoreCase)));
            StringBuilder out = new StringBuilder("SAF ROOT=\n").append(tree.getUri()).append("\n\n");
            int max = Math.min(300, files.length);
            for (int i = 0; i < max; i++) {
                DocumentFile f = files[i];
                out.append(f.isDirectory() ? "[D] " : "[F] ")
                        .append(f.getName() == null ? "(unnamed)" : f.getName())
                        .append(f.isDirectory() ? "/" : "  " + f.length() + " B")
                        .append('\n');
            }
            if (files.length > max) out.append("… ").append(files.length - max).append(" more\n");
            target.setText(out);
            target.setTextColor(Color.rgb(100,116,139));
        } catch (RuntimeException e) {
            target.setText("SAF ROOT\nREAD_FAILED=" + e.getClass().getSimpleName());
        }
    }

    private static TextView findText(View root, String exact) {
        if (root instanceof TextView && exact.contentEquals(((TextView) root).getText())) return (TextView) root;
        if (!(root instanceof ViewGroup)) return null;
        ViewGroup group = (ViewGroup) root;
        for (int i = 0; i < group.getChildCount(); i++) {
            TextView result = findText(group.getChildAt(i), exact);
            if (result != null) return result;
        }
        return null;
    }

    private static TextView findRootText(View root) {
        if (root instanceof TextView) {
            CharSequence text = ((TextView) root).getText();
            if (text != null && text.toString().startsWith("ROOT=")) return (TextView) root;
        }
        if (!(root instanceof ViewGroup)) return null;
        ViewGroup group = (ViewGroup) root;
        for (int i = 0; i < group.getChildCount(); i++) {
            TextView result = findRootText(group.getChildAt(i));
            if (result != null) return result;
        }
        return null;
    }
}
