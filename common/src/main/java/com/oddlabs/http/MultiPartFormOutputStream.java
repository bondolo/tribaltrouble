package com.oddlabs.http;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import org.jspecify.annotations.Nullable;

/// Writes `multipart/form-data` payloads to an output stream for HTTP POST uploads.
public class MultiPartFormOutputStream {

  private static final String NEWLINE = "\r\n";
  private static final String PREFIX = "--";
  private final DataOutputStream out;
  private final String boundary;

  public MultiPartFormOutputStream(OutputStream os, String boundary) {
    if (boundary.isEmpty()) {
      throw new IllegalArgumentException("Boundary stream is required.");
    }
    this.out = new DataOutputStream(os);
    this.boundary = boundary;
  }

  public void writeField(String name, boolean value) throws IOException {
    writeField(name, Boolean.toString(value));
  }

  public void writeField(String name, double value) throws IOException {
    writeField(name, Double.toString(value));
  }

  public void writeField(String name, float value) throws IOException {
    writeField(name, Float.toString(value));
  }

  public void writeField(String name, long value) throws IOException {
    writeField(name, Long.toString(value));
  }

  public void writeField(String name, int value) throws IOException {
    writeField(name, Integer.toString(value));
  }

  public void writeField(String name, short value) throws IOException {
    writeField(name, Short.toString(value));
  }

  public void writeField(String name, char value) throws IOException {
    writeField(name, Character.toString(value));
  }

  /// Writes a string field value, sending an empty string if `value` is `null`.
  public void writeField(String name, @Nullable String value) throws IOException {
    if (value == null) {
      value = "";
    }
    /*
      --boundary\r\n
      Content-Disposition: form-data; name="<fieldName>"\r\n
      \r\n
      <value>\r\n
    */
    // write boundary
    out.writeBytes(PREFIX);
    out.writeBytes(boundary);
    out.writeBytes(NEWLINE);
    // write content header
    out.writeBytes("Content-Disposition: form-data; name=\"" + name + "\"");
    out.writeBytes(NEWLINE);
    out.writeBytes(NEWLINE);
    // write content
    out.writeBytes(value);
    out.writeBytes(NEWLINE);
    out.flush();
  }

  public void writeFile(String name, @Nullable String mimeType, File file)
      throws IllegalArgumentException, IOException {
    if (!file.exists()) {
      throw new IllegalArgumentException("File does not exist.");
    }
    if (file.isDirectory()) {
      throw new IllegalArgumentException("File cannot be a directory.");
    }
    writeFile(name, mimeType, file.getCanonicalPath(), new FileInputStream(file));
  }

  private void writeFileHeader(String name, @Nullable String mimeType, String fileName) throws IOException {
    out.writeBytes(PREFIX);
    out.writeBytes(boundary);
    out.writeBytes(NEWLINE);
    // write content header
    out.writeBytes("Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + fileName + "\"");
    out.writeBytes(NEWLINE);
    if (mimeType != null) {
      out.writeBytes("Content-Type: " + mimeType);
      out.writeBytes(NEWLINE);
    }
    out.writeBytes(NEWLINE);
  }

  /// Writes the contents of `is` as a multipart file entry and closes `is`.
  public void writeFile(String name, @Nullable String mimeType, String fileName, InputStream is)
      throws IllegalArgumentException, IOException {
    if (fileName.isEmpty()) {
      throw new IllegalArgumentException("File name cannot be null or empty.");
    }
    /*
      --boundary\r\n
      Content-Disposition: form-data; name="<fieldName>"; filename="<filename>"\r\n
      Content-Type: <mime-type>\r\n
      \r\n
      <file-data>\r\n
    */
    // write boundary
    writeFileHeader(name, mimeType, fileName);
    try (is) {
      // write content
      byte[] data = new byte[1024];
      int r;
      while ((r = is.read(data, 0, data.length)) != -1) {
        out.write(data, 0, r);
      }
    }
    out.writeBytes(NEWLINE);
    out.flush();
  }

  public void writeFile(String name, String mimeType, String fileName, byte[] data)
      throws IllegalArgumentException, IOException {
    if (fileName.isEmpty()) {
      throw new IllegalArgumentException("File name cannot be empty.");
    }
    /*
      --boundary\r\n
      Content-Disposition: form-data; name="<fieldName>"; filename="<filename>"\r\n
      Content-Type: <mime-type>\r\n
      \r\n
      <file-data>\r\n
    */
    // write boundary
    writeFileHeader(name, mimeType, fileName);
    // write content
    out.write(data, 0, data.length);
    out.writeBytes(NEWLINE);
    out.flush();
  }

  /// No-op; `writeField` and `writeFile` flush automatically.
  public void flush() throws IOException {
    // out.flush();
  }

  /// Writes the final multipart boundary and closes the underlying stream; must be called to finalize the payload.
  public void close() throws IOException {
    // write final boundary
    out.writeBytes(PREFIX);
    out.writeBytes(boundary);
    out.writeBytes(PREFIX);
    out.writeBytes(NEWLINE);
    out.flush();
    out.close();
  }

  public String getBoundary() {
    return this.boundary;
  }

  /// Opens a `URLConnection` configured for non-cached HTTP `POST` input/output.
  public static URLConnection createConnection(URL url) throws IOException {
    URLConnection urlConn = url.openConnection();
    if (urlConn instanceof HttpURLConnection httpConn) {
      httpConn.setRequestMethod("POST");
    }
    urlConn.setDoInput(true);
    urlConn.setDoOutput(true);
    urlConn.setUseCaches(false);
    urlConn.setDefaultUseCaches(false);
    return urlConn;
  }

  public static String createBoundary() {
    return "--------------------" + Long.toString(System.currentTimeMillis(), 16);
  }

  public static String getContentType(String boundary) {
    return "multipart/form-data; boundary=" + boundary;
  }

  // Test method
  void main() throws Exception {
    URL url = URI.create("http://www.domain.com/webems/upload.do").toURL();
    // --create a boundary string
    String boundary = MultiPartFormOutputStream.createBoundary();
    URLConnection urlConn = MultiPartFormOutputStream.createConnection(url);
    urlConn.setRequestProperty("Accept", "*/*");
    urlConn.setRequestProperty("Content-Type", MultiPartFormOutputStream.getContentType(boundary));
    // --set some other request headers...
    urlConn.setRequestProperty("Connection", "Keep-Alive");
    urlConn.setRequestProperty("Cache-Control", "no-cache");
    // --no need to connect cuz getOutputStream() does it
    MultiPartFormOutputStream out = new MultiPartFormOutputStream(urlConn.getOutputStream(), boundary);
    // --write a text field element
    out.writeField("myText", "text field text");
    // --upload a file
    out.writeFile("myFile", "text/plain", new File("C:\\test.txt"));
    // --can also write bytes directly
    // out.writeFile("myFile", "text/plain", "C:\\test.txt",
    // "This is some file text.".getBytes("ASCII"));
    out.close();
    // --read response from server
    BufferedReader in = new BufferedReader(new InputStreamReader(urlConn.getInputStream(), StandardCharsets.UTF_8));
    String line;
    while ((line = in.readLine()) != null) {
      IO.println(line);
    }
    in.close();
  }
}
