/*
 *  Copyright (C) 2023 Piotr Przybył
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package dev.softwaregarden.adventCalendar;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.InfoResponse;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.lifecycle.Startables;

import java.io.IOException;
import java.util.Arrays;

public class TwoContainersTest {

    static final ElasticsearchContainer es7Container =
            new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:7.17.15")
                    .withPassword(ElasticsearchContainer.ELASTICSEARCH_DEFAULT_PASSWORD);
    static final ElasticsearchContainer es8Container =
            new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:8.11.1");

    static ElasticsearchClient es7Client;
    static ElasticsearchClient es8Client;

    @BeforeAll
    static void setUpContainersAndClients() {

        Startables.deepStart(es7Container, es8Container).join();

        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(
                AuthScope.ANY,
                new UsernamePasswordCredentials("elastic", ElasticsearchContainer.ELASTICSEARCH_DEFAULT_PASSWORD)
        );
        JacksonJsonpMapper jsonpMapper = new JacksonJsonpMapper();

        RestClient restClient7 = RestClient
                .builder(HttpHost.create(es7Container.getHttpHostAddress()))
                .setHttpClientConfigCallback(httpClientBuilder ->
                        httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
                ).build();


        es7Client = new ElasticsearchClient(new RestClientTransport(restClient7, jsonpMapper));

        RestClient restClient8 = RestClient
                .builder(new HttpHost(es8Container.getHost(), es8Container.getMappedPort(9200), "https"))
                .setHttpClientConfigCallback(httpClientBuilder ->
                        httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
                                .setSSLContext(es8Container.createSslContextFromCa())
                ).build();

        es8Client = new ElasticsearchClient(new RestClientTransport(restClient8, jsonpMapper));
    }

    @Test
    void whatIsItForTest() throws IOException {
        for (ElasticsearchClient esClient : Arrays.asList(es7Client, es8Client)) {
            InfoResponse info = esClient.info();
            Assertions.assertEquals("You Know, for Search", info.tagline());
        }
    }
}
