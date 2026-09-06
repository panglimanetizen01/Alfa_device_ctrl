package com.alfa.device_ctrl;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.DocumentsContract.Document;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Explicit Android device proof harness for the SAF tree provider.
 *
 * <p>This is not a host-visibility claim by itself. The activity creates a
 * provider-backed fixture, closes and reopens every document through content://,
 * verifies bytes/size/SHA/file-count, renames the provider staging tree to a
 * final proof tree, and then asks the user to confirm visibility in Android
 * Files. No raw filesystem path is used as a destination or as evidence.</p>
 */
public final class HostStorageDeviceProofActivity extends Activity {
    private static final int REQUEST_CODE = SafTreeDestinationSelector.REQUEST_CODE + 1;
    private final SafTreeDestinationStore destinationStore = new SafTreeDestinationStore();
    private TextView evidenceView;
    private Button runButton;
    private SafTreeDestinationRecord selectedRecord;
    private Uri finalProofUri;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setTitle("Alfa SAF Device Proof");

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        content.setPadding(padding, padding, padding, padding);

        TextView heading = new TextView(this);
        heading.setText("Alfa Device Ctrl — Host Storage Proof\n\n"
                + "Pilih folder melalui Android SAF. Alfa akan membuat tree provider "
                + "berisi nested files, membaca ulang melalui content://, lalu meminta "
                + "konfirmasi visual dari aplikasi Files.");
        content.addView(heading);

        Button selectButton = new Button(this);
        selectButton.setText("1. Pilih folder host melalui SAF");
        selectButton.setOnClickListener(v -> selectDestination());
        content.addView(selectButton);

        runButton = new Button(this);
        runButton.setText("2. Jalankan fixture provider proof");
        runButton.setEnabled(false);
        runButton.setOnClickListener(v -> runProof());
        content.addView(runButton);

        Button openButton = new Button(this);
        openButton.setText("3. Buka hasil di Android Files");
        openButton.setOnClickListener(v -> openProofInFiles());
        content.addView(openButton);

