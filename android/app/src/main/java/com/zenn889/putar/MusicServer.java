package com.zenn889.putar;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Server HTTP mini di dalam aplikasi untuk streaming lagu dari MediaStore
 * ke WebView (<audio>). Mendukung Range request agar seek berjalan mulus,
 * plus header CORS agar visualizer (Web Audio) tetap hidup.
 */
public class MusicServer {
    private static MusicServer instance;
    private final Context ctx;
    private final int port;
    private final ExecutorService pool = Executors.newCachedThreadPool();
    private volatile boolean running = true;

    public static synchronized MusicServer get(Context ctx) {
        if (instance == null) instance = new MusicServer(ctx);
        return instance;
    }

    private MusicServer(Context ctx) {
        this.ctx = ctx.getApplicationContext();
        try (ServerSocket s = new ServerSocket(0)) {
            this.port = s.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException("gagal pilih port", e);
        }
        new Thread(this::acceptLoop, "putar-music-server").start();
    }

    public int getPort() { return port; }

    private void acceptLoop() {
        try (ServerSocket ss = new ServerSocket(port)) {
            while (running) {
                Socket sock = ss.accept();
                pool.execute(() -> handle(sock));
            }
        } catch (IOException ignored) { }
    }

    private void handle(Socket sock) {
        try (sock) {
            sock.setSoTimeout(15000);
            String req = readRequestLine(sock);
            if (req == null) return;
            String[] parts = req.split(" ");
            if (parts.length < 2) return;
            String method = parts[0];
            String path = parts[1];
            String headers = readHeaders(sock);
            if (!"GET".equals(method)) return;

            long id;
            try {
                id = Long.parseLong(queryParam(path, "id"));
            } catch (Exception e) {
                respond(sock, 400, "text/plain", null, null, 0, 0, 0);
                return;
            }
            String mime = queryParam(path, "m");
            if (mime == null || mime.isEmpty()) mime = "audio/mpeg";

            Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
            long total;
            try (android.os.ParcelFileDescriptor pfd =
                     ctx.getContentResolver().openFileDescriptor(uri, "r")) {
                if (pfd == null) { respond(sock, 404, "text/plain", null, null, 0, 0, 0); return; }
                total = pfd.getStatSize();
            } catch (Exception e) {
                respond(sock, 404, "text/plain", null, null, 0, 0, 0);
                return;
            }
            if (total <= 0) { respond(sock, 404, "text/plain", null, null, 0, 0, 0); return; }

            long start = 0, end = total - 1;
            String range = headerValue(headers, "range");
            boolean partial = false;
            if (range != null && range.startsWith("bytes=")) {
                String r = range.substring(6).trim();
                try {
                    int dash = r.indexOf('-');
                    if (dash > 0) {
                        start = Long.parseLong(r.substring(0, dash));
                        if (dash + 1 < r.length()) {
                            end = Math.min(total - 1, Long.parseLong(r.substring(dash + 1)));
                        }
                    } else if (dash == 0) {
                        long suffix = Long.parseLong(r.substring(1));
                        start = Math.max(0, total - suffix);
                    }
                    if (start > end || start >= total) {
                        respond(sock, 416, "text/plain", null, null, 0, 0, total);
                        return;
                    }
                    partial = true;
                } catch (NumberFormatException ignored) { }
            }

            String cors = "Accept-Ranges: bytes\r\n"
                    + "Access-Control-Allow-Origin: *\r\n"
                    + "Cache-Control: no-cache\r\n";
            respond(sock, partial ? 206 : 200, mime, cors, partial, start, end, total);
            streamBody(sock, uri, start, end - start + 1);
        } catch (Exception ignored) { }
    }

    private void streamBody(Socket sock, Uri uri, long start, long len) {
        ContentResolver cr = ctx.getContentResolver();
        try (android.os.ParcelFileDescriptor pfd = cr.openFileDescriptor(uri, "r");
             OutputStream out = new BufferedOutputStream(sock.getOutputStream(), 65536)) {
            if (pfd == null) return;
            try (FileInputStream fis = new FileInputStream(pfd.getFileDescriptor())) {
                fis.skipNBytes(start);
                byte[] buf = new byte[65536];
                long remaining = len;
                while (remaining > 0) {
                    int n = fis.read(buf, 0, (int) Math.min(buf.length, remaining));
                    if (n < 0) break;
                    out.write(buf, 0, n);
                    remaining -= n;
                }
                out.flush();
            }
        } catch (Exception ignored) { }
    }

    private void respond(Socket sock, int code, String type, String extraHeaders,
                         Boolean partial, long start, long end, long total) throws IOException {
        String statusLine = code == 200 ? "HTTP/1.1 200 OK"
                : code == 206 ? "HTTP/1.1 206 Partial Content"
                : code == 416 ? "HTTP/1.1 416 Range Not Satisfiable"
                : "HTTP/1.1 " + code + " Error";
        OutputStream out = sock.getOutputStream();
        StringBuilder sb = new StringBuilder();
        sb.append(statusLine).append("\r\n");
        sb.append("Content-Type: ").append(type).append("\r\n");
        sb.append("Connection: close\r\n");
        if (code == 206 && partial != null && partial) {
            sb.append("Content-Range: bytes ").append(start).append('-').append(end)
              .append('/').append(total).append("\r\n");
            sb.append("Content-Length: ").append(end - start + 1).append("\r\n");
        } else if (code == 416) {
            sb.append("Content-Range: bytes */").append(total).append("\r\n");
            sb.append("Content-Length: 0\r\n");
        } else if (code == 200) {
            sb.append("Content-Length: ").append(total).append("\r\n");
        } else {
            sb.append("Content-Length: 0\r\n");
        }
        if (extraHeaders != null) sb.append(extraHeaders);
        sb.append("\r\n");
        out.write(sb.toString().getBytes("UTF-8"));
        out.flush();
    }

    private static String readRequestLine(Socket sock) throws IOException {
        StringBuilder sb = new StringBuilder();
        int c;
        while (sb.length() < 8192 && (c = sock.getInputStream().read()) != -1) {
            if (c == '\n') break;
            if (c != '\r') sb.append((char) c);
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    private static String readHeaders(Socket sock) throws IOException {
        StringBuilder sb = new StringBuilder();
        int c, prev = -1;
        while (sb.length() < 16384 && (c = sock.getInputStream().read()) != -1) {
            sb.append((char) c);
            if (prev == '\n' && c == '\n') break;
            prev = c;
        }
        return sb.toString();
    }

    private static String headerValue(String headers, String name) {
        String lower = headers.toLowerCase();
        int idx = lower.indexOf("\n" + name.toLowerCase() + ":");
        if (idx < 0) idx = lower.indexOf("\r\n" + name.toLowerCase() + ":");
        if (idx < 0) return null;
        int start = headers.indexOf(':', idx) + 1;
        int end = headers.indexOf('\r', start);
        if (end < 0) end = headers.length();
        return headers.substring(start, end).trim();
    }

    private static String queryParam(String path, String key) {
        int q = path.indexOf('?');
        if (q < 0) return null;
        for (String pair : path.substring(q + 1).split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0 && pair.substring(0, eq).equals(key)) {
                return Uri.decode(pair.substring(eq + 1));
            }
        }
        return null;
    }
}
