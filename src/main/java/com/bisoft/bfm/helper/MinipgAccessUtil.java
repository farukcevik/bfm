package com.bisoft.bfm.helper;

import com.bisoft.bfm.dto.CheckPointDTO;
import com.bisoft.bfm.dto.PromoteDTO;
import com.bisoft.bfm.dto.RewindDTO;
import com.bisoft.bfm.model.PostgresqlServer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.client5.http.ssl.TrustSelfSignedStrategy;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.http.ssl.TLS;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.ssl.TrustStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.StringBufferInputStream;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
@Data
@RequiredArgsConstructor
public class MinipgAccessUtil {

    private final SymmetricEncryptionUtil symmetricEncryptionUtil;

    @Value("${minipg.username:postgres}")
    private String username;

    @Value("${minipg.password:bfm}")
    private String password;

    @Value("${minipg.port:9995}")
    private int port;

    @Value("${minipg.use-tls:false}")
    private boolean useTls;

    private String serverUrl;

    @Value("${bfm.user-crypted:false}")
    public boolean isEncrypted;


    @PostConstruct
    public void init(){
        final String scheme = useTls == false?"http":"https";
        serverUrl = scheme+"://{HOST}:"+String.valueOf(port);
        if(isEncrypted) {
            //  log.info(symmetricEncryptionUtil.decrypt(tlsSecret).replace("=",""));
            password = (symmetricEncryptionUtil.decrypt(password).replace("=", ""));
        }
    }

    public String status(PostgresqlServer postgresqlServer) throws Exception{

        SSLConnectionSocketFactory scsf = new SSLConnectionSocketFactory(
                SSLContexts.custom().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build(),
                NoopHostnameVerifier.INSTANCE);

        final HttpClientConnectionManager cm = PoolingHttpClientConnectionManagerBuilder.create()
                .setSSLSocketFactory(scsf)
                .build();

        final String serverAddress = postgresqlServer.getServerAddress().split(":")[0];
        String minipgUrl = serverUrl.replace("{HOST}",serverAddress);
        final BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope(serverAddress, port),
                new UsernamePasswordCredentials(username, password.toCharArray()));

        try (CloseableHttpClient httpclient = HttpClients.custom()
                .setConnectionManager(cm)
                .setDefaultCredentialsProvider(credsProvider)
                .build()) {

            HttpGet httpGet = new HttpGet(minipgUrl+"/minipg/pgstatus");
           // httpGet.setScheme("https");

            try (CloseableHttpResponse response1 = httpclient.execute(httpGet)) {
               // log.info(response1.getCode() + " " + response1.getReasonPhrase());
                HttpEntity entity1 = (HttpEntity) response1.getEntity();
                // do something useful with the response body
                // and ensure it is fully consumed
                String result = (EntityUtils.toString(response1.getEntity()));
                return result;
            }catch (Exception e){
                log.error("Unable get status of server "+postgresqlServer.getServerAddress());
            }


        } catch (IOException e) {
            log.error("Unable get status of server "+postgresqlServer.getServerAddress());
        }

