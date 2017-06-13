package bootwildfly;

import static bootwildfly.NotificationBodyParser.retrieveData;
import static java.util.Arrays.asList;
import static java.util.TimeZone.getTimeZone;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class NotificationController {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationController.class);

    private Map<String, String> notifications = new ConcurrentHashMap<>();
    private Map<String, String> endpoints = new ConcurrentHashMap<>();
    private static final String MSG_ID = "MsgId";
    private static final String RTP_ID = "RTPId";
    private static final String MERCHANT_ID = "MerchantId";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    static {
        DATE_FORMAT.setTimeZone(getTimeZone("Zulu"));
    }

    private final String RESP_TEMPLATE =
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:dis=\"http://ap.com/xsd/distrmanagedistributordata_3\" xmlns:ack=\"http://ap.com/xsd/acknowledgement_3\" xmlns:mes=\"http://ap.com/xsd/messageheader_3\">" +
                    "<soapenv:Body>" +
                       "<dis:NotifyPayment_1Response>" +
                          "<ack:Ack>" +
                             "<mes:MsgTimestamp>-Timestamp-</mes:MsgTimestamp>" +
                             "<mes:OrgReqMsgId>-MsgId-</mes:OrgReqMsgId>" +
                             "<ack:MessageStatus>RECD</ack:MessageStatus>" +
                          "</ack:Ack>" +
                       "</dis:NotifyPayment_1Response>" +
                    "</soapenv:Body>" +
                 "</soapenv:Envelope>";

    @RequestMapping(value = "/ping", method = RequestMethod.GET)
    public String ping() {
        return "alive";
    }

    @RequestMapping(value = "/notification/services/manageDistributor_3", method = RequestMethod.POST, produces = "text/xml")
    public ResponseEntity<String> notify(@RequestBody String body, HttpServletRequest req) {

        LOG.info("Received notification from '{}'. ", req.getRemoteAddr());

        Map<String, String> values = retrieveData(body, asList(MSG_ID, RTP_ID, MERCHANT_ID));

        if (values.containsKey(RTP_ID)) {
            LOG.info("Saving notification with id '{}'. ", values.get(RTP_ID));
            notifications.put(values.get(RTP_ID), body);
        }

        ResponseEntity<String> resp = handleForward(body, values.get(MERCHANT_ID));

        if (resp == null) {
            resp = createFallbackResponse(values);
        }

        LOG.info("Notification processed. ");
        return resp;
    }


    @RequestMapping(value = "/notification/{id}", method = RequestMethod.GET, produces = "text/xml")
    public ResponseEntity<String> getNotification(@PathVariable(value = "id") String id) {
        LOG.info("Get notification with id '{}'", id);
        if (!notifications.containsKey(id)) {
            LOG.info("No notification with id '{}' found. ", id);
            return new ResponseEntity<>(NO_CONTENT);
        }
        return new ResponseEntity<>(notifications.get(id), OK);
    }

    @RequestMapping(value = "/notification/{id}", method = RequestMethod.DELETE)
    public void removeNotification(@PathVariable(value = "id") String id) {
        LOG.info("Removing stored notification with id '{}'", id);
        notifications.remove(id);
    }

    @RequestMapping(value = "/endpoint/{merchantId}", method = RequestMethod.POST)
    public void addEndpoint(@PathVariable(value = "merchantId") String merchantId, @RequestBody String body) {
        endpoints.put(merchantId, body);
        LOG.info("Added endpoint '{}' for merchantId '{}'", body, merchantId);
    }

    private ResponseEntity<String> handleForward(String reqBody, String merchantId) {

        if (endpoints.get(merchantId) == null) {
            LOG.info("No endpoint for merchantId '{}'. Forward skipped. ", merchantId);
            return null;
        }

        try {
            RestTemplate template = new RestTemplate();
            HttpEntity<String> body = new HttpEntity<>(reqBody);
            template.setErrorHandler(new ResponseErrorHandler());
            return template.exchange(endpoints.get(merchantId), HttpMethod.POST, body, String.class);
        } catch (Exception e) {
            LOG.warn("Exception occured while forwarding notification for merchantId '{}'. ", merchantId, e);
            return null;
        }
    }

    private ResponseEntity<String> createFallbackResponse(Map<String, String> values) {
        ResponseEntity<String> resp;
        LOG.info("Retrieving of notification response was not successful. Fallback response will be returned. ");
        resp = new ResponseEntity<>(
                RESP_TEMPLATE.replaceFirst("-Timestamp-", DATE_FORMAT.format(new Date())).replaceFirst("-MsgId-", values.get(MSG_ID)),
                HttpStatus.OK);
        return resp;
    }
}
