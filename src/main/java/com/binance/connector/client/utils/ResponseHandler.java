package com.binance.connector.client.utils;

import com.binance.connector.client.exceptions.BinanceClientException;
import com.binance.connector.client.exceptions.BinanceConnectorException;
import com.binance.connector.client.exceptions.BinanceServerException;
import java.io.IOException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.json.JSONException;
import org.json.JSONObject;

public class ResponseHandler {
    private static final OkHttpClient client = HttpClientSingleton.getHttpClient();

    private ResponseHandler() {
    }

    public static final String handleResponse(Request request, boolean showLimitUsage) {
        try (Response response = client.newCall(request).execute()) {
            if (null == response) {
                throw new BinanceServerException("[ResponseHandler] No response from server");
            }

            String responseAsString = getResponseBodyAsString(response.body());

            if (response.code() >= 400 && response.code() <= 499) {
                throw handleErrorResponse(responseAsString, response.code());
            } else if (response.code() >= 500) {
                throw new BinanceServerException(responseAsString, response.code());
            }

            if (showLimitUsage) {
                return getlimitUsage(response, responseAsString);
            } else {
                return responseAsString;
            }
        }
        catch (IOException | IllegalStateException e) {
            throw new BinanceConnectorException("[ResponseHandler] OKHTTP Error: " + e.getMessage());
        }
    }

    private static String getlimitUsage(Response response, String resposeBodyAsString) throws IOException {
        JSONObject json = new JSONObject();
        json.put("x-sapi-used-ip-weight-1m", response.header("X-SAPI-USED-IP-WEIGHT-1M"));
        json.put("x-mbx-used-weight", response.header("x-mbx-used-weight"));
        json.put("x-mbx-used-weight-1m", response.header("x-mbx-used-weight-1m"));
        json.put("data", resposeBodyAsString);

        return json.toString();
    }

    private static BinanceClientException handleErrorResponse(String responseBody, int responseCode) {
        try {
            String errorMsg = JSONParser.getJSONStringValue(responseBody, "msg");
            int errorCode = JSONParser.getJSONIntValue(responseBody, "code");
            return new BinanceClientException(responseBody, errorMsg, responseCode, errorCode);
        }
        catch (JSONException e) {
            throw new BinanceClientException(responseBody, responseCode);
        }
    }

    private static String getResponseBodyAsString(ResponseBody body) throws IOException {
        if (null != body) {
            return body.string();
        } else {
            return "";
        }
    }
}
