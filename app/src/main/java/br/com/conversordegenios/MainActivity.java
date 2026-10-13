package br.com.conversordegenios;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.database.Cursor;
import android.provider.OpenableColumns;
import android.view.Gravity;
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

public class MainActivity extends Activity {
    private static final int PICK_PROJECT = 72;
    private LinearLayout root;
    private TextView status;
    private WebView preview;
    private File projectDir;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        projectDir = new File(getFilesDir(), "imported_project");
        showHome();
    }

    private int dp(float n) { return (int)(n * getResources().getDisplayMetrics().density); }
    private TextView label(String text, int size) {
        TextView t = new TextView(this); t.setText(text); t.setTextSize(size);
        t.setTextColor(Color.WHITE); t.setGravity(Gravity.CENTER); t.setPadding(dp(12),dp(10),dp(12),dp(10)); return t;
    }
    private Button button(String text, Runnable action) {
        Button b = new Button(this); b.setText(text); b.setAllCaps(false);
        b.setOnClickListener(v -> action.run()); return b;
    }
    private void showHome() {
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18),dp(22),dp(18),dp(16)); root.setBackgroundColor(Color.rgb(16,19,32));
        root.addView(label("CONVERSOR DE GÊNIOS", 24));
        root.addView(label("HTML / ZIP → prévia Android", 16));
        root.addView(label("Importe um projeto HTML ou ZIP. O app procura index.html e seus arquivos.", 14));
        root.addView(button("Selecionar HTML ou ZIP", this::pickProject));
        root.addView(button("Abrir último projeto importado", this::openLast));
        status = label("Pronto para importar.", 13); root.addView(status);
        root.addView(label("Versão inicial de teste. Ainda não gera APKs de outros projetos dentro do celular.", 12));
        setContentView(root);
    }
    private void pickProject() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.setType("*/*"); i.addCategory(Intent.CATEGORY_OPENABLE);
        i.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"text/html","application/zip","application/x-zip-compressed","application/octet-stream"});
        startActivityForResult(i, PICK_PROJECT);
    }
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode != PICK_PROJECT || resultCode != RESULT_OK || data == null) return;
        Uri uri = data.getData();
        try {
            deleteTree(projectDir); projectDir.mkdirs();
            String name = getName(uri).toLowerCase();
            if(name.endsWith(".zip")) {
                try(InputStream in=getContentResolver().openInputStream(uri)) { unzip(in, projectDir); }
            } else {
                File out = new File(projectDir,"index.html");
                try(InputStream in=getContentResolver().openInputStream(uri); OutputStream o=new FileOutputStream(out)) {
                    byte[] buf=new byte[8192]; int n; while((n=in.read(buf))>0)o.write(buf,0,n);
                }
            }
            File index = findIndex(projectDir);
            if(index == null) { status.setText("Não encontrei index.html no projeto. Confira o ZIP."); return; }
            status.setText("Projeto importado: " + name);
            openFile(index);
        } catch(Exception e) {
            status.setText("Falha ao importar: " + e.getMessage());
        }
    }
    private String getName(Uri uri) {
        try(Cursor c=getContentResolver().query(uri,null,null,null,null)) {
            if(c!=null && c.moveToFirst()) { int col=c.getColumnIndex(OpenableColumns.DISPLAY_NAME); if(col>=0)return c.getString(col); }
        } catch(Exception ignored) {}
        return "projeto.html";
    }
    private void unzip(InputStream input, File dest) throws IOException {
        try(ZipInputStream zis=new ZipInputStream(new BufferedInputStream(input))) {
            ZipEntry entry; byte[] buffer=new byte[8192];
            while((entry=zis.getNextEntry())!=null) {
                File out=new File(dest,entry.getName()).getCanonicalFile();
                String base = dest.getCanonicalPath() + File.separator;
                String target = out.getCanonicalPath();
                if(!target.startsWith(base)) throw new IOException("ZIP contém caminho inseguro");
                if(entry.isDirectory()) out.mkdirs();
                else { File parent=out.getParentFile(); if(parent!=null)parent.mkdirs();
                    try(OutputStream os=new FileOutputStream(out)){int n;while((n=zis.read(buffer))>0)os.write(buffer,0,n);}
                }
                zis.closeEntry();
            }
        }
    }
    private File findIndex(File dir) {
        File f=new File(dir,"index.html"); if(f.isFile())return f;
        File[] children=dir.listFiles(); if(children!=null) for(File c:children) if(c.isDirectory()){File found=findIndex(c);if(found!=null)return found;}
        return null;
    }
    private void openLast() {
        File index=findIndex(projectDir);
        if(index==null){Toast.makeText(this,"Importe um projeto primeiro.",Toast.LENGTH_SHORT).show();return;}
        openFile(index);
    }
    private void openFile(File index) {
        preview=new WebView(this);
        WebSettings s=preview.getSettings(); s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true); s.setAllowContentAccess(true);
        preview.setWebViewClient(new WebViewClient());
        preview.setWebChromeClient(new WebChromeClient());
        preview.loadUrl(Uri.fromFile(index).toString());
        setContentView(preview);
    }
    private void deleteTree(File f) {
        if(!f.exists())return; File[] children=f.listFiles(); if(children!=null)for(File c:children)deleteTree(c); f.delete();
    }
    @Override public void onBackPressed() {
        if(preview!=null && preview.canGoBack()) preview.goBack(); else { preview=null; showHome(); }
    }
}
