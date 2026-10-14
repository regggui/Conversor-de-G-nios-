package br.com.conversordegenios;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.database.Cursor;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.widget.Toast;
import android.graphics.Color;
import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import android.os.Environment;

public class MainActivity extends Activity {
    private static final int PICK_PROJECT = 72;
    private static final int SAVE_PROJECT_ZIP = 73;
    private LinearLayout root;
    private TextView status;
    private WebView preview;
    private File projectDir;
    private File lastIndex;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        projectDir = new File(getFilesDir(), "imported_project");
        if (hasBundledSite()) {
            openBundledSite();
        } else {
            showHome();
        }
    }

    private boolean hasBundledSite() {
        try (InputStream in = getAssets().open("site/index.html")) {
            return in != null;
        } catch (IOException e) {
            return false;
        }
    }

    private void openBundledSite() {
        LinearLayout page = verticalLayout();
        page.addView(label("Projeto HTML instalado", 14));
        page.addView(button("← Voltar", this::showHome));
        WebView web = new WebView(this);
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        web.setWebViewClient(new WebViewClient());
        web.setWebChromeClient(new WebChromeClient());
        page.addView(web, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(page);
        web.loadUrl("file:///android_asset/site/index.html");
    }

    private int dp(float n) {
        return (int)(n * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView label(String text, int size) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(size);
        t.setTextColor(Color.WHITE);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(12), dp(10), dp(12), dp(10));
        return t;
    }

    private Button button(String text, Runnable action) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(16);
        b.setMinHeight(dp(52));
        b.setOnClickListener(v -> {
            try { action.run(); }
            catch (Exception e) {
                String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                Toast.makeText(MainActivity.this, "Não foi possível executar: " + msg, Toast.LENGTH_LONG).show();
                if (status != null) status.setText("Erro: " + msg);
            }
        });
        return b;
    }

    private LinearLayout verticalLayout() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(16), dp(12), dp(16), dp(12));
        l.setBackgroundColor(Color.rgb(16, 19, 32));
        return l;
    }

    private void showHome() {
        preview = null;
        root = verticalLayout();
        root.addView(label("CONVERSOR DE GÊNIOS", 25));
        root.addView(label("Importe e teste projetos HTML ou ZIP no Android.", 16));
        root.addView(button("📂 Selecionar arquivo HTML ou ZIP", this::pickProject));
        root.addView(button("▶ Abrir último projeto importado", this::openLast));
        root.addView(button("📦 Preparar projeto para gerar APK", this::exportProjectZip));
        root.addView(button("🌐 Enviar ZIP ao GitHub", () -> openExternal("https://github.com/regggui/Conversor-de-G-nios-/upload/main")));
        root.addView(button("⬇ Ver APKs gerados", () -> openExternal("https://github.com/regggui/Conversor-de-G-nios-/actions/workflows/html-to-apk.yml")));
        root.addView(button("ℹ Como usar", this::showHelp));
        status = label(lastIndex == null ? "Pronto para importar." : "Último projeto disponível: " + lastIndex.getName(), 14);
        root.addView(status);
        TextView note = label("A prévia abre aqui dentro. A geração de APK de outros projetos ainda é feita pelo fluxo de compilação no GitHub.", 12);
        root.addView(note);
        setContentView(root);
    }

    private void showHelp() {
        new android.app.AlertDialog.Builder(this)
            .setTitle("Como usar")
            .setMessage("1. Toque em Selecionar arquivo HTML ou ZIP.\n2. Escolha o arquivo no gerenciador do celular.\n3. Para projetos com imagens, CSS e JavaScript, importe o ZIP completo com index.html dentro.\n4. Use os botões no topo da prévia para voltar ou importar outro projeto.\n\nEste app importa e prepara o projeto. O envio ao GitHub e o download do APK ainda exigem etapas manuais.")
            .setPositiveButton("Entendi", (d, w) -> d.dismiss())
            .show();
    }

    private void pickProject() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.setType("*/*");
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.putExtra(Intent.EXTRA_MIME_TYPES, new String[] {
            "text/html", "application/zip", "application/x-zip-compressed",
            "application/octet-stream"
        });
        try {
            startActivityForResult(i, PICK_PROJECT);
        } catch (Exception e) {
            // Fallback for file providers that do not support ACTION_OPEN_DOCUMENT.
            Intent fallback = new Intent(Intent.ACTION_GET_CONTENT);
            fallback.setType("*/*");
            fallback.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(fallback, PICK_PROJECT);
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SAVE_PROJECT_ZIP && resultCode == RESULT_OK && data != null && data.getData() != null) {
            saveProjectZip(data.getData());
            return;
        }
        if (requestCode != PICK_PROJECT || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        String name = getName(uri);
        try {
            deleteTree(projectDir);
            if (!projectDir.mkdirs() && !projectDir.isDirectory()) throw new IOException("não consegui criar a pasta do projeto");
            if (name.toLowerCase().endsWith(".zip")) {
                InputStream in = getContentResolver().openInputStream(uri);
                if (in == null) throw new IOException("não consegui ler o ZIP");
                try (InputStream input = in) { unzip(input, projectDir); }
            } else if (name.toLowerCase().endsWith(".html") || name.toLowerCase().endsWith(".htm") ||
                       "text/html".equalsIgnoreCase(getContentResolver().getType(uri))) {
                File out = new File(projectDir, "index.html");
                InputStream in = getContentResolver().openInputStream(uri);
                if (in == null) throw new IOException("não consegui ler o HTML");
                try (InputStream input = in; OutputStream output = new FileOutputStream(out)) {
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = input.read(buf)) > 0) output.write(buf, 0, n);
                }
            } else {
                throw new IOException("escolha um arquivo .html, .htm ou .zip");
            }
            File index = findIndex(projectDir);
            if (index == null) throw new IOException("não encontrei index.html dentro do projeto/ZIP");
            lastIndex = index;
            openFile(index, name);
        } catch (Exception e) {
            showHome();
            String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            status.setText("Falha ao importar: " + msg);
            new android.app.AlertDialog.Builder(this)
                .setTitle("Não consegui abrir o arquivo")
                .setMessage(msg + "\n\nPara um projeto completo, escolha o ZIP que contém o index.html e os arquivos usados por ele.")
                .setPositiveButton("OK", (d, w) -> d.dismiss())
                .show();
        }
    }

    private void exportProjectZip() {
        File index = lastIndex != null && lastIndex.isFile() ? lastIndex : findIndex(projectDir);
        if (index == null) {
            Toast.makeText(this, "Importe um projeto HTML ou ZIP primeiro.", Toast.LENGTH_LONG).show();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/zip");
        intent.putExtra(Intent.EXTRA_TITLE, "input-project.zip");
        startActivityForResult(intent, SAVE_PROJECT_ZIP);
    }

    private void zipDirectory(File base, File current, ZipOutputStream zos) throws IOException {
        File[] files = current.listFiles();
        if (files == null) return;
        byte[] buffer = new byte[8192];
        for (File file : files) {
            String path = base.toURI().relativize(file.toURI()).getPath();
            if (file.isDirectory()) {
                if (!path.endsWith("/")) path += "/";
                zos.putNextEntry(new ZipEntry(path));
                zos.closeEntry();
                zipDirectory(base, file, zos);
            } else {
                zos.putNextEntry(new ZipEntry(path));
                try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
                    int count;
                    while ((count = in.read(buffer)) != -1) zos.write(buffer, 0, count);
                }
                zos.closeEntry();
            }
        }
    }

    private void saveProjectZip(Uri destination) {
        try (OutputStream raw = getContentResolver().openOutputStream(destination);
             ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(raw))) {
            if (raw == null) throw new IOException("não consegui criar o arquivo ZIP");
            zipDirectory(projectDir, projectDir, zip);
            zip.finish();
            new android.app.AlertDialog.Builder(this)
                .setTitle("Projeto preparado")
                .setMessage("O arquivo input-project.zip está pronto. Agora envie esse ZIP para a raiz do repositório GitHub e confirme o commit. Isso inicia a compilação. Depois, abra Ver APKs gerados e baixe o artefato APK-do-projeto-HTML quando o fluxo terminar. O envio e o download ainda são manuais para proteger sua conta.")
                .setPositiveButton("Enviar ZIP ao GitHub", (d, w) -> openExternal("https://github.com/regggui/Conversor-de-G-nios-/upload/main"))
                .setNeutralButton("Ver compilações", (d, w) -> openExternal("https://github.com/regggui/Conversor-de-G-nios-/actions/workflows/html-to-apk.yml"))
                .setNegativeButton("Fechar", (d, w) -> d.dismiss())
                .show();
        } catch (Exception e) {
            String msg = e.getMessage() == null ? "erro ao criar ZIP" : e.getMessage();
            Toast.makeText(this, "Falha ao preparar projeto: " + msg, Toast.LENGTH_LONG).show();
        }
    }

    private void openExternal(String url) {
        try {
            Intent open = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(open);
        } catch (Exception e) {
            Toast.makeText(this, "Não consegui abrir o navegador.", Toast.LENGTH_LONG).show();
        }
    }

    private String getName(Uri uri) {
        try (Cursor c = getContentResolver().query(uri, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int col = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (col >= 0) return c.getString(col);
            }
        } catch (Exception ignored) {}
        return "projeto.html";
    }

    private void unzip(InputStream input, File dest) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(input))) {
            ZipEntry entry;
            byte[] buffer = new byte[8192];
            while ((entry = zis.getNextEntry()) != null) {
                File out = new File(dest, entry.getName()).getCanonicalFile();
                String base = dest.getCanonicalPath() + File.separator;
                String target = out.getCanonicalPath();
                if (!target.startsWith(base)) throw new IOException("ZIP contém caminho inseguro");
                if (entry.isDirectory()) {
                    if (!out.mkdirs() && !out.isDirectory()) throw new IOException("não consegui criar pasta do ZIP");
                } else {
                    File parent = out.getParentFile();
                    if (parent != null && !parent.mkdirs() && !parent.isDirectory()) throw new IOException("não consegui criar pasta do arquivo");
                    try (OutputStream os = new FileOutputStream(out)) {
                        int n;
                        while ((n = zis.read(buffer)) > 0) os.write(buffer, 0, n);
                    }
                }
                zis.closeEntry();
            }
        }
    }

    private File findIndex(File dir) {
        File f = new File(dir, "index.html");
        if (f.isFile()) return f;
        File[] children = dir.listFiles();
        if (children != null) {
            for (File c : children) if (c.isDirectory()) {
                File found = findIndex(c);
                if (found != null) return found;
            }
        }
        return null;
    }

    private void openLast() {
        File index = lastIndex != null && lastIndex.isFile() ? lastIndex : findIndex(projectDir);
        if (index == null) {
            Toast.makeText(this, "Importe um projeto primeiro.", Toast.LENGTH_LONG).show();
            return;
        }
        lastIndex = index;
        openFile(index, index.getName());
    }

    private void openFile(File index, String projectName) {
        LinearLayout page = verticalLayout();
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.addView(button("← Início", this::showHome), new LinearLayout.LayoutParams(0, dp(52), 1));
        actions.addView(button("+ Importar", this::pickProject), new LinearLayout.LayoutParams(0, dp(52), 1));
        page.addView(actions);
        page.addView(button("📦 Preparar projeto para gerar APK", this::exportProjectZip));
        TextView title = label("Prévia: " + projectName, 13);
        page.addView(title);

        LinearLayout zoom = new LinearLayout(this);
        zoom.setOrientation(LinearLayout.HORIZONTAL);
        zoom.addView(button("A− Diminuir", () -> changeZoom(-1)),
            new LinearLayout.LayoutParams(0, dp(48), 1));
        zoom.addView(button("100%", this::resetZoom),
            new LinearLayout.LayoutParams(0, dp(48), 1));
        zoom.addView(button("A+ Aumentar", () -> changeZoom(1)),
            new LinearLayout.LayoutParams(0, dp(48), 1));
        page.addView(zoom);

        preview = new WebView(this);
        WebSettings s = preview.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setJavaScriptCanOpenWindowsAutomatically(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        preview.setWebViewClient(new WebViewClient());
        preview.setWebChromeClient(new WebChromeClient());
        page.addView(preview, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(page);
        preview.loadUrl(Uri.fromFile(index).toString());
    }

    private void changeZoom(int direction) {
        if (preview == null) {
            Toast.makeText(this, "Abra um projeto primeiro.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (direction > 0) preview.zoomIn();
        else preview.zoomOut();
    }

    private void resetZoom() {
        if (preview == null) {
            Toast.makeText(this, "Abra um projeto primeiro.", Toast.LENGTH_SHORT).show();
            return;
        }
        preview.getSettings().setTextZoom(100);
        preview.zoomOut();
        preview.zoomOut();
        preview.zoomOut();
        preview.zoomOut();
        Toast.makeText(this, "Zoom reajustado. Se precisar, use A+ ou A−.", Toast.LENGTH_SHORT).show();
    }

    private void deleteTree(File f) {
        if (!f.exists()) return;
        File[] children = f.listFiles();
        if (children != null) for (File c : children) deleteTree(c);
        f.delete();
    }

    @Override public void onBackPressed() {
        if (preview != null && preview.canGoBack()) {
            preview.goBack();
        } else {
            showHome();
        }
    }
}
