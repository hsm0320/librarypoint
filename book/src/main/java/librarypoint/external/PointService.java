
package librarypoint.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Date;

//@FeignClient(name="point", url="http://localhost:8085")
@FeignClient(name="point", url="${api.point.url}")
public interface PointService {

    @RequestMapping(method= RequestMethod.POST, path="/points")
    public void registership(@RequestBody Point point);

}