        evidenceView = new TextView(this);
        evidenceView.setText("DEVICE_PROOF=WAITING_FOR_SAF_SELECTION");
        evidenceView.setTextIsSelectable(true);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(evidenceView);
        content.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
        setContentView(content);
    }

    private void selectDestination() {
        try {
            startActivityForResult(
                    SafTreeDestinationSelector.createSelectionIntent(), REQUEST_CODE);
        } catch (RuntimeException error) {
            show("DEVICE_PROOF=BLOCKED\nREASON=SAF_PICKER_UNAVAILABLE\nDETAIL="
                    + safe(error.getMessage()));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_CODE) return;
        String requestId = "device-proof-" + System.currentTimeMillis();
        SafTreeDestinationSelector.SelectionResult result =
                SafTreeDestinationSelector.handleActivityResult(
                        resultCode,
                        data,
                        getContentResolver(),
                        requestId,
                        "Alfa SAF Device Proof");
        if (!result.success()) {
            selectedRecord = null;
            runButton.setEnabled(false);
            show("TREE_SELECTION=FAIL\nSTATE=" + result.state().name()
                    + "\nERROR_CODE=" + result.errorCode()
                    + "\nERROR=" + result.errorMessage()
                    + "\nNEXT=" + result.actionableNextStep());
            return;
        }
        selectedRecord = result.record();
        boolean saved = destinationStore.save(this, selectedRecord);
        runButton.setEnabled(saved);
        show("TREE_SELECTION=PASS\nPERSISTED_PERMISSION=PASS\n"
                + "PROVIDER_AUTHORITY=" + selectedRecord.providerAuthority()
                + "\nTREE_URI=" + selectedRecord.treeUri()
                + "\nDOCUMENT_ID=" + selectedRecord.documentId()
                + "\nSTORE_SAVE=" + (saved ? "PASS" : "FAIL")
                + "\nDEVICE_PROOF=READY_TO_RUN");
    }

    private void runProof() {
        if (selectedRecord == null) {
            show("DEVICE_PROOF=BLOCKED\nREASON=NO_SAF_DESTINATION_SELECTED");
            return;
        }
        runButton.setEnabled(false);
        new Thread(() -> {
            try {
                ProofRun result = executeProof();
                runOnUiThread(() -> {
                    finalProofUri = result.finalUri;
                    show(result.evidence);
                });
            } catch (Exception error) {
                runOnUiThread(() -> show(
                        "DEVICE_PROOF=FAIL\nPROVIDER_WRITE=FAIL_OR_UNVERIFIED\n"
                                + "REASON=" + safe(error.getClass().getSimpleName())
                                + "\nDETAIL=" + safe(error.getMessage())
                                + "\nHOST_FILE_MANAGER_VISIBLE=UNVERIFIED"));
            } finally {
                runOnUiThread(() -> runButton.setEnabled(true));
            }
        }).start();
    }

    private ProofRun executeProof() throws Exception {
        List<ProofEntry> payload = new ArrayList<>();
        payload.add(ProofEntry.file(
                "proof-root/hello.txt",
                "text/plain",
                "Alfa SAF device proof\n".getBytes(StandardCharsets.UTF_8)));
        payload.add(ProofEntry.file(
                "proof-root/nested/space file.txt",
                "text/plain",
                "space-name-pass\n".getBytes(StandardCharsets.UTF_8)));
        payload.add(ProofEntry.file(
                "proof-root/nested/Unicode-文件.txt",
                "text/plain",
                "unicode-name-pass\n".getBytes(StandardCharsets.UTF_8)));
        payload.add(ProofEntry.file(
                "proof-root/proof-archive.zip",
                "application/zip",
                createProofZip()));

        String manifest = buildManifest(payload);
        List<ProofEntry> allEntries = new ArrayList<>(payload);
        allEntries.add(ProofEntry.file(
                "proof-root/manifest.txt",
                "text/plain",
                manifest.getBytes(StandardCharsets.UTF_8)));

        HostStorageExportContract.ExportRequest request =
                HostStorageExportContract.ExportRequest.builder()
                        .requestId("device-proof-" + System.currentTimeMillis())
                        .artifactType(HostStorageExportContract.ArtifactType.SOURCE_EXPORT)
                        .sourceDescriptor("android-device-proof-fixture")
                        .displayName("Alfa SAF Device Proof")
                        .mimeType("application/octet-stream")
                        .destinationMode(HostStorageExportContract.DestinationMode.SAF_TREE)
                        .requestedDestination(selectedRecord.treeUri())
                        .overwritePolicy(HostStorageExportContract.OverwritePolicy.NEVER_SILENT)
                        .expectedFileCount(allEntries.size())
                        .expectedByteSize(totalBytes(allEntries))
                        .build();

        HostStorageProvider.ProviderDestination destination = new HostStorageProvider.ProviderDestination() {
            @Override public String providerName() { return selectedRecord.providerAuthority(); }
            @Override public String canonicalUri() { return selectedRecord.treeUri(); }
            @Override public String documentId() { return selectedRecord.documentId(); }
            @Override public String displayName() { return selectedRecord.displayName(); }
        };

        SafTreeProvider provider = new SafTreeProvider(getContentResolver());
        HostStorageProvider.ExportSession session = provider.begin(request, destination);
        List<HostStorageProvider.SourceEntry> sources = new ArrayList<>();
        for (ProofEntry entry : allEntries) sources.add(entry);
        session.writeEntries(sources);
        Uri stagingUri = Uri.parse(session.snapshot().uri());
        Verification verificationBeforeRename = verifyEntries(stagingUri, allEntries);
        if (!verificationBeforeRename.pass) {
            throw new IOException("provider-readback-before-rename-failed: "
                    + verificationBeforeRename.detail);
        }

        String finalName = "Alfa-SAF-Device-Proof-" + System.currentTimeMillis();
        Uri renamed = DocumentsContract.renameDocument(
                getContentResolver(), stagingUri, finalName);
        if (renamed == null) throw new IOException("provider-final-rename-returned-null");
        Verification verificationAfterRename = verifyEntries(renamed, allEntries);
        if (!verificationAfterRename.pass) {
            throw new IOException("provider-readback-after-rename-failed: "
                    + verificationAfterRename.detail);
        }

        String evidence = "=== ALFA SAF DEVICE PROOF ===\n"
                + "SAF_PROVIDER=PASS\n"
                + "TREE_SELECTION=PASS\n"
                + "PERSISTED_PERMISSION=PASS\n"
                + "PROVIDER_WRITE=PASS\n"
                + "PROVIDER_READBACK=PASS\n"
                + "SHA256=PASS\n"
                + "FILE_COUNT=PASS\n"
                + "NESTED_TREE=PASS\n"
                + "UNICODE_FILENAME=PASS\n"
                + "SPACE_FILENAME=PASS\n"
                + "APK_OR_ARCHIVE=PASS\n"
                + "CONTENT_URI_READ=PASS\n"
                + "STAGING_FINALIZED=PASS\n"
                + "HOST_FILE_MANAGER_VISIBLE=UNVERIFIED_USER_ACTION_REQUIRED\n"
                + "HOST_FILE_MANAGER_OPEN=UNVERIFIED_USER_ACTION_REQUIRED\n"
                + "DEVICE_PROOF=PROVIDER_PASS_HOST_VISIBILITY_UNVERIFIED\n"
                + "PROVIDER_AUTHORITY=" + selectedRecord.providerAuthority() + "\n"
                + "TREE_URI=" + selectedRecord.treeUri() + "\n"
                + "FINAL_URI=" + renamed + "\n"
                + "FINAL_NAME=" + finalName + "\n"
                + "FILE_COUNT=" + verificationAfterRename.fileCount + "\n"
                + "TOTAL_BYTES=" + verificationAfterRename.totalBytes + "\n"
                + "MANIFEST_SHA256=" + sha256(manifest.getBytes(StandardCharsets.UTF_8)) + "\n"
                + "READBACK_SHA256S=" + verificationAfterRename.shaSummary;
        return new ProofRun(renamed, evidence);
    }

    private Verification verifyEntries(Uri root, List<ProofEntry> entries) throws Exception {
        long total = 0;
        StringBuilder hashes = new StringBuilder();
        for (ProofEntry expected : entries) {
            Uri document = findRelativeDocument(root, expected.relativePath);
            if (document == null) return Verification.fail("missing=" + expected.relativePath);
            byte[] actual = readAll(document);
            String actualSha = sha256(actual);
            if (actual.length != expected.bytes.length || !actualSha.equals(expected.sha256)) {
                return Verification.fail("mismatch=" + expected.relativePath
                        + ",expectedSize=" + expected.bytes.length
                        + ",actualSize=" + actual.length
                        + ",expectedSha=" + expected.sha256
                        + ",actualSha=" + actualSha);
            }
            total += actual.length;
            if (hashes.length() > 0) hashes.append(';');
            hashes.append(expected.relativePath).append('=').append(actualSha);
        }
        return Verification.pass(entries.size(), total, hashes.toString());
    }

    private Uri findRelativeDocument(Uri root, String relativePath) throws Exception {
        String[] parts = SafTreeProvider.validateRelativePath(relativePath, false);
        Uri parent = root;
        for (String part : parts) {
            parent = findChild(parent, part);
            if (parent == null) return null;
        }
        return parent;
    }

    private Uri findChild(Uri parent, String displayName) throws Exception {
        Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(
                parent, DocumentsContract.getDocumentId(parent));
        CursorAdapter cursor = new CursorAdapter(getContentResolver().query(
                children,
                new String[]{Document.COLUMN_DOCUMENT_ID, Document.COLUMN_DISPLAY_NAME},
                null, null, null));
        try {
            while (cursor.moveToNext()) {
                if (displayName.equals(cursor.name())) {
                    return DocumentsContract.buildDocumentUriUsingTree(
                            parent, cursor.documentId());
                }
            }
            return null;
        } finally {
            cursor.close();
        }
    }

    private byte[] readAll(Uri document) throws Exception {
        InputStream input = getContentResolver().openInputStream(document);
        if (input == null) throw new IOException("content-uri-read-null");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int count;
        try {
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
        } finally {
            input.close();
        }
        return output.toByteArray();
    }

    private static String buildManifest(List<ProofEntry> entries) {
        StringBuilder result = new StringBuilder();
        result.append("ALFA_SAF_DEVICE_PROOF_MANIFEST_V1\n");
        result.append("PAYLOAD_FILE_COUNT=").append(entries.size()).append('\n');
        for (ProofEntry entry : entries) {
            result.append(entry.relativePath).append('\t')
                    .append(entry.bytes.length).append('\t')
                    .append(entry.sha256).append('\n');
        }
        return result.toString();
    }

    private static byte[] createProofZip() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ZipOutputStream zip = new ZipOutputStream(bytes);
        try {
            zip.putNextEntry(new ZipEntry("inside.txt"));
            zip.write("valid zip payload\n".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("nested/inside-nested.txt"));
            zip.write("nested archive payload\n".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        } finally {
            zip.close();
        }
        return bytes.toByteArray();
    }

    private static long totalBytes(List<ProofEntry> entries) {
        long result = 0;
        for (ProofEntry entry : entries) result += entry.bytes.length;
        return result;
    }

    private void openProofInFiles() {
        if (finalProofUri == null) {
            show("HOST_FILE_MANAGER_OPEN=BLOCKED\nREASON=NO_FINAL_PROOF_URI");
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(finalProofUri, "vnd.android.document/directory");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
            show(evidenceView.getText() + "\nHOST_FILE_MANAGER_OPEN=USER_ACTION_STARTED\n"
                    + "CONFIRM_IN_FILES=Navigate and verify the final proof tree visibly");
        } catch (RuntimeException error) {
            show(evidenceView.getText() + "\nHOST_FILE_MANAGER_OPEN=FAIL\nDETAIL="
                    + safe(error.getMessage()));
        }
    }

    private void show(String value) {
        if (evidenceView != null) evidenceView.setText(value);
    }

    private static String sha256(byte[] bytes) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(bytes);
        StringBuilder result = new StringBuilder(64);
        for (byte value : hash) result.append(String.format(Locale.US, "%02x", value & 0xff));
        return result.toString();
    }

    private static String safe(String value) {
        return value == null ? "(none)" : value.replace('\n', ' ').replace('\r', ' ');
    }

    private static final class ProofEntry implements HostStorageProvider.SourceEntry {
        private final String relativePath;
        private final String mimeType;
        private final byte[] bytes;
        private final String sha256;

        private ProofEntry(String relativePath, String mimeType, byte[] bytes) throws Exception {
            this.relativePath = relativePath;
            this.mimeType = mimeType;
            this.bytes = bytes.clone();
            this.sha256 = sha256(this.bytes);
        }

        static ProofEntry file(String relativePath, String mimeType, byte[] bytes) throws Exception {
            return new ProofEntry(relativePath, mimeType, bytes);
        }

        @Override public String relativePath() { return relativePath; }
        @Override public boolean directory() { return false; }
        @Override public String mimeType() { return mimeType; }
        @Override public InputStream openStream() { return new ByteArrayInputStream(bytes); }
    }

    private static final class ProofRun {
        private final Uri finalUri;
        private final String evidence;
        private ProofRun(Uri finalUri, String evidence) {
            this.finalUri = finalUri;
            this.evidence = evidence;
        }
    }

    private static final class Verification {
        private final boolean pass;
        private final String detail;
        private final int fileCount;
        private final long totalBytes;
        private final String shaSummary;

        private Verification(boolean pass, String detail, int fileCount, long totalBytes, String shaSummary) {
            this.pass = pass;
            this.detail = detail;
            this.fileCount = fileCount;
            this.totalBytes = totalBytes;
            this.shaSummary = shaSummary;
        }

        static Verification pass(int count, long bytes, String hashes) {
            return new Verification(true, "", count, bytes, hashes);
        }

        static Verification fail(String detail) {
            return new Verification(false, detail, 0, 0, "");
        }
    }

    /** Small cursor adapter so all cursor close paths are explicit. */
    private static final class CursorAdapter {
        private final android.database.Cursor cursor;
        private final int nameIndex;
        private final int idIndex;

        private CursorAdapter(android.database.Cursor cursor) throws IOException {
            if (cursor == null) throw new IOException("content-uri-child-query-null");
            this.cursor = cursor;
            this.nameIndex = cursor.getColumnIndex(Document.COLUMN_DISPLAY_NAME);
            this.idIndex = cursor.getColumnIndex(Document.COLUMN_DOCUMENT_ID);
            if (nameIndex < 0 || idIndex < 0) {
                cursor.close();
                throw new IOException("content-uri-child-columns-missing");
            }
        }

        boolean moveToNext() { return cursor.moveToNext(); }
        String name() { return cursor.getString(nameIndex); }
        String documentId() { return cursor.getString(idIndex); }
        void close() { cursor.close(); }
    }
}
