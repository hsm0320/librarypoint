package librarypoint.external;

/**
 * Created by uengine on 2020. 4. 18..
 */
public class PointServiceFallback implements PointService {
    @Override
    public void registership(Point point) {
        //do nothing if you want to forgive it

        System.out.println("Circuit breaker has been opened. Fallback returned instead.");
    }
}
