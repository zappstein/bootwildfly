package bootwildfly;

import static java.util.regex.Pattern.DOTALL;
import static java.util.regex.Pattern.compile;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NotificationController {

    private Map<String, String> notifications = new ConcurrentHashMap<>();

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

    private final Pattern p = compile(".+MsgId>(\\w+)<.+", DOTALL);

    @RequestMapping(value = "/ping", method = RequestMethod.GET)
    public String ping() {
        return "alive";
    }

    @RequestMapping(value = "/notification", method = RequestMethod.POST, produces = "text/xml")
    public String notify(@RequestBody String body) {
        Matcher m = p.matcher(body);
        String msgId = "";
        if (m.matches()) {
            msgId = m.group(1);
            notifications.put(msgId, body);
        }
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        df.setTimeZone(java.util.TimeZone.getTimeZone("Zulu"));
        String timeStamp = df.format(new Date());
        return RESP_TEMPLATE.replaceFirst("-Timestamp-", timeStamp).replaceFirst("-MsgId-", msgId);
    }

    @RequestMapping(value = "/notification/{id}", method = RequestMethod.GET, produces = "text/xml")
    public String getNotification(@PathVariable(value = "id") String id, HttpServletResponse response) {
        if (!notifications.containsKey(id)) {
            response.setStatus(HttpStatus.NO_CONTENT.value());
        }
        return notifications.get(id);
    }

    @RequestMapping(value = "/notification/{id}", method = RequestMethod.DELETE)
    public void removeNotification(@PathVariable(value = "id") String id) {
        notifications.remove(id);
    }
}
