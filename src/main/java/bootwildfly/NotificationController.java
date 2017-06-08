package bootwildfly;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NotificationController {


    @RequestMapping(value="/ping", method=RequestMethod.GET)
    public String ping(){
        return "alive";
    }

    @RequestMapping(value="/notify", method=RequestMethod.POST)
    public String notify(@RequestBody String body){
        return body;
    }
}