package bootwildfly;

import java.io.IOException;

import org.springframework.http.client.ClientHttpResponse;

public class ResponseErrorHandler implements org.springframework.web.client.ResponseErrorHandler {

    @Override
    public void handleError(ClientHttpResponse resp) throws IOException {
        //should be never called
    }

    @Override
    public boolean hasError(ClientHttpResponse resp) throws IOException {
        return false;
    }

}
