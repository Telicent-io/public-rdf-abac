/*
 *  Copyright (c) Telicent Ltd.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.telicent.jena.abac.fuseki;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import org.apache.jena.atlas.RuntimeIOException;
import org.apache.jena.atlas.io.AWriter;
import org.apache.jena.atlas.io.IO;
import org.apache.jena.cmd.CmdException;
import org.apache.jena.http.HttpEnv;
import org.apache.jena.http.HttpLib;
import org.apache.jena.http.Push;

/**
 * Library for (re)playing HTT requests.
 * <p>
 * A request is the raw HTTP request.
 * <pre>
 * Headers
 * -- blank line --
 * body
 * </pre>
 * The library does parse the headers because the JDK library accepts headers as a map
 * the parsing is not full error checking.
 */
public class PlayLib {

    static public void sendFileHTTP(String url, String filename) {
        sendHTTP(url, IO.openFileBuffered(filename));
    }

    static public void sendStringHTTP(String url, String string) {
        InputStream input = new ByteArrayInputStream(string.getBytes(StandardCharsets.UTF_8));
        String x = httpRequestResponse(url, input, true);
    }

    static public void sendHTTP(String url, InputStream input) {
        httpRequestResponse(url, input, false);
    }

    static public String httpRequestResponse(String url, InputStream input, boolean withResponse) {
        try ( input ) {
            Map<String, String> headers = new HashMap<>();
            // Read until blank line.
            byte[] line = new byte[1000];
            for ( ;; ) {
                int x = readLine(input, line);
                if ( x == -1 )
                    break;
                if ( x == 1 && line[0] == '\n' ) {
                    break;
                }
                // Exclude the final newline.
                String header = new String(line, 0, x-1, StandardCharsets.UTF_8);
                accumulateHeader(headers, header);
            }

            BodyPublisher bodyPublisher = BodyPublishers.ofInputStream(()->input);
            Consumer<HttpRequest.Builder> modifier = builder -> headers.forEach((h,v) -> builder.header(h, v));

            if ( ! withResponse ) {
                // If don't want the response.
                HttpLib.httpPushData(HttpEnv.getDftHttpClient(), Push.POST, url, modifier, bodyPublisher);
                return null;
            }

            // With response
            HttpResponse<InputStream> response = httpPushWithResponse(HttpEnv.getDftHttpClient(), Push.POST, url, modifier, bodyPublisher);
            HttpLib.handleHttpStatusCode(response);
            StringWriter w = new StringWriter();
            try ( AWriter out = IO.wrap(w) ) {
                printResponse(out, response);
            }
            HttpLib.finish(response.body());
            return w.toString();
        } catch (IOException e) {
            throw new RuntimeIOException(e.getMessage());
        }

    }

    private static void printResponse(AWriter out, HttpResponse<InputStream> response) throws IOException {
        //Write out response.
        response.headers().map().forEach((header,values)->{
            values.forEach(v->out.printf("%s: %s\n", header, v));
        });
        out.println();
        String x = IO.readWholeFileAsUTF8(response.body());
        if ( ! x.isEmpty() )
            out.print(x);
    }

    // From jena HttpLib (not public)
    public static HttpResponse<InputStream> httpPushWithResponse(HttpClient httpClient, Push style, String url,
                                                                 Consumer<HttpRequest.Builder> modifier, BodyPublisher body) {
        URI uri = HttpLib.toRequestURI(url);
        HttpRequest.Builder builder = HttpLib.requestBuilderFor(url);
        builder.uri(uri);
        builder.method(style.method(), body);
        if ( modifier != null )
            modifier.accept(builder);
        HttpResponse<InputStream> response = HttpLib.execute(httpClient, builder.build());
        return response;
    }

    private static void send(String filename, OutputStream out,
                             Consumer<String> headerHandler,
                             Runnable endHeader,
                             Consumer<InputStream> bodyHandler) {
        Objects.requireNonNull(headerHandler);
        Objects.requireNonNull(bodyHandler);

        try (InputStream input = IO.openFileBuffered(filename)) {
            // Read until blank line.
            byte[] line = new byte[1000];
            for ( ;; ) {
                int x = readLine(input, line);
                if ( x == -1 )
                    break;
                if ( x == 1 && line[0] == '\n' ) {
                    endHeader.run();
                    break;
                }
                // Exclude the final newline.
                String header = new String(line, 0, x-1, StandardCharsets.UTF_8);
                if ( headerHandler != null )
                    headerHandler.accept(header);
                else
                    System.out.println(header);
            }
            bodyHandler.accept(input);
            input.transferTo(System.out);
        } catch (IOException e) {
            throw new RuntimeIOException(e.getMessage());
        }
    }

    private static void accumulateHeader(Map<String, String> headers, String header) {
        int idx = header.indexOf(':');
        if ( idx < 0 )
            throw new CmdException("Bad HTTP header: "+header);
        String h = header.substring(0,idx).strip();
        String v = header.substring(idx+1, header.length()).strip();
        headers.put(h, v);
    }

    private static int readLine(InputStream input, byte[] line) throws IOException {
        final int N = line.length;
        int i = 0;
        int bytesRead = 0;
        for(;;) {
            int x;
            x = input.read();
            if ( x == -1 )
                return bytesRead==0 ? -1 : bytesRead;
            bytesRead++;
            line[i++] = (byte)x;
            if ( x == '\n' )
                break;
            if ( i >= N )
                break;
        }
        return bytesRead;
    }

}
