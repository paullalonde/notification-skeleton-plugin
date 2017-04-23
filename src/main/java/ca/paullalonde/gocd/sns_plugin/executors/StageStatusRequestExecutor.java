/*
 * Copyright 2017 Paul Lalonde enrg.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ca.paullalonde.gocd.sns_plugin.executors;

import ca.paullalonde.gocd.sns_plugin.requests.StageStatusRequest;
import ca.paullalonde.gocd.sns_plugin.PluginRequest;
import ca.paullalonde.gocd.sns_plugin.PluginSettings;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;

import java.util.Arrays;
import java.util.HashMap;

public class StageStatusRequestExecutor implements RequestExecutor {
    private static final Gson GSON = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();

    private final StageStatusRequest request;
    private final PluginRequest pluginRequest;

    public StageStatusRequestExecutor(StageStatusRequest request, PluginRequest pluginRequest) {
        this.request = request;
        this.pluginRequest = pluginRequest;
    }

    @Override
    public GoPluginApiResponse execute() throws Exception {
        HashMap<String, Object> responseJson = new HashMap<>();
        try {
            sendNotification();
            responseJson.put("status", "success");
        } catch (Exception e) {
            responseJson.put("status", "failure");
            responseJson.put("messages", Arrays.asList(e.getMessage()));
        }
        return new DefaultGoPluginApiResponse(200, GSON.toJson(responseJson));
    }

    protected void sendNotification() throws Exception {
        PluginSettings pluginSettings = pluginRequest.getPluginSettings();
        AmazonSNS sns = makeSns(pluginSettings);

        sns.publish(pluginSettings.getTopic(), request.toJSON());
    }

    private AmazonSNS makeSns(PluginSettings pluginSettings) throws Exception {
        String region = pluginSettings.getRegion();
        String awsAccessId = pluginSettings.getAwsAccessId();
        String awsSecretId = pluginSettings.getAwsSecretAccessId();
        AmazonSNSClientBuilder builder = AmazonSNSClientBuilder.standard();

        if ((region != null) && !region.isEmpty()) {
            builder = builder.withRegion(region);
        }

        if ((awsAccessId != null) && !awsAccessId.isEmpty() && (awsSecretId != null) && !awsSecretId.isEmpty()) {
            BasicAWSCredentials awsCreds = new BasicAWSCredentials(awsAccessId, awsSecretId);
            builder = builder.withCredentials(new AWSStaticCredentialsProvider(awsCreds));
        }

        return builder.build();
    }
}