        return "OK";

    }

    public String vipUp(PostgresqlServer postgresqlServer) throws Exception{
        //log.info("username : "+username+", password : "+password);
        log.info("vip up sent to "+postgresqlServer.getServerAddress());
        final String serverAddress = postgresqlServer.getServerAddress().split(":")[0];
        String minipgUrl = serverUrl.replace("{HOST}",serverAddress);
        final BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope(serverAddress, port),
                new UsernamePasswordCredentials(username, password.toCharArray()));
        SSLConnectionSocketFactory scsf = new SSLConnectionSocketFactory(
                SSLContexts.custom().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build(),
                NoopHostnameVerifier.INSTANCE);
        final HttpClientConnectionManager cm = PoolingHttpClientConnectionManagerBuilder.create()
                .setSSLSocketFactory(scsf)
                .build();

        try (CloseableHttpClient httpclient = HttpClients.custom()
                .setConnectionManager(cm)
                .setDefaultCredentialsProvider(credsProvider)
                .build()) {

            HttpGet httpGet = new HttpGet(minipgUrl+"/minipg/vip-up");

            try (CloseableHttpResponse response1 = httpclient.execute(httpGet)) {
                log.info(response1.getCode() + " " + response1.getReasonPhrase());
                HttpEntity entity1 = (HttpEntity) response1.getEntity();
                // do something useful with the response body
                // and ensure it is fully consumed
                String result = (EntityUtils.toString(response1.getEntity()));
                return result;
            }catch (Exception e){
                log.error("Unable set vip to server "+postgresqlServer.getServerAddress());
            }


        } catch (IOException e) {
            log.error("Unable set vip to server "+postgresqlServer.getServerAddress());
        }

        return "OK";

    }

    public String vipDown(PostgresqlServer postgresqlServer) throws Exception{
        //log.info("username : "+username+", password : "+password);
        log.info("vip down sent to "+postgresqlServer.getServerAddress());
        final String serverAddress = postgresqlServer.getServerAddress().split(":")[0];
        String minipgUrl = serverUrl.replace("{HOST}",serverAddress);
        final BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope(serverAddress, port),
                new UsernamePasswordCredentials(username, password.toCharArray()));

        SSLConnectionSocketFactory scsf = new SSLConnectionSocketFactory(
                SSLContexts.custom().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build(),
                NoopHostnameVerifier.INSTANCE);

        final HttpClientConnectionManager cm = PoolingHttpClientConnectionManagerBuilder.create()
                .setSSLSocketFactory(scsf)
                .build();

        try (CloseableHttpClient httpclient = HttpClients.custom()
                .setConnectionManager(cm)
                .setDefaultCredentialsProvider(credsProvider)
                .build()) {

            HttpGet httpGet = new HttpGet(minipgUrl+"/minipg/vip-down");

            try (CloseableHttpResponse response1 = httpclient.execute(httpGet)) {
                log.info(response1.getCode() + " " + response1.getReasonPhrase());
                HttpEntity entity1 = (HttpEntity) response1.getEntity();
                // do something useful with the response body
                // and ensure it is fully consumed
                String result = (EntityUtils.toString(response1.getEntity()));
                return result;
            }catch (Exception e){
                log.error("Unable get vip from server "+postgresqlServer.getServerAddress());
            }


        } catch (IOException e) {
            log.error("Unable get vip from server "+postgresqlServer.getServerAddress());
        }

        return "OK";

    }

    public String checkpoint(PostgresqlServer postgresqlServer) throws JsonProcessingException {
        //log.info("username : "+username+", password : "+password);
        final String serverAddress = postgresqlServer.getServerAddress().split(":")[0];
        final String serverPort = postgresqlServer.getServerAddress().split(":")[1];
        String minipgUrl = serverUrl.replace("{HOST}",serverAddress);
        final BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();

        //CheckPointDTO cpdto = CheckPointDTO.builder().user(serverAddress).port(serverPort).password().build();

        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        //String json = ow.writeValueAsString(cpdto);

        credsProvider.setCredentials(
                new AuthScope(serverAddress, port),
                new UsernamePasswordCredentials(username, password.toCharArray()));

        try (CloseableHttpClient httpclient = HttpClients.custom()
                .setDefaultCredentialsProvider(credsProvider)
                .build()) {

            HttpGet httpGet = new HttpGet(minipgUrl+"/minipg/checkpoint");

            try (CloseableHttpResponse response1 = httpclient.execute(httpGet)) {
                log.info(response1.getCode() + " " + response1.getReasonPhrase());
                HttpEntity entity1 = (HttpEntity) response1.getEntity();
                // do something useful with the response body
                // and ensure it is fully consumed
                String result = (EntityUtils.toString(response1.getEntity()));
                return result;
            }catch (Exception e){
                log.error("Unable perform checkpoint in server "+postgresqlServer.getServerAddress());
            }


        } catch (IOException e) {
            log.error("Unable perform checkpoint in server "+postgresqlServer.getServerAddress());
        }

        return "OK";

    }

    public String promote(PostgresqlServer postgresqlServer) throws Exception {
        //log.info("username : "+username+", password : "+password);
        log.info("promote sent to "+postgresqlServer.getServerAddress());
        final String serverAddress = postgresqlServer.getServerAddress().split(":")[0];
        final String serverPort = postgresqlServer.getServerAddress().split(":")[1];
        String minipgUrl = serverUrl.replace("{HOST}",serverAddress);
        final BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();

        SSLConnectionSocketFactory scsf = new SSLConnectionSocketFactory(
                SSLContexts.custom().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build(),
                NoopHostnameVerifier.INSTANCE);
        final HttpClientConnectionManager cm = PoolingHttpClientConnectionManagerBuilder.create()
                .setSSLSocketFactory(scsf)
                .build();

        PromoteDTO promoteDTO = PromoteDTO.builder().masterIp(postgresqlServer.getServerAddress().split(":")[0]).port(postgresqlServer.getServerAddress().split(":")[1]).user(postgresqlServer.getUsername()).password(postgresqlServer.getPassword()).build();

        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String json = ow.writeValueAsString(promoteDTO);

        credsProvider.setCredentials(
                new AuthScope(serverAddress, port),
                new UsernamePasswordCredentials(username, password.toCharArray()));

        try (CloseableHttpClient httpclient = HttpClients.custom()
                .setConnectionManager(cm)
                .setDefaultCredentialsProvider(credsProvider)
                .build()) {

            HttpPost request = new HttpPost(minipgUrl+"/minipg/promote");
            request.setHeader("Accept", "application/json");
            request.setHeader("Content-type", "application/json");

            final StringEntity entity = new StringEntity(json);
            request.setEntity(entity);

            try (CloseableHttpResponse response1 = httpclient.execute(request)) {
                log.info(response1.getCode() + " " + response1.getReasonPhrase());
                HttpEntity entity1 = (HttpEntity) response1.getEntity();
                // do something useful with the response body
                // and ensure it is fully consumed
                String result = (EntityUtils.toString(response1.getEntity()));
                return result;
            }catch (Exception e){
                log.error("Promote failed for "+postgresqlServer.getServerAddress());
            }


        } catch (IOException e) {
            log.error("Promote failed "+postgresqlServer.getServerAddress()+" is unreacable");
        }

        return "OK";

    }

    public String rewind(PostgresqlServer postgresqlServer,PostgresqlServer newMaster) throws Exception {
        //log.info("username : "+username+", password : "+password);
        log.info("rewind sent to "+postgresqlServer.getServerAddress()+" for master "+newMaster.getServerAddress());
        final String serverAddress = postgresqlServer.getServerAddress().split(":")[0];
        final String serverPort = postgresqlServer.getServerAddress().split(":")[1];
        String minipgUrl = serverUrl.replace("{HOST}",serverAddress);
        final BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();

        SSLConnectionSocketFactory scsf = new SSLConnectionSocketFactory(
                SSLContexts.custom().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build(),
                NoopHostnameVerifier.INSTANCE);
        final HttpClientConnectionManager cm = PoolingHttpClientConnectionManagerBuilder.create()
                .setSSLSocketFactory(scsf)
                .build();

        RewindDTO rewindDTO = RewindDTO.builder().masterIp(newMaster.getServerAddress().split(":")[0]).port(newMaster.getServerAddress().split(":")[1]).user(postgresqlServer.getUsername()).password(postgresqlServer.getPassword()).build();

        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String json = ow.writeValueAsString(rewindDTO);

        credsProvider.setCredentials(
                new AuthScope(serverAddress, port),
                new UsernamePasswordCredentials(username, password.toCharArray()));

        try (CloseableHttpClient httpclient = HttpClients.custom()
                .setConnectionManager(cm)
                .setDefaultCredentialsProvider(credsProvider)
                .build()) {

            HttpPost request = new HttpPost(minipgUrl+"/minipg/rewind");
            request.setHeader("Accept", "application/json");
            request.setHeader("Content-type", "application/json");

            final StringEntity entity = new StringEntity(json);
            request.setEntity(entity);

            try (CloseableHttpResponse response1 = httpclient.execute(request)) {
                log.info(response1.getCode() + " " + response1.getReasonPhrase());
                HttpEntity entity1 = (HttpEntity) response1.getEntity();
                // do something useful with the response body
                // and ensure it is fully consumed
                String result = (EntityUtils.toString(response1.getEntity()));
                return result;
            }catch (Exception e){
                log.error("Rewind failed for "+postgresqlServer.getServerAddress());
            }


        } catch (IOException e) {
            log.error("Rewind failed "+postgresqlServer.getServerAddress()+" is unreacable");
        }

        return "OK";

    }
}
