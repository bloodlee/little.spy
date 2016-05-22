import org.junit.Test;
import org.yli.littlespy.LittleSpy;
import org.yli.littlespy.LittleSpyConfig;

/**
 * Created by yli on 5/21/2016.
 */
public class LittleSpyTest {

  @Test
  public void testCreation() {
    LittleSpy aSpy = new LittleSpy(9999, new LittleSpyConfig());
  }

}